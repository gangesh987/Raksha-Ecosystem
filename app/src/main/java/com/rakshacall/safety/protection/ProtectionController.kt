package com.rakshacall.safety.protection

import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.intelligence.ProtectionDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real-Time Emergency Protection Controller.
 * Manages the evidence-driven state machine:
 * NORMAL -> MONITORING -> WARNING -> HIGH_RISK -> CRITICAL -> PROTECTION_PENDING -> PROTECTED -> USER_OVERRIDE -> CALL_ENDED.
 * Evaluates evidence strength and enforces the configurable Force-Cut & Protection Policy.
 */
class ProtectionController(
    initialPolicy: ProtectionPolicy = ProtectionPolicy(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val onAutoTerminate: () -> Unit = {}
) {

    private val _policy = MutableStateFlow(initialPolicy)
    val policy: StateFlow<ProtectionPolicy> = _policy.asStateFlow()

    private val _state = MutableStateFlow(ProtectionState.NORMAL)
    val state: StateFlow<ProtectionState> = _state.asStateFlow()

    private val _evidenceStrength = MutableStateFlow(EvidenceStrength.WEAK)
    val evidenceStrength: StateFlow<EvidenceStrength> = _evidenceStrength.asStateFlow()

    private val _countdownSecondsRemaining = MutableStateFlow(5)
    val countdownSecondsRemaining: StateFlow<Int> = _countdownSecondsRemaining.asStateFlow()

    private var countdownJob: Job? = null

    fun updatePolicy(newPolicy: ProtectionPolicy) {
        _policy.value = newPolicy
    }

    fun setUIMode(uiMode: UIMode) {
        _policy.value = _policy.value.copy(uiMode = uiMode)
    }

    fun setProtectionMode(mode: ProtectionMode, autoProtectConsent: Boolean = false) {
        _policy.value = _policy.value.copy(
            mode = mode,
            autoProtectEnabled = if (mode == ProtectionMode.AUTO_PROTECT) autoProtectConsent else false
        )
    }

    /**
     * Compute Evidence Strength based on independent signal categories (Section 19).
     */
    fun calculateEvidenceStrength(
        decision: ProtectionDecision,
        visualThreatCount: Int = 0
    ): EvidenceStrength {
        val tactics = decision.detectedTactics.map { it.tactic }.distinct()

        var independentCategories = 0
        if (tactics.contains(ScamTactic.AUTHORITY_IMPERSONATION)) independentCategories++
        if (tactics.contains(ScamTactic.CRIMINAL_ALLEGATION) || tactics.contains(ScamTactic.ESCALATION)) independentCategories++
        if (tactics.contains(ScamTactic.ISOLATION)) independentCategories++
        if (tactics.any { it.isIrreversibleAction }) independentCategories++
        if (visualThreatCount > 0) independentCategories++

        val strength = when {
            independentCategories >= 3 && decision.confidence >= 0.85f -> EvidenceStrength.VERY_STRONG
            independentCategories >= 2 -> EvidenceStrength.STRONG
            tactics.isNotEmpty() -> EvidenceStrength.MODERATE
            else -> EvidenceStrength.WEAK
        }
        _evidenceStrength.value = strength
        return strength
    }

    /**
     * Evaluate incoming real-time AI decision against the active protection policy.
     */
    fun evaluate(
        decision: ProtectionDecision,
        visualThreatCount: Int = 0
    ) {
        val current = _state.value
        if (current == ProtectionState.CALL_ENDED || current == ProtectionState.PROTECTED) return

        val strength = calculateEvidenceStrength(decision, visualThreatCount)
        val score = decision.riskScore
        val pol = _policy.value

        // If user explicitly chose KEEP CALL, maintain USER_OVERRIDE state
        if (current == ProtectionState.USER_OVERRIDE) {
            return
        }

        when {
            // CRITICAL THREAT STATE
            score >= 80 || decision.riskLevel == RiskLevel.CRITICAL -> {
                val hasDangerousAction = decision.detectedTactics.any { it.tactic.isIrreversibleAction }

                // Check Force-Cut / Auto-Protect Policy (Section 4):
                // AUTO_PROTECT enabled + VERY_STRONG evidence + High Confidence + Dangerous Action Request
                if (pol.mode == ProtectionMode.AUTO_PROTECT &&
                    pol.autoProtectEnabled &&
                    strength == EvidenceStrength.VERY_STRONG &&
                    hasDangerousAction &&
                    decision.confidence >= 0.85f
                ) {
                    if (_state.value != ProtectionState.PROTECTION_PENDING) {
                        startProtectionCountdown(pol.countdownSeconds)
                    }
                } else {
                    _state.value = ProtectionState.CRITICAL
                }
            }

            // HIGH RISK STATE
            score >= 60 || decision.riskLevel == RiskLevel.HIGH -> {
                _state.value = ProtectionState.HIGH_RISK
            }

            // WARNING STATE (MEDIUM RISK)
            score >= 40 || decision.riskLevel == RiskLevel.MEDIUM -> {
                _state.value = ProtectionState.WARNING
            }

            // MONITORING / LOW RISK
            score >= 20 || decision.detectedTactics.isNotEmpty() -> {
                _state.value = ProtectionState.MONITORING
            }

            // NORMAL
            else -> {
                if (_state.value != ProtectionState.MONITORING) {
                    _state.value = ProtectionState.NORMAL
                }
            }
        }
    }

    /**
     * Start the 5-second Force-Cut Protection Countdown (Section 4).
     */
    private fun startProtectionCountdown(seconds: Int) {
        countdownJob?.cancel()
        _state.value = ProtectionState.PROTECTION_PENDING
        _countdownSecondsRemaining.value = seconds

        countdownJob = scope.launch {
            var remaining = seconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _countdownSecondsRemaining.value = remaining
            }

            if (isActive && _state.value == ProtectionState.PROTECTION_PENDING) {
                _state.value = ProtectionState.PROTECTED
                onAutoTerminate()
                _state.value = ProtectionState.CALL_ENDED
            }
        }
    }

    /**
     * User Override (Section 21): Cancel countdown, keep call, dismiss termination.
     */
    fun cancelCountdownAndKeepCall() {
        countdownJob?.cancel()
        countdownJob = null
        _state.value = ProtectionState.USER_OVERRIDE
    }

    /**
     * User manually dismisses warning banner.
     */
    fun dismissWarning() {
        if (_state.value == ProtectionState.WARNING || _state.value == ProtectionState.HIGH_RISK) {
            _state.value = ProtectionState.USER_OVERRIDE
        }
    }

    /**
     * User explicitly ends call.
     */
    fun endCallNow() {
        countdownJob?.cancel()
        countdownJob = null
        _state.value = ProtectionState.CALL_ENDED
    }

    fun reset() {
        countdownJob?.cancel()
        countdownJob = null
        _state.value = ProtectionState.NORMAL
        _evidenceStrength.value = EvidenceStrength.WEAK
        _countdownSecondsRemaining.value = 5
    }
}
