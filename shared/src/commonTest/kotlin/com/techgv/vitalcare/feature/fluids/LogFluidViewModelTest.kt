package com.techgv.vitalcare.feature.fluids

import com.techgv.vitalcare.FakeFluidRepository
import com.techgv.vitalcare.FakeSettingsRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.VolumeUnit
import com.techgv.vitalcare.domain.usecase.GetFluidEntry
import com.techgv.vitalcare.domain.usecase.SaveFluidEntry
import com.techgv.vitalcare.domain.validation.FluidField
import com.techgv.vitalcare.domain.validation.FluidValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LogFluidViewModelTest {

    private val repository = FakeFluidRepository()
    private val settings = FakeSettingsRepository()

    @BeforeTest
    fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(
        entryId: String? = null,
        initialType: FluidType? = null,
    ): LogFluidViewModel {
        val validator = FluidValidator(Fixtures.clock, Fixtures.timeZone)
        return LogFluidViewModel(
            entryId = entryId,
            initialType = initialType,
            saveFluidEntry = SaveFluidEntry(repository, validator, Fixtures.clock, Fixtures.timeZone),
            getFluidEntry = GetFluidEntry(repository),
            settingsRepository = settings,
            clock = Fixtures.clock,
            timeZone = Fixtures.timeZone,
        )
    }

    @Test
    fun initialTypePreselectsTheForm() = runTest {
        val viewModel = createViewModel(initialType = FluidType.OUTPUT)
        assertEquals(FluidType.OUTPUT, viewModel.uiState.value.type)
        // No initial type falls back to intake.
        assertEquals(FluidType.INTAKE, createViewModel().uiState.value.type)
    }

    @Test
    fun amountFieldKeepsDigitsAndSingleDecimal() = runTest {
        settings.setVolumeUnit(VolumeUnit.OZ)
        val viewModel = createViewModel()

        viewModel.onEvent(LogFluidEvent.AmountChanged("8.5x.2"))

        assertEquals("8.52", viewModel.uiState.value.amount)
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun presetSetsAmountInDisplayUnit() = runTest {
        settings.setVolumeUnit(VolumeUnit.ML)
        val viewModel = createViewModel()

        viewModel.onEvent(LogFluidEvent.PresetSelected(250))

        assertEquals("250", viewModel.uiState.value.amount)
    }

    @Test
    fun invalidSaveExposesErrorAndSavesNothing() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(LogFluidEvent.SaveClicked) // amount blank
        advanceUntilIdle()

        assertEquals(FluidField.AMOUNT, viewModel.uiState.value.fieldErrors.keys.single())
        assertTrue(repository.current().isEmpty())
    }

    @Test
    fun validSaveEmitsSavedAndPersists() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(LogFluidEvent.TypeChanged(FluidType.OUTPUT))
        viewModel.onEvent(LogFluidEvent.AmountChanged("300"))
        viewModel.onEvent(LogFluidEvent.SaveClicked)
        advanceUntilIdle()

        assertIs<LogFluidEffect.Saved>(viewModel.effects.first())
        val stored = repository.current().single()
        assertEquals(FluidType.OUTPUT, stored.type)
        assertEquals(300, stored.amountMl)
    }

    @Test
    fun editModePrefillsAmountInDisplayUnit() = runTest {
        settings.setVolumeUnit(VolumeUnit.ML)
        repository.seed(Fixtures.fluid(id = "f1", type = FluidType.INTAKE, amountMl = 400, note = "tea"))
        val viewModel = createViewModel(entryId = "f1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEdit)
        assertFalse(state.isLoading)
        assertEquals("400", state.amount)
        assertEquals("tea", state.note)
        assertEquals(FluidType.INTAKE, state.type)
    }
}
