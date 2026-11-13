package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.Band7ViewModel
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.screens.DeviceScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SleepScreen
import com.example.ui.screens.VitalsScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.Band7Theme

private sealed class BandRoute(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Overview : BandRoute("overview", R.string.nav_overview, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard)
    data object Vitals : BandRoute("vitals", R.string.nav_vitals, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder)
    data object Sleep : BandRoute("sleep", R.string.nav_sleep, Icons.Rounded.Bedtime, Icons.Outlined.Bedtime)
    data object Workouts : BandRoute("workouts", R.string.nav_workouts, Icons.Rounded.FitnessCenter, Icons.Outlined.FitnessCenter)
    data object Device : BandRoute("device", R.string.nav_device, Icons.Rounded.Watch, Icons.Outlined.Watch)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: Band7ViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            Band7Theme(darkTheme = uiState.userPrefs.darkTheme) {
                Band7AppShell(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Band7AppShell(
    viewModel: Band7ViewModel,
    uiState: com.example.ui.Band7UiState
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BandRoute.Overview.route
    val snackbarHostState = remember { SnackbarHostState() }

    val routes = listOf(
        BandRoute.Overview,
        BandRoute.Vitals,
        BandRoute.Sleep,
        BandRoute.Workouts,
        BandRoute.Device
    )

    LaunchedEffect(uiState.feedbackBanner) {
        uiState.feedbackBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.band_model_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            ConnectionStatusBadge(status = uiState.liveTelemetry.connectionStatus)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        routes.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = stringResource(id = item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        routes.forEach { item ->
                            val selected = currentRoute == item.route
                            val label = stringResource(id = item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_rail_${item.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = BandRoute.Overview.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    ) {
                        composable(BandRoute.Overview.route) {
                            OverviewScreen(
                                uiState = uiState,
                                onSelectDate = viewModel::selectDate,
                                onNavigateToDevice = { navigateTo(BandRoute.Device.route) },
                                onNavigateToVitals = { navigateTo(BandRoute.Vitals.route) },
                                onNavigateToSleep = { navigateTo(BandRoute.Sleep.route) },
                                onSyncNow = viewModel::syncNow,
                                onFindBand = { viewModel.findBand(true) }
                            )
                        }
                        composable(BandRoute.Vitals.route) {
                            VitalsScreen(
                                uiState = uiState,
                                onSelectDate = viewModel::selectDate,
                                onNavigateToDevice = { navigateTo(BandRoute.Device.route) }
                            )
                        }
                        composable(BandRoute.Sleep.route) {
                            SleepScreen(
                                uiState = uiState,
                                onSelectDate = viewModel::selectDate,
                                onNavigateToDevice = { navigateTo(BandRoute.Device.route) }
                            )
                        }
                        composable(BandRoute.Workouts.route) {
                            WorkoutsScreen(
                                uiState = uiState,
                                onNavigateToDevice = { navigateTo(BandRoute.Device.route) }
                            )
                        }
                        composable(BandRoute.Device.route) {
                            DeviceScreen(
                                uiState = uiState,
                                requiredBlePermissions = viewModel.requiredBlePermissions(),
                                hasBlePermissions = viewModel.hasBlePermissions(),
                                onRequestScan = viewModel::startBleScan,
                                onStopScan = viewModel::stopBleScan,
                                onConnectDevice = viewModel::connectToDevice,
                                onReconnectPaired = viewModel::reconnectPairedDevice,
                                onDisconnect = viewModel::disconnectDevice,
                                onSyncNow = viewModel::syncNow,
                                onFindBand = { viewModel.findBand(true) },
                                onReadCharacteristic = viewModel::readGattCharacteristic,
                                onSaveAuthKey = viewModel::saveAuthKey,
                                onSaveStepGoal = viewModel::saveStepGoal,
                                onToggleTheme = viewModel::toggleTheme,
                                onImportFile = viewModel::importExportFile,
                                onClearLocalData = viewModel::clearLocalHistory
                            )
                        }
                    }
                }
            }
        }
    }
}
