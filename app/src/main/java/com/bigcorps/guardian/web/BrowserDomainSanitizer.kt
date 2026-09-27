package com.bigcorps.guardian.web

import java.net.IDN
import java.net.URI
import java.util.Locale

object BrowserDomainSanitizer {
    private const val MAX_RAW_LENGTH =
        2048

    private val forbiddenSchemes =
        setOf(
            "about",
            "blob",
            "chrome",
            "chrome-native",
            "data",
            "file",
            "javascript",
            "view-source",
            "edge",
            "opera"
        )

    // OCR is inherently noisier than a browser tree value. Keep the general
    // sanitizer permissive enough for real URLs, but require a plausible public
    // suffix before accepting a visual-only candidate. Two-letter ccTLDs and
    // 3-4 letter TLDs cover the common case; longer modern TLDs are explicit.
    private val visualLongTlds =
        setOf(
            "academy",
            "agency",
            "business",
            "camera",
            "careers",
            "center",
            "cloud",
            "company",
            "digital",
            "education",
            "email",
            "finance",
            "financial",
            "global",
            "health",
            "international",
            "media",
            "money",
            "museum",
            "network",
            "online",
            "photography",
            "services",
            "solutions",
            "software",
            "store",
            "studio",
            "support",
            "systems",
            "technology",
            "today",
            "travel",
            "website",
            "world"
        )

    fun hostFromRaw(
        raw: CharSequence?
    ): String? {
        val text =
            raw
                ?.toString()
                ?.trim()
                ?.take(
                    MAX_RAW_LENGTH
                )
                ?: return null

        if (
            text.isBlank() ||
            text.any {
                it.isWhitespace()
            }
        ) {
            return null
        }

        val schemeCandidate =
            text.substringBefore(
                ":",
                ""
            )
                .lowercase(
                    Locale.ROOT
                )

        if (
            schemeCandidate in
            forbiddenSchemes
        ) {
            return null
        }

        val candidate =
            if (
                text.matches(
                    Regex(
                        "^[A-Za-z][A-Za-z0-9+.-]*://.*"
                    )
                )
            ) {
                text
            } else {
                "https://$text"
            }

        val uri =
            runCatching {
                URI(
                    candidate
                )
            }.getOrNull()
                ?: return null

        val scheme =
            uri.scheme
                ?.lowercase(
                    Locale.ROOT
                )
                ?: return null

        if (
            scheme !=
            "http" &&
            scheme !=
            "https"
        ) {
            return null
        }

        val rawHost =
            uri.host
                ?: return null

        val ascii =
            runCatching {
                IDN.toASCII(
                    rawHost
                )
            }.getOrNull()
                ?.lowercase(
                    Locale.ROOT
                )
                ?.trimEnd(
                    '.'
                )
                ?.removePrefix(
                    "www."
                )
                ?: return null

        if (
            ascii.isBlank() ||
            ascii.length >
            253 ||
            ascii.contains(
                "/"
            ) ||
            ascii.contains(
                "?"
            ) ||
            ascii.contains(
                "#"
            ) ||
            ascii.contains(
                "@"
            )
        ) {
            return null
        }

        val validHost =
            ascii ==
                "localhost" ||
                ascii.matches(
                    Regex(
                        "^[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?$"
                    )
                )

        return ascii.takeIf {
            validHost
        }
    }

    /**
     * Stricter acceptance gate for ML Kit OCR. This intentionally rejects
     * single words such as "kit", "fallback", "https", "app" and "quiser"
     * even though a permissive URI parser could treat them as host names.
     * Tree-derived URL-bar values continue to use [hostFromRaw].
     */
    fun hostFromVisualOcr(
        raw: CharSequence?
    ): String? {
        val host =
            hostFromRaw(
                raw
            )
                ?: return null

        if (
            host ==
            "localhost"
        ) {
            return null
        }

        val labels =
            host.split(
                '.'
            )

        if (
            labels.size <
            2 ||
            labels.any {
                it.isBlank() ||
                    it.startsWith(
                        "-"
                    ) ||
                    it.endsWith(
                        "-"
                    )
            }
        ) {
            return null
        }

        val tld =
            labels.last()
                .lowercase(
                    Locale.ROOT
                )

        val plausibleSuffix =
            tld.startsWith(
                "xn--"
            ) ||
                (
                    tld.length ==
                        2 &&
                    tld.all {
                        it.isLetter()
                    }
                ) ||
                (
                    tld.length in
                        3..4 &&
                    tld.all {
                        it.isLetter()
                    }
                ) ||
                tld in
                    visualLongTlds

        if (
            !plausibleSuffix
        ) {
            return null
        }

        val hasNamedLabel =
            labels
                .dropLast(
                    1
                )
                .any {
                    label ->
                    label.any {
                        it.isLetter()
                    }
                }

        return host.takeIf {
            hasNamedLabel
        }
    }

    fun isSanitizedHost(
        value: String?
    ): Boolean =
        value !=
            null &&
            hostFromRaw(
                value
            ) ==
                value
}
