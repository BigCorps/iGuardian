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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.bigcorps.guardian.core.*
import com.bigcorps.guardian.ui.PrivateAppsActivity
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var permissionHelpCard: LinearLayout
    private lateinit var deviceName: EditText
    private lateinit var appsValue: TextView
    private lateinit var privateValue: TextView
    private lateinit var screenValue: TextView
    private lateinit var unlockValue: TextView
    private lateinit var systemValue: TextView
    private lateinit var topAppsText: TextView
    private lateinit var capabilityText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuardianPrivacyOverride.closePendingIfAny(applicationContext)
        buildUi()
        refresh(false)
    }

    override fun onResume() {
        super.onResume()
        // Scheduler is established at process start/boot/package replacement.
        // Do not replace the periodic job on every Activity resume.
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
            // Prevent scrolled content from drawing under status/navigation bars.
            clipToPadding = true
            addView(root)
        }
        applySystemBarInsets(scroll)

        root.addView(textView("GUARDIAN • BUILD DE TESTE", 11f, true, PRIMARY))
        root.addView(textView("Seu aparelho, explicado.", 28f, true, TEXT_PRIMARY).apply {
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(textView("Android 0.1.5 • 100% local", 14f, false, TEXT_MUTED).apply {
            setPadding(0, dp(4), 0, dp(16))
        })

        root.addView(card().apply {
            addView(textView("Privacidade por padrão", 18f, true, TEXT_PRIMARY))
            addView(
                textView(
                    "O Guardian registra metadados de uso, não conteúdo. Configurações, apps protegidos e outros usuários viram apenas PRIVATE antes do armazenamento.",
                    14f,
                    false,
                    TEXT_MUTED
                ).apply { setPadding(0, dp(6), 0, 0) }
            )
            addView(
                textView(
                    "Sem Internet • Sem captura de tela • Sem teclado",
                    12f,
                    true,
                    PRIMARY
                ).apply { setPadding(0, dp(12), 0, 0) }
            )
        })

        root.addView(sectionTitle("Configuração"))

        root.addView(card().apply {
            statusText = textView("", 15f, true, TEXT_PRIMARY)
            addView(statusText)
            addView(
                outlineButton("Revisar apps privados") {
                    startActivity(
                        Intent(
                            this@MainActivity,
                            PrivateAppsActivity::class.java
                        )
                    )
                }.apply { topMargin(12) }
            )
        })

        permissionHelpCard = card(WARNING_BG).apply {
            addView(textView("Acesso de uso necessário", 17f, true, WARNING_TEXT))
            addView(
                textView(
                    "Em APK instalado manualmente, o Android pode bloquear este acesso. Na tela principal de Informações do app, use ⋮ > Permitir configurações restritas.",
                    13f,
                    false,
                    WARNING_TEXT
                ).apply { setPadding(0, dp(7), 0, 0) }
            )
            addView(
                outlineButton("1. Abrir Informações do app") {
                    showRestrictedSettingsHelp()
                }.apply { topMargin(12) }
            )
            addView(
                primaryButton("2. Conceder acesso de uso") {
                    openUsageSettings()
                }.apply { topMargin(10) }
            )
        }
        root.addView(permissionHelpCard)

        root.addView(card().apply {
            addView(textView("Nome local do aparelho", 16f, true, TEXT_PRIMARY))
            addView(
                textView(
                    "Ajuda a identificar este aparelho nos relatórios exportados. Fica somente aqui.",
                    12f,
                    false,
                    TEXT_MUTED
                )
            )

            deviceName = EditText(this@MainActivity).apply {
                hint = "Ex.: Meu celular"
                setSingleLine(true)
                textSize = 16f
                backgroundTintList = ColorStateList.valueOf(PRIMARY)
                setText(
                    getSharedPreferences("guardian_user", MODE_PRIVATE)
                        .getString("device_name", "")
                )
            }
            addView(deviceName, matchWidth())

            addView(
                outlineButton("Salvar nome") {
                    getSharedPreferences("guardian_user", MODE_PRIVATE)
                        .edit()
                        .putString(
                            "device_name",
                            deviceName.text.toString().trim()
                        )
                        .apply()

                    Toast.makeText(
                        this@MainActivity,
                        "Nome salvo somente neste aparelho.",
                        Toast.LENGTH_SHORT
                    ).show()
                }.apply { topMargin(10) }
            )
        })

        root.addView(sectionTitle("Hoje"))

        val statsCard = card()
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        appsValue = statBlock(statsRow, "Apps")
        privateValue = statBlock(statsRow, "PRIVATE")
        screenValue = statBlock(statsRow, "Tela off")
        statsCard.addView(statsRow, matchWidth())

        statsCard.addView(
            View(this).apply { setBackgroundColor(DIVIDER) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(14)
            }
        )

        unlockValue = textView("0 desbloqueios", 14f, true, TEXT_PRIMARY)
        statsCard.addView(unlockValue)

        systemValue =
            textView("Navegação do sistema: 0s", 12f, false, TEXT_MUTED)
                .apply { setPadding(0, dp(6), 0, 0) }
        statsCard.addView(systemValue)

        topAppsText =
            textView("Ainda sem dados de uso.", 13f, false, TEXT_MUTED)
                .apply { setPadding(0, dp(8), 0, 0) }
        statsCard.addView(topAppsText)
        root.addView(statsCard)

        root.addView(
            primaryButton("Atualizar dados locais") {
                refresh(true)
            }.apply { topMargin(10) }
        )

        root.addView(sectionTitle("Exportar"))

        root.addView(card().apply {
            addView(
                textView(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        "O JSON será salvo diretamente em Downloads/iGuardian e verificado por leitura de volta."
                    else
                        "Escolha onde salvar. O Guardian verifica o arquivo por leitura de volta.",
                    13f,
                    false,
                    TEXT_MUTED
                )
            )

            addView(
                outlineButton("Exportar JSON do dia") {
                    exportFresh(false)
                }.apply { topMargin(12) }
            )

            addView(
                outlineButton("Exportar JSON de diagnóstico") {
                    exportFresh(true)
                }.apply { topMargin(10) }
            )
        })

        root.addView(sectionTitle("Capacidades deste build"))

        capabilityText = textView("", 13f, false, TEXT_MUTED)
        root.addView(card().apply { addView(capabilityText) })

        setContentView(scroll)
    }

    private fun refresh(runCollector: Boolean) {
        val privacyReady = PrivatePreferences(this).setupComplete()
        val usageAllowed = UsageAccess.hasPermission(this)

        setStatus(privacyReady, usageAllowed)
        permissionHelpCard.visibility = if (usageAllowed) View.GONE else View.VISIBLE

        capabilityText.text = buildString {
            appendLine("✓ Uso por aplicativo")
            appendLine("✓ Tela não interativa e desbloqueios")
            appendLine("✓ PRIVATE antes do armazenamento")
            appendLine("✓ Outro usuário/perfil vira PRIVATE")
            appendLine("✓ Navegação do sistema separada dos apps")
            appendLine("✓ Timeline sem dupla contagem")
            appendLine("✓ SQLite + JSON local")
            appendLine("✓ Exportação verificada")
            appendLine("✓ Coleta serializada")
            appendLine("— Domínios: ainda não")
            appendLine("— Guia anônima: schema pronto; detecção ainda não")
            append("— Nuvem/API externa: não existe neste build")
        }

        if (runCollector && usageAllowed) {
            statusText.text = "Atualizando eventos locais…"

            Thread {
                val result = UsageCollector(applicationContext).collect()

                runOnUiThread {
                    setStatus(privacyReady, result.permission)
                    permissionHelpCard.visibility =
                        if (result.permission) View.GONE else View.VISIBLE
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

    private fun setStatus(
        privacyReady: Boolean,
        usageAllowed: Boolean
    ) {
        statusText.text = buildString {
            append(
                if (privacyReady)
                    "✓ Revisão de apps privados concluída"
                else
                    "• Revisão de apps privados pendente"
            )
            append("\n")
            append(
                if (usageAllowed)
                    "✓ Acesso de uso autorizado"
                else
                    "• Acesso de uso pendente"
            )
        }
    }

    private fun updateSummary() {
        val summary = ReportGenerator(this).todaySummary()

        appsValue.text = durationLabel(summary.appSeconds)
        privateValue.text = durationLabel(summary.privateSeconds)
        screenValue.text = durationLabel(summary.screenOffSeconds)
        unlockValue.text = "${summary.unlockCount} desbloqueios detectados"
        systemValue.text =
            "Navegação do sistema: ${durationLabel(summary.systemSeconds)}"

        topAppsText.text =
            if (summary.topApps.isEmpty()) {
                if (UsageAccess.hasPermission(this))
                    "Ainda sem uso suficiente para mostrar os aplicativos mais usados."
                else
                    "Autorize o Acesso de uso para começar a formar o histórico local."
            } else {
                buildString {
                    appendLine("Mais usados:")
                    summary.topApps.take(5).forEach { app ->
                        appendLine(
                            "• ${app.name}: ${durationLabel(app.seconds)}"
                        )
                    }
                }.trim()
            }
    }

    private fun durationLabel(seconds: Long): String {
        if (seconds < 60L) return "${seconds}s"

        val minutes = seconds / 60L
        if (minutes < 60L) {
            val rest = seconds % 60L
            return if (rest == 0L) "${minutes}m"
            else "${minutes}m ${rest}s"
        }

        val hours = minutes / 60L
        val restMinutes = minutes % 60L
        return if (restMinutes == 0L) "${hours}h"
        else "${hours}h ${restMinutes}m"
    }

    private fun showRestrictedSettingsHelp() {
        AlertDialog.Builder(this)
            .setTitle("Liberar configuração restrita")
            .setMessage(
                "Esse bloqueio é imposto pelo Android quando um APK é instalado manualmente.\n\n" +
                    "1. Abra Informações do app.\n" +
                    "2. Fique na tela PRINCIPAL.\n" +
                    "3. Toque nos três pontos (⋮).\n" +
                    "4. Escolha “Permitir configurações restritas”.\n" +
                    "5. Volte e conceda o Acesso de uso."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Abrir Informações do app") { _, _ ->
                openAppDetails()
            }
            .show()
    }

    private fun openAppDetails() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun openUsageSettings() {
        if (!PrivatePreferences(this).setupComplete()) {
            Toast.makeText(
                this,
                "Revise primeiro quais apps devem ficar sempre PRIVATE.",
                Toast.LENGTH_LONG
            ).show()

            startActivity(
                Intent(this, PrivateAppsActivity::class.java)
            )
            return
        }

        val primary =
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }

        try {
            startActivity(primary)
        } catch (_: Exception) {
            startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            )
        }
    }

    private fun exportFresh(diagnostic: Boolean) {
        statusText.text = "Preparando exportação local…"

        Thread {
            try {
                UsageCollector(applicationContext).collect()

                val contents: String
                val filename: String

                if (diagnostic) {
                    contents =
                        DiagnosticsGenerator(applicationContext)
                            .generate()
                            .toString(2)

                    filename =
                        "guardian-diagnostico-${
                            SimpleDateFormat(
                                "yyyyMMdd-HHmmss",
                                Locale.US
                            ).format(Date())
                        }.json"
                } else {
                    contents =
                        ReportGenerator(applicationContext)
                            .todayJson()
                            .toString(2)

                    filename =
                        "guardian-dia-${
                            SimpleDateFormat(
                                "yyyy-MM-dd-HHmmss",
                                Locale.US
                            ).format(Date())
                        }.json"
                }

                if (contents.isBlank()) {
                    throw IOException("generated_payload_empty")
                }

                val storage = ExportStorage(applicationContext)

                if (storage.supportsDirectDownloads()) {
                    val saved =
                        storage.saveToDownloads(filename, contents)

                    runCatching {
                        GuardianDatabase(applicationContext).logTechnical(
                            "EXPORT_OK",
                            "bytes=${saved.bytes}"
                        )
                    }

                    runOnUiThread {
                        updateSummary()
                        showSavedDialog(saved)
                    }
                } else {
                    val pending = pendingExportFile()
                    pending.writeText(contents, Charsets.UTF_8)

                    if (pending.length() <= 0L) {
                        throw IOException("pending_export_empty")
                    }

                    getSharedPreferences(EXPORT_PREFS, MODE_PRIVATE)
                        .edit()
                        .putString(EXPORT_FILENAME, filename)
                        .apply()

                    runOnUiThread {
                        requestLegacySave(filename)
                    }
                }
            } catch (t: Throwable) {
                runCatching {
                    GuardianDatabase(applicationContext).logTechnical(
                        "EXPORT_PREPARE_ERROR",
                        t::class.java.simpleName
                    )
                }

                runOnUiThread {
                    refresh(false)

                    Toast.makeText(
                        this,
                        "Não foi possível exportar: ${
                            t.message ?: t::class.java.simpleName
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun showSavedDialog(saved: ExportStorage.SavedFile) {
        AlertDialog.Builder(this)
            .setTitle("JSON salvo e verificado")
            .setMessage(
                "${saved.bytes} bytes foram gravados e lidos de volta com sucesso.\n\n" +
                    saved.locationLabel
            )
            .setNegativeButton("Fechar", null)
            .setPositiveButton("Compartilhar") { _, _ ->
                shareUri(saved.uri)
            }
            .show()
    }

    private fun shareUri(uri: Uri) {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        try {
            startActivity(
                Intent.createChooser(send, "Compartilhar JSON")
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Nenhum aplicativo disponível para compartilhar.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestLegacySave(filename: String) {
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, filename)
            }

        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_EXPORT)
    }

    @Deprecated("Legacy Activity result API kept for Android 9 and lower")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_EXPORT) return

        if (resultCode != RESULT_OK) {
            clearPendingExport()
            return
        }

        val uri = data?.data
        val pending = pendingExportFile()

        if (
            uri == null ||
            !pending.exists() ||
            pending.length() <= 0L
        ) {
            clearPendingExport()

            Toast.makeText(
                this,
                "A exportação perdeu o arquivo temporário. Tente novamente.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            val bytes =
                ExportStorage(applicationContext).writeAndVerifyDocument(
                    uri,
                    pending.readText(Charsets.UTF_8)
                )

            runCatching {
                GuardianDatabase(applicationContext).logTechnical(
                    "EXPORT_OK",
                    "bytes=$bytes"
                )
            }

            clearPendingExport()

            showSavedDialog(
                ExportStorage.SavedFile(
                    uri,
                    bytes,
                    "Arquivo selecionado pelo usuário"
                )
            )
        } catch (t: Throwable) {
            runCatching {
                GuardianDatabase(applicationContext).logTechnical(
                    "EXPORT_WRITE_ERROR",
                    t::class.java.simpleName
                )
            }

            Toast.makeText(
                this,
                "Não foi possível gravar o JSON: ${
                    t.message ?: t::class.java.simpleName
                }",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun pendingExportFile(): File =
        File(cacheDir, "guardian-pending-export.json")

    private fun clearPendingExport() {
        runCatching { pendingExportFile().delete() }

        getSharedPreferences(EXPORT_PREFS, MODE_PRIVATE)
            .edit()
            .remove(EXPORT_FILENAME)
            .apply()
    }

    private fun applySystemBarInsets(view: View) {
        view.setOnApplyWindowInsetsListener { target, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars =
                    insets.getInsets(WindowInsets.Type.systemBars())

                target.setPadding(
                    0,
                    bars.top,
                    0,
                    bars.bottom
                )
            } else {
                @Suppress("DEPRECATION")
                target.setPadding(
                    0,
                    insets.systemWindowInsetTop,
                    0,
                    insets.systemWindowInsetBottom
                )
            }
            insets
        }

        view.requestApplyInsets()
    }

    private fun sectionTitle(value: String) =
        textView(value, 18f, true, TEXT_PRIMARY).apply {
            setPadding(0, dp(22), 0, dp(8))
        }

    private fun card(backgroundColor: Int = CARD) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(backgroundColor, 18f)

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
        }

    private fun statBlock(
        parent: LinearLayout,
        label: String
    ): TextView {
        val box =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

        val value =
            textView("0s", 21f, true, TEXT_PRIMARY).apply {
                gravity = Gravity.CENTER
            }

        box.addView(value, matchWidth())
        box.addView(
            textView(label, 11f, false, TEXT_MUTED).apply {
                gravity = Gravity.CENTER
            },
            matchWidth()
        )

        parent.addView(
            box,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        return value
    }

    private fun textView(
        value: String,
        sizeSp: Float,
        bold: Boolean,
        color: Int
    ) =
        TextView(this).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)

            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }

    private fun primaryButton(
        label: String,
        action: (View) -> Unit
    ) =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            minHeight = dp(52)
            stateListAnimator = null
            background = rounded(PRIMARY, 14f)
            setOnClickListener { action(it) }
            layoutParams = matchWidth()
        }

    private fun outlineButton(
        label: String,
        action: (View) -> Unit
    ) =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(PRIMARY)
            minHeight = dp(50)
            stateListAnimator = null
            background = rounded(CARD, 14f, PRIMARY, 1)
            setOnClickListener { action(it) }
            layoutParams = matchWidth()
        }

    private fun rounded(
        fillColor: Int,
        radiusDp: Float,
        strokeColor: Int? = null,
        strokeDp: Int = 0
    ) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius = dp(radiusDp.toInt()).toFloat()

            if (strokeColor != null && strokeDp > 0) {
                setStroke(dp(strokeDp), strokeColor)
            }
        }

    private fun matchWidth() =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun View.topMargin(valueDp: Int) {
        val lp =
            (layoutParams as? LinearLayout.LayoutParams) ?: matchWidth()
        lp.topMargin = dp(valueDp)
        layoutParams = lp
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_EXPORT = 9001
        private const val EXPORT_PREFS = "guardian_export"
        private const val EXPORT_FILENAME = "pending_filename"

        private val BACKGROUND = Color.rgb(245, 248, 251)
        private val CARD = Color.WHITE
        private val TEXT_PRIMARY = Color.rgb(24, 33, 43)
        private val TEXT_MUTED = Color.rgb(99, 115, 129)
        private val PRIMARY = Color.rgb(18, 111, 137)
        private val DIVIDER = Color.rgb(229, 234, 239)
        private val WARNING_BG = Color.rgb(255, 247, 232)
        private val WARNING_TEXT = Color.rgb(112, 75, 16)
    }
}
