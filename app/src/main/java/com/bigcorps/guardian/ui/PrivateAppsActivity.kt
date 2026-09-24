package com.bigcorps.guardian.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.bigcorps.guardian.core.PrivacyClassifier
import com.bigcorps.guardian.core.PrivatePreferences

class PrivateAppsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BACKGROUND
        window.navigationBarColor = BACKGROUND
        render()
    }

    private fun render() {
        val prefs = PrivatePreferences(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(34))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(BACKGROUND)
            clipToPadding = false
            addView(root)
        }

        applySystemBarInsets(scroll)

        root.addView(text("PRIVACIDADE", 11f, true, PRIMARY))
        root.addView(
            text(
                "Apps que devem ficar PRIVATE",
                25f,
                true,
                TEXT_PRIMARY
            ).apply {
                setPadding(0, dp(4), 0, 0)
            }
        )

        root.addView(
            text(
                "A proteção automática agora considera o pacote E o nome visível do app. Você também pode proteger qualquer outro app manualmente.",
                13f,
                false,
                TEXT_MUTED
            ).apply {
                setPadding(0, dp(6), 0, dp(14))
            }
        )

        root.addView(card().apply {
            addView(
                text(
                    "Configurações, bancos, carteiras, autenticadores e gerenciadores de senha reconhecidos entram como PRIVATE antes do SQLite.",
                    13f,
                    false,
                    TEXT_MUTED
                )
            )
        })

        val launcherIntent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)

        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(
                launcherIntent,
                0
            )
        }

        val apps = resolved
            .map { info ->
                Triple(
                    info.activityInfo.packageName,
                    info.loadLabel(packageManager).toString(),
                    info.activityInfo.name
                )
            }
            .distinctBy { it.first }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }

        apps.forEach { (pkg, label, _) ->
            val automatic =
                PrivacyClassifier.isAutomaticallyPrivate(pkg, label)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(12),
                    dp(7),
                    dp(12),
                    dp(7)
                )
                background = rounded(Color.WHITE, 14f)
            }

            val box = CheckBox(this).apply {
                text = label
                isChecked = automatic || prefs.isPrivate(pkg)
                isEnabled = !automatic
                textSize = 15f
                setTextColor(TEXT_PRIMARY)
                buttonTintList =
                    android.content.res.ColorStateList.valueOf(PRIMARY)

                setOnCheckedChangeListener { _, checked ->
                    if (!automatic) {
                        prefs.setPrivate(pkg, checked)
                    }
                }
            }

            row.addView(box)

            if (automatic) {
                row.addView(
                    text(
                        "Protegido automaticamente",
                        11f,
                        true,
                        PRIMARY
                    ).apply {
                        setPadding(
                            dp(42),
                            0,
                            0,
                            dp(4)
                        )
                    }
                )
            }

            root.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(7)
                }
            )
        }

        val finishButton = Button(this).apply {
            text = "Concluir revisão de privacidade"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            minHeight = dp(54)
            stateListAnimator = null
            background = rounded(PRIMARY, 14f)

            setOnClickListener {
                prefs.markSetupComplete()

                Toast.makeText(
                    this@PrivateAppsActivity,
                    "Revisão concluída.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }

        root.addView(
            finishButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        setContentView(scroll)
    }

    private fun applySystemBarInsets(view: View) {
        view.setOnApplyWindowInsetsListener { target, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars =
                    insets.getInsets(
                        WindowInsets.Type.systemBars()
                    )

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

    private fun card(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
            )
            background = rounded(Color.WHITE, 16f)

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
        }

    private fun text(
        value: String,
        size: Float,
        bold: Boolean,
        color: Int
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                setTypeface(
                    typeface,
                    Typeface.BOLD
                )
            }
        }

    private fun rounded(
        fillColor: Int,
        radiusDp: Float
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            cornerRadius =
                dp(radiusDp.toInt()).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private val BACKGROUND =
            Color.rgb(245, 248, 251)

        private val TEXT_PRIMARY =
            Color.rgb(24, 33, 43)

        private val TEXT_MUTED =
            Color.rgb(99, 115, 129)

        private val PRIMARY =
            Color.rgb(18, 111, 137)
    }
}
