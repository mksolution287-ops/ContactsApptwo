package com.contactsapptwomktech.utils

import android.media.AudioManager
import android.media.ToneGenerator

object DialTonePlayer {

    private var toneGenerator: ToneGenerator? = null

    fun init() {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 80)
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    fun playTone(digit: Char) {
        val tone = when (digit) {
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '0' -> ToneGenerator.TONE_DTMF_0
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> return
        }

        toneGenerator?.startTone(tone, 150) // duration in ms
    }
}