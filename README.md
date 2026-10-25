<p align="center">
  <img src="assets/logo.png" width="110" height="110" alt="Skyeng Avatar Logo" style="border-radius: 26px;">
</p>

<h1 align="center">Skyeng Avatar</h1>

<p align="center">
  <b>Современный, быстрый и нативный Android-клиент кабинета студента Skypro (<a href="https://avatar.skyeng.ru">avatar.skyeng.ru</a>).</b><br>
  Полноценный учебный кабинет на Jetpack Compose: расписание, БРС, умный тренажёр с LaTeX и AI-ассистентом.
</p>

<p align="center">
  <a href="https://github.com/factorydeath/skyeng-avatar/releases/latest">
    <img src="https://img.shields.io/badge/Скачать_APK-1.0.5-6750A4?style=for-the-badge&logo=android&logoColor=white" height="38" alt="Скачать APK">
  </a>
  &nbsp;
  <a href="https://avatar.skyeng.ru">
    <img src="https://img.shields.io/badge/Кабинет_Skypro-0066FF?style=for-the-badge&logo=google-chrome&logoColor=white" height="38" alt="Кабинет Skypro">
  </a>
</p>

<p align="center">
  <a href="#о-проекте">О проекте</a> &nbsp;·&nbsp;
  <a href="#ключевые-возможности">Возможности</a> &nbsp;·&nbsp;
  <a href="#технологический-стек">Стек</a> &nbsp;·&nbsp;
  <a href="#скачать">Скачать</a> &nbsp;·&nbsp;
  <a href="#сборка-из-исходников">Сборка</a> &nbsp;·&nbsp;
  <a href="#канал-обновлений">OTA-обновления</a> &nbsp;·&nbsp;
  <a href="#архитектура">Архитектура</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0+_(API_26+)-3DDC84?style=flat-square&logo=android&logoColor=white&labelColor=16191C" alt="Android Version">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white&labelColor=16191C" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white&labelColor=16191C" alt="Material 3">
  <a href="https://github.com/factorydeath/skyeng-avatar/releases"><img src="https://img.shields.io/github/v/release/factorydeath/skyeng-avatar?style=flat-square&color=D0BCFF&labelColor=16191C&label=Версия" alt="Release"></a>
  <img src="https://img.shields.io/badge/Размер_APK-~14_МБ-blue?style=flat-square&labelColor=16191C" alt="APK Size">
  <a href="LICENSE"><img src="https://img.shields.io/github/license/factorydeath/skyeng-avatar?style=flat-square&color=8EEDD8&labelColor=16191C" alt="License"></a>
</p>

> [!NOTE]
> **Дисклеймер:** Проект является независимым неофициальным клиентом, разработанным в личных и образовательных целях. Приложение не связано с компаниями Skyeng и Skypro.

---

## О проекте

**Skyeng Avatar** — это нативное приложение для операционной системы Android, заменяющее необходимость открывать веб-версию личного кабинета студента через браузер. Приложение адаптировано под управление одной рукой, быстро запускается, поддерживает работу в фоне и бережно расходует батарею и трафик.

Интерфейс построен по принципам **Material Design 3 (Material You)** с плавной анимацией и поддержкой динамических темных и светлых тем оформления.

---

## Ключевые возможности

### 📱 Дашборд «Мой день»
- **Главный экран студента**: аккуратная сводка текущего прогресса, дедлайны по домашним заданиям и долги.
- **Индикатор БРС (Балльно-рейтинговая система)**: точный расчет баллов (*набрано / доступно*), динамическая цветная шкала прогресса и подробная расшифровка по предметам.
- **Мотивация**: ежедневная фраза дня и быстрый переход к ближайшему занятию.

### 📅 Интерактивное расписание
- Просмотр занятий по неделям с удобным переключателем дат.
- Полная детализация каждого слота: лекции, воркшопы, Q&A-сессии, статус присутствия и ссылки на трансляцию.
- Учет учебных часовых поясов платформы.

### 🎓 Учебный трек и предметы
- Список предметов и модулей вашей профессии с визуализацией прогресса.
- Темы занятий, ссылки на методические материалы, домашние задания и скачивание документов.
- Раздел профиля с градиентной сводкой по ключевым показателям обучения.

### 🧠 Тренажёр с формулами LaTeX и AI
- **Нативный рендер формул**: формулы и математические выражения форматируются через библиотеку LaTeX (JLatexMath) без задержек вебвью.
- **Поддержка графики**: оптимизированная загрузка и зум графических материалов задач.
- **AI-ассистент**: встроенный помощник для подсказок логики решений и анализа ошибок.

