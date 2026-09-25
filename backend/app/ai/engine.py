import re, time

TACTICS={
'AUTHORITY':(['cbi','police','supreme court','customs','enforcement directorate',' ed ','narcotics','trai','telecom authority','cyber crime cell','inspector','rbi'],15),
'FEAR':(['money laundering','parcel seized','passport suspended','illegal drugs','fir registered','arrest warrant','non-bailable warrant','under arrest','criminal case'],15),
'URGENCY':(['immediately','within 15 minutes','right now','urgent','instant transfer','no time to wait','do it now'],10),
'ISOLATION':(['do not disconnect','do not tell anyone','stay in a quiet room','quiet closed room','keep camera on','digital custody','stay on this call'],15),
'PAYMENT':(['clear your funds','security deposit','escrow account','rbi verification account','penalty fee','refund processing','transfer','send money','pay now','move your money'],20),
'CREDENTIAL':(['otp','cvv','upi pin','banking password','aadhaar otp','card pin','verification code'],20),
'REMOTE_ACCESS':(['anydesk','teamviewer','quicksupport','rustdesk','screen share','remote access'],15),
'SUSPICIOUS_LINK':(['click this link','open this link','download this app','apk','short url','verify-account'],10),
'ESCALATION':(['threatening arrest','seize your','penalty escalation','we will arrest','final warning'],10),
}

def _matches(t):
    out=[]
    for k,(phrases,_) in TACTICS.items():
        if any(p.strip() in t for p in phrases): out.append(k)
    return out

def analyze_conversation(text:str):
    t=text.lower()
    tactics=_matches(t)
    score=sum(TACTICS[k][1] for k in tactics)
    irreversible=any(x in tactics for x in ['PAYMENT','CREDENTIAL','REMOTE_ACCESS'])
    if len(tactics)>=3: score*=1.25
    score=min(100,score)
    reasons=[]
    labels={'AUTHORITY':'Authority impersonation','FEAR':'Criminal allegation/fear','URGENCY':'Urgency','ISOLATION':'Isolation/coercion','PAYMENT':'Payment pressure','CREDENTIAL':'Credential/OTP pressure','REMOTE_ACCESS':'Remote-access pressure','SUSPICIOUS_LINK':'Suspicious-link/app pressure','ESCALATION':'Escalation/coercion'}
    reasons=[labels[k]+' detected' for k in tactics]
    stage='CONTACT'
    order=['AUTHORITY','FEAR','ISOLATION','URGENCY','PAYMENT','CREDENTIAL','ESCALATION']
    if tactics:
        rank=max(order.index(k) for k in tactics if k in order)
        stage=['CONTACT','AUTHORITY','FEAR','ISOLATION','DEMAND','PAYMENT_CREDENTIAL','ESCALATION'][rank+1]
    return {'conversation_score':round(score/100,3),'tactics':tactics,'stage':stage,'irreversible_action':irreversible,'reasons':reasons}

def fuse(text,visual,liveness):
    conv=analyze_conversation(text)
    score=(conv['conversation_score']*.70)+(visual*.20)+((1-liveness)*.10)
    level='HIGH' if score>=.62 else 'MEDIUM' if score>=.32 else 'LOW'
    reasons=conv['reasons'] or ['No strong coercion pattern detected']
    return {'conversation_score':round(conv['conversation_score'],3),'visual_score':round(visual,3),'liveness_score':round(liveness,3),'fused_score':round(score,3),'risk_level':level,'reasons':reasons,'tactics':conv['tactics'],'stage':conv['stage'],'irreversible_action':conv['irreversible_action']}
