# План реализации remoteclaude-android

Спека: `../specs/2026-08-13-android-client-spec.md`.

Окружение проверено: JDK 17 (Corretto 17.0.17), Gradle 9.5.1 в PATH, Android SDK в
`%LOCALAPPDATA%\Android\Sdk` (платформы 35/36/36.1, build-tools 34–37, без
cmdline-tools — доустановить компоненты нечем, поэтому compileSdk берём из уже
установленных). Сборка APK возможна.

## Шаг 1. Каркас сборки

- `settings.gradle.kts` (pluginManagement + dependencyResolutionManagement: google,
  mavenCentral), `gradle/libs.versions.toml`, корневой `build.gradle.kts`,
  `gradle.properties`, wrapper на Gradle 8.14.3.
- Модули `:protocol` (kotlin-jvm) и `:app` (com.android.application). `:app`
  подключается только при найденном SDK — без SDK JVM-тесты `:protocol` всё равно
  конфигурируются и проходят.
- Версии: AGP 8.13.2, Kotlin 2.4.10, compileSdk 36, minSdk 26, Compose BOM.
- Коммит: каркас Gradle.

## Шаг 2. Протокол: ссылка, сообщения, клавиши, реконнект (TDD)

- Тесты → реализация: `ConnectionLink`, `ProtocolJson` (ServerMessage/ChatEvent/
  ClientMessage), `TerminalKeys` (панель + bracketed paste), `ReconnectPolicy`.
- Коммит: разбор ссылки и протокольных сообщений.

## Шаг 3. Эмулятор терминала (TDD)

- `Utf8StreamDecoder` (склейка на границе кадров), `TerminalEmulator` (сетка, курсор,
  CSI/SGR/ED/EL/IL/DL/скролл-регион/alt-screen), снимок в стилевые прогоны.
- Тесты: печать и перенос, цвета и атрибуты, очистки, позиционирование, скроллбэк,
  разрезанные посреди кадра UTF-8 и CSI, alt-screen, resize.
- Коммит: эмулятор терминала.

## Шаг 4. Сеть

- `RemoteClaudeApi` (health, sessions) на OkHttp, `SessionSocket` (интерфейс +
  OkHttp-реализация), `SessionConnection` — реконнект-цикл поверх сокета, Flow событий.
- Тесты: `SessionConnection` на поддельной фабрике сокетов (переподключение, сброс
  паузы, стоп по exit/закрытию пользователем), `RemoteClaudeApi` на MockWebServer
  (заголовок токена, 401, разбор списка).
- Коммит: HTTP- и WebSocket-клиент.

## Шаг 5. Каркас приложения

- Manifest (cleartext, камера для QR), тема Material 3, `AppContainer`, DataStore
  `SettingsRepository`, навигация connect → sessions → session.
- Коммит: каркас Android-приложения.

## Шаг 6. Экраны

- Подключение: ссылка, скан QR, проверка health, сохранение.
- Список сессий: карточки, pull-to-refresh, ошибки.
- Сессия: вкладки, чат-лента с автопрокруткой, терминал со снимком эмулятора, панель
  клавиш, композер, плашки menuWaiting/соединения, «Под экран».
- Коммит: экраны подключения и списка; коммит: экран сессии.

## Шаг 7. Проверка и документация

- `./gradlew test` (JVM-юниты), `./gradlew :app:lintDebug` по возможности,
  `./gradlew assembleDebug` → путь к APK.
- README: требования, установка SDK при её отсутствии, команды сборки и установки,
  структура, что вне рамок.
- Коммит: README и итоги проверки.
