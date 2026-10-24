# Skyeng Avatar

Неофициальный нативный Android-клиент кабинета студента Skypro (avatar.skyeng.ru).

> Проект не связан со Skyeng и Skypro, разрабатывается для личного пользования и распространяется как есть.

## Возможности

- Вход через id.skyeng.ru (OTP или пароль), сессия хранится в зашифрованном виде и не попадает в бэкапы
- «Мой день»: расписание, долги, баллы БРС, фраза дня
- Расписание по неделям, детали занятий и слотов
- Предметы и прогресс по профессии, документы, профиль
- Тренажёр: задачи с изображениями и формулами (LaTeX), AI-хелпер
- Шесть цветовых тем, тёмный режим
- Самообновление: проверка версии, загрузка и установка APK с проверкой подписи

## Технологии

Kotlin 2.0 · Jetpack Compose (Material 3) · Ktor 3 · kotlinx.serialization · DataStore · Navigation Compose

minSdk 26 · targetSdk 34 · JDK 17

## Скачать

Последняя сборка — на странице [Releases](https://github.com/factorydeath/skyeng-avatar/releases/latest): файл `skyeng-avatar.apk`.

## Сборка

```bash
./gradlew assembleDebug       # отладочная сборка
./gradlew assembleRelease     # релизная сборка
./gradlew testDebugUnitTest   # unit-тесты
```

Релизная сборка подписывается локальным ключом: положите `keystore.properties` и `release.keystore`
в корень проекта (в репозиторий они не входят).

## Канал обновлений

Установленное приложение проверяет обновления само: `version.txt` в корне репозитория хранит номер
последней версии, `skyeng-avatar.apk` — последнюю сборку. Приложение сравнивает версию, скачивает APK,
проверяет пакет, versionCode и подпись, затем предлагает установку. Тот же файл продублирован
в [Releases](https://github.com/factorydeath/skyeng-avatar/releases/latest).

## Структура

```
app/src/main/java/ru/skypro/avatar/student/
├── core/    # сеть (Ktor), сессия, обновления, время, логирование
├── data/    # репозитории: расписание, предметы, тренажёр, документы, AI-хелпер
├── domain/  # модели
└── ui/      # Compose-экраны, навигация, тема
```
