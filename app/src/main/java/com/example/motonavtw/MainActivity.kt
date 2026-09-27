package com.motonavtw

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.motonavtw.ble.BleManager
import com.motonavtw.data.GatewayManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)

        setContent {
            MaterialTheme {
                val connectionState by bleManager.connectionState.collectAsState()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.entries.all { it.value }
                    if (allGranted) {
                        bleManager.startScan()
                    } else {
                        Toast.makeText(this@MainActivity, "需授權藍牙與定位權限", Toast.LENGTH_SHORT).show()
                    }
                }

                MotoNavApp(
                    connectionState = connectionState,
                    onConnectClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        } else {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.closeGatt()
    }
}

@Composable
fun MotoNavApp(connectionState: String, onConnectClick: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val gatewayManager = remember { GatewayManager(context) }
    val gatewayUrl by gatewayManager.gatewayUrlFlow.collectAsState(initial = "")
    val coroutineScope = rememberCoroutineScope()

    var showGatewayDialog by remember { mutableStateOf(false) }
    var tempUrlInput by remember { mutableStateOf("") }

    // 檢查網關是否為空
    LaunchedEffect(gatewayUrl) {
        if (gatewayUrl.isEmpty()) {
            showGatewayDialog = true
        }
    }

    if (showGatewayDialog) {
        AlertDialog(
            onDismissRequest = { /* 強制設定，不允許點擊外部關閉 */ },
            title = { Text("設定導航網關") },
            text = {
                OutlinedTextField(
                    value = tempUrlInput,
                    onValueChange = { tempUrlInput = it },
                    label = { Text("伺服器 URL") },
                    placeholder = { Text("例如: http://192.168.1.100:8080") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempUrlInput.isNotBlank()) {
                            coroutineScope.launch {
                                gatewayManager.saveGatewayUrl(tempUrlInput)
                                showGatewayDialog = false
                            }
                        }
                    }
                ) {
                    Text("儲存")
                }
            }
        )
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                connectionState = connectionState,
                recentLocations = listOf("台北市大安區", "新北市板橋區"),
                gatewayUrl = gatewayUrl,
                onSearchClick = {
                    if (gatewayUrl.isEmpty()) showGatewayDialog = true
                    // TODO: 執行 HTTP 搜尋請求
                },
                onLocationSelect = { loc -> /* TODO: 使用該地點規劃路線 */ },
                onNavigateToOfflineMaps = { /* TODO: 跳轉地圖下載頁 */ },
                onStartDemo = { navController.navigate("nav_preview") },
                onConfigGateway = {
                    tempUrlInput = gatewayUrl
                    showGatewayDialog = true
                },
                onConnectClick = onConnectClick
            )
        }

        composable("nav_preview") {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "導航運作中",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "目前以無藍牙硬體模式運行\nHTTP 請求與坐標轉換邏輯測試中...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    connectionState: String,
    recentLocations: List<String>,
    gatewayUrl: String,
    onSearchClick: () -> Unit,
    onLocationSelect: (String) -> Unit,
    onNavigateToOfflineMaps: () -> Unit,
    onStartDemo: () -> Unit,
    onConfigGateway: () -> Unit,
    onConnectClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MOTO GPS") },
                actions = {
                    AssistChip(
                        onClick = onConnectClick,
                        label = { Text(connectionState) },
                        leadingIcon = { Icon(Icons.Default.Build, null, Modifier.size(16.dp)) },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { onSearchClick() },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("輸入城市和地點...")
                    }
                }
            }

            if (recentLocations.isNotEmpty()) {
                item {
                    Text(
                        text = "最近地點",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(recentLocations) { location ->
                    ListItem(
                        headlineContent = { Text(location) },
                        leadingContent = { Icon(Icons.Default.Place, null) },
                        modifier = Modifier.clickable { onLocationSelect(location) }
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                ListItem(
                    headlineContent = { Text("網關設定") },
                    supportingContent = { Text(gatewayUrl.ifEmpty { "尚未配置" }) },
                    leadingContent = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.clickable { onConfigGateway() }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("演示導航 (免硬體)") },
                    supportingContent = { Text("執行固定路線模擬邏輯") },
                    leadingContent = { Icon(Icons.Default.PlayArrow, null) },
                    modifier = Modifier.clickable { onStartDemo() }
                )
            }
        }
    }
}