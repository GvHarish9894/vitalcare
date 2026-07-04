package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.repository.VitalsRepository

class GetVitalRecord(private val repository: VitalsRepository) {
    suspend operator fun invoke(id: String): AppResult<VitalRecord> = repository.getById(id)
}
