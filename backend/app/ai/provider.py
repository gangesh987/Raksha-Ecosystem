import os
import sys
from .engine import fuse
from .gemini_live import enabled as gemini_enabled
from . import groq_provider

_local_neural_model = None

def get_local_neural_model():
    global _local_neural_model
    if _local_neural_model is None:
        try:
            repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(__file__))))
            if repo_root not in sys.path:
                sys.path.insert(0, repo_root)
            from ml.models.scam_classifier.v2.inference import ProductionMultilingualClassifier
            _local_neural_model = ProductionMultilingualClassifier()
        except Exception:
            _local_neural_model = False
    return _local_neural_model if _local_neural_model is not False else None

class ModelProvider:
    def score(self, transcript, visual_score, liveness_score, ai=None):
        # 1. Deterministic safety floor / regex baseline
        base = fuse(transcript, visual_score, liveness_score)
        
        # 2. Local Neural Multilingual Semantic Inference (dual-head PyTorch V2 model)
        neural_model = get_local_neural_model()
        if neural_model and transcript:
            try:
                neural_res = neural_model.predict(transcript)
                if neural_res and "scam_probability" in neural_res:
                    p_scam = float(neural_res["scam_probability"])
                    base['neural_scam_score'] = round(p_scam, 3)
                    base['neural_tactics'] = neural_res.get("detected_tactics", [])
                    det_score = base['conversation_score']
                    fused_conv = max(det_score, (p_scam * 0.70) + (det_score * 0.30))
                    base['conversation_score'] = round(fused_conv, 3)
                    for t in neural_res.get("detected_tactics", []):
                        reason_str = f"Neural semantic tactic: {t.replace('_', ' ').title()}"
                        if reason_str not in base['reasons']:
                            base['reasons'].append(reason_str)
                    base['model_mode'] = 'neural-multilingual-v2+safety-floor'
            except Exception:
                pass

        if not ai and groq_provider.enabled() and transcript:
            try:
                ai = groq_provider.analyze(transcript)
            except Exception:
                ai = None

        if ai:
            ai_score = max(0.0, min(1.0, float(ai.get('risk_score', 0))))
            base['ai_score'] = round(ai_score, 3)
            base['ai_confidence'] = round(float(ai.get('confidence', 0)), 3)
            base['ai_tactics'] = ai.get('tactics', [])
            base['ai_stage'] = ai.get('stage', 'CONTACT')
            conversation = (base['conversation_score'] * 0.65) + (ai_score * 0.35)
            fused = (conversation * 0.70) + (base['visual_score'] * 0.20) + (1 - base['liveness_score']) * 0.10
            base['conversation_score'] = round(conversation, 3)
            base['fused_score'] = round(fused, 3)
            base['risk_level'] = 'HIGH' if fused >= 0.62 else 'MEDIUM' if fused >= 0.32 else 'LOW'
            for r in ai.get('reasons', []):
                if r not in base['reasons']: base['reasons'].append(r)
            if ai.get('irreversible_action'):
                base['irreversible_action'] = True
            base['model_mode'] = 'gemini-live+groq+neural-v2+deterministic'
        elif 'model_mode' not in base:
            base['model_mode'] = 'deterministic-local'
        return base

provider = ModelProvider()

