# Smart Planogram (spmob)

Мобильный клиент для мерчандайзеров: поиск и сканер товаров, отделы, вертикальная планограмма стеллажа.  
Каталог: **Firebase Realtime Database → Room (SQLite)** с офлайн-режимом.

| | |
|---|---|
| **Author** | **devopstem** |
| **Package** | `by.smartplanogram.spmob` |
| **Version** | **1.2.0** (`versionCode` 3) |
| **Min / Target SDK** | 24 / 37 |
| **UI** | Jetpack Compose · Material 3 |
| **Data** | Firebase Auth + RTDB · Room · SharedPreferences |
| **Native** | C++ / JNI (`plna-core`) — карта + сокет (задел) |

---

## Возможности (v1.2.0)

- **Авторизация**
  - Firebase Auth (email / employee id + пароль)
  - Встроенный локальный вход без облака (обфусцированные credentials в коде)
  - Биометрия: отпечаток / Face ID для повторного открытия приложения
- **Firebase RTDB → Room** — полный каталог (~6 600+ SKU), зоны магазина, синтез стеллажей
- **Поиск** — имя / штрихкод, фильтры отдела и типа места, счётчик «найдено / в каталоге», длинные названия с ellipsis, иконки по категории/названию
- **Отделы + планограмма** — список зон → edge-to-edge полки, иконки товаров, детали по тапу
- **Сканер** — CameraX + ML Kit; повторное сканирование без перезахода; haptic
- **Профиль** — язык (RU / BE / EN), тема, биометрия, выход
- **Синхронизация** — offline-first; при сети обновление каталога из Firebase
- **Онбординг** и splash при старте

---

## Стек

- Kotlin · AGP 9 · Compose BOM · KSP · Room
- Firebase BOM (Auth, Realtime Database)
- androidx.biometric
- CameraX · ML Kit Barcode
- AppCompat locales · Material Icons Extended
- CMake / NDK: субмодуль `app/src/main/cpp` → [plna-core-cpp](https://github.com/devopstem/plna-core-cpp)

---

## Быстрый старт

```bash
git clone --recurse-submodules git@github.com:devopstem/plnaemployees.git
cd plnaemployees
./gradlew :app:assembleDebug
```

Открыть в **Android Studio**, Sync Gradle, Run.

### Субмодуль C++

```bash
git submodule update --init --recursive
```

### Firebase

- `app/google-services.json` — проект `spmob-120212`
- RTDB rules: чтение для `auth != null`
- После локального/Firebase входа каталог тянется в Room

Локальный bootstrap-логин (без облака): см. `EmbeddedLocalAuth` в коде (не в assets).

---

## Структура

```
app/src/main/
├── java/by/smartplanogram/spmob/
│   ├── MainActivity.kt          # Splash → login / biometric → shell
│   ├── SpmobApp.kt              # Room, Auth, sync, native init
│   ├── data/
│   │   ├── auth/                # Firebase + local + biometric
│   │   ├── firebase/            # RTDB import / client
│   │   ├── local/               # Room entities / DAOs
│   │   ├── sync/                # CatalogSyncManager
│   │   └── prefs/
│   └── ui/                      # home, departments, rack, search,
│                                # scanner, profile, auth, onboarding
├── java/by/smartplanogram/plnanote/   # JNI bridges
├── cpp/                         # git submodule plna-core
└── res/                         # темы, строки RU/EN/BE, иконка
```

Подробнее: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Локализация

`values` (RU), `values-en`, `values-be`. Язык — в профиле (`AppCompatDelegate.setApplicationLocales`).

---

## Сборка release

```bash
./gradlew :app:assembleRelease
```

Перед Store: `signingConfigs`, ProGuard/R8 по необходимости.

---

## Публикация (чеклист)

```bash
# 1) Проверка
./gradlew :app:assembleDebug detekt

# 2) Коммит релиза
git add -A
git commit -m "$(cat <<'EOF'
Release 1.2.0: Firebase auth, biometrics, catalog sync, scanner & icons

EOF
)"

# 3) Тег
git tag -a v1.2.0 -m "Smart Planogram 1.2.0 by devopstem"

# 4) Push
git push origin HEAD
git push origin v1.2.0
```

GitHub Release: заметки из [CHANGELOG.md](CHANGELOG.md) секции `1.2.0`.  
Полный чеклист: [docs/PUBLISH_CHECKLIST.md](docs/PUBLISH_CHECKLIST.md).

---

## CI/CD

GitHub Actions: Detekt · unit tests · assemble · SonarCloud · CodeQL · Dependabot.  
Инструкция: [docs/CI_CD.md](docs/CI_CD.md). Secret: `SONAR_TOKEN`.

---

## Changelog

См. [CHANGELOG.md](CHANGELOG.md).

---

## Author

**devopstem** · [github.com/devopstem](https://github.com/devopstem)
