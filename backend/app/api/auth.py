import secrets
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session as DBSession
from ..schemas import Register, Login, GoogleAuth
from ..models import User
from ..auth import hash_password, verify_password, token_for, db, current_user

router=APIRouter(prefix="/api/auth",tags=["auth"])

@router.post("/register")
def register(p:Register,d:DBSession=Depends(db)):
    if d.query(User).filter_by(email=p.email.lower()).first(): raise HTTPException(409,"Email already registered")
    u=User(email=p.email.lower(),name=p.name,password_hash=hash_password(p.password));d.add(u);d.commit();d.refresh(u)
    return {"access_token":token_for(u),"token_type":"bearer","user":{"id":u.id,"name":u.name,"email":u.email}}

@router.post("/login")
def login(p:Login,d:DBSession=Depends(db)):
    u=d.query(User).filter_by(email=p.email.lower()).first()
    if not u or not verify_password(p.password,u.password_hash): raise HTTPException(401,"Invalid credentials")
    return {"access_token":token_for(u),"token_type":"bearer","user":{"id":u.id,"name":u.name,"email":u.email}}

@router.post("/google")
def google_auth(p:GoogleAuth, d:DBSession=Depends(db)):
    if not p.id_token: raise HTTPException(400, "Missing id_token")
    try:
        from google.oauth2 import id_token as google_id_token
        from google.auth.transport import requests as google_requests
        id_info = google_id_token.verify_oauth2_token(
            p.id_token,
            google_requests.Request()
        )
        email = id_info.get("email")
        if not email: raise HTTPException(400, "Google token does not contain a verified email")
        name = id_info.get("name") or email.split("@")[0]
    except ValueError as e:
        raise HTTPException(401, f"Invalid Google token: {str(e)}")
    except Exception as e:
        raise HTTPException(400, f"Token verification failed: {str(e)}")

    u = d.query(User).filter_by(email=email.lower()).first()
    if not u:
        random_pw = secrets.token_urlsafe(32)
        u = User(email=email.lower(), name=name, password_hash=hash_password(random_pw))
        d.add(u)
        d.commit()
        d.refresh(u)

    return {"access_token": token_for(u), "token_type": "bearer", "user": {"id": u.id, "name": u.name, "email": u.email}}

@router.get("/me")
def me(u=Depends(current_user)): return {"id":u.id,"name":u.name,"email":u.email}
