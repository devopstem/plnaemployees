# Changelog

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/).  
Версии: `versionName` / `versionCode` в `app/build.gradle.kts`.

## [1.1.0] — 2026-11-20

### Added
- Вертикальная планограмма (edge-to-edge полки, sticky header, fade, scroll indicator)
- Сканер штрихкода (CameraX + ML Kit) и демо-скан
- Детали товара по тапу на полке (bottom sheet)
- Offline-first синхронизация каталога + время последнего обновления на главной
- Онбординг (отделы → полки → поиск)
- Haptic + scroll-to-target shelf
- Иконки товара: `iconHint` из Room/облака + keyword rules
- Splash / loader при seed Room
- Adaptive launcher icon (бренд: navy / purple / cyan)
- Системная кнопка «Назад» для вложенных экранов
- Недавние стеллажи и быстрый доступ на главной

### Changed
- Навигация: 4 вкладки — Главная, Отделы, Поиск, Профиль
- Улучшен контраст приглушённого текста
- Компактные карточки отделов с тематическими иконками
- Язык UI синхронизирован с выбором в профиле (RU / BE / EN)
- Room schema **v4** (`ProductEntity.iconHint`)
- Демо-бандл **v4**, cloud-бандл **v5**

### Fixed
- Утечки: fire-and-forget `CoroutineScope`, камера без `unbind`, HTTP без `disconnect`
- Native listener больше не сбрасывается в `MainActivity.onDestroy`
- Busy-wait `seedReady` заменён на `StateFlow`

### Native
- C++ ядро сохранено (карта + сокет); UI карты не входит в 1.1.0 — задел под маршрут/realtime

## [1.0.0] — 2026-11

### Added
- Первый Compose-клиент: каталог Room, отделы, поиск, профиль, демо-данные
