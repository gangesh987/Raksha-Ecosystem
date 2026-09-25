
export const advancedFeatures = [
  { id: "trajectory", title: "Risk Trajectory", detail: "Tracks score direction and acceleration instead of treating each utterance independently." },
  { id: "tactics", title: "Scam Tactic Graph", detail: "Maps authority, urgency, isolation, payment and credential tactics across the conversation." },
  { id: "counterfactual", title: "Counterfactual Safety", detail: "Explains which user action would reduce the current risk state." },
  { id: "integrity", title: "Evidence Integrity", detail: "Uses a hash-chain provenance boundary for exported incident events." },
  { id: "quality", title: "Signal Quality", detail: "Separates model confidence from capture quality and detects weak evidence." },
  { id: "privacy", title: "Privacy Guard", detail: "Consent-first capture, minimization and explicit retention boundaries." },
  { id: "response", title: "Response Ladder", detail: "Graduated intervention with cooldowns to avoid notification storms." },
  { id: "mlops", title: "Model Adapter Layer", detail: "Swap demo adapters for local ONNX/Hugging Face models without changing the product loop." }
] as const;
