package com.raksha.video.safety

import com.raksha.video.protection.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

enum class SafetyState { NORMAL, MONITORING, CAUTION, WARNING, HIGH_RISK, USER_REVIEW, INTERVENTION_READY, INTERVENTION_ACTIVE, RESOLVED }
enum class SafetyRecommendation { NONE, SHOW_WARNING, REVIEW_SIGNALS, VERIFY_CALLER, PAUSE_AND_REVIEW, CONTACT_TRUSTED_PERSON, END_CALL, CREATE_EVIDENCE_SUMMARY }
enum class SafetyAction { REVIEW, PAUSE_REVIEW, RESUME_CALL, END_CALL, TRUSTED_CONTACT_ALERT, EXPORT_EVIDENCE }

data class SafetyDecision(val decisionId:String=UUID.randomUUID().toString(), val riskLevel:RiskLevel, val recommendation:SafetyRecommendation, val reasons:List<String>, val requiresConfirmation:Boolean=true, val timestamp:Long=System.currentTimeMillis())
data class SafetyAuditEvent(val id:String=UUID.randomUUID().toString(), val actor:String, val action:String, val reason:String?, val result:String, val timestamp:Long=System.currentTimeMillis())

class SafetyEngine {
    private val _state = MutableStateFlow(SafetyState.NORMAL)
    val state: StateFlow<SafetyState> = _state
    private val _decision = MutableStateFlow<SafetyDecision?>(null)
    val decision: StateFlow<SafetyDecision?> = _decision
    fun evaluate(level: RiskLevel, reasons: List<String>) {
        _state.value = when(level) { RiskLevel.CRITICAL -> SafetyState.HIGH_RISK; RiskLevel.HIGH -> SafetyState.HIGH_RISK; RiskLevel.MEDIUM -> SafetyState.CAUTION; RiskLevel.LOW -> SafetyState.MONITORING; RiskLevel.UNKNOWN -> SafetyState.MONITORING }
        _decision.value = SafetyDecision(riskLevel=level, recommendation=when(level){RiskLevel.CRITICAL, RiskLevel.HIGH->SafetyRecommendation.REVIEW_SIGNALS; RiskLevel.MEDIUM->SafetyRecommendation.SHOW_WARNING; else->SafetyRecommendation.NONE}, reasons=reasons)
    }
    fun setUserReview() { _state.value = SafetyState.USER_REVIEW }
    fun setInterventionActive() { _state.value = SafetyState.INTERVENTION_ACTIVE }
    fun resolve() { _state.value = SafetyState.RESOLVED }
}
