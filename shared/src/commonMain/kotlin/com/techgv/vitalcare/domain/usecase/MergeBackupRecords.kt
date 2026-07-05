package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.domain.model.VitalRecord

/**
 * Restore-merge policy (D-024): union by record id; when both sides share an
 * id, the newer `updatedAt` wins (LWW, D-007). Records that exist only
 * locally are never touched, so restore is non-destructive; applying the same
 * backup twice yields no writes the second time (idempotent).
 */
class MergeBackupRecords {

    /** Returns exactly the incoming records that must be written locally. */
    operator fun invoke(
        local: List<VitalRecord>,
        incoming: List<VitalRecord>,
    ): List<VitalRecord> {
        val localById = local.associateBy { it.id }
        return incoming.filter { candidate ->
            val current = localById[candidate.id]
            current == null || candidate.updatedAt > current.updatedAt
        }
    }
}
