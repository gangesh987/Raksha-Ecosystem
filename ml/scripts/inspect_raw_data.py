import json
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

with open("ml/datasets/raw/scambench/samples.json", "r", encoding="utf-8") as f:
    sb = json.load(f)
print("ScamBench total:", len(sb))
print("First item messages type:", type(sb[0].get("messages")))
print("First item messages repr:", repr(sb[0].get("messages")[:250]))

with open("ml/datasets/raw/scam_dialogue/samples.json", "r", encoding="utf-8") as f:
    sd = json.load(f)
print("\nScamDialogue total:", len(sd))
print("First item keys:", list(sd[0].keys()))
print("First dialogue snippet:", repr(sd[0].get("dialogue")[:250]))
print("First item label:", sd[0].get("label"), "type:", sd[0].get("type"))
