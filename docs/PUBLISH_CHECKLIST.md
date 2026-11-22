# Publish checklist — Smart Planogram 1.2.0

Author: **devopstem**

- [x] Version **1.2.0** / versionCode **3**
- [x] README.md, CHANGELOG.md обновлены
- [x] «О приложении» → 1.2.0 + автор devopstem
- [x] Firebase Auth + RTDB sync + biometric + local login
- [x] Поиск: полный каталог, ellipsis названий, иконки
- [x] Сканер: повторное сканирование
- [ ] `./gradlew :app:assembleDebug` / `assembleRelease` зелёный
- [ ] Push `main` + tag `v1.2.0`
- [ ] GitHub Release из CHANGELOG `1.2.0`

```bash
./gradlew :app:assembleDebug
git add -A
git commit -m "Release 1.2.0: Firebase auth, biometrics, catalog sync, scanner & icons"
git tag -a v1.2.0 -m "Smart Planogram 1.2.0 by devopstem"
git push origin HEAD
git push origin v1.2.0
```
