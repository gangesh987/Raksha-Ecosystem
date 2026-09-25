"""
RakshaCall Unified ML Data Schema.
Defines turn-level and conversation-level schemas for ingested datasets.
"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field
import uuid
import datetime

class TurnRecord(BaseModel):
    """Unified schema for a single conversational turn or standalone utterance."""
    sample_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    dataset_source: str
    conversation_id: str
    turn_id: int
    language: str  # e.g., 'ta', 'ta-Latn', 'hi', 'hi-Latn', 'en'
    text: str
    speaker: str = "caller"  # 'caller', 'receiver', 'system', 'agent'
    is_scam: bool
    tactics: List[str] = Field(default_factory=list)
    stage: Optional[str] = None
    scenario: Optional[str] = None
    severity: Optional[float] = None
    source_type: str = "conversation"  # 'conversation', 'utterance', 'sms', 'email'
    timestamp: Optional[str] = Field(default_factory=lambda: datetime.datetime.utcnow().isoformat())

class ConversationRecord(BaseModel):
    """Unified schema for a multi-turn conversation preserving dialogue order."""
    conversation_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    dataset_source: str
    language: str
    is_scam: bool
    scenario: Optional[str] = None
    tactics: List[str] = Field(default_factory=list)
    stage_sequence: List[str] = Field(default_factory=list)
    turns: List[TurnRecord] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)
