package com.mphocodes.acetimetracker.data.repository

internal object CleanupTransaction {
    fun run(steps: List<() -> Boolean>, purgeRecords: ((Boolean) -> Unit) -> Unit,
            acknowledge: () -> Boolean, callback: (Boolean) -> Unit) {
        val localSuccess = steps.map { runCatching(it).getOrDefault(false) }.all { it }
        purgeRecords { recordsSuccess ->
            callback(localSuccess && recordsSuccess && runCatching(acknowledge).getOrDefault(false))
        }
    }
}
