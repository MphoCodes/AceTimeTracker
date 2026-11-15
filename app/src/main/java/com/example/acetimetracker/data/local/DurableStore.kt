package com.example.acetimetracker.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Account-scoped source of truth and coalescing outbox, committed in the same transaction. */
class DurableStore(context: Context, name: String = "ace-offline.db") :
    SQLiteOpenHelper(context.applicationContext, name, null, 1) {
    data class Row(val account: String, val kind: String, val id: String, val payload: String,
                   val revision: Long, val deleted: Boolean, val pending: Boolean, val error: String?)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE records(account TEXT NOT NULL,kind TEXT NOT NULL,id TEXT NOT NULL,payload TEXT NOT NULL,revision INTEGER NOT NULL,deleted INTEGER NOT NULL,pending INTEGER NOT NULL,error TEXT,PRIMARY KEY(account,kind,id))")
        db.execSQL("CREATE TABLE metadata(meta_key TEXT PRIMARY KEY,meta_value INTEGER NOT NULL)")
        db.execSQL("INSERT INTO metadata VALUES('revision',0)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private fun <T> transaction(block: () -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        return try { block().also { db.setTransactionSuccessful() } } finally { db.endTransaction() }
    }
    fun revision(): Long = readableDatabase.rawQuery("SELECT meta_value FROM metadata WHERE meta_key='revision'", null).use {
        it.moveToFirst(); it.getLong(0)
    }
    private fun nextRevision(): Long {
        writableDatabase.execSQL("UPDATE metadata SET meta_value=meta_value+1 WHERE meta_key='revision'")
        return revision()
    }
    private fun Cursor.row() = Row(getString(0), getString(1), getString(2), getString(3), getLong(4),
        getInt(5) != 0, getInt(6) != 0, if (isNull(7)) null else getString(7))
    private fun rows(where: String, args: Array<String>): List<Row> =
        readableDatabase.rawQuery("SELECT account,kind,id,payload,revision,deleted,pending,error FROM records WHERE $where ORDER BY revision", args).use {
            buildList { while (it.moveToNext()) add(it.row()) }
        }
    fun visible(account: String, kind: String) = rows("account=? AND kind=? AND deleted=0", arrayOf(account, kind))
    fun pending(account: String) = rows("account=? AND pending=1", arrayOf(account))
    private fun existing(account: String, kind: String, id: String) =
        rows("account=? AND kind=? AND id=?", arrayOf(account, kind, id)).firstOrNull()
    private fun write(row: Row) {
        writableDatabase.insertWithOnConflict("records", null, ContentValues().apply {
            put("account", row.account); put("kind", row.kind); put("id", row.id); put("payload", row.payload)
            put("revision", row.revision); put("deleted", if (row.deleted) 1 else 0)
            put("pending", if (row.pending) 1 else 0); put("error", row.error)
        }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) { "Could not persist entry" } }
    }
    fun put(account: String, kind: String, id: String, payload: String, localOnly: Boolean = false): Row = transaction {
        require(account.isNotBlank() && id.isNotBlank())
        Row(account, kind, id, payload, nextRevision(), false, !localOnly, null).also(::write)
    }
    fun delete(account: String, kind: String, id: String, localOnly: Boolean = false): Row = transaction {
        require(account.isNotBlank() && id.isNotBlank())
        Row(account, kind, id, "", nextRevision(), true, !localOnly, null).also(::write)
    }
    fun acknowledge(sent: Row) = transaction {
        val current = existing(sent.account, sent.kind, sent.id)
        if (current?.revision == sent.revision) {
            // Advance on ACK as well: a fetch launched while this row was dirty is stale.
            write(current.copy(revision = nextRevision(), pending = false, error = null))
        }
    }
    fun fail(sent: Row, message: String) = transaction {
        val current = existing(sent.account, sent.kind, sent.id)
        if (current?.revision == sent.revision) write(current.copy(error = message))
    }
    /** Only server-authoritative full snapshots may call this. Tombstones are never resurrected. */
    fun mergeRemote(account: String, kind: String, payloads: Map<String, String>, startedAt: Long): Boolean = transaction {
        var changed = false
        val local = rows("account=? AND kind=?", arrayOf(account, kind)).associateBy { it.id }
        payloads.forEach { (id, payload) ->
            val current = local[id]
            if (current == null || (!current.pending && !current.deleted && current.revision <= startedAt && current.payload != payload)) {
                write(Row(account, kind, id, payload, nextRevision(), false, false, null)); changed = true
            }
        }
        local.values.filter { !it.pending && !it.deleted && it.revision <= startedAt && it.id !in payloads }.forEach {
            write(it.copy(deleted = true, revision = nextRevision())); changed = true
        }
        changed
    }
    fun seedOnce(account: String, kind: String, payloads: Map<String, String>) = transaction {
        val key = "seed:$account:$kind"
        val seeded = readableDatabase.rawQuery("SELECT meta_value FROM metadata WHERE meta_key=?", arrayOf(key)).use { it.moveToFirst() }
        if (!seeded) {
            payloads.forEach { (id, payload) ->
                if (existing(account, kind, id) == null) write(Row(account, kind, id, payload, nextRevision(), false, false, null))
            }
            writableDatabase.execSQL("INSERT INTO metadata(meta_key,meta_value) VALUES(?,1)", arrayOf(key))
        }
    }
}
