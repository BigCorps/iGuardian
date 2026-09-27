package com.bigcorps.guardian.web

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.bigcorps.guardian.core.FinancialAppCatalog
import com.bigcorps.guardian.core.GuardianDatabase

class BrowserWebActivity :
    Activity() {
    private data class FinancialAppEntry(
        val packageName: String,
        val label: String
    )

    private lateinit var root: LinearLayout

    private val bankModeHandler =
        Handler(
            Looper.getMainLooper()
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        window.statusBarColor =
            BACKGROUND

        window.navigationBarColor =
            BACKGROUND

        root =
            LinearLayout(
                this
            ).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(
                        18
                    ),
                    dp(
                        22
                    ),
                    dp(
                        18
                    ),
                    dp(
                        40
                    )
                )
            }

        val scroll =
            ScrollView(
                this
            ).apply {
                setBackgroundColor(
                    BACKGROUND
                )

                addView(
                    root
                )
            }

        setContentView(
            scroll
        )

        scroll.setOnApplyWindowInsetsListener {
            _,
            insets ->
            @Suppress("DEPRECATION")
            val topInset =
                insets.systemWindowInsetTop

            @Suppress("DEPRECATION")
            val bottomInset =
                insets.systemWindowInsetBottom

            root.setPadding(
                dp(18),
                topInset + dp(18),
                dp(18),
                bottomInset + dp(32)
            )

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

        root.addView(
            text(
                "GUARDIAN WEB • BETA",
                11f,
                true,
                PRIMARY
            )
        )

        root.addView(
            text(
                "Sites, sem guardar sua URL completa.",
                25f,
                true,
                TEXT_PRIMARY
            ).apply {
                setPadding(
                    0,
                    dp(
                        5
                    ),
                    0,
                    dp(
                        8
                    )
                )
            }
        )

        root.addView(
            text(
                "O Guardian Web Hybrid tenta primeiro ler localmente a barra de endereço pela árvore de acessibilidade. Se o navegador não expuser um host utilizável, usa screenshot temporário + OCR local como fallback. Só o host sanitizado pode entrar no histórico; imagem, OCR bruto, caminho, parâmetros, buscas, título, conteúdo e texto digitado não são salvos.",
                14f,
                false,
                TEXT_MUTED
            )
        )

        val prefs =
            BrowserWebPreferences(
                this
            )

        val accessStatus =
            BrowserWebAccess.status(
                this
            )

        val enabled =
            accessStatus.enabled

        val alive =
            accessStatus.alive

        root.addView(
            card().apply {
                addView(
                    text(
                        when {
                            !prefs.consented() ->
                                "• Consentimento ainda não concedido"

                            enabled &&
                                alive ->
                                "✓ Guardian Web ativo e respondendo"

                            enabled ->
                                "• Guardian Web habilitado; aguardando o observador responder"

                            else ->
                                "• Guardian Web autorizado, mas o serviço de acessibilidade está desligado"
                        },
                        14f,
                        true,
                        if (
                            enabled
                        ) {
                            PRIMARY
                        } else {
                            WARNING_TEXT
                        }
                    )
                )

                addView(
                    text(
                        "Guardian Web Hybrid v3: árvore primeiro, OCR embarcado como fallback, tudo local e sem INTERNET. A validação física desta rodada continua focada em Chrome Dev e calibração opcional do Mi Browser.",
                        12f,
                        false,
                        TEXT_MUTED
                    ).apply {
                        setPadding(
                            0,
                            dp(
                                8
                            ),
                            0,
                            0
                        )
                    }
                )

                addView(
                    primaryButton(
                        when {
                            !prefs.consented() ->
                                "Ativar Guardian Web"

                            enabled ->
                                "Abrir configurações de acessibilidade"

                            else ->
                                "Concluir ativação na Acessibilidade"
                        }
                    ) {
                        if (
                            prefs.consented()
                        ) {
                            openAccessibility()
                        } else {
                            showDisclosure()
                        }
                    }.apply {
                        topMargin(
                            12
                        )
                    }
                )

                addView(
                    outlineButton(
                        "Ajuda: configuração restrita"
                    ) {
                        showRestrictedHelp()
                    }.apply {
                        topMargin(
                            8
                        )
                    }
                )
            }.apply {
                topMargin(
                    14
                )
            }
        )

        val bankSystemEnabled =
            accessStatus.managerReported ||
                accessStatus.secureSettingReported

        val financialApps =
            installedFinancialApps()

        root.addView(
            card().apply {
                addView(
                    text(
                        "Modo Banco",
                        15f,
                        true,
                        TEXT_PRIMARY
                    )
                )

                addView(
                    text(
                        if (
                            bankSystemEnabled
                        ) {
                            "Apps financeiros reconhecidos ficam automaticamente classificados como PRIVATE no histórico geral. Para compatibilidade bancária, use ‘Abrir com proteção’: o Guardian desliga completamente apenas o Guardian Web/Acessibilidade, confirma que ficou OFF e só então abre o banco."
                        } else {
                            "Guardian Web está desligado no Android. O monitoramento normal de apps continua funcionando; você pode abrir um banco abaixo sem nenhum serviço de Acessibilidade do Guardian ativo."
                        },
                        12f,
                        false,
                        TEXT_MUTED
                    ).apply {
                        setPadding(
                            0,
                            dp(8),
                            0,
                            0
                        )
                    }
                )

                if (
                    financialApps.isNotEmpty()
                ) {
                    addView(
                        text(
                            "Apps financeiros encontrados neste aparelho:",
                            12f,
                            true,
                            TEXT_PRIMARY
                        ).apply {
                            setPadding(
                                0,
                                dp(12),
                                0,
                                0
                            )
                        }
                    )

                    financialApps.forEach {
                        app ->
                        addView(
                            outlineButton(
                                if (
                                    bankSystemEnabled
                                ) {
                                    "Abrir ${app.label} com proteção"
                                } else {
                                    "Abrir ${app.label}"
                                }
                            ) {
                                showProtectedLaunchDisclosure(
                                    app
                                )
                            }.apply {
                                topMargin(8)
                            }
                        )
                    }
                } else {
                    addView(
                        text(
                            "Nenhum app financeiro reconhecido foi encontrado pelo launcher. O desligamento manual continua disponível abaixo.",
                            12f,
                            false,
                            TEXT_MUTED
                        ).apply {
                            setPadding(
                                0,
                                dp(12),
                                0,
                                0
                            )
                        }
                    )
                }

                if (
                    bankSystemEnabled
                ) {
                    addView(
                        outlineButton(
                            "Desligar Guardian Web sem abrir banco"
                        ) {
                            showBankModeDisclosure()
                        }.apply {
                            topMargin(12)
                        }
                    )

                    addView(
                        text(
                            "Proteção automática extra: se você abrir diretamente um app financeiro reconhecido, o Guardian tenta desligar o Guardian Web assim que o UsageStats detectar esse app em primeiro plano. Como essa detecção ocorre depois que o processo do banco começou, ‘Abrir com proteção’ continua sendo o fluxo recomendado.",
                            11f,
                            false,
                            TEXT_MUTED
                        ).apply {
                            setPadding(
                                0,
                                dp(10),
                                0,
                                0
                            )
                        }
                    )
                } else {
                    addView(
                        outlineButton(
                            "Reativar Guardian Web depois"
                        ) {
                            openAccessibility()
                        }.apply {
                            topMargin(12)
                        }
                    )
                }
            }.apply {
                topMargin(10)
            }
        )

        val health =
            buildString {
                append("Guardian Web Hybrid v3: ")
                append(
                    if (alive) {
                        "respondendo"
                    } else {
                        "sem heartbeat recente"
                    }
                )

                append(
                    "\nÁrvore: ${prefs.treeHostFoundCount()}/${prefs.treeProbeCount()} hosts • direta: ${prefs.treeDirectHostCount()} • fallback: ${prefs.treeFallbackHostCount()}"
                )

                prefs.treeLastUrlBarId()
                    ?.let {
                        append(
                            "\nÚltima barra: $it"
                        )
                    }

                append(
                    "\nScreenshots: ${prefs.visualScreenshotSuccessCount()}/${prefs.visualScreenshotRequestCount()} • falhas: ${prefs.visualScreenshotFailureCount()}"
                )

                append(
                    "\nOCR: ${prefs.visualOcrRunCount()} • hosts: ${prefs.visualOcrHostCount()} • probes privados: ${prefs.visualPrivateProbeCount()} • detecções privadas: ${prefs.visualPrivateDetectedCount()}"
                )

                if (
                    prefs.visualWindowScreenshotRequestCount() >
                    0
                ) {
                    append(
                        "\nScreenshot por janela: ${prefs.visualWindowScreenshotSuccessCount()}/${prefs.visualWindowScreenshotRequestCount()} • falhas: ${prefs.visualWindowScreenshotFailureCount()}"
                    )
                }

                if (
                    prefs.secureBrowserWindow()
                ) {
                    append(
                        "\nJanela segura observada (sinal diagnóstico; não significa incógnito por si só)."
                    )
                }

                prefs.visualLastHost()
                    ?.let {
                        append(
                            "\nÚltimo host visual: $it"
                        )
                    }

                if (
                    prefs.visualLastPrivateDetected()
                ) {
                    append(
                        " • modo anônimo"
                    )
                }

                prefs.visualLastPipelineError()
                    ?.let {
                        append(
                            "\nÚltimo erro visual: $it"
                        )
                    }

                if (
                    prefs.visualLastScreenshotError() !=
                    0
                ) {
                    append(
                        "\nCódigo da última falha de screenshot: ${prefs.visualLastScreenshotError()}"
                    )
                }
            }

        root.addView(
            card().apply {
                addView(
                    text(
                        "Diagnóstico local",
                        14f,
                        true,
                        TEXT_PRIMARY
                    )
                )

                addView(
                    text(
                        health,
                        12f,
                        false,
                        TEXT_MUTED
                    ).apply {
                        setPadding(
                            0,
                            dp(8),
                            0,
                            0
                        )
                    }
                )

                addView(
                    outlineButton(
                        "Atualizar diagnóstico"
                    ) {
                        render()
                    }.apply {
                        topMargin(10)
                    }
                )
            }.apply {
                topMargin(10)
            }
        )

        val today =
            BrowserReport.todayJson(
                this
            )

        val total =
            today.optLong(
                "total_milliseconds",
                0L
            )

        val anonymous =
            today.optLong(
                "anonymous_milliseconds",
                0L
            )

        val domains =
            today.optJSONArray(
                "domains"
            )

        root.addView(
            text(
                "Hoje",
                18f,
                true,
                TEXT_PRIMARY
            ).apply {
                setPadding(
                    0,
                    dp(
                        22
                    ),
                    0,
                    dp(
                        8
                    )
                )
            }
        )

        root.addView(
            card().apply {
                addView(
                    text(
                        "Sites detectados: ${
                            durationLabel(
                                total
                            )
                        } • anônimo: ${
                            durationLabel(
                                anonymous
                            )
                        }",
                        15f,
                        true,
                        TEXT_PRIMARY
                    )
                )

                val detail =
                    if (
                        domains ==
                        null ||
                        domains.length() ==
                        0
                    ) {
                        "Ainda não há domínios registrados. Depois de ativar, abra um site no Chrome Dev por alguns segundos."
                    } else {
                        buildString {
                            appendLine(
                                "Mais usados:"
                            )

                            for (
                                index in
                                0 until minOf(
                                    10,
                                    domains.length()
                                )
                            ) {
                                val item =
                                    domains.getJSONObject(
                                        index
                                    )

                                append(
                                    "• ${
                                        item.optString(
                                            "host"
                                        )
                                    }: ${
                                        durationLabel(
                                            item.optLong(
                                                "foreground_milliseconds",
                                                0L
                                            )
                                        )
                                    }"
                                )

                                val privateMs =
                                    item.optLong(
                                        "anonymous_milliseconds",
                                        0L
                                    )

                                if (
                                    privateMs >
                                    0L
                                ) {
                                    append(
                                        " • anônimo ${
                                            durationLabel(
                                                privateMs
                                            )
                                        }"
                                    )
                                }

                                if (
                                    index <
                                    minOf(
                                        10,
                                        domains.length()
                                    ) -
                                    1
                                ) {
                                    appendLine()
                                }
                            }
                        }
                    }

                addView(
                    text(
                        detail,
                        13f,
                        false,
                        TEXT_MUTED
                    ).apply {
                        setPadding(
                            0,
                            dp(
                                10
                            ),
                            0,
                            0
                        )
                    }
                )

                addView(
                    outlineButton(
                        "Limpar histórico de sites"
                    ) {
                        confirmClear()
                    }.apply {
                        topMargin(
                            12
                        )
                    }
                )
            }
        )

        root.addView(
            text(
                "Teste desta rodada",
                18f,
                true,
                TEXT_PRIMARY
            ).apply {
                setPadding(
                    0,
                    dp(
                        22
                    ),
                    0,
                    dp(
                        8
                    )
                )
            }
        )

        root.addView(
            card().apply {
                addView(
                    primaryButton(
                        "Iniciar teste limpo"
                    ) {
                        confirmStartCleanTest()
                    }
                )

                addView(
                    text(
                        "Use o botão acima antes desta rodada. Ele apaga somente histórico/telemetria web e reseta o host/incógnito mantidos em memória; o histórico geral dos aplicativos não é apagado.\n\n1. Confirme que Guardian Web/Acessibilidade estão ativos.\n2. Chrome Dev normal: uol.com.br por ~20 s e globo.com por ~20 s.\n3. Abra uma nova guia anônima e permaneça ~8 s na tela inicial antes de navegar.\n4. Ainda anônimo, visite github.com por ~20 s.\n5. Volte aqui: a lista deve conter apenas os sites desta rodada e ‘anônimo’ deve ser maior que 0 s.\n6. Teste banco: use ‘Abrir Inter Empresas com proteção’. Depois, se quiser testar o failsafe automático, reative Guardian Web e abra o Inter diretamente uma única vez.\n7. Exporte um novo pacote de validação.",
                        13f,
                        false,
                        TEXT_MUTED
                    ).apply {
                        setPadding(
                            0,
                            dp(12),
                            0,
                            0
                        )
                    }
                )
            }
        )
    }

    private fun showBankModeDisclosure() {
        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Ativar Modo Banco"
            )
            .setMessage(
                "O Guardian Web será realmente desligado na Acessibilidade antes de você abrir o banco. O Guardian continuará registrando normalmente o tempo de uso dos aplicativos pelo Usage Access.\n\n" +
                    "Aguarde a confirmação de que o Guardian Web ficou desligado antes de abrir o app financeiro. Depois, para voltar a registrar sites, será necessário reativar o Guardian Web manualmente nas Configurações de Acessibilidade."
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Desligar Guardian Web"
            ) {
                _,
                _ ->
                activateBankMode()
            }
            .show()
    }

    private fun activateBankMode() {
        ensureBankModeDisabled {
            Toast.makeText(
                this,
                "Modo Banco ativo: Guardian Web desligado. Agora você pode abrir seu banco.",
                Toast.LENGTH_SHORT
            ).show()

            render()
        }
    }

    private fun showProtectedLaunchDisclosure(
        app: FinancialAppEntry
    ) {
        val status =
            BrowserWebAccess.status(
                this
            )

        if (
            !status.managerReported &&
            !status.secureSettingReported
        ) {
            launchFinancialApp(
                app
            )
            return
        }

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Abrir ${app.label} com proteção"
            )
            .setMessage(
                "O Guardian Web será desligado de verdade na Acessibilidade. O app ${app.label} só será aberto depois que o Android confirmar que o serviço ficou OFF. O monitoramento normal de tempo de uso continua pelo Usage Access."
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Desligar e abrir"
            ) {
                _,
                _ ->
                openFinancialWithProtection(
                    app
                )
            }
            .show()
    }

    private fun openFinancialWithProtection(
        app: FinancialAppEntry
    ) {
        ensureBankModeDisabled {
            launchFinancialApp(
                app
            )
        }
    }

    private fun ensureBankModeDisabled(
        onReady: () -> Unit
    ) {
        val before =
            BrowserWebAccess.status(
                this
            )

        val alreadyOff =
            !before.managerReported &&
                !before.secureSettingReported

        if (
            alreadyOff
        ) {
            onReady()
            return
        }

        val requested =
            BrowserAccessibilityService
                .requestBankModeDisable()

        if (
            !requested
        ) {
            Toast.makeText(
                this,
                "Não foi possível desligar automaticamente. Desative Guardian Web na Acessibilidade antes de abrir o banco.",
                Toast.LENGTH_SHORT
            ).show()
            openAccessibility()
            return
        }

        verifyBankModeDisabled(
            attempt = 0,
            onReady = onReady
        )
    }

    private fun verifyBankModeDisabled(
        attempt: Int,
        onReady: () -> Unit
    ) {
        bankModeHandler.postDelayed(
            {
                val status =
                    BrowserWebAccess.status(
                        this
                    )

                val systemOff =
                    !status.managerReported &&
                        !status.secureSettingReported

                if (
                    systemOff
                ) {
                    onReady()
                } else if (
                    attempt <
                    BANK_MODE_MAX_VERIFY_ATTEMPTS
                ) {
                    verifyBankModeDisabled(
                        attempt =
                            attempt +
                                1,
                        onReady =
                            onReady
                    )
                } else {
                    Toast.makeText(
                        this,
                        "Confirme manualmente que Guardian Web está desligado antes de abrir o banco.",
                        Toast.LENGTH_SHORT
                    ).show()
                    openAccessibility()
                }
            },
            BANK_MODE_VERIFY_INTERVAL_MS
        )
    }

    private fun installedFinancialApps():
        List<FinancialAppEntry> {
        val launcherIntent =
            Intent(
                Intent.ACTION_MAIN
            ).apply {
                addCategory(
                    Intent.CATEGORY_LAUNCHER
                )
            }

        @Suppress("DEPRECATION")
        val resolved =
            packageManager.queryIntentActivities(
                launcherIntent,
                0
            )

        return resolved
            .mapNotNull {
                info ->
                val packageName =
                    info.activityInfo
                        ?.packageName
                        ?.trim()
                        .orEmpty()

                if (
                    packageName.isBlank() ||
                    packageName ==
                        this.packageName
                ) {
                    return@mapNotNull null
                }

                val label =
                    runCatching {
                        info.loadLabel(
                            packageManager
                        )
                            ?.toString()
                            ?.trim()
                    }.getOrNull()
                        .orEmpty()
                        .ifBlank {
                            packageName
                        }

                if (
                    !FinancialAppCatalog.isFinancial(
                        packageName,
                        label
                    )
                ) {
                    return@mapNotNull null
                }

                FinancialAppEntry(
                    packageName =
                        packageName,
                    label =
                        label
                )
            }
            .distinctBy {
                it.packageName
            }
            .sortedBy {
                it.label.lowercase()
            }
    }

    private fun launchFinancialApp(
        app: FinancialAppEntry
    ) {
        val direct =
            packageManager.getLaunchIntentForPackage(
                app.packageName
            )

        val fallback =
            Intent(
                Intent.ACTION_MAIN
            ).apply {
                addCategory(
                    Intent.CATEGORY_LAUNCHER
                )
                setPackage(
                    app.packageName
                )
            }.takeIf {
                it.resolveActivity(
                    packageManager
                ) !=
                    null
            }

        val launch =
            direct
                ?: fallback

        if (
            launch ==
            null
        ) {
            Toast.makeText(
                this,
                "Não foi possível abrir ${app.label}. Guardian Web já está desligado; abra o banco manualmente.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        startActivity(
            launch
        )
    }

    private fun confirmStartCleanTest() {
        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Iniciar teste limpo?"
            )
            .setMessage(
                "Isso apaga somente os hosts/tempos e a telemetria do Guardian Web desta instalação. O histórico normal dos aplicativos não será apagado."
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Limpar e iniciar"
            ) {
                _,
                _ ->
                startCleanValidationTest()
            }
            .show()
    }

    private fun startCleanValidationTest() {
        val requested =
            BrowserAccessibilityService
                .requestValidationReset()

        if (
            !requested
        ) {
            GuardianDatabase(
                applicationContext
            ).clearBrowserSessions()

            BrowserWebPreferences(
                applicationContext
            ).apply {
                clearRuntimeEvidence()
                markTrackingStartedIfMissing()
            }

            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "WEB_TEST_RESET"
            )
        }

        Toast.makeText(
            this,
            "Teste web zerado. Comece agora pelo Chrome Dev normal.",
            Toast.LENGTH_SHORT
        ).show()

        bankModeHandler.postDelayed(
            {
                render()
            },
            CLEAN_TEST_RENDER_DELAY_MS
        )
    }

    private fun showDisclosure() {
        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Ativar Guardian Web"
            )
            .setMessage(
                "Nesta versão, o Guardian Web usa a Acessibilidade somente em navegadores compatíveis. Primeiro tenta ler a barra de endereço pela árvore do navegador e reduzir o endereço ao host. Se a árvore não fornecer um host utilizável, pode solicitar uma captura temporária da janela/tela para OCR local.\n\n" +
                    "O screenshot existe apenas em memória pelo tempo necessário ao processamento e nunca é salvo, exportado ou enviado.\n\n" +
                    "O OCR é embarcado no aplicativo e funciona sem INTERNET. O texto OCR bruto também nunca é persistido.\n\n" +
                    "ANTES DE SALVAR, qualquer endereço reconhecido é reduzido ao host. Ex.: https://www.google.com/search?q=segredo vira google.com.\n\n" +
                    "Não são armazenados: screenshot, OCR bruto, texto da árvore, caminho, parâmetros, pesquisas, fragmentos, título da página, conteúdo, texto digitado ou senhas.\n\n" +
                    "Para diagnóstico de compatibilidade, o Guardian DEV pode registrar apenas os identificadores técnicos dos elementos da árvore (resource IDs), nunca o texto desses elementos.\n\n" +
                    "Para detectar navegação anônima, o Guardian combina indicadores da árvore, activity/class e texto visual explícito quando disponível. Uma janela protegida contra screenshot é apenas um sinal diagnóstico e não é tratada sozinha como prova de modo anônimo.\n\n" +
                    "Tudo permanece neste aparelho e o Guardian continua sem permissão de INTERNET."
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Concordo e continuar"
            ) {
                _,
                _ ->
                BrowserWebPreferences(
                    this
                ).grantConsent()

                openAccessibility()
            }
            .show()
    }

    private fun showRestrictedHelp() {
        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Se o Android bloquear a ativação"
            )
            .setMessage(
                "Em APK instalado manualmente, Android 13+ pode bloquear a Acessibilidade como configuração restrita.\n\n" +
                    "1. Abra Informações do app Guardian.\n" +
                    "2. Na tela principal, toque em ⋮.\n" +
                    "3. Escolha “Permitir configurações restritas”.\n" +
                    "4. Volte para Acessibilidade e ative Guardian Web."
            )
            .setNegativeButton(
                "Fechar",
                null
            )
            .setPositiveButton(
                "Abrir Informações do app"
            ) {
                _,
                _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse(
                            "package:$packageName"
                        )
                    )
                )
            }
            .show()
    }

    private fun openAccessibility() {
        startActivity(
            Intent(
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            )
        )
    }

    private fun confirmClear() {
        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Limpar histórico de sites?"
            )
            .setMessage(
                "Apaga somente os hosts e tempos coletados pelo Guardian Web. O histórico de apps do Guardian não é afetado."
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Limpar"
            ) {
                _,
                _ ->
                GuardianDatabase(
                    applicationContext
                ).clearBrowserSessions()

                BrowserWebPreferences(
                    applicationContext
                ).clearRuntimeEvidence()

                Toast.makeText(
                    this,
                    "Histórico web local limpo.",
                    Toast.LENGTH_SHORT
                ).show()

                render()
            }
            .show()
    }

    private fun text(
        value: String,
        sizeSp: Float,
        bold: Boolean,
        color: Int
    ) =
        TextView(
            this
        ).apply {
            text =
                value

            textSize =
                sizeSp

            setTextColor(
                color
            )

            if (
                bold
            ) {
                setTypeface(
                    typeface,
                    Typeface.BOLD
                )
            }
        }

    private fun card() =
        LinearLayout(
            this
        ).apply {
            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(
                    16
                ),
                dp(
                    16
                ),
                dp(
                    16
                ),
                dp(
                    16
                )
            )

            background =
                rounded(
                    CARD,
                    18f
                )

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin =
                        dp(
                            10
                        )
                }
        }

    private fun primaryButton(
        label: String,
        action: (View) -> Unit
    ) =
        Button(
            this
        ).apply {
            text =
                label

            isAllCaps =
                false

            textSize =
                14f

            setTextColor(
                Color.WHITE
            )

            minHeight =
                dp(
                    52
                )

            stateListAnimator =
                null

            background =
                rounded(
                    PRIMARY,
                    14f
                )

            setOnClickListener {
                action(
                    it
                )
            }

            layoutParams =
                matchWidth()
        }

    private fun outlineButton(
        label: String,
        action: (View) -> Unit
    ) =
        Button(
            this
        ).apply {
            text =
                label

            isAllCaps =
                false

            textSize =
                14f

            setTextColor(
                PRIMARY
            )

            minHeight =
                dp(
                    50
                )

            stateListAnimator =
                null

            background =
                rounded(
                    CARD,
                    14f,
                    PRIMARY,
                    1
                )

            setOnClickListener {
                action(
                    it
                )
            }

            layoutParams =
                matchWidth()
        }

    private fun rounded(
        fillColor: Int,
        radiusDp: Float,
        strokeColor: Int? =
            null,
        strokeDp: Int =
            0
    ) =
        GradientDrawable().apply {
            shape =
                GradientDrawable.RECTANGLE

            setColor(
                fillColor
            )

            cornerRadius =
                dp(
                    radiusDp.toInt()
                ).toFloat()

            if (
                strokeColor !=
                null &&
                strokeDp >
                0
            ) {
                setStroke(
                    dp(
                        strokeDp
                    ),
                    strokeColor
                )
            }
        }

    private fun matchWidth() =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun View.topMargin(
        valueDp: Int
    ) {
        val lp =
            (
                layoutParams as?
                    LinearLayout.LayoutParams
                )
                ?: matchWidth()

        lp.topMargin =
            dp(
                valueDp
            )

        layoutParams =
            lp
    }

    private fun durationLabel(
        milliseconds: Long
    ): String {
        val seconds =
            milliseconds /
                1000L

        if (
            seconds <
            60L
        ) {
            return "${seconds}s"
        }

        val minutes =
            seconds /
                60L

        if (
            minutes <
            60L
        ) {
            return "${minutes}m"
        }

        val hours =
            minutes /
                60L

        val rest =
            minutes %
                60L

        return if (
            rest ==
            0L
        ) {
            "${hours}h"
        } else {
            "${hours}h ${rest}m"
        }
    }

    private fun dp(
        value: Int
    ) =
        (
            value *
                resources.displayMetrics.density
            )
            .toInt()

    companion object {
        private const val BANK_MODE_VERIFY_INTERVAL_MS =
            250L

        private const val CLEAN_TEST_RENDER_DELAY_MS =
            900L

        private const val BANK_MODE_MAX_VERIFY_ATTEMPTS =
            8

        private val BACKGROUND =
            Color.rgb(
                245,
                248,
                251
            )

        private val CARD =
            Color.WHITE

        private val TEXT_PRIMARY =
            Color.rgb(
                24,
                33,
                43
            )

        private val TEXT_MUTED =
            Color.rgb(
                99,
                115,
                129
            )

        private val PRIMARY =
            Color.rgb(
                18,
                111,
                137
            )

        private val WARNING_TEXT =
            Color.rgb(
                112,
                75,
                16
            )
    }
}
