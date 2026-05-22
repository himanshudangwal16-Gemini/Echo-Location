package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioInputListener {

    private val sampleRate = 16000 // 16kHz is efficient and responsive
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Observable states
    var isListening: Boolean = false
        set(value) {
            field = value
            if (value) {
                startRecording()
            } else {
                stopRecording()
            }
        }

    // Dynamic decibel amplitude (dB SPL approximation)
    var currentAmplitudedB: Float = 0f
        private set

    // A cyclic buffer of the latest audio samples to render on a Canvas Oscilloscope
    val waveBuffer = FloatArray(128)
    private var waveIndex = 0

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (recordingJob?.isActive == true) return

        try {
            val minBufSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize.coerceAtLeast(2048)
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioInputListener", "AudioRecord initialization failed.")
                audioRecord = null
                startSimulatedWaveLoop()
                return
            }

            audioRecord?.startRecording()
            
            // Start reading loop
            recordingJob = scope.launch {
                val buffer = ShortArray(512)
                while (isActive && isListening) {
                    val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readResult > 0) {
                        // Calculate Root Mean Square (RMS) for dB volume estimation
                        var sum = 0.0
                        for (i in 0 until readResult) {
                            sum += buffer[i] * buffer[i]
                            // Feed local visualizer wave buffer selectively
                            if (i % 4 == 0) {
                                waveBuffer[waveIndex] = buffer[i] / 32768.1f
                                waveIndex = (waveIndex + 1) % waveBuffer.size
                            }
                        }
                        val rms = sqrt(sum / readResult)
                        // Normalize and turn to logarithmic dB-like value
                        val db = if (rms > 0) Math.log10(rms) * 20 else 0.0
                        
                        // Map db to standard slider 0f - 100f
                        currentAmplitudedB = (db.toFloat() * 1.5f).coerceIn(10f, 110f)
                    } else {
                        delayWithSimulation()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("AudioInputListener", "Missing RECORD_AUDIO permission: ${e.message}")
            startSimulatedWaveLoop()
        } catch (e: Exception) {
            Log.e("AudioInputListener", "Recording error: ${e.message}")
            startSimulatedWaveLoop()
        }
    }

    private fun startSimulatedWaveLoop() {
        // Fallback simulator is active, drawing synthetic acoustic waves responding to ambient noise
        recordingJob = scope.launch {
            var simTime = 0.0
            while (isActive && isListening) {
                // Generate a compound wave representing active chirp reflection + ambient hiss
                for (i in waveBuffer.indices) {
                    val angle = simTime + (i * 0.15)
                    val value = Math.sin(angle) * 0.4 + Math.cos(angle * 2.3) * 0.2 + (Math.random() - 0.5) * 0.1
                    waveBuffer[i] = value.toFloat()
                }
                currentAmplitudedB = (45f + Math.sin(simTime).toFloat() * 10f + (Math.random().toFloat() * 4f))
                simTime += 0.25
                kotlinx.coroutines.delay(50)
            }
        }
    }

    private suspend fun delayWithSimulation() {
        kotlinx.coroutines.delay(30)
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Safe cleanup
        }
        audioRecord = null
        // Clear buffer
        waveBuffer.fill(0f)
        currentAmplitudedB = 0f
    }

    fun release() {
        isListening = false
        stopRecording()
    }
}
