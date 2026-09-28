package com.raksha.video.trusted

data class TrustedContact(val id:String, val displayName:String, val phoneNumber:String?=null, val email:String?=null, val relationship:String?=null, val enabled:Boolean=true, val createdAt:Long=System.currentTimeMillis())
data class TrustedContactAlert(val alertId:String, val riskLevel:String, val callId:String, val reasons:List<String>, val timestamp:Long=System.currentTimeMillis())
