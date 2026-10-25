# xiaomi-tv-bt-pairing-fix — правила для AI-агентов

Публичный репозиторий. Accessibility-служба для телевизоров Xiaomi на Android TV: в окне
`com.android.tv.settings/.accessories.BluetoothPairingDialog` кнопка OK пульта нажимает «ОК».
Описание ошибки и инструкция для людей — в [README.md](README.md).

## Сборка и выпуск

- Сборка без Gradle: `./build.sh` (aapt2 → javac → d8 → zipalign → apksigner).
- Релизный ключ подписи хранится **вне репозитория**:
  `~/.config/xiaomi-tv-bt-pairing-fix/release.keystore` на Маке владельца. Не коммитить:
  `*.keystore` в `.gitignore`.
- Выпуск: `KEYSTORE=~/.config/xiaomi-tv-bt-pairing-fix/release.keystore ./build.sh`, поднять
  `versionCode`/`versionName` в `AndroidManifest.xml`, затем
  `gh release create vX.Y.Z bt-pairing-fix.apk` с SHA-256 файла в описании.
- Потеря ключа = обновления не встанут поверх у тех, кто поставил APK из Releases.

## Проверено на

Xiaomi Mi TV P1 55″ (MiTV-MOOQ0), Android TV 10, сборка 4716 — 25.10.2026, живым пультом.
На телевизоре владельца рядом включена служба Button Mapper (`flar2.homebutton/a.i`) — список
служб дописывать, а не перезаписывать.

## Ловушки на этом телевизоре

- `com.android.tv.settings/.device.privacy.PrivacyActivity` открывает **сброс до заводских**.
- Модуль `com.xiaomi.android.tvsetup.partnercustomizer` обрабатывает кнопки пульта «Настройки» и
  «Источник» — не отключать.
- В zsh не писать `"$VAR:r..."` в строках: `:r` — модификатор zsh.
