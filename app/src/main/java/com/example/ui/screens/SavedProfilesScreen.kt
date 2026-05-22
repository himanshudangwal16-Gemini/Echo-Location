package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SavedRoom
import com.example.ui.EchoAppViewModel
import com.example.ui.theme.AcousticNeonGreen
import com.example.ui.theme.SonicLightCyan
import com.example.ui.theme.TactileSonarAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedProfilesScreen(
    viewModel: EchoAppViewModel,
    modifier: Modifier = Modifier
) {
    val roomsList by viewModel.savedRooms.collectAsStateWithLifecycle()
    val activeTemplate by viewModel.selectedRoomTemplate.collectAsStateWithLifecycle()

    var profileToDelete by remember { mutableStateOf<SavedRoom?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SAVED FOOTPRINTS",
                    color = SonicLightCyan,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Local snapshots of mapped rooms database",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Badge(
                containerColor = AcousticNeonGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "${roomsList.size} Saved",
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }

        if (roomsList.isEmpty()) {
            // HIGH-CONTRAST EMPTY SNAPSHOT BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "No Echo Maps Saved",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Because GPS is offline indoors, saving your custom hallways and doorways creates local mental sonar outlines.\n\nTo save your first room map, go to the Scanner screen, sweep the area, and tap 'Save Footprint' below.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("saved_profiles_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = roomsList,
                    key = { it.id }
                ) { room ->
                    val isCurrentlyMatched = activeTemplate.id == room.roomType
                    
                    val formattedDate = remember(room.timestamp) {
                        try {
                            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                            sdf.format(Date(room.timestamp))
                        } catch (e: Exception) {
                            "Unknown Date"
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = if (isCurrentlyMatched) 1.5.dp else 1.dp,
                                color = if (isCurrentlyMatched) AcousticNeonGreen else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    // Load matching template in simulator
                                    val match = viewModel.roomTemplates.find { it.id == room.roomType }
                                    if (match != null) {
                                        viewModel.loadRoomGeometry(match)
                                    }
                                },
                                onLongClick = {
                                    profileToDelete = room
                                }
                            )
                            .testTag("profile_item_${room.id}")
                            .semantics {
                                contentDescription = "Saved location: ${room.name}. Room template is ${room.roomType}. Notes: ${room.notes}. Double tap to activate in simulator. Long press to delete."
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentlyMatched) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCurrentlyMatched) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active simulation loaded",
                                            tint = AcousticNeonGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = room.name,
                                        color = if (isCurrentlyMatched) AcousticNeonGreen else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Text(
                                    text = formattedDate,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                if (room.notes.isNotEmpty()) {
                                    Text(
                                        text = "Notes: ${room.notes}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(room.roomType.replace("_", " ").uppercase()) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = SonicLightCyan
                                        )
                                    )
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("OBSTACLES: ${room.obstacleCount}") },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = TactileSonarAmber
                                        )
                                    )
                                }
                            }

                            Row {
                                // Reload button
                                IconButton(
                                    onClick = {
                                        val match = viewModel.roomTemplates.find { it.id == room.roomType }
                                        if (match != null) {
                                            viewModel.loadRoomGeometry(match)
                                        }
                                    },
                                    modifier = Modifier.semantics { contentDescription = "Load footprint into radar" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = SonicLightCyan
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = { profileToDelete = room },
                                    modifier = Modifier.semantics { contentDescription = "Delete from database" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Deletion assurance popup dialogue
    if (profileToDelete != null) {
        val currentRoom = profileToDelete!!
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = {
                Text(
                    text = "Confirm Footprint Delete",
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete the cached echo map footprint for '${currentRoom.name}' from your visual navigation library?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProfile(currentRoom.id)
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DELETE SNAPSHOT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
