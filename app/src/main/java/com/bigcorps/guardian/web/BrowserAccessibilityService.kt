package com.bigcorps.guardian.web

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bigcorps.guardian.core.GuardianDatabase

class BrowserAccessibilityService : AccessibilityService() {
    private data class Observation(
        val host: String,
        val browserPackage: String,
        val privateMode: Boolean
    )

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private lateinit var storageThread:
        HandlerThread

    private lateinit var storage:
        Handler

    private var currentObservation:
        Observation? =
        null

    private var foregroundPackage:
        String? =
        null

    private var lastBankWallMs =
        0L

    private var lastBankElapsedMs =
        0L

    private var lastExtractElapsedMs =
        0L

    private var lastFallbackElapsedMs =
        0L

    private var lastPrivateScanElapsedMs =
        0L

    private var cachedPrivateMode =
        false

    private var cachedPrivatePackage:
        String? =
        null

    private val ticker =
        object :
            Runnable {
            override fun run() {
                runCatching {
                    tick(
                        verifyForeground =
                            true
                    )
                }.onFailure {
                    BrowserWebPreferences(
                        applicationContext
                    ).recordSampleError(
                        it::class.java.simpleName
                    )
                }

                mainHandler.postDelayed(
                    this,
                    TICK_MS
                )
            }
        }

