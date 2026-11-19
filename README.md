# Косынка для Android

Нативная игра Klondike на Java, без сторонних библиотек, рекламы и разрешений.

## Возможности
- 52 карты, 7 игровых столбцов, 4 домика, колода и сброс
- Раздача по 1 или 3 карты, неограниченный переворот сброса
- Перетаскивание последовательностей и одиночных карт
- Касание верхней карты столбца/сброса — автоматический перенос в домик, когда возможно
- Отмена до 100 ходов, подсказки, новая игра, сохранение состояния
- Вертикальная ориентация

## Сборка APK
1. Установите Android Studio и откройте папку `KlondikeAndroid`.
2. Дождитесь синхронизации Gradle (потребуется интернет для загрузки Android Gradle Plugin и SDK).
3. В меню выберите Build → Build Bundle(s) / APK(s) → Build APK(s).
4. APK появится по пути `app/build/outputs/apk/debug/app-debug.apk`.

Либо в терминале с установленным Gradle и Android SDK: `gradle assembleDebug`.

## Управление
- Коснитесь закрытой колоды, чтобы открыть 1 или 3 карты.
- Перетащите карту или последовательность на подходящий столбец.
- Перетащите верхнюю карту в домик или просто коснитесь её для автопереноса.
- Карты из домика можно вернуть в столбец перетаскиванием.
- «Отмена» возвращает предыдущее состояние.

## Ограничения версии 1.0
- Подсказки простые, без поиска оптимального решения.
- Три карты в сбросе представлены верхней доступной картой.
- Не включены звук и анимации перемещения.

## GitHub Actions: APK without Android Studio
Upload the **contents** of this folder to a GitHub repository (root level), on the `main` branch. Go to Actions → Build Android APK → Run workflow. Once green, open the run and download the `Klondike-debug-APK` artifact. Extract the zip to get `app-debug.apk`, then install it on your Android phone. The APK is a debug build, not a Play Store release.
