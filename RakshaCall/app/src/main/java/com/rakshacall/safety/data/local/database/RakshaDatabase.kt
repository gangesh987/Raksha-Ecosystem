package com.rakshacall.safety.data.local.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.rakshacall.safety.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Local-first SQLite database supporting all 19 Room/SQLite entities,
 * reactive StateFlow triggers, and cryptographic SHA-256 evidence verification.
 */
class RakshaDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "rakshacall_security.db"
        const val DATABASE_VERSION = 3
        const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

        @Volatile
        private var INSTANCE: RakshaDatabase? = null

        fun getInstance(context: Context): RakshaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RakshaDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Reactive invalidation trackers
    private val _usersFlow = MutableStateFlow<UserEntity?>(null)
    private val _sessionsFlow = MutableStateFlow<List<ProtectionSessionEntity>>(emptyList())
    private val _callSessionsFlow = MutableStateFlow<List<CallSessionEntity>>(emptyList())
    private val _transcriptsFlow = MutableStateFlow<Map<String, List<TranscriptEventEntity>>>(emptyMap())
    private val _riskEventsFlow = MutableStateFlow<Map<String, List<RiskEventEntity>>>(emptyMap())
    private val _tacticEventsFlow = MutableStateFlow<Map<String, List<TacticEventEntity>>>(emptyMap())
    private val _stageEventsFlow = MutableStateFlow<Map<String, List<ScamStageEventEntity>>>(emptyMap())
    private val _riskPointsFlow = MutableStateFlow<Map<String, List<Pair<Long, Int>>>>(emptyMap())
    private val _evidenceFlow = MutableStateFlow<Map<String, List<EvidenceEventEntity>>>(emptyMap())
    private val _contactsFlow = MutableStateFlow<List<TrustedContactEntity>>(emptyList())
    private val _notificationsFlow = MutableStateFlow<List<NotificationEventEntity>>(emptyList())
    private val _verificationFlow = MutableStateFlow<Map<String, List<VerificationEventEntity>>>(emptyMap())
    private val _platformFlow = MutableStateFlow<List<PlatformConnectionEntity>>(emptyList())
    private val _incidentReportsFlow = MutableStateFlow<Map<String, IncidentReportEntity>>(emptyMap())

    override fun onCreate(db: SQLiteDatabase) {
        // 1. users
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                rakshaCallId TEXT NOT NULL,
                phoneNumber TEXT NOT NULL,
                email TEXT,
                authMode TEXT NOT NULL DEFAULT 'LOCAL_DEV',
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. protection_sessions
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS protection_sessions (
                id TEXT PRIMARY KEY,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                status TEXT NOT NULL,
                peakRisk INTEGER NOT NULL,
                finalRisk INTEGER NOT NULL,
                highestStage TEXT NOT NULL,
                inputSource TEXT NOT NULL,
                safetyBrakeTriggered INTEGER NOT NULL,
                totalTacticsDetected INTEGER NOT NULL,
                isDemoSession INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // 3. call_sessions
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS call_sessions (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                callerName TEXT NOT NULL,
                callerNumber TEXT NOT NULL,
                platform TEXT NOT NULL,
                callType TEXT NOT NULL,
                durationSeconds INTEGER NOT NULL,
                outcome TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_call_session ON call_sessions(sessionId)")

        // 4. media_sources
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS media_sources (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                connectionState TEXT NOT NULL,
                sampleRate INTEGER NOT NULL,
                isHardwareMuted INTEGER NOT NULL,
                connectedAt INTEGER NOT NULL
            )
        """.trimIndent())

        // 5. transcript_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS transcript_events (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                speaker TEXT NOT NULL,
                text TEXT NOT NULL,
                confidence REAL NOT NULL,
                language TEXT NOT NULL DEFAULT 'en-IN'
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transcript_session ON transcript_events(sessionId)")

        // 6. tactic_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tactic_events (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                tactic TEXT NOT NULL,
                confidence REAL NOT NULL,
                riskContribution INTEGER NOT NULL,
                evidenceText TEXT NOT NULL,
                modelProvider TEXT NOT NULL DEFAULT 'LOCAL'
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tactic_session ON tactic_events(sessionId)")

        // 7. scam_stage_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scam_stage_events (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                fromStage TEXT NOT NULL,
                toStage TEXT NOT NULL,
                triggeringTactic TEXT NOT NULL,
                reason TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stage_session ON scam_stage_events(sessionId)")

        // 8. risk_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS risk_events (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                tactic TEXT NOT NULL,
                confidence REAL NOT NULL,
                riskContribution INTEGER NOT NULL,
                evidenceText TEXT NOT NULL,
                cumulativeRisk INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_risk_session ON risk_events(sessionId)")

        // 9. risk_points
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS risk_points (
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                riskScore INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_risk_points ON risk_points(sessionId)")

        // 10. velocity_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS velocity_events (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                velocityLevel TEXT NOT NULL,
                tacticCount INTEGER NOT NULL,
                weightedSum REAL NOT NULL,
                durationMs INTEGER NOT NULL
            )
        """.trimIndent())

        // 11. trusted_contacts
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS trusted_contacts (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                phoneNumber TEXT NOT NULL,
                relationship TEXT NOT NULL,
                consentStatus TEXT NOT NULL DEFAULT 'CONSENTED',
                isEmergency INTEGER NOT NULL,
                lastAlertTimestamp INTEGER,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        // 12. alert_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS alert_events (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                contactId TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL,
                alertType TEXT NOT NULL,
                deliveryStatus TEXT NOT NULL DEFAULT 'ALERT_REQUESTED',
                payload TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())

        // 13. evidence_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS evidence_events (
                eventId TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                eventType TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                previousHash TEXT NOT NULL,
                currentHash TEXT NOT NULL,
                syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
                serverId TEXT,
                version INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_evidence_session ON evidence_events(sessionId)")

        // 14. consent_records
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS consent_records (
                id TEXT PRIMARY KEY,
                permissionName TEXT NOT NULL,
                grantedTimestamp INTEGER NOT NULL,
                revokedTimestamp INTEGER,
                policyVersion TEXT NOT NULL DEFAULT '2.0.0'
            )
        """.trimIndent())

        // 15. verification_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS verification_events (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                stepNumber INTEGER NOT NULL,
                stepName TEXT NOT NULL,
                status TEXT NOT NULL,
                notes TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())

        // 16. platform_connections
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS platform_connections (
                id TEXT PRIMARY KEY,
                platformName TEXT NOT NULL,
                protectionMethod TEXT NOT NULL,
                isSupported INTEGER NOT NULL,
                permissionStatus TEXT NOT NULL,
                lastUsedAt INTEGER
            )
        """.trimIndent())

        // 17. ai_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ai_events (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                providerName TEXT NOT NULL,
                modelName TEXT NOT NULL,
                latencyMs INTEGER NOT NULL,
                status TEXT NOT NULL,
                errorDetails TEXT
            )
        """.trimIndent())

        // 18. incident_reports
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS incident_reports (
                id TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                generatedAt INTEGER NOT NULL,
                summaryJson TEXT NOT NULL,
                humanReportText TEXT NOT NULL,
                integrityStatus TEXT NOT NULL,
                exportFormat TEXT NOT NULL DEFAULT 'JSON_AND_TEXT'
            )
        """.trimIndent())

        // 19. notification_events
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS notification_events (
                id TEXT PRIMARY KEY,
                timestamp INTEGER NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                priority TEXT NOT NULL DEFAULT 'HIGH',
                category TEXT NOT NULL DEFAULT 'RISK_ALERT',
                isRead INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            onCreate(db)
        }
    }

    // ==========================================
    // 1. USER OPERATIONS
    // ==========================================
    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", user.id)
            put("rakshaCallId", user.rakshaCallId)
            put("phoneNumber", user.phoneNumber)
            put("email", user.email)
            put("authMode", user.authMode)
            put("createdAt", user.createdAt)
        }
        writableDatabase.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        _usersFlow.value = user
    }

    suspend fun getUser(): UserEntity? = withContext(Dispatchers.IO) {
        val cursor = readableDatabase.query("users", null, null, null, null, null, "createdAt DESC", "1")
        cursor.use {
            if (it.moveToFirst()) {
                val user = cursorToUser(it)
                _usersFlow.value = user
                user
            } else {
                _usersFlow.value = null
                null
            }
        }
    }

    fun observeUser(): Flow<UserEntity?> = _usersFlow.asStateFlow()

    suspend fun deleteUser() = withContext(Dispatchers.IO) {
        writableDatabase.delete("users", null, null)
        _usersFlow.value = null
    }

    // ==========================================
    // 2. SESSION OPERATIONS
    // ==========================================
    suspend fun insertSession(session: ProtectionSessionEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", session.id)
            put("startTime", session.startTime)
            put("endTime", session.endTime)
            put("status", session.status)
            put("peakRisk", session.peakRisk)
            put("finalRisk", session.finalRisk)
            put("highestStage", session.highestStage)
            put("inputSource", session.inputSource)
            put("safetyBrakeTriggered", if (session.safetyBrakeTriggered) 1 else 0)
            put("totalTacticsDetected", session.totalTacticsDetected)
            put("isDemoSession", if (session.isDemoSession) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("protection_sessions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshSessions()
    }

    suspend fun getSession(id: String): ProtectionSessionEntity? = withContext(Dispatchers.IO) {
        val cursor = readableDatabase.query("protection_sessions", null, "id = ?", arrayOf(id), null, null, null)
        cursor.use {
            if (it.moveToFirst()) cursorToSession(it) else null
        }
    }

    suspend fun getAllSessions(): List<ProtectionSessionEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ProtectionSessionEntity>()
        val cursor = readableDatabase.query("protection_sessions", null, null, null, null, null, "startTime DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToSession(it))
            }
        }
        _sessionsFlow.value = list
        list
    }

    fun observeSessions(): Flow<List<ProtectionSessionEntity>> = _sessionsFlow.asStateFlow()

    private suspend fun refreshSessions() {
        getAllSessions()
    }

    suspend fun deleteSession(id: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("protection_sessions", "id = ?", arrayOf(id))
        writableDatabase.delete("transcript_events", "sessionId = ?", arrayOf(id))
        writableDatabase.delete("risk_events", "sessionId = ?", arrayOf(id))
        writableDatabase.delete("tactic_events", "sessionId = ?", arrayOf(id))
        writableDatabase.delete("scam_stage_events", "sessionId = ?", arrayOf(id))
        writableDatabase.delete("evidence_events", "sessionId = ?", arrayOf(id))
        writableDatabase.delete("risk_points", "sessionId = ?", arrayOf(id))
        writableDatabase.delete("verification_events", "sessionId = ?", arrayOf(id))
        writableDatabase.delete("incident_reports", "sessionId = ?", arrayOf(id))
        refreshSessions()
    }

    // ==========================================
    // 3. CALL SESSIONS
    // ==========================================
    suspend fun insertCallSession(call: CallSessionEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", call.id)
            put("sessionId", call.sessionId)
            put("callerName", call.callerName)
            put("callerNumber", call.callerNumber)
            put("platform", call.platform)
            put("callType", call.callType)
            put("durationSeconds", call.durationSeconds)
            put("outcome", call.outcome)
            put("timestamp", call.timestamp)
        }
        writableDatabase.insertWithOnConflict("call_sessions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshCallSessions()
    }

    suspend fun getAllCallSessions(): List<CallSessionEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CallSessionEntity>()
        val cursor = readableDatabase.query("call_sessions", null, null, null, null, null, "timestamp DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    CallSessionEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                        callerName = it.getString(it.getColumnIndexOrThrow("callerName")),
                        callerNumber = it.getString(it.getColumnIndexOrThrow("callerNumber")),
                        platform = it.getString(it.getColumnIndexOrThrow("platform")),
                        callType = it.getString(it.getColumnIndexOrThrow("callType")),
                        durationSeconds = it.getLong(it.getColumnIndexOrThrow("durationSeconds")),
                        outcome = it.getString(it.getColumnIndexOrThrow("outcome")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        _callSessionsFlow.value = list
        list
    }

    fun observeCallSessions(): Flow<List<CallSessionEntity>> = _callSessionsFlow.asStateFlow()

    private suspend fun refreshCallSessions() {
        getAllCallSessions()
    }

    // ==========================================
    // 5. TRANSCRIPT OPERATIONS
    // ==========================================
    suspend fun insertTranscript(event: TranscriptEventEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", event.id)
            put("sessionId", event.sessionId)
            put("timestamp", event.timestamp)
            put("speaker", event.speaker)
            put("text", event.text)
            put("confidence", event.confidence)
            put("language", event.language)
        }
        writableDatabase.insertWithOnConflict("transcript_events", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshTranscripts(event.sessionId)
    }

    suspend fun getTranscripts(sessionId: String): List<TranscriptEventEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TranscriptEventEntity>()
        val cursor = readableDatabase.query("transcript_events", null, "sessionId = ?", arrayOf(sessionId), null, null, "timestamp ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TranscriptEventEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        speaker = it.getString(it.getColumnIndexOrThrow("speaker")),
                        text = it.getString(it.getColumnIndexOrThrow("text")),
                        confidence = it.getFloat(it.getColumnIndexOrThrow("confidence")),
                        language = it.getString(it.getColumnIndexOrThrow("language"))
                    )
                )
            }
        }
        val currentMap = _transcriptsFlow.value.toMutableMap()
        currentMap[sessionId] = list
        _transcriptsFlow.value = currentMap
        list
    }

    fun observeTranscripts(sessionId: String): Flow<List<TranscriptEventEntity>> {
        return _transcriptsFlow.map { it[sessionId] ?: emptyList() }
    }

    private suspend fun refreshTranscripts(sessionId: String) {
        getTranscripts(sessionId)
    }

    // ==========================================
    // 6. TACTIC EVENTS
    // ==========================================
    suspend fun insertTacticEvent(event: TacticEventEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", event.id)
            put("sessionId", event.sessionId)
            put("timestamp", event.timestamp)
            put("tactic", event.tactic)
            put("confidence", event.confidence)
            put("riskContribution", event.riskContribution)
            put("evidenceText", event.evidenceText)
            put("modelProvider", event.modelProvider)
        }
        writableDatabase.insertWithOnConflict("tactic_events", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshTacticEvents(event.sessionId)
    }

    suspend fun getTacticEvents(sessionId: String): List<TacticEventEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TacticEventEntity>()
        val cursor = readableDatabase.query("tactic_events", null, "sessionId = ?", arrayOf(sessionId), null, null, "timestamp ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TacticEventEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        tactic = it.getString(it.getColumnIndexOrThrow("tactic")),
                        confidence = it.getFloat(it.getColumnIndexOrThrow("confidence")),
                        riskContribution = it.getInt(it.getColumnIndexOrThrow("riskContribution")),
                        evidenceText = it.getString(it.getColumnIndexOrThrow("evidenceText")),
                        modelProvider = it.getString(it.getColumnIndexOrThrow("modelProvider"))
                    )
                )
            }
        }
        val map = _tacticEventsFlow.value.toMutableMap()
        map[sessionId] = list
        _tacticEventsFlow.value = map
        list
    }

    private suspend fun refreshTacticEvents(sessionId: String) {
        getTacticEvents(sessionId)
    }

    // ==========================================
    // 7. SCAM STAGE EVENTS
    // ==========================================
    suspend fun insertScamStageEvent(event: ScamStageEventEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", event.id)
            put("sessionId", event.sessionId)
            put("timestamp", event.timestamp)
            put("fromStage", event.fromStage)
            put("toStage", event.toStage)
            put("triggeringTactic", event.triggeringTactic)
            put("reason", event.reason)
        }
        writableDatabase.insertWithOnConflict("scam_stage_events", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshScamStageEvents(event.sessionId)
    }

    suspend fun getScamStageEvents(sessionId: String): List<ScamStageEventEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ScamStageEventEntity>()
        val cursor = readableDatabase.query("scam_stage_events", null, "sessionId = ?", arrayOf(sessionId), null, null, "timestamp ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ScamStageEventEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        fromStage = it.getString(it.getColumnIndexOrThrow("fromStage")),
                        toStage = it.getString(it.getColumnIndexOrThrow("toStage")),
                        triggeringTactic = it.getString(it.getColumnIndexOrThrow("triggeringTactic")),
                        reason = it.getString(it.getColumnIndexOrThrow("reason"))
                    )
                )
            }
        }
        val map = _stageEventsFlow.value.toMutableMap()
        map[sessionId] = list
        _stageEventsFlow.value = map
        list
    }

    private suspend fun refreshScamStageEvents(sessionId: String) {
        getScamStageEvents(sessionId)
    }

    // ==========================================
    // 8. RISK EVENTS & TRAJECTORY POINTS
    // ==========================================
    suspend fun insertRiskEvent(event: RiskEventEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", event.id)
            put("sessionId", event.sessionId)
            put("timestamp", event.timestamp)
            put("tactic", event.tactic)
            put("confidence", event.confidence)
            put("riskContribution", event.riskContribution)
            put("evidenceText", event.evidenceText)
            put("cumulativeRisk", event.cumulativeRisk)
        }
        writableDatabase.insertWithOnConflict("risk_events", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshRiskEvents(event.sessionId)
    }

    suspend fun getRiskEvents(sessionId: String): List<RiskEventEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<RiskEventEntity>()
        val cursor = readableDatabase.query("risk_events", null, "sessionId = ?", arrayOf(sessionId), null, null, "timestamp ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    RiskEventEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        tactic = it.getString(it.getColumnIndexOrThrow("tactic")),
                        confidence = it.getFloat(it.getColumnIndexOrThrow("confidence")),
                        riskContribution = it.getInt(it.getColumnIndexOrThrow("riskContribution")),
                        evidenceText = it.getString(it.getColumnIndexOrThrow("evidenceText")),
                        cumulativeRisk = it.getInt(it.getColumnIndexOrThrow("cumulativeRisk"))
                    )
                )
            }
        }
        val map = _riskEventsFlow.value.toMutableMap()
        map[sessionId] = list
        _riskEventsFlow.value = map
        list
    }

    private suspend fun refreshRiskEvents(sessionId: String) {
        getRiskEvents(sessionId)
    }

    suspend fun insertRiskPoint(sessionId: String, timestamp: Long, score: Int) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("sessionId", sessionId)
            put("timestamp", timestamp)
            put("riskScore", score)
        }
        writableDatabase.insert("risk_points", null, values)
        refreshRiskPoints(sessionId)
    }

    suspend fun getRiskPoints(sessionId: String): List<Pair<Long, Int>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Pair<Long, Int>>()
        val cursor = readableDatabase.query("risk_points", arrayOf("timestamp", "riskScore"), "sessionId = ?", arrayOf(sessionId), null, null, "timestamp ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(Pair(it.getLong(0), it.getInt(1)))
            }
        }
        val map = _riskPointsFlow.value.toMutableMap()
        map[sessionId] = list
        _riskPointsFlow.value = map
        list
    }

    private suspend fun refreshRiskPoints(sessionId: String) {
        getRiskPoints(sessionId)
    }

    fun observeRiskPoints(sessionId: String): Flow<List<Pair<Long, Int>>> {
        return _riskPointsFlow.map { it[sessionId] ?: emptyList() }
    }

    // ==========================================
    // 13. EVIDENCE CHAIN OPERATIONS & SHA-256 INTEGRITY
    // ==========================================
    suspend fun insertEvidence(event: EvidenceEventEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("eventId", event.eventId)
            put("sessionId", event.sessionId)
            put("timestamp", event.timestamp)
            put("eventType", event.eventType)
            put("payloadJson", event.payloadJson)
            put("previousHash", event.previousHash)
            put("currentHash", event.currentHash)
            put("syncState", event.syncState)
            put("serverId", event.serverId)
            put("version", event.version)
        }
        writableDatabase.insertWithOnConflict("evidence_events", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshEvidence(event.sessionId)
    }

    suspend fun getEvidenceForSession(sessionId: String): List<EvidenceEventEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<EvidenceEventEntity>()
        val cursor = readableDatabase.query("evidence_events", null, "sessionId = ?", arrayOf(sessionId), null, null, "timestamp ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    EvidenceEventEntity(
                        eventId = it.getString(it.getColumnIndexOrThrow("eventId")),
                        sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        eventType = it.getString(it.getColumnIndexOrThrow("eventType")),
                        payloadJson = it.getString(it.getColumnIndexOrThrow("payloadJson")),
                        previousHash = it.getString(it.getColumnIndexOrThrow("previousHash")),
                        currentHash = it.getString(it.getColumnIndexOrThrow("currentHash")),
                        syncState = try {
                            val idx = it.getColumnIndex("syncState")
                            if (idx >= 0 && !it.isNull(idx)) it.getString(idx) else "LOCAL_ONLY"
                        } catch (e: Exception) { "LOCAL_ONLY" },
                        serverId = try {
                            val idx = it.getColumnIndex("serverId")
                            if (idx >= 0 && !it.isNull(idx)) it.getString(idx) else null
                        } catch (e: Exception) { null },
                        version = try {
                            val idx = it.getColumnIndex("version")
                            if (idx >= 0 && !it.isNull(idx)) it.getInt(idx) else 1
                        } catch (e: Exception) { 1 }
                    )
                )
            }
        }
        val map = _evidenceFlow.value.toMutableMap()
        map[sessionId] = list
        _evidenceFlow.value = map
        list
    }

    suspend fun getLastEvidenceHash(sessionId: String): String = withContext(Dispatchers.IO) {
        val cursor = readableDatabase.query("evidence_events", arrayOf("currentHash"), "sessionId = ?", arrayOf(sessionId), null, null, "timestamp DESC", "1")
        cursor.use {
            if (it.moveToFirst()) it.getString(0) else GENESIS_HASH
        }
    }

    /**
     * Verifies the cryptographic integrity of the append-only evidence chain for a session.
     * Returns true if all SHA-256 links match from GENESIS to the latest record.
     */
    suspend fun verifyEvidenceChainIntegrity(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val events = getEvidenceForSession(sessionId)
        if (events.isEmpty()) return@withContext true

        var expectedPrevHash = GENESIS_HASH
        for (event in events) {
            if (event.previousHash != expectedPrevHash) {
                return@withContext false // Chain broken
            }
            val computedHash = calculateSha256(
                "${event.previousHash}:${event.eventId}:${event.timestamp}:${event.eventType}:${event.payloadJson}"
            )
            if (event.currentHash != computedHash) {
                return@withContext false // Content tampered
            }
            expectedPrevHash = event.currentHash
        }
        true
    }

    private fun calculateSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private suspend fun refreshEvidence(sessionId: String) {
        getEvidenceForSession(sessionId)
    }

    // ==========================================
    // 11. TRUSTED CONTACT OPERATIONS
    // ==========================================
    suspend fun insertContact(contact: TrustedContactEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", contact.id)
            put("name", contact.name)
            put("phoneNumber", contact.phoneNumber)
            put("relationship", contact.relationship)
            put("consentStatus", contact.consentStatus)
            put("isEmergency", if (contact.isEmergency) 1 else 0)
            put("lastAlertTimestamp", contact.lastAlertTimestamp)
            put("createdAt", contact.createdAt)
        }
        writableDatabase.insertWithOnConflict("trusted_contacts", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshContacts()
    }

    suspend fun getAllContacts(): List<TrustedContactEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TrustedContactEntity>()
        val cursor = readableDatabase.query("trusted_contacts", null, null, null, null, null, "createdAt ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TrustedContactEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        phoneNumber = it.getString(it.getColumnIndexOrThrow("phoneNumber")),
                        relationship = it.getString(it.getColumnIndexOrThrow("relationship")),
                        consentStatus = try {
                            val idx = it.getColumnIndex("consentStatus")
                            if (idx >= 0 && !it.isNull(idx)) it.getString(idx) else "CONSENTED"
                        } catch (e: Exception) { "CONSENTED" },
                        isEmergency = it.getInt(it.getColumnIndexOrThrow("isEmergency")) == 1,
                        lastAlertTimestamp = try {
                            val idx = it.getColumnIndex("lastAlertTimestamp")
                            if (idx >= 0 && !it.isNull(idx)) it.getLong(idx) else null
                        } catch (e: Exception) { null },
                        createdAt = it.getLong(it.getColumnIndexOrThrow("createdAt"))
                    )
                )
            }
        }
        _contactsFlow.value = list
        list
    }

    fun observeContacts(): Flow<List<TrustedContactEntity>> = _contactsFlow.asStateFlow()

    suspend fun deleteContact(id: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("trusted_contacts", "id = ?", arrayOf(id))
        refreshContacts()
    }

    private suspend fun refreshContacts() {
        getAllContacts()
    }

    // ==========================================
    // 15. VERIFICATION EVENTS
    // ==========================================
    suspend fun insertVerificationEvent(event: VerificationEventEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", event.id)
            put("sessionId", event.sessionId)
            put("timestamp", event.timestamp)
            put("stepNumber", event.stepNumber)
            put("stepName", event.stepName)
            put("status", event.status)
            put("notes", event.notes)
        }
        writableDatabase.insertWithOnConflict("verification_events", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshVerification(event.sessionId)
    }

    suspend fun getVerificationEvents(sessionId: String): List<VerificationEventEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<VerificationEventEntity>()
        val cursor = readableDatabase.query("verification_events", null, "sessionId = ?", arrayOf(sessionId), null, null, "stepNumber ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    VerificationEventEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        stepNumber = it.getInt(it.getColumnIndexOrThrow("stepNumber")),
                        stepName = it.getString(it.getColumnIndexOrThrow("stepName")),
                        status = it.getString(it.getColumnIndexOrThrow("status")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        val map = _verificationFlow.value.toMutableMap()
        map[sessionId] = list
        _verificationFlow.value = map
        list
    }

    private suspend fun refreshVerification(sessionId: String) {
        getVerificationEvents(sessionId)
    }

    // ==========================================
    // 18. INCIDENT REPORTS
    // ==========================================
    suspend fun insertIncidentReport(report: IncidentReportEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", report.id)
            put("sessionId", report.sessionId)
            put("generatedAt", report.generatedAt)
            put("summaryJson", report.summaryJson)
            put("humanReportText", report.humanReportText)
            put("integrityStatus", report.integrityStatus)
            put("exportFormat", report.exportFormat)
        }
        writableDatabase.insertWithOnConflict("incident_reports", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        val map = _incidentReportsFlow.value.toMutableMap()
        map[report.sessionId] = report
        _incidentReportsFlow.value = map
    }

    suspend fun getIncidentReport(sessionId: String): IncidentReportEntity? = withContext(Dispatchers.IO) {
        val cursor = readableDatabase.query("incident_reports", null, "sessionId = ?", arrayOf(sessionId), null, null, null)
        cursor.use {
            if (it.moveToFirst()) {
                IncidentReportEntity(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                    generatedAt = it.getLong(it.getColumnIndexOrThrow("generatedAt")),
                    summaryJson = it.getString(it.getColumnIndexOrThrow("summaryJson")),
                    humanReportText = it.getString(it.getColumnIndexOrThrow("humanReportText")),
                    integrityStatus = it.getString(it.getColumnIndexOrThrow("integrityStatus")),
                    exportFormat = it.getString(it.getColumnIndexOrThrow("exportFormat"))
                )
            } else null
        }
    }

    // ==========================================
    // 19. NOTIFICATIONS
    // ==========================================
    suspend fun insertNotification(notif: NotificationEventEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", notif.id)
            put("timestamp", notif.timestamp)
            put("title", notif.title)
            put("body", notif.body)
            put("priority", notif.priority)
            put("category", notif.category)
            put("isRead", if (notif.isRead) 1 else 0)
        }
        writableDatabase.insertWithOnConflict("notification_events", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshNotifications()
    }

    suspend fun getAllNotifications(): List<NotificationEventEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<NotificationEventEntity>()
        val cursor = readableDatabase.query("notification_events", null, null, null, null, null, "timestamp DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    NotificationEventEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        body = it.getString(it.getColumnIndexOrThrow("body")),
                        priority = it.getString(it.getColumnIndexOrThrow("priority")),
                        category = it.getString(it.getColumnIndexOrThrow("category")),
                        isRead = it.getInt(it.getColumnIndexOrThrow("isRead")) == 1
                    )
                )
            }
        }
        _notificationsFlow.value = list
        list
    }

    fun observeNotifications(): Flow<List<NotificationEventEntity>> = _notificationsFlow.asStateFlow()

    private suspend fun refreshNotifications() {
        getAllNotifications()
    }

    // ==========================================
    // CLEAN ALL DATA (Privacy Center Wipe)
    // ==========================================
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        writableDatabase.delete("users", null, null)
        writableDatabase.delete("protection_sessions", null, null)
        writableDatabase.delete("call_sessions", null, null)
        writableDatabase.delete("media_sources", null, null)
        writableDatabase.delete("transcript_events", null, null)
        writableDatabase.delete("tactic_events", null, null)
        writableDatabase.delete("scam_stage_events", null, null)
        writableDatabase.delete("risk_events", null, null)
        writableDatabase.delete("risk_points", null, null)
        writableDatabase.delete("velocity_events", null, null)
        writableDatabase.delete("trusted_contacts", null, null)
        writableDatabase.delete("alert_events", null, null)
        writableDatabase.delete("evidence_events", null, null)
        writableDatabase.delete("consent_records", null, null)
        writableDatabase.delete("verification_events", null, null)
        writableDatabase.delete("platform_connections", null, null)
        writableDatabase.delete("ai_events", null, null)
        writableDatabase.delete("incident_reports", null, null)
        writableDatabase.delete("notification_events", null, null)

        _usersFlow.value = null
        _sessionsFlow.value = emptyList()
        _callSessionsFlow.value = emptyList()
        _transcriptsFlow.value = emptyMap()
        _riskEventsFlow.value = emptyMap()
        _tacticEventsFlow.value = emptyMap()
        _stageEventsFlow.value = emptyMap()
        _riskPointsFlow.value = emptyMap()
        _evidenceFlow.value = emptyMap()
        _contactsFlow.value = emptyList()
        _notificationsFlow.value = emptyList()
        _verificationFlow.value = emptyMap()
        _incidentReportsFlow.value = emptyMap()
    }

    // Helpers
    private fun cursorToUser(c: Cursor): UserEntity {
        return UserEntity(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            rakshaCallId = c.getString(c.getColumnIndexOrThrow("rakshaCallId")),
            phoneNumber = c.getString(c.getColumnIndexOrThrow("phoneNumber")),
            email = if (c.isNull(c.getColumnIndexOrThrow("email"))) null else c.getString(c.getColumnIndexOrThrow("email")),
            authMode = try {
                val idx = c.getColumnIndex("authMode")
                if (idx >= 0 && !c.isNull(idx)) c.getString(idx) else "LOCAL_DEV"
            } catch (e: Exception) { "LOCAL_DEV" },
            createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt"))
        )
    }

    private fun cursorToSession(c: Cursor): ProtectionSessionEntity {
        return ProtectionSessionEntity(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            startTime = c.getLong(c.getColumnIndexOrThrow("startTime")),
            endTime = if (c.isNull(c.getColumnIndexOrThrow("endTime"))) null else c.getLong(c.getColumnIndexOrThrow("endTime")),
            status = c.getString(c.getColumnIndexOrThrow("status")),
            peakRisk = c.getInt(c.getColumnIndexOrThrow("peakRisk")),
            finalRisk = c.getInt(c.getColumnIndexOrThrow("finalRisk")),
            highestStage = c.getString(c.getColumnIndexOrThrow("highestStage")),
            inputSource = c.getString(c.getColumnIndexOrThrow("inputSource")),
            safetyBrakeTriggered = c.getInt(c.getColumnIndexOrThrow("safetyBrakeTriggered")) == 1,
            totalTacticsDetected = c.getInt(c.getColumnIndexOrThrow("totalTacticsDetected")),
            isDemoSession = try {
                val idx = c.getColumnIndex("isDemoSession")
                if (idx >= 0 && !c.isNull(idx)) c.getInt(idx) == 1 else false
            } catch (e: Exception) { false }
        )
    }
}
