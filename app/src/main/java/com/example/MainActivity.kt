package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.EchoAppViewModel
import com.example.ui.screens.SavedProfilesScreen
import com.example.ui.screens.ScanningScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AcousticNeonGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SecondaryCharcoal
import com.example.ui.theme.SonicLightCyan
import com.example.ui.theme.TactileSonarAmber
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

enum class NavigationTab {
    SCAN,
    SAVED_ROOMS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val viewModel: EchoAppViewModel = viewModel()
                
                var currentTab by remember { mutableStateOf(NavigationTab.SCAN) }
                var hasMicPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                // Request microphone permissions gracefully on startup
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasMicPermission = granted
                    if (granted && viewModel.isSonarActive.value) {
                        // restart recording if scanner was active
                        viewModel.audioInput.isListening = true
                    }
                }

                LaunchedEffect(Unit) {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ECHO-LOCATION",
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = AcousticNeonGreen,
                                        modifier = Modifier.semantics {
                                            contentDescription = "Echo-location application heading"
                                        }
                                    )
                                    Text(
                                        text = "INDOOR ACOUSTIC SCANNER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SonicLightCyan,
                                        letterSpacing = 1.sp
                                    )
                                }
                            },
                            actions = {
                                // GPS is offline badge
                                Row(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, TactileSonarAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .semantics { contentDescription = "Sensing State: GPS is inactive indoors, active sonar enabled" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GpsOff,
                                        contentDescription = null,
                                        tint = TactileSonarAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "GPS OFFLINE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TactileSonarAmber,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    bottomBar = {
                        // Standard Material 3 Navigation Bar with active pills
                        NavigationBar(
                            containerColor = SecondaryCharcoal,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav_bar")
                                .drawBehind {
                                    drawLine(
                                        color = Color(0x13FFFFFF),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                        ) {
                            NavigationBarItem(
                                selected = currentTab == NavigationTab.SCAN,
                                onClick = { currentTab = NavigationTab.SCAN },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = "Tap to view virtual radar panel and sweep the area"
                                    )
                                },
                                label = { Text("ECHOMAP", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AcousticNeonGreen,
                                    indicatorColor = AcousticNeonGreen,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_scan")
                            )

                            NavigationBarItem(
                                selected = currentTab == NavigationTab.SAVED_ROOMS,
                                onClick = { currentTab = NavigationTab.SAVED_ROOMS },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Bookmarks,
                                        contentDescription = "Tap to view saved indoor room mappings library"
                                    )
                                },
                                label = { Text("SAVED RUNS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AcousticNeonGreen,
                                    indicatorColor = AcousticNeonGreen,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_saved")
                            )

                            NavigationBarItem(
                                selected = currentTab == NavigationTab.SETTINGS,
                                onClick = { currentTab = NavigationTab.SETTINGS },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Tap to load training layouts and adjust audio configurations"
                                    )
                                },
                                label = { Text("CONFIG", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AcousticNeonGreen,
                                    indicatorColor = AcousticNeonGreen,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Warn of simulator mode if permission is disabled
                        if (!hasMicPermission) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .border(1.5.dp, TactileSonarAmber, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Warning info indicator",
                                        tint = TactileSonarAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "MICROPHONE DISCONNECTED",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TactileSonarAmber,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Permission denied. Sonic radar is currently active in pure High-Fidelity Simulator mode. Reflection sounds will still generate.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TactileSonarAmber, contentColor = Color.Black),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("ALLOW", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Display selected Tab
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentTab) {
                                NavigationTab.SCAN -> ScanningScreen(viewModel = viewModel)
                                NavigationTab.SAVED_ROOMS -> SavedProfilesScreen(viewModel = viewModel)
                                NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
