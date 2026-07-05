package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reactive single-record lookup for Record Details — emits null when the
 * record is deleted, and fresh values after an edit (FR-D5 semantics).
 */
class ObserveVitalRecord(private val repository: VitalsRepository) {
    operator fun invoke(id: String): Flow<VitalRecord?> =
        repository.observeAll().map { records -> records.firstOrNull { it.id == id } }
}
