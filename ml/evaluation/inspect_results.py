import json
import sys
sys.stdout.reconfigure(encoding='utf-8')

def load(name):
    with open(f"docs/evaluation_results/{name}", "r", encoding="utf-8") as f:
        return json.load(f)

neu = load("heldout_neural_results.json")
rul = load("heldout_rule_results.json")
hyb = load("heldout_hybrid_results.json")
hn = load("hard_negative_results.json")
uns = load("unseen_scam_results.json")
lng = load("language_results.json")
leg = load("legacy_22_benchmark_results.json")
con = load("pipeline_consistency_results.json")

print("=== 1. ABLATION STUDY METRICS (Held-out Test Split, N=1292 turns) ===")
for m_name, d in [("Neural Only", neu), ("Rule Floor Only", rul), ("Hybrid Live", hyb)]:
    s = d["scam_classification"]
    t = d["tactic_classification"]
    print(f"[{m_name}]")
    print(f"  Scam Binary: Acc={s['accuracy']:.4f}, Prec={s['precision']:.4f}, Rec={s['recall']:.4f}, F1={s['f1']:.4f}, MacroF1={s['macro_f1']:.4f}, MicroF1={s['micro_f1']:.4f}")
    print(f"  Confusion: TP={s['true_positives']}, TN={s['true_negatives']}, FP={s['false_positives']}, FN={s['false_negatives']}, FPR={s['false_positive_rate']:.4f}, FNR={s['false_negative_rate']:.4f}")
    print(f"  Tactics (9-class): MacroF1={t['macro_f1']:.4f}, MicroF1={t['micro_f1']:.4f}, MicroPrec={t['micro_precision']:.4f}, MicroRec={t['micro_recall']:.4f}")

print("\n=== 2. HARD NEGATIVES (N=30) ===")
for k in ["neural_only", "rule_only", "hybrid_system"]:
    print(f"  {k}: FP={hn[k]['false_positives']}, TN={hn[k]['true_negatives']}, Specificity={hn[k]['specificity']:.4f}, FPR={hn[k]['fpr']:.4f}")

# Detail on hard negatives
print("  Sample breakdown of hard negatives:")
for s in hn["sample_results"][:6]:
    print(f"    - \"{s['text'][:55]}...\" -> NeuralFP: {s['neural_predicted_scam']} (p={s['neural_scam_prob']}), RuleFP: {s['rule_predicted_scam']}, HybridTop: {s['hybrid_top_tactic']} (p={s['hybrid_scam_prob']})")

print("\n=== 3. UNSEEN SCAM CONVERSATIONS (N=5 convos, 22 turns) ===")
u = uns["metrics"]
print(f"  Turns={uns['turn_count']}, Acc={u['accuracy']:.4f}, Prec={u['precision']:.4f}, Rec={u['recall']:.4f}, F1={u['f1']:.4f}, TP={u['true_positives']}, FN={u['false_negatives']}")
print(f"  Missed turns (FN count): {uns['false_negative_count']}")
for fn_case in uns["false_negative_samples"]:
    print(f"    - Convo {fn_case['conversation_id']}, Turn {fn_case['turn_index']} ({fn_case['language']}): \"{fn_case['text']}\" -> prob={fn_case['scam_probability']}, top={fn_case['top_tactic']}")

print("\n=== 4. LANGUAGE-WISE RESULTS ===")
for l_name, l_data in lng.items():
    print(f"  {l_name}: N={l_data['sample_count']} (Scam: {l_data['scam_ground_truth_count']}, Benign: {l_data['benign_ground_truth_count']}), Prec={l_data['scam_precision']:.4f}, Rec={l_data['scam_recall']:.4f}, F1={l_data['scam_f1']:.4f} [{l_data['disclaimer']}]")

print("\n=== 5. LEGACY 22-SCENARIO SMOKE TEST ===")
print(f"  Scenarios Passed: {leg['scenarios_passed']}/{leg['sample_count']} ({leg['pass_rate']*100:.1f}%)")

print("\n=== 6. RUNTIME PIPELINE CONSISTENCY (20 Convos, 137 turns) ===")
print(f"  Total Turns Checked: {con['total_turns_tested']}")
print(f"  Runtime Consuming Neural Verified: {con['runtime_consuming_neural_verified']}")
print(f"  Explanation: {con['explanation']}")
