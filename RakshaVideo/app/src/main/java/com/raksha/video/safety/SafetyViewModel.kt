package com.raksha.video.safety

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raksha.video.protection.RiskEvent
import com.raksha.video.protection.RiskLevel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SafetyViewModel(app:Application):AndroidViewModel(app){
    private val engine=SafetyEngine(); private val client=SafetyClient()
    val state=engine.state; val decision=engine.decision
    private val _lastAction=MutableStateFlow<String?>(null); val lastAction=_lastAction.asStateFlow()
    fun onRisk(risk:RiskEvent){engine.evaluate(risk.level,risk.reasons)}
    fun review(){engine.setUserReview(); _lastAction.value="Risk details opened"}
    fun pause(){engine.setInterventionActive(); _lastAction.value="Pause & Review requested"}
    fun action(sessionId:String?,callId:String,action:String,contactId:String?=null){ if(sessionId==null){_lastAction.value="Safety action unavailable: no protection session";return}; viewModelScope.launch{client.submitAction(sessionId,callId,action,contactId).onSuccess{_lastAction.value="Safety action accepted"}.onFailure{_lastAction.value="Safety action unavailable"}} }
}
