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

def hash_password(x):
    # Bcrypt has a hard 72-byte limit. Passwords exceeding 72 bytes are safely SHA-256 hashed.
    if isinstance(x, str) and len(x.encode("utf-8")) > 72:
        import hashlib
        x = hashlib.sha256(x.encode("utf-8")).hexdigest()[:72]
    return pwd.hash(x)

def verify_password(x, h):
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
