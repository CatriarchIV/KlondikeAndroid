package com.example.klondike;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.graphics.*;
import android.content.Context;
import android.view.MotionEvent;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    GameView board;
    @Override public void onCreate(Bundle b) { super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(13,69,47)); getWindow().setNavigationBarColor(Color.rgb(13,69,47)); board=new GameView(this); setContentView(board); }
    @Override protected void onPause(){ super.onPause(); if(board!=null)board.save(); }

    static class Card {
        int suit,rank; boolean up;
        Card(int s,int r,boolean u){suit=s;rank=r;up=u;}
        int color(){return suit==1||suit==2?Color.rgb(190,43,53):Color.rgb(31,40,49);}
        JSONObject json() throws JSONException {JSONObject o=new JSONObject();o.put("s",suit);o.put("r",rank);o.put("u",up);return o;}
        static Card from(JSONObject o)throws JSONException{return new Card(o.getInt("s"),o.getInt("r"),o.getBoolean("u"));}
    }
    static class GameView extends View {
        ArrayList<Card> stock=new ArrayList<>(),waste=new ArrayList<>();
        ArrayList<Card>[] tableau=new ArrayList[7],foundation=new ArrayList[4];
        ArrayDeque<String> history=new ArrayDeque<>();
        Paint p=new Paint(3); Random random=new Random();
        int drawCount=1,moves=0; boolean won=false; float w,h,cw,ch,gap,top,tableY,step,hiddenStep;
        float downX,downY,dragX,dragY; int source=-1,sourceIndex=-1; boolean dragging=false;
        final String[] symbols={"♠","♥","♦","♣"}; final String[] ranks={"","A","2","3","4","5","6","7","8","9","10","J","Q","K"};
        GameView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);for(int i=0;i<7;i++)tableau[i]=new ArrayList<>();for(int i=0;i<4;i++)foundation[i]=new ArrayList<>();if(!load())newGame();}
        void newGame(){stock.clear();waste.clear();history.clear();moves=0;won=false;for(ArrayList<Card> a:tableau)a.clear();for(ArrayList<Card> a:foundation)a.clear();ArrayList<Card> deck=new ArrayList<>();for(int s=0;s<4;s++)for(int r=1;r<=13;r++)deck.add(new Card(s,r,false));Collections.shuffle(deck,random);int k=0;for(int col=0;col<7;col++)for(int j=0;j<=col;j++){Card c=deck.get(k++);c.up=j==col;tableau[col].add(c);}while(k<52)stock.add(deck.get(k++));invalidate();save();}
        JSONObject snapshot()throws JSONException {JSONObject o=new JSONObject();o.put("draw",drawCount);o.put("moves",moves);o.put("won",won);o.put("stock",arr(stock));o.put("waste",arr(waste));JSONArray t=new JSONArray(),f=new JSONArray();for(ArrayList<Card> a:tableau)t.put(arr(a));for(ArrayList<Card> a:foundation)f.put(arr(a));o.put("tableau",t);o.put("foundation",f);return o;}
        JSONArray arr(ArrayList<Card> a)throws JSONException {JSONArray out=new JSONArray();for(Card c:a)out.put(c.json());return out;}
        void restore(JSONObject o)throws JSONException {drawCount=o.optInt("draw",1);moves=o.optInt("moves",0);won=o.optBoolean("won",false);fill(stock,o.getJSONArray("stock"));fill(waste,o.getJSONArray("waste"));for(int i=0;i<7;i++)fill(tableau[i],o.getJSONArray("tableau").getJSONArray(i));for(int i=0;i<4;i++)fill(foundation[i],o.getJSONArray("foundation").getJSONArray(i));invalidate();}
        void fill(ArrayList<Card> a,JSONArray j)throws JSONException {a.clear();for(int i=0;i<j.length();i++)a.add(Card.from(j.getJSONObject(i)));}
        void save(){try{getContext().getSharedPreferences("game",0).edit().putString("state",snapshot().toString()).apply();}catch(Exception ignored){}}
        boolean load(){try{String s=getContext().getSharedPreferences("game",0).getString("state",null);if(s==null)return false;restore(new JSONObject(s));return true;}catch(Exception e){return false;}}
        void checkpoint(){try{history.push(snapshot().toString());if(history.size()>100)history.removeLast();}catch(Exception ignored){}}
        void undo(){if(!history.isEmpty()){try{restore(new JSONObject(history.pop()));save();}catch(Exception ignored){}}}
        void finish(){moves++;boolean full=true;for(ArrayList<Card> a:foundation)if(a.size()!=13)full=false;if(full&&!won){won=true;post(()->new AlertDialog.Builder(getContext()).setTitle("Победа!").setMessage("Все 52 карты собраны. Ходов: "+moves).setPositiveButton("Новая игра",(d,x)->newGame()).setNegativeButton("Закрыть",null).show());}invalidate();save();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);w=getWidth();h=getHeight();canvas.drawColor(Color.rgb(16,104,72));gap=w*.017f;cw=(w-gap*8)/7f;ch=cw*1.43f;top=Math.max(78*getResources().getDisplayMetrics().density,h*.105f);tableY=top+ch+Math.max(30,w*.085f);step=Math.min(ch*.27f,Math.max(18,h*.032f));hiddenStep=step*.65f;
            p.setColor(Color.rgb(230,245,236));p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));p.setTextSize(Math.min(w*.057f,55));canvas.drawText("КОСЫНКА",gap*2,top*.42f,p);
            p.setTextSize(Math.min(w*.035f,35));p.setTypeface(Typeface.DEFAULT);p.setTextAlign(Paint.Align.RIGHT);canvas.drawText("Ходы: "+moves,w-gap*2,top*.42f,p);p.setTextAlign(Paint.Align.LEFT);
            slot(canvas,x(0),top,"↻");if(!stock.isEmpty())back(canvas,x(0),top);if(!waste.isEmpty())card(canvas,waste.get(waste.size()-1),x(1),top);
            for(int i=0;i<4;i++){slot(canvas,x(i+3),top,symbols[i]);if(!foundation[i].isEmpty())card(canvas,foundation[i].get(foundation[i].size()-1),x(i+3),top);}
            for(int i=0;i<7;i++){float y=tableY;slot(canvas,x(i),y,"");for(int j=0;j<tableau[i].size();j++){Card c=tableau[i].get(j);if(dragging&&source==i&&j>=sourceIndex)continue;if(c.up)card(canvas,c,x(i),y);else back(canvas,x(i),y);y+=c.up?step:hiddenStep;}}
            if(dragging&&source>=0){ArrayList<Card> a=tableau[source];float y=dragY-(downY-cardY(source,sourceIndex));for(int i=sourceIndex;i<a.size();i++){card(canvas,a.get(i),dragX-(downX-x(source)),y);y+=step;}}
            float by=h-Math.max(75*getResources().getDisplayMetrics().density,h*.10f);button(canvas,"НОВАЯ",0,by,w*.245f);button(canvas,"ОТМЕНА",w*.255f,by,w*.245f);button(canvas,"ПОДСКАЗКА",w*.51f,by,w*.31f);button(canvas,drawCount==1?"1 КАРТА":"3 КАРТЫ",w*.83f,by,w*.16f);
        }
        float x(int col){return gap+(cw+gap)*col;}
        float cardY(int col,int index){float y=tableY;for(int i=0;i<index;i++)y+=tableau[col].get(i).up?step:hiddenStep;return y;}
        void rounded(Canvas c,float left,float t,float right,float bottom,int color,float radius){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(left,t,right,bottom,radius,radius,p);}
        void slot(Canvas c,float xx,float yy,String mark){rounded(c,xx,yy,xx+cw,yy+ch,Color.rgb(13,86,60),cw*.1f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(88,153,123));c.drawRoundRect(xx,yy,xx+cw,yy+ch,cw*.1f,cw*.1f,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(cw*.43f);p.setColor(Color.rgb(110,169,141));c.drawText(mark,xx+cw/2,yy+ch*.63f,p);p.setTextAlign(Paint.Align.LEFT);}
        void back(Canvas c,float xx,float yy){rounded(c,xx,yy,xx+cw,yy+ch,Color.WHITE,cw*.1f);rounded(c,xx+cw*.06f,yy+cw*.06f,xx+cw*.94f,yy+ch-cw*.06f,Color.rgb(45,99,167),cw*.07f);p.setColor(Color.rgb(131,178,221));p.setStrokeWidth(2);for(int i=0;i<6;i++){float z=xx+cw*(.15f+i*.13f);c.drawLine(z,yy+cw*.17f,z,yy+ch-cw*.17f,p);}}
        void card(Canvas c,Card a,float xx,float yy){if(!a.up){back(c,xx,yy);return;}rounded(c,xx+1,yy+2,xx+cw+2,yy+ch+3,0x44000000,cw*.1f);rounded(c,xx,yy,xx+cw,yy+ch,Color.WHITE,cw*.1f);p.setColor(a.color());p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(cw*.245f);c.drawText(ranks[a.rank],xx+cw*.09f,yy+ch*.20f,p);p.setTextSize(cw*.24f);c.drawText(symbols[a.suit],xx+cw*.10f,yy+ch*.38f,p);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(cw*.51f);c.drawText(symbols[a.suit],xx+cw*.53f,yy+ch*.71f,p);p.setTextAlign(Paint.Align.LEFT);}
        void button(Canvas c,String label,float xx,float yy,float ww){rounded(c,xx+gap*.3f,yy,xx+ww-gap*.3f,yy+Math.min(h*.065f,60*getResources().getDisplayMetrics().density),Color.rgb(13,79,56),12);p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(10,Math.min(w*.028f,30)));c.drawText(label,xx+ww/2,yy+Math.min(h*.042f,39*getResources().getDisplayMetrics().density),p);p.setTextAlign(Paint.Align.LEFT);}
        boolean in(float xx,float yy,float cx,float cy){return xx>=cx&&xx<=cx+cw&&yy>=cy&&yy<=cy+ch;}
        int topSource(float xx,float yy){for(int i=0;i<7;i++)if(xx>=x(i)&&xx<=x(i)+cw&&yy>=tableY){for(int j=tableau[i].size()-1;j>=0;j--)if(yy>=cardY(i,j)&&yy<=cardY(i,j)+ch&&tableau[i].get(j).up)return i;}return -1;}
        int sourceAtIndex(int col,float yy){for(int j=tableau[col].size()-1;j>=0;j--)if(yy>=cardY(col,j)&&yy<=cardY(col,j)+ch&&tableau[col].get(j).up)return j;return -1;}
        boolean canFoundation(Card c,int col){ArrayList<Card> a=foundation[col];return c.suit==col&&(a.isEmpty()?c.rank==1:a.get(a.size()-1).rank+1==c.rank);}
        boolean canTableau(Card c,int col){ArrayList<Card> a=tableau[col];if(a.isEmpty())return c.rank==13;Card last=a.get(a.size()-1);return last.up&&last.rank==c.rank+1&&(last.color()!=c.color());}
        void reveal(int col){ArrayList<Card> a=tableau[col];if(!a.isEmpty())a.get(a.size()-1).up=true;}
        boolean moveTableau(int from,int idx,int to){if(from==to||idx<0)return false;ArrayList<Card> a=tableau[from];if(!canTableau(a.get(idx),to))return false;checkpoint();ArrayList<Card> moving=new ArrayList<>(a.subList(idx,a.size()));a.subList(idx,a.size()).clear();tableau[to].addAll(moving);reveal(from);finish();return true;}
        boolean moveToFoundation(int from,int idx,int to){ArrayList<Card> a=tableau[from];if(idx!=a.size()-1||idx<0||!canFoundation(a.get(idx),to))return false;checkpoint();foundation[to].add(a.remove(idx));reveal(from);finish();return true;}
        boolean wasteToFoundation(int to){if(waste.isEmpty()||!canFoundation(waste.get(waste.size()-1),to))return false;checkpoint();foundation[to].add(waste.remove(waste.size()-1));finish();return true;}
        boolean wasteToTableau(int to){if(waste.isEmpty()||!canTableau(waste.get(waste.size()-1),to))return false;checkpoint();tableau[to].add(waste.remove(waste.size()-1));finish();return true;}
        boolean foundationToTableau(int from,int to){ArrayList<Card>a=foundation[from];if(a.isEmpty()||!canTableau(a.get(a.size()-1),to))return false;checkpoint();tableau[to].add(a.remove(a.size()-1));finish();return true;}
        void drawStock(){if(stock.isEmpty()&&waste.isEmpty())return;checkpoint();if(stock.isEmpty()){for(int i=waste.size()-1;i>=0;i--){Card c=waste.get(i);c.up=false;stock.add(c);}waste.clear();}else for(int i=0;i<drawCount&&!stock.isEmpty();i++){Card c=stock.remove(stock.size()-1);c.up=true;waste.add(c);}finish();}
        void auto(int from,int idx){if(idx<0||idx!=tableau[from].size()-1)return;Card c=tableau[from].get(idx);for(int i=0;i<4;i++)if(canFoundation(c,i)){moveToFoundation(from,idx,i);return;}}
        void autoWaste(){if(waste.isEmpty())return;Card c=waste.get(waste.size()-1);for(int i=0;i<4;i++)if(canFoundation(c,i)){wasteToFoundation(i);return;}}
        void hint(){String msg="Пока нет доступных ходов. Попробуйте взять карту из колоды.";outer:for(int i=0;i<7;i++){ArrayList<Card>a=tableau[i];if(a.isEmpty())continue;for(int j=0;j<4;j++)if(canFoundation(a.get(a.size()-1),j)){msg="Переместите верхнюю карту из столбца "+(i+1)+" в домик.";break outer;}for(int k=0;k<7;k++)if(k!=i)for(int j=0;j<a.size();j++)if(a.get(j).up&&canTableau(a.get(j),k)){msg="Переместите карты из столбца "+(i+1)+" в столбец "+(k+1)+".";break outer;}}if(!waste.isEmpty()){Card c=waste.get(waste.size()-1);for(int j=0;j<4;j++)if(canFoundation(c,j))msg="Отправьте карту из сброса в домик.";}new AlertDialog.Builder(getContext()).setTitle("Подсказка").setMessage(msg).setPositiveButton("Понятно",null).show();}
        void tap(float xx,float yy){if(yy>h-Math.max(75*getResources().getDisplayMetrics().density,h*.10f)){if(xx<w*.25f)new AlertDialog.Builder(getContext()).setTitle("Новая игра?").setPositiveButton("Начать",(d,z)->newGame()).setNegativeButton("Отмена",null).show();else if(xx<w*.505f)undo();else if(xx<w*.825f)hint();else{drawCount=drawCount==1?3:1;save();invalidate();}return;}
            if(in(xx,yy,x(0),top)){drawStock();return;}if(in(xx,yy,x(1),top)){autoWaste();return;}for(int i=0;i<7;i++)if(xx>=x(i)&&xx<=x(i)+cw&&yy>=tableY){int idx=sourceAtIndex(i,yy);auto(i,idx);return;}}
        }
        void drop(float xx,float yy){if(source<0)return;for(int i=0;i<4;i++)if(in(xx,yy,x(i+3),top)){moveToFoundation(source,sourceIndex,i);return;}for(int i=0;i<7;i++)if(xx>=x(i)&&xx<=x(i)+cw&&yy>=tableY){moveTableau(source,sourceIndex,i);return;}}
        void dragWaste(float xx,float yy){if(waste.isEmpty())return;for(int i=0;i<4;i++)if(in(xx,yy,x(i+3),top)&&wasteToFoundation(i))return;for(int i=0;i<7;i++)if(xx>=x(i)&&xx<=x(i)+cw&&yy>=tableY&&wasteToTableau(i))return;}
        void dragFoundation(int from,float xx,float yy){for(int i=0;i<7;i++)if(xx>=x(i)&&xx<=x(i)+cw&&yy>=tableY&&foundationToTableau(from,i))return;}
        @Override public boolean onTouchEvent(MotionEvent e){float xx=e.getX(),yy=e.getY();switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=xx;downY=yy;dragX=xx;dragY=yy;source=topSource(xx,yy);sourceIndex=source<0?-1:sourceAtIndex(source,yy);dragging=false;return true;
            case MotionEvent.ACTION_MOVE:dragX=xx;dragY=yy;if(source>=0&&Math.hypot(xx-downX,yy-downY)>12)dragging=true;invalidate();return true;
            case MotionEvent.ACTION_UP:if(Math.hypot(xx-downX,yy-downY)>20){if(source>=0)drop(xx,yy);else if(in(downX,downY,x(1),top))dragWaste(xx,yy);else for(int i=0;i<4;i++)if(in(downX,downY,x(i+3),top)){dragFoundation(i,xx,yy);break;}}else tap(xx,yy);dragging=false;source=-1;invalidate();return true;
            case MotionEvent.ACTION_CANCEL:dragging=false;source=-1;invalidate();return true;
        }return true;}
    }
}
