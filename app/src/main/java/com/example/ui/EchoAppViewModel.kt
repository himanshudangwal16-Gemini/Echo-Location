package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AcousticSynthesizer
import com.example.audio.AudioInputListener
import com.example.audio.VoiceFeedback
import com.example.data.AppDatabase
import com.example.data.RoomRepository
import com.example.data.SavedRoom
import com.example.sensor.OrientationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class EchoAppViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Data Repositories & Room
    private val roomRepository: RoomRepository
    val savedRooms: StateFlow<List<SavedRoom>>

    // 2. Audio & Sensor Hardware Engines
    val synthesizer = AcousticSynthesizer()
    val audioInput = AudioInputListener()
    private val orientationTracker = OrientationTracker(application)
    private val voiceFeedback = VoiceFeedback(application)
    private var vibrator: Vibrator? = null

    // 3. UI State - Control & Settings
    private val _isSonarActive = MutableStateFlow(false)
    val isSonarActive = _isSonarActive.asStateFlow()

    private val _isTactileFeedbackEnabled = MutableStateFlow(true)
    val isTactileFeedbackEnabled = _isTactileFeedbackEnabled.asStateFlow()

    private val _isVoiceFeedbackEnabled = MutableStateFlow(true)
    val isVoiceFeedbackEnabled = _isVoiceFeedbackEnabled.asStateFlow()

    private val _soundVolume = MutableStateFlow(0.8f)
    val soundVolume = _soundVolume.asStateFlow()

    private val _selectedSoundMode = MutableStateFlow(AcousticSynthesizer.SoundMode.HIGH_CHIRP)
    val selectedSoundMode = _selectedSoundMode.asStateFlow()

    // 4. Physical orientation tracking states
    private val _currentHeading = MutableStateFlow(0f) // Current physical angle of phone (0..360)
    val currentHeading = _currentHeading.asStateFlow()

    // Drag-based orientation sweeping when user is not using physical compass
    private val _manualSweepAngle = MutableStateFlow(0f) // -90 deg to +90 deg relative to screen center
    val manualSweepAngle = _manualSweepAngle.asStateFlow()

    private val _isUsingPhysicalCompass = MutableStateFlow(false)
    val isUsingPhysicalCompass = _isUsingPhysicalCompass.asStateFlow()

    // 5. Simulated Room Navigation State
    sealed class RoomTemplate(val id: String, val displayName: String, val description: String) {
        object NarrowCorridor : RoomTemplate("narrow_corridor", "Narrow Corridor", "A tight 2.5m wide hallway with a blocking trash bin at 2.4m and end wall at 6.0m.")
        object SpaciousLobby : RoomTemplate("spacious_lobby", "Spacious Lobby", "A broad open area with columns on sides and a circular lounge sofa ahead at 3.0m.")
        object LChamber : RoomTemplate("l_chamber", "L-Shaped Corridor", "A corridor extending forward, turning sharp right after 3.5m. Clear acoustic signals outline the open doorway.")
    }

    val roomTemplates = listOf(
        RoomTemplate.NarrowCorridor,
        RoomTemplate.SpaciousLobby,
        RoomTemplate.LChamber
    )

    private val _selectedRoomTemplate = MutableStateFlow<RoomTemplate>(RoomTemplate.NarrowCorridor)
    val selectedRoomTemplate = _selectedRoomTemplate.asStateFlow()

    // User position offset inside virtual template room
    private val _userForwardPos = MutableStateFlow(0f) // 0.0m to 5.0m
    val userForwardPos = _userForwardPos.asStateFlow()

    private val _userLateralPos = MutableStateFlow(0f) // -1.5m to 1.5m (left-right drift)
    val userLateralPos = _userLateralPos.asStateFlow()

    // Outputs of the active Echolocation Solver
    private val _obstacleDistance = MutableStateFlow(3.0f)
    val obstacleDistance = _obstacleDistance.asStateFlow()

    private val _obstacleLeftRightPan = MutableStateFlow(0f) // Angle of intersection
    val obstacleLeftRightPan = _obstacleLeftRightPan.asStateFlow()

    private val _clearanceLeft = MutableStateFlow(1.5f)
    val clearanceLeft = _clearanceLeft.asStateFlow()

    private val _clearanceRight = MutableStateFlow(1.5f)
    val clearanceRight = _clearanceRight.asStateFlow()

    private val _clearanceAhead = MutableStateFlow(3.0f)
    val clearanceAhead = _clearanceAhead.asStateFlow()

    // 6. Active background feedback jobs
    private var tactileLoopJob: Job? = null
    private var voiceTimerJob: Job? = null

    // Geometry components computed for render
    data class WallSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
    data class ObstacleCircle(val cx: Float, val cy: Float, val r: Float)

    val currentWalls = mutableStateListOf<WallSegment>()
    val currentObstacles = mutableStateListOf<ObstacleCircle>()

    init {
        // Initialize Room DB
        val db = AppDatabase.getDatabase(application)
        roomRepository = RoomRepository(db.roomDao())
        
        savedRooms = roomRepository.allSavedRooms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize vibrates
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Apply fallback visual geometry
        loadRoomGeometry(RoomTemplate.NarrowCorridor)

        // Setup compass tracker callback
        orientationTracker.onHeadingChanged = { heading ->
            if (_isUsingPhysicalCompass.value) {
                _currentHeading.value = heading
                recalculateEcholocation()
            }
        }

        // Active solver link
        viewModelScope.launch {
            // Regularly monitor and sync settings to synthesizers
            while (true) {
                synthesizer.soundMode = _selectedSoundMode.value
                synthesizer.masterVolume = _soundVolume.value
                recalculateEcholocation()
                delay(80)
            }
        }

        // Setup dynamic haptics & voice engines
        startTactileVibeLoop()
        startVoicePeriodicAnnouncer()
    }

    /**
     * Loads 2D physics geometry for Ray-Casting solver
     */
    fun loadRoomGeometry(template: RoomTemplate) {
        _selectedRoomTemplate.value = template
        currentWalls.clear()
        currentObstacles.clear()

        when (template) {
            RoomTemplate.NarrowCorridor -> {
                // Bounds left / right
                currentWalls.add(WallSegment(-1.25f, -2.0f, -1.25f, 8.0f)) // Left wall
                currentWalls.add(WallSegment(1.25f, -2.0f, 1.25f, 8.0f))  // Right wall
                currentWalls.add(WallSegment(-1.25f, 6.0f, 1.25f, 6.0f))  // End wall
                
                // Obstacle box right in the center path
                currentObstacles.add(ObstacleCircle(0.0f, 2.4f, 0.35f))  // Wet waste bin
            }
            RoomTemplate.SpaciousLobby -> {
                // Wide borders
                currentWalls.add(WallSegment(-3.5f, -2.0f, -3.5f, 8.0f)) // Left border
                currentWalls.add(WallSegment(3.5f, -2.0f, 3.5f, 8.0f))  // Right border
                currentWalls.add(WallSegment(-3.5f, 7.5f, 3.5f, 7.5f))  // Front border
                currentWalls.add(WallSegment(-3.5f, -2.0f, 3.5f, -2.0f)) // Back border

                // Multiple scattered structures
                currentObstacles.add(ObstacleCircle(-1.8f, 2.0f, 0.45f)) // Left stone column
                currentObstacles.add(ObstacleCircle(1.8f, 2.0f, 0.45f))  // Right stone column
                currentObstacles.add(ObstacleCircle(0.0f, 4.0f, 0.70f))  // Large circular sofa group
            }
            RoomTemplate.LChamber -> {
                // L-shape hallway going up then turning right
                // Straight segment going up
                currentWalls.add(WallSegment(-1.2f, -2.0f, -1.2f, 4.8f))  // Left outer wall
                currentWalls.add(WallSegment(1.2f, -2.0f, 1.2f, 2.4f))   // Right inner wall

                // Horizontally turning segments
                currentWalls.add(WallSegment(-1.2f, 4.8f, 7.0f, 4.8f))   // Top outer turn wall
                currentWalls.add(WallSegment(1.2f, 2.4f, 7.0f, 2.4f))    // Bottom inner turn wall

                // Columns or block obstacles in L turn
                currentObstacles.add(ObstacleCircle(3.2f, 3.6f, 0.4f))   // Turn sign pillar
            }
        }
        _userForwardPos.value = 0f
        _userLateralPos.value = 0f
        recalculateEcholocation()
    }

    /**
     * Active Echolocation Solver
     * Casts 2D acoustic rays to estimate obstacles and clear directions
     */
    private fun recalculateEcholocation() {
        val posX = _userLateralPos.value
        val posY = _userForwardPos.value
        
        // Active scan heading is the combination of physical heading or screen sweep
        val scanAngleDegrees = if (_isUsingPhysicalCompass.value) {
            // physical angle
            _currentHeading.value
        } else {
            // visual slider angle relative to straight ahead (which is 0 degrees / up)
            _manualSweepAngle.value
        }

        // Convert heading to math radians (Standard 0 rad is facing standard Y, which is 90 deg)
        // Let facing straight up (0 deg) be angle PI/2
        val rad = Math.toRadians((90f - scanAngleDegrees).toDouble())
        val dirX = cos(rad).toFloat()
        val dirY = sin(rad).toFloat()

        // RAY-CASTING SOLVER
        var minDistance = 6.0f // max sonar range 6 meters
        var hitType = "Clear Space"

        // 1. Ray intersection with line segments (walls)
        for (wall in currentWalls) {
            val dist = getRayLineIntersection(posX, posY, dirX, dirY, wall)
            if (dist in 0.1f..minDistance) {
                minDistance = dist
                hitType = "Wall"
            }
        }

        // 2. Ray intersection with circles (obstacles)
        for (obs in currentObstacles) {
            val dist = getRayCircleIntersection(posX, posY, dirX, dirY, obs)
            if (dist in 0.1f..minDistance) {
                minDistance = dist
                hitType = "Obstacle"
            }
        }

        _obstacleDistance.value = minDistance
        // Pan: Map scan angle to sound left-right
        // Keep inside -90 to +90 degrees for visual and sound representation
        val relativePanAngle = if (_isUsingPhysicalCompass.value) {
            // Translate compass absolute grid delta to relative
            ((scanAngleDegrees + 180f) % 360f) - 180f
        } else {
            _manualSweepAngle.value
        }
        _obstacleLeftRightPan.value = relativePanAngle

        // Propagate values directly to physical hardware synthesizer
        synthesizer.currentDistance = minDistance
        synthesizer.obstacleAngle = relativePanAngle

        // Calculate discrete clearance rays (left, center, right)
        // Left sweep space check (-45 degrees)
        _clearanceLeft.value = castQueryRay(posX, posY, scanAngleDegrees - 45f)
        // Right sweep space check (+45 degrees)
        _clearanceRight.value = castQueryRay(posX, posY, scanAngleDegrees + 45f)
        // Centre sweep space check (0 degrees)
        _clearanceAhead.value = castQueryRay(posX, posY, scanAngleDegrees)
    }

    private fun castQueryRay(px: Float, py: Float, angleDeg: Float): Float {
        val rad = Math.toRadians((90f - angleDeg).toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        var maxD = 6.0f

        for (wall in currentWalls) {
            val d = getRayLineIntersection(px, py, dx, dy, wall)
            if (d in 0.1f..maxD) maxD = d
        }
        for (obs in currentObstacles) {
            val d = getRayCircleIntersection(px, py, dx, dy, obs)
            if (d in 0.1f..maxD) maxD = d
        }
        return maxD
    }

    private fun getRayLineIntersection(rx: Float, ry: Float, dx: Float, dy: Float, wall: WallSegment): Float {
        val ax = wall.x1
        val ay = wall.y1
        val bx = wall.x2
        val by = wall.y2

        val vx = bx - ax
        val vy = by - ay

        val det = dy * vx - dx * vy
        if (Math.abs(det) < 1e-5f) return -1f // Parallel

        val t = (vx * (ay - ry) - vy * (ax - rx)) / det
        val s = (dx * (ay - ry) - dy * (ax - rx)) / det

        return if (t >= 0f && s in 0f..1f) t else -1f
    }

    private fun getRayCircleIntersection(rx: Float, ry: Float, dx: Float, dy: Float, obs: ObstacleCircle): Float {
        val cx = obs.cx
        val cy = obs.cy
        val r = obs.r

        val mx = rx - cx
        val my = ry - cy

        // Solving square coefficients
        val b = 2f * (mx * dx + my * dy)
        val c = (mx * mx + my * my) - (r * r)

        val disc = b * b - 4f * c
        if (disc < 0) return -1f

        val t1 = (-b - sqrt(disc)) / 2f
        val t2 = (-b + sqrt(disc)) / 2f

        if (t1 >= 0f && t2 >= 0f) return Math.min(t1, t2)
        if (t1 >= 0f) return t1
        if (t2 >= 0f) return t2
        return -1f
    }

    // Dynamic tactile (haptic vibrating) loop
    private fun startTactileVibeLoop() {
        tactileLoopJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_isSonarActive.value && _isTactileFeedbackEnabled.value) {
                    val dist = _obstacleDistance.value
                    if (dist < 3.0f) {
                        // Strong tactile intensity closer
                        val duration = if (dist < 0.6f) 120L else 40L
                        val amplitude = if (dist < 0.6f) 220 else 110

                        triggerVibration(duration, amplitude)

                        // Wait longer when obstacles are far away, and vibrate rapidly when close
                        // 0.1m -> 70ms, 3.0m -> 1100ms
                        val delayMs = ((dist / 3.0f) * 1000f + 70f).toLong()
                        delay(delayMs)
                    } else {
                        delay(300)
                    }
                } else {
                    delay(300)
                }
            }
        }
    }

    // Voice announcement system for hands-free and talking controls
    private fun startVoicePeriodicAnnouncer() {
        voiceTimerJob = viewModelScope.launch {
            delay(4000) // boot delay
            while (isActive) {
                if (_isSonarActive.value && _isVoiceFeedbackEnabled.value) {
                    val dist = _obstacleDistance.value
                    val angle = _obstacleLeftRightPan.value

                    val directionDesc = when {
                        angle < -25f -> "on your left"
                        angle > 25f -> "on your right"
                        else -> "straight ahead"
                    }

                    val distanceText = String.format("%.1f meters", dist)
                    
                    if (dist < 0.8f) {
                        voiceFeedback.speak("Warning. Immediate wall obstacle $distanceText $directionDesc.", overrideCurrent = true)
                    } else if (dist < 2.2f) {
                        voiceFeedback.speak("Object detected at $distanceText $directionDesc.", overrideCurrent = false)
                    }
                    
                    // Announce overall clear path guidance periodically
                    delay(7000)
                } else {
                    delay(1000)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerVibration(duration: Long, amplitude: Int) {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val clampedAmp = amplitude.coerceIn(1, 255)
                v.vibrate(VibrationEffect.createOneShot(duration, clampedAmp))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        } catch (e: Exception) {
            // Handled safely
        }
    }

    // UI actions
    fun toggleSonar() {
        val newVal = !_isSonarActive.value
        _isSonarActive.value = newVal
        synthesizer.isEnabled = newVal
        audioInput.isListening = newVal

        if (newVal) {
            triggerVibration(180, 200)
            voiceFeedback.speak("Starting acoustic scanning stream.")
        } else {
            triggerVibration(80, 100)
            voiceFeedback.speak("Active scan paused.")
        }
    }

    fun toggleTactile() {
        _isTactileFeedbackEnabled.value = !_isTactileFeedbackEnabled.value
        triggerVibration(50, 120)
    }

    fun toggleVoice() {
        val newVal = !_isVoiceFeedbackEnabled.value
        _isVoiceFeedbackEnabled.value = newVal
        voiceFeedback.isVoiceEnabled = newVal
        triggerVibration(50, 120)
    }

    fun changeVolume(volume: Float) {
        _soundVolume.value = volume
    }

    fun selectSoundMode(mode: AcousticSynthesizer.SoundMode) {
        _selectedSoundMode.value = mode
        triggerVibration(50, 100)
        
        val modeText = when (mode) {
            AcousticSynthesizer.SoundMode.AUDIBLE_PULSE -> "Click Ticker"
            AcousticSynthesizer.SoundMode.HIGH_CHIRP -> "High Frequency sweep"
            AcousticSynthesizer.SoundMode.ULTRASONIC -> "Inaudible Ultrasonic sonar"
            AcousticSynthesizer.SoundMode.STABLE_TONE -> "Continuous Hum tone"
        }
        voiceFeedback.speak("Synthesizer tone changed to $modeText.")
    }

    fun setManualSweep(angle: Float) {
        _manualSweepAngle.value = angle
        recalculateEcholocation()
    }

    fun togglePhysicalCompass() {
        val compassOn = !_isUsingPhysicalCompass.value
        _isUsingPhysicalCompass.value = compassOn
        triggerVibration(50, 120)
        
        if (compassOn) {
            orientationTracker.startListening()
            voiceFeedback.speak("Physical tracking active. Turn the phone in your hands to scan.")
        } else {
            orientationTracker.stopListening()
            _manualSweepAngle.value = 0f
            voiceFeedback.speak("Manual slider navigation active.")
        }
        recalculateEcholocation()
    }

    // Walking controls so user can explore the room map
    fun walkForward(delta: Float) {
        val current = _userForwardPos.value
        val next = (current + delta).coerceIn(0f, 6f)
        _userForwardPos.value = next
        recalculateEcholocation()
        triggerVibration(30, 80)
    }

    fun walkLateral(delta: Float) {
        val current = _userLateralPos.value
        val next = (current + delta).coerceIn(-1.5f, 1.5f)
        _userLateralPos.value = next
        recalculateEcholocation()
        triggerVibration(30, 80)
    }

    // 7. Room DB persistence operations
    fun saveActiveMapping(customName: String, notes: String) {
        viewModelScope.launch {
            val roomName = customName.ifEmpty { "My Floorplan ${savedRooms.value.size + 1}" }
            val savedEntry = SavedRoom(
                name = roomName,
                roomType = _selectedRoomTemplate.value.id,
                obstacleCount = currentObstacles.size,
                notes = notes,
                maxDistance = _obstacleDistance.value
            )
            val rowId = roomRepository.insertRoom(savedEntry)
            Log.d("EchoAppViewModel", "Stored saved mapping ID: $rowId")
            triggerVibration(150, 200)
            voiceFeedback.speak("Echolocation profile saved successfully as $roomName.")
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch {
            roomRepository.deleteRoomById(id)
            triggerVibration(80, 100)
            voiceFeedback.speak("Profile removed from database.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        synthesizer.release()
        audioInput.release()
        voiceFeedback.release()
        orientationTracker.stopListening()
        tactileLoopJob?.cancel()
        voiceTimerJob?.cancel()
    }
}
