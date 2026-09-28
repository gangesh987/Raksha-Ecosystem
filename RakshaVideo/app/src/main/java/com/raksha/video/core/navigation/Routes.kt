package com.raksha.video.core.navigation

object Routes {
    const val Auth = "auth"
    const val Permissions = "permissions"
    const val Home = "home"
    const val Settings = "settings"
    const val SafetyCenter = "safety_center"
    const val CreateCall = "create_call"
    const val JoinCall = "join_call"
    const val Call = "call/{callId}/{caller}"
    fun call(callId: String, caller: Boolean) = "call/${android.net.Uri.encode(callId)}/$caller"
}
