package com.bigcorps.guardian.web

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import java.io.IOException

/**
 * ConfIA Web 0.1.28 — no-USB POC helper.
 *
 * The installed APK remains offline and does not use Accessibility, screenshots,
 * OCR, VPN or browser content. It only ships two extension packages as assets and
 * exports them to Downloads/ConfIA so the user can install them directly on the
 * phone in Firefox Nightly and Edge Canary/Beta. The extensions themselves reduce
 * the active URL to a host before any network transmission.
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
                addView(text("CONFIA WEB • POC 0.1.28", 11f, true, PURPLE))
                addView(text("Teste sem USB", 25f, true, TEXT_PRIMARY))
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        root.addView(brand)

        root.addView(text(
            "Esta build já traz os dois pacotes de extensão dentro do APK. Você salva cada arquivo em Downloads/ConfIA, instala diretamente no navegador de teste e navega normalmente. Não precisa de computador, ADB ou cabo USB.",
            14f,
            false,
            TEXT_MUTED
        ).apply { setPadding(0, dp(10), 0, 0) })

        root.addView(card(BRAND_SOFT).apply {
            addView(text("✓ Sem Acessibilidade", 14f, true, PURPLE))
            addView(text("✓ Sem screenshot ou OCR", 14f, true, PURPLE).apply { setPadding(0, dp(4), 0, 0) })
            addView(text("✓ Sem VPN", 14f, true, PURPLE).apply { setPadding(0, dp(4), 0, 0) })
            addView(text("✓ APK continua sem INTERNET", 14f, true, PURPLE).apply { setPadding(0, dp(4), 0, 0) })
            addView(text("✓ Extensões já pareadas com tokens temporários desta rodada", 14f, true, PURPLE).apply { setPadding(0, dp(4), 0, 0) })
        }.apply { topMargin(14) })

        root.addView(sectionTitle("1. Firefox Nightly"))
        val nightlyInstalled = isInstalled(FIREFOX_NIGHTLY)
        root.addView(card().apply {
            addView(text(
                if (nightlyInstalled) "✓ Firefox Nightly encontrado" else "Firefox Nightly necessário para o XPI local não assinado",
                15f,
                true,
                if (nightlyInstalled) PURPLE else ORANGE
            ))
            addView(text(
                "O Firefox Stable exige extensão assinada. Para este POC sem computador usamos Firefox Nightly, que permite instalar o arquivo local após habilitar o menu de desenvolvimento.",
                12f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })

            addView(primaryButton(
                if (nightlyInstalled) "Abrir Firefox Nightly" else "Instalar Firefox Nightly"
            ) {
                if (nightlyInstalled) openPackage(FIREFOX_NIGHTLY) else openStore(FIREFOX_NIGHTLY)
            }.apply { topMargin(10) })

            addView(outlineButton("Salvar extensão Firefox em Downloads") {
                exportExtension(
                    FIREFOX_ASSET,
                    FIREFOX_FILE,
                    "application/x-xpinstall",
                    firefoxInstructions()
                )
            }.apply { topMargin(10) })

            addView(text(
                "Depois de salvar:\n" +
                    "1. Firefox Nightly → Configurações → Sobre o Firefox Nightly.\n" +
                    "2. Toque 5× no logo para liberar o menu de desenvolvimento.\n" +
                    "3. Abra about:config e defina xpinstall.signatures.required = false.\n" +
                    "4. Configurações → Instalar extensão do arquivo.\n" +
                    "5. Escolha Downloads/ConfIA/$FIREFOX_FILE.\n" +
                    "6. Abra ConfIA Web POC e toque “Ativar e testar”.",
                12f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(12), 0, 0) })
        })

        root.addView(sectionTitle("2. Edge Canary / Beta"))
        val edgeCanaryInstalled = isInstalled(EDGE_CANARY)
        val edgeBetaInstalled = isInstalled(EDGE_BETA)
        val edgeTestPackage = when {
            edgeCanaryInstalled -> EDGE_CANARY
            edgeBetaInstalled -> EDGE_BETA
            else -> null
        }
        root.addView(card().apply {
            addView(text(
                if (edgeTestPackage != null) "✓ Edge de teste encontrado" else "Use Edge Canary ou Beta para sideload do CRX",
                15f,
                true,
                if (edgeTestPackage != null) PURPLE else ORANGE
            ))
            addView(text(
                "O Edge Stable não expõe um fluxo público confiável para instalar nosso CRX local. O POC usa Canary/Beta pelo menu oculto de Developer Options.",
                12f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })

            addView(primaryButton(
                if (edgeTestPackage != null) "Abrir Edge de teste" else "Instalar Edge Canary"
            ) {
                if (edgeTestPackage != null) openPackage(edgeTestPackage) else openStore(EDGE_CANARY)
            }.apply { topMargin(10) })

            addView(outlineButton("Salvar extensão Edge em Downloads") {
                exportExtension(
                    EDGE_ASSET,
                    EDGE_FILE,
                    "application/x-chrome-extension",
                    edgeInstructions()
                )
            }.apply { topMargin(10) })

            addView(text(
                "Depois de salvar:\n" +
                    "1. Edge Canary/Beta → Configurações → Sobre o Microsoft Edge.\n" +
                    "2. Toque 5× no número da versão/build para liberar Developer Options.\n" +
                    "3. Developer Options → Extension install by crx.\n" +
                    "4. Escolha Downloads/ConfIA/$EDGE_FILE.\n" +
                    "5. Abra ConfIA Web POC e toque “Ativar e testar”.",
                12f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(12), 0, 0) })
        })

        root.addView(sectionTitle("3. Navegação de teste"))
        root.addView(card().apply {
            addView(text("Firefox Nightly", 14f, true, TEXT_PRIMARY))
            addView(text("• uol.com.br — ~15 s\n• github.com — ~15 s\n• troque de aba e volte", 12f, false, TEXT_MUTED).apply { setPadding(0, dp(5), 0, 0) })

            addView(text("Edge Canary/Beta", 14f, true, TEXT_PRIMARY).apply { setPadding(0, dp(14), 0, 0) })
            addView(text("• google.com — ~15 s\n• wikipedia.org — ~15 s\n• troque de aba e volte", 12f, false, TEXT_MUTED).apply { setPadding(0, dp(5), 0, 0) })

            addView(text(
                "Ao terminar, volte ao ConfIA.vc e exporte o pacote de validação da tela principal. Eu cruzarei os intervalos UsageStats desse JSON com os eventos confia.browser_events no Supabase.",
                12f,
                true,
                PURPLE
            ).apply { setPadding(0, dp(14), 0, 0) })
        })

        root.addView(sectionTitle("Outros navegadores"))
        root.addView(browserInfoCard(
            "Chrome / Chrome Dev",
            "TEMPO DO APP",
            "O Chrome Android oficial continua sem instalação comum de WebExtensions; o ConfIA mede apenas o tempo do navegador via UsageStats nesta fase."
        ))
        root.addView(browserInfoCard(
            "Brave • Opera • Samsung Internet • Mi Browser",
            "TEMPO DO APP",
            "Continuam monitoráveis como aplicativos. O domínio só será ativado quando existir uma integração de navegador tecnicamente comprovada."
        ))

        root.addView(card().apply {
            addView(text("Histórico do método antigo", 14f, true, TEXT_PRIMARY))
            addView(text(
                "Domínios antigos de OCR/Acessibilidade são apenas legado local e não fazem parte deste teste.",
                12f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })
            addView(outlineButton("Limpar histórico web legado") {
                confirmLegacyClear()
            }.apply { topMargin(10) })
        }.apply { topMargin(10) })
    }

    private fun exportExtension(
        assetName: String,
        fileName: String,
        mimeType: String,
        instructions: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "O exportador direto deste POC requer Android 10+.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ConfIA")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("downloads_insert_failed")

            try {
                assets.open(assetName).use { input ->
                    resolver.openOutputStream(uri, "w")?.use { output ->
                        input.copyTo(output)
                        output.flush()
                    } ?: throw IOException("downloads_output_stream_unavailable")
                }
                val publish = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, publish, null, null)
            } catch (t: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw t
            }

            AlertDialog.Builder(this)
                .setTitle("Extensão salva")
                .setMessage("Downloads/ConfIA/$fileName\n\n$instructions")
                .setPositiveButton("OK", null)
                .show()
        } catch (t: Throwable) {
            Toast.makeText(
                this,
                "Não foi possível salvar a extensão: ${t.message ?: t::class.java.simpleName}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun firefoxInstructions(): String =
        "Use Firefox Nightly. Libere o menu de desenvolvimento, desative temporariamente a exigência de assinatura em about:config e escolha ‘Instalar extensão do arquivo’. O token POC já está pré-configurado dentro deste XPI."

    private fun edgeInstructions(): String =
        "Use Edge Canary/Beta. Libere Developer Options tocando 5× na versão e use ‘Extension install by crx’. O token POC já está pré-configurado dentro deste CRX."

    private fun browserInfoCard(title: String, badge: String, body: String): LinearLayout = card().apply {
        addView(text(title, 16f, true, TEXT_PRIMARY))
        addView(text(badge, 11f, true, TEXT_MUTED).apply { setPadding(0, dp(4), 0, 0) })
        addView(text(body, 12f, false, TEXT_MUTED).apply { setPadding(0, dp(8), 0, 0) })
    }

    private fun isInstalled(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

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
            .setMessage("Apaga somente sessões de sites produzidas pelos testes antigos. O histórico normal de aplicativos permanece intacto.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->
                GuardianDatabase(applicationContext).clearBrowserSessions()
                Toast.makeText(this, "Histórico web legado limpo.", Toast.LENGTH_SHORT).show()
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
        private const val FIREFOX_NIGHTLY = "org.mozilla.fenix"
        private const val EDGE_CANARY = "com.microsoft.emmx.canary"
        private const val EDGE_BETA = "com.microsoft.emmx.beta"
        private const val FIREFOX_ASSET = "confia-web-firefox-poc-0.1.1.xpi"
        private const val EDGE_ASSET = "confia-web-edge-poc-0.1.1.crx"
        private const val FIREFOX_FILE = "confia-web-firefox-poc-0.1.1.xpi"
        private const val EDGE_FILE = "confia-web-edge-poc-0.1.1.crx"

        private val BACKGROUND = Color.rgb(255, 249, 246)
        private val CARD = Color.WHITE
        private val BRAND_SOFT = Color.rgb(255, 247, 242)
        private val TEXT_PRIMARY = Color.rgb(30, 31, 38)
        private val TEXT_MUTED = Color.rgb(88, 91, 103)
        private val PURPLE = Color.rgb(122, 22, 232)
        private val ORANGE = Color.rgb(255, 106, 26)
    }
}
