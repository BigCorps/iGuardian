package com.bigcorps.guardian.web

import java.util.Locale

data class BrowserAccessibilityMarker(
    val resourceId: String?,
    val text: String?,
    val contentDescription: String?
)

data class BrowserPrivateModeResult(
    val isPrivate: Boolean,
    val reason: String
)

object BrowserPrivateModeHeuristics {
    fun detect(
        spec: BrowserSpec,
        markers: List<BrowserAccessibilityMarker>
    ): BrowserPrivateModeResult {
        val normalized =
            markers.map { marker ->
                BrowserAccessibilityMarker(
                    resourceId =
                        marker.resourceId
                            ?.lowercase(
                                Locale.ROOT
                            ),
                    text =
                        marker.text
                            ?.trim()
                            ?.lowercase(
                                Locale.ROOT
                            ),
                    contentDescription =
                        marker.contentDescription
                            ?.trim()
                            ?.lowercase(
                                Locale.ROOT
                            )
                )
            }

        if (spec.family == "firefox") {
            val firefoxPrivate =
                normalized.any { marker ->
                    val value =
                        (
                            marker.contentDescription
                                ?: marker.text
                                ?: ""
                            )

                    value.contains(
                        "disable private browsing"
                    ) ||
                        value.contains(
                            "desativar navegação privada"
                        ) ||
                        value.contains(
                            "desativar navegação privativa"
                        )
                }

            if (firefoxPrivate) {
                return BrowserPrivateModeResult(
                    true,
                    "FIREFOX_DISABLE_PRIVATE_BUTTON"
                )
            }
        }

        val positiveText =
            normalized.any { marker ->
                listOfNotNull(
                    marker.contentDescription,
                    marker.text
                ).any { value ->
                    isStrongPrivateText(
                        value
                    )
                }
            }

        if (positiveText) {
            return BrowserPrivateModeResult(
                true,
                "STRONG_PRIVATE_ACCESSIBILITY_LABEL"
            )
        }

        val resourceMatch =
            normalized.any { marker ->
                val id =
                    marker.resourceId
                        .orEmpty()

                id.endsWith(
                    ":id/location_bar_incognito_badge"
                ) ||
                    id.endsWith(
                        ":id/incognito_indicator"
                    ) ||
                    (
                        (
                            id.contains("incognito") ||
                                id.contains("private_browsing")
                            ) &&
                            (
                                id.contains("badge") ||
                                    id.contains("indicator") ||
                                    id.contains("selected")
                                )
                        )
            }

        if (resourceMatch) {
            return BrowserPrivateModeResult(
                true,
                "PRIVATE_RESOURCE_INDICATOR"
            )
        }

        return BrowserPrivateModeResult(
            false,
            "NO_STRONG_PRIVATE_MARKER"
        )
    }

    private fun isStrongPrivateText(
        raw: String
    ): Boolean {
        val value =
            raw.trim()
                .lowercase(
                    Locale.ROOT
                )

        if (
            value.contains("enter incognito mode") ||
            value.contains("new incognito tab") ||
            value.contains("nova guia anônima") ||
            value.contains("nova aba anônima") ||
            value.contains("entrar no modo de navegação anônima")
        ) {
            return false
        }

        return value == "incognito mode" ||
            value.contains("leave incognito mode") ||
            value.contains("selected incognito tab") ||
            value.endsWith(", incognito tab") ||
            value.endsWith(", selected incognito tab") ||
            value == "modo de navegação anônima" ||
            value.contains("sair do modo de navegação anônima") ||
            value.contains("guia anônima selecionada") ||
            value.contains("aba anônima selecionada") ||
            value.endsWith(", guia anônima") ||
            value.endsWith(", aba anônima")
    }
}
