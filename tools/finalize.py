# -*- coding: utf-8 -*-
"""Turn the raw extraction into the dataset the app ships.

  * normalises whitespace and strips the stray numbering left in option text;
  * lifts the rule reference ("(COLREGS-72. 24 taisyklė)") out of the option
    text into the question - in the source it is only ever printed next to the
    correct option, so leaving it in place would give the answer away;
  * assigns stable ids and records where each answer came from.
"""
import json, os, re, sys

REF_RE = re.compile(
    r'\s*\((?:COLREGS[^)]*|[^)]*taisykl[ėe][^)]*|CEVNI[^)]*|[^)]*SM įsakym[^)]*'
    r'|Praktinė užduotis\.?|Elvyra Barauskienė[^)]*)(?:\)|$)\s*', re.I)
# Leftovers of rule references that lost their opening bracket in the source,
# e.g. "... laivui A (ii) taisyklė)", "Abu atsakymai teisingi, c(i) )",
# "... (ii) g) )". Like the references themselves they only ever trail the
# correct option, so they have to go.
TAIL_RES = [
    re.compile(r'\s*taisyklė\)?$'),
    re.compile(r'\s*,?\s*[a-z]?\((?:i|ii|iii|iv)\)(?:\s*[a-z]\))?\s*\)?$'),
    re.compile(r'(?<=teisingi),\s*[a-z]$'),
    re.compile(r'\s*Pav\.\s*[\d.]+,?\s*[a-z]\)$'),
    re.compile(r'\s*\($'),
]
WS_RE = re.compile(r'[ \t ]+')


def clean(text):
    text = WS_RE.sub(' ', text.replace(' ', ' ')).strip()
    text = re.sub(r'\s+([,.;:])', r'\1', text)
    # Words glued across a line break in the source ("asmeniuiužtraukia").
    text = re.sub(r'(?<=[a-ząčęėįšųūž])(?=užtraukia\b)', ' ', text)
    text = re.sub(r'\s*Top of Form\s*$', '', text)
    return strip_tail(text.strip(' ;'))


def strip_tail(text):
    while True:
        before = text
        for r in TAIL_RES:
            text = r.sub('', text).rstrip()
        if text.endswith(')') and text.count(')') > text.count('('):
            text = text[:-1].rstrip()
        if text == before:
            return text


def main():
    raw_path, ans_path, out_path = sys.argv[1], sys.argv[2], sys.argv[3]
    raw = json.load(open(raw_path, encoding='utf-8'))

    overrides = {}
    if os.path.exists(ans_path):
        overrides = json.load(open(ans_path, encoding='utf-8'))
    # Hand corrections for extraction mistakes the parser cannot detect
    # (text from a neighbouring block glued onto an option, duplicate numbers).
    fixes_path = os.path.join(os.path.dirname(ans_path), 'fixes.json')
    fixes = json.load(open(fixes_path, encoding='utf-8')) if os.path.exists(fixes_path) else {}

    seen, out = {}, []
    for q in raw['questions']:
        qid = f"t{q['topic']}_{q['number']}"
        if qid in seen:
            seen[qid] += 1
            qid = f"{qid}_{seen[qid]}"
        else:
            seen[qid] = 0

        refs = []
        options = []
        for o in q['options']:
            text = o['text']
            for m in REF_RE.findall(text):
                m = m.strip()
                if 'Praktinė užduotis' in m:
                    continue
                refs.append(m + ')' * (m.count('(') - m.count(')')))
            text = REF_RE.sub(' ', text)
            options.append({'text': clean(text), 'images': o['images']})

        answer = q['answer']
        source = 'doc' if answer is not None else None
        if qid in overrides:
            answer = overrides[qid]['answer']
            source = overrides[qid].get('source', 'derived')

        item = {
            'id': qid,
            'topic': q['topic'],
            'number': q['number'],
            'text': clean(q['text']),
            'images': q['images'],
            'options': options,
            'answer': answer,
            'answerSource': source,
        }
        if refs:
            item['reference'] = '; '.join(dict.fromkeys(refs))
        if qid in overrides:
            if overrides[qid].get('note'):
                item['note'] = overrides[qid]['note']
            if overrides[qid].get('confidence'):
                item['confidence'] = overrides[qid]['confidence']
        fix = fixes.get(qid, {})
        for idx, opt in fix.get('options', {}).items():
            item['options'][int(idx)].update(opt)
        out.append(item)
        out.extend(fix.get('insertAfter', []))

    data = {
        'version': 1,
        'topics': [{'id': int(k), 'name': v} for k, v in sorted(raw['topics'].items(), key=lambda x: int(x[0]))],
        'questions': out,
    }
    with open(out_path, 'w', encoding='utf-8') as fh:
        json.dump(data, fh, ensure_ascii=False, separators=(',', ':'))

    doc = sum(1 for q in out if q['answerSource'] == 'doc')
    der = sum(1 for q in out if q['answerSource'] == 'derived')
    none = sum(1 for q in out if q['answer'] is None)
    print(f"questions={len(out)}  answers: from document={doc}, derived={der}, missing={none}")
    print(f"with rule reference={sum(1 for q in out if q.get('reference'))}")
    print(f"wrote {out_path} ({os.path.getsize(out_path)/1024:.0f} KB)")


if __name__ == '__main__':
    main()
