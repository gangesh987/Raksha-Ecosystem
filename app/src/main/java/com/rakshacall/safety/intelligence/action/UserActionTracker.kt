package com.rakshacall.safety.intelligence.action

enum class SafetyUserActionType {
    OPENED_PAYMENT_INTERFACE,
    ATTEMPTED_SCREEN_SHARE,
    ATTEMPTED_REMOTE_ACCESS,
    COPIED_SUSPICIOUS_INFO,
    PRESSED_SAFETY_BRAKE_PAUSE,
    DISMISSED_WARNING,
    USER_OVERRIDE_KEEP_CALL,
    INITIATED_VERIFICATION
}

data class SafetyUserAction(
    val actionType: SafetyUserActionType,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String? = null
)

/**
 * Tracks safety-relevant user actions during active call sessions (Phase 17).
 * Feeds supporting context to the Evidence Graph without inferring intent from
 * benign actions in isolation.
 */
class UserActionTracker {

    private val actions = mutableListOf<SafetyUserAction>()

    fun recordAction(actionType: SafetyUserActionType, details: String? = null) {
        actions.add(SafetyUserAction(actionType = actionType, details = details))
    }

    fun getActions(): List<SafetyUserAction> = actions.toList()

    fun hasCriticalUserAction(): Boolean {
        return actions.any {
            it.actionType == SafetyUserActionType.OPENED_PAYMENT_INTERFACE ||
            it.actionType == SafetyUserActionType.ATTEMPTED_SCREEN_SHARE ||
            it.actionType == SafetyUserActionType.ATTEMPTED_REMOTE_ACCESS
        }
    }

    fun clear() {
        actions.clear()
    }
}