### 🎨 Дизайн и персонализация
- **Шесть цветовых схем**: фиолетовая, синяя, изумрудная, янтарная и другие фирменные палитры.
- **Глубокая тёмная тема**: оптимизирована для AMOLED-экранов.
- Плавные Material 3 переходы и жестовая навигация.

### 🔒 Безопасность и конфиденциальность
- Вход через официальный сервис `id.skyeng.ru` (SMS с одноразовым кодом OTP или пароль).
- Сессия и токены сохраняются в зашифрованном виде с использованием аппаратного **Android Keystore** / `EncryptedSharedPreferences`.
- Данные авторизации исключены из системных бэкапов и никогда не покидают ваше устройство.

### 🔄 Бесшовное самообновление (OTA)
- Автоматическая проверка свежих версий прямо внутри приложения.
- Безопасная загрузка APK с криптографической валидацией контрольных сумм и подписи сертификата перед установкой.

---

## Технологический стек

| Категория | Технологии |
| :--- | :--- |
| **Язык разработки** | Kotlin 2.0 |
| **Пользовательский интерфейс** | Jetpack Compose, Material 3, Navigation Compose |
| **Сетевой уровень** | Ktor Client 3 (CIO / OkHttp), WebSockets, kotlinx.serialization |
| **Хранение данных** | Jetpack DataStore Preferences, EncryptedSharedPreferences (Android Keystore) |
| **Рендеринг формул** | JLatexMath (нативный рендер математики) |
| **Архитектура** | Clean Architecture + MVVM + Unidirectional Data Flow (StateFlow / Coroutines) |
| **Требования к системе** | Android 8.0+ (API 26+) · Рекомендуется Android 12–14 (API 31–34) |

---

## Скачать

Готовые релизные сборки APK доступны на странице [**Releases**](https://github.com/factorydeath/skyeng-avatar/releases/latest):

1. Перейдите в [**Последний релиз**](https://github.com/factorydeath/skyeng-avatar/releases/latest).
2. Скачайте файл **`skyeng-avatar.apk`** (~14 МБ).
3. При первом открытии разрешите установку приложений из вашего браузера/проводника.
4. Войдите по своему номеру телефона через SMS-код.

---

## Сборка из исходников

Для сборки проекта на локальном компьютере необходимы **JDK 17** и **Android SDK (API 34)**:

```bash
# Клонирование репозитория
git clone https://github.com/factorydeath/skyeng-avatar.git
cd skyeng-avatar

# Отладочная сборка (Debug APK)
./gradlew assembleDebug

# Запуск unit-тестов
./gradlew testDebugUnitTest

# Релизная сборка (требуется release.keystore)
./gradlew assembleRelease
```

Собранный Debug APK появится по пути: `app/build/outputs/apk/debug/app-debug.apk`.

---

## Канал обновлений

Приложение содержит механизм автоматических обновлений:
1. При старте сверяется версия с файлом `version.txt` в корне репозитория.
2. При наличии новой версии пользователю показывается диалоговое окно обновления с описанием.
3. APK скачивается во временный каталог, валидируется пакет `ru.skypro.avatar.student`, номер сборки и SHA-256 подписи приложения.
4. Вызывается системный PackageInstaller.

---

## Архитектура

Проект организован по принципам Clean Architecture с разделением на независимые слои:

```
app/src/main/java/ru/skypro/avatar/student/
├── core/
│   ├── network/      # Ktor HTTP клиент, интерцепторы авторизации, cookies
│   ├── session/      # Шифрованное хранилище сессии и токенов (Keystore)
│   ├── update/       # OTA апдейтер, проверка подписей и версий
│   └── util/         # Форматирование времени, часовые пояса, ссылки
├── data/
│   ├── repository/   # Репозитории: расписание, предметы, тренажёр, документы, БРС
│   ├── remote/       # DTO сетевых запросов и WebSocket клиент
│   └── local/        # Локальный кэш и настройки пользователя (DataStore)
├── domain/
│   └── model/        # Чистые сущности предметной области (Lessons, Subjects, Tasks)
└── ui/
    ├── navigation/   # Граф навигации и экраны нижнего бара
    ├── screens/      # Экраны Compose (DayScreen, ScheduleScreen, TrainerScreen)
    ├── components/   # Переиспользуемые UI компоненты (BrsBlocks, LatexView, Slots)
    └── theme/        # Material 3 палитры, шрифты, формы и анимации
```

---

## Лицензия

Проект распространяется под открытой лицензией [MIT License](LICENSE).
