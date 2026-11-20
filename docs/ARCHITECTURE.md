# Architecture — Smart Planogram 1.1.0

## Принципы

1. **Offline-first** — UI всегда читает Room; сеть только обновляет локальную БД.
2. **Compose UI** — один `MainActivity` + `SpmobAppShell` (4 вкладки).
3. **Native optional** — C++ для карты/сокета; продукт 1.1 работает и без активного сокета (asset/HTTP sync).

## Потоки данных

```
assets/demo/*.json  ──seed/sync──►  Room (SQLite)
                                      │
HTTP cloudUrl (opt) ──sync────────────┤
Native PACKET_SYNC ──listener─────────┘
                                      ▼
                         CatalogRepository / DAOs
                                      ▼
                    ViewModels / Screens (Compose)
```

### Синхронизация

`CatalogSyncManager` + `NetworkMonitor`:

- нет сети → `SyncPhase.Offline`, работа из Room;
- есть сеть → сравнение `sync_meta.demo_seed_version` с `meta.version` remote-бандла;
- при различии → `DemoDataSeeder.applyBundleJson`.

Время последнего успешного синка: `AppSettings.lastSyncAt` (показ на главной).

## UI-модули

| Пакет | Назначение |
|---|---|
| `ui.home` | Дашборд, sync chip, сканер, recent |
| `ui.departments` | Список отделов → `RackCarouselScreen` |
| `ui.rack` | Планограмма, детали товара |
| `ui.search` | Поиск + фильтры |
| `ui.scanner` | Камера / демо → планограмма |
| `ui.profile` | Язык, тема, about, logout |
| `ui.onboarding` | Первый запуск |
| `ui.splash` | Loader до `seedReady` |
| `ui.product` | `productIcon()` / `ProductIconHint` |

## Планограмма

- `RackCarouselViewModel` грузит стеллажи отдела и раскладку полок из `product_locations`.
- `RackFaceCard` — вертикальный `LazyColumn` полок; `targetShelf` → `animateScrollToItem` + haptic.
- Горизонтальный pager — соседние стеллажи отдела.

## Иконки товара

Приоритет: `ProductEntity.iconHint` → keyword rules (name+category) → `Icons.Outlined.Inventory2`.

Токены hint: `water`, `soda`, `kvas`, `juice`, `milk`, … (`ProductIconHint`).

## Native (JNI)

| Bridge | Роль в 1.1 |
|---|---|
| `NativeMapEngine` | Загрузка JSON карты / findProduct / route — UI карты отключён, API сохранён |
| `NativeNetwork` | Сокет, heartbeat, `PACKET_SYNC_PLANOGRAM` → sync listener |

Инициализация: `SpmobApp.onCreate` → `NativeNetwork.nativeInit()`; shutdown в `onTerminate`.

## Безопасность / ресурсы

- Нет хардкода секретов; каталог демо в assets.
- Камера: permission + unbind/close на dispose.
- Корутины UI: `lifecycleScope` / `rememberCoroutineScope` / `viewModelScope`.
- HTTP: timeouts + `disconnect()` в `finally`.

## Версии артефактов данных

| Артефакт | Версия |
|---|---|
| Room DB | 4 |
| `plna_demo_bundle.json` | meta 4 |
| `plna_cloud_bundle.json` | meta 5 |
