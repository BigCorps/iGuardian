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
            markers.map {
                marker ->
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

        if (
            spec.family ==
            "firefox"
        ) {
            val firefoxPrivate =
                normalized.any {
                    marker ->
                    val description =
                        marker.contentDescription
                            .orEmpty()

                    description.contains(
                        "disable private browsing"
                    ) ||
                        description.contains(
                            "desativar navegação privada"
                        ) ||
                        description.contains(
                            "desativar navegação privativa"
                        )
                }

            if (
                firefoxPrivate
            ) {
                return BrowserPrivateModeResult(
                    true,
                    "FIREFOX_DISABLE_PRIVATE_BUTTON"
                )
            }
        }

        val strongDescriptions =
            setOf(
                "leave incognito mode",
                "selected incognito tab",
                "incognito tab selected",
                "sair do modo de navegação anônima",
                "guia anônima selecionada",
                "guias anônimas selecionadas"
            )

        val descriptionMatch =
            normalized.any {
                marker ->
                marker.contentDescription in
                    strongDescriptions ||
                    marker.text in
                        strongDescriptions
            }

        if (
            descriptionMatch
        ) {
            return BrowserPrivateModeResult(
                true,
                "STRONG_PRIVATE_ACCESSIBILITY_LABEL"
            )
        }

        val resourceMatch =
            normalized.any {
                marker ->
                val id =
                    marker.resourceId
                        .orEmpty()

                (
                    id.contains(
                        "incognito"
                    ) ||
                        id.contains(
                            "private_browsing"
                        )
                    ) &&
                    (
                        id.contains(
                            "badge"
                        ) ||
                            id.contains(
                                "indicator"
                            ) ||
                            id.contains(
                                "selected"
                            )
                        )
            }

        if (
            resourceMatch
        ) {
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
}
