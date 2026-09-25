from fastapi import APIRouter
from .realtime_connectors import MeetMediaConnector, UserConsentedCaptureConnector

router=APIRouter(prefix="/api/realtime", tags=["realtime"])
meet=MeetMediaConnector()
whatsapp=UserConsentedCaptureConnector()

@router.get("/connectors")
async def connectors():
    return {
        "connectors":[
            (await meet.status()).__dict__,
            (await whatsapp.status()).__dict__,
        ],
        "policy":"Explicit user consent required. No encrypted transport interception."
    }
