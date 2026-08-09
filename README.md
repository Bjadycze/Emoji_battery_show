# Emoji baterie

Ukazuje stav baterie jako emoji –
ve stavovém řádku, v plovoucí bublině a ve widgetu na ploše.

**Zdarma, bez reklam, bez sledování.** Aplikace nemá oprávnění `INTERNET`, takže reklamu ani
analytiku nemůže načíst, i kdyby chtěla. Licence MIT.

---

## Co umí

- kreslená baterie s plynulou barvou a výplní podle procent (jemné rozlišení na celé škále)
- 9 sad emoji (autíčka od koloběžky po formuli, měsíční fáze, nálady, srdíčka, počasí, rostlina, jídlo, kočky, semafor)
- zvláštní emoji během nabíjení
- čtyři režimy zobrazení: vypnuto, jen stavová lišta, lišta i bublina, jen bublina
- trvalé oznámení s ikonou ve stavovém řádku (emoji silueta nebo číslo procent)
- plovoucí bublina s plnobarevným emoji: přetažení prstem, nastavitelná velikost i průhlednost pozadí (25–100 %)
- widget na plochu s plnobarevným emoji a procenty
- automatický start po restartu telefonu
- sekce „O aplikaci“ s odkazem na repozitář (adresu nastavíte v `MainActivity.kt`, konstanta `PROJECT_URL`)
- živý náhled: posuvníkem si projdete, jak sada vypadá na všech úrovních nabití

## Co Android neumožňuje (a jak to obchází i originál)

Systémovou ikonu baterie ve stavovém řádku **nelze nahradit** bez rootu nebo systémových
oprávnění. Žádná aplikace z Obchodu Play to nedělá. Místo toho se používá trvalé oznámení,
jehož malá ikona se v řádku zobrazí vedle té systémové.

Malé ikony oznámení navíc systém kreslí jen z alfa kanálu, takže barevné emoji se změní na
jednobarevnou siluetu. Proto aplikace nabízí:

| Kde | Barvy | Poznámka |
|---|---|---|
| stavový řádek | ne (silueta nebo číslo) | limit Androidu |
| rozbalené oznámení | ano | emoji je v titulku |
| plovoucí bublina | ano | vyžaduje oprávnění „zobrazení přes jiné aplikace“ |
| widget | ano | doporučené řešení, nespotřebovává službu |

U tvarově výrazných sad (měsíční fáze, semafor, srdíčka) vypadá silueta dobře. U složitých
emoji je čitelnější volba „Ukázat procenta místo emoji“.

## Sestavení

Potřebujete Android Studio (Ladybug nebo novější) nebo JDK 17 + Android SDK 35.

```bash
# v Android Studiu: File → Open → složka EmojiBattery, pak Run
# nebo z příkazové řádky:
./gradlew assembleDebug
# výsledek: app/build/outputs/apk/debug/app-debug.apk
```

Repozitář neobsahuje `gradle-wrapper.jar` (binární soubor). Android Studio si ho doplní samo,
případně stačí jednou spustit `gradle wrapper`.

Podepsané vydání:

```bash
./gradlew assembleRelease
```

## Struktura

```
app/src/main/java/com/emojibattery/free/
├─ MainActivity.kt           nastavení v Jetpack Compose
├─ BatteryMonitorService.kt  foreground služba: oznámení, bublina, obnova widgetu
├─ BatteryWidgetProvider.kt  widget na plochu
├─ BootReceiver.kt           start po restartu
├─ EmojiSets.kt              definice sad emoji
├─ EmojiRenderer.kt          vykreslení emoji/čísla do bitmapy
├─ BatteryInfo.kt            čtení stavu baterie
└─ Prefs.kt                  nastavení (SharedPreferences)
```

## Oprávnění a proč jsou potřeba

- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` – běžící služba, která sleduje
  `ACTION_BATTERY_CHANGED` (tento broadcast nelze registrovat v manifestu)
- `POST_NOTIFICATIONS` – oznámení je nositelem ikony (Android 13+)
- `SYSTEM_ALERT_WINDOW` – plovoucí bublina, volitelné
- `RECEIVE_BOOT_COMPLETED` – obnovení ikony po restartu

## Než to pošlete na Google Play

Typ služby `specialUse` vyžaduje při odesílání zdůvodnění v Play Console. Uveďte, že jde
o trvalý indikátor stavu baterie, který uživatel sám zapíná a kdykoli vypne. Alternativou
bez služby je pouze widget – ten na review nic nepotřebuje.

## Nápady na rozšíření

- vlastní emoji zadané uživatelem pro každý interval
- vlastní hranice intervalů místo rovnoměrného dělení
- odhad zbývajícího času a teplota baterie v oznámení
- upozornění při dosažení zvolené úrovně nabití
- Wear OS ciferník


## Build

Postup pro sestavení debug verze aplikace pomocí Gradle Wrapperu:

1. Ujistěte se, že máte nainstalované JDK 17 a Android SDK (compileSdk 36).
2. Naklonujte repozitář a přejděte do jeho kořenového adresáře.
3. Na Linuxu/macOS udělte souboru `gradlew` práva ke spuštění (pokud ještě nemá):

   ```bash
   chmod +x gradlew
   ```

4. Spusťte sestavení debug varianty:

   ```bash
   ./gradlew assembleDebug
   ```

   Na Windows použijte `gradlew.bat assembleDebug`.

5. Po úspěšném sestavení najdete výsledný APK soubor v adresáři:

   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
