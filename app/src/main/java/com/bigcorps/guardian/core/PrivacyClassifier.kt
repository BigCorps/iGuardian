package com.bigcorps.guardian.core

object PrivacyClassifier {
    private val exactPrivatePackages = setOf(
        "com.android.settings",
        "com.google.android.settings.intelligence",
        "com.google.android.apps.walletnfcrel",
        "com.google.android.apps.authenticator2",
        "com.azure.authenticator",
        "com.authy.authy",
        "com.bitwarden.app",
        "com.onepassword.android",
        "com.lastpass.lpandroid",
        "com.kunzisoft.keepass.free"
    )

    private val sensitiveTokens = listOf(
        "bank", "banco", "banking", "nubank", "intermedium", "bancointer", "itau", "bradesco",
        "santander", "bancodobrasil", "bb.android", "caixa", "picpay", "mercadopago", "wallet",
        "authenticator", "authy", "bitwarden", "1password", "onepassword", "lastpass", "keepass",
        "password", "passwd", "settings", "finance"
    )

    fun isAutomaticallyPrivate(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        if (normalized in exactPrivatePackages) return true
        if (normalized == "com.android.settings" || normalized.startsWith("com.android.settings.")) return true
        return sensitiveTokens.any { token -> normalized.contains(token) }
    }

    fun isPrivate(packageName: String, manuallyPrivate: Set<String> = emptySet()): Boolean {
        return packageName in manuallyPrivate || isAutomaticallyPrivate(packageName)
    }
}
