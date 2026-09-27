package com.bigcorps.guardian.web

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.bigcorps.guardian.core.GuardianDatabase

class BrowserAccessibilityService : AccessibilityService() {
    private data class Observation(
        val host: String,
        val browserPackage: String,
        val privateMode: Boolean
    )

    private lateinit var workerThread: HandlerThread
    private lateinit var worker: Handler

    private var lastObservation: Observation? = null
    private var lastWallMs = 0L
    private var lastElapsedMs = 0L

    private val sampleRunnable =
        object : Runnable {
            override fun run() {
                sampleSafely()
                if (::worker.isInitialized) {
                    worker.postDelayed(this, SAMPLE_INTERVAL_MS)
                }
            }
        }

    private val eventRunnable = Runnable { sampleSafely() }

    override fun onCreate() {
        super.onCreate()
        workerThread = HandlerThread("GuardianWebObserver").apply { start() }
        worker = Handler(workerThread.looper)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val prefs = BrowserWebPreferences(applicationContext)
        prefs.markTrackingStartedIfMissing()
        prefs.recordServiceConnected()

        runCatching {
            GuardianDatabase(applicationContext)
                .logTechnical("WEB_SERVICE_CONNECTED")
        }

        worker.removeCallbacks(sampleRunnable)
        worker.post(sampleRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val prefs = BrowserWebPreferences(applicationContext)
        if (!prefs.consented()) return

        val pkg =
            event?.packageName?.toString()
                ?.takeIf { BrowserCatalog.isSupported(it) }

        prefs.recordAccessibilityEvent(pkg, event?.eventType ?: 0)

        if (!::worker.isInitialized) return
        worker.removeCallbacks(eventRunnable)
        worker.postDelayed(eventRunnable, EVENT_DEBOUNCE_MS)
    }

    override fun onInterrupt() {
        if (::worker.isInitialized) worker.post { sampleAsInactive() }
    }

    override fun onDestroy() {
        if (::worker.isInitialized) {
            worker.removeCallbacks(sampleRunnable)
            worker.removeCallbacks(eventRunnable)
            worker.post { sampleAsInactive() }
        }

        runCatching {
            GuardianDatabase(applicationContext)
                .logTechnical("WEB_SERVICE_DISCONNECTED")
        }

        if (::workerThread.isInitialized) workerThread.quitSafely()
        super.onDestroy()
    }

    private fun sampleSafely() {
        val prefs = BrowserWebPreferences(applicationContext)
        prefs.recordHeartbeat()

        try {
            sampleNow(prefs)
        } catch (t: Throwable) {
            prefs.recordSampleError(t::class.java.simpleName)
            runCatching {
                GuardianDatabase(applicationContext)
                    .logTechnical("WEB_SAMPLE_ERROR", t::class.java.simpleName)
            }
            sampleAsInactive()
        }
    }

    private fun sampleNow(prefs: BrowserWebPreferences) {
        if (!prefs.consented()) {
            prefs.recordSample(null, "NO_CONSENT", false)
            sampleAsInactive()
            return
        }

        val power = getSystemService(POWER_SERVICE) as PowerManager
        if (!power.isInteractive) {
            prefs.recordSample(null, "SCREEN_OFF", false)
            sampleAsInactive()
            return
        }

        val root = rootInActiveWindow
        if (root == null) {
            prefs.recordSample(null, "ROOT_NULL", false)
            sampleAsInactive()
            return
        }

        val packageName = root.packageName?.toString()
        val spec = BrowserCatalog.spec(packageName)

        if (spec == null || packageName == null) {
            prefs.recordSample(null, "UNSUPPORTED_WINDOW", false)
            sampleAsInactive()
            return
        }

        val extraction =
            BrowserAccessibilityExtractor.extractHost(root, spec)

        prefs.recordSample(
            packageName,
            extraction.state,
            extraction.host != null
        )

        if (extraction.state == "FOCUSED" || extraction.state == "INVALID") {
            updateObservation(null)
            return
        }

        val host = extraction.host
        if (host == null) {
            val previous = lastObservation
            if (
                previous != null &&
                previous.browserPackage == packageName &&
                extraction.state == "MISSING"
            ) {
                updateObservation(previous)
            } else {
                updateObservation(null)
            }
            return
        }

        val mode =
            BrowserAccessibilityExtractor.detectPrivateMode(root, spec)

        prefs.recordDetectorState(
            packageName,
            extraction.urlBarId,
            mode.privateMode,
            mode.reason
        )

        updateObservation(
            Observation(host, packageName, mode.privateMode)
        )
    }

    private fun sampleAsInactive() = updateObservation(null)

    private fun updateObservation(current: Observation?) {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val previous = lastObservation

        if (
            previous != null &&
            lastWallMs > 0L &&
            lastElapsedMs > 0L
        ) {
            val elapsed = nowElapsed - lastElapsedMs
            if (
                elapsed in 1L..MAX_BANK_GAP_MS &&
                nowWall > lastWallMs
            ) {
                runCatching {
                    GuardianDatabase(applicationContext).recordBrowserChunk(
                        startMs = lastWallMs,
                        endMs = nowWall,
                        host = previous.host,
                        browserPackage = previous.browserPackage,
                        privateMode = previous.privateMode
                    )
                }.onFailure {
                    BrowserWebPreferences(applicationContext)
                        .recordSampleError(it::class.java.simpleName)
                }
            }
        }

        lastObservation = current
        lastWallMs = nowWall
        lastElapsedMs = nowElapsed
    }

    companion object {
        private const val SAMPLE_INTERVAL_MS = 5_000L
        private const val EVENT_DEBOUNCE_MS = 500L
        private const val MAX_BANK_GAP_MS = 15_000L
    }
}
