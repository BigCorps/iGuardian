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
    val privateReason: String
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
                null,
                false,
                "INVALID_BITMAP"
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

            if (
                toolbarParsed.host !=
                    null ||
                toolbarParsed.privateModeDetected ||
                !allowModeProbe
            ) {
                return BrowserVisualOcrResult(
                    host =
                        toolbarParsed.host,
                    privateModeDetected =
                        toolbarParsed.privateModeDetected,
                    privateReason =
                        toolbarParsed.privateReason
                )
            }
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
                    null,
                privateModeDetected =
                    privateDetected,
                privateReason =
                    if (
                        privateDetected
                    ) {
                        "VISUAL_INCOGNITO_START_PAGE"
                    } else {
                        "NO_VISUAL_PRIVATE_TEXT"
                    }
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
        private const val TOOLBAR_HEIGHT_RATIO =
            0.22

        private const val MODE_PROBE_HEIGHT_RATIO =
            0.62

        private const val OCR_TIMEOUT_SECONDS =
            3L
    }
}
