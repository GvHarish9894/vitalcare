package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.Fixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class MergeFluidEntriesTest {

    private val merge = MergeFluidEntries()

    @Test
    fun addsIncomingNotPresentLocally() {
        val result = merge(
            local = listOf(Fixtures.fluid(id = "a")),
            incoming = listOf(Fixtures.fluid(id = "b")),
        )
        assertEquals(listOf("b"), result.map { it.id })
    }

    @Test
    fun newerIncomingWinsOlderLocalIgnored() {
        val result = merge(
            local = listOf(Fixtures.fluid(id = "a", updatedAt = 100L)),
            incoming = listOf(
                Fixtures.fluid(id = "a", amountMl = 999, updatedAt = 500L), // newer → write
                Fixtures.fluid(id = "b", updatedAt = 50L), // brand new → write
            ),
        )
        assertEquals(setOf("a", "b"), result.map { it.id }.toSet())
        assertEquals(999, result.first { it.id == "a" }.amountMl)
    }

    @Test
    fun olderIncomingNeverOverwritesNewerLocal() {
        val result = merge(
            local = listOf(Fixtures.fluid(id = "a", updatedAt = 500L)),
            incoming = listOf(Fixtures.fluid(id = "a", updatedAt = 100L)),
        )
        assertEquals(emptyList(), result)
    }

    @Test
    fun idempotentWhenBackupEqualsLocal() {
        val entries = listOf(Fixtures.fluid(id = "a", updatedAt = 100L))
        assertEquals(emptyList(), merge(local = entries, incoming = entries))
    }
}
