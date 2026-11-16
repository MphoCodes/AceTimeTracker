package com.mphocodes.acetimetracker.data.repository
import org.junit.Assert.*
import org.junit.Test
class CleanupTransactionTest {
    @Test fun failedFileOrPreferenceCleanupRetainsMarkerAndRetryCanComplete() {
        var marker = true
        var result = true
        var fails = true
        val steps = listOf<() -> Boolean>({ !fails }, { true })
        val acknowledge = { marker = false; true }
        CleanupTransaction.run(steps, { it(true) }, acknowledge) { result = it }
        assertFalse(result); assertTrue(marker)
        fails = false
        CleanupTransaction.run(steps, { it(true) }, acknowledge) { result = it }
        assertTrue(result); assertFalse(marker)
    }
    @Test fun failedDatabaseCleanupRetainsMarker() {
        var acknowledged = false
        CleanupTransaction.run(listOf({true}), {it(false)}, {acknowledged=true; true}) {assertFalse(it)}
        assertFalse(acknowledged)
    }
}
