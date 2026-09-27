package com.bigcorps.guardian.web

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.bigcorps.guardian.core.FinancialAppCatalog
import com.bigcorps.guardian.core.GuardianDatabase
import java.util.concurrent.Executor

class BrowserAccessibilityService :
    AccessibilityService() {
    private data class Observation(
        val host: String,
        val browserPackage: String,
        val privateMode: Boolean
    )

    private data class TreeProbe(
        val host: String?,
        val urlBarId: String?,
        val state: String,
        val source: String,
        val privateDetected: Boolean,
        val privateReason: String,
        val windowId: Int?,
        val eventSourceAvailable: Boolean,
        val rootResolved: Boolean
    )

    private data class WindowRoot(
        val root: AccessibilityNodeInfo,
        val windowId: Int?,
        val source: String
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

    private var lastBrowserWindowId:
        Int? =
        null

    private var lastResourceIdProbeElapsedMs =
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

    @Volatile
    private var bankModeDisableRequested =
        false

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
                        resetHybridSession()
                    } else {
                        maintainCurrentObservation(
                            browserPackage
                        )

                        val treeHostFound =
                            probeTreeFromWindows(
                                browserPackage
                            )

                        if (
                            !treeHostFound ||
                            !privateModeLatched
                        ) {
                            maybeRequestScreenshot(
                                browserPackage
                            )
                        }
                    }
                }.onFailure {
                    prefs.recordVisualPipelineError(
                        it::class.java.simpleName
                    )
                }

                if (
                    ::observer.isInitialized &&
                    !bankModeDisableRequested
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
                "GuardianWebHybrid"
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

        activeInstance =
            this

        bankModeDisableRequested =
            false

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
            null ||
            bankModeDisableRequested
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

        // Intentionally synchronous and cheap: only direct known IDs are read
        // while the event/source are still valid. No BFS, OCR or SQLite here.
        val treeProbe =
            runCatching {
                probeTreeDirectOnCallback(
                    event,
                    packageName
                )
            }.getOrElse {
                TreeProbe(
                    host = null,
                    urlBarId = null,
                    state = "CALLBACK_ERROR",
                    source = "EVENT_CALLBACK",
                    privateDetected = false,
                    privateReason = "CALLBACK_ERROR",
                    windowId = event.windowId.takeIf { id -> id >= 0 },
                    eventSourceAvailable = false,
                    rootResolved = false
                )
            }

        if (
            ::observer.isInitialized
        ) {
            observer.post {
                usageForegroundPackage =
                    packageName

                processTreeProbe(
                    packageName,
                    treeProbe
                )

                if (
                    treeProbe.host ==
                        null ||
                    !privateModeLatched
                ) {
                    maybeRequestScreenshot(
                        packageName,
                        treeProbe.windowId
                    )
                }
            }
        }
    }

    override fun onInterrupt() {
        if (
            ::observer.isInitialized
        ) {
            observer.post {
                stopAccruing()
                resetHybridSession()
            }
        }
    }

    override fun onDestroy() {
        if (
            activeInstance ===
            this
        ) {
            activeInstance =
                null
        }

        if (
            ::observer.isInitialized
        ) {
            observer.removeCallbacks(
                heartbeat
            )

            observer.post {
                stopAccruing()
                resetHybridSession()
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

    /**
     * Hot-path direct probe. It deliberately avoids a tree walk. We query the
     * event source, active root and matching application-window roots only by
     * BrowserCatalog's known address-bar IDs.
     */
    private fun probeTreeDirectOnCallback(
        event: AccessibilityEvent,
        browserPackage: String
    ): TreeProbe {
        val spec =
            BrowserCatalog.spec(
                browserPackage
            )
                ?: return TreeProbe(
                    host = null,
                    urlBarId = null,
                    state = "UNSUPPORTED_WINDOW",
                    source = "NONE",
                    privateDetected = false,
                    privateReason = "UNSUPPORTED_BROWSER",
                    windowId = null,
                    eventSourceAvailable = false,
                    rootResolved = false
                )

        val eventSource =
            runCatching {
                event.source
            }.getOrNull()

        val candidates =
            mutableListOf<Triple<AccessibilityNodeInfo, String, Int?>>()

        eventSource?.let {
            candidates +=
                Triple(
                    it,
                    "EVENT_SOURCE_DIRECT",
                    event.windowId.takeIf { id -> id >= 0 }
                )
        }

        runCatching {
            rootInActiveWindow
        }.getOrNull()
            ?.takeIf {
                nodeMatchesBrowser(
                    it,
                    browserPackage
                )
            }
            ?.let {
                candidates +=
                    Triple(
                        it,
                        "ACTIVE_ROOT_DIRECT",
                        event.windowId.takeIf { id -> id >= 0 }
                    )
            }

        runCatching {
            windows
        }.getOrNull()
            .orEmpty()
            .asSequence()
            .filter {
                it.type ==
                    AccessibilityWindowInfo.TYPE_APPLICATION
            }
            .forEach {
                window ->
                val root =
                    runCatching {
                        window.root
                    }.getOrNull()

                if (
                    root !=
                        null &&
                    nodeMatchesBrowser(
                        root,
                        browserPackage
                    )
                ) {
                    candidates +=
                        Triple(
                            root,
                            "APPLICATION_WINDOW_DIRECT",
                            window.id
                        )
                }
            }

        var bestState =
            "MISSING_DIRECT"

        var bestUrlBarId:
            String? =
            null

        var privateDetected =
            false

        var privateReason =
            "NO_DIRECT_PRIVATE_MARKER"

        var privateWindowId:
            Int? =
            event.windowId.takeIf {
                it >=
                    0
            }

        candidates.forEach {
            (root, source, windowId) ->
            val mode =
                BrowserAccessibilityExtractor
                    .detectPrivateModeDirect(
                        root,
                        spec
                    )

            if (
                mode.privateMode
            ) {
                privateDetected =
                    true
                privateReason =
                    mode.reason
                privateWindowId =
                    windowId
            }

            val extraction =
                BrowserAccessibilityExtractor
                    .extractHost(
                        root,
                        spec,
                        allowFallback =
                            false
                    )

            if (
                extraction.host !=
                null
            ) {
                return TreeProbe(
                    host = extraction.host,
                    urlBarId = extraction.urlBarId,
                    state = extraction.state,
                    source = source,
                    privateDetected = privateDetected || mode.privateMode,
                    privateReason = if (mode.privateMode) mode.reason else privateReason,
                    windowId = windowId ?: privateWindowId,
                    eventSourceAvailable = eventSource != null,
                    rootResolved = true
                )
            }

            if (
                extraction.state ==
                    "FOCUSED" ||
                extraction.state ==
                    "INVALID"
            ) {
                bestState =
                    extraction.state
                bestUrlBarId =
                    extraction.urlBarId
            }
        }

        return TreeProbe(
            host = null,
            urlBarId = bestUrlBarId,
            state = bestState,
            source = if (candidates.isEmpty()) "NO_ROOT" else "DIRECT_IDS",
            privateDetected = privateDetected,
            privateReason = privateReason,
            windowId = privateWindowId,
            eventSourceAvailable = eventSource != null,
            rootResolved = candidates.isNotEmpty()
        )
    }

    private fun processTreeProbe(
        browserPackage: String,
        probe: TreeProbe
    ) {
        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        probe.windowId?.let {
            if (
                it >=
                0
            ) {
                lastBrowserWindowId =
                    it
            }
        }

        prefs.recordEventSourceResult(
            browserPackage =
                browserPackage,
            sourceAvailable =
                probe.eventSourceAvailable,
            rootResolved =
                probe.rootResolved
        )

        prefs.recordSample(
            browserPackage =
                browserPackage,
            state =
                probe.state,
            hostFound =
                probe.host !=
                    null
        )

        prefs.recordTreeProbe(
            browserPackage =
                browserPackage,
            state =
                probe.state,
            source =
                probe.source,
            urlBarId =
                probe.urlBarId,
            hostFound =
                probe.host !=
                    null,
            privateDetected =
                probe.privateDetected,
            privateReason =
                probe.privateReason
        )

        if (
            probe.privateDetected
        ) {
            latchPrivateMode(
                browserPackage,
                probe.privateReason
            )
        }

        probe.host?.let {
            acceptTreeHost(
                host =
                    it,
                browserPackage =
                    browserPackage,
                urlBarId =
                    probe.urlBarId,
                source =
                    probe.source
            )
        }
    }

    /**
     * Worker-thread tree pass. This may use a bounded BFS fallback and is also
     * where the resource-ID-only diagnostic inventory is produced.
     */
    private fun probeTreeFromWindows(
        browserPackage: String
    ): Boolean {
        val spec =
            BrowserCatalog.spec(
                browserPackage
            )
                ?: return false

        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        val resolved =
            resolveBrowserWindowRoot(
                browserPackage
            )

        if (
            resolved ==
            null
        ) {
            prefs.recordSample(
                browserPackage,
                "ROOT_NULL",
                false
            )

            prefs.recordTreeProbe(
                browserPackage = browserPackage,
                state = "ROOT_NULL",
                source = "WINDOW_WORKER",
                urlBarId = null,
                hostFound = false,
                privateDetected = false,
                privateReason = "NO_WINDOW_ROOT"
            )

            return false
        }

        resolved.windowId?.let {
            if (
                it >=
                0
            ) {
                lastBrowserWindowId =
                    it
            }
        }

        return runCatching {
            val extraction =
                BrowserAccessibilityExtractor
                    .extractHost(
                        resolved.root,
                        spec,
                        allowFallback =
                            true
                    )

            val mode =
                BrowserAccessibilityExtractor
                    .detectPrivateMode(
                        resolved.root,
                        spec
                    )

            prefs.recordSample(
                browserPackage,
                extraction.state,
                extraction.host !=
                    null
            )

            prefs.recordTreeProbe(
                browserPackage = browserPackage,
                state = extraction.state,
                source = resolved.source,
                urlBarId = extraction.urlBarId,
                hostFound = extraction.host != null,
                privateDetected = mode.privateMode,
                privateReason = mode.reason,
                normalDetected = mode.normalModeDetected
            )

            if (
                mode.privateMode
            ) {
                latchPrivateMode(
                    browserPackage,
                    mode.reason
                )
            } else if (
                mode.normalModeDetected
            ) {
                clearPrivateMode(
                    browserPackage,
                    mode.reason
                )
            }

            maybeRecordResourceIds(
                browserPackage,
                resolved.root
            )

            extraction.host?.let {
                acceptTreeHost(
                    host = it,
                    browserPackage = browserPackage,
                    urlBarId = extraction.urlBarId,
                    source = resolved.source
                )
            }

            extraction.host !=
                null
        }.onFailure {
            prefs.recordSampleError(
                it::class.java.simpleName
            )
        }.getOrDefault(
            false
        )
    }

    private fun maybeRecordResourceIds(
        browserPackage: String,
        root: AccessibilityNodeInfo
    ) {
        val nowElapsed =
            SystemClock.elapsedRealtime()

        if (
            nowElapsed -
                lastResourceIdProbeElapsedMs <
            RESOURCE_ID_PROBE_INTERVAL_MS
        ) {
            return
        }

        lastResourceIdProbeElapsedMs =
            nowElapsed

        val ids =
            BrowserAccessibilityExtractor
                .collectResourceIds(
                    root
                )

        BrowserWebPreferences(
            applicationContext
        ).recordTreeResourceIds(
            browserPackage =
                browserPackage,
            ids =
                ids,
            privateMode =
                privateModeLatched
        )
    }

    private fun acceptTreeHost(
        host: String,
        browserPackage: String,
        urlBarId: String?,
        source: String
    ) {
        candidateHost =
            host

        candidateHostCount =
            REQUIRED_CONSECUTIVE_HOST_READS

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
                urlBarId,
            privateMode =
                privateModeLatched,
            privateReason =
                privateModeReason,
            detectionSource =
                "TREE:$source"
        )

        updateObservation(
            observation
        )
    }

    private fun maybeRequestScreenshot(
        browserPackage: String,
        preferredWindowId: Int? =
            null
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

        val windowId =
            preferredWindowId
                ?.takeIf {
                    it >=
                        0
                }
                ?: lastBrowserWindowId
                ?: resolveBrowserWindowId(
                    browserPackage
                )

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            windowId !=
                null
        ) {
            requestWindowScreenshot(
                browserPackage,
                windowId
            )
        } else {
            requestDisplayScreenshot(
                browserPackage
            )
        }
    }

    private fun requestWindowScreenshot(
        browserPackage: String,
        windowId: Int
    ) {
        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        screenshotInFlight =
            true

        prefs.recordScreenshotRequested()
        prefs.recordWindowScreenshotRequested(
            windowId
        )

        takeScreenshotOfWindow(
            windowId,
            observerExecutor,
            object :
                AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(
                    screenshot:
                        AccessibilityService.ScreenshotResult
                ) {
                    prefs.recordScreenshotSuccess()
                    prefs.recordWindowScreenshotSuccess(
                        windowId
                    )

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
                    val secure =
                        errorCode ==
                            AccessibilityService.ERROR_TAKE_SCREENSHOT_SECURE_WINDOW

                    prefs.recordScreenshotFailure(
                        errorCode
                    )

                    prefs.recordWindowScreenshotFailure(
                        windowId =
                            windowId,
                        errorCode =
                            errorCode,
                        secureWindow =
                            secure
                    )

                    if (
                        errorCode ==
                        AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_WINDOW
                    ) {
                        lastBrowserWindowId =
                            null
                    }

                    // SECURE_WINDOW is evidence about this window only. It is
                    // deliberately NOT promoted to private/incognito=true.
                    screenshotInFlight =
                        false

                    // 0.1.23 proved display screenshots work on the Redmi. A
                    // per-window failure must therefore remain diagnostic and
                    // must not remove the already-proven visual fallback.
                    scheduleDisplayScreenshotFallback(
                        browserPackage
                    )
                }
            }
        )
    }

    private fun scheduleDisplayScreenshotFallback(
        browserPackage: String
    ) {
        if (
            !::observer.isInitialized
        ) {
            return
        }

        observer.postDelayed(
            {
                val power =
                    getSystemService(
                        POWER_SERVICE
                    ) as
                        PowerManager

                if (
                    !screenshotInFlight &&
                    power.isInteractive &&
                    usageForegroundPackage ==
                        browserPackage
                ) {
                    requestDisplayScreenshot(
                        browserPackage
                    )
                }
            },
            WINDOW_SCREENSHOT_FALLBACK_DELAY_MS
        )
    }

    private fun requestDisplayScreenshot(
        browserPackage: String
    ) {
        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

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
            // Unlike 0.1.23, the private-mode probe remains eligible after a
            // normal host has already been found. This fixes the previous
            // visual_private_probe_count=0 blind spot on normal -> incognito.
            val allowModeProbe =
                !privateModeLatched

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
                    result.privateReason,
                privateProbeAttempted =
                    result.privateProbeAttempted
            )

            if (
                result.privateModeDetected
            ) {
                latchPrivateMode(
                    browserPackage,
                    result.privateReason
                )
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
                privateModeReason,
            detectionSource =
                "VISUAL_OCR_FALLBACK"
        )

        updateObservation(
            observation
        )
    }

    private fun latchPrivateMode(
        browserPackage: String,
        reason: String
    ) {
        val changed =
            !privateModeLatched

        privateModeLatched =
            true

        privateModeReason =
            reason

        if (
            changed
        ) {
            val current =
                currentObservation

            if (
                current !=
                    null &&
                current.browserPackage ==
                    browserPackage &&
                !current.privateMode
            ) {
                BrowserWebPreferences(
                    applicationContext
                ).recordDetectorState(
                    browserPackage =
                        browserPackage,
                    urlBarId =
                        BrowserWebPreferences(
                            applicationContext
                        ).lastUrlBarId(),
                    privateMode =
                        true,
                    privateReason =
                        reason,
                    detectionSource =
                        "PRIVATE_MODE_PROMOTION"
                )

                updateObservation(
                    current.copy(
                        privateMode =
                            true
                    )
                )
            }
        }
    }

    private fun clearPrivateMode(
        browserPackage: String,
        reason: String
    ) {
        val changed =
            privateModeLatched

        privateModeLatched =
            false

        privateModeReason =
            reason

        if (
            changed
        ) {
            val current =
                currentObservation

            if (
                current !=
                    null &&
                current.browserPackage ==
                    browserPackage &&
                current.privateMode
            ) {
                val prefs =
                    BrowserWebPreferences(
                        applicationContext
                    )

                prefs.recordDetectorState(
                    browserPackage =
                        browserPackage,
                    urlBarId =
                        prefs.lastUrlBarId(),
                    privateMode =
                        false,
                    privateReason =
                        reason,
                    detectionSource =
                        "NORMAL_MODE_PROMOTION"
                )

                updateObservation(
                    current.copy(
                        privateMode =
                            false
                    )
                )
            }
        }
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

        var newestPrivateHintTs =
            Long.MIN_VALUE

        var newestPrivateHintPackage:
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

            val privateActivityHint =
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

            if (
                privateActivityHint &&
                event.timeStamp >=
                    newestPrivateHintTs
            ) {
                newestPrivateHintTs =
                    event.timeStamp

                newestPrivateHintPackage =
                    eventPackage
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

            // Best-effort automatic banking failsafe. It does not replace the
            // protected launcher because the financial process is already in
            // foreground at this point; it simply minimizes how long Guardian
            // Web remains enabled if the user opens a recognized bank directly.
            if (
                FinancialAppCatalog.isFinancial(
                    newestPackage
                )
            ) {
                disableForBankMode(
                    "AUTO_FINANCIAL_FOREGROUND"
                )

                usageCursorMs =
                    (
                        now -
                            1000L
                        )
                        .coerceAtLeast(
                            0L
                        )

                return
            }

            if (
                previous !=
                    newestPackage &&
                !BrowserCatalog.isSupported(
                    newestPackage
                )
            ) {
                stopAccruing()
                resetHybridSession()
            }
        }

        // Do not let an old incognito activity from the initial UsageStats
        // lookback contaminate a newer normal-browser foreground state. The
        // hint must be at least as recent as the newest ACTIVITY_RESUMED event
        // observed in this query (or there was no resume event in this slice).
        val privatePackage =
            newestPrivateHintPackage

        if (
            privatePackage !=
                null &&
            privatePackage ==
                usageForegroundPackage &&
            (
                newestTs ==
                    Long.MIN_VALUE ||
                    newestPrivateHintTs >=
                    newestTs
                )
        ) {
            latchPrivateMode(
                privatePackage,
                "USAGE_EVENT_INCOGNITO_ACTIVITY"
            )
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

    private fun resolveBrowserWindowRoot(
        browserPackage: String
    ): WindowRoot? {
        runCatching {
            windows
        }.getOrNull()
            .orEmpty()
            .asSequence()
            .filter {
                it.type ==
                    AccessibilityWindowInfo.TYPE_APPLICATION
            }
            .forEach {
                window ->
                val root =
                    runCatching {
                        window.root
                    }.getOrNull()

                if (
                    root !=
                        null &&
                    nodeMatchesBrowser(
                        root,
                        browserPackage
                    )
                ) {
                    return WindowRoot(
                        root =
                            root,
                        windowId =
                            window.id,
                        source =
                            "APPLICATION_WINDOW_TREE"
                    )
                }
            }

        val active =
            runCatching {
                rootInActiveWindow
            }.getOrNull()

        if (
            active !=
                null &&
            nodeMatchesBrowser(
                active,
                browserPackage
            )
        ) {
            return WindowRoot(
                root =
                    active,
                windowId =
                    lastBrowserWindowId,
                source =
                    "ACTIVE_ROOT_TREE"
            )
        }

        return null
    }

    private fun resolveBrowserWindowId(
        browserPackage: String
    ): Int? =
        resolveBrowserWindowRoot(
            browserPackage
        )?.windowId

    private fun nodeMatchesBrowser(
        node: AccessibilityNodeInfo,
        browserPackage: String
    ): Boolean {
        val nodePackage =
            runCatching {
                node.packageName
                    ?.toString()
            }.getOrNull()

        return nodePackage ==
            null ||
            nodePackage ==
                browserPackage
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
            resetHybridSession()
            return
        }

        // A missing/hidden toolbar never clears the last valid host while the
        // same browser remains foreground according to UsageStats.
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

    private fun disableForBankMode(
        reason: String
    ) {
        if (
            bankModeDisableRequested
        ) {
            return
        }

        bankModeDisableRequested =
            true

        val shutdown =
            Runnable {
                if (
                    ::observer.isInitialized
                ) {
                    observer.removeCallbacks(
                        heartbeat
                    )
                }

                stopAccruing()
                resetHybridSession()

                runCatching {
                    GuardianDatabase(
                        applicationContext
                    ).logTechnical(
                        "WEB_BANK_MODE_DISABLE",
                        reason
                    )
                }

                Handler(
                    Looper.getMainLooper()
                ).post {
                    disableSelf()
                }
            }

        if (
            ::observer.isInitialized
        ) {
            observer.post(
                shutdown
            )
        } else {
            shutdown.run()
        }
    }

    private fun resetForValidation() {
        val reset =
            Runnable {
                stopAccruing()
                resetHybridSession()

                lastScreenshotElapsedMs =
                    0L

                lastResourceIdProbeElapsedMs =
                    0L

                // bankCurrent() posts its final chunk to storage. Queue the
                // clear after it so pre-test data cannot reappear after reset.
                storage.post {
                    GuardianDatabase(
                        applicationContext
                    ).clearBrowserSessions()

                    BrowserWebPreferences(
                        applicationContext
                    ).apply {
                        clearRuntimeEvidence()
                        markTrackingStartedIfMissing()
                    }

                    GuardianDatabase(
                        applicationContext
                    ).logTechnical(
                        "WEB_TEST_RESET"
                    )
                }
            }

        if (
            ::observer.isInitialized
        ) {
            observer.post(
                reset
            )
        } else {
            reset.run()
        }
    }

    private fun resetHybridSession() {
        candidateHost =
            null

        candidateHostCount =
            0

        privateModeLatched =
            false

        privateModeReason =
            "NONE"

        lastBrowserWindowId =
            null
    }

    companion object {
        @Volatile
        private var activeInstance:
            BrowserAccessibilityService? =
            null

        fun requestBankModeDisable(): Boolean {
            val service =
                activeInstance
                    ?: return false

            service.disableForBankMode(
                "MANUAL_OR_PROTECTED_LAUNCH"
            )
            return true
        }

        fun requestValidationReset(): Boolean {
            val service =
                activeInstance
                    ?: return false

            service.resetForValidation()
            return true
        }

        private const val HEARTBEAT_MS =
            2500L

        private const val SCREENSHOT_MIN_INTERVAL_MS =
            5000L

        private const val WINDOW_SCREENSHOT_FALLBACK_DELAY_MS =
            1000L

        private const val RESOURCE_ID_PROBE_INTERVAL_MS =
            10_000L

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
