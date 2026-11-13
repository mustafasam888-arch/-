package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ble.BleConnectionStatus
import com.example.ble.ScannedBleDevice
import com.example.ui.Band7UiState
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.TelemetryCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MiOrange
import com.example.ui.theme.PulseRose
import com.example.ui.theme.RecoveryEmerald
import com.example.ui.theme.StressAmber
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun DeviceScreen(
    uiState: Band7UiState,
    requiredBlePermissions: Array<String>,
    hasBlePermissions: Boolean,
    onRequestScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (ScannedBleDevice) -> Unit,
    onReconnectPaired: () -> Unit,
    onDisconnect: () -> Unit,
    onSyncNow: () -> Unit,
    onFindBand: () -> Unit,
    onReadCharacteristic: (UUID, UUID) -> Unit,
    onSaveAuthKey: (String) -> Unit,
    onSaveStepGoal: (Int) -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onImportFile: (Uri) -> Unit,
    onClearLocalData: () -> Unit
) {
    val live = uiState.liveTelemetry
    val summary = uiState.dailySummary
    val isConnected = live.connectionStatus == BleConnectionStatus.CONNECTED

    var permissionGrantedLocal by remember(hasBlePermissions) { mutableStateOf(hasBlePermissions) }
    var showAuthKeyDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        val allGranted = resultMap.values.all { it }
        permissionGrantedLocal = allGranted
        if (allGranted) {
            onRequestScan()
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportFile(uri)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // BLE Scanner & Connection Card
        item {
            TelemetryCard(accentBorder = ElectricCyan.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("اتصال البلوتوث المباشر (BLE)", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = when {
                                !live.isBluetoothSupported -> "محول BLE غير متاح على هذا الجهاز"
                                !live.isBluetoothEnabled -> "البلوتوث متوقف — يرجى تشغيله من الإعدادات"
                                else -> "جاهز للاتصال بسوار Xiaomi Smart Band 7"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ConnectionStatusBadge(status = live.connectionStatus)
                }

                live.errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PulseRose.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, PulseRose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PulseRose,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (live.authStatusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (live.isAuthenticated) RecoveryEmerald else StressAmber).copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = live.authStatusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (live.isAuthenticated) RecoveryEmerald else StressAmber,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (live.isScanning) {
                                onStopScan()
                            } else if (permissionGrantedLocal) {
                                onRequestScan()
                            } else {
                                blePermissionLauncher.launch(requiredBlePermissions)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("ble_scan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (live.isScanning) StressAmber else MiOrange
                        )
                    ) {
                        Icon(
                            imageVector = if (live.isScanning) Icons.Rounded.BluetoothSearching else Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (live.isScanning) "إيقاف البحث" else "بحث عن السوار")
                    }

                    if (isConnected) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ble_disconnect_button")
                        ) {
                            Text("قطع الاتصال")
                        }
                    } else if (uiState.userPrefs.pairedMacAddress.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                if (permissionGrantedLocal) {
                                    onReconnectPaired()
                                } else {
                                    blePermissionLauncher.launch(requiredBlePermissions)
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ble_reconnect_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("إعادة ربط")
                        }
                    }
                }

                if (isConnected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onSyncNow,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("device_sync_button")
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("مزامنة الخصائص")
                        }
                        FilledTonalButton(
                            onClick = onFindBand,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("device_find_band_button")
                        ) {
                            Icon(Icons.Rounded.Vibration, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("اهتزاز السوار")
                        }
                    }
                }
            }
        }

        // Discovered BLE Devices List
        if (uiState.scannedDevices.isNotEmpty()) {
            item {
                Text(
                    text = "الأجهزة المكتشفة (${uiState.scannedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(uiState.scannedDevices, key = { it.address }) { device ->
                TelemetryCard(
                    accentBorder = if (device.isXiaomiBandCandidate) MiOrange.copy(alpha = 0.65f) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnectDevice(device) }
                            .minimumInteractiveComponentSize()
                            .testTag("scanned_device_${device.address}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        (if (device.isXiaomiBandCandidate) MiOrange else ElectricCyan).copy(alpha = 0.16f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Watch,
                                    contentDescription = device.name,
                                    tint = if (device.isXiaomiBandCandidate) MiOrange else ElectricCyan
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    if (device.isXiaomiBandCandidate) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MiOrange.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = "شاومي باند",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MiOrange,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.SignalCellularAlt,
                                contentDescription = "قوة الإشارة",
                                tint = RecoveryEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${device.rssi} dBm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Device Hardware & Software Info Card
        item {
            TelemetryCard {
                Text("معلومات السوار والنظام", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                InfoRow("اسم الجهاز", live.connectedName.ifBlank { summary?.deviceName.orEmpty().ifBlank { "--" } })
                InfoRow("عنوان MAC", live.connectedMac.ifBlank { uiState.userPrefs.pairedMacAddress.ifBlank { "--" } })
                InfoRow("الشركة المصنعة", live.manufacturer.ifBlank { "--" })
                InfoRow("الطراز", live.modelNumber.ifBlank { "--" })
                InfoRow("الرقم التسلسلي", live.serialNumber.ifBlank { summary?.serialNumber.orEmpty().ifBlank { "--" } })
                InfoRow("البرنامج الثابت (FW)", live.firmwareVersion.ifBlank { summary?.firmwareVersion.orEmpty().ifBlank { "--" } })
                InfoRow("إصدار العتاد (HW)", live.hardwareVersion.ifBlank { summary?.hardwareVersion.orEmpty().ifBlank { "--" } })
                val batt = if (live.batteryLevel >= 0) "${live.batteryLevel}%" else if ((summary?.batteryLevel ?: -1) >= 0) "${summary?.batteryLevel}%" else "--"
                InfoRow("البطارية والشحن", if (live.isCharging) "$batt (قيد الشحن)" else batt)
                if (live.lastChargedDate.isNotBlank()) {
                    InfoRow("آخر شحن كامل", live.lastChargedDate)
                }
            }
        }

        // Settings & Import Data Card
        item {
            TelemetryCard {
                Text("الإعدادات واستيراد بيانات شاومي", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // Auth Key Row
                SettingActionRow(
                    icon = Icons.Rounded.Key,
                    title = "مفتاح المصادقة المشفر (Auth Key)",
                    subtitle = if (uiState.userPrefs.authKeyHex.length == 32) {
                        "تم تعيين مفتاح من 32 خانة (${uiState.userPrefs.authKeyHex.take(6)}••••)"
                    } else {
                        "مطلوب لفتح خصائص Huami المشفرة على باند 7"
                    },
                    testTag = "open_auth_key_dialog",
                    onClick = { showAuthKeyDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Step Goal Row
                SettingActionRow(
                    icon = Icons.Rounded.Flag,
                    title = "هدف الخطوات اليومي",
                    subtitle = "${uiState.userPrefs.stepGoal} خطوة يومياً",
                    testTag = "open_step_goal_dialog",
                    onClick = { showGoalDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Dark Theme Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = ElectricCyan)
                        Column {
                            Text("نمط شاشة AMOLED الداكن", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "تباين عالٍ مستوحى من شاومي باند 7",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.userPrefs.darkTheme,
                        onCheckedChange = onToggleTheme,
                        modifier = Modifier.testTag("dark_theme_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            fileImportLauncher.launch(
                                arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "*/*")
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("import_export_file_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041C24))
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("استيراد ملف Zepp/JSON")
                    }

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_data_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "مسح السجل", tint = PulseRose)
                    }
                }
            }
        }

        // Live GATT Inspector
        if (live.discoveredServices.isNotEmpty()) {
            item {
                Text(
                    text = "مفتش خدمات GATT الحية (${live.discoveredServices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(live.discoveredServices, key = { it.uuid.toString() }) { service ->
                var expanded by remember { mutableStateOf(false) }
                TelemetryCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .minimumInteractiveComponentSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Memory, contentDescription = null, tint = MiOrange)
                            Column {
                                Text(service.titleAr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = service.uuid.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "طي" else "توسيع"
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            service.characteristics.forEach { ch ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(ch.titleAr, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                            if (ch.canRead) {
                                                TextButton(
                                                    onClick = { onReadCharacteristic(service.uuid, ch.uuid) },
                                                    modifier = Modifier.minimumInteractiveComponentSize()
                                                ) {
                                                    Text("قراءة")
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${ch.uuid} • ${ch.propertiesLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (ch.decodedValue.isNotBlank()) {
                                            Text(
                                                text = "القيمة: ${ch.decodedValue}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ElectricCyan
                                            )
                                        }
                                        if (ch.lastRawHex.isNotBlank()) {
                                            Text(
                                                text = "HEX: ${ch.lastRawHex}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BLE Event Log
        if (live.statusLog.isNotEmpty()) {
            item {
                TelemetryCard {
                    Text("سجل أحداث البلوتوث", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    live.statusLog.take(8).forEach { entry ->
                        Text(
                            text = "• $entry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAuthKeyDialog) {
        var keyText by remember { mutableStateOf(uiState.userPrefs.authKeyHex) }
        AlertDialog(
            onDismissRequest = { showAuthKeyDialog = false },
            title = { Text("مفتاح المصادقة (Auth Key)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "أدخل مفتاح المصادقة المكوّن من 32 خانة ست عشرية (16 بايت AES) الخاص بسوار شاومي باند 7 لفتح خصائص Huami المشفرة.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        label = { Text("32 خانة Hex") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_key_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAuthKey(keyText)
                        showAuthKeyDialog = false
                    },
                    modifier = Modifier.testTag("save_auth_key_button")
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showAuthKeyDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showGoalDialog) {
        var sliderVal by remember { mutableFloatStateOf(uiState.userPrefs.stepGoal.toFloat()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("هدف الخطوات اليومي") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${sliderVal.roundToInt()} خطوة",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MiOrange
                    )
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 2000f..25000f,
                        steps = 22
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveStepGoal(sliderVal.roundToInt())
                        showGoalDialog = false
                    },
                    modifier = Modifier.testTag("save_step_goal_button")
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("مسح السجل المحلي") },
            text = { Text("هل تريد حذف كافة بيانات النشاط والنبض والنوم والتمارين المخزنة محلياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearLocalData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseRose),
                    modifier = Modifier.testTag("confirm_clear_data_button")
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = title, tint = MiOrange)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
