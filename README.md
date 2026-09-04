# Laivavedžio testai

Android programėlė motorinių pramoginių laivų laivavedžio kursų klausimynams
spręsti. Klausimai paimti iš 15 kursų dokumentų (611 klausimų su paveikslėliais).

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

## Kaip gauti APK

Kiekvienas `push` paleidžia GitHub Actions darbą **Build APK**. Atsisiųsk
`laivavedys-debug-apk` artefaktą iš to darbo ir įsidiek telefone (reikės leisti
diegti iš nežinomų šaltinių).

Vietoje:

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Reikia Android SDK (compileSdk 34) ir JDK 17. Minimali Android versija – 7.0 (API 24).

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
app/                     Android programėlė (Kotlin + Jetpack Compose, Material 3)
  src/main/assets/       questions.json + 225 paveikslėliai
data/answers_derived.json  8–15 temų atsakymai su pagrindimais
data/questions_raw.json    neapdorotas ekstrakcijos rezultatas
tools/                   .doc -> questions.json vamzdynas
```
