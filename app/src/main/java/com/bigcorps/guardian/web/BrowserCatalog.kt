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
            chromium("com.chrome.dev", "Chrome Dev"),
            chromium("com.brave.browser", "Brave"),
            chromium("com.microsoft.emmx", "Microsoft Edge"),
            chromium("com.vivaldi.browser", "Vivaldi"),
            chromium("com.opera.browser", "Opera"),
            BrowserSpec(
                packageName = "org.mozilla.firefox",
                label = "Firefox",
                family = "firefox",
                urlBarIds = listOf(
                    "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
                    "org.mozilla.firefox:id/mozac_browser_toolbar_edit_url_view",
                    "org.mozilla.firefox:id/toolbar_url_view"
                )
            ),
            BrowserSpec(
                packageName = "com.sec.android.app.sbrowser",
                label = "Samsung Internet",
                family = "samsung",
                urlBarIds = listOf(
                    "com.sec.android.app.sbrowser:id/location_bar_edit_text"
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
            )
        )
}
