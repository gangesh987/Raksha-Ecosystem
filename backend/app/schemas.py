from pydantic import BaseModel, Field

class Register(BaseModel):
    email: str
    password: str = Field(min_length=8)
    name: str

class Login(BaseModel):
    email: str
    password: str

class SessionCreate(BaseModel):
    source: str="simulated"

class Analyze(BaseModel):
    transcript: str
    visual_score: float=Field(default=.2,ge=0,le=1)
    liveness_score: float=Field(default=.8,ge=0,le=1)

class ContactCreate(BaseModel):
    name: str
    phone: str
    consent_enabled: bool=True
