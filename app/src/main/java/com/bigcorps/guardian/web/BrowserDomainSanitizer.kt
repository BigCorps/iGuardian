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
