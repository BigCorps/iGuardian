package com.bigcorps.guardian.web

import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque
import java.util.Locale

data class BrowserUrlExtraction(
    val host: String?,
    val urlBarId: String?,
    val state: String
)

data class BrowserModeExtraction(
    val privateMode: Boolean,
    val normalModeDetected: Boolean,
    val reason: String
)

object BrowserAccessibilityExtractor {
    private const val MAX_TREE_NODES =
        180

    private const val MAX_RESOURCE_IDS =
        96

    /**
     * Hot-path extraction. Known address-bar IDs are queried first. The URL bar
     * does NOT need to be visible: Chromium and other browsers may keep the node
     * in the accessibility tree while the toolbar is collapsed by scrolling.
     *
     * The focused guard remains intentional: while the user is typing, partial
     * input must never be accepted as browsing history.
     */
    fun extractHost(
        root: AccessibilityNodeInfo,
        spec: BrowserSpec,
        allowFallback: Boolean = true
    ): BrowserUrlExtraction {
        spec.urlBarIds.forEach { id ->
            val nodes =
                runCatching {
                    root.findAccessibilityNodeInfosByViewId(
                        id
                    )
                }.getOrNull()
                    .orEmpty()

            val node =
                nodes.firstOrNull {
                    !it.isFocused
                }
                    ?: nodes.firstOrNull()

            if (
                node !=
                null
            ) {
                if (
                    node.isFocused
                ) {
                    return BrowserUrlExtraction(
                        null,
                        id,
                        "FOCUSED"
                    )
                }

                val host =
                    BrowserDomainSanitizer.hostFromRaw(
                        node.text
                            ?: node.contentDescription
                    )

                return BrowserUrlExtraction(
                    host,
                    id,
                    if (
                        host ==
                        null
                    ) {
                        "INVALID"
                    } else {
                        "FOUND"
                    }
                )
            }
        }

        if (
            !allowFallback
        ) {
            return BrowserUrlExtraction(
                null,
                null,
                "MISSING_DIRECT"
            )
        }

        val queue =
            ArrayDeque<AccessibilityNodeInfo>()

        queue.add(
            root
        )

        var visited =
            0

        while (
            queue.isNotEmpty() &&
            visited <
                MAX_TREE_NODES
        ) {
            val node =
                queue.removeFirst()

            visited +=
                1

            val id =
                node.viewIdResourceName
                    ?.lowercase(
                        Locale.ROOT
                    )

            val looksLikeAddressField =
                id !=
                    null &&
                    (
                        id.contains(
                            "url"
                        ) ||
                            id.contains(
                                "address"
                            ) ||
                            id.contains(
                                "location_bar"
                            ) ||
                            id.contains(
                                "omnibar"
                            )
                        )

            if (
                looksLikeAddressField
            ) {
                if (
                    node.isFocused
                ) {
                    return BrowserUrlExtraction(
                        null,
                        node.viewIdResourceName,
                        "FOCUSED"
                    )
                }

                val host =
                    BrowserDomainSanitizer.hostFromRaw(
                        node.text
                            ?: node.contentDescription
                    )

                if (
                    host !=
                    null
                ) {
                    return BrowserUrlExtraction(
                        host,
                        node.viewIdResourceName,
                        "FOUND_FALLBACK"
                    )
                }
            }

            for (
                index in
                0 until node.childCount
            ) {
                node.getChild(
                    index
                )
                    ?.let(
                        queue::addLast
                    )
            }
        }

        return BrowserUrlExtraction(
            null,
            null,
            "MISSING"
        )
    }

    /** Cheap private-mode probe for the Accessibility callback hot path. */
    fun detectPrivateModeDirect(
        root: AccessibilityNodeInfo,
        spec: BrowserSpec
    ): BrowserModeExtraction {
        if (
            spec.family !=
            "chromium"
        ) {
            return BrowserModeExtraction(
                privateMode = false,
                normalModeDetected = false,
                reason = "NO_DIRECT_PRIVATE_MARKER"
            )
        }

        val directIds =
            listOf(
                "${spec.packageName}:id/location_bar_incognito_badge",
                "${spec.packageName}:id/incognito_indicator",
                "com.android.chrome:id/location_bar_incognito_badge",
                "com.android.chrome:id/incognito_indicator"
            ).distinct()

        directIds.forEach { id ->
            val visible =
                runCatching {
                    root.findAccessibilityNodeInfosByViewId(
                        id
                    )
                }.getOrNull()
                    .orEmpty()
                    .any {
                        it.isVisibleToUser
                    }

            if (
                visible
            ) {
                return BrowserModeExtraction(
                    privateMode = true,
                    normalModeDetected = false,
                    reason = "CHROMIUM_VISIBLE_INCOGNITO_BADGE"
                )
            }
        }

        return BrowserModeExtraction(
            privateMode = false,
            normalModeDetected = false,
            reason = "NO_DIRECT_PRIVATE_MARKER"
        )
    }

