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
        data/answers_derived.json app/src/main/assets/questions.json
cp out/images/* app/src/main/assets/images/
```

* `extract.py` sugrupuoja pastraipas į klausimus, atpažįsta pabrauktą (teisingą)
  atsakymą ir išsaugo paveikslėlius.
* `finalize.py` sunormina tekstą, iškelia taisyklių nuorodas iš atsakymų teksto
  (dokumente jos rašomos tik prie teisingo varianto, todėl išduotų atsakymą) ir
  prijungia `data/answers_derived.json` atsakymus 8–15 temoms.
