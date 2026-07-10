package com.techgv.vitalcare.data.repository

import com.techgv.vitalcare.data.local.FluidEntryEntity
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.model.FluidType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun FluidEntryEntity.toDomain(): FluidEntry = FluidEntry(
    id = id,
    date = LocalDate.parse(date),
    time = LocalTime.parse(time),
    type = FluidType.valueOf(type),
    amountMl = amountMl,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FluidEntry.toEntity(): FluidEntryEntity = FluidEntryEntity(
    id = id,
    date = date.toString(),
    time = time.toString(),
    type = type.name,
    amountMl = amountMl,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
