package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.domain.model.FluidEntry

/**
 * Restore-merge policy for fluid entries (D-024/D-033), mirroring
 * [MergeBackupRecords]: union by id; when both sides share an id, the newer
 * `updatedAt` wins (LWW). Local-only entries are untouched; applying the same
 * backup twice writes nothing the second time.
 */
class MergeFluidEntries {

    /** Returns exactly the incoming entries that must be written locally. */
    operator fun invoke(
        local: List<FluidEntry>,
        incoming: List<FluidEntry>,
    ): List<FluidEntry> {
        val localById = local.associateBy { it.id }
        return incoming.filter { candidate ->
            val current = localById[candidate.id]
            current == null || candidate.updatedAt > current.updatedAt
        }
    }
}
