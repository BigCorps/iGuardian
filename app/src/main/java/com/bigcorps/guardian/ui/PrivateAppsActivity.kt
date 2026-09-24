package com.bigcorps.guardian.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
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
        title = "Apps privados"
        render()
    }

    private fun render() {
        val prefs = PrivatePreferences(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(30))
        }
        root.addView(TextView(this).apply {
            text = "Apps privados"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Apps protegidos viram apenas PRIVATE. Os protegidos automaticamente ficam bloqueados nesta tela; os demais podem ser marcados por você. A seleção fica somente neste aparelho."
            textSize = 14f
            setPadding(0, dp(6), 0, dp(14))
        })

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (android.os.Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
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
            val automatic = PrivacyClassifier.isAutomaticallyPrivate(pkg)
            val box = CheckBox(this).apply {
                text = if (automatic) "$label — protegido automaticamente" else label
                isChecked = automatic || prefs.isPrivate(pkg)
                isEnabled = !automatic
                textSize = 15f
                setPadding(0, dp(3), 0, dp(3))
                setOnCheckedChangeListener { _, checked ->
                    if (!automatic) prefs.setPrivate(pkg, checked)
                }
            }
            root.addView(box)
        }

        root.addView(Button(this).apply {
            text = "Concluir revisão de privacidade"
            isAllCaps = false
            setPadding(0, dp(8), 0, dp(8))
            setOnClickListener {
                prefs.markSetupComplete()
                Toast.makeText(this@PrivateAppsActivity, "Revisão concluída. Você já pode conceder o Acesso de uso.", Toast.LENGTH_LONG).show()
                finish()
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
