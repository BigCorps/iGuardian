package com.bigcorps.guardian.web

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.bigcorps.guardian.R
import com.bigcorps.guardian.core.GuardianDatabase

/**
 * ConfIA Web compatibility/onboarding screen for the post-Accessibility design.
 *
 * Domain acquisition is delegated to a browser extension on browsers that expose
 * the required WebExtension APIs. Android UsageStats remains responsible only for
 * the foreground interval. This Activity intentionally contains no Accessibility,
 * screenshot, OCR or bank-mode control path.
 */
class BrowserWebActivity : Activity() {
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BACKGROUND
        window.navigationBarColor = BACKGROUND

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(40))
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(BACKGROUND)
            addView(root)
        }
        setContentView(scroll)

        scroll.setOnApplyWindowInsetsListener { _, insets ->
            @Suppress("DEPRECATION") val topInset = insets.systemWindowInsetTop
            @Suppress("DEPRECATION") val bottomInset = insets.systemWindowInsetBottom
            root.setPadding(dp(18), topInset + dp(18), dp(18), bottomInset + dp(32))
            insets
        }
        scroll.requestApplyInsets()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        root.removeAllViews()

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brand.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.confia_mark)
                scaleType = ImageView.ScaleType.FIT_CENTER
            },
            LinearLayout.LayoutParams(dp(58), dp(58)).apply { marginEnd = dp(10) }
        )
        brand.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(text("CONFIA WEB • NOVA ARQUITETURA", 11f, true, PURPLE))
                addView(text("Domínio sem ler a tela", 25f, true, TEXT_PRIMARY))
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        root.addView(brand)

        root.addView(text(
            "O ConfIA Web agora busca o domínio onde essa informação realmente existe: dentro do próprio navegador. A extensão reduz a URL imediatamente para o host (ex.: github.com) e só esse host pode seguir para o backend. O app Android mede apenas quando o navegador está em primeiro plano.",
            14f,
            false,
            TEXT_MUTED
        ).apply { setPadding(0, dp(10), 0, 0) })

        root.addView(card(BRAND_SOFT).apply {
            addView(text("✓ Sem Acessibilidade", 14f, true, PURPLE))
            addView(text("✓ Sem screenshot ou OCR", 14f, true, PURPLE).apply { setPadding(0, dp(4), 0, 0) })
            addView(text("✓ Sem VPN", 14f, true, PURPLE).apply { setPadding(0, dp(4), 0, 0) })
            addView(text("✓ Bancos continuam fora do fluxo de captura web", 14f, true, PURPLE).apply { setPadding(0, dp(4), 0, 0) })
        }.apply { topMargin(14) })

        root.addView(sectionTitle("Navegadores compatíveis"))

        val firefoxPackages = listOf("org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix")
        val firefoxInstalled = firstInstalled(firefoxPackages)
        root.addView(browserCard(
            title = "Firefox Android",
            badge = "POC PRINCIPAL",
            supported = true,
            body = "Integração completa por WebExtension: mudança de aba/navegação → host principal. O modo privado pode ser suportado quando o usuário permite o complemento em navegação privativa.",
            actionLabel = if (firefoxInstalled != null) "Abrir Firefox" else "Instalar Firefox",
            action = {
                if (firefoxInstalled != null) openPackage(firefoxInstalled)
                else openStore("org.mozilla.firefox")
            }
        ))

        val edgePackages = listOf("com.microsoft.emmx", "com.microsoft.emmx.beta", "com.microsoft.emmx.dev", "com.microsoft.emmx.canary")
        val edgeInstalled = firstInstalled(edgePackages)
        root.addView(browserCard(
            title = "Edge Android",
            badge = "COMPATÍVEL / PRÓXIMA VALIDAÇÃO",
            supported = true,
            body = "A Microsoft documenta APIs móveis de extensão como tabs e webNavigation. Vamos validar o fluxo de distribuição/instalação Android depois do POC Firefox.",
            actionLabel = if (edgeInstalled != null) "Abrir Edge" else "Instalar Edge",
            action = {
                if (edgeInstalled != null) openPackage(edgeInstalled)
                else openStore("com.microsoft.emmx")
            }
        ))

        root.addView(browserCard(
            title = "Chrome / Chrome Dev",
            badge = "TEMPO DO APP",
            supported = false,
            body = "O Chrome Android oficial não oferece a mesma instalação normal de extensões. Nesta fase o ConfIA mede o tempo do navegador via UsageStats, mas não atribui um domínio sem uma fonte confiável.",
            actionLabel = null,
            action = null
        ))

        root.addView(browserCard(
            title = "Brave • Opera • Samsung Internet • Mi Browser",
            badge = "TEMPO DO APP",
            supported = false,
            body = "Continuam monitoráveis como aplicativos. O domínio só será ativado quando houver uma integração de navegador tecnicamente comprovada para cada família.",
            actionLabel = null,
            action = null
        ))

        root.addView(sectionTitle("Como o teste vai funcionar"))
        root.addView(card().apply {
            addView(text("1. Extensão", 14f, true, TEXT_PRIMARY))
            addView(text("Vê a aba ativa dentro do navegador e transforma a URL em host antes de transmitir.", 12f, false, TEXT_MUTED).apply { setPadding(0, dp(4), 0, 0) })

            addView(text("2. Supabase minhAi", 14f, true, TEXT_PRIMARY).apply { setPadding(0, dp(12), 0, 0) })
            addView(text("O schema confia recebe somente host + horário + fonte. A fundação já está preparada para o POC.", 12f, false, TEXT_MUTED).apply { setPadding(0, dp(4), 0, 0) })

            addView(text("3. Android", 14f, true, TEXT_PRIMARY).apply { setPadding(0, dp(12), 0, 0) })
            addView(text("UsageStats informa quando Firefox/Edge estavam em primeiro plano. Depois correlacionamos o host da extensão com esse intervalo.", 12f, false, TEXT_MUTED).apply { setPadding(0, dp(4), 0, 0) })
        })

        root.addView(card().apply {
            addView(text("Estado do POC", 14f, true, TEXT_PRIMARY))
            addView(text(
                "Backend confia.*: estrutura criada no Supabase.\n" +
                    "APK Android: sem AccessibilityService registrado.\n" +
                    "Extensão Firefox/Edge: próxima peça do teste.\n\n" +
                    "Esta build Android continua sem permissão INTERNET; quem transmitirá o host no POC será a extensão do navegador.",
                12f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })
        }.apply { topMargin(10) })

        root.addView(card().apply {
            addView(text("Histórico do método antigo", 14f, true, TEXT_PRIMARY))
            addView(text(
                "Se ainda houver domínios antigos salvos pelos testes de OCR/Acessibilidade, eles são apenas legado local e não fazem parte da nova arquitetura.",
                12f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })
            addView(outlineButton("Limpar histórico web legado") {
                confirmLegacyClear()
            }.apply { topMargin(10) })
        }.apply { topMargin(10) })
    }

    private fun browserCard(
        title: String,
        badge: String,
        supported: Boolean,
        body: String,
        actionLabel: String?,
        action: (() -> Unit)?
    ): LinearLayout = card().apply {
        addView(text(title, 16f, true, TEXT_PRIMARY))
        addView(text(
            badge,
            11f,
            true,
            if (supported) PURPLE else TEXT_MUTED
        ).apply { setPadding(0, dp(4), 0, 0) })
        addView(text(body, 12f, false, TEXT_MUTED).apply { setPadding(0, dp(8), 0, 0) })
        if (actionLabel != null && action != null) {
            addView(primaryButton(actionLabel) { action() }.apply { topMargin(10) })
        }
    }

    private fun firstInstalled(packages: List<String>): String? =
        packages.firstOrNull { packageName ->
            packageManager.getLaunchIntentForPackage(packageName) != null
        }

    private fun openPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Toast.makeText(this, "Não foi possível abrir o navegador.", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(intent)
    }

    private fun openStore(packageName: String) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        try {
            startActivity(market)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun confirmLegacyClear() {
        AlertDialog.Builder(this)
            .setTitle("Limpar histórico web legado?")
            .setMessage("Apaga somente as sessões de sites produzidas pelos testes antigos. O histórico normal de aplicativos permanece intacto.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->
                GuardianDatabase(applicationContext).clearBrowserSessions()
                Toast.makeText(this, "Histórico web legado limpo.", Toast.LENGTH_SHORT).show()
                render()
            }
            .show()
    }

    private fun sectionTitle(value: String) = text(value, 18f, true, TEXT_PRIMARY).apply {
        setPadding(0, dp(22), 0, dp(8))
    }

    private fun card(backgroundColor: Int = CARD) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(backgroundColor, 18f)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) }
    }

    private fun text(value: String, sizeSp: Float, bold: Boolean, color: Int) = TextView(this).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun primaryButton(label: String, action: (View) -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        minHeight = dp(52)
        stateListAnimator = null
        background = rounded(PURPLE, 14f)
        setOnClickListener { action(it) }
        layoutParams = matchWidth()
    }

    private fun outlineButton(label: String, action: (View) -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(PURPLE)
        minHeight = dp(50)
        stateListAnimator = null
        background = rounded(CARD, 14f, PURPLE, 1)
        setOnClickListener { action(it) }
        layoutParams = matchWidth()
    }

    private fun rounded(fillColor: Int, radiusDp: Float, strokeColor: Int? = null, strokeDp: Int = 0) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            if (strokeColor != null && strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun View.topMargin(valueDp: Int) {
        val lp = (layoutParams as? LinearLayout.LayoutParams) ?: matchWidth()
        lp.topMargin = dp(valueDp)
        layoutParams = lp
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BACKGROUND = Color.rgb(255, 249, 246)
        private val CARD = Color.WHITE
        private val BRAND_SOFT = Color.rgb(255, 247, 242)
        private val TEXT_PRIMARY = Color.rgb(30, 31, 38)
        private val TEXT_MUTED = Color.rgb(88, 91, 103)
        private val PURPLE = Color.rgb(122, 22, 232)
    }
}
