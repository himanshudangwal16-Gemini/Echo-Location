package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.SonicLightCyan
import com.example.ui.theme.TactileSonarAmber

@Composable
fun SettingsScreen(
    viewModel: EchoAppViewModel,
    modifier: Modifier = Modifier
) {
    val soundMode by viewModel.selectedSoundMode.collectAsStateWithLifecycle()
    val volume by viewModel.soundVolume.collectAsStateWithLifecycle()
    val roomTemplates = viewModel.roomTemplates
    val selectedRoom by viewModel.selectedRoomTemplate.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. WAVEBAND/SYNTHESIZER CONFIGURATION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ACOUSTIC BAND SELECTION",
                    color = SonicLightCyan,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Pick the chirp wave frequency that suits your ears. Clicks are great for high-noise areas; High Chirp provides maximum clarity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Mode List
                listOf(
                    AcousticSynthesizer.SoundMode.HIGH_CHIRP to Triple("HIGH CHIRP (2.4 - 5.5 kHz)", "Frequency swept linear sonar chirp", AcousticNeonGreen),
                    AcousticSynthesizer.SoundMode.AUDIBLE_PULSE to Triple("AUDIBLE Clicks", "Low-frequency clicker pulse", TactileSonarAmber),
                    AcousticSynthesizer.SoundMode.STABLE_TONE to Triple("STABLE Hum pitch", "Continuous variable-pitch soundwave", SonicLightCyan),
                    AcousticSynthesizer.SoundMode.ULTRASONIC to Triple("ULTRASONIC (18 - 21.5 kHz)", "Near-inaudible high acoustic pulse", MaterialTheme.colorScheme.onSurface)
                ).forEach { (mode, details) ->
                    val isSelected = soundMode == mode
                    val (title, description, accentColor) = details

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.selectSoundMode(mode) }
                            .testTag("sound_mode_${mode.name}")
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.selectSoundMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                    }
                }
            }
        }

        // 2. VOLUME SLIDER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MASTER VOLUME ADJUST",
                    color = SonicLightCyan,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeMute,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = volume,
                        onValueChange = { viewModel.changeVolume(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("volume_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = AcousticNeonGreen,
                            activeTrackColor = AcousticNeonGreen
                        )
                    )
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 3. BLUEPRINT MANAGER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "INDENSIBLE TRAINING BLUEPRINTS",
                    color = SonicLightCyan,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "GPS does not operate inside buildings. Practice navigating corridors, columns, and doorways using our physical orientation simulator layouts:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                roomTemplates.forEach { template ->
                    val isSelected = selectedRoom.id == template.id
                    val borderGlowColor = if (isSelected) AcousticNeonGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = borderGlowColor,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.loadRoomGeometry(template) }
                            .testTag("blueprint_template_${template.id}")
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (template.id) {
                                "narrow_corridor" -> Icons.Default.ViewStream
                                "spacious_lobby" -> Icons.Default.OpenInFull
                                else -> Icons.Default.TurnRight
                            },
                            contentDescription = null,
                            tint = if (isSelected) AcousticNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = template.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AcousticNeonGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = template.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Badge(containerColor = AcousticNeonGreen, contentColor = Color.Black) {
                                Text("LOADED", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(2.dp))
                            }
                        }
                    }
                }
            }
        }

        // 4. ACCESSIBILITY NAVIGATION INSTRUCTIONS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccessibilityNew, contentDescription = null, tint = TactileSonarAmber)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ECHOLOCATION USER MANUAL",
                        color = TactileSonarAmber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Acoustic Reflection Echolocation maps walls and central columns by pulsating high-frequency wave sweeps and listening to reflected bounds.\n\n" +
                            "Guidelines for blind and low-vision users:\n" +
                            "1. Standing scan: Turn on 'Compass Tracking' and spin your phone slowly like a radar sweep. As you cross walls or obstacles, the click rhythm accelerates and haptic vibrations pulse intensively.\n\n" +
                            "2. 3D Spatial Audio: Plug in stereo headphones. The audio will automatically pan left or right depending on which side of you the hallway obstacle is located.\n\n" +
                            "3. Voice Companion: With Text-to-Speech active, the service will periodically declare boundaries and warn you of imminent collisions within 1 meter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
