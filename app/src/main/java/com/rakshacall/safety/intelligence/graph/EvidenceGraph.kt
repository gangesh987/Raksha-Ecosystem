package com.rakshacall.safety.intelligence.graph

import java.util.UUID

enum class EvidenceNodeType {
    CALLER_CLAIM,
    THREAT,
    URGENCY,
    ISOLATION,
    CREDENTIAL_REQUEST,
    PAYMENT_REQUEST,
    REMOTE_ACCESS,
    VISUAL_EVENT,
    USER_ACTION
}

enum class EvidenceEdgeRelationship {
    FOLLOWED_BY,
    ESCALATED_TO,
    SUPPORTS,
    CONTRADICTS,
    CORROBORATES
}

data class EvidenceNode(
    val id: String = UUID.randomUUID().toString(),
    val type: EvidenceNodeType,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val weight: Float = 1.0f
)

data class EvidenceEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val relationship: EvidenceEdgeRelationship,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Live Coercive Evidence Graph (Phase 13).
 * Represents structural relationships, causal escalations, and cross-modal corroborations.
 */
class EvidenceGraph {

    private val nodes = mutableListOf<EvidenceNode>()
    private val edges = mutableListOf<EvidenceEdge>()

    @Synchronized
    fun addNode(type: EvidenceNodeType, description: String, weight: Float = 1.0f): EvidenceNode {
        val node = EvidenceNode(type = type, description = description, weight = weight)
        val lastNode = nodes.lastOrNull()

        nodes.add(node)

        // Automatically connect sequential temporal link
        if (lastNode != null) {
            val relation = when {
                (lastNode.type == EvidenceNodeType.CALLER_CLAIM || lastNode.type == EvidenceNodeType.THREAT) &&
                        (type == EvidenceNodeType.CREDENTIAL_REQUEST || type == EvidenceNodeType.PAYMENT_REQUEST || type == EvidenceNodeType.REMOTE_ACCESS) ->
                    EvidenceEdgeRelationship.ESCALATED_TO

                lastNode.type == type -> EvidenceEdgeRelationship.CORROBORATES

                else -> EvidenceEdgeRelationship.FOLLOWED_BY
            }
            edges.add(EvidenceEdge(fromNodeId = lastNode.id, toNodeId = node.id, relationship = relation))
        }

        return node
    }

    @Synchronized
    fun addEdge(fromId: String, toId: String, relationship: EvidenceEdgeRelationship) {
        edges.add(EvidenceEdge(fromNodeId = fromId, toNodeId = toId, relationship = relationship))
    }

    @Synchronized
    fun getNodes(): List<EvidenceNode> = nodes.toList()

    @Synchronized
    fun getEdges(): List<EvidenceEdge> = edges.toList()

    /**
     * Measure graph corroboration score based on node diversity and escalation edges.
     */
    @Synchronized
    fun calculateCorroborationScore(): Float {
        if (nodes.isEmpty()) return 0.0f

        val distinctTypes = nodes.map { it.type }.distinct().size
        val escalationCount = edges.count { it.relationship == EvidenceEdgeRelationship.ESCALATED_TO }

        val typeScore = (distinctTypes.toFloat() / 5.0f).coerceIn(0f, 1f)
        val escalationScore = (escalationCount.toFloat() / 3.0f).coerceIn(0f, 1f)

        return (typeScore * 0.6f + escalationScore * 0.4f).coerceIn(0f, 1f)
    }

    @Synchronized
    fun hasCriticalEscalation(): Boolean {
        val types = nodes.map { it.type }
        val hasIrreversibleDemand = types.contains(EvidenceNodeType.CREDENTIAL_REQUEST) ||
                types.contains(EvidenceNodeType.PAYMENT_REQUEST) ||
                types.contains(EvidenceNodeType.REMOTE_ACCESS)

        val hasPressure = types.contains(EvidenceNodeType.THREAT) ||
                types.contains(EvidenceNodeType.ISOLATION) ||
                types.contains(EvidenceNodeType.CALLER_CLAIM)

        return hasIrreversibleDemand && hasPressure
    }

    @Synchronized
    fun clear() {
        nodes.clear()
        edges.clear()
    }
}
