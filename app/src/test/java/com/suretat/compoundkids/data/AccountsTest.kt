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
        assertEquals(49.92, projectAccount(Account("Money Walkie", 49.92, 0.0), 10), 1e-9)
        // 100 € à 2,4 %/an capitalisés mensuellement pendant 1 an ≈ 102,43 €
        assertEquals(102.43, projectAccount(Account("Livret A", 100.0, 0.024), 1), 0.01)
    }
}
