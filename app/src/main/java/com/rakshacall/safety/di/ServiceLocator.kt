package com.rakshacall.safety.di

import android.content.Context
import com.rakshacall.safety.core.consent.ConsentManager
import com.rakshacall.safety.core.permissions.PermissionStateManager
import com.rakshacall.safety.core.speech.LanguageAwareTacticEngine
import com.rakshacall.safety.data.firebase.FirebaseAuthRepository
import com.rakshacall.safety.data.local.database.RakshaDatabase
import com.rakshacall.safety.data.local.datastore.RakshaPreferences
import com.rakshacall.safety.data.repository.LocalEvidenceRepository
import com.rakshacall.safety.data.repository.LocalRiskRepository
import com.rakshacall.safety.data.repository.LocalSessionRepository
import com.rakshacall.safety.data.repository.LocalTrustedContactRepository
import com.rakshacall.safety.data.repository.LocalUserRepository
import com.rakshacall.safety.data.sync.SyncManager
import com.rakshacall.safety.domain.engine.LivenessEngine
import com.rakshacall.safety.domain.engine.LocalLivenessEngine
import com.rakshacall.safety.domain.engine.LocalVisualAnalysisEngine
import com.rakshacall.safety.domain.engine.ManipulationVelocityEngine
import com.rakshacall.safety.domain.engine.RiskEngine
import com.rakshacall.safety.domain.engine.RiskFusionEngine
import com.rakshacall.safety.domain.engine.ScamStageMachine
import com.rakshacall.safety.domain.engine.VisualAnalysisEngine
import com.rakshacall.safety.domain.repository.EvidenceRepository
import com.rakshacall.safety.domain.repository.RiskRepository
import com.rakshacall.safety.domain.repository.SessionRepository
import com.rakshacall.safety.domain.repository.TrustedContactRepository
import com.rakshacall.safety.domain.repository.UserRepository
import com.rakshacall.safety.domain.usecase.AlertTrustedContactUseCase
import com.rakshacall.safety.domain.usecase.AnalyzeRiskUseCase
import com.rakshacall.safety.domain.usecase.CalculateManipulationVelocityUseCase
import com.rakshacall.safety.domain.usecase.CreateEvidenceEventUseCase
import com.rakshacall.safety.domain.usecase.DeleteUserDataUseCase
import com.rakshacall.safety.domain.usecase.FuseRiskSignalsUseCase
import com.rakshacall.safety.domain.usecase.ProcessTranscriptUseCase
import com.rakshacall.safety.domain.usecase.StartProtectionSessionUseCase
import com.rakshacall.safety.domain.usecase.TriggerSafetyBrakeUseCase
import com.rakshacall.safety.domain.usecase.UpdateScamStageUseCase
import com.rakshacall.safety.domain.usecase.VerifyEvidenceIntegrityUseCase

/**
 * Dependency Injection container and modular ServiceLocator.
 * Provides constructor-injectable UseCases and clean architectural boundaries.
 */
object ServiceLocator {

    @Volatile
    private var isInitialized = false

    lateinit var preferences: RakshaPreferences
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var sessionRepository: SessionRepository
        private set

    lateinit var riskRepository: RiskRepository
        private set

    lateinit var evidenceRepository: EvidenceRepository
        private set

    lateinit var trustedContactRepository: TrustedContactRepository
        private set

    lateinit var consentManager: ConsentManager
        private set

    lateinit var permissionStateManager: PermissionStateManager
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var protectionRepository: com.rakshacall.safety.domain.repository.ProtectionRepository
        private set

    lateinit var authenticationProvider: com.rakshacall.safety.domain.provider.AuthenticationProvider
        private set

    // Core Domain Engines
    val riskEngine: RiskEngine by lazy { RiskEngine() }
    val scamStageMachine: ScamStageMachine by lazy { ScamStageMachine() }
    val velocityEngine: ManipulationVelocityEngine by lazy { ManipulationVelocityEngine() }
    val visualAnalysisEngine: VisualAnalysisEngine by lazy { LocalVisualAnalysisEngine() }
    val livenessEngine: LivenessEngine by lazy { LocalLivenessEngine() }
    val riskFusionEngine: RiskFusionEngine by lazy { RiskFusionEngine() }
    val languageAwareTacticEngine: LanguageAwareTacticEngine by lazy { LanguageAwareTacticEngine() }

    val mediaSourceRegistry: com.rakshacall.safety.data.media.MediaSourceRegistry by lazy { com.rakshacall.safety.data.media.MediaSourceRegistry() }

    // Domain UseCases
    val startProtectionSessionUseCase: StartProtectionSessionUseCase by lazy {
        StartProtectionSessionUseCase(sessionRepository, evidenceRepository)
    }
    val processTranscriptUseCase: ProcessTranscriptUseCase by lazy {
        ProcessTranscriptUseCase(riskRepository, riskEngine)
    }
    val analyzeRiskUseCase: AnalyzeRiskUseCase by lazy {
        AnalyzeRiskUseCase(riskRepository, riskEngine)
    }
    val updateScamStageUseCase: UpdateScamStageUseCase by lazy {
        UpdateScamStageUseCase(scamStageMachine)
    }
    val calculateManipulationVelocityUseCase: CalculateManipulationVelocityUseCase by lazy {
        CalculateManipulationVelocityUseCase(velocityEngine)
    }
    val triggerSafetyBrakeUseCase: TriggerSafetyBrakeUseCase by lazy {
        TriggerSafetyBrakeUseCase(riskEngine)
    }
    val createEvidenceEventUseCase: CreateEvidenceEventUseCase by lazy {
        CreateEvidenceEventUseCase(evidenceRepository)
    }
    val verifyEvidenceIntegrityUseCase: VerifyEvidenceIntegrityUseCase by lazy {
        VerifyEvidenceIntegrityUseCase(evidenceRepository)
    }
    val fuseRiskSignalsUseCase: FuseRiskSignalsUseCase by lazy {
        FuseRiskSignalsUseCase(riskFusionEngine, velocityEngine)
    }
    val alertTrustedContactUseCase: AlertTrustedContactUseCase by lazy {
        AlertTrustedContactUseCase(trustedContactRepository)
    }
    val deleteUserDataUseCase: DeleteUserDataUseCase by lazy {
        DeleteUserDataUseCase(userRepository, sessionRepository, evidenceRepository, trustedContactRepository)
    }

    fun initialize(context: Context) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    val appContext = context.applicationContext
                    val database = RakshaDatabase.getInstance(appContext)

                    preferences = RakshaPreferences(appContext)
                    val localUserRepo = LocalUserRepository(database)
                    userRepository = FirebaseAuthRepository(localUserRepo)
                    sessionRepository = LocalSessionRepository(database)
                    riskRepository = LocalRiskRepository(database)
                    evidenceRepository = LocalEvidenceRepository(database)
                    trustedContactRepository = LocalTrustedContactRepository(database)

                    consentManager = ConsentManager(appContext)
                    permissionStateManager = PermissionStateManager(appContext)
                    syncManager = SyncManager(evidenceRepository)

                    com.rakshacall.safety.data.firebase.FirebaseManager.initialize(appContext)
                    protectionRepository = com.rakshacall.safety.data.firebase.ProtectionRepositoryImpl(
                        evidenceRepository = evidenceRepository,
                        sessionRepository = sessionRepository,
                        trustedContactRepository = trustedContactRepository
                    )
                    authenticationProvider = com.rakshacall.safety.data.provider.AuthenticationProviderImpl(
                        userRepository = userRepository,
                        firebaseEnabled = true
                    )

                    isInitialized = true
                }
            }
        }
    }
}
