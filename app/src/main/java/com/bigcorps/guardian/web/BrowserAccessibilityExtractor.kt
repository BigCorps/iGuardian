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
        300

    fun extractHost(
        root: AccessibilityNodeInfo,
        spec: BrowserSpec
    ): BrowserUrlExtraction {
        spec.urlBarIds.forEach {
            id ->
            val nodes =
                runCatching {
                    root.findAccessibilityNodeInfosByViewId(
                        id
                    )
                }.getOrNull()
                    .orEmpty()

            nodes.firstOrNull {
                node ->
                node.isVisibleToUser
            }?.let {
                node ->
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

        val queue =
            ArrayDeque<
                AccessibilityNodeInfo
                >()

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
                        ) &&
                    node.className
                        ?.toString()
                        ?.contains(
                            "EditText"
                        ) ==
                        true

            if (
                looksLikeAddressField &&
                node.isVisibleToUser
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
                    ?.let {
                        queue.addLast(
                            it
                        )
                    }
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
        val queue =
            ArrayDeque<
                AccessibilityNodeInfo
                >()

        queue.add(
            root
        )

        val markers =
            mutableListOf<
                BrowserAccessibilityMarker
                >()

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
                    ?.let {
                        queue.addLast(
                            it
                        )
                    }
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
