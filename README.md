# Smart Planogram (spmob)

Мобильное приложение для мерчандайзеров: поиск товара, сканер штрихкода, отделы и **вертикальная планограмма** стеллажа. Каталог работает **офлайн** (Room / SQLite); при сети — проверка обновлений из облачного бандла.

| | |
|---|---|
| **Package** | `by.smartplanogram.spmob` |
| **Version** | **1.1.0** (`versionCode` 2) |
| **Min / Target SDK** | 24 / 37 |
| **UI** | Jetpack Compose · Material 3 |
| **Data** | Room · SharedPreferences |
| **Native** | C++ / JNI (`plna-core`) — карта + сокет (задел) |

---

## Возможности (v1.1.0)

- **Главная** — отделы / поиск, сканер, статус синка с временем обновления, недавние стеллажи
- **Отделы** — компактный список с иконками → планограмма
- **Планограмма** — edge-to-edge полки, sticky header, тап по товару → детали, scroll к целевой полке + haptic
- **Поиск** — имя / штрихкод, фильтры отдела и типа места, иконки товара (`iconHint` + keywords)
- **Сканер** — CameraX + ML Kit → полка; демо-скан без камеры
- **Профиль** — язык (RU / BE / EN), тема, выход
- **Онбординг** — короткий тур при первом запуске
- **Синхронизация** — offline-first; онлайн сравнивает версию бандла и обновляет Room
- **Системная «Назад»** — закрывает вложенные экраны, не сразу всё приложение

---

## Стек

- Kotlin · AGP 9 · Compose BOM · KSP · Room
- CameraX · ML Kit Barcode
- AppCompat locales · Material Icons Extended
- CMake / NDK: субмодуль `app/src/main/cpp` → [plna-core-cpp](https://github.com/devopstem/plna-core-cpp)

---

## Быстрый старт

```bash
git clone --recurse-submodules <repo-url>
cd plnaemployees   # или имя клона
./gradlew :app:assembleDebug
```

Открыть в **Android Studio** (Ladybug / newer), Sync Gradle, Run на устройстве/эмуляторе.

### Субмодуль C++

```bash
git submodule update --init --recursive
```

Без субмодуля native-сборка не соберётся.

### Облачный endpoint (опционально)

Файл `app/src/main/assets/demo/cloud_endpoint.json`:

```json
{ "cloudUrl": "https://example.com/catalog.json", "note": "…" }
```

Пустой `cloudUrl` → используется пакетный `plna_cloud_bundle.json`.

---

## Структура

```
app/src/main/
├── java/by/smartplanogram/spmob/
│   ├── MainActivity.kt          # Splash → shell
│   ├── SpmobApp.kt              # DI-lite, seed, sync, native init
│   ├── data/                    # Room, seed, sync, prefs, repos
│   └── ui/                      # home, departments, rack, search,
│                                # scanner, profile, onboarding, splash
├── java/by/smartplanogram/plnanote/   # JNI bridges (map + network)
├── cpp/                         # git submodule plna-core
├── assets/demo/                 # локальный + cloud JSON бандлы
└── res/                         # темы, строки RU/EN/BE, иконка
```

Подробнее: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Локализация

Ресурсы: `values` (RU), `values-en`, `values-be`. Язык задаётся в профиле через `AppCompatDelegate.setApplicationLocales`.

---

## Сборка release

```bash
./gradlew :app:assembleRelease
```

Подпись — через стандартный `signingConfigs` (добавить перед публикацией в Store).

---

## Лицензия native-ядра

См. `app/src/main/cpp/LICENSE` (субмодуль).

---

## Changelog

См. [CHANGELOG.md](CHANGELOG.md).
