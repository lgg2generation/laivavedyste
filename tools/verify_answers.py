# -*- coding: utf-8 -*-
"""Cross-check our answer key against an external question bank.

This environment cannot reach mblaivyba.lt / etestai.lt (the egress proxy
denies every .lt host), so the source data has to be supplied as a file.

Input: JSON, a list of objects in either shape

    {"question": "...", "correct": "the correct option text"}
    {"question": "...", "options": ["a", "b", "c"], "answer": 0}

Questions are matched to ours by normalised text similarity, so the source
does not have to use identical wording or numbering.

    python3 tools/verify_answers.py source.json [--threshold 0.72]
"""
import json, re, sys, unicodedata
from difflib import SequenceMatcher

BANK = "composeApp/src/commonMain/composeResources/files/questions.json"

PUNCT = re.compile(r"[^\w\s]", re.UNICODE)
WS = re.compile(r"\s+")


def norm(text):
    """Fold case, strip diacritics and punctuation so wording differences survive."""
    text = unicodedata.normalize("NFKD", text or "")
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = PUNCT.sub(" ", text.lower())
    return WS.sub(" ", text).strip()


def similar(a, b):
    return SequenceMatcher(None, a, b).ratio()


def load_source(path):
    raw = json.load(open(path, encoding="utf-8"))
    items = raw["questions"] if isinstance(raw, dict) and "questions" in raw else raw
    out = []
    for it in items:
        question = it.get("question") or it.get("text") or ""
        options = it.get("options") or []
        correct = it.get("correct")
        if correct is None and "answer" in it and options:
            idx = it["answer"]
            if isinstance(idx, int) and 0 <= idx < len(options):
                correct = options[idx]
        if question and correct:
            out.append({"question": question, "correct": correct, "options": options})
    return out


def pick_question(sn, source_options, ours, threshold):
    """Many questions share a stem ("Ka reiskia sis zenklas"), so the question
    text alone is not enough - the options decide which one it really is.

    Returns (question, score) or (None, best_score) when nothing is decisive.
    """
    cands = []
    for q in ours:
        r = similar(sn, q["_n"])
        if r >= threshold:
            cands.append((r, q))
    if not cands:
        best = max((similar(sn, q["_n"]) for q in ours), default=0.0)
        return None, best

    def option_fit(q):
        if not source_options:
            return 0.0
        opts = [norm(o["text"]) for o in q["options"]]
        total = 0.0
        for so in source_options:
            son = norm(so)
            total += max((similar(son, o) for o in opts), default=0.0)
        return total / len(source_options)

    scored = sorted(((r + option_fit(q), r, q) for r, q in cands), reverse=True,
                    key=lambda x: x[0])
    if len(scored) > 1 and (scored[0][0] - scored[1][0]) < 0.05:
        return None, scored[0][1]      # stem shared by several questions
    return scored[0][2], scored[0][1]


def main():
    src_path = sys.argv[1]
    threshold = 0.72
    if "--threshold" in sys.argv:
        threshold = float(sys.argv[sys.argv.index("--threshold") + 1])

    ours = json.load(open(BANK, encoding="utf-8"))["questions"]
    for q in ours:
        q["_n"] = norm(q["text"])
    source = load_source(src_path)

    agree, disagree, unmatched, ambiguous = [], [], [], []

    for s in source:
        sn = norm(s["question"])
        best, score = pick_question(sn, s.get("options"), ours, threshold)
        if best is None:
            unmatched.append((s, score))
            continue

        # Which of our options does the source's correct answer correspond to?
        cn = norm(s["correct"])
        ranked = sorted(
            ((similar(cn, norm(o["text"])), i) for i, o in enumerate(best["options"])),
            reverse=True,
        )
        top_score, top_idx = ranked[0]
        second = ranked[1][0] if len(ranked) > 1 else 0.0
        if top_score < 0.55 or (top_score - second) < 0.08:
            ambiguous.append((s, best, score, ranked))
            continue

        if top_idx == best["answer"]:
            agree.append((s, best, score))
        else:
            disagree.append((s, best, score, top_idx))

    total = len(agree) + len(disagree)
    print(f"source questions      : {len(source)}")
    print(f"matched to our bank   : {total} (threshold {threshold})")
    print(f"  agree               : {len(agree)}")
    print(f"  DISAGREE            : {len(disagree)}")
    print(f"answer text ambiguous : {len(ambiguous)}")
    print(f"question unmatched    : {len(unmatched)}")

    if disagree:
        print("\n=== MISMATCHES (source says a different option) ===")
        for s, q, score, src_idx in sorted(disagree, key=lambda x: x[1]["id"]):
            print(f"\n[{q['id']}] ({q['answerSource']}"
                  f"{'/' + q['confidence'] if q.get('confidence') else ''}) match={score:.2f}")
            print(f"  Q: {q['text'][:150]}")
            print(f"  ours   [{q['answer']}]: {q['options'][q['answer']]['text'][:130]}")
            print(f"  source [{src_idx}]: {q['options'][src_idx]['text'][:130]}")

    if ambiguous:
        print("\n=== AMBIGUOUS (could not map the source answer onto our options) ===")
        for s, q, score, ranked in ambiguous[:40]:
            print(f"[{q['id']}] ours=[{q['answer']}] source_text={s['correct'][:90]!r}")

    with open("verify_report.json", "w", encoding="utf-8") as fh:
        json.dump(
            {
                "agree": [q["id"] for _, q, _ in agree],
                "disagree": [
                    {"id": q["id"], "ours": q["answer"], "source": i,
                     "source_answer": q["options"][i]["text"],
                     "answerSource": q["answerSource"],
                     "confidence": q.get("confidence")}
                    for _, q, _, i in disagree
                ],
                "ambiguous": [q["id"] for _, q, _, _ in ambiguous],
                "unmatched": [s["question"][:120] for s, _ in unmatched],
            },
            fh, ensure_ascii=False, indent=1,
        )
    print("\nwrote verify_report.json")


if __name__ == "__main__":
    main()
