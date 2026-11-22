# Changelog

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/).  
Версии: `versionName` / `versionCode` в `app/build.gradle.kts`.  
Автор: **devopstem**.

## [1.2.0] — 2026-11-22

### Added
- Firebase Auth (email / employee id + пароль) и экран входа
- Встроенный локальный логин без облака (`EmbeddedLocalAuth`, обфускация в bytecode)
- Биометрия: отпечаток / Face ID для разблокировки сессии
- Live-синхронизация каталога из Firebase Realtime Database → Room
- Профили пользователей в RTDB `users/{uid}`
- Счётчик поиска «Найдено: X · в каталоге Y» (полный каталог 6 000+)
- `ProductTitleText` — единый ellipsis для длинных названий
- Иконки товаров по категории/названию во всех экранах (поиск, планограмма, детали)

### Changed
- Источник данных: Firebase RTDB вместо demo JSON-бандлов
- Поиск: лимит выдачи поднят (до 10 000), без искусственного «50»
- Сканер остаётся открытым для повторных сканов; re-arm после ошибок / onResume
- Версия приложения **1.2.0** / versionCode **3**
- «О приложении»: автор **devopstem**, описание 1.2.0

### Fixed
- Сканер блокировался после первого кадра
- Несовпадение иконок с названием/категорией (приоритет category → name)
- Обрезка длинных названий в поиске и карточке товара

### Removed
- Demo assets (`plna_demo_bundle`, cloud tip), DemoDataSeeder, демо-скан
- Зависимость UI от тестовых JSON-файлов

## [1.1.0] — 2026-11-20

### Added
- CI/CD (GitHub Actions): Detekt, unit tests, assembleDebug, SonarCloud, CodeQL, Dependabot
- Вертикальная планограмма, сканер, детали товара, offline sync, онбординг, splash
- Иконки товара (первая версия keyword rules)

### Changed
- Навигация: Главная, Отделы, Поиск, Профиль
- Room schema v4 (`iconHint`)

### Fixed
- Утечки корутин/камеры/HTTP; `seedReady` через StateFlow

## [1.0.0] — 2026-11

### Added
- Первый Compose-клиент: каталог Room, отделы, поиск, профиль
