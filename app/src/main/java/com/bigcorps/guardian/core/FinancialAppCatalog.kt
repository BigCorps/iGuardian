package com.bigcorps.guardian.core

import java.text.Normalizer
import java.util.Locale

/**
 * Local-only classifier used by Bank Mode. It intentionally contains no account
 * or transaction information: only app package/label patterns needed to decide
 * whether Guardian Web should be disabled before a financial app is opened.
 */
object FinancialAppCatalog {
    private val exactPackages =
        setOf(
            // Confirmed on the user's device / Google Play.
            "br.com.inter.cdpro",       // Inter Empresas
            "br.com.intermedium",       // Inter
            "com.nu.production",        // Nubank
            "io.cloudwalk.infinitepaydash"
        )

    private val packageTokens =
        listOf(
            "bancointer",
            "intermedium",
            "nubank",
            "itau",
            "bradesco",
            "santander",
            "bancodobrasil",
            "bb.android",
            "caixa",
            "picpay",
            "mercadopago",
            "infinitepay",
            ".bank.",
            ".banco.",
            ".banking."
        )

    private val financialLabels =
        listOf(
            "inter",
            "inter empresas",
            "nubank",
            "itau",
            "bradesco",
            "santander",
            "caixa",
            "caixa tem",
            "banco do brasil",
            "bb",
            "mercado pago",
            "picpay",
            "infinitepay"
        )

    fun isFinancial(
        packageName: String,
        appLabel: String? = null
    ): Boolean {
        val normalizedPackage =
            packageName
                .trim()
                .lowercase(
                    Locale.ROOT
                )

        if (
            normalizedPackage in
            exactPackages
        ) {
            return true
        }

        if (
            packageTokens.any {
                token ->
                normalizedPackage.contains(
                    token
                )
            }
        ) {
            return true
        }

        val normalizedLabel =
            normalizeLabel(
                appLabel
            )

        if (
            normalizedLabel.isBlank()
        ) {
            return false
        }

        return financialLabels.any {
            label ->
            normalizedLabel ==
                label ||
                normalizedLabel.startsWith(
                    "$label "
                )
        }
    }

    private fun normalizeLabel(
        value: String?
    ): String =
        Normalizer.normalize(
            value
                ?.trim()
                .orEmpty(),
            Normalizer.Form.NFD
        )
            .replace(
                Regex("\\p{Mn}+") ,
                ""
            )
            .lowercase(
                Locale.ROOT
            )
}
