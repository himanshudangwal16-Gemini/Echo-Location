package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AcousticSynthesizer {

    enum class SoundMode {
        AUDIBLE_PULSE, // Classic rapid sonar tick
        HIGH_CHIRP,    // High-pitched 2.4kHz - 5.5kHz swept chirp
        ULTRASONIC,    // Inaudible ultrasonic 18kHz - 21kHz sweep (requires good speakers)
        STABLE_TONE    // Continuous variable-pitch hum
    }

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var synthesizerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Dynamic runtime states updated by ViewModel (and simulated sensors)
    var isEnabled: Boolean = false
        set(value) {
            field = value
            if (value) {
                startSynthesizerLoop()
            } else {
                stopSynthesizerLoop()
            }
        }

    var soundMode: SoundMode = SoundMode.HIGH_CHIRP
    var currentDistance: Float = 2.5f // Wall distance in meters (0.1m - 6.0m)
    var obstacleAngle: Float = 0f    // Direction of obstacle (-90 deg (left) to +90 deg (right))
    var masterVolume: Float = 0.8f

    init {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("AcousticSynthesizer", "Failed to initialize AudioTrack: ${e.message}")
        }
    }

    private fun startSynthesizerLoop() {
        if (synthesizerJob?.isActive == true) return

        synthesizerJob = scope.launch {
            var phase = 0.0
            while (isActive) {
                if (!isEnabled) {
                    delay(200)
                    continue
                }

                val dist = currentDistance.coerceIn(0.1f, 6.0f)
                val angle = obstacleAngle.coerceIn(-90f, 90f)

                // Calculate 3D Spatial Audio Volume Balance (stereo panning)
                // If angle is positive (right), left channel is softer.
                // If angle is negative (left), right channel is softer.
                val rad = Math.toRadians(angle.toDouble())
                val rightGain = (sin(rad) + 1.0) / 2.0 // 0.0 to 1.0
                val leftGain = 1.0 - rightGain

                // Master volume scale
                val volLeft = (leftGain.toFloat() * masterVolume).coerceIn(0f, 1f)
                val volRight = (rightGain.toFloat() * masterVolume).coerceIn(0f, 1f)

                try {
                    audioTrack?.setVolume(masterVolume) // overall scaling
                } catch (e: Exception) {
                    // Ignore on older SDKs or unexpected states
                }

                when (soundMode) {
                    SoundMode.AUDIBLE_PULSE -> {
                        // Classical clicker: short clicks whose repeating rhythm speeds up as distance drops
                        val clickBuffer = generateClickSample(0.02f, volLeft, volRight) // 20ms pulse
                        audioTrack?.write(clickBuffer, 0, clickBuffer.size)
                        
                        // Active interval scales with distance: 0.1m -> 70ms interval, 6.0m -> 1200ms
                        val repeatDelay = ((dist / 6.0f) * 1130f + 70f).toLong()
                        delay(repeatDelay)
                    }

                    SoundMode.HIGH_CHIRP -> {
                        // High sweeping chirp from 2400Hz to 5500Hz
                        val chirpBuffer = generateSweepSample(0.04f, 2400.0, 5500.0, volLeft, volRight)
                        audioTrack?.write(chirpBuffer, 0, chirpBuffer.size)

                        // Delay between chirps scales with distance: 0.1m -> 100ms, 6.0m -> 1500ms
                        val repeatDelay = ((dist / 6.0f) * 1400f + 100f).toLong()
                        delay(repeatDelay)
                    }

                    SoundMode.ULTRASONIC -> {
                        // Inaudible sweep from 18000Hz to 21500Hz
                        val chirpBuffer = generateSweepSample(0.04f, 18000.0, 21500.0, volLeft, volRight)
                        audioTrack?.write(chirpBuffer, 0, chirpBuffer.size)

                        val repeatDelay = ((dist / 6.0f) * 1400f + 100f).toLong()
                        delay(repeatDelay)
                    }

                    SoundMode.STABLE_TONE -> {
                        // Continuous variable hum
                        // Pitch goes higher as obstacle gets closer: 300Hz (far) to 1800Hz (very close)
                        val targetFrequency = 1800.0 - ((dist / 6.0f) * 1500.0)
                        val bufferLength = 1024 // Small blocks for fast updates
                        val toneBuffer = ShortArray(bufferLength * 2) // Stereo

                        for (i in 0 until bufferLength) {
                            val t = i.toDouble() / sampleRate
                            val sampleVal = (sin(phase + 2.0 * Math.PI * targetFrequency * t) * 32767.0).toInt().toShort()
                            
                            toneBuffer[i * 2] = (sampleVal * volLeft).toInt().toShort()     // Left
                            toneBuffer[i * 2 + 1] = (sampleVal * volRight).toInt().toShort() // Right
                        }
                        
                        phase += 2.0 * Math.PI * targetFrequency * (bufferLength.toDouble() / sampleRate)
                        // Wrap phase to avoid overflow
                        while (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }

                        audioTrack?.write(toneBuffer, 0, toneBuffer.size)
                        delay(12) // Low sleep for stream smoothness
                    }
                }
            }
        }
    }

    private fun stopSynthesizerLoop() {
        synthesizerJob?.cancel()
        synthesizerJob = null
    }

    /**
     * Generates stereo linear Frequency Modulated sweep (Chirp)
     */
    private fun generateSweepSample(
        durationSec: Float,
        f0: Double,
        f1: Double,
        leftGain: Float,
        rightGain: Float
    ): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val data = ShortArray(numSamples * 2) // Stereo interleaving

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Linear frequency modulation formula: phase = 2 * pi * (f0 * t + beta * t^2) where beta = (f1 - f0) / (2 * duration)
            val angle = 2.0 * Math.PI * (f0 * t + ((f1 - f0) / (2.0 * durationSec)) * t * t)
            
            // Envelope smoothing (tapered window to avoid sharp transient clicks at sound edges)
            val envelope = if (t < 0.005) {
                t / 0.005 // fade in
            } else if (t > durationSec - 0.005) {
                (durationSec - t) / 0.005 // fade out
            } else {
                1.0
            }

            val rawSample = (sin(angle) * envelope * 32767.0).toInt().toShort()
            data[i * 2] = (rawSample * leftGain).toInt().toShort()
            data[i * 2 + 1] = (rawSample * rightGain).toInt().toShort()
        }
        return data
    }

    /**
     * Generates stereo brief acoustic pulse (Click)
     */
    private fun generateClickSample(
        durationSec: Float,
        leftGain: Float,
        rightGain: Float
    ): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val data = ShortArray(numSamples * 2)
        val pulseFrequency = 1800.0 // crisp clicks at 1.8kHz

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val angle = 2.0 * Math.PI * pulseFrequency * t
            
            // Fast exponential decay envelope
            val envelope = Math.exp(-t * 200.0) // quick damping

            val rawSample = (sin(angle) * envelope * 32767.0).toInt().toShort()
            data[i * 2] = (rawSample * leftGain).toInt().toShort()
            data[i * 2 + 1] = (rawSample * rightGain).toInt().toShort()
        }
        return data
    }

    fun release() {
        isEnabled = false
        stopSynthesizerLoop()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Safe cleanup
        }
        audioTrack = null
    }
}
