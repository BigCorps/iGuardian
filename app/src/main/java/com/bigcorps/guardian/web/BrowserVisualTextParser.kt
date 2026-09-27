package com.bigcorps.guardian.web

import java.text.Normalizer
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
                    BrowserDomainSanitizer.hostFromVisualOcr(
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

        // OCR may insert spaces around URL punctuation. Tighten only punctuation
        // that is meaningful inside an address before extracting candidates.
        val tightened =
            normalized
                .replace(
                    Regex(
                        "(?i)https?\\s*:\\s*/\\s*/"
                    )
                ) {
                    match ->
                    match.value
                        .replace(
                            Regex("\\s+") ,
                            ""
                        )
                }
                .replace(
                    Regex(
                        "\\s*\\.\\s*"
                    ),
                    "."
                )

        val tokenized =
            tightened
                .split(
                    Regex(
                        """[\s|]+"""
                    )
                )
                .map {
                    cleanCandidate(
                        it
                    )
                }

        val regexCandidates =
            DOMAIN_LIKE_REGEX
                .findAll(
                    tightened
                )
                .map {
                    cleanCandidate(
                        it.value
                    )
                }
                .toList()

        return (
            tokenized +
                regexCandidates
            )
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
            .distinct()
    }

    private fun cleanCandidate(
        token: String
    ): String =
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

    fun detectsPrivateModeText(
        raw: String
    ): Boolean {
        val value =
            normalizePrivateText(
                raw
            )

        if (
            value.isBlank()
        ) {
            return false
        }

        val strongMarkers =
            listOf(
                "youve gone incognito",
                "you are incognito",
                "now you can browse privately",
                "browsing privately",
                "leave incognito mode",
                "selected incognito tab",
                "voce entrou no modo de navegacao anonima",
                "voce entrou na navegacao anonima",
                "voce esta no modo de navegacao anonima",
                "voce esta na navegacao anonima",
                "modo de navegacao anonima",
                "modo anonimo",
                "agora voce pode navegar com privacidade",
                "guia anonima selecionada",
                "aba anonima selecionada"
            )

        if (
            strongMarkers.any {
                marker ->
                value.contains(
                    marker
                )
            }
        ) {
            return true
        }

        // These are actions that can be visible while the user is still in a
        // normal tab. Never treat them alone as proof that the current tab is
        // private/incognito.
        val actionOnlyMarkers =
            listOf(
                "new incognito tab",
                "new private tab",
                "enter incognito mode",
                "nova guia anonima",
                "nova aba anonima",
                "abrir guia anonima",
                "abrir aba anonima",
                "entrar no modo anonimo",
                "entrar no modo de navegacao anonima"
            )

        if (
            actionOnlyMarkers.any {
                marker ->
                value.contains(
                    marker
                )
            }
        ) {
            return false
        }

        // Chromium's current redesigned incognito NTP can expose its privacy
        // explanation even when the title OCR is imperfect. Require a pair of
        // contextual phrases rather than the ambiguous word "private" alone.
        val englishContext =
            value.contains(
                "other people who use this device"
            ) &&
                value.contains(
                    "activity"
                ) &&
                (
                    value.contains(
                        "privately"
                    ) ||
                    value.contains(
                        "incognito"
                    )
                )

        val portugueseContext =
            value.contains(
                "outras pessoas que usarem este dispositivo"
            ) &&
                value.contains(
                    "atividade"
                ) &&
                value.contains(
                    "privacidade"
                )

        return englishContext ||
            portugueseContext
    }

    private fun normalizePrivateText(
        raw: String
    ): String =
        Normalizer.normalize(
            raw,
            Normalizer.Form.NFD
        )
            .replace(
                Regex("\\p{Mn}+") ,
                ""
            )
            .lowercase(
                Locale.ROOT
            )
            .replace(
                '’',
                '\''
            )
            .replace(
                "'",
                ""
            )
            .replace(
                Regex("\\s+") ,
                " "
            )
            .trim()

    private val DOMAIN_LIKE_REGEX =
        Regex(
            "(?i)(?:https?://)?(?:www\\.)?[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?\\.[a-z]{2,}(?:[/?:#][^\\s]*)?"
        )
}
