package com.bigcorps.guardian.web

import java.util.Locale

data class BrowserVisualParseResult(
    val host: String?,
    val privateModeDetected: Boolean,
    val privateReason: String
)

object BrowserVisualTextParser {
    fun parse(
        toolbarTexts: List<String>,
        modeProbeText: String
    ): BrowserVisualParseResult {
        val host =
            toolbarTexts
                .asSequence()
                .flatMap {
                    line ->
                    tokenCandidates(
                        line
                    ).asSequence()
                }
                .mapNotNull {
                    candidate ->
                    BrowserDomainSanitizer.hostFromRaw(
                        candidate
                    )
                }
                .firstOrNull()

        val privateDetected =
            detectsPrivateModeText(
                modeProbeText
            )

        return BrowserVisualParseResult(
            host =
                host,
            privateModeDetected =
                privateDetected,
            privateReason =
                if (
                    privateDetected
                ) {
                    "VISUAL_INCOGNITO_TEXT"
                } else {
                    "NO_VISUAL_PRIVATE_TEXT"
                }
        )
    }

    private fun tokenCandidates(
        raw: String
    ): List<String> {
        val normalized =
            raw
                .replace(
                    '\u00A0',
                    ' '
                )
                .replace(
                    '／',
                    '/'
                )
                .replace(
                    '：',
                    ':'
                )
                .trim()

        val tokens =
            normalized.split(
                Regex(
                    """[\s|]+"""
                )
            )

        return tokens
            .map {
                token ->
                token.trim(
                    ' ',
                    '"',
                    '\'',
                    '(',
                    ')',
                    '[',
                    ']',
                    '{',
                    '}',
                    ',',
                    ';',
                    '!',
                    '?',
                    '•',
                    '·'
                )
            }
            .filter {
                token ->
                token.length in
                    4..253 &&
                    token.contains(
                        '.'
                    ) &&
                    token.any {
                        it.isLetter()
                    } &&
                    !token.contains(
                        '@'
                    )
            }
    }

    fun detectsPrivateModeText(
        raw: String
    ): Boolean {
        val value =
            raw
                .lowercase(
                    Locale.ROOT
                )
                .replace(
                    'á',
                    'a'
                )
                .replace(
                    'â',
                    'a'
                )
                .replace(
                    'ã',
                    'a'
                )
                .replace(
                    'é',
                    'e'
                )
                .replace(
                    'ê',
                    'e'
                )
                .replace(
                    'í',
                    'i'
                )
                .replace(
                    'ó',
                    'o'
                )
                .replace(
                    'ô',
                    'o'
                )
                .replace(
                    'õ',
                    'o'
                )
                .replace(
                    'ú',
                    'u'
                )
                .replace(
                    'ç',
                    'c'
                )

        return listOf(
            "you've gone incognito",
            "you are incognito",
            "incognito mode",
            "leave incognito mode",
            "selected incognito tab",
            "modo de navegacao anonima",
            "voce entrou no modo de navegacao anonima",
            "guia anonima selecionada",
            "aba anonima selecionada"
        ).any {
            marker ->
            value.contains(
                marker
            )
        }
    }
}
