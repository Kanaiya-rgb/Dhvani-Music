package com.mrsep.musicrecognizer.core.recognition.shazam

object SongRecSignature {

    init {
        try {
            System.loadLibrary("songrecfp")
        } catch (t: Throwable) {
            android.util.Log.e("SongRecSignature", "Failed to load libsongrecfp.so", t)
        }
    }

    const val REQUIRED_SAMPLE_RATE = 16_000

    /**
     * Accepts a ShortArray (mono, PCM int16, 16_000 Hz),
     * returns the encoded signature string, e.g. "data:audio/vnd.shazam.sig;base64,..."
     */
    @JvmStatic
    external fun fromPcm16Mono16kHz(samples: ShortArray): String
}
