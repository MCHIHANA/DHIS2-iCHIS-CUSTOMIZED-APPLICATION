package org.dhis2.usescases.vitaldashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import org.dhis2.R
import org.dhis2.usescases.general.FragmentGlobalAbstract
import org.dhis2.usescases.vitaldashboard.model.VitalSignType
import org.dhis2.usescases.vitaldashboard.ui.*
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import javax.inject.Inject

/**
 * Vital Signs Dashboard Fragment
 * 
 * Displays real-time patient vital signs monitoring dashboard for authorized healthcare workers.
 * Features:
 * - Patient summary cards
 * - Recent measurements timeline
 * - Trend visualizations
 * - Medical alerts
 * - Offline-first support
 * 
 * Access: Restricted to Doctors, Clinicians, and Administrators only
 * 
 * @author Shadreck Mkandawire
 */
class VitalDashboardFragment : FragmentGlobalAbstract() {

    @Inject
    lateinit var viewModelFactory: VitalDashboardViewModelFactory

    private val viewModel: VitalDashboardViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inject dependencies
        (activity as? MainActivity)?.mainComponent
            ?.plus(VitalDashboardModule())
            ?.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DHIS2Theme {
                    VitalDashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboardData()
    }

    companion object {
        fun newInstance() = VitalDashboardFragment()
    }
}

@Composable
fun VitalDashboardScreen(viewModel: VitalDashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            VitalDashboardTopBar(
                onRefresh = { viewModel.refreshData() },
                onFilterClick = { viewModel.showFilterDialog() }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is VitalDashboardUiState.Loading -> {
                LoadingScreen(modifier = Modifier.padding(paddingValues))
            }
            is VitalDashboardUiState.Success -> {
                val data = (uiState as VitalDashboardUiState.Success).data
                VitalDashboardContent(
                    data = data,
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    onPatientClick = { viewModel.navigateToPatientDetails(it) },
                    onVitalSignClick = { viewModel.showVitalSignDetails(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is VitalDashboardUiState.Error -> {
                ErrorScreen(
                    message = (uiState as VitalDashboardUiState.Error).message,
                    onRetry = { viewModel.loadDashboardData() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is VitalDashboardUiState.Unauthorized -> {
                UnauthorizedScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is VitalDashboardUiState.Empty -> {
                EmptyDashboardScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalDashboardTopBar(
    onRefresh: () -> Unit,
    onFilterClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Vital Signs Dashboard") },
        actions = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.FilterList,
                    contentDescription = "Filter"
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun VitalDashboardContent(
    data: VitalDashboardData,
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    onPatientClick: (String) -> Unit,
    onVitalSignClick: (VitalSignType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Alert Summary Section
        if (data.alerts.isNotEmpty()) {
            AlertSummarySection(
                alerts = data.alerts,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            DashboardTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(tab.title) }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            DashboardTab.OVERVIEW -> {
                OverviewTabContent(
                    data = data,
                    onPatientClick = onPatientClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            DashboardTab.TRENDS -> {
                TrendsTabContent(
                    data = data,
                    onVitalSignClick = onVitalSignClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            DashboardTab.RECENT -> {
                RecentMeasurementsTabContent(
                    measurements = data.recentMeasurements,
                    onPatientClick = onPatientClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            DashboardTab.ALERTS -> {
                AlertsTabContent(
                    alerts = data.alerts,
                    onPatientClick = onPatientClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Loading vital signs data...")
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Error Loading Dashboard",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun UnauthorizedScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Access Restricted",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "You do not have permission to access the Vital Signs Dashboard. This feature is restricted to Doctors, Clinicians, and Administrators only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyDashboardScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No Data Available",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "No vital signs data has been recorded yet. Start capturing patient measurements to see them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class DashboardTab(val title: String) {
    OVERVIEW("Overview"),
    TRENDS("Trends"),
    RECENT("Recent"),
    ALERTS("Alerts")
}
