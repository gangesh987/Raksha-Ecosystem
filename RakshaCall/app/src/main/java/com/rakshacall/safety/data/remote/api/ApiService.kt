package com.rakshacall.safety.data.remote.api

import com.rakshacall.safety.data.remote.dto.AuthSessionRequest
import com.rakshacall.safety.data.remote.dto.AuthSessionResponse
import com.rakshacall.safety.data.remote.dto.CreateSessionRequest
import com.rakshacall.safety.data.remote.dto.EventResponse
import com.rakshacall.safety.data.remote.dto.ExportEvidenceRequest
import com.rakshacall.safety.data.remote.dto.ExportEvidenceResponse
import com.rakshacall.safety.data.remote.dto.RemoteAppConfigResponse
import com.rakshacall.safety.data.remote.dto.RiskAnalyzeRequest
import com.rakshacall.safety.data.remote.dto.RiskAnalyzeResponse
import com.rakshacall.safety.data.remote.dto.SessionEventRequest
import com.rakshacall.safety.data.remote.dto.SessionResponse
import com.rakshacall.safety.data.remote.dto.SyncRequest
import com.rakshacall.safety.data.remote.dto.SyncResponse
import com.rakshacall.safety.data.remote.dto.TimelineResponse
import com.rakshacall.safety.data.remote.dto.TrustedContactAlertRequest
import com.rakshacall.safety.data.remote.dto.TrustedContactAlertResponse
import com.rakshacall.safety.data.remote.dto.VerifyEvidenceRequest
import com.rakshacall.safety.data.remote.dto.VerifyEvidenceResponse
import com.rakshacall.safety.data.remote.dto.AuthResponse
import com.rakshacall.safety.data.remote.dto.GoogleAuthRequest
import com.rakshacall.safety.data.remote.dto.LoginRequest
import com.rakshacall.safety.data.remote.dto.RegisterRequest
import com.rakshacall.safety.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Production Retrofit ApiService interface.
 * Implements all endpoints specified in the production architecture requirements.
 */
interface ApiService {

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("/api/auth/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @POST("/api/auth/google")
    suspend fun googleLogin(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("/v1/auth/session")
    suspend fun authenticateSession(@Body request: AuthSessionRequest): Response<AuthSessionResponse>

    @POST("/v1/sessions")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<SessionResponse>

    @POST("/v1/sessions/{id}/events")
    suspend fun sendSessionEvent(
        @Path("id") sessionId: String,
        @Body request: SessionEventRequest
    ): Response<EventResponse>

    @POST("/v1/risk/analyze")
    suspend fun analyzeRiskRemotely(@Body request: RiskAnalyzeRequest): Response<RiskAnalyzeResponse>

    @GET("/v1/sessions/{id}")
    suspend fun getSession(@Path("id") sessionId: String): Response<SessionResponse>

    @GET("/v1/sessions")
    suspend fun getSessions(): Response<List<SessionResponse>>

    @GET("/v1/sessions/{id}/timeline")
    suspend fun getSessionTimeline(@Path("id") sessionId: String): Response<TimelineResponse>

    @POST("/v1/evidence/{id}/verify")
    suspend fun verifyEvidence(
        @Path("id") sessionId: String,
        @Body request: VerifyEvidenceRequest
    ): Response<VerifyEvidenceResponse>

    @POST("/v1/evidence/{id}/export")
    suspend fun exportEvidence(
        @Path("id") sessionId: String,
        @Body request: ExportEvidenceRequest
    ): Response<ExportEvidenceResponse>

    @POST("/v1/trusted-contacts/alerts")
    suspend fun sendTrustedContactAlert(@Body request: TrustedContactAlertRequest): Response<TrustedContactAlertResponse>

    @POST("/v1/sync")
    suspend fun syncPendingEvents(@Body request: SyncRequest): Response<SyncResponse>

    @GET("/v1/config")
    suspend fun getRemoteConfig(): Response<RemoteAppConfigResponse>
}
