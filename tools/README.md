# Duomenų vamzdynas

Klausimai paimti iš 15 Word (`.doc`) dokumentų. Kadangi tai senas dvejetainis
Word 97-2003 formatas, jie pirma konvertuojami į `.docx`:

```bash
soffice --headless --convert-to docx --outdir docx *.doc
```

Tada:

```bash
python3 tools/extract.py  docx out                              # -> out/questions_raw.json + out/images/
python3 tools/finalize.py out/questions_raw.json \
        data/answers_derived.json composeApp/src/commonMain/composeResources/files/questions.json
cp out/images/* composeApp/src/commonMain/composeResources/files/images/
```

* `extract.py` sugrupuoja pastraipas į klausimus, atpažįsta pabrauktą (teisingą)
  atsakymą ir išsaugo paveikslėlius.
* `finalize.py` sunormina tekstą, iškelia taisyklių nuorodas iš atsakymų teksto
  (dokumente jos rašomos tik prie teisingo varianto, todėl išduotų atsakymą) ir
  prijungia `data/answers_derived.json` atsakymus 8–15 temoms.

## Atsakymų patikrinimas pagal išorinį šaltinį

`verify_answers.py` sulygina mūsų atsakymus su kito klausimyno (pvz.
mblaivyba.lt ar etestai.lt) duomenimis. Šaltinį reikia paduoti failu — šios
aplinkos tinklo politika blokuoja visus `.lt` domenus, tad parsisiųsti
automatiškai negalima.

Šaltinio formatas (JSON sąrašas, bet kuris iš dviejų variantų):

```json
[{"question": "...", "options": ["a","b","c"], "answer": 0},
 {"question": "...", "correct": "teisingo varianto tekstas"}]
```

```bash
python3 tools/verify_answers.py source.json      # -> verify_report.json
```

Klausimai sulyginami pagal normalizuotą tekstą, todėl formuluotės ir numeracija
gali skirtis. Daug klausimų turi vienodą pradžią („Ką reiškia šis ženklas“),
todėl sutapimą lemia ir atsakymų variantai; kai nė vienas variantas nėra
akivaizdžiai artimesnis, klausimas pažymimas kaip neatpažintas, o ne spėjamas.

Savipatikra (120 klausimų, 12 tyčia apverstų atsakymų): rasta 10 iš 12, nė
vieno klaidingo įspėjimo ir nė vienas blogas atsakymas nepažymėtas kaip geras —
likę du pateko į „neatpažinta“.

`data/derived_answers_review.csv` — visi 266 atkurti atsakymai rankiniam
tikrinimui, surikiuoti nuo žemiausio patikimumo.
