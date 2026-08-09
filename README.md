# Emoji baterie

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
