package com.bigcorps.guardian.core

object PrivacyClassifier {
    const val CLASSIFIER_VERSION = 2

    private val exactPrivatePackages = setOf(
        // Android/system settings
        "com.android.settings",
        "com.google.android.settings.intelligence",

        // Wallets/authenticators/password managers
        "com.google.android.apps.walletnfcrel",
        "com.google.android.apps.authenticator2",
        "com.azure.authenticator",
        "com.authy.authy",
        "com.bitwarden.app",
        "com.onepassword.android",
        "com.lastpass.lpandroid",
        "com.kunzisoft.keepass.free",

        // Confirmed financial apps from real-device testing
        "com.nu.production",
        "io.cloudwalk.infinitepaydash"
    )

    private val packageTokens = listOf(
        "bank",
        "banco",
        "banking",
        "nubank",
        "intermedium",
        "bancointer",
        "itau",
        "bradesco",
        "santander",
        "bancodobrasil",
        "bb.android",
        "caixa",
        "picpay",
        "mercadopago",
        "wallet",
        "authenticator",
        "authy",
        "bitwarden",
        "1password",
        "onepassword",
        "lastpass",
        "keepass",
        "password",
        "passwd",
        "settings",
        "finance"
    )

    private val labelTokens = listOf(
        "banco",
        "bank",
        "nubank",
        "infinitepay",
        "itaú",
        "itau",
        "bradesco",
        "santander",
        "caixa",
        "mercado pago",
        "picpay",
        "carteira",
        "wallet",
        "autenticador",
        "authenticator",
        "bitwarden",
        "1password",
        "onepassword",
        "lastpass",
        "keepass",
        "gerenciador de senhas",
        "password manager"
    )

    fun isAutomaticallyPrivate(
        packageName: String,
        appLabel: String? = null
    ): Boolean {
        val normalizedPackage = packageName.lowercase()

        if (normalizedPackage in exactPrivatePackages) return true

        if (
            normalizedPackage == "com.android.settings" ||
            normalizedPackage.startsWith("com.android.settings.")
        ) {
            return true
        }

        if (packageTokens.any { token -> normalizedPackage.contains(token) }) {
            return true
        }

        val normalizedLabel = appLabel
            ?.trim()
            ?.lowercase()
            .orEmpty()

        if (normalizedLabel.isNotBlank() &&
            labelTokens.any { token -> normalizedLabel.contains(token) }
        ) {
            return true
        }

        return false
    }

    fun isPrivate(
        packageName: String,
        manuallyPrivate: Set<String> = emptySet(),
        appLabel: String? = null
    ): Boolean {
        return packageName in manuallyPrivate ||
            isAutomaticallyPrivate(packageName, appLabel)
    }
}
