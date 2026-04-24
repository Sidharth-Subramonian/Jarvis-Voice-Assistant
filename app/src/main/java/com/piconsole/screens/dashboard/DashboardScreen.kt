package com.piconsole.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piconsole.components.ActionButtons
import com.piconsole.components.CircularStat
import com.piconsole.components.DeviceInfoCard
import com.piconsole.components.ErrorBanner
import com.piconsole.ui.theme.CpuColor
import com.piconsole.ui.theme.RamColor
import com.piconsole.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val status by viewModel.status.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchStatus()
    }

    Scaffold(
        topBar = {
            error?.let {
                ErrorBanner(
                    message = it,
                    onDismiss = { viewModel.fetchStatus() }, // Refresh on dismiss or just clear
                    onRetry = { viewModel.fetchStatus() }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.fetchStatus() }) {
                Text("Refresh")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isLoading && status == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                status?.let { deviceStatus ->
                    DeviceInfoCard(
                        deviceName = deviceStatus.deviceName,
                        ipAddress = deviceStatus.ipAddress,
                        status = if (deviceStatus.isOnline) "Online" else "Offline",
                        uptime = deviceStatus.uptime
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CircularStat(
                            percentage = deviceStatus.cpuUsage,
                            label = "CPU",
                            color = CpuColor
                        )
                        CircularStat(
                            percentage = deviceStatus.ramUsage,
                            label = "RAM",
                            color = RamColor
                        )
                    }

                    Text(
                        text = "Temperature: ${deviceStatus.temperature}°C",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    ActionButtons(
                        onFindPhoneClick = { viewModel.findPhone() },
                        onMuteClick = { viewModel.mute() },
                        onRebootClick = { viewModel.reboot() },
                        onShutdownClick = { viewModel.shutdown() }
                    )
                } ?: run {
                    Text("No status available. Tap refresh.", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}
