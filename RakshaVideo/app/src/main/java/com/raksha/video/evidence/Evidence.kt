package com.raksha.video.evidence

import java.security.MessageDigest
import org.json.JSONObject

enum class EvidenceType { CALL_STARTED, CALL_CONNECTED, PROTECTION_ENABLED, CONVERSATION_SIGNAL, VISUAL_SIGNAL, RISK_EVENT, USER_ACTION, SAFETY_ACTION, TRUSTED_CONTACT_ALERT, CALL_ENDED }
data class EvidenceItem(val id:String,val callId:String,val type:EvidenceType,val timestamp:Long,val description:String,val metadata:Map<String,String>,val hash:String?=null)
class EvidenceRepository {
    private val items=mutableListOf<EvidenceItem>()
    @Synchronized fun add(item:EvidenceItem):EvidenceItem { val h=EvidenceHasher.hash(item); val x=item.copy(hash=h); items+=x; return x }
    @Synchronized fun list(callId:String?=null):List<EvidenceItem> = items.filter { callId==null || it.callId==callId }.toList()
    @Synchronized fun clear(){items.clear()}
    fun exportJson(callId:String):String { val arr=org.json.JSONArray(); list(callId).forEach { e -> arr.put(JSONObject().apply { put("id",e.id); put("call_id",e.callId); put("type",e.type.name); put("timestamp",e.timestamp); put("description",e.description); put("metadata",JSONObject(e.metadata)); put("hash",e.hash) }) }; return JSONObject().put("call_id",callId).put("privacy",JSONObject().put("raw_audio_stored",false).put("raw_video_stored",false).put("raw_frames_stored",false)).put("evidence",arr).toString(2) }
}
object EvidenceHasher { fun hash(e:EvidenceItem):String { val canonical="${e.id}|${e.callId}|${e.type}|${e.timestamp}|${e.description}|${e.metadata.toSortedMap()}"; return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()).joinToString(""){ "%02x".format(it) } } }
