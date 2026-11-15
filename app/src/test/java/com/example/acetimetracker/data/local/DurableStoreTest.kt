package com.example.acetimetracker.data.local

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class DurableStoreTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun store() = DurableStore(context, "durable-test.db")

    @Test fun queuedEditAndPhotoSurviveReopeningAndStayAccountScoped() {
        store().use { db ->
            db.put("alice", "entries", "one", "{photo:kept}")
            db.put("bob", "entries", "one", "bob-private")
            db.put("alice", "entries", "one", "{photo:kept,edited:true}")
        }
        store().use { db ->
            assertEquals("{photo:kept,edited:true}", db.visible("alice", "entries").single().payload)
            assertEquals("bob-private", db.visible("bob", "entries").single().payload)
            assertEquals(1, db.pending("alice").size)
        }
    }

    @Test fun staleAcknowledgmentCannotClearANewerEditOrFailure() {
        store().use { db ->
            val first = db.put("alice", "entries", "one", "old")
            val second = db.put("alice", "entries", "one", "new")
            db.fail(second, "offline")
            db.acknowledge(first)
            assertEquals(second.revision, db.pending("alice").single().revision)
            assertEquals("offline", db.pending("alice").single().error)
            db.acknowledge(second)
            assertTrue(db.pending("alice").isEmpty())
            assertEquals("new", db.visible("alice", "entries").single().payload)
        }
    }

    @Test fun deleteTombstoneSurvivesRestartAckAndStaleRemoteMerge() {
        store().use { db ->
            db.put("alice", "entries", "one", "old")
            val deletion = db.delete("alice", "entries", "one")
            db.acknowledge(deletion)
        }
        store().use { db ->
            db.mergeRemote("alice", "entries", mapOf("one" to "old"), db.revision())
            assertTrue(db.visible("alice", "entries").isEmpty())
        }
    }

    @Test fun snapshotStartedBeforeLocalEditCannotOverwriteAcknowledgedEdit() {
        store().use { db ->
            val watermark = db.revision()
            val edit = db.put("alice", "entries", "one", "new")
            db.acknowledge(edit)
            db.mergeRemote("alice", "entries", mapOf("one" to "stale"), watermark)
            assertEquals("new", db.visible("alice", "entries").single().payload)
        }
    }

    @Test fun remoteRefreshPreservesPendingRowsAndReflectsRemoteDeletions() {
        store().use { db ->
            db.mergeRemote("alice", "entries", mapOf("remote" to "server"), db.revision())
            db.put("alice", "entries", "local", "unsynced")
            db.mergeRemote("alice", "entries", emptyMap(), db.revision())
            assertEquals(listOf("local"), db.visible("alice", "entries").map { it.id })
        }
    }

    @Test fun failedOutboxOperationSurvivesReopenAndStaleFailureCannotReplaceNewEdit() {
        store().use { db ->
            val old = db.put("alice", "entries", "one", "old")
            val newer = db.put("alice", "entries", "one", "new")
            db.fail(newer, "permission denied")
            db.fail(old, "stale network error")
        }
        store().use { db ->
            val retry = db.pending("alice").single()
            assertEquals("new", retry.payload)
            assertEquals("permission denied", retry.error)
            db.acknowledge(retry)
            assertTrue(db.pending("alice").isEmpty())
        }
    }

    @Test fun fetchStartedWhilePendingCannotReplaceRowAfterAcknowledgment() {
        store().use { db ->
            val edit = db.put("alice", "entries", "one", "new")
            val watermark = db.revision()
            db.acknowledge(edit)
            db.mergeRemote("alice", "entries", mapOf("one" to "old"), watermark)
            assertEquals("new", db.visible("alice", "entries").single().payload)
        }
    }

    @Test fun demoSeedsOnceEvenWhenEverySampleWasDeleted() {
        store().use { db ->
            db.seedOnce("demo", "entries", mapOf("sample" to "value"))
            db.delete("demo", "entries", "sample", localOnly = true)
        }
        store().use { db ->
            db.seedOnce("demo", "entries", mapOf("sample" to "value"))
            assertTrue(db.visible("demo", "entries").isEmpty())
            assertTrue(db.pending("demo").isEmpty())
        }
    }
}
