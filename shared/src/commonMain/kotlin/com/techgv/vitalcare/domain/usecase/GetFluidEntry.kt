package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.repository.FluidRepository

class GetFluidEntry(private val repository: FluidRepository) {
    suspend operator fun invoke(id: String): AppResult<FluidEntry> = repository.getById(id)
}
