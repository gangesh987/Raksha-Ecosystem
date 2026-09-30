"""
RakshaCall Multi-Vector Risk Fusion Analyzer.
Synthesizes speech signals, visual cues, and behavioral triggers into
fused risk levels, scores, confidence metrics, and numbered transparent reasons.
Conforms to:
- Section 20: 0-19 SAFE, 20-39 LOW, 40-59 MEDIUM, 60-79 HIGH, 80-100 CRITICAL
- Section 28: Numbered transparent explainability
"""

from typing import List, Tuple
from ..models import Signal
from ..taxonomy import (
    CRITICAL_TACTICS, HIGH_TACTICS, MEDIUM_TACTICS, LOW_TACTICS,
    TAXONOMY_CATEGORIES
)


def fuse(signals: List[Signal]) -> Tuple[str, int, List[str], float]:
    """
    Fuse a sliding window of Signal objects into (risk_level, risk_score, reasons, confidence).
    """
    if not signals:
        return "SAFE", 0, ["Normal conversation. No threat signals detected."], 0.90

    types = set(s.type for s in signals)
    score = 0.0
    reasons_list: List[str] = []

    # 1. Base weights from tactic severity
    for s in signals:
        if s.type in CRITICAL_TACTICS:
            score += 32.0 * s.confidence
        elif s.type in HIGH_TACTICS:
            score += 18.0 * s.confidence
        elif s.type in MEDIUM_TACTICS:
            score += 10.0 * s.confidence
        elif s.type in LOW_TACTICS:
            score += 5.0 * s.confidence

    # 2. Coercive Combinations (Synergy Boosters)
    combos = [
        ({"AUTHORITY_IMPERSONATION", "URGENCY"}, 12.0, "Authority claim combined with artificial urgency."),
        ({"POLICE_IMPERSONATION", "LEGAL_THREAT"}, 15.0, "Police impersonation coupled with legal prosecution threats."),
        ({"BANK_IMPERSONATION", "OTP_REQUEST"}, 20.0, "Bank impersonation paired with verification code extraction."),
        ({"ISOLATION", "BANK_TRANSFER_REQUEST"}, 20.0, "Social isolation enforced while demanding money transfer."),
        ({"ISOLATION", "CONTROL"}, 10.0, "Active control demands prohibiting call termination or family contact."),
        ({"ACCOUNT_BLOCK_THREAT", "URGENCY"}, 12.0, "Immediate threat of account freezing used to induce panic."),
        ({"DIGITAL_ARREST", "CONTROL"}, 18.0, "Fabricated digital arrest keeping victim under persistent surveillance."),
        ({"REMOTE_ACCESS", "BANKING_ACTION"}, 18.0, "Attempting remote device access while targeting banking applications.")
    ]

    for combo, boost, desc in combos:
        if combo.issubset(types):
            score += boost
            reasons_list.append(desc)

    # 3. Add descriptive names for detected tactics
    for t in sorted(list(types)):
        if t in TAXONOMY_CATEGORIES:
            reasons_list.append(TAXONOMY_CATEGORIES[t])

    # 4. Score clamping & level discretization
    final_score = int(min(max(score, 0.0), 100.0))

    if final_score >= 80 or any(s.type in CRITICAL_TACTICS for s in signals):
        final_score = max(final_score, 82)
        level = "CRITICAL"
    elif final_score >= 60:
        level = "HIGH"
    elif final_score >= 40:
        level = "MEDIUM"
    elif final_score >= 20:
        level = "LOW"
    else:
        level = "SAFE"

    # 5. Numbered reasons formatting (Section 28)
    deduped = list(dict.fromkeys(reasons_list))
    if not deduped:
        deduped = ["Normal conversation."]
    numbered_reasons = [f"{i+1}. {r}" for i, r in enumerate(deduped[:6])]

    # 6. Confidence calculation
    avg_conf = sum(s.confidence for s in signals) / max(len(signals), 1)
    confidence = min(round(avg_conf + (0.05 if len(types) >= 2 else 0.0), 2), 0.99)

    return level, final_score, numbered_reasons, confidence
