package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceFeedback(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    var isVoiceEnabled: Boolean = true

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "TTS Initialization failed: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceFeedback", "US English Language is not supported on this device.")
            } else {
                isInitialized = true
                Log.i("VoiceFeedback", "TTS Engine successfully initialized.")
                // Gentle launch greeting
                speak("Echo-Location service has launched. Hold device vertically to begin scanning.")
            }
        } else {
            Log.e("VoiceFeedback", "TTS Initialization Status Failed.")
        }
    }

    fun speak(text: String, overrideCurrent: Boolean = true) {
        if (!isVoiceEnabled || !isInitialized) return
        try {
            val queueMode = if (overrideCurrent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, queueMode, null, "EchoLocationMsgId")
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "TTS play error: ${e.message}")
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // Safe cleanup
        }
        tts = null
        isInitialized = false
    }
}
