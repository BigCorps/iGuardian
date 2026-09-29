package com.bigcorps.guardian

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.bigcorps.guardian.core.ExportStorage
import com.bigcorps.guardian.core.GuardianDatabase
import com.bigcorps.guardian.core.PrivatePreferences
import com.bigcorps.guardian.core.ReportGenerator
import com.bigcorps.guardian.core.UsageAccess
import com.bigcorps.guardian.core.UsageCollector
import com.bigcorps.guardian.core.ValidationPackGenerator
import com.bigcorps.guardian.ui.PrivateAppsActivity
import com.bigcorps.guardian.web.BrowserWebActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ConfIA.vc launcher for the post-Accessibility architecture.
 *
 * The legacy Guardian MainActivity remains in source temporarily only to preserve
 * internal lineage while the new product UI is validated. It is intentionally no
 * longer registered in AndroidManifest.xml.
 */
class ConfiaMainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var summaryText: TextView
    private lateinit var permissionHelpCard: LinearLayout
    private lateinit var exportButton: Button
    private lateinit var exportSpinner: ProgressBar

    @Volatile
    private var exportInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refresh(false)
    }

    override fun onResume() {
        super.onResume()
        refresh(true)
    }

    private fun buildUi() {
        window.statusBarColor = BACKGROUND
        window.navigationBarColor = BACKGROUND

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(36))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(BACKGROUND)
            clipToPadding = true
            addView(root)
        }
        applySystemBarInsets(scroll)

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brandRow.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.confia_mark)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            },
            LinearLayout.LayoutParams(dp(76), dp(76)).apply {
                marginEnd = dp(12)
            }
        )
        brandRow.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(textView("ConfIA.vc", 27f, true, TEXT_PRIMARY))
                addView(textView("Mais controle. Mais tranquilidade.", 13f, false, TEXT_MUTED).apply {
                    setPadding(0, dp(3), 0, 0)
                })
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        root.addView(brandRow)

        root.addView(
            textView("Android ${currentVersionName()} • build de teste", 12f, true, PURPLE).apply {
                setPadding(0, dp(8), 0, dp(14))
            }
        )

        root.addView(card().apply {
            addView(textView("Privacidade por padrão", 18f, true, TEXT_PRIMARY))
            addView(textView(
                "O ConfIA registra tempo de uso e metadados mínimos. Apps protegidos continuam PRIVATE antes do armazenamento. O monitoramento web novo não usa leitura de tela.",
                14f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(7), 0, 0) })
            addView(textView(
                "Sem Acessibilidade • sem screenshot • sem VPN",
                12f,
                true,
                PURPLE
            ).apply { setPadding(0, dp(12), 0, 0) })
        })

        root.addView(sectionTitle("Configuração local"))

        root.addView(card().apply {
            statusText = textView("", 14f, true, TEXT_PRIMARY)
            addView(statusText)

            addView(outlineButton("Revisar apps privados") {
                startActivity(Intent(this@ConfiaMainActivity, PrivateAppsActivity::class.java))
            }.apply { topMargin(12) })
        })

        permissionHelpCard = card(WARNING_BG).apply {
            addView(textView("Acesso de uso necessário", 17f, true, WARNING_TEXT))
            addView(textView(
                "Esse acesso permite medir qual aplicativo está em primeiro plano. Ele não mostra URL, conteúdo, senha ou texto digitado.",
                13f,
                false,
                WARNING_TEXT
            ).apply { setPadding(0, dp(7), 0, 0) })
            addView(outlineButton("1. Abrir Informações do app") {
                showRestrictedSettingsHelp()
            }.apply { topMargin(12) })
            addView(primaryButton("2. Conceder acesso de uso") {
                openUsageSettings()
            }.apply { topMargin(10) })
        }
        root.addView(permissionHelpCard)

        root.addView(sectionTitle("Hoje"))
        root.addView(card().apply {
            summaryText = textView("Calculando…", 14f, false, TEXT_MUTED)
            addView(summaryText)
            addView(primaryButton("Atualizar dados locais") {
                refresh(true)
            }.apply { topMargin(12) })
        })

        root.addView(sectionTitle("ConfIA Web"))
        root.addView(card().apply {
            addView(textView(
                "Domínio pelo próprio navegador, não pela tela",
                15f,
                true,
                PURPLE
            ))
            addView(textView(
                "A nova arquitetura usa uma extensão autorizada no navegador para reduzir a URL imediatamente ao domínio principal. O app Android continua responsável apenas pelo tempo em primeiro plano via Usage Access.",
                13f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })
            addView(textView(
                "Firefox Nightly + Edge Canary/Beta: POC sem USB pronto nesta build • Chrome/Brave/Opera/Samsung/Mi: tempo do navegador apenas nesta fase.",
                12f,
                true,
                ORANGE
            ).apply { setPadding(0, dp(12), 0, 0) })
            addView(primaryButton("Ver navegadores compatíveis") {
                startActivity(Intent(this@ConfiaMainActivity, BrowserWebActivity::class.java))
            }.apply { topMargin(12) })
        })

        root.addView(sectionTitle("Validação"))
        root.addView(card().apply {
            addView(textView(
                "O pacote técnico confirma o estado local do aparelho e ajuda a validar cada rodada do POC.",
                13f,
                false,
                TEXT_MUTED
            ))

            val row = LinearLayout(this@ConfiaMainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, 0)
            }
            exportButton = primaryButton("Exportar pacote de validação") {
                exportValidationPack()
            }
            exportSpinner = ProgressBar(this@ConfiaMainActivity).apply {
                isIndeterminate = true
                indeterminateTintList = ColorStateList.valueOf(PURPLE)
                visibility = View.GONE
            }
            row.addView(exportButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(exportSpinner, LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                marginStart = dp(10)
            })
            addView(row, matchWidth())
        })

        root.addView(card(BRAND_SOFT).apply {
            addView(textView("Arquitetura desta build", 14f, true, TEXT_PRIMARY))
            addView(textView(
                "✓ UsageStats local para tempo por app\n" +
                    "✓ PRIVATE antes do armazenamento\n" +
                    "✓ ConfIA Web sem AccessibilityService registrado\n" +
                    "✓ Nenhum screenshot/OCR usado pelo fluxo novo\n" +
                    "✓ Nenhuma VPN\n" +
                    "✓ Backend confia.* preparado no Supabase minhAi\n" +
                    "✓ XPI Firefox e CRX Edge incorporados para teste sem USB",
                12f,
                false,
                TEXT_MUTED
            ).apply { setPadding(0, dp(8), 0, 0) })
        }.apply { topMargin(14) })

        setContentView(scroll)
    }

    private fun refresh(runCollector: Boolean) {
        val privacyReady = PrivatePreferences(this).setupComplete()
        val usageAllowed = UsageAccess.hasPermission(this)

        statusText.text = buildString {
            append(if (privacyReady) "✓ Revisão de apps privados concluída" else "• Revisão de apps privados pendente")
            append("\n")
            append(if (usageAllowed) "✓ Acesso de uso autorizado" else "• Acesso de uso pendente")
            append("\n✓ AccessibilityService do ConfIA não faz parte desta build")
        }
        permissionHelpCard.visibility = if (usageAllowed) View.GONE else View.VISIBLE

        if (runCollector && usageAllowed) {
            summaryText.text = "Atualizando eventos locais…"
            Thread {
                val result = UsageCollector(applicationContext).collect()
                runOnUiThread {
                    updateSummary()
                    Toast.makeText(
                        this,
                        "Coleta local: ${result.eventsRead} eventos • ${result.note}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.start()
        } else {
            updateSummary()
        }
    }

    private fun updateSummary() {
        val summary = ReportGenerator(this).todaySummary()
        summaryText.text = buildString {
            appendLine("Apps: ${durationLabel(summary.appSeconds)}")
            appendLine("PRIVATE: ${durationLabel(summary.privateSeconds)}")
            appendLine("Tela não interativa: ${durationLabel(summary.screenOffSeconds)}")
            appendLine("Desbloqueios: ${summary.unlockCount}")
            if (summary.topApps.isNotEmpty()) {
                appendLine()
                appendLine("Mais usados:")
                summary.topApps.take(5).forEach { app ->
                    appendLine("• ${app.name}: ${durationLabel(app.seconds)}")
                }
            }
        }.trim()
    }

    private fun exportValidationPack() {
        if (exportInProgress) return
        exportInProgress = true
        exportButton.isEnabled = false
        exportSpinner.visibility = View.VISIBLE

        Thread {
            try {
                UsageCollector(applicationContext).collect()
                val contents = ValidationPackGenerator.generate(applicationContext).toString()
                val filename = "confia-validacao-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.json"
                val storage = ExportStorage(applicationContext)
                if (!storage.supportsDirectDownloads()) {
                    throw IllegalStateException("exportacao_direta_requer_android_10")
                }
                val saved = storage.saveToDownloads(filename, contents)
                runCatching {
                    GuardianDatabase(applicationContext).logTechnical("VALIDATION_EXPORT_OK", "bytes=${saved.bytes}")
                }
                runOnUiThread {
                    exportInProgress = false
                    exportButton.isEnabled = true
                    exportSpinner.visibility = View.GONE
                    AlertDialog.Builder(this)
                        .setTitle("JSON salvo e verificado")
                        .setMessage("${saved.bytes} bytes gravados com sucesso.\n\n${saved.locationLabel}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    exportInProgress = false
                    exportButton.isEnabled = true
                    exportSpinner.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Não foi possível exportar: ${t.message ?: t::class.java.simpleName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun openUsageSettings() {
        if (!PrivatePreferences(this).setupComplete()) {
            Toast.makeText(this, "Revise primeiro quais apps devem ficar sempre PRIVATE.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, PrivateAppsActivity::class.java))
            return
        }
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun showRestrictedSettingsHelp() {
        AlertDialog.Builder(this)
            .setTitle("Liberar configuração restrita")
            .setMessage(
                "Em APK instalado manualmente, o Android pode exigir uma liberação adicional.\n\n" +
                    "1. Abra Informações do app.\n" +
                    "2. Na tela principal, toque em ⋮.\n" +
                    "3. Escolha ‘Permitir configurações restritas’.\n" +
                    "4. Volte ao ConfIA.vc e conceda apenas o Acesso de uso."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Abrir Informações do app") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
            .show()
    }

    private fun applySystemBarInsets(view: View) {
        view.setOnApplyWindowInsetsListener { target, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                target.setPadding(0, bars.top, 0, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                target.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            }
            insets
        }
        view.requestApplyInsets()
    }

    private fun sectionTitle(value: String) = textView(value, 19f, true, TEXT_PRIMARY).apply {
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

    private fun textView(value: String, sizeSp: Float, bold: Boolean, color: Int) = TextView(this).apply {
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

    private fun durationLabel(seconds: Long): String {
        if (seconds < 60L) return "${seconds}s"
        val minutes = seconds / 60L
        if (minutes < 60L) return "${minutes}m"
        val hours = minutes / 60L
        val rest = minutes % 60L
        return if (rest == 0L) "${hours}h" else "${hours}h ${rest}m"
    }

    private fun currentVersionName(): String = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    } catch (_: Exception) {
        "?"
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BACKGROUND = Color.rgb(255, 249, 246)
        private val CARD = Color.WHITE
        private val BRAND_SOFT = Color.rgb(255, 247, 242)
        private val TEXT_PRIMARY = Color.rgb(30, 31, 38)
        private val TEXT_MUTED = Color.rgb(88, 91, 103)
        private val PURPLE = Color.rgb(122, 22, 232)
        private val ORANGE = Color.rgb(255, 106, 26)
        private val WARNING_BG = Color.rgb(255, 247, 232)
        private val WARNING_TEXT = Color.rgb(112, 75, 16)
    }
}
