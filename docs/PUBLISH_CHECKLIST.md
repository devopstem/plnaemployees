# Чеклист перед публикацией на GitHub

- [x] Версия приложения **1.1.0** / versionCode **2**
- [x] README.md, CHANGELOG.md, docs/ARCHITECTURE.md, docs/CI_CD.md
- [x] Строка «О приложении» обновлена до 1.1.0
- [x] `.gitignore` исключает `local.properties`, `build/`, `.idea/`
- [x] CI: Detekt + unit tests + assemble + SonarCloud + CodeQL + Dependabot
- [ ] `git submodule` доступен по URL в `.gitmodules`
- [ ] Push `main` + tag `v1.1.0`
- [ ] GitHub Secret **`SONAR_TOKEN`**
- [ ] SonarCloud: import repo, Quality Gate, disable Automatic Analysis
- [ ] Branch protection: require CI + SonarCloud + CodeQL
- [ ] (Опционально) GitHub Release с заметками из CHANGELOG
- [ ] Не коммитить: `local.properties`, keystore, токены

## Команды

```bash
git submodule update --init --recursive
git add -A
git status   # проверить, что local.properties нет в staging
git commit -m "Release 1.1.0: planogram, scanner, sync, docs"
git tag -a v1.1.0 -m "Smart Planogram 1.1.0"
git remote add origin git@github.com:<user>/<repo>.git   # если ещё нет
git push -u origin main --tags
```
