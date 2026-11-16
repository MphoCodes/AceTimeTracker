package com.mphocodes.acetimetracker.data.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mphocodes.acetimetracker.AceTimeTrackerApp
import com.mphocodes.acetimetracker.data.local.DurableStore
import com.mphocodes.acetimetracker.data.local.RecordCodec
import com.mphocodes.acetimetracker.data.model.Category
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import com.mphocodes.acetimetracker.data.repository.AuthRepository
import com.mphocodes.acetimetracker.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Pending count is durable; success of a local save does not imply server acknowledgment. */
data class SyncStatus(val pendingCount: Int = 0, val isSyncing: Boolean = false,
                      val lastError: String? = null, val isLocalOnly: Boolean = false)

/** One application-owned worker; it holds no Activity, ViewModel, or query listeners. */
internal class OfflineData private constructor(context: Context, private val firestore: FirebaseFirestore) {
    companion object {
        private var owner = WeakReference<Context>(null)
        private var instance: OfflineData? = null
        @Synchronized fun get(firestore: FirebaseFirestore): OfflineData {
            val context = AceTimeTrackerApp.context
            if (owner.get() !== context) {
                instance?.close()
                owner = WeakReference(context)
                instance = OfflineData(context, firestore)
            }
            return requireNotNull(instance)
        }
    }
    private val store = DurableStore(context)
    private val worker = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "Ace-offline-data").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())
    private val statuses = ConcurrentHashMap<String, MutableLiveData<SyncStatus>>()
    private val changes = ConcurrentHashMap<String, MutableLiveData<Long>>()
    private val refreshErrors = mutableMapOf<Pair<String, String>, String>()
    private val auth = FirebaseAuth.getInstance(firestore.app)
    private val session = AuthRepository(auth)
    private val inFlight = mutableMapOf<String, DurableStore.Row>()
    private val refreshing = mutableSetOf<String>()
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        firebaseAuth.currentUser?.uid?.let { uid -> worker.execute { sync(uid) } }
    }
    init {
        auth.addAuthStateListener(authListener)
        // A pending SDK write also resumes automatically when connectivity returns. Failed writes
        // are retried here while the process lives and immediately after authenticated relaunch.
        worker.scheduleWithFixedDelay({
            auth.currentUser?.uid?.let { uid -> runCatching { sync(uid) } }
        }, 30, 30, TimeUnit.SECONDS)
    }
    private fun close() {
        auth.removeAuthStateListener(authListener)
        worker.shutdown()
        // Queue the close behind already submitted operations, outside the UI thread.
        Thread { worker.awaitTermination(5, TimeUnit.SECONDS); store.close() }.start()
    }
    fun status(account: String): LiveData<SyncStatus> = statuses.getOrPut(account) {
        MutableLiveData(SyncStatus(isLocalOnly = demo(account)))
    }.also { worker.execute { publish(account) } }
    fun changes(account: String): LiveData<Long> = changes.getOrPut(account) { MutableLiveData(0L) }
    fun retry(account: String) { worker.execute {
        sync(account)
        refresh(account, RecordCodec.ENTRIES) { }
        refresh(account, RecordCodec.CATEGORIES) { }
    } }
    private fun demo(account: String) = account == AuthRepository.TEST_USER_ID
    private fun allowed(account: String) = account.isNotBlank() && session.currentUserId == account
    private fun signedIn(account: String) = allowed(account) && auth.currentUser?.uid == account
    private fun publish(account: String) {
        val pending = store.pending(account)
        statuses.getOrPut(account) { MutableLiveData() }.postValue(SyncStatus(
            pending.size, account in inFlight, pending.firstNotNullOfOrNull { it.error } ?: refreshErrors.entries.firstOrNull { it.key.first == account }?.value, demo(account)))
    }
    private fun changed(account: String) { changes.getOrPut(account) { MutableLiveData() }.postValue(store.revision()) }
    private fun seed(account: String, kind: String) {
        if (!demo(account)) return
        val data = if (kind == RecordCodec.CATEGORIES) listOf(
            Category("mock_cat_1", "Development", "Android App development and refactoring", 2f, 6f, 4f, "#2563EB"),
            Category("mock_cat_2", "Design", "UI/UX wireframing and prototyping", 1f, 4f, 2f, "#F59E0B"),
            Category("mock_cat_3", "Testing", "Unit testing and QA verification", 1f, 3f, 2f, "#10B981"),
            Category("mock_cat_4", "Documentation", "Architecture and user documentation", 1f, 2f, 1.5f, "#8B5CF6")
        ).associate { it.documentId to RecordCodec.encode(it) } else listOf(
            TimesheetEntry("mock_ts_1", account, "Development", "2026-10-25", "09:00", "2026-10-25", "14:30", 19_800_000, "", "Refactored Clean Architecture and MVVM layers"),
            TimesheetEntry("mock_ts_2", account, "Design", "2026-10-26", "10:00", "2026-10-26", "13:00", 10_800_000, "", "Designed progress chart layouts and navigation"),
            TimesheetEntry("mock_ts_3", account, "Testing", "2026-10-27", "08:00", "2026-10-27", "10:00", 7_200_000, "", "Verified offline demo login flow")
        ).associate { it.id to RecordCodec.encode(it) }
        store.seedOnce(account, kind, data)
    }
    fun write(account: String, kind: String, id: String, payload: String?, callback: (Resource<Unit>) -> Unit) {
        if (account.isBlank() || id.isBlank()) { callback(Resource.Error("Invalid request")); return }
        callback(Resource.Loading)
        worker.execute {
            try {
                if (!allowed(account)) {
                    main.post { callback(Resource.Error("Account changed. Please sign in again.")) }
                    return@execute
                }
                seed(account, kind)
                if (payload == null) store.delete(account, kind, id, demo(account))
                else store.put(account, kind, id, payload, demo(account))
                changed(account); publish(account)
                main.post { callback(Resource.Success(Unit)) }
                runCatching { sync(account) }
            } catch (error: Exception) {
                main.post { callback(Resource.Error("Could not save on this device. Please try again.", error)) }
            }
        }
    }
    fun read(account: String, kind: String, callback: (Resource<List<String>>) -> Unit) {
        if (account.isBlank()) { callback(Resource.Error("User not authenticated")); return }
        callback(Resource.Loading)
        worker.execute {
            try {
                if (!allowed(account)) {
                    main.post { callback(Resource.Error("Account changed. Please sign in again.")) }
                    return@execute
                }
                seed(account, kind)
                val local = store.visible(account, kind).map { it.payload }
                main.post {
                    if (allowed(account)) callback(Resource.Success(local))
                    else callback(Resource.Error("Account changed. Please sign in again."))
                }
                publish(account)
                refresh(account, kind, callback)
                sync(account)
            } catch (error: Exception) {
                main.post { callback(Resource.Error("Could not read saved data on this device.", error)) }
            }
        }
    }
    private fun refresh(account: String, kind: String, callback: (Resource<List<String>>) -> Unit) {
        if (demo(account) || !signedIn(account)) return
        val key = "$account:$kind"
        if (!refreshing.add(key)) return
        val watermark = store.revision()
        val query = if (kind == RecordCodec.ENTRIES) firestore.collection("timesheetEntries").whereEqualTo("userId", account)
            else firestore.collection("users").document(account).collection("categories")
        query.get(Source.SERVER).addOnSuccessListener(worker) { snapshot ->
            refreshing.remove(key)
            if (!signedIn(account) || snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()) return@addOnSuccessListener
            try {
                val payloads = snapshot.documents.associate { doc ->
                    doc.id to if (kind == RecordCodec.ENTRIES) {
                        val entry = requireNotNull(doc.toObject(TimesheetEntry::class.java)).copy(id = doc.id, userId = account)
                        RecordCodec.encode(entry)
                    } else RecordCodec.encode(Category(doc.id, doc.getString("name").orEmpty(), doc.getString("description").orEmpty(),
                        doc.getDouble("minGoal")?.toFloat(), doc.getDouble("maxGoal")?.toFloat(), doc.getDouble("goal")?.toFloat(), doc.getString("colorHex") ?: "#DC2626"))
                }
                val didChange = store.mergeRemote(account, kind, payloads, watermark)
                refreshErrors.remove(account to kind)
                if (didChange) {
                    changed(account)
                    val local = store.visible(account, kind).map { it.payload }
                    main.post {
                        if (allowed(account)) callback(Resource.Success(local))
                        else callback(Resource.Error("Account changed. Please sign in again."))
                    }
                }
                publish(account)
            } catch (error: Exception) { refreshErrors[account to kind] = error.localizedMessage ?: "Could not refresh remote data"; publish(account) }
        }.addOnFailureListener(worker) { error ->
            refreshing.remove(key)
            refreshErrors[account to kind] = error.localizedMessage ?: "Offline; showing saved data"
            publish(account)
        }
    }
    private fun sync(account: String) {
        if (demo(account) || !signedIn(account) || account in inFlight) { publish(account); return }
        val next = store.pending(account).firstOrNull() ?: run { publish(account); return }
        inFlight[account] = next; publish(account)
        try {
            val doc = if (next.kind == RecordCodec.ENTRIES) firestore.collection("timesheetEntries").document(next.id)
                else firestore.collection("users").document(account).collection("categories").document(next.id)
            val task = if (next.deleted) doc.delete()
                else if (next.kind == RecordCodec.ENTRIES) doc.set(RecordCodec.entry(next.payload))
                else doc.set(RecordCodec.remoteCategory(RecordCodec.category(next.payload)), SetOptions.merge())
            task.addOnSuccessListener(worker) {
                store.acknowledge(next); inFlight.remove(account); publish(account)
                // A changed session must not send another account's outbox under its credentials.
                auth.currentUser?.uid?.let(::sync)
            }.addOnFailureListener(worker) { error ->
                store.fail(next, error.localizedMessage ?: "Sync failed; saved on this device")
                inFlight.remove(account); publish(account)
            }
        } catch (error: Exception) {
            store.fail(next, error.localizedMessage ?: "Sync failed; saved on this device")
            inFlight.remove(account); publish(account)
        }
    }
}
