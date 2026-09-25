from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session as DBSession
from ..schemas import Register,Login
from ..models import User
from ..auth import hash_password,verify_password,token_for,db,current_user

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

@router.get("/me")
def me(u=Depends(current_user)): return {"id":u.id,"name":u.name,"email":u.email}
