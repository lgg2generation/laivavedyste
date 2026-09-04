# -*- coding: utf-8 -*-
"""Extract questions, answer keys and images from the .doc question banks.

The sources are Word 97-2003 documents (converted to .docx with LibreOffice
first).  Where the source carries an answer key the correct option is
underlined.

Layout quirks the parser has to survive:
  * question numbering is not always dense (T11 only uses odd numbers);
  * options are numbered 1..3 (never four), but a few blocks leave them unnumbered;
  * a question or an option may continue on the following paragraphs;
  * pictures live in their own paragraphs between question and options, and
    LibreOffice emits every picture twice (DrawingML + VML fallback).
"""
import json, os, re, sys, hashlib
import docx
from docx.oxml.ns import qn

# The separator after the number is optional: a few options are typed '3 Burlaivis'.
NUM_RE = re.compile(r'^\s*(\d{1,3})\s*(?:[\.\)]\s*|\s+)(.*)$', re.S)
LETTER_RE = re.compile(r'[^\W\d_]', re.UNICODE)

TOPICS = {
    1:  "Laivo teorija",
    2:  "Locija",
    3:  "Navigacija",
    4:  "Žiburiai ir ženklai (COLREGS-72)",
    5:  "Laivybos taisyklės",
    6:  "Vidaus vandenų laivų žiburiai ir signalai",
    7:  "Garsiniai signalai",
    8:  "Laivybos ženklai",
    9:  "Gelbėjimo ir priešgaisrinės priemonės",
    10: "UTB radijo ryšys",
    11: "LR vidaus vandenų transporto kodeksas",
    12: "Hidrometeorologija",
    13: "Aplinkosaugos reikalavimai",
    14: "Avarijų tyrimo taisyklės",
    15: "Administracinių nusižengimų kodeksas",
}


def is_underlined(par):
    """True when a meaningful run of the paragraph is underlined."""
    for r in par.runs:
        if not r.text.strip():
            continue
        rPr = r._element.rPr
        if rPr is None:
            continue
        u = rPr.find(qn('w:u'))
        if u is not None and u.get(qn('w:val')) not in (None, 'none'):
            return True
    return False


def par_images(par, doc):
    """Ordered, de-duplicated image parts referenced by the paragraph."""
    out, seen = [], set()
    for rid in re.findall(r'r:(?:embed|id)="(rId\d+)"', par._p.xml):
        part = doc.part.related_parts.get(rid)
        if part is None:
            continue
        name = str(part.partname)
        if '/media/' not in name or name in seen:
            continue
        seen.add(name)
        out.append(part)
    return out


def looks_like_question(body):
    """Question bodies are prose; the '5.50' printed on a road sign is not."""
    return len(body) >= 6 and len(LETTER_RE.findall(body)) >= 4


def split_blocks(doc):
    """Split the document into (question_number, question_body, [lines]) blocks.

    A line is a dict with text / underlined / images / number.
    """
    lines = []
    for par in doc.paragraphs:
        text = par.text.replace('\xa0', ' ').strip()
        imgs = par_images(par, doc)
        if not text and not imgs:
            continue
        m = NUM_RE.match(text)
        lines.append({
            'text': text,
            'number': int(m.group(1)) if m else None,
            'body': m.group(2).strip() if m else text,
            'underlined': is_underlined(par),
            'images': imgs,
        })

    blocks, cur, last_q = [], None, 0
    for ln in lines:
        n, body = ln['number'], ln['body']
        # How many numbered option lines has the current block collected?
        opts = 0
        if cur:
            for c in cur['lines']:
                if c['number'] == opts + 1 and c['number'] <= 3 and c['body']:
                    opts += 1
        if n is not None and body and looks_like_question(body) and n > last_q \
                and (n >= 4 or opts >= 3 or cur is None):
            cur = {'number': n, 'body': body, 'lines': []}
            blocks.append(cur)
            last_q = n
            ln['images'] = list(ln['images'])
            cur['q_images'] = ln['images']
            continue
        if cur is not None:
            cur['lines'].append(ln)
    return blocks


