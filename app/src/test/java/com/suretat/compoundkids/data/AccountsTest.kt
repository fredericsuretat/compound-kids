package com.suretat.compoundkids.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountsTest {
    @Test fun parseAmount_accepteVirguleEtEspaces() {
        assertEquals(2177.16, parseAmount("2 177,16")!!, 1e-9)
        assertEquals(10.75, parseAmount("10.75")!!, 1e-9)
        assertNull(parseAmount(""))
        assertNull(parseAmount("abc"))
    }

    @Test fun projection_sansRendementEtAvecRendement() {
        assertEquals(49.92, projectAccount(Account("Money Walkie", 49.92, 49.92, 0.0), 10), 1e-9)
        // 100 € à 2,4 %/an capitalisés mensuellement pendant 1 an ≈ 102,43 €
        assertEquals(102.43, projectAccount(Account("Livret A", 100.0, 100.0, 0.024), 1), 0.01)
    }

    @Test fun projection_avecAjouts() {
        val sansRendement = Account("Espèces", 100.0, 100.0, 0.0)
        assertEquals(100.0 + 20 * 12 * 2, projectAccount(sansRendement, 2, monthly = 20.0), 1e-9)
        assertEquals(150.0, projectAccount(sansRendement, 5, lump = 50.0), 1e-9)
        val vanguard = Account("LifeStrategy", 202.10, 200.0, 0.07)
        assert(projectAccount(vanguard, 10, monthly = 20.0) > projectAccount(vanguard, 10) + 20 * 12 * 10)
    }

    @Test fun formatCode_groupesDe4() {
        assertEquals("ABCD-EFGH-JKLM", formatCode("ABCDEFGHJKLM"))
    }
}
