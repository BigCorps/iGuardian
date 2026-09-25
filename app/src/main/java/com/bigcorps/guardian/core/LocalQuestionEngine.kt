package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

enum class LocalQuestionIntent {
    SUMMARY_TODAY,
    TOP_APP_TODAY,
    TOP_APPS_TODAY,
    APP_USAGE_TODAY,
    SCREEN_OFF_TODAY,
    UNLOCKS_TODAY,
    PRIVATE_TODAY,
    SYSTEM_TODAY,
    COVERAGE_TODAY,
    UNSUPPORTED_PERIOD,
    HELP
}

object LocalQuestionIntentParser {
    fun normalize(
        value: String
    ): String {
        val noAccents =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFD
            ).replace(
                Regex("\\p{M}+"),
                ""
            )

        return noAccents
            .lowercase(
                Locale.ROOT
            )
            .replace(
                Regex("[^a-z0-9]+"),
                " "
            )
            .trim()
    }

    fun parse(
        raw: String
    ): LocalQuestionIntent {
        val q =
            normalize(raw)

        if (q.isBlank()) {
            return LocalQuestionIntent.HELP
        }

        if (
            listOf(
                "ontem",
                "semana",
                "mes passado",
                "ultimo mes",
                "ultimos 7",
                "ultimas 24"
            ).any {
                q.contains(it)
            }
        ) {
            return LocalQuestionIntent.UNSUPPORTED_PERIOD
        }

        if (
            q.contains("top 5") ||
            q.contains("top cinco") ||
            q.contains("mais usados") ||
            q.contains("apps mais usados")
        ) {
            return LocalQuestionIntent.TOP_APPS_TODAY
        }

        if (
            q.contains("qual app") &&
            q.contains("mais")
        ) {
            return LocalQuestionIntent.TOP_APP_TODAY
        }

        if (
            q.contains("mais usei") &&
            (
                q.contains("app") ||
                q.contains("aplicativo")
                )
        ) {
            return LocalQuestionIntent.TOP_APP_TODAY
        }

        if (
            q.contains("desbloq")
        ) {
            return LocalQuestionIntent.UNLOCKS_TODAY
        }

        if (
            q.contains("tela desligada") ||
            q.contains("screen off")
        ) {
            return LocalQuestionIntent.SCREEN_OFF_TODAY
        }

        if (
            q.contains("private") ||
            q.contains("privado")
        ) {
            return LocalQuestionIntent.PRIVATE_TODAY
        }

        if (
            q.contains("navegacao do sistema") ||
            q.contains("tempo do sistema")
        ) {
            return LocalQuestionIntent.SYSTEM_TODAY
        }

        if (
            q.contains("cobertura") ||
            q.contains("quanto foi registrado") ||
            q.contains("dados confiaveis")
        ) {
            return LocalQuestionIntent.COVERAGE_TODAY
        }

        if (
            q.contains("tempo em apps") ||
            q.contains("tempo de uso") ||
            q.contains("quanto usei o celular") ||
            q.contains("quanto usei os apps")
        ) {
            return LocalQuestionIntent.APP_USAGE_TODAY
        }

        if (
            q.contains("resumo") ||
            q == "hoje" ||
            q.contains("como foi meu dia") ||
            q.contains("o que fiz hoje")
        ) {
            return LocalQuestionIntent.SUMMARY_TODAY
        }

        return LocalQuestionIntent.HELP
    }
}

data class LocalQuestionAnswer(
    val intent: LocalQuestionIntent,
    val text: String
)

