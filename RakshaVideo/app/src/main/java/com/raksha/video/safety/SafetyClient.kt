package com.raksha.video.safety

import com.raksha.video.core.security.ProductionConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SafetyClient(private val baseUrl:String=ProductionConfig.backendHttpUrl, private val token:String=ProductionConfig.apiToken) {
    private val client=OkHttpClient()
    fun submitAction(sessionId:String, callId:String, action:String, contactId:String?=null, confirmed:Boolean=true):Result<String> = runCatching {
        val body=JSONObject().apply { put("action",action); put("call_id",callId); put("confirmed",confirmed); if(contactId!=null)put("contact_id",contactId); put("timestamp",System.currentTimeMillis()); put("idempotency_key",java.util.UUID.randomUUID().toString()) }.toString().toRequestBody("application/json".toMediaType())
        val req=Request.Builder().url("$baseUrl/api/protection/sessions/$sessionId/safety/actions").header("Authorization","Bearer $token").post(body).build()
        client.newCall(req).execute().use { r -> if(!r.isSuccessful) error("Safety action HTTP ${r.code}"); r.body?.string() ?: "{}" }
    }
}