    override fun onCreate() {
        super.onCreate()

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

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "WEB_SERVICE_CONNECTED"
            )
        }

        mainHandler.removeCallbacks(
            ticker
        )

        mainHandler.post(
            ticker
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

        prefs.recordAccessibilityEvent(
            browserPackage =
                packageName,
            eventType =
                event.eventType
        )

        val spec =
            BrowserCatalog.spec(
                packageName
            )
                ?: return

        foregroundPackage =
            spec.packageName

        val now =
            SystemClock.elapsedRealtime()

        if (
            event.eventType ==
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            now -
                lastExtractElapsedMs <
                EXTRACT_THROTTLE_MS
        ) {
            return
        }

        lastExtractElapsedMs =
            now

        runCatching {
            inspectActiveBrowser(
                spec,
                allowFallback =
                    now -
                        lastFallbackElapsedMs >=
                        FALLBACK_SCAN_INTERVAL_MS,
                allowPrivateScan =
                    now -
                        lastPrivateScanElapsedMs >=
                        PRIVATE_SCAN_INTERVAL_MS
            )
        }.onFailure {
            prefs.recordSampleError(
                it::class.java.simpleName
            )
        }

        tick(
            verifyForeground =
                false
        )
    }

    override fun onInterrupt() {
        stopAccruing()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(
            ticker
        )

        stopAccruing()

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "WEB_SERVICE_DISCONNECTED"
            )
        }

        if (
            ::storageThread.isInitialized
        ) {
            storageThread.quitSafely()
        }

        super.onDestroy()
    }

    private fun tick(
        verifyForeground: Boolean
    ) {
        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        prefs.recordHeartbeat()

        val power =
            getSystemService(
                POWER_SERVICE
            ) as
                PowerManager

        if (
            !power.isInteractive
        ) {
            prefs.recordSample(
                null,
                "SCREEN_OFF",
                false
            )
            stopAccruing()
            return
        }

        if (
            verifyForeground
        ) {
            val active =
                activeRoot()

            if (
                active ==
                null
            ) {
                prefs.recordSample(
                    null,
                    "ROOT_NULL_MAIN",
                    false
                )
                stopAccruing()
                return
            }

            try {
                foregroundPackage =
                    active.packageName
                        ?.toString()

                val spec =
                    BrowserCatalog.spec(
                        foregroundPackage
                    )

                if (
                    spec ==
                    null
                ) {
                    prefs.recordSample(
                        null,
                        "NON_BROWSER_FOREGROUND",
                        false
                    )
                    stopAccruing()
                    return
                }

                val now =
                    SystemClock.elapsedRealtime()

                inspectRoot(
                    root =
                        active,
                    spec =
                        spec,
                    allowFallback =
                        now -
                            lastFallbackElapsedMs >=
                            FALLBACK_SCAN_INTERVAL_MS,
                    allowPrivateScan =
                        now -
                            lastPrivateScanElapsedMs >=
                            PRIVATE_SCAN_INTERVAL_MS
                )
            } finally {
                recycleQuietly(
                    active
                )
            }
        }

        val observation =
            currentObservation

        if (
            observation ==
            null ||
            foregroundPackage !=
                observation.browserPackage
        ) {
            stopAccruing()
            return
        }

        bankCurrent(
            observation
        )
    }

    private fun inspectActiveBrowser(
        spec: BrowserSpec,
        allowFallback: Boolean,
        allowPrivateScan: Boolean
    ) {
        val root =
            activeRoot()

        if (
            root ==
            null
        ) {
            BrowserWebPreferences(
                applicationContext
            ).recordSample(
                spec.packageName,
                "ROOT_NULL_EVENT",
                false
            )
            return
        }

        try {
            if (
                root.packageName
                    ?.toString() !=
                spec.packageName
            ) {
                BrowserWebPreferences(
                    applicationContext
                ).recordSample(
                    spec.packageName,
                    "ROOT_PACKAGE_MISMATCH",
                    false
                )
                return
            }

            inspectRoot(
                root,
                spec,
                allowFallback,
                allowPrivateScan
            )
        } finally {
            recycleQuietly(
                root
            )
        }
    }

    private fun inspectRoot(
        root: AccessibilityNodeInfo,
        spec: BrowserSpec,
        allowFallback: Boolean,
        allowPrivateScan: Boolean
    ) {
        val prefs =
            BrowserWebPreferences(
                applicationContext
            )

        val extraction =
            BrowserAccessibilityExtractor.extractHost(
                root,
                spec,
                allowFallback
            )

        if (
            extraction.state ==
            "FOUND_FALLBACK" ||
            (
                allowFallback &&
                extraction.state ==
                    "MISSING"
                )
        ) {
            lastFallbackElapsedMs =
                SystemClock.elapsedRealtime()
        }

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
            return
        }

        val host =
            extraction.host

        if (
            host ==
            null
        ) {
            if (
                currentObservation
                    ?.browserPackage ==
                spec.packageName
            ) {
                return
            }

            stopAccruing()
            return
        }

        val privateMode =
            if (
                allowPrivateScan ||
                cachedPrivatePackage !=
                    spec.packageName
            ) {
                val mode =
                    BrowserAccessibilityExtractor.detectPrivateMode(
                        root,
                        spec
                    )

                lastPrivateScanElapsedMs =
                    SystemClock.elapsedRealtime()

                cachedPrivatePackage =
                    spec.packageName

                cachedPrivateMode =
                    mode.privateMode

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

                mode.privateMode
            } else {
                cachedPrivateMode
            }

        val next =
            Observation(
                host =
                    host,
                browserPackage =
                    spec.packageName,
                privateMode =
                    privateMode
            )

        if (
            next !=
            currentObservation
        ) {
            currentObservation
                ?.let {
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
                0L
        ) {
            return
        }

        if (
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

        foregroundPackage =
            null

        lastBankWallMs =
            0L

        lastBankElapsedMs =
            0L
    }

    private fun activeRoot():
        AccessibilityNodeInfo? =
        runCatching {
            rootInActiveWindow
        }.getOrNull()

    @Suppress(
        "DEPRECATION"
    )
    private fun recycleQuietly(
        node: AccessibilityNodeInfo?
    ) {
        if (
            node ==
            null ||
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        runCatching {
            node.recycle()
        }
    }

    companion object {
        private const val TICK_MS =
            5_000L

        private const val MAX_BANK_GAP_MS =
            TICK_MS *
                3L

        private const val EXTRACT_THROTTLE_MS =
            700L

        private const val FALLBACK_SCAN_INTERVAL_MS =
            3_000L

        private const val PRIVATE_SCAN_INTERVAL_MS =
            3_000L
    }
}
