package org.dhis2.usescases.vitaldashboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.dhis2.commons.vitals.VitalDashboardRefreshBus
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.usescases.vitaldashboard.model.VitalSignType
import org.dhis2.usescases.vitaldashboard.repository.VitalDashboardRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class VitalDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: VitalDashboardRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should expose unauthorized state when user cannot access dashboard`() {
        repository =
            mock {
                onBlocking { isUserAuthorized() } doReturn false
            }

        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(VitalDashboardUiState.Unauthorized, viewModel.uiState.value)
    }

    @Test
    fun `init should load dashboard data when user is authorized`() {
        val dashboardData =
            VitalDashboardData(
                patientSummaries =
                    listOf(
                        PatientVitalSummary(
                            patientUid = "patient-1",
                            patientName = "Jane Doe",
                            age = 32,
                            gender = "Female",
                            latestVitals =
                                mapOf(
                                    VitalSignType.TEMPERATURE to VitalValue("37.1", "C", 37.1f),
                            ),
                            hasAlerts = false,
                            lastMeasurementTime = 1L,
                            lastUpdatedTime = 1L,
                        ),
                    ),
                recentMeasurements = emptyList(),
                alerts = emptyList(),
                statistics =
                    VitalStatistics(
                        totalPatients = 1,
                        totalMeasurements = 1,
                        activeAlerts = 0,
                        measurementsToday = 1,
                        averagesByType = emptyMap(),
                    ),
                trends = emptyMap(),
            )

        repository =
            mock {
                onBlocking { isUserAuthorized() } doReturn true
                onBlocking { getDashboardData(VitalDashboardFilter()) } doReturn dashboardData
            }

        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is VitalDashboardUiState.Success)
        val successState = viewModel.uiState.value as VitalDashboardUiState.Success
        assertEquals("Jane Doe", successState.data.patientSummaries.first().patientName)
    }

    @Test
    fun `should refresh dashboard data when refresh bus emits`() {
        val initialData =
            VitalDashboardData(
                patientSummaries = emptyList(),
                recentMeasurements = emptyList(),
                alerts = emptyList(),
                statistics =
                    VitalStatistics(
                        totalPatients = 0,
                        totalMeasurements = 0,
                        activeAlerts = 0,
                        measurementsToday = 0,
                        averagesByType = emptyMap(),
                    ),
                trends = emptyMap(),
            )
        val refreshedData =
            VitalDashboardData(
                patientSummaries =
                    listOf(
                        PatientVitalSummary(
                            patientUid = "patient-2",
                            patientName = "John Doe",
                            age = 29,
                            gender = "Male",
                            latestVitals =
                                mapOf(
                                    VitalSignType.TEMPERATURE to VitalValue("36.8", "C", 36.8f),
                                ),
                            hasAlerts = false,
                            lastMeasurementTime = 2L,
                            lastUpdatedTime = 3L,
                        ),
                    ),
                recentMeasurements = emptyList(),
                alerts = emptyList(),
                statistics =
                    VitalStatistics(
                        totalPatients = 1,
                        totalMeasurements = 1,
                        activeAlerts = 0,
                        measurementsToday = 1,
                        averagesByType = emptyMap(),
                    ),
                trends = emptyMap(),
            )

        repository =
            mock {
                onBlocking { isUserAuthorized() } doReturn true
                onBlocking { getDashboardData(VitalDashboardFilter()) } doReturn initialData doReturn refreshedData
            }

        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(VitalDashboardUiState.Empty, viewModel.uiState.value)

        VitalDashboardRefreshBus.notifyRefresh()
        testDispatcher.scheduler.advanceTimeBy(400)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is VitalDashboardUiState.Success)
        val successState = viewModel.uiState.value as VitalDashboardUiState.Success
        assertEquals("John Doe", successState.data.patientSummaries.first().patientName)
    }

    private fun buildViewModel() =
        VitalDashboardViewModel(
            repository = repository,
            dispatchers =
                Dispatcher(
                    io = testDispatcher,
                    main = testDispatcher,
                    default = testDispatcher,
                ),
        )
}
