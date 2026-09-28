package com.rakshacall.safety.domain.provider

import com.rakshacall.safety.domain.model.User
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class OtpSent(val verificationId: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object ConfigurationRequired : AuthResult()
}

interface AuthenticationProvider {
    val currentUser: Flow<User?>
    val isConfigured: Boolean
    suspend fun requestPhoneOtp(phoneNumber: String): AuthResult
    suspend fun verifyPhoneOtp(verificationId: String, otp: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun signUpWithEmail(email: String, password: String): AuthResult
    suspend fun signOut()
    suspend fun deleteAccount(): Boolean
}
