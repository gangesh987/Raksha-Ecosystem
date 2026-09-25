from __future__ import annotations
from dataclasses import dataclass, field
from enum import Enum
from time import time
from typing import Any, AsyncIterator, Optional
import asyncio

class ConnectorState(str, Enum):
    UNAVAILABLE="unavailable"
    PERMISSION_REQUIRED="permission_required"
    CONNECTING="connecting"
    CONNECTED="connected"
    DEGRADED="degraded"
    STOPPED="stopped"
    ERROR="error"

@dataclass
class MediaChunk:
    source: str
    kind: str  # audio | video
    payload_ref: str
    timestamp: float = field(default_factory=time)
    metadata: dict[str, Any] = field(default_factory=dict)

@dataclass
class ConnectorStatus:
    provider: str
    state: ConnectorState
    consent: bool
    capabilities: list[str]
    detail: str = ""

class BaseRealtimeConnector:
    provider="base"
    async def status(self) -> ConnectorStatus:
        return ConnectorStatus(self.provider, ConnectorState.UNAVAILABLE, False, [])
    async def start(self, consent_token: Optional[str]=None) -> ConnectorStatus:
        raise NotImplementedError
    async def stop(self) -> ConnectorStatus:
        raise NotImplementedError
    async def chunks(self) -> AsyncIterator[MediaChunk]:
        if False: yield  # pragma: no cover

class MeetMediaConnector(BaseRealtimeConnector):
    """Provider boundary for Google Meet Media API.

    Real OAuth/Meet Media API calls belong in the deployment adapter.
    This class intentionally refuses to pretend a connection exists.
    """
    provider="google_meet_media_api"
    def __init__(self):
        self._state=ConnectorState.PERMISSION_REQUIRED
        self._consent=False
    async def status(self):
        return ConnectorStatus(self.provider, self._state, self._consent,
                               ["audio","video","participant_events"],
                               "Requires authorized Meet Media API session and explicit consent.")
    async def start(self, consent_token=None):
        if not consent_token:
            self._state=ConnectorState.PERMISSION_REQUIRED
            return await self.status()
        self._consent=True
        self._state=ConnectorState.CONNECTING
        return await self.status()
    async def stop(self):
        self._state=ConnectorState.STOPPED
        return await self.status()

class UserConsentedCaptureConnector(BaseRealtimeConnector):
    """Desktop/browser capture boundary for WhatsApp and other call apps.

    The client must obtain OS/browser capture permission. The backend receives
    chunks through an authenticated streaming channel; it never receives or
    decrypts WhatsApp protocol traffic.
    """
    provider="user_consented_capture"
    def __init__(self, source="whatsapp_web_or_desktop"):
        self.source=source
        self._state=ConnectorState.PERMISSION_REQUIRED
        self._consent=False
    async def status(self):
        return ConnectorStatus(self.source, self._state, self._consent,
                               ["audio","video","screen_context"],
                               "Requires explicit OS/browser capture permission.")
    async def start(self, consent_token=None):
        if not consent_token:
            self._state=ConnectorState.PERMISSION_REQUIRED
            return await self.status()
        self._consent=True
        self._state=ConnectorState.CONNECTING
        return await self.status()
    async def stop(self):
        self._state=ConnectorState.STOPPED
        return await self.status()
