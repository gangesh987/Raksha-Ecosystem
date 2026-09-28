from pydantic import BaseModel, Field
from typing import Any, Literal

RiskLevel = Literal["LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN"]

class Signal(BaseModel):
    type: str
    confidence: float = Field(ge=0.0, le=1.0)
    timestamp: int
    source: str = "unknown"
    evidence_ref: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)

class SignalBatch(BaseModel):
    version: int = 1
    session_id: str
    call_id: str
    signals: list[Signal]

class RiskUpdate(BaseModel):
    level: RiskLevel
    score: int | None = None
    reasons: list[str] = Field(default_factory=list)
    confidence: float | None = None
    signals: list[str] = Field(default_factory=list)
