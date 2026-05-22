package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AcousticSynthesizer
import com.example.ui.EchoAppViewModel
import com.example.ui.theme.AcousticNeonGreen
import com.example.ui.theme.RadarBackdropGreen
import com.example.ui.theme.SonicLightCyan
import com.example.ui.theme.TactileSonarAmber
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanningScreen(
    viewModel: EchoAppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val isSonarActive by viewModel.isSonarActive.collectAsStateWithLifecycle()
    val isTactileEnabled by viewModel.isTactileFeedbackEnabled.collectAsStateWithLifecycle()
    val isVoiceEnabled by viewModel.isVoiceFeedbackEnabled.collectAsStateWithLifecycle()
    val isCompassEnabled by viewModel.isUsingPhysicalCompass.collectAsStateWithLifecycle()
    
    val selectedRoom by viewModel.selectedRoomTemplate.collectAsStateWithLifecycle()
    val obstacleDist by viewModel.obstacleDistance.collectAsStateWithLifecycle()
    val currentPan by viewModel.obstacleLeftRightPan.collectAsStateWithLifecycle()
    val headingAngle by viewModel.currentHeading.collectAsStateWithLifecycle()
    val sweepAngle by viewModel.manualSweepAngle.collectAsStateWithLifecycle()
    
    val userForward by viewModel.userForwardPos.collectAsStateWithLifecycle()
    val userLateral by viewModel.userLateralPos.collectAsStateWithLifecycle()
    
    val clLeft by viewModel.clearanceLeft.collectAsStateWithLifecycle()
    val clRight by viewModel.clearanceRight.collectAsStateWithLifecycle()
    val clAhead by viewModel.clearanceAhead.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var saveNotes by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Pulsing animations to represent active sonar scans
    val infiniteTransition = rememberInfiniteTransition(label = "sonar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Accessibility guide bar
        Text(
            text = "Echolocation Panel • Double-tap keys with screen readers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // 1. ACTIVE SONAR POWER SWITCH (Tactile accessibility size: 72dp high)
        val containerColor by animateColorAsState(
            targetValue = if (isSonarActive) AcousticNeonGreen else MaterialTheme.colorScheme.surface,
            label = "power_color"
        )
        val textPowerColor by animateColorAsState(
            targetValue = if (isSonarActive) Color.White else MaterialTheme.colorScheme.onSurface,
            label = "text_power_color"
        )
        val powerBorderAlpha by animateFloatAsState(
            targetValue = if (isSonarActive) 0.0f else 0.1f,
            label = "power_border"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = if (isSonarActive) Color.Transparent else Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable { viewModel.toggleSonar() }
                .testTag("toggle_sonar_button")
                .semantics {
                    contentDescription = if (isSonarActive) "Acoustic Scanner Active. Double tap to deactivate" else "Acoustic Scanner Paused. Double tap to activate sonar pulse"
                }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSonarActive) Icons.Default.Sensors else Icons.Default.SensorsOff,
                    contentDescription = null,
                    tint = textPowerColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isSonarActive) "ACTIVE SCANNING" else "SCANNER OFFLINE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPowerColor,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isSonarActive) "CHIRPS ACTIVE • EMITTING" else "TAP TO START SONAR",
                        style = MaterialTheme.typography.bodySmall,
                        color = textPowerColor.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }
            }
            if (isSonarActive) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f),
                                radius = size.minDimension / 2 * pulseScale,
                                alpha = pulseAlpha
                            )
                            drawCircle(color = Color.White, radius = size.minDimension / 4)
                        }
                )
            }
        }

        // 2. RADAR VIEW CANVAS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .border(1.dp, Color(0x13FFFFFF), RoundedCornerShape(24.dp))
                .testTag("radar_canvas_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .pointerInput(isCompassEnabled) {
                        if (!isCompassEnabled) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // Drag left/right controls the scanning sweep angle relative to straight ahead
                                val deltaAngle = dragAmount.x / size.width * 140f
                                val nextAngle = (sweepAngle + deltaAngle).coerceIn(-90f, 90f)
                                viewModel.setManualSweep(nextAngle)
                            }
                        }
                    }
                    .semantics {
                        contentDescription = "Radar sonar sweep display. Selected room is ${selectedRoom.displayName}. Angle offset is ${currentPan.toInt()} degrees. Distance to wall in this direction is ${String.format("%.1f", obstacleDist)} meters."
                    }
            ) {
                // Background Radar Net Grid
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2
                    val cy = h - 24.dp.toPx() // Pivot anchor near bottom

                    // Draw sonar concentric rings with progressive opacities
                    val maxRadius = w * 0.9f
                    val steps = 5
                    for (i in 1..steps) {
                        val r = (maxRadius / steps) * i
                        val alphaFactor = when (i) {
                            5 -> 0.05f // Outer ring (most translucent)
                            4 -> 0.10f
                            3 -> 0.18f
                            2 -> 0.28f
                            1 -> 0.45f // Inner ring (most defined)
                            else -> 0.12f
                        }
                        drawCircle(
                            color = AcousticNeonGreen.copy(alpha = alphaFactor),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Sonar Sweep Cone representation
                    // We draw a cone of 30-degree beam width centered at 'currentPan'
                    val centerAngle = 90f - currentPan // Pivot angle where 90 deg is straight up
                    val beamWidth = 32f
                    val angleStart = centerAngle - beamWidth / 2
                    val angleEnd = centerAngle + beamWidth / 2

                    val xStart = cx + maxRadius * cos(Math.toRadians(angleStart.toDouble())).toFloat()
                    val yStart = cy - maxRadius * sin(Math.toRadians(angleStart.toDouble())).toFloat()
                    val xEnd = cx + maxRadius * cos(Math.toRadians(angleEnd.toDouble())).toFloat()
                    val yEnd = cy - maxRadius * sin(Math.toRadians(angleEnd.toDouble())).toFloat()

                    // Glowing sweep shape
                    val coneBrush = Brush.radialGradient(
                        colors = listOf(AcousticNeonGreen.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = maxRadius
                    )
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, cy)
                        lineTo(xStart, yStart)
                        lineTo(xEnd, yEnd)
                        close()
                    }
                    drawPath(path = path, brush = coneBrush)

                    // Draw sweep radial guidelines
                    drawLine(
                        color = AcousticNeonGreen.copy(alpha = 0.25f),
                        start = Offset(cx, cy),
                        end = Offset(xStart, yStart),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = AcousticNeonGreen.copy(alpha = 0.25f),
                        start = Offset(cx, cy),
                        end = Offset(xEnd, yEnd),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw Walls/Perimeters (scaled)
                    // Scale factor: 1 meter = maxRadius / 5.5 meters
                    val scale = maxRadius / 5.5f

                    // Draw the walls in the viewModel
                    for (wall in viewModel.currentWalls) {
                        // Offset virtual coordinate to anchor center cy, cx
                        // Visualizing user at (0, 0) relative coordinates
                        // virtual x mapped to horizontal, virtual y mapped straight up (y increases up)
                        val x1Phys = cx + (wall.x1 - userLateral) * scale
                        val y1Phys = cy - (wall.y1 - userForward) * scale
                        val x2Phys = cx + (wall.x2 - userLateral) * scale
                        val y2Phys = cy - (wall.y2 - userForward) * scale

                        drawLine(
                            color = SonicLightCyan,
                            start = Offset(x1Phys, y1Phys),
                            end = Offset(x2Phys, y2Phys),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Draw Obstacles in the room (circles) with double layer neon aura glow
                    for (obs in viewModel.currentObstacles) {
                        val cxPhys = cx + (obs.cx - userLateral) * scale
                        val cyPhys = cy - (obs.cy - userForward) * scale
                        val rPhys = obs.r * scale

                        // Translucent glow aura
                        drawCircle(
                            color = TactileSonarAmber.copy(alpha = 0.22f),
                            radius = rPhys + 8.dp.toPx(),
                            center = Offset(cxPhys, cyPhys)
                        )
                        drawCircle(
                            color = TactileSonarAmber.copy(alpha = 0.12f),
                            radius = rPhys + 18.dp.toPx(),
                            center = Offset(cxPhys, cyPhys)
                        )
                        drawCircle(
                            color = TactileSonarAmber,
                            radius = rPhys,
                            center = Offset(cxPhys, cyPhys),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        // fill transparently
                        drawCircle(
                            color = TactileSonarAmber.copy(alpha = 0.25f),
                            radius = rPhys,
                            center = Offset(cxPhys, cyPhys)
                        )
                    }

                    // Draw collision intersection target indicator
                    val intersectDist = obstacleDist
                    val scanRad = Math.toRadians((90f - currentPan).toDouble())
                    val intersectX = cx + (intersectDist * cos(scanRad)).toFloat() * scale
                    val intersectY = cy - (intersectDist * sin(scanRad)).toFloat() * scale

                    drawCircle(
                        color = TactileSonarAmber,
                        radius = 8.dp.toPx() + (3.dp.toPx() * pulseScale),
                        center = Offset(intersectX, intersectY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = TactileSonarAmber,
                        radius = 3.dp.toPx(),
                        center = Offset(intersectX, intersectY)
                    )

                    // Draw user pointer position circle with double glass frame
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = 16.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = 16.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = AcousticNeonGreen,
                        radius = 5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = AcousticNeonGreen.copy(alpha = 0.25f),
                        radius = 20.dp.toPx() * pulseScale,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Superposed Visual Overlay Text Indicators (Large size)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = selectedRoom.displayName,
                        color = SonicLightCyan,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "RANGE: 6.0M • ACTIVE CHIRPS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        letterSpacing = 1.sp
                    )
                }

                // Superposed distance feedback (Extremely large for low vision users)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format("%.2f m", obstacleDist),
                        color = if (obstacleDist < 1.2f) TactileSonarAmber else AcousticNeonGreen,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "OBSTACLE REFLECTION",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Swipe guide overlay
                if (!isCompassEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Drag radar left-right to sweep sonar beam manually",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 3. HARDWARE CONTROL HUB (Buttons for Compass, Sound band, etc.)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compass Tracking Button (Toggles between manual sliding vs internal gyroscope compass)
            val compassBg by animateColorAsState(
                targetValue = if (isCompassEnabled) SonicLightCyan else MaterialTheme.colorScheme.surface,
                label = "comp_bg"
            )
            Button(
                onClick = { viewModel.togglePhysicalCompass() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = if (isCompassEnabled) Color.Transparent else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("toggle_compass_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = compassBg,
                    contentColor = if (isCompassEnabled) Color.Black else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.CompassCalibration, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCompassEnabled) "PHYSICAL ON" else "COMPASS OFF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Save Mapping Footprint button
            Button(
                onClick = {
                    saveName = ""
                    saveNotes = ""
                    showSaveDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .border(1.dp, SonicLightCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .testTag("save_mapping_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = SonicLightCyan
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Bookmark, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "SAVE FOOTPRINT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // 4. SONIC WALK CONTROLLER (EXPLORATION TRAINING PAD)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x13FFFFFF), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "VIRTUAL TRAINING WALK DECK",
                    style = MaterialTheme.typography.titleSmall,
                    color = SonicLightCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Text(
                    text = "GPS maps are blank here. Walk through this simulated blueprint. Tactile vibes and verbal cues will respond in real time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lateral slide drift (Left/Right)
                    IconButton(
                        onClick = { viewModel.walkLateral(-0.25f) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, AcousticNeonGreen.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowLeft, contentDescription = "Drift lateral Left", tint = AcousticNeonGreen, modifier = Modifier.size(36.dp))
                    }

                    // Forward/Backward walking
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.walkForward(0.35f) },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, AcousticNeonGreen.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Walk forward", tint = AcousticNeonGreen)
                        }
                        
                        Text(
                            text = String.format("Pos: F=%.1fm, L=%.1fm", userForward, userLateral),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { viewModel.walkForward(-0.35f) },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, AcousticNeonGreen.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Step backward", tint = AcousticNeonGreen)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.walkLateral(0.25f) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, AcousticNeonGreen.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowRight, contentDescription = "Drift lateral Right", tint = AcousticNeonGreen, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        // 5. ACCESSIBILITY CHANNELS (Toggles for Sound / Vibes / Voices)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ACCESSIBILITY CUES",
                    style = MaterialTheme.typography.titleSmall,
                    color = SonicLightCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Tactile feedback toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleTactile() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = if (isTactileEnabled) TactileSonarAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Haptic/Tactile Vibrations", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Faster pacing closer to walls", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isTactileEnabled,
                        onCheckedChange = { viewModel.toggleTactile() },
                        colors = SwitchDefaults.colors(checkedThumbColor = TactileSonarAmber)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Voice feedback toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleVoice() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isVoiceEnabled) AcousticNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Verbal Announcements", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Text-to-Speech spatial reports", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isVoiceEnabled,
                        onCheckedChange = { viewModel.toggleVoice() },
                        colors = SwitchDefaults.colors(checkedThumbColor = AcousticNeonGreen)
                    )
                }
            }
        }

        // 6. DUAL ACOUSTIC OSCILLOSCOPE (OSCILLATING SINES)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .border(2.dp, AcousticNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OSCILLOSCOPE SIGNAL ANALYSIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AcousticNeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.semantics { contentDescription = "Oscilloscope frequency visualizer" }
                    )
                    Text(
                        text = "dB: ${viewModel.audioInput.currentAmplitudedB.toInt()} SPL",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonicLightCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Emit Wave Chirp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        val currentSoundMode by viewModel.selectedSoundMode.collectAsStateWithLifecycle()
                        
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val halfH = h / 2
                            
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(0f, halfH)
                            
                            // Synthesize mock representations of emit pulses (High chirp, Ultrasonic)
                            val samplesCount = w.toInt()
                            for (i in 0 until samplesCount step 2) {
                                val t = i.toFloat() / w
                                val y = when (currentSoundMode) {
                                    AcousticSynthesizer.SoundMode.AUDIBLE_PULSE -> {
                                        // Quick decayed sync pulse
                                        val envelope = Math.exp(-t.toDouble() * 10.0)
                                        halfH + (sin(t * 180f + 50f) * halfH * 0.7f * envelope).toFloat()
                                    }
                                    AcousticSynthesizer.SoundMode.HIGH_CHIRP -> {
                                        // Sweeping sinusoidal chirper (frequency sweeps up over space)
                                        halfH + sin(t * t * 300f + t * 40f) * halfH * 0.65f
                                    }
                                    AcousticSynthesizer.SoundMode.ULTRASONIC -> {
                                        // Very high frequency dense waveforms
                                        halfH + sin(t * 1200f) * halfH * 0.5f
                                    }
                                    AcousticSynthesizer.SoundMode.STABLE_TONE -> {
                                        // Constant frequency hum proportional to pitch
                                        val freqFactor = 15f + (6f - obstacleDist) * 40f
                                        halfH + sin(t * freqFactor) * halfH * 0.65f
                                    }
                                    else -> halfH
                                }
                                if (i == 0) path.moveTo(0f, y) else path.lineTo(i.toFloat(), y)
                            }
                            drawPath(path = path, color = AcousticNeonGreen, style = Stroke(width = 1.5.dp.toPx()))
                        }
                        
                        Text(
                            text = "EMIT: CHIRP",
                            style = MaterialTheme.typography.labelSmall,
                            color = AcousticNeonGreen.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right: Capture Mic Reflections Wave (Real microphone data!)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        val micWaves = viewModel.audioInput.waveBuffer
                        
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val halfH = h / 2
                            
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(0f, halfH)
                            
                            val stepX = w / micWaves.size
                            for (i in micWaves.indices) {
                                val xVal = i * stepX
                                // Expand amplitude representation for visibility
                                val yVal = halfH + (micWaves[i] * halfH * 2.3f)
                                if (i == 0) path.moveTo(0f, yVal) else path.lineTo(xVal, yVal)
                            }
                            drawPath(path = path, color = SonicLightCyan, style = Stroke(width = 1.5.dp.toPx()))
                        }

                        Text(
                            text = "MIC: ECHO",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicLightCyan.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet or Dialog to SAVE Mapping Profile
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Save Echolocation Footprint",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SonicLightCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Name this indoor footprint profile so you can reload these layouts and warnings anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        label = { Text("E.g., Living Room Entry") },
                        placeholder = { Text("My Custom House") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = saveNotes,
                        onValueChange = { saveNotes = it },
                        label = { Text("Add Obstacle Notes (optional)") },
                        placeholder = { Text("Avoid bin on front left corner") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_notes_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveActiveMapping(saveName, saveNotes)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcousticNeonGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_save_profile_button")
                ) {
                    Text("SAVE RUN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
