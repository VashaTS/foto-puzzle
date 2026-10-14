# Foto Puzzle

Gra puzzle na Androida (Kotlin, minSdk 24) – ułóż obrazek z własnego zdjęcia.

## Funkcje
- Zdjęcie z galerii (systemowy Photo Picker, bez uprawnień) lub z aparatu (TakePicture + FileProvider)
- Poziomy trudności 3×3, 4×4, 5×5
- Zamiana kafelków: stuknij dwa kolejno lub przeciągnij jeden na drugi
- Licznik ruchów i czas, ekran wygranej z „Zagraj ponownie”, przycisk „Podgląd”

## Budowanie
Wymaga JDK 17 i Android SDK (platforma 34). Utwórz `local.properties` z `sdk.dir=...`.
Do podpisu release utwórz `keystore.properties` (storeFile, storePassword, keyAlias, keyPassword) – bez niego używany jest klucz debug.

    ./gradlew assembleRelease