def build_question(block, topic_id):
    q = {'topic': topic_id, 'number': block['number'], 'text': block['body'],
         'options': [], 'images': list(block['q_images']), 'answer': None}

    # --- pass 1: numbered options ------------------------------------------
    opts, idx = [], 0
    for i, ln in enumerate(block['lines']):
        if ln['number'] == len(opts) + 1 and ln['number'] <= 3 and ln['body']:
            opts.append({'text': ln['body'], 'underlined': ln['underlined'],
                         'images': list(ln['images']), 'at': i})
    if len(opts) >= 2:
        first = opts[0]['at']
        for ln in block['lines'][:first]:          # question continuation
            if ln['text']:
                q['text'] = (q['text'] + ' ' + ln['text']).strip()
            q['images'].extend(ln['images'])
        bounds = [o['at'] for o in opts] + [len(block['lines'])]
        for k, o in enumerate(opts):
            for ln in block['lines'][bounds[k] + 1:bounds[k + 1]]:
                if ln['text']:
                    o['text'] += ' ' + ln['text']
                o['images'].extend(ln['images'])
            del o['at']
        q['options'] = opts
        return q

    # --- pass 2: unnumbered options (last three text paragraphs) -----------
    texty = [i for i, ln in enumerate(block['lines']) if ln['text']]
    if len(texty) >= 3:
        opt_at = texty[-3:]
        for i, ln in enumerate(block['lines']):
            if i in opt_at:
                q['options'].append({'text': ln['text'],
                                     'underlined': ln['underlined'],
                                     'images': list(ln['images'])})
            elif i < opt_at[0]:
                if ln['text']:
                    q['text'] = (q['text'] + ' ' + ln['text']).strip()
                q['images'].extend(ln['images'])
            else:
                q['options'][-1]['images'].extend(ln['images'])
        return q
    return None


def parse(path, topic_id):
    doc = docx.Document(path)
    out = []
    for b in split_blocks(doc):
        q = build_question(b, topic_id)
        if q and len(q['options']) >= 2:
            out.append(q)
    return out


def save_images(questions, media_dir, index):
    for q in questions:
        for holder in [q] + q['options']:
            names = []
            for part in holder['images']:
                blob = part.blob
                digest = hashlib.sha1(blob).hexdigest()[:16]
                if digest not in index:
                    ext = os.path.splitext(str(part.partname))[1].lower()
                    fname = f"img_{digest}{ext}"
                    with open(os.path.join(media_dir, fname), 'wb') as fh:
                        fh.write(blob)
                    index[digest] = fname
                if index[digest] not in names:
                    names.append(index[digest])
            holder['images'] = names


def main():
    docx_dir, out_dir = sys.argv[1], sys.argv[2]
    media_dir = os.path.join(out_dir, 'images')
    os.makedirs(media_dir, exist_ok=True)

    all_q, stats, index = [], [], {}
    for t in range(1, 16):
        qs = parse(os.path.join(docx_dir, f'T{t}.docx'), t)
        save_images(qs, media_dir, index)
        marked = 0
        for q in qs:
            hits = [i for i, o in enumerate(q['options']) if o['underlined']]
            if hits:
                q['answer'] = hits[0]
                marked += 1
                if len(hits) > 1:
                    q['multi_marked'] = hits
        nums = sorted(q['number'] for q in qs)
        gaps = [n for n in range(1, (nums[-1] if nums else 0) + 1) if n not in nums]
        stats.append((t, len(qs), marked,
                      sum(1 for q in qs if q['images'] or any(o['images'] for o in q['options'])),
                      gaps))
        all_q.extend(qs)

    print(f"{'topic':>5} {'parsed':>7} {'answered':>9} {'images':>7}  gaps")
    for t, n, m, im, gaps in stats:
        print(f"{t:>5} {n:>7} {m:>9} {im:>7}  {gaps if len(gaps) < 12 else str(len(gaps)) + ' (sparse numbering)'}")
    print(f"{'TOTAL':>5} {sum(s[1] for s in stats):>7} {sum(s[2] for s in stats):>9} {sum(s[3] for s in stats):>7}")

    with open(os.path.join(out_dir, 'questions_raw.json'), 'w', encoding='utf-8') as fh:
        json.dump({'topics': TOPICS, 'questions': all_q}, fh, ensure_ascii=False, indent=1)


if __name__ == '__main__':
    main()
