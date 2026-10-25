# Xiaomi TV: кнопка OK пульта в окне сопряжения Bluetooth

[English below](#english)

Бесплатное приложение с открытым кодом. Исправляет известную ошибку телевизоров Xiaomi на Android TV: при подключении Bluetooth-наушников или колонки в окне «Запрос на подключение по Bluetooth» кнопка OK пульта не работает, и приходится подключать мышь, чтобы нажать «ОК».

![Окно «Запрос на подключение по Bluetooth»](docs/pairing-dialog.png)

**Скачать:** [bt-pairing-fix.apk](../../releases/latest/download/bt-pairing-fix.apk) (последняя версия, около 35 КБ).

## Установка с флешки — без компьютера и без режима разработчика

Понадобятся флешка и компьютер или телефон, чтобы скачать на неё файл. Всё остальное делается пультом.

1. **Скачайте** [bt-pairing-fix.apk](../../releases/latest/download/bt-pairing-fix.apk) и скопируйте его в корень флешки.
2. **Вставьте флешку** в USB-разъём телевизора.
3. **Откройте «Медиаплеер».** На главном экране это плитка MEDIA PLAYER, в списке приложений — «Проигрыватель».
4. Вкладка **«Устройства»** → ваша флешка → **bt-pairing-fix.apk** → кнопка OK.
5. В окне установки выберите **«Установить»**, затем **«Открыть»**.
   Если телевизор пишет, что установка из неизвестных источников заблокирована: выберите «Настройки», включите переключатель у «Проигрыватель», нажмите «Назад» и снова откройте файл.
6. Откроется приложение «Bluetooth: OK с пульта». Нажмите **«Открыть настройки»**.

   ![Экран приложения](docs/app-on-ru.png)

7. Откроются «Специальные возможности». В разделе **«Сервисы»** выберите **«Bluetooth: OK с пульта»** → **«Включить»** → в предупреждении **«ОК»**.
8. Нажмите **«Назад»**. Приложение покажет **«✓ Исправление включено»**. Готово, флешку можно вынуть.

Предупреждение на шаге 7 — стандартный текст Android для любой службы специальных возможностей: «может сохранять любой вводимый текст». Это приложение ничего не сохраняет, и у него нет доступа в интернет (см. «Безопасность»).

## Установка через компьютер — от режима разработчика до результата

Подходит, если флешки нет или «Медиаплеер» не открывает APK.

**На телевизоре:**

1. «Настройки» → «Настройки устройства» → «Об устройстве» → пункт **«Сборка»**: нажмите OK **7 раз подряд**, пока не появится «Вы стали разработчиком».
2. «Назад» → в «Настройках устройства» появится **«Для разработчиков»** → **«Отладка по USB»** → ВКЛ. На телевизорах Xiaomi этот же переключатель открывает отладку и по сети.
   Пункт «Factory Menu» в этом разделе — заводское меню, его не трогайте.
3. «Настройки» → «Сеть и Интернет» → ваше подключение: запишите **IP-адрес** телевизора, например `192.168.1.50`.

**На компьютере** (компьютер и телевизор в одной домашней сети):

4. Установите ADB:
   - Windows: скачайте [Platform Tools](https://developer.android.com/tools/releases/platform-tools), распакуйте, откройте командную строку в этой папке;
   - macOS: `brew install android-platform-tools`;
   - Linux: `sudo apt install adb`.
5. Подключитесь (подставьте свой IP):

   ```bash
   adb connect 192.168.1.50:5555
   ```

   На телевизоре появится «Разрешить отладку?»: отметьте «Всегда разрешать с этого компьютера» и нажмите «ОК». Если `adb` пишет `unauthorized`, повторите команду после подтверждения.
6. Установите приложение (файл должен лежать в текущей папке):

   ```bash
   adb install bt-pairing-fix.apk
   ```

7. Включите исправление. Либо с пульта, как на шагах 6–8 установки с флешки, либо командой. Она добавляет службу к уже включённым и ничего не ломает:

   ```bash
   adb shell 'svc=ru.chepelenko.btpairok/.PairOkService; cur=$(settings get secure enabled_accessibility_services); case "$cur" in *"$svc"*) new="$cur";; null|"") new="$svc";; *) new="$cur:$svc";; esac; settings put secure enabled_accessibility_services "$new"; settings put secure accessibility_enabled 1'
   ```

8. По желанию после установки выключите «Отладку по USB» обратно: для работы исправления она не нужна.

## Проверка

«Настройки» → «Пульты и аксессуары» → «Добавить аксессуар» → переведите наушники в режим сопряжения и выберите их. В окне «Запрос на подключение по Bluetooth» нажмите **OK на пульте**: наушники подключатся. «Назад» в этом окне отменяет подключение.

## В чём ошибка

Пункт «ОК» на экране выглядит выделенным, но фокуса на нём нет. Фокус стоит на заголовке окна, а кнопки «ОК» и «Отмена» сделаны так, что фокус на них не переходит никогда. Нажатие OK уходит в заголовок, и система его отбрасывает:

```
WindowManager: Unhandled key: win=...BluetoothPairingDialog, keyCode=23 (DPAD_CENTER)
```

Стрелки фокус тоже не двигают. Через 30 секунд окно закрывается и отменяет сопряжение. Мышь работает, потому что нажимает кнопку напрямую. Пульт тут ни при чём: нажатия до телевизора доходят. Ошибка в окне `com.android.tv.settings/.accessories.BluetoothPairingDialog` прошивки Xiaomi.

## Как работает исправление

Служба специальных возможностей ([PairOkService.java](src/ru/chepelenko/btpairok/PairOkService.java), около 140 строк):

- пока на экране окно запроса на подключение Bluetooth, кнопка OK пульта нажимает в нём «ОК», как это сделала бы мышь;
- на всех остальных экранах служба ничего не делает и нажатия не трогает;
- надпись кнопки служба берёт из самих настроек телевизора, поэтому язык меню не важен.

Включить себя приложение не может: Android это запрещает, чтобы вредоносные программы не включали себе такие службы тайком. Поэтому экран приложения ([MainActivity.java](src/ru/chepelenko/btpairok/MainActivity.java)) только показывает состояние и открывает нужную страницу настроек. Стандартная ссылка на «Специальные возможности» на телевизорах Xiaomi ведёт в заглушку «не поддерживается», поэтому приложение открывает страницу так же, как это делают сами настройки Xiaomi.

## Совместимость

| | |
|---|---|
| **Проверено** | Xiaomi Mi TV P1 55″ (MiTV-MOOQ0), Android TV 10, сборка QTG3.200305.006.4716; русский и английский язык меню |
| **Скорее всего, подойдёт** | другие Xiaomi на Android TV 9–11 с тем же окном: Mi TV P1, P1E, 4S, 4A, Q1E и похожие |
| **Скорее всего, не нужно** | модели на Google TV (Xiaomi TV A, A Pro, Q2 и новее): там другое меню сопряжения |
| **Не подойдёт** | телевизоры не на Android, Android ниже 7.0 |

Установка через «Медиаплеер» собрана по его коду и разрешениям на проверенном телевизоре; с настоящей флешкой автор её ещё не проходил. Если у вас другая модель или что-то пошло не так, напишите в [Issues](../../issues): модель и версию Android из «Настройки → Об устройстве».

**Как убедиться, что у вас та же ошибка:** пульт работает везде, кроме окна «Запрос на подключение», а мышь в этом окне срабатывает. Если в этот момент пульт перестаёт отвечать совсем, причина другая. Точная проверка через ADB: откройте окно сопряжения, нажмите OK и выполните `adb logcat -d | grep "Unhandled key.*BluetoothPairingDialog"` — строки с `keyCode=23` означают именно эту ошибку.

## Если исправление перестало работать

Android выключает службу, если приложение принудительно остановить: «Остановить» в настройках приложения или очистка памяти программами-«ускорителями». Откройте приложение «Bluetooth: OK с пульта»: если служба выключена, оно покажет кнопку «Открыть настройки» — включите её снова (шаги 6–8).

На Android 12 и новее переключатель службы может быть серым, потому что приложение не из Google Play. Тогда: «Настройки» → «Приложения» → «Bluetooth: OK с пульта» → ⋮ → «Разрешить ограниченные настройки», затем включите службу.

## Удаление

«Настройки» → «Приложения» → «Bluetooth: OK с пульта» → «Удалить». Или `adb uninstall ru.chepelenko.btpairok`.

## Безопасность и приватность

Службе специальных возможностей система разрешает видеть содержимое экрана и перехватывать нажатия кнопок. Поэтому:

- у приложения **нет доступа в интернет**: разрешения `INTERNET` в [манифесте](AndroidManifest.xml) нет, передать что-либо наружу оно не может;
- оно ничего не сохраняет и не собирает;
- код короткий, его можно прочитать целиком;
- APK можно собрать самому из исходников и не доверять готовому файлу. Контрольные суммы каждого выпуска указаны на странице [Releases](../../releases).

## Сборка из исходников

Нужны JDK 11+ и Android SDK с пакетами `platforms;android-29` и любыми `build-tools`. Gradle не нужен.

```bash
brew install openjdk && brew install --cask android-commandlinetools
```

```bash
sdkmanager --sdk_root=/opt/homebrew/share/android-commandlinetools "platforms;android-29" "build-tools;34.0.0"
```

```bash
./build.sh
```

Готовый файл — `bt-pairing-fix.apk`. При первой сборке скрипт создаёт свой ключ подписи `debug.keystore`. Путь к SDK задаётся переменной `ANDROID_HOME`, ключ — `KEYSTORE`, `KS_PASS`, `KS_ALIAS`. Иконку и баннер рисует `python3 tools/make_art.py`.

Обновление ставится поверх старой версии, только если оба APK подписаны одним ключом. Если вы ставили APK из Releases, а потом собрали свой, сначала удалите старую версию.

## Лицензия

[MIT](LICENSE): можно свободно использовать, изменять и распространять.

---

<a id="english"></a>

# English

Free, open-source fix for a known Xiaomi Android TV bug: when pairing Bluetooth headphones or a speaker, the remote's OK button does nothing in the "Bluetooth pairing request" dialog, and people end up plugging in a mouse to click "Pair".

![The "Bluetooth pairing request" dialog](docs/pairing-dialog-en.png)

**Download:** [bt-pairing-fix.apk](../../releases/latest/download/bt-pairing-fix.apk) (latest version, about 35 KB).

## Install from a USB stick — no computer, no developer mode

You need a USB stick and a computer or phone to put the file on it. Everything else is done with the remote.

1. **Download** [bt-pairing-fix.apk](../../releases/latest/download/bt-pairing-fix.apk) and copy it to the root of the USB stick.
2. **Plug the stick** into the TV.
3. **Open "Media Player"** (the MEDIA PLAYER tile on the home screen).
4. **Devices** tab → your USB stick → **bt-pairing-fix.apk** → OK.
5. In the installer choose **Install**, then **Open**.
   If the TV says installing from unknown sources is blocked: choose Settings, turn on "Media Player", press BACK and open the file again.
6. The app "Bluetooth pairing: remote OK fix" opens. Press **Open settings**.

   ![App screen](docs/app-off-en.png)

7. Accessibility settings open. Under **Services** select **Bluetooth pairing: remote OK fix** → **Enable** → **OK** in the warning.
8. Press **BACK**. The app shows **"✓ The fix is on"**. Done, you can unplug the stick.

The warning in step 7 is Android's standard text for every accessibility service ("can collect all the text you type"). This app stores nothing and has no internet access (see Privacy).

## Install with a computer — from developer mode to the result

**On the TV:**

1. Settings → Device Preferences → About → **Build**: press OK **7 times** until "You are now a developer".
2. BACK → Device Preferences → **Developer options** → **USB debugging** → On. On Xiaomi TVs this also enables debugging over the network. Do not touch "Factory Menu" there.
3. Settings → Network & Internet → your connection: note the TV's **IP address**, e.g. `192.168.1.50`.

**On the computer** (same home network):

4. Install ADB: Windows — [Platform Tools](https://developer.android.com/tools/releases/platform-tools); macOS — `brew install android-platform-tools`; Linux — `sudo apt install adb`.
5. `adb connect 192.168.1.50:5555` (your IP). On the TV, tick "Always allow from this computer" and press OK.
6. `adb install bt-pairing-fix.apk`
7. Enable the service with the remote (steps 6–8 above) or with the one-liner from the Russian section, step 7 (it appends to services already enabled).
8. Optionally turn USB debugging off again; the fix does not need it.

## Check

Settings → Remotes & Accessories → Add accessory → put the headphones in pairing mode and select them. In the "Bluetooth pairing request" dialog press **OK on the remote**. BACK cancels pairing.

## The bug

"Pair" looks highlighted but is not focused. Focus sits on the dialog title, and the Pair/Cancel actions are not focusable at all, so the OK key is dropped (`WindowManager: Unhandled key: win=...BluetoothPairingDialog, keyCode=23`). The D-pad cannot move focus either; after 30 seconds the dialog times out and cancels pairing. A mouse works because it clicks the button directly.

## The fix

A tiny accessibility service ([PairOkService.java](src/ru/chepelenko/btpairok/PairOkService.java), ~140 lines). While `com.android.tv.settings/.accessories.BluetoothPairingDialog` is in front, the remote's OK clicks the dialog's "Pair" action; on every other screen it does nothing. The label is read from the TV's own Settings, so any menu language works.

An app cannot enable its own accessibility service, so the app screen ([MainActivity.java](src/ru/chepelenko/btpairok/MainActivity.java)) shows the status and opens the right settings page. On Xiaomi TVs the standard Accessibility intent is a "not supported" stub, so the app opens the page the way Xiaomi's own Settings does.

**Tested:** Xiaomi Mi TV P1 55" (MiTV-MOOQ0), Android TV 10, Russian and English menus. **Likely works:** other Xiaomi Android TV 9–11 models with the same dialog (P1, P1E, 4S, 4A, Q1E). **Likely not needed:** Google TV models. The USB stick path follows Media Player's code and permissions on the tested TV but has not yet been walked with a real stick. Reports are welcome in [Issues](../../issues).

## If the fix stops working

Android turns the service off when the app is force-stopped (Settings → Force stop, or "cleaner" apps). Open the app: if the service is off it shows "Open settings" — enable it again. On Android 12+ a greyed-out switch needs Settings → Apps → the app → ⋮ → "Allow restricted settings" first.

Uninstall: Settings → Apps → the app → Uninstall, or `adb uninstall ru.chepelenko.btpairok`.

## Privacy

**No internet permission**, nothing stored, nothing collected. The code is short enough to read in full, and you can build the APK yourself; release checksums are on the [Releases](../../releases) page.

## Build

JDK 11+ and Android SDK with `platforms;android-29` and any build-tools; no Gradle. Run `./build.sh`. Environment: `ANDROID_HOME`, `KEYSTORE`, `KS_PASS`, `KS_ALIAS`. `python3 tools/make_art.py` redraws the icon and banner. An update installs over an older copy only when both are signed with the same key.

## License

[MIT](LICENSE)
