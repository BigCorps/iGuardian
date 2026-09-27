package com.bigcorps.guardian.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAppCatalogTest {
    @Test
    fun confirmedInterPackagesAreFinancial() {
        assertTrue(
            FinancialAppCatalog.isFinancial(
                "br.com.Inter.CDPro",
                "Inter Empresas"
            )
        )
        assertTrue(
            FinancialAppCatalog.isFinancial(
                "br.com.intermedium",
                "Inter"
            )
        )
    }

    @Test
    fun commonFinancialLabelsAreRecognizedWithoutGenericInterFalsePositive() {
        assertTrue(
            FinancialAppCatalog.isFinancial(
                "com.nu.production",
                "Nubank"
            )
        )
        assertFalse(
            FinancialAppCatalog.isFinancial(
                "com.pinterest",
                "Pinterest"
            )
        )
        assertFalse(
            FinancialAppCatalog.isFinancial(
                "com.example.social",
                "Rede Social"
            )
        )
    }

    @Test
    fun internetUtilityIsNotFinancial() {
        assertFalse(
            FinancialAppCatalog.isFinancial(
                "com.example.speedtest",
                "Internet Speed Test"
            )
        )
    }
}
