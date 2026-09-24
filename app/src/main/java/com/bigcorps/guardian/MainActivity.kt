package com.bigcorps.guardian

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.bigcorps.guardian.core.DiagnosticsGenerator
import com.bigcorps.guardian.core.GuardianPrivacyOverride
import com.bigcorps.guardian.core.PrivatePreferences
import com.bigcorps.guardian.core.ReportGenerator
import com.bigcorps.guardian.core.UsageAccess
import com.bigcorps.guardian.core.UsageCollector
import com.bigcorps.guardian.ui.PrivateAppsActivity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var summaryText: TextView
    private lateinit var capabilityText: TextView
    private lateinit var deviceName: EditText
    private var pendingExport: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuardianPrivacyOverride.closePendingIfAny(applicationContext)
        buildUi()
        refresh(false)
    }

    override fun onResume() {
        super.onResume()
        refresh(true)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        val scroll = ScrollView(this).apply { addView(root) }

        root.addView(text("Guardian DEV", 28f, true))
        root.addView(text("Android 0.1.0 • local-only", 14f, false).apply { setPadding(0, dp(4), 0, dp(18)) })

        root.addView(text("Privacidade", 19f, true))
        root.addView(text(
            "Este build não declara permissão de Internet. Configurações, apps protegidos e troca de usuário viram apenas PRIVATE antes de chegar ao SQLite. Navegação anônima tem tipo próprio, mas a detecção automática ainda não é confiável no Android v0.1.0.",
            15f,
            false
        ).apply { setPadding(0, dp(6), 0, dp(18)) })

        statusText = text("", 16f, true)
        root.addView(statusText)
        root.addView(button("Conceder acesso de uso") { openUsageSettings() })
        root.addView(button("Atualizar agora") { refresh(true) })
        root.addView(button("Escolher apps privados") {
            startActivity(Intent(this, PrivateAppsActivity::class.java))
        })

        root.addView(text("Nome local do aparelho", 16f, true).apply { setPadding(0, dp(18), 0, dp(4)) })
        deviceName = EditText(this).apply {
            hint = "Ex.: Meu celular"
            setText(getSharedPreferences("guardian_user", MODE_PRIVATE).getString("device_name", ""))
        }
        root.addView(deviceName)
        root.addView(button("Salvar nome") {
            getSharedPreferences("guardian_user", MODE_PRIVATE).edit()
                .putString("device_name", deviceName.text.toString().trim())
                .apply()
            Toast.makeText(this, "Nome salvo somente neste aparelho.", Toast.LENGTH_SHORT).show()
            refresh(false)
        })

        root.addView(text("Hoje", 19f, true).apply { setPadding(0, dp(22), 0, dp(6)) })
        summaryText = text("", 15f, false)
        root.addView(summaryText)

        root.addView(button("Exportar JSON do dia") {
            exportFresh(false)
        })

        root.addView(button("Exportar JSON de diagnóstico") {
            exportFresh(true)
        })

        root.addView(text("Capacidades deste build", 19f, true).apply { setPadding(0, dp(22), 0, dp(6)) })
        capabilityText = text("", 14f, false)
        root.addView(capabilityText)

        setContentView(scroll)
    }

    private fun refresh(runCollector: Boolean) {
        val privacyReady = PrivatePreferences(this).setupComplete()
        statusText.text = buildString {
            append(if (privacyReady) "Revisão de apps privados: OK" else "Revisão de apps privados: NECESSÁRIA")
            append("\n")
            append(if (UsageAccess.hasPermission(this@MainActivity)) "Acesso de uso: AUTORIZADO" else "Acesso de uso: NECESSÁRIO")
        }

        capabilityText.text = buildString {
            appendLine("✓ Uso por aplicativo")
            appendLine("✓ Tela não interativa / desbloqueios")
            appendLine("✓ SQLite + JSON local")
            appendLine("✓ PRIVATE antes do armazenamento")
            appendLine("✓ Diagnóstico para suporte BigCorps")
            appendLine("— Domínios: ainda não")
            appendLine("— Detecção automática de guia anônima: ainda não")
            append("— Nuvem/API externa: não existe neste build")
        }

        if (runCollector) {
            statusText.text = "Coletando eventos locais…"
            Thread {
                val result = UsageCollector(applicationContext).collect()
                runOnUiThread {
                    Toast.makeText(this, "Coleta: ${result.note} • ${result.eventsRead} eventos lidos", Toast.LENGTH_SHORT).show()
                    updateSummary()
                    statusText.text = if (result.permission) "Acesso de uso: AUTORIZADO" else "Acesso de uso: NECESSÁRIO"
                }
            }.start()
        } else {
            updateSummary()
        }
    }

    private fun updateSummary() {
        val summary = ReportGenerator(this).todaySummary()
        summaryText.text = buildString {
            appendLine("Apps: ${summary.appSeconds / 60} min")
            appendLine("PRIVATE: ${summary.privateSeconds / 60} min")
            appendLine("Tela desligada/não interativa: ${summary.screenOffSeconds / 60} min")
            appendLine("Navegação anônima: detecção automática indisponível neste build")
            appendLine("Desbloqueios detectados: ${summary.unlockCount}")
            if (summary.topApps.isNotEmpty()) {
                appendLine()
                appendLine("Mais usados:")
                summary.topApps.take(5).forEach { app ->
                    appendLine("• ${app.name}: ${app.seconds / 60} min")
                }
            }
        }.trim()
    }

    private fun openUsageSettings() {
        if (!PrivatePreferences(this).setupComplete()) {
            Toast.makeText(this, "Revise primeiro quais apps devem ficar sempre PRIVATE.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, PrivateAppsActivity::class.java))
            return
        }
        val primary = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(primary)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun exportFresh(diagnostic: Boolean) {
        statusText.text = "Preparando exportação local…"
        Thread {
            UsageCollector(applicationContext).collect()
            val contents: String
            val filename: String
            if (diagnostic) {
                contents = DiagnosticsGenerator(applicationContext).generate().toString(2)
                filename = "guardian-diagnostico-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.json"
            } else {
                contents = ReportGenerator(applicationContext).todayJson().toString(2)
                filename = "guardian-dia-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"
            }
            runOnUiThread {
                statusText.text = if (UsageAccess.hasPermission(this)) "Acesso de uso: AUTORIZADO" else "Acesso de uso: NECESSÁRIO"
                updateSummary()
                requestSave(contents, filename)
            }
        }.start()
    }

    private fun requestSave(contents: String, filename: String) {
        pendingExport = contents
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_EXPORT)
    }

    @Deprecated("Legacy Activity result API kept to avoid AndroidX dependency in the foundation build")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_EXPORT || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val payload = pendingExport ?: return
        try {
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer?.write(payload)
            }
            Toast.makeText(this, "JSON exportado.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível salvar o JSON.", Toast.LENGTH_LONG).show()
        } finally {
            pendingExport = null
        }
    }

    private fun text(value: String, sizeSp: Float, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = sizeSp
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setTextIsSelectable(true)
    }

    private fun button(label: String, action: (View) -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { view -> action(view) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_EXPORT = 9001
    }
}
