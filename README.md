# Laivavedžio testai

Programėlė **Android ir iPhone** motorinių pramoginių laivų laivavedžio kursų
klausimynams spręsti. Vienas bendras kodas (Kotlin Multiplatform + Compose
Multiplatform): visi ekranai, logika ir duomenys yra `commonMain`, o kiekvienai
platformai lieka tik paleidimo taškas ir failų saugojimas.

Klausimai paimti iš 15 kursų dokumentų (611 klausimų su paveikslėliais).

## Ką moka

* **30 klausimų sesija** (galima rinktis 10 / 20 / 30 / 50), klausimai išmėtomi
  atsitiktine tvarka.
* **Temų pasirinkimas** – po vieną, kelias arba visas kartu.
* **Šviesoforas kiekvienam klausimui** pagal paskutinius 2 sprendimus:
  * 🟢 **žalia** – paskutinius 2 kartus atsakyta teisingai;
  * 🟡 **geltona** – vieną iš paskutinių 2 kartų suklysta (arba teisingai atsakyta tik kartą);
  * 🔴 **raudona** – paskutinius 2 kartus atsakyta neteisingai;
  * ⚪ **pilka** – dar nespręsta.
* **Progresas nuo paskutinės sesijos** – kiek klausimų pajudėjo į žalią, geltoną
  ar raudoną pusę, plius sesijų istorija ir kiekvienos temos padengimas.
* **Kartojimo režimas** – pirmiausia pateikia raudonus ir geltonus klausimus.
* Progresas saugomas įrenginyje (`progress.json`), interneto nereikia.

## Android

Kiekvienas `push` paleidžia GitHub Actions darbą **Build**. Atsisiųsk
`laivavedys-debug-apk` artefaktą ir įsidiek telefone (reikės leisti diegti iš
nežinomų šaltinių).

Vietoje:

```bash
./gradlew :composeApp:assembleDebug
# composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Reikia Android SDK (compileSdk 34) ir JDK 17. Minimali versija – Android 7.0 (API 24).

## iPhone

Tai tikra native iOS programa – ta pati Compose sąsaja, sukompiliuota į iOS
framework'ą. **Kompiliavimui reikia Mac su Xcode** (kitaip Apple neleidžia).

```bash
open iosApp/iosApp.xcodeproj
```

Xcode pats paleidžia Gradle, susikompiliuoja Kotlin framework'ą ir paleidžia
programą simuliatoriuje arba prijungtame telefone.

Norint įsidiegti į savo iPhone, Xcode → target `iosApp` → Signing & Capabilities
nurodyk savo Apple ID (nemokamas asmeninis sertifikatas galioja 7 dienas;
App Store platinimui reikia Apple Developer Program – 99 $/m.).

Minimali versija – iOS 16. CI (macOS runner) kiekvieną push'ą patikrina, kad iOS
versija susikompiliuoja, ir įkelia `laivavedys-ios-simulator-app` artefaktą.

## Atsakymų šaltinis — svarbu

* **1–7 temos (345 klausimai)** – teisingas atsakymas paimtas tiesiai iš
  dokumento, kur jis pabrauktas.
* **8–15 temos (265 klausimai) ir vienas 2 temos klausimas** – šaltinio
  dokumentuose atsakymai **nepažymėti** (patikrinta dviem nepriklausomais `.doc`
  skaitytuvais: nėra nei pabraukimo, nei kitokio formatavimo skirtumo). Šiuos
  atsakymus nustatė Claude pagal COLREGS, CEVNI laivybos ženklus, MARPOL, UTB
  kanalų lenteles ir LR teisės aktus.

Programėlėje toks klausimas po atsakymo pažymimas užrašu „Atsakymas atkurtas“ su
patikimumo lygiu (aukštas / vidutinis / žemas) ir trumpu pagrindimu. Žemo
patikimumo atsakymus (daugiausia tikslios ANK baudų sumos) verta pasitikrinti
šaltinyje. Visi atsakymai surašyti `data/answers_derived.json` – juos galima
taisyti ir perleisti vamzdyną (žr. `tools/README.md`).

## Struktūra

```
composeApp/src/commonMain/     bendras kodas: ekranai, logika, duomenys
  kotlin/.../ui/screens/       HomeScreen, QuizScreen, ResultScreen, ProgressScreen
  composeResources/files/      questions.json + 225 paveikslėliai
composeApp/src/androidMain/    MainActivity, piktogramos, failų saugojimas
composeApp/src/iosMain/        MainViewController, failų saugojimas
iosApp/                        Xcode projektas (SwiftUI apvalkalas)
data/answers_derived.json      8–15 temų atsakymai su pagrindimais
data/questions_raw.json        neapdorotas ekstrakcijos rezultatas
tools/                         .doc -> questions.json vamzdynas
```

Platformoms specifinis kodas – tik du failai (`Storage.android.kt`,
`Storage.ios.kt`) plius paleidimo taškai. Visa kita bendra.
