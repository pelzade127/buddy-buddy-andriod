package com.budgetbuddy.android

import com.budgetbuddy.android.data.asMoney
import com.budgetbuddy.android.data.toMoney
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun parsesValidString() {
        assertEquals(3000.0, "3000.00".toMoney(), 0.001)
    }

    @Test fun nullBecomesZero() {
        assertEquals(0.0, (null as String?).toMoney(), 0.001)
    }

    @Test fun garbageBecomesZero() {
        assertEquals(0.0, "not a number".toMoney(), 0.001)
    }

    @Test fun formatsWithCommasAndCents() {
        assertEquals("$1,250.50", 1250.5.asMoney())
    }
}