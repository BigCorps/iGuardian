package com.bigcorps.guardian.web

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bigcorps.guardian.core.GuardianDatabase
import java.util.ArrayDeque

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

    private val eventLock =
        Any()

    private val pendingEvents =
        ArrayDeque<AccessibilityEvent>()

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

    private var lastFallbackScanElapsedMs =
        0L

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
                    inspectForegroundInteractiveWindow()
                    maintainCurrentObservation()
                }.onFailure {
                    prefs.recordSampleError(
                        it::class.java.simpleName
                    )
                }

                if (::observer.isInitialized) {
                    observer.postDelayed(
                        this,
                        HEARTBEAT_MS
                    )
                }
            }
        }

    private val processEvents =
        Runnable {
            processPendingEvents()
        }

    override fun onCreate() {
        super.onCreate()

        observerThread =
            HandlerThread(
                "GuardianWebObserverV4"
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
        if (event == null) {
            return
        }

        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        if (!prefs.consented()) {
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

        usageForegroundPackage =
            packageName

        /*
         * Android owns the callback event. Copy it before the callback returns,
         * then perform tree queries on GuardianWebObserverV4 instead of the UI
         * thread. Keeping several recent events avoids losing the one whose
         * source is attached to Chrome's toolbar.
         */
        @Suppress("DEPRECATION")
        val copy =
            AccessibilityEvent.obtain(
                event
            )

        synchronized(
            eventLock
        ) {
            pendingEvents.addLast(
                copy
            )

            while (
                pendingEvents.size >
                MAX_PENDING_EVENTS
            ) {
                @Suppress("DEPRECATION")
                pendingEvents.removeFirst()
                    .recycle()
            }
        }

        if (
            !::observer.isInitialized
        ) {
            return
        }

        observer.removeCallbacks(
            processEvents
        )

        observer.postDelayed(
            processEvents,
            EVENT_DEBOUNCE_MS
        )
    }

    override fun onInterrupt() {
        if (::observer.isInitialized) {
            observer.post {
                stopAccruing()
            }
        }
    }

    override fun onDestroy() {
        if (::observer.isInitialized) {
            observer.removeCallbacks(
                heartbeat
            )
            observer.removeCallbacks(
                processEvents
            )
            observer.post {
                stopAccruing()
            }
        }

        synchronized(
            eventLock
        ) {
            while (
                pendingEvents.isNotEmpty()
            ) {
                @Suppress("DEPRECATION")
                pendingEvents.removeFirst()
                    .recycle()
            }
        }

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "WEB_SERVICE_DISCONNECTED"
            )
        }

        if (::observerThread.isInitialized) {
            observerThread.quitSafely()
        }

        if (::storageThread.isInitialized) {
            storageThread.quitSafely()
        }

        super.onDestroy()
    }

    private fun processPendingEvents() {
        val events =
            synchronized(
                eventLock
            ) {
                buildList {
                    while (
                        pendingEvents.isNotEmpty()
                    ) {
                        add(
                            pendingEvents.removeLast()
                        )
                    }
                }
            }

        var found =
            false

        try {
            for (event in events) {
                if (
                    inspectEvent(
                        event
                    )
                ) {
                    found =
                        true
                    break
                }
            }

            if (!found) {
                inspectForegroundInteractiveWindow()
            }
        } finally {
            events.forEach {
                @Suppress("DEPRECATION")
                it.recycle()
            }
        }
    }

    private fun inspectEvent(
        event: AccessibilityEvent
    ): Boolean {
        val packageName =
            event.packageName
                ?.toString()
                ?: return false

        val spec =
            BrowserCatalog.spec(
                packageName
            )
                ?: return false

        val source =
            runCatching {
                event.source
            }.getOrNull()
                ?: run {
                    BrowserWebPreferences(
                        applicationContext
                    ).recordSample(
                        packageName,
                        "EVENT_SOURCE_NULL",
                        false
                    )
                    return false
                }

        val root =
            resolveRoot(
                source,
                packageName
            )
                ?: run {
                    BrowserWebPreferences(
                        applicationContext
                    ).recordSample(
                        packageName,
                        "EVENT_ROOT_NULL",
                        false
                    )
                    return false
                }

        return inspectRoot(
            root,
            spec,
            "EVENT"
        )
    }

    private fun inspectForegroundInteractiveWindow():
        Boolean {
        val packageName =
            usageForegroundPackage
                ?.takeIf {
                    BrowserCatalog.isSupported(
                        it
                    )
                }
                ?: return false

        val spec =
            BrowserCatalog.spec(
                packageName
            )
                ?: return false

        val now =
            SystemClock.elapsedRealtime()

        if (
            now -
                lastFallbackScanElapsedMs <
            WINDOW_SCAN_MIN_INTERVAL_MS
        ) {
            return false
        }

        lastFallbackScanElapsedMs =
            now

        val root =
            interactiveRootFor(
                packageName
            )
                ?: return false

        return inspectRoot(
            root,
            spec,
            "WINDOW"
        )
    }

    private fun inspectRoot(
        root: AccessibilityNodeInfo,
        spec: BrowserSpec,
        source: String
    ): Boolean {
        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        val extraction =
            BrowserAccessibilityExtractor.extractHost(
                root =
                    root,
                spec =
                    spec,
                allowFallback =
                    true
            )

        prefs.recordSample(
            browserPackage =
                spec.packageName,
            state =
                extraction.state,
            hostFound =
                extraction.host !=
                    null
        )

        if (
            extraction.state ==
            "FOCUSED" ||
            extraction.state ==
                "INVALID"
        ) {
            stopAccruing()
            return false
        }

        val host =
            extraction.host
                ?: return false

        val mode =
            BrowserAccessibilityExtractor.detectPrivateMode(
                root,
                spec
            )

        prefs.recordDetectorState(
            browserPackage =
                spec.packageName,
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
                    spec.packageName,
                privateMode =
                    mode.privateMode
            )
        )

        return true
    }

    private fun resolveRoot(
        source: AccessibilityNodeInfo,
        packageName: String
    ): AccessibilityNodeInfo? {
        val windowRoot =
            runCatching {
                source.window
                    ?.root
            }.getOrNull()

        if (
            windowRoot
                ?.packageName
                ?.toString() ==
            packageName
        ) {
            return windowRoot
        }

        var current:
            AccessibilityNodeInfo =
            source

        repeat(
            MAX_PARENT_HOPS
        ) {
            val parent =
                runCatching {
                    current.parent
                }.getOrNull()
                    ?: return current

            current =
                parent
        }

        return current
    }

    private fun interactiveRootFor(
        packageName: String
    ): AccessibilityNodeInfo? =
        runCatching {
            windows
                .asSequence()
                .mapNotNull {
                    it.root
                }
                .firstOrNull {
                    it.packageName
                        ?.toString() ==
                        packageName
                }
        }.getOrNull()

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

            if (
                event.eventType ==
                UsageEvents.Event.ACTIVITY_RESUMED &&
                event.timeStamp >=
                newestTs
            ) {
                newestTs =
                    event.timeStamp

                newestPackage =
                    event.packageName
            }
        }

        if (
            newestPackage !=
            null
        ) {
            usageForegroundPackage =
                newestPackage
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

    private fun maintainCurrentObservation() {
        val observation =
            currentObservation
                ?: return

        if (
            usageForegroundPackage !=
            observation.browserPackage
        ) {
            stopAccruing()
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
                ).recordSampleError(
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

    companion object {
        private const val HEARTBEAT_MS =
            5_000L

        private const val EVENT_DEBOUNCE_MS =
            200L

        private const val WINDOW_SCAN_MIN_INTERVAL_MS =
            2_500L

        private const val MAX_PENDING_EVENTS =
            6

        private const val MAX_PARENT_HOPS =
            24

        private const val MAX_BANK_GAP_MS =
            15_000L

        private const val INITIAL_USAGE_LOOKBACK_MS =
            5L *
                60L *
                1000L
    }
}
