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
    val reason: String
)

object BrowserAccessibilityExtractor {
    private const val MAX_TREE_NODES =
        160

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

            nodes.firstOrNull {
                it.isVisibleToUser
            }?.let { node ->
                if (node.isFocused) {
                    return BrowserUrlExtraction(
                        null,
                        id,
                        "FOCUSED"
                    )
                }

                val host =
                    BrowserDomainSanitizer.hostFromRaw(
                        node.text
                    )

                return BrowserUrlExtraction(
                    host,
                    id,
                    if (host == null) {
                        "INVALID"
                    } else {
                        "FOUND"
                    }
                )
            }
        }

        if (!allowFallback) {
            return BrowserUrlExtraction(
                null,
                null,
                "MISSING_THROTTLED"
            )
        }

        val queue =
            ArrayDeque<AccessibilityNodeInfo>()

        queue.add(root)

        var visited =
            0

        while (
            queue.isNotEmpty() &&
            visited < MAX_TREE_NODES
        ) {
            val node =
                queue.removeFirst()

            visited += 1

            val id =
                node.viewIdResourceName
                    ?.lowercase(
                        Locale.ROOT
                    )

            val looksLikeAddressField =
                id != null &&
                    (
                        id.contains("url") ||
                            id.contains("address") ||
                            id.contains("location_bar") ||
                            id.contains("omnibar")
                        ) &&
                    node.className
                        ?.toString()
                        ?.contains(
                            "EditText"
                        ) == true

            if (
                looksLikeAddressField &&
                node.isVisibleToUser
            ) {
                if (node.isFocused) {
                    return BrowserUrlExtraction(
                        null,
                        node.viewIdResourceName,
                        "FOCUSED"
                    )
                }

                val host =
                    BrowserDomainSanitizer.hostFromRaw(
                        node.text
                    )

                if (host != null) {
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
                node.getChild(index)
                    ?.let(queue::addLast)
            }
        }

        return BrowserUrlExtraction(
            null,
            null,
            "MISSING"
        )
    }

    fun detectPrivateMode(
        root: AccessibilityNodeInfo,
        spec: BrowserSpec
    ): BrowserModeExtraction {
        if (spec.family == "chromium") {
            val directIds =
                listOf(
                    "${spec.packageName}:id/location_bar_incognito_badge",
                    "${spec.packageName}:id/incognito_indicator",
                    "com.android.chrome:id/location_bar_incognito_badge",
                    "com.android.chrome:id/incognito_indicator"
                )

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

                if (visible) {
                    return BrowserModeExtraction(
                        true,
                        "CHROMIUM_VISIBLE_INCOGNITO_BADGE"
                    )
                }
            }
        }

        val queue =
            ArrayDeque<AccessibilityNodeInfo>()

        queue.add(root)

        val markers =
            mutableListOf<BrowserAccessibilityMarker>()

        var visited =
            0

        while (
            queue.isNotEmpty() &&
            visited < MAX_TREE_NODES
        ) {
            val node =
                queue.removeFirst()

            visited += 1

            val resourceId =
                node.viewIdResourceName

            if (
                resourceId != null &&
                resourceId.contains(":id/")
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
                node.getChild(index)
                    ?.let(queue::addLast)
            }
        }

        val result =
            BrowserPrivateModeHeuristics.detect(
                spec,
                markers
            )

        return BrowserModeExtraction(
            result.isPrivate,
            result.reason
        )
    }
}
