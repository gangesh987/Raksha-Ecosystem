from datetime import datetime, timezone
from sqlalchemy import String, Text, Float, Boolean, DateTime, ForeignKey, Integer, JSON
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .db import Base

def utcnow(): return datetime.now(timezone.utc)

class User(Base):
    __tablename__="users"
    id: Mapped[int]=mapped_column(Integer,primary_key=True)
    email: Mapped[str]=mapped_column(String(255),unique=True,index=True)
    password_hash: Mapped[str]=mapped_column(String(255))
    name: Mapped[str]=mapped_column(String(120))
    created_at: Mapped[datetime]=mapped_column(DateTime(timezone=True),default=utcnow)

class Session(Base):
    __tablename__="protection_sessions"
    id: Mapped[int]=mapped_column(Integer,primary_key=True)
    user_id: Mapped[int]=mapped_column(ForeignKey("users.id"),index=True)
    status: Mapped[str]=mapped_column(String(30),default="active")
    risk_level: Mapped[str]=mapped_column(String(20),default="LOW")
    risk_score: Mapped[float]=mapped_column(Float,default=0)
    source: Mapped[str]=mapped_column(String(40),default="simulated")
    created_at: Mapped[datetime]=mapped_column(DateTime(timezone=True),default=utcnow)
    updated_at: Mapped[datetime]=mapped_column(DateTime(timezone=True),default=utcnow)
    events=relationship("RiskEvent",back_populates="session",cascade="all,delete-orphan")

class RiskEvent(Base):
    __tablename__="risk_events"
    id: Mapped[int]=mapped_column(Integer,primary_key=True)
    session_id: Mapped[int]=mapped_column(ForeignKey("protection_sessions.id"),index=True)
    transcript: Mapped[str]=mapped_column(Text,default="")
    conversation_score: Mapped[float]=mapped_column(Float,default=0)
    visual_score: Mapped[float]=mapped_column(Float,default=0)
    liveness_score: Mapped[float]=mapped_column(Float,default=1)
    fused_score: Mapped[float]=mapped_column(Float,default=0)
    risk_level: Mapped[str]=mapped_column(String(20),default="LOW")
    reasons: Mapped[list]=mapped_column(JSON,default=list)
    created_at: Mapped[datetime]=mapped_column(DateTime(timezone=True),default=utcnow)
    session=relationship("Session",back_populates="events")

class TrustedContact(Base):
    __tablename__="trusted_contacts"
    id: Mapped[int]=mapped_column(Integer,primary_key=True)
    user_id: Mapped[int]=mapped_column(ForeignKey("users.id"),index=True)
    name: Mapped[str]=mapped_column(String(120))
    phone: Mapped[str]=mapped_column(String(40))
    consent_enabled: Mapped[bool]=mapped_column(Boolean,default=True)

class EvidenceReport(Base):
    __tablename__="evidence_reports"
    id: Mapped[int]=mapped_column(Integer,primary_key=True)
    session_id: Mapped[int]=mapped_column(ForeignKey("protection_sessions.id"),index=True)
    payload: Mapped[dict]=mapped_column(JSON)
    created_at: Mapped[datetime]=mapped_column(DateTime(timezone=True),default=utcnow)

class AuditEvent(Base):
    __tablename__="audit_events"
    id: Mapped[int]=mapped_column(Integer,primary_key=True)
    user_id: Mapped[int|None]=mapped_column(ForeignKey("users.id"),nullable=True)
    action: Mapped[str]=mapped_column(String(100))
    metadata_json: Mapped[dict]=mapped_column(JSON,default=dict)
    created_at: Mapped[datetime]=mapped_column(DateTime(timezone=True),default=utcnow)
