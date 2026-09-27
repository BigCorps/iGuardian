package com.bigcorps.guardian.web

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class BrowserVisualOcrResult(
    val host: String?,
    val privateModeDetected: Boolean,
    val privateReason: String,
    val privateProbeAttempted: Boolean
)

class BrowserVisualOcr {
    private val recognizer:
        TextRecognizer =
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    fun analyze(
        bitmap: Bitmap,
        allowModeProbe: Boolean
    ): BrowserVisualOcrResult {
        val width =
            bitmap.width

        val height =
            bitmap.height

        if (
            width <=
                0 ||
            height <=
                0
        ) {
            return BrowserVisualOcrResult(
                host = null,
                privateModeDetected = false,
                privateReason = "INVALID_BITMAP",
                privateProbeAttempted = false
            )
        }

        val toolbarHeight =
            (
                height *
                    TOOLBAR_HEIGHT_RATIO
                )
                .roundToInt()
                .coerceIn(
                    1,
                    height
                )

        val toolbar =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                width,
                toolbarHeight
            )

        var toolbarHost:
            String? =
            null

        try {
            val toolbarText =
                recognize(
                    toolbar
                )

            val toolbarLines =
                toolbarText.textBlocks
                    .flatMap {
                        it.lines
                    }
                    .map {
                        it.text
                    }

            val toolbarParsed =
                BrowserVisualTextParser.parse(
                    toolbarTexts =
                        toolbarLines,
                    modeProbeText =
                        toolbarText.text
                )

            toolbarHost =
                toolbarParsed.host

            if (
                toolbarParsed.privateModeDetected ||
                !allowModeProbe
            ) {
                return BrowserVisualOcrResult(
                    host =
                        toolbarHost,
                    privateModeDetected =
                        toolbarParsed.privateModeDetected,
                    privateReason =
                        toolbarParsed.privateReason,
                    privateProbeAttempted =
                        allowModeProbe
                )
            }

            // 0.1.23 returned as soon as a toolbar host existed, which meant
            // the larger incognito-start-page probe could remain at zero. In
            // Hybrid v3, a known host no longer suppresses the private probe.
        } finally {
            toolbar.recycle()
        }

        val probeHeight =
            (
                height *
                    MODE_PROBE_HEIGHT_RATIO
                )
                .roundToInt()
                .coerceIn(
                    1,
                    height
                )

        val probe =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                width,
                probeHeight
            )

        try {
            val probeText =
                recognize(
                    probe
                )

            val privateDetected =
                BrowserVisualTextParser
                    .detectsPrivateModeText(
                        probeText.text
                    )

            return BrowserVisualOcrResult(
                host =
                    toolbarHost,
                privateModeDetected =
                    privateDetected,
                privateReason =
                    if (
                        privateDetected
                    ) {
                        "VISUAL_INCOGNITO_START_PAGE"
                    } else {
                        "NO_VISUAL_PRIVATE_TEXT"
                    },
                privateProbeAttempted =
                    true
            )
        } finally {
            probe.recycle()
        }
    }

    private fun recognize(
        bitmap: Bitmap
    ) =
        Tasks.await(
            recognizer.process(
                InputImage.fromBitmap(
                    bitmap,
                    0
                )
            ),
            OCR_TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        )

    fun close() {
        recognizer.close()
    }

    companion object {
        // Keep host OCR inside the browser chrome. 0.1.24 used 22% of the
        // display, which reached page content on tall phones and produced false
        // host-shaped words. Private-mode probing uses a separate deep crop.
        private const val TOOLBAR_HEIGHT_RATIO =
            0.16

        // Chromium's redesigned private-tab explanation can extend below
        // the first 62% of the display. Keep the toolbar crop small for host
        // recognition, but use a deeper one-off private-mode probe.
        private const val MODE_PROBE_HEIGHT_RATIO =
            0.90

        private const val OCR_TIMEOUT_SECONDS =
            3L
    }
}
