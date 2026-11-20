# CI/CD — Smart Planogram (middle+)

Строгий контур качества для GitHub + **SonarCloud** + **CodeQL** + **Detekt**.

## Что гоняет бот

| Workflow | Триггер | Проверки |
|---|---|---|
| `.github/workflows/ci.yml` | push/PR `main`/`develop` | Detekt → Unit tests → Assemble debug → **SonarCloud quality gate** |
| `.github/workflows/codeql.yml` | push/PR + weekly cron | GitHub CodeQL (java-kotlin, security-and-quality) |
| Dependabot | weekly | Gradle + GitHub Actions bumps |

CI **падает**, если:
- Detekt находит **новый** smell (baseline + `maxIssues: 0`)
- Unit-тесты красные
- Debug APK не собирается
- SonarCloud **Quality Gate** failed (`sonar.qualitygate.wait=true`)
- Нет секрета `SONAR_TOKEN` (job SonarCloud)

## Обязательные secrets / vars

### Secrets (Settings → Secrets and variables → Actions)

| Name | Где взять |
|---|---|
| `SONAR_TOKEN` | [SonarCloud](https://sonarcloud.io) → Account → Security → Generate token |

### Variables (optional overrides)

| Name | Default |
|---|---|
| `SONAR_PROJECT_KEY` | `devopstem_plnaemployees` |
| `SONAR_ORGANIZATION` | `devopstem` |

## Настройка SonarCloud (один раз)

1. Зайти на https://sonarcloud.io через GitHub.
2. **Import** репозиторий Smart Planogram / `plnaemployees`.
3. Analysis method: **GitHub Actions** (не Automatic Analysis — у нас свой Gradle scanner).
4. Скопировать `sonar.projectKey` / `sonar.organization` в GitHub Variables при необходимости.
5. Создать **Quality Gate** (рекомендуется для middle+):
   - Coverage on new code ≥ **70%** (постепенно; UI пока в exclusions)
   - Duplications on new code ≤ **3%**
   - Maintainability / Reliability / Security ratings on new code = **A**
   - 0 new blocker / high issues
6. В SonarCloud → Project → Administration → Quality Gate → назначить gate.
7. В GitHub → Settings → Branches → Branch protection for `main`:
   - Require status checks: `Detekt (static)`, `Unit tests`, `Assemble debug`, `SonarCloud`, `Analyze (Java/Kotlin)`
   - Require branches up to date
   - Do not allow bypass

## Локально перед PR

```bash
./gradlew detekt --no-configuration-cache
./gradlew :app:testDebugUnitTest --no-configuration-cache
./gradlew :app:assembleDebug --no-configuration-cache
```

Обновить baseline только осознанно (после рефакторинга старых smell):

```bash
./gradlew detektBaseline --no-configuration-cache
```

## Detekt

- Конфиг: `config/detekt/detekt.yml` (complexity, coroutines, potential-bugs, style)
- Baseline: `config/detekt/baseline.xml`
- Formatting (ktlint) пока **выключен** — включить в `formatting.active: true` после первого cleanup-PR

## Submodules

Все workflow делают `submodules: recursive` — без `plna-core` assemble падает.

## Что не класть в CI

- `local.properties`, keystore, `SONAR_TOKEN` в логах
- Release signing (отдельный workflow + environment `production` позже)
