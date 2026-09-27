package com.bigcorps.guardian.web

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.bigcorps.guardian.core.GuardianDatabase

class BrowserAccessibilityService :
    AccessibilityService() {
    private data class Observation(
        val host: String,
        val browserPackage: String,
        val privateMode: Boolean
    )

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var lastObservation: Observation? =
        null

    private var lastWallMs =
        0L

    private var lastElapsedMs =
        0L

    private val sampleRunnable =
        object :
            Runnable {
            override fun run() {
                sampleNow()

                handler.postDelayed(
                    this,
                    SAMPLE_INTERVAL_MS
                )
            }
        }

    private val eventRunnable =
        Runnable {
            sampleNow()
        }

    override fun onServiceConnected() {
        super.onServiceConnected()

        BrowserWebPreferences(
            applicationContext
        ).markTrackingStartedIfMissing()

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "WEB_SERVICE_CONNECTED"
            )
        }

        handler.removeCallbacks(
            sampleRunnable
        )

        handler.post(
            sampleRunnable
        )
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (
            !BrowserWebPreferences(
                applicationContext
            ).consented()
        ) {
            return
        }

        handler.removeCallbacks(
            eventRunnable
        )

        handler.postDelayed(
            eventRunnable,
            EVENT_DEBOUNCE_MS
        )
    }

    override fun onInterrupt() {
        sampleAsInactive()
    }

    override fun onDestroy() {
        handler.removeCallbacks(
            sampleRunnable
        )

        handler.removeCallbacks(
            eventRunnable
        )

        sampleAsInactive()

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "WEB_SERVICE_DISCONNECTED"
            )
        }

        super.onDestroy()
    }

    private fun sampleNow() {
        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        if (
            !prefs.consented()
        ) {
            sampleAsInactive()
            return
        }

        val power =
            getSystemService(
                POWER_SERVICE
            ) as
                PowerManager

        if (
            !power.isInteractive
        ) {
            sampleAsInactive()
            return
        }

        val root =
            rootInActiveWindow

        val packageName =
            root
                ?.packageName
                ?.toString()

        val spec =
            BrowserCatalog.spec(
                packageName
            )

        if (
            root ==
            null ||
            spec ==
            null ||
            packageName ==
            null
        ) {
            sampleAsInactive()
            return
        }

        val extraction =
            BrowserAccessibilityExtractor.extractHost(
                root,
                spec
            )

        if (
            extraction.state ==
            "FOCUSED" ||
            extraction.state ==
            "INVALID"
        ) {
            updateObservation(
                null
            )
            return
        }

        val host =
            extraction.host

        if (
            host ==
            null
        ) {
            val previous =
                lastObservation

            if (
                previous !=
                null &&
                previous.browserPackage ==
                    packageName
            ) {
                updateObservation(
                    previous
                )
            } else {
                updateObservation(
                    null
                )
            }

            return
        }

        val mode =
            BrowserAccessibilityExtractor.detectPrivateMode(
                root,
                spec
            )

        prefs.recordDetectorState(
            browserPackage =
                packageName,
            urlBarId =
                extraction.urlBarId,
            privateMode =
                mode.privateMode,
            privateReason =
                mode.reason
        )

        updateObservation(
            Observation(
                host =
                    host,
                browserPackage =
                    packageName,
                privateMode =
                    mode.privateMode
            )
        )
    }

    private fun sampleAsInactive() {
        updateObservation(
            null
        )
    }

    private fun updateObservation(
        current: Observation?
    ) {
        val nowWall =
            System.currentTimeMillis()

        val nowElapsed =
            SystemClock.elapsedRealtime()

        val previous =
            lastObservation

        if (
            previous !=
            null &&
            lastWallMs >
            0L &&
            lastElapsedMs >
            0L
        ) {
            val elapsed =
                nowElapsed -
                    lastElapsedMs

            if (
                elapsed in
                1L..MAX_BANK_GAP_MS &&
                nowWall >
                lastWallMs
            ) {
                runCatching {
                    GuardianDatabase(
                        applicationContext
                    ).recordBrowserChunk(
                        startMs =
                            lastWallMs,
                        endMs =
                            nowWall,
                        host =
                            previous.host,
                        browserPackage =
                            previous.browserPackage,
                        privateMode =
                            previous.privateMode
                    )
                }
            }
        }

        lastObservation =
            current

        lastWallMs =
            nowWall

        lastElapsedMs =
            nowElapsed
    }

    companion object {
        private const val SAMPLE_INTERVAL_MS =
            2_000L

        private const val EVENT_DEBOUNCE_MS =
            220L

        private const val MAX_BANK_GAP_MS =
            8_000L
    }
}
