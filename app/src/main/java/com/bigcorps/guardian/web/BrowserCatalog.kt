package com.bigcorps.guardian.web

data class BrowserSpec(
    val packageName: String,
    val label: String,
    val family: String,
    val urlBarIds: List<String>
)

object BrowserCatalog {
    val supported: List<BrowserSpec> =
        listOf(
            chromium("com.android.chrome", "Chrome"),
            chromium("com.chrome.beta", "Chrome Beta"),
            chromium("com.chrome.dev", "Chrome Dev"),
            chromium("com.chrome.canary", "Chrome Canary"),
            chromium("org.chromium.chrome", "Chromium"),
            chromium("com.brave.browser", "Brave"),
            chromium("com.brave.browser_beta", "Brave Beta"),
            chromium("com.brave.browser_nightly", "Brave Nightly"),
            chromium("com.microsoft.emmx", "Microsoft Edge"),
            chromium("com.microsoft.emmx.beta", "Microsoft Edge Beta"),
            chromium("com.microsoft.emmx.dev", "Microsoft Edge Dev"),
            chromium("com.microsoft.emmx.canary", "Microsoft Edge Canary"),
            chromium("com.vivaldi.browser", "Vivaldi"),
            chromium("com.vivaldi.browser.snapshot", "Vivaldi Snapshot"),
            chromium("com.kiwibrowser.browser", "Kiwi Browser"),
            BrowserSpec(
                packageName = "com.opera.browser",
                label = "Opera",
                family = "chromium",
                urlBarIds = listOf(
                    "com.opera.browser:id/url_field",
                    "com.opera.browser:id/url_bar"
                )
            ),
            BrowserSpec(
                packageName = "com.opera.browser.beta",
                label = "Opera Beta",
                family = "chromium",
                urlBarIds = listOf(
                    "com.opera.browser.beta:id/url_bar",
                    "com.opera.browser.beta:id/url_field"
                )
            ),
            BrowserSpec(
                packageName = "com.opera.mini.native",
                label = "Opera Mini",
                family = "chromium",
                urlBarIds = listOf(
                    "com.opera.mini.native:id/url_field",
                    "com.opera.mini.native:id/url_bar"
                )
            ),
            BrowserSpec(
                packageName = "com.opera.mini.native.beta",
                label = "Opera Mini Beta",
                family = "chromium",
                urlBarIds = listOf(
                    "com.opera.mini.native.beta:id/url_field",
                    "com.opera.mini.native.beta:id/url_bar"
                )
            ),
            firefox("org.mozilla.firefox", "Firefox"),
            firefox("org.mozilla.firefox_beta", "Firefox Beta"),
            firefox("org.mozilla.fenix", "Firefox Nightly"),
            firefox("org.mozilla.focus", "Firefox Focus"),
            firefox("org.mozilla.klar", "Firefox Klar"),
            BrowserSpec(
                packageName = "com.sec.android.app.sbrowser",
                label = "Samsung Internet",
                family = "samsung",
                urlBarIds = listOf(
                    "com.sec.android.app.sbrowser:id/location_bar_edit_text"
                )
            ),
            BrowserSpec(
                packageName = "com.sec.android.app.sbrowser.beta",
                label = "Samsung Internet Beta",
                family = "samsung",
                urlBarIds = listOf(
                    "com.sec.android.app.sbrowser.beta:id/location_bar_edit_text"
                )
            ),
            BrowserSpec(
                packageName = "com.duckduckgo.mobile.android",
                label = "DuckDuckGo",
                family = "duckduckgo",
                urlBarIds = listOf(
                    "com.duckduckgo.mobile.android:id/omnibarTextInput",
                    "com.duckduckgo.mobile.android:id/addressBar"
                )
            ),
            BrowserSpec(
                packageName = "com.android.browser",
                label = "Mi Browser / Android Browser",
                family = "unknown_chromium",
                // Intentionally no guessed URL-bar resource ID. The Hybrid v3
                // diagnostic exports resource IDs only, so Xiaomi can be
                // calibrated from real-device evidence before pinning an ID.
                urlBarIds = emptyList()
            )
        )

    private val byPackage =
        supported.associateBy {
            it.packageName
        }

    fun spec(
        packageName: String?
    ): BrowserSpec? =
        packageName
            ?.let {
                byPackage[it]
            }

    fun isSupported(
        packageName: String?
    ): Boolean =
        spec(packageName) !=
            null

    fun supportedPackages(): List<String> =
        supported.map {
            it.packageName
        }

    private fun chromium(
        packageName: String,
        label: String
    ): BrowserSpec =
        BrowserSpec(
            packageName = packageName,
            label = label,
            family = "chromium",
            urlBarIds = listOf(
                "$packageName:id/url_bar",
                "com.android.chrome:id/url_bar"
            ).distinct()
        )

    private fun firefox(
        packageName: String,
        label: String
    ): BrowserSpec =
        BrowserSpec(
            packageName = packageName,
            label = label,
            family = "firefox",
            urlBarIds = listOf(
                "$packageName:id/mozac_browser_toolbar_url_view",
                "$packageName:id/mozac_browser_toolbar_edit_url_view",
                "$packageName:id/toolbar_url_view",
                "$packageName:id/url_bar"
            )
        )
}
