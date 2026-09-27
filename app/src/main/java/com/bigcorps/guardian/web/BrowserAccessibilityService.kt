package com.bigcorps.guardian.web

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.bigcorps.guardian.core.GuardianDatabase
import java.util.concurrent.Executor

class BrowserAccessibilityService :
    AccessibilityService() {
    private data class Observation(
        val host: String,
        val browserPackage: String,
        val privateMode: Boolean
    )

    private lateinit var observerThread:
        HandlerThread

    private lateinit var observer:
        Handler

    private lateinit var storageThread:
        HandlerThread

    private lateinit var storage:
        Handler

    private lateinit var visualOcr:
        BrowserVisualOcr

    private var currentObservation:
        Observation? =
        null

    private var lastBankWallMs =
        0L

    private var lastBankElapsedMs =
        0L

    private var usageCursorMs =
        0L

    private var usageForegroundPackage:
        String? =
        null

    private var screenshotInFlight =
        false

    private var lastScreenshotElapsedMs =
        0L

    private var candidateHost:
        String? =
        null

    private var candidateHostCount =
        0

    private var privateModeLatched =
        false

    private var privateModeReason =
        "NONE"

    private val observerExecutor =
        Executor {
            command ->
            if (
                ::observer.isInitialized
            ) {
                observer.post(
                    command
                )
            } else {
                command.run()
            }
        }

    private val heartbeat =
        object :
            Runnable {
            override fun run() {
                val prefs =
                    BrowserWebPreferences(
                        applicationContext
                    )

                prefs.recordHeartbeat()

                runCatching {
                    refreshForegroundFromUsage()

                    val browserPackage =
                        usageForegroundPackage
                            ?.takeIf {
                                BrowserCatalog.isSupported(
                                    it
                                )
                            }

                    if (
                        browserPackage ==
                        null
                    ) {
                        stopAccruing()
                        resetVisualSession()
                    } else {
                        maintainCurrentObservation(
                            browserPackage
                        )
                        maybeRequestScreenshot(
                            browserPackage
                        )
                    }
                }.onFailure {
                    prefs.recordVisualPipelineError(
                        it::class.java.simpleName
                    )
                }

                if (
                    ::observer.isInitialized
                ) {
                    observer.postDelayed(
                        this,
                        HEARTBEAT_MS
                    )
                }
            }
        }

    override fun onCreate() {
        super.onCreate()

        observerThread =
            HandlerThread(
                "GuardianWebVisual"
            ).apply {
                start()
            }

        observer =
            Handler(
                observerThread.looper
            )

        storageThread =
            HandlerThread(
                "GuardianWebStorage"
            ).apply {
                start()
            }

        storage =
            Handler(
                storageThread.looper
            )

        visualOcr =
            BrowserVisualOcr()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        prefs.markTrackingStartedIfMissing()
        prefs.recordServiceConnected()

        usageCursorMs =
            System.currentTimeMillis() -
                INITIAL_USAGE_LOOKBACK_MS

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "WEB_SERVICE_CONNECTED"
            )
        }

        observer.removeCallbacks(
            heartbeat
        )

        observer.post(
            heartbeat
        )
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (
            event ==
            null
        ) {
            return
        }

        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        if (
            !prefs.consented()
        ) {
            return
        }

        val packageName =
            event.packageName
                ?.toString()
                ?.takeIf {
                    BrowserCatalog.isSupported(
                        it
                    )
                }
                ?: return

        prefs.recordAccessibilityEvent(
            browserPackage =
                packageName,
            eventType =
                event.eventType
        )

        if (
            ::observer.isInitialized
        ) {
            observer.post {
                usageForegroundPackage =
                    packageName

                maybeRequestScreenshot(
                    packageName
                )
            }
        }
    }

    override fun onInterrupt() {
        if (
            ::observer.isInitialized
        ) {
            observer.post {
                stopAccruing()
                resetVisualSession()
            }
        }
    }

    override fun onDestroy() {
        if (
            ::observer.isInitialized
        ) {
            observer.removeCallbacks(
                heartbeat
            )

            observer.post {
                stopAccruing()
                resetVisualSession()
            }
        }

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "WEB_SERVICE_DISCONNECTED"
            )
        }

        if (
            ::visualOcr.isInitialized
        ) {
            runCatching {
                visualOcr.close()
            }
        }

        if (
            ::observerThread.isInitialized
        ) {
            observerThread.quitSafely()
        }

        if (
            ::storageThread.isInitialized
        ) {
            storageThread.quitSafely()
        }

        super.onDestroy()
    }

    private fun maybeRequestScreenshot(
        browserPackage: String
    ) {
        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        if (
            !prefs.consented() ||
            Build.VERSION.SDK_INT <
                Build.VERSION_CODES.R ||
            !BrowserCatalog.isSupported(
                browserPackage
            ) ||
            screenshotInFlight
        ) {
            return
        }

        val nowElapsed =
            SystemClock.elapsedRealtime()

        if (
            nowElapsed -
                lastScreenshotElapsedMs <
            SCREENSHOT_MIN_INTERVAL_MS
        ) {
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
            return
        }

        lastScreenshotElapsedMs =
            nowElapsed

        screenshotInFlight =
            true

        prefs.recordScreenshotRequested()

        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            observerExecutor,
            object :
                AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(
                    screenshot:
                        AccessibilityService.ScreenshotResult
                ) {
                    prefs.recordScreenshotSuccess()

                    try {
                        processScreenshot(
                            screenshot,
                            browserPackage
                        )
                    } catch (
                        t: Throwable
                    ) {
                        prefs.recordVisualPipelineError(
                            t::class.java.simpleName
                        )
                    } finally {
                        screenshotInFlight =
                            false
                    }
                }

                override fun onFailure(
                    errorCode: Int
                ) {
                    prefs.recordScreenshotFailure(
                        errorCode
                    )

                    screenshotInFlight =
                        false
                }
            }
        )
    }

    private fun processScreenshot(
        screenshot:
            AccessibilityService.ScreenshotResult,
        browserPackage: String
    ) {
        val hardwareBuffer =
            screenshot.hardwareBuffer

        val colorSpace =
            screenshot.colorSpace
                ?: ColorSpace.get(
                    ColorSpace.Named.SRGB
                )

        val wrapped =
            Bitmap.wrapHardwareBuffer(
                hardwareBuffer,
                colorSpace
            )

        if (
            wrapped ==
            null
        ) {
            hardwareBuffer.close()

            BrowserWebPreferences(
                applicationContext
            ).recordVisualPipelineError(
                "BitmapWrapFailed"
            )

            return
        }

        val software =
            try {
                wrapped.copy(
                    Bitmap.Config.ARGB_8888,
                    false
                )
            } finally {
                hardwareBuffer.close()
                wrapped.recycle()
            }

        if (
            software ==
            null
        ) {
            BrowserWebPreferences(
                applicationContext
            ).recordVisualPipelineError(
                "BitmapCopyFailed"
            )

            return
        }

        try {
            val allowModeProbe =
                !privateModeLatched &&
                    currentObservation ==
                        null

            val result =
                visualOcr.analyze(
                    bitmap =
                        software,
                    allowModeProbe =
                        allowModeProbe
                )

            BrowserWebPreferences(
                applicationContext
            ).recordVisualOcr(
                host =
                    result.host,
                privateDetected =
                    result.privateModeDetected,
                privateReason =
                    result.privateReason
            )

            if (
                result.privateModeDetected
            ) {
                privateModeLatched =
                    true

                privateModeReason =
                    result.privateReason
            }

            observeVisualHost(
                host =
                    result.host,
                browserPackage =
                    browserPackage
            )
        } finally {
            software.recycle()
        }
    }

    private fun observeVisualHost(
        host: String?,
        browserPackage: String
    ) {
        if (
            host ==
            null
        ) {
            return
        }

        if (
            candidateHost ==
            host
        ) {
            candidateHostCount +=
                1
        } else {
            candidateHost =
                host

            candidateHostCount =
                1
        }

        if (
            candidateHostCount <
            REQUIRED_CONSECUTIVE_HOST_READS
        ) {
            return
        }

        val observation =
            Observation(
                host =
                    host,
                browserPackage =
                    browserPackage,
                privateMode =
                    privateModeLatched
            )

        BrowserWebPreferences(
            applicationContext
        ).recordDetectorState(
            browserPackage =
                browserPackage,
            urlBarId =
                "VISUAL_OCR_TOOLBAR",
            privateMode =
                privateModeLatched,
            privateReason =
                privateModeReason
        )

        updateObservation(
            observation
        )
    }

    private fun refreshForegroundFromUsage() {
        val manager =
            getSystemService(
                UsageStatsManager::class.java
            )
                ?: return

        val now =
            System.currentTimeMillis()

        val begin =
            if (
                usageCursorMs >
                    0L &&
                usageCursorMs <
                    now
            ) {
                usageCursorMs
            } else {
                now -
                    INITIAL_USAGE_LOOKBACK_MS
            }

        val events =
            runCatching {
                manager.queryEvents(
                    begin,
                    now
                )
            }.getOrNull()
                ?: return

        val event =
            UsageEvents.Event()

        var newestTs =
            Long.MIN_VALUE

        var newestPackage:
            String? =
            null

        while (
            events.hasNextEvent()
        ) {
            events.getNextEvent(
                event
            )

            val eventPackage =
                event.packageName

            val className =
                event.className
                    .orEmpty()

            if (
                BrowserCatalog.isSupported(
                    eventPackage
                ) &&
                (
                    className.contains(
                        "IncognitoTabLauncher",
                        ignoreCase =
                            true
                    ) ||
                    className.contains(
                        "IncognitoDocumentActivity",
                        ignoreCase =
                            true
                    )
                    )
            ) {
                privateModeLatched =
                    true

                privateModeReason =
                    "USAGE_EVENT_INCOGNITO_ACTIVITY"
            }

            if (
                event.eventType ==
                UsageEvents.Event.ACTIVITY_RESUMED &&
                event.timeStamp >=
                    newestTs
            ) {
                newestTs =
                    event.timeStamp

                newestPackage =
                    eventPackage
            }
        }

        if (
            newestPackage !=
            null
        ) {
            val previous =
                usageForegroundPackage

            usageForegroundPackage =
                newestPackage

            if (
                previous !=
                    newestPackage &&
                !BrowserCatalog.isSupported(
                    newestPackage
                )
            ) {
                stopAccruing()
                resetVisualSession()
            }
        }

        usageCursorMs =
            (
                now -
                    1000L
                )
                .coerceAtLeast(
                    0L
                )
    }

    private fun maintainCurrentObservation(
        browserPackage: String
    ) {
        val observation =
            currentObservation
                ?: return

        if (
            observation.browserPackage !=
            browserPackage
        ) {
            stopAccruing()
            resetVisualSession()
            return
        }

        bankCurrent(
            observation
        )
    }

    private fun updateObservation(
        next: Observation
    ) {
        val current =
            currentObservation

        if (
            current ==
            next
        ) {
            return
        }

        current?.let {
            bankCurrent(
                it
            )
        }

        currentObservation =
            next

        lastBankWallMs =
            System.currentTimeMillis()

        lastBankElapsedMs =
            SystemClock.elapsedRealtime()
    }

    private fun bankCurrent(
        observation: Observation
    ) {
        val nowWall =
            System.currentTimeMillis()

        val nowElapsed =
            SystemClock.elapsedRealtime()

        if (
            lastBankWallMs <=
                0L ||
            lastBankElapsedMs <=
                0L
        ) {
            lastBankWallMs =
                nowWall

            lastBankElapsedMs =
                nowElapsed

            return
        }

        val elapsed =
            nowElapsed -
                lastBankElapsedMs

        if (
            elapsed <=
                0L ||
            elapsed >
                MAX_BANK_GAP_MS
        ) {
            lastBankWallMs =
                nowWall

            lastBankElapsedMs =
                nowElapsed

            return
        }

        val start =
            lastBankWallMs

        val end =
            nowWall

        lastBankWallMs =
            nowWall

        lastBankElapsedMs =
            nowElapsed

        if (
            end <=
            start
        ) {
            return
        }

        storage.post {
            runCatching {
                GuardianDatabase(
                    applicationContext
                ).recordBrowserChunk(
                    startMs =
                        start,
                    endMs =
                        end,
                    host =
                        observation.host,
                    browserPackage =
                        observation.browserPackage,
                    privateMode =
                        observation.privateMode
                )
            }.onFailure {
                BrowserWebPreferences(
                    applicationContext
                ).recordVisualPipelineError(
                    it::class.java.simpleName
                )
            }
        }
    }

    private fun stopAccruing() {
        currentObservation
            ?.let {
                bankCurrent(
                    it
                )
            }

        currentObservation =
            null

        lastBankWallMs =
            0L

        lastBankElapsedMs =
            0L
    }

    private fun resetVisualSession() {
        candidateHost =
            null

        candidateHostCount =
            0

        privateModeLatched =
            false

        privateModeReason =
            "NONE"
    }

    companion object {
        private const val HEARTBEAT_MS =
            2500L

        private const val SCREENSHOT_MIN_INTERVAL_MS =
            3500L

        private const val REQUIRED_CONSECUTIVE_HOST_READS =
            2

        private const val MAX_BANK_GAP_MS =
            15_000L

        private const val INITIAL_USAGE_LOOKBACK_MS =
            5L *
                60L *
                1000L
    }
}
