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
versija susikompiliuoja, ir įkelia `laivavedys-ios-simulator-app` artefaktą
(paleidžiamas simuliatoriuje, į tikrą telefoną reikia pasirašyti Xcode).

## Atsakymų šaltinis — svarbu

* **Visos 15 temų (630 iš 634 klausimų)** – teisingas atsakymas paimtas tiesiai
  iš dokumento, kur jis pabrauktas (15 temoje – paryškintas). 8–15 temų
  pažymėtos versijos gautos vėliau; pirmosiose kopijose atsakymai nebuvo
  pažymėti. Pažymėtose 9 ir 11 temų versijose buvo ir 21 klausimas, kurių
  pirmosiose kopijose nebuvo. Po pažymėtu atsakymu dokumente atspausdinta
  teisės akto nuoroda programėlėje rodoma tik atsakius.
* **4 klausimai** – atskirti iš suklijuotų dokumento vietų (t2_37_1, t15_8_1),
  arba dokumente pažymėti du variantai (t14_8), arba nepažymėti (t2_19). Jų
  atsakymai nustatyti pagal teisės aktų tekstus.

Programėlėje šie klausimai po atsakymo pažymimi užrašu „Atsakymas patikrintas“
arba „Atsakymas atkurtas“ su trumpu pagrindimu. Jie surašyti
`data/answers_derived.json`, rankiniai ekstrakcijos pataisymai (suklijuoti
klausimai, trūkstamas t8_43 ženklas, iškirptas iš EVVKLT 7 priedo) –
`data/fixes.json`. Juos galima taisyti ir perleisti vamzdyną (žr. `tools/README.md`).

## Struktūra

```
composeApp/src/commonMain/     bendras kodas: ekranai, logika, duomenys
  kotlin/.../ui/screens/       HomeScreen, QuizScreen, ResultScreen, ProgressScreen
  composeResources/files/      questions.json + 225 paveikslėliai
composeApp/src/androidMain/    MainActivity, piktogramos, failų saugojimas
composeApp/src/iosMain/        MainViewController, failų saugojimas
iosApp/                        Xcode projektas (SwiftUI apvalkalas)
data/answers_derived.json      atkurti atsakymai su pagrindimais
data/fixes.json                rankiniai ekstrakcijos pataisymai
data/questions_raw.json        neapdorotas ekstrakcijos rezultatas
tools/                         .doc -> questions.json vamzdynas
```

Platformoms specifinis kodas – tik du failai (`Storage.android.kt`,
`Storage.ios.kt`) plius paleidimo taškai. Visa kita bendra.
