package com.techgv.vitalcare.feature.vitals

import com.techgv.vitalcare.FakeVitalsRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.domain.usecase.GetVitalRecord
import com.techgv.vitalcare.domain.usecase.SaveVitalRecord
import com.techgv.vitalcare.domain.validation.VitalField
import com.techgv.vitalcare.domain.validation.VitalsError
import com.techgv.vitalcare.domain.validation.VitalsValidator
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
class RecordVitalsViewModelTest {

    private val repository = FakeVitalsRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(recordId: String? = null): RecordVitalsViewModel {
        val validator = VitalsValidator(Fixtures.clock, Fixtures.timeZone)
        return RecordVitalsViewModel(
            recordId = recordId,
            saveVitalRecord = SaveVitalRecord(repository, validator, Fixtures.clock, Fixtures.timeZone),
            getVitalRecord = GetVitalRecord(repository),
            clock = Fixtures.clock,
            timeZone = Fixtures.timeZone,
        )
    }

    @Test
    fun fieldChangeUpdatesStateAndMarksDirty() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(RecordVitalsEvent.Spo2Changed("98"))

        assertEquals("98", viewModel.uiState.value.spo2)
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun numericFieldsFilterNonDigits() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(RecordVitalsEvent.HeartRateChanged("7a2!"))

        assertEquals("72", viewModel.uiState.value.heartRate)
    }

    @Test
    fun invalidSaveExposesFieldErrorsAndSavesNothing() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(RecordVitalsEvent.Spo2Changed("101"))
        viewModel.onEvent(RecordVitalsEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            VitalsError.SPO2_OUT_OF_RANGE,
            viewModel.uiState.value.fieldErrors[VitalField.SPO2],
        )
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(repository.current().isEmpty())
    }

    @Test
    fun validSaveEmitsSavedEffect() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(RecordVitalsEvent.Spo2Changed("98"))
        viewModel.onEvent(RecordVitalsEvent.SaveClicked)
        advanceUntilIdle()

        assertIs<RecordVitalsEffect.Saved>(viewModel.effects.first())
        assertEquals(1, repository.current().size)
    }

    @Test
    fun editModePrefillsFromRepository() = runTest {
        repository.seed(
            Fixtures.record(id = "r1", spo2 = 95, heartRate = null, remarks = "morning"),
        )
        val viewModel = createViewModel(recordId = "r1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEdit)
        assertFalse(state.isLoading)
        assertEquals("95", state.spo2)
        assertEquals("", state.heartRate)
        assertEquals("morning", state.remarks)
        assertFalse(state.isDirty)
    }

    @Test
    fun closeWhenDirtyAsksForDiscardConfirmation() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(RecordVitalsEvent.Spo2Changed("98"))
        viewModel.onEvent(RecordVitalsEvent.CloseClicked)

        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun closeWhenPristineClosesImmediately() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(RecordVitalsEvent.CloseClicked)
        advanceUntilIdle()

        assertIs<RecordVitalsEffect.Close>(viewModel.effects.first())
    }
}
