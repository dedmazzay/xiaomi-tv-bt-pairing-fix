# Xiaomi TV: кнопка OK пульта в окне сопряжения Bluetooth

[English below](#english)

Бесплатное приложение с открытым кодом. Исправляет известную ошибку телевизоров Xiaomi на Android TV: при подключении Bluetooth-наушников или колонки в окне «Запрос на подключение по Bluetooth» кнопка OK пульта не работает. Обычно приходится подключать мышь, чтобы нажать «ОК».

![Окно «Запрос на подключение по Bluetooth»](docs/pairing-dialog.png)

## В чём ошибка

Пункт «ОК» на экране выглядит выделенным, но фокуса на нём нет. Фокус стоит на заголовке окна, а кнопки «ОК» и «Отмена» сделаны так, что фокус на них не переходит никогда. Нажатие OK уходит в заголовок, и система его отбрасывает:

```
WindowManager: Unhandled key: win=...BluetoothPairingDialog, keyCode=23 (DPAD_CENTER)
```

Стрелки фокус тоже не двигают. Через 30 секунд окно закрывается и отменяет сопряжение. Мышь работает, потому что нажимает кнопку напрямую, минуя фокус.

Пульт тут ни при чём: нажатия до телевизора доходят. Ошибка в окне `com.android.tv.settings/.accessories.BluetoothPairingDialog` прошивки Xiaomi.

## Как работает исправление

Приложение — служба специальных возможностей (Accessibility Service), около 140 строк кода ([PairOkService.java](src/ru/chepelenko/btpairok/PairOkService.java)):

- пока на экране окно запроса на подключение Bluetooth, кнопка OK пульта нажимает в нём «ОК», как это сделала бы мышь;
- на всех остальных экранах служба ничего не делает и нажатия не трогает;
- надпись кнопки служба берёт из самих настроек телевизора, поэтому язык меню не важен.

Отменить сопряжение в этом окне можно кнопкой «Назад», она работает и без исправления.

## Совместимость

| | |
|---|---|
| **Проверено** | Xiaomi Mi TV P1 55″ (MiTV-MOOQ0), Android TV 10, сборка QTG3.200305.006.4716 |
| **Скорее всего, подойдёт** | другие Xiaomi на Android TV 9–11 с тем же окном: Mi TV P1, P1E, 4S, 4A, Q1E и похожие |
| **Скорее всего, не нужно** | модели на Google TV (Xiaomi TV A, A Pro, Q2 и новее): там другое меню сопряжения |
| **Не подойдёт** | телевизоры не на Android, Android ниже 7.0 |

Если у вас другая модель и исправление помогло или не помогло, напишите в [Issues](../../issues): модель и версию Android из «Настройки → Об устройстве».

### Как убедиться, что у вас та же ошибка

Признак: пульт работает везде, кроме окна «Запрос на подключение», а мышь в этом окне срабатывает. Если в этот момент пульт перестаёт отвечать совсем, причина другая, и это приложение не поможет.

Точная проверка через ADB: откройте окно сопряжения, нажмите OK и выполните

```bash
adb logcat -d | grep "Unhandled key.*BluetoothPairingDialog"
```

Если в выводе есть строки с `keyCode=23`, у вас именно эта ошибка.

## Установка

Скачайте `bt-pairing-fix.apk` со страницы [Releases](../../releases/latest).

### Способ 1. С компьютера через ADB

1. На телевизоре включите режим разработчика: «Настройки → Настройки устройства → Об устройстве → Сборка», нажать 7 раз.
2. В «Настройки устройства → Для разработчиков» включите «Отладка по USB» (на многих моделях она же работает по сети).
3. Узнайте IP-адрес телевизора: «Настройки → Сеть и Интернет».
4. На компьютере:

```bash
adb connect 192.168.1.50:5555
```

```bash
adb install bt-pairing-fix.apk
```

При первом подключении телевизор спросит, доверять ли компьютеру, — подтвердите.

5. Включите службу. Её можно включить с пульта (см. «Включение службы») или одной командой. Команда добавляет службу к уже включённым и ничего не ломает:

```bash
adb shell 'svc=ru.chepelenko.btpairok/.PairOkService; cur=$(settings get secure enabled_accessibility_services); case "$cur" in *"$svc"*) new="$cur";; null|"") new="$svc";; *) new="$cur:$svc";; esac; settings put secure enabled_accessibility_services "$new"; settings put secure accessibility_enabled 1'
```

### Способ 2. Без компьютера, с флешки

1. Скопируйте `bt-pairing-fix.apk` на флешку и вставьте её в телевизор.
2. Откройте файл в файловом менеджере (например, CX File Explorer из Google Play).
3. Разрешите файловому менеджеру установку из неизвестных источников, когда телевизор спросит.
4. Включите службу с пульта (см. ниже).

### Включение службы

«Настройки → Настройки устройства → Специальные возможности → Bluetooth: OK с пульта → Вкл.»

На Android 12 и новее система может пометить службу как «ограниченную настройку», потому что приложение не из Google Play. Тогда сначала: «Настройки → Приложения → Bluetooth: OK с пульта → ⋮ → Разрешить ограниченные настройки», потом снова включите службу.

## Удаление

«Настройки → Приложения → Bluetooth: OK с пульта → Удалить» или:

```bash
adb uninstall ru.chepelenko.btpairok
```

## Безопасность и приватность

Службе специальных возможностей система разрешает видеть содержимое экрана и перехватывать нажатия кнопок. Поэтому:

- у приложения **нет доступа в интернет**: разрешения `INTERNET` в манифесте нет, передать что-либо наружу оно не может;
- оно ничего не сохраняет и не собирает;
- код короткий, его можно прочитать целиком: [PairOkService.java](src/ru/chepelenko/btpairok/PairOkService.java), [AndroidManifest.xml](AndroidManifest.xml);
- APK можно собрать самому из исходников (см. ниже) и не доверять готовому файлу.

## Сборка из исходников

Нужны JDK 11+ и Android SDK с пакетами `platforms;android-29` и любыми `build-tools`. Gradle не нужен.

macOS (Homebrew):

```bash
brew install openjdk && brew install --cask android-commandlinetools
```

```bash
sdkmanager --sdk_root=/opt/homebrew/share/android-commandlinetools "platforms;android-29" "build-tools;34.0.0"
```

Сборка:

```bash
./build.sh
```

Готовый файл — `bt-pairing-fix.apk`. При первой сборке скрипт создаёт свой ключ подписи `debug.keystore`. Путь к SDK задаётся переменной `ANDROID_HOME`, ключ — `KEYSTORE`, `KS_PASS`, `KS_ALIAS`.

Обновление ставится поверх старой версии, только если оба APK подписаны одним ключом. Если вы ставили APK из Releases, а потом собрали свой, сначала удалите старую версию.

## Лицензия

[MIT](LICENSE): можно свободно использовать, изменять и распространять.

---

<a id="english"></a>

# English

Free, open-source fix for a known Xiaomi Android TV bug: when pairing Bluetooth headphones or a speaker, the remote's OK button does nothing in the "Bluetooth pairing request" dialog. People usually plug in a mouse to click "Pair".

## The bug

"Pair" looks highlighted but is not focused. Focus sits on the dialog title, and the "Pair"/"Cancel" actions are not focusable at all, so the OK key is dropped (`WindowManager: Unhandled key: win=...BluetoothPairingDialog, keyCode=23`). The D-pad cannot move focus either; after 30 seconds the dialog times out and cancels pairing. A mouse works because it clicks the button directly.

## The fix

A tiny accessibility service ([PairOkService.java](src/ru/chepelenko/btpairok/PairOkService.java), ~140 lines). While `com.android.tv.settings/.accessories.BluetoothPairingDialog` is in front, the remote's OK clicks the dialog's "Pair" action. On every other screen it does nothing. The button label is read from the TV's own Settings, so any menu language works. BACK still cancels pairing.

**Tested:** Xiaomi Mi TV P1 55" (MiTV-MOOQ0), Android TV 10. **Likely works:** other Xiaomi Android TV 9–11 models with the same dialog (P1, P1E, 4S, 4A, Q1E). **Likely not needed:** Google TV models. Reports for other models are welcome in [Issues](../../issues).

Check that you have this exact bug: open the pairing dialog, press OK, then run `adb logcat -d | grep "Unhandled key.*BluetoothPairingDialog"`. Lines with `keyCode=23` confirm it.

## Install

1. Download `bt-pairing-fix.apk` from [Releases](../../releases/latest).
2. Install it with `adb install bt-pairing-fix.apk`, or from a USB stick with a file manager.
3. Enable the service: Settings → Device Preferences → Accessibility → "Bluetooth pairing: remote OK fix" → On. Via ADB, use the one-liner from the Russian section above (it appends to the services already enabled).

On Android 12+ you may first need Settings → Apps → the app → ⋮ → "Allow restricted settings".

Uninstall: `adb uninstall ru.chepelenko.btpairok` or from Settings → Apps.

## Privacy

The app has **no internet permission**, stores nothing and collects nothing. The code is short enough to read in full, and you can build the APK yourself.

## Build

JDK 11+ and Android SDK with `platforms;android-29` and any build-tools; no Gradle. Run `./build.sh`. Environment: `ANDROID_HOME`, `KEYSTORE`, `KS_PASS`, `KS_ALIAS`. An update installs over an older copy only when both are signed with the same key.

## License

[MIT](LICENSE)