    /**
     * Full bounded private-mode heuristic. This is intentionally kept off the
     * Accessibility callback hot path by BrowserAccessibilityService.
     */
    fun detectPrivateMode(
        root: AccessibilityNodeInfo,
        spec: BrowserSpec
    ): BrowserModeExtraction {
        val direct =
            detectPrivateModeDirect(
                root,
                spec
            )

        if (
            direct.privateMode
        ) {
            return direct
        }

        val queue =
            ArrayDeque<AccessibilityNodeInfo>()

        queue.add(
            root
        )

        val markers =
            mutableListOf<BrowserAccessibilityMarker>()

        var visited =
            0

        while (
            queue.isNotEmpty() &&
            visited <
                MAX_TREE_NODES
        ) {
            val node =
                queue.removeFirst()

            visited +=
                1

            val resourceId =
                node.viewIdResourceName

            if (
                resourceId !=
                    null &&
                resourceId.contains(
                    ":id/"
                )
            ) {
                markers +=
                    BrowserAccessibilityMarker(
                        resourceId =
                            resourceId,
                        text =
                            node.text
                                ?.toString(),
                        contentDescription =
                            node.contentDescription
                                ?.toString()
                    )
            }

            for (
                index in
                0 until node.childCount
            ) {
                node.getChild(
                    index
                )
                    ?.let(
                        queue::addLast
                    )
            }
        }

        val result =
            BrowserPrivateModeHeuristics.detect(
                spec,
                markers
            )

        if (
            result.isPrivate
        ) {
            return BrowserModeExtraction(
                privateMode = true,
                normalModeDetected = false,
                reason = result.reason
            )
        }

        val normalModeDetected =
            spec.family ==
                "chromium" &&
                markers.any { marker ->
                    listOfNotNull(
                        marker.contentDescription,
                        marker.text
                    ).any { value ->
                        isStrongChromiumNormalModeText(
                            value
                        )
                    }
                }

        return BrowserModeExtraction(
            privateMode = false,
            normalModeDetected = normalModeDetected,
            reason =
                if (
                    normalModeDetected
                ) {
                    "CHROMIUM_ENTER_INCOGNITO_TOGGLE"
                } else {
                    result.reason
                }
        )
    }

    private fun isStrongChromiumNormalModeText(
        raw: String
    ): Boolean {
        val value =
            raw.trim()
                .lowercase(
                    Locale.ROOT
                )

        return value ==
            "enter incognito mode" ||
            value ==
                "entrar no modo de navegação anônima" ||
            value ==
                "entrar no modo de navegacao anonima" ||
            value ==
                "enter private mode"
    }

    /**
     * Diagnostic-only bounded tree inventory. It returns resource IDs only;
     * never node text, URLs, content descriptions, titles or page content.
     */
    fun collectResourceIds(
        root: AccessibilityNodeInfo
    ): List<String> {
        val queue =
            ArrayDeque<AccessibilityNodeInfo>()

        val ids =
            linkedSetOf<String>()

        queue.add(
            root
        )

        var visited =
            0

        while (
            queue.isNotEmpty() &&
            visited <
                MAX_TREE_NODES &&
            ids.size <
                MAX_RESOURCE_IDS
        ) {
            val node =
                queue.removeFirst()

            visited +=
                1

            node.viewIdResourceName
                ?.takeIf {
                    it.contains(
                        ":id/"
                    )
                }
                ?.take(
                    180
                )
                ?.let(
                    ids::add
                )

            for (
                index in
                0 until node.childCount
            ) {
                node.getChild(
                    index
                )
                    ?.let(
                        queue::addLast
                    )
            }
        }

        return ids
            .sorted()
    }
}
