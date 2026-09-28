from datetime import datetime, timedelta, timezone
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import jwt, JWTError
from passlib.context import CryptContext
from sqlalchemy.orm import Session as DBSession
from .config import settings
from .db import SessionLocal
from .models import User

pwd = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth = OAuth2PasswordBearer(tokenUrl="/api/auth/login")

def hash_password(x: str) -> str:
    """
    Hashes a password with bcrypt.
    
    Design Decision (Bcrypt 72-byte limit handling):
    Bcrypt has an architectural input limit of 72 bytes. When passwords exceed 72 bytes
    (common with passphrases and multi-byte UTF-8 scripts such as Tamil/Hindi):
    - Truncation is unsafe (passwords with identical prefixes collide).
    - Hard 4xx rejection frustrates users with legitimate high-entropy passphrases.
    - We adopt the Dropbox/OWASP standard: Passwords > 72 bytes are pre-hashed using
      SHA-256 to a 64-character hexadecimal string, guaranteeing safe input <= 72 bytes
      while preserving full 256-bit collision resistance and entropy.
    """
    if isinstance(x, str) and len(x.encode("utf-8")) > 72:
        import hashlib
        x = hashlib.sha256(x.encode("utf-8")).hexdigest()[:72]
    return pwd.hash(x)

def verify_password(x: str, h: str) -> bool:
    """Verifies a password against a stored bcrypt hash using identical pre-hashing logic."""
    if isinstance(x, str) and len(x.encode("utf-8")) > 72:
        import hashlib
        x = hashlib.sha256(x.encode("utf-8")).hexdigest()[:72]
    return pwd.verify(x, h)

def token_for(user):
    exp=datetime.now(timezone.utc)+timedelta(minutes=settings.access_token_minutes)
    return jwt.encode({"sub":str(user.id),"exp":exp},settings.jwt_secret,algorithm=settings.jwt_algorithm)

def db():
    d=SessionLocal()
    try: yield d
    finally: d.close()

def current_user(token=Depends(oauth), d:DBSession=Depends(db)):
    try: uid=int(jwt.decode(token,settings.jwt_secret,algorithms=[settings.jwt_algorithm])["sub"])
    except (JWTError,ValueError,KeyError): raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED,detail="Invalid or expired token")
    user=d.get(User,uid)
    if not user: raise HTTPException(401,"User not found")
    return user
