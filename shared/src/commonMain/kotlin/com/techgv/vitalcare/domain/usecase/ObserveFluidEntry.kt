package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.repository.FluidRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reactive single-entry lookup for editing — emits null when the entry is
 * deleted, and fresh values after an edit.
 */
class ObserveFluidEntry(private val repository: FluidRepository) {
    operator fun invoke(id: String): Flow<FluidEntry?> =
        repository.observeAll().map { entries -> entries.firstOrNull { it.id == id } }
}