class LocalQuestionEngine(
    private val context: Context
) {
    fun answer(
        question: String
    ): LocalQuestionAnswer {
        val report =
            ReportGenerator(context)
                .todayJson()

        val summary =
            report.getJSONObject(
                "summary"
            )

        val tracking =
            report.getJSONObject(
                "tracking"
            )

        val apps =
            report.getJSONArray(
                "apps"
            )

        val normalized =
            LocalQuestionIntentParser
                .normalize(
                    question
                )

        val matchingApp =
            findNamedApp(
                normalized,
                apps
            )

        val asksForTime =
            listOf(
                "quanto",
                "tempo",
                "usei",
                "usei o",
                "usei a"
            ).any {
                normalized.contains(it)
            }

        if (
            matchingApp != null &&
            asksForTime
        ) {
            val seconds =
                matchingApp
                    .getLong(
                        "foreground_seconds"
                    )

            val name =
                matchingApp
                    .getString(
                        "name"
                    )

            return LocalQuestionAnswer(
                LocalQuestionIntent
                    .APP_USAGE_TODAY,
                "Hoje, $name ficou registrado por ${
                    durationLabel(seconds)
                } em primeiro plano."
            )
        }

        return when (
            val intent =
                LocalQuestionIntentParser
                    .parse(
                        question
                    )
        ) {
            LocalQuestionIntent
                .SUMMARY_TODAY ->
                summaryAnswer(
                    summary,
                    tracking,
                    apps
                )

            LocalQuestionIntent
                .TOP_APP_TODAY ->
                topAppAnswer(
                    apps
                )

            LocalQuestionIntent
                .TOP_APPS_TODAY ->
                topAppsAnswer(
                    apps
                )

            LocalQuestionIntent
                .APP_USAGE_TODAY ->
                LocalQuestionAnswer(
                    intent,
                    "Hoje há ${
                        durationLabel(
                            summary.getLong(
                                "app_usage_seconds"
                            )
                        )
                    } de uso registrado em aplicativos."
                )

            LocalQuestionIntent
                .SCREEN_OFF_TODAY ->
                LocalQuestionAnswer(
                    intent,
                    "Hoje a tela ficou desligada por ${
                        durationLabel(
                            summary.getLong(
                                "screen_off_seconds"
                            )
                        )
                    } no período acompanhado."
                )

            LocalQuestionIntent
                .UNLOCKS_TODAY ->
                LocalQuestionAnswer(
                    intent,
                    "Hoje foram detectados ${
                        summary.getInt(
                            "unlock_count"
                        )
                    } desbloqueios."
                )

            LocalQuestionIntent
                .PRIVATE_TODAY ->
                LocalQuestionAnswer(
                    intent,
                    "Hoje houve ${
                        durationLabel(
                            summary.getLong(
                                "private_seconds"
                            )
                        )
                    } classificados como PRIVATE. O Guardian não guarda qual app ou motivo gerou esses períodos."
                )

            LocalQuestionIntent
                .SYSTEM_TODAY ->
                LocalQuestionAnswer(
                    intent,
                    "Hoje a navegação técnica do sistema somou ${
                        durationLabel(
                            summary.getLong(
                                "system_seconds"
                            )
                        )
                    }. Ela não entra no ranking de aplicativos."
                )

            LocalQuestionIntent
                .COVERAGE_TODAY ->
                LocalQuestionAnswer(
                    intent,
                    "A cobertura registrada hoje está em ${
                        coverageLabel(
                            tracking.getDouble(
                                "coverage_percent"
                            )
                        )
                    }, com ${
                        tracking.getLong(
                            "unclassified_seconds"
                        )
                    } segundos ainda não classificados."
                )

            LocalQuestionIntent
                .UNSUPPORTED_PERIOD ->
                LocalQuestionAnswer(
                    intent,
                    "Nesta versão eu respondo apenas sobre hoje. Ontem, semana e períodos personalizados entram nas próximas etapas."
                )

            LocalQuestionIntent
                .HELP ->
                LocalQuestionAnswer(
                    intent,
                    helpText(
                        matchingApp == null
                    )
                )
        }
    }

    private fun summaryAnswer(
        summary: JSONObject,
        tracking: JSONObject,
        apps: JSONArray
    ): LocalQuestionAnswer {
        val top =
            if (
                apps.length() > 0
            ) {
                val first =
                    apps.getJSONObject(0)

                "${
                    first.getString(
                        "name"
                    )
                } (${
                    durationLabel(
                        first.getLong(
                            "foreground_seconds"
                        )
                    )
                })"
            } else {
                "nenhum app com tempo suficiente"
            }

        return LocalQuestionAnswer(
            LocalQuestionIntent
                .SUMMARY_TODAY,
            "Hoje: ${
                durationLabel(
                    summary.getLong(
                        "app_usage_seconds"
                    )
                )
            } em apps, ${
                durationLabel(
                    summary.getLong(
                        "screen_off_seconds"
                    )
                )
            } de tela desligada, ${
                durationLabel(
                    summary.getLong(
                        "private_seconds"
                    )
                )
            } em PRIVATE e ${
                summary.getInt(
                    "unlock_count"
                )
            } desbloqueios. Mais usado: $top. Cobertura ${
                coverageLabel(
                    tracking.getDouble(
                        "coverage_percent"
                    )
                )
            }."
        )
    }

    private fun topAppAnswer(
        apps: JSONArray
    ): LocalQuestionAnswer {
        if (
            apps.length() == 0
        ) {
            return LocalQuestionAnswer(
                LocalQuestionIntent
                    .TOP_APP_TODAY,
                "Ainda não há tempo suficiente de aplicativos registrado hoje."
            )
        }

        val first =
            apps.getJSONObject(0)

        return LocalQuestionAnswer(
            LocalQuestionIntent
                .TOP_APP_TODAY,
            "O app mais usado hoje é ${
                first.getString(
                    "name"
                )
            }, com ${
                durationLabel(
                    first.getLong(
                        "foreground_seconds"
                    )
                )
            }."
        )
    }

    private fun topAppsAnswer(
        apps: JSONArray
    ): LocalQuestionAnswer {
        if (
            apps.length() == 0
        ) {
            return LocalQuestionAnswer(
                LocalQuestionIntent
                    .TOP_APPS_TODAY,
                "Ainda não há aplicativos com tempo suficiente registrado hoje."
            )
        }

        val lines =
            mutableListOf<String>()

        val count =
            minOf(
                5,
                apps.length()
            )

        for (
            index in 0 until count
        ) {
            val item =
                apps.getJSONObject(
                    index
                )

            lines +=
                "${index + 1}. ${
                    item.getString(
                        "name"
                    )
                } — ${
                    durationLabel(
                        item.getLong(
                            "foreground_seconds"
                        )
                    )
                }"
        }

        return LocalQuestionAnswer(
            LocalQuestionIntent
                .TOP_APPS_TODAY,
            "Mais usados hoje:\n" +
                lines.joinToString(
                    "\n"
                )
        )
    }

    private fun findNamedApp(
        normalizedQuestion: String,
        apps: JSONArray
    ): JSONObject? {
        val candidates =
            mutableListOf<
                Pair<String, JSONObject>
                >()

        for (
            index in 0 until apps.length()
        ) {
            val item =
                apps.getJSONObject(
                    index
                )

            val label =
                LocalQuestionIntentParser
                    .normalize(
                        item.getString(
                            "name"
                        )
                    )

            if (
                label.isNotBlank()
            ) {
                candidates +=
                    label to item
            }
        }

        return candidates
            .sortedByDescending {
                it.first.length
            }
            .firstOrNull {
                normalizedQuestion
                    .contains(
                        it.first
                    )
            }
            ?.second
    }

    private fun helpText(
        noNamedMatch: Boolean
    ): String {
        val base =
            "Tente perguntar: “Qual app mais usei hoje?”, “Top 5 apps”, “Quanto tempo usei o WhatsApp?”, “Quantas vezes desbloqueei?”, “Quanto tempo a tela ficou desligada?”, “Quanto tempo ficou PRIVATE?” ou “Qual a cobertura de hoje?”"

        return if (
            noNamedMatch
        ) {
            "$base. Perguntas não são salvas."
        } else {
            base
        }
    }

    private fun durationLabel(
        seconds: Long
    ): String {
        if (
            seconds < 60L
        ) {
            return "${seconds}s"
        }

        val minutes =
            seconds / 60L

        val remainingSeconds =
            seconds % 60L

        if (
            minutes < 60L
        ) {
            return if (
                remainingSeconds == 0L
            ) {
                "${minutes}m"
            } else {
                "${minutes}m ${remainingSeconds}s"
            }
        }

        val hours =
            minutes / 60L

        val remainingMinutes =
            minutes % 60L

        return if (
            remainingMinutes == 0L
        ) {
            "${hours}h"
        } else {
            "${hours}h ${remainingMinutes}m"
        }
    }

    private fun coverageLabel(
        value: Double
    ): String =
        String.format(
            Locale("pt", "BR"),
            "%.1f%%",
            value
        )
}
