package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ValidationLineage {
    const val SCHEMA =
        1

    private const val ASSET_NAME =
        "validation-contracts.json"

    fun manifest(
        context: Context
    ): JSONObject {
        val text =
            context.assets
                .open(
                    ASSET_NAME
                )
                .bufferedReader(
                    Charsets.UTF_8
                )
                .use {
                    it.readText()
                }

        return JSONObject(
            text
        )
    }

    fun snapshot(
        context: Context
    ): JSONObject {
        val source =
            manifest(
                context
            )

        val contracts =
            source.getJSONArray(
                "contracts"
            )

        val compactContracts =
            JSONArray()

        for (
            index in
            0 until contracts.length()
        ) {
            val item =
                contracts.getJSONObject(
                    index
                )

            compactContracts.put(
                JSONObject().apply {
                    put(
                        "id",
                        item.getString(
                            "id"
                        )
                    )
                    put(
                        "validation_mode",
                        item.getString(
                            "validation_mode"
                        )
                    )
                    put(
                        "validated_in",
                        item.getString(
                            "validated_in"
                        )
                    )
                    put(
                        "manual_retest_required",
                        item.optBoolean(
                            "manual_retest_required",
                            false
                        )
                    )
                    put(
                        "continuous_autotest",
                        item.optBoolean(
                            "continuous_autotest",
                            false
                        )
                    )
                    put(
                        "sha256",
                        item.getString(
                            "sha256"
                        )
                    )
                }
            )
        }

        return JSONObject().apply {
            put(
                "schema",
                source.getInt(
                    "schema"
                )
            )
            put(
                "build_version",
                source.getString(
                    "build_version"
                )
            )
            put(
                "base_physically_validated_version",
                source.getString(
                    "base_physically_validated_version"
                )
            )
            put(
                "strategy",
                source.getString(
                    "strategy"
                )
            )
            put(
                "contracts",
                compactContracts
            )
        }
    }
}
