package com.example.acetimetracker.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.data.repository.TimesheetRepository
import com.example.acetimetracker.ui.components.HomeBottomBar
import com.example.acetimetracker.ui.screen.*
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.ProfileViewModel
import com.example.acetimetracker.ui.viewmodel.TimerViewModel
import com.example.acetimetracker.ui.viewmodel.TimesheetViewModel
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.util.TimeUtils
import com.example.acetimetracker.util.TimesheetCsv

/** One activity owns primary navigation and the timer, including external quick actions. */
class MainActivity : ComponentActivity() {
    private val profileViewModel: ProfileViewModel by viewModels()
    private val entriesViewModel: TimesheetViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()
    private val authRepository = AuthRepository()
    private val repository = TimesheetRepository()
    private var tab by mutableStateOf(Destination.Today)
    private var selectedImageUri by mutableStateOf<Uri?>(null)
    private var formResetKey by mutableIntStateOf(0)
    private var entryTab by mutableIntStateOf(1)
    private var entryRequest by mutableIntStateOf(0)
    private var restoredEditId: String? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
            selectedImageUri = uri
            entriesViewModel.uploadImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireAccount()) return
        tab = savedInstanceState?.getString("tab")?.let { runCatching { Destination.valueOf(it) }.getOrNull() } ?: Destination.Today
        entryTab = savedInstanceState?.getInt("entry_tab", 1) ?: 1
        entryRequest = savedInstanceState?.getInt("entry_request", 0) ?: 0
        if (savedInstanceState == null) handleIntent(intent)
        restoredEditId = if (entriesViewModel.editingEntry == null) savedInstanceState?.getString("editing_id") else null
        selectedImageUri = savedInstanceState?.getString("selected_image")?.let(Uri::parse)
        entriesViewModel.timesheetsState.observe(this) { result ->
            val id = restoredEditId
            if (id != null && result is Resource.Success) {
                result.data.firstOrNull { it.id == id }?.let {
                    entriesViewModel.beginEdit(it)
                    savedInstanceState?.getString("editing_image_url")?.let { url -> entriesViewModel.selectedImageUrl = url }
                    if (savedInstanceState?.getBoolean("upload_pending") == true) selectedImageUri?.let(entriesViewModel::uploadImage)
                    restoredEditId = null
                }
            }
        }
        observeResults()
        repository.observeChanges(authRepository.currentUserId).observe(this) { refreshEntries() }
        setContent {
            AceTimeTrackerTheme {
                val vm = entriesViewModel
                val timer = timerViewModel
                val profiles by profileViewModel.userProfileState.observeAsState()
                val entries by vm.timesheetsState.observeAsState()
                val categories by vm.categoriesState.observeAsState()
                val saving by vm.saveEntryState.observeAsState()
                val uploading by vm.uploadImageState.observeAsState()
                val sync by repository.observeSyncStatus(authRepository.currentUserId).observeAsState()
                val stateHolder = rememberSaveableStateHolder()
                BackHandler(tab != Destination.Today) { navigate(Destination.Today) }
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        Column {
                            if (timer.isAnyRunning || timer.unsavedMs > 0) {
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f).clickable { navigate(Destination.Timer) }.padding(vertical = 8.dp)) {
                                            Text(timer.selectedCategory.ifBlank { "Current session" }, style = MaterialTheme.typography.labelLarge)
                                            Text("${if (timer.isAnyRunning) "Running" else "Paused"} · ${TimeUtils.formatDuration(timer.unsavedMs)}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        TextButton(enabled = !timer.isSaving, onClick = { timer.setRunning(!timer.isAnyRunning) }) { Text(if (timer.isAnyRunning) "Pause" else "Resume") }
                                    }
                                }
                            }
                            sync?.takeIf { it.lastError != null || it.pendingCount > 0 }?.let { status ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(when {
                                        status.isLocalOnly -> "Saved on this device · Demo"
                                        status.lastError != null -> "Saved on device · Sync needs attention"
                                        status.isSyncing -> "Syncing ${status.pendingCount} changes…"
                                        status.pendingCount > 0 -> "Saved on device · ${status.pendingCount} pending"
                                        else -> "Synced"
                                    }, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                    if (status.lastError != null || status.pendingCount > 0) TextButton(onClick = { repository.retrySync(authRepository.currentUserId) }) { Text("Retry") }
                                }
                            }
                            HomeBottomBar(onNavigate = ::navigate, selected = tab)
                        }
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        stateHolder.SaveableStateProvider(tab.name) {
                            when (tab) {
                                Destination.Today -> MainScreen(
                                    userProfile = (profiles as? Resource.Success<UserProfile>)?.data,
                                    onNavigate = ::navigate,
                                    timesheets = (entries as? Resource.Success)?.data.orEmpty(),
                                    categories = (categories as? Resource.Success)?.data.orEmpty(),
                                    isLoading = entries == null || categories == null,
                                    showBottomBar = false
                                )
                                Destination.Timer -> TimerScreen(
                                    categoriesState = categories, mode = timer.mode, onModeChange = timer::changeMode,
                                    stopwatchElapsedTime = timer.stopwatchElapsedMs, isStopwatchRunning = timer.isStopwatchRunning,
                                    onToggleStopwatch = { requestNotifications(); timer.toggleStopwatch() }, onResetStopwatch = timer::resetStopwatch,
                                    pomodoroPhase = timer.pomodoroPhase, pomodoroRemainingTime = timer.pomodoroRemainingMs,
                                    pomodoroTotalPhaseDuration = timer.pomodoroTotalDurationMs, pomodoroFocusMinutes = timer.pomodoroFocusMinutes,
                                    isPomodoroRunning = timer.isPomodoroRunning, pomodoroRoundsCompleted = timer.pomodoroRoundsCompleted,
                                    pomodoroFocusLogged = timer.pomodoroFocusLoggedMs,
                                    onTogglePomodoro = { requestNotifications(); timer.togglePomodoro() }, onResetPomodoro = timer::resetPomodoro,
                                    onSkipPomodoroPhase = timer::skipPomodoroPhase, onSelectPomodoroPreset = timer::selectPomodoroPreset,
                                    selectedCategory = timer.selectedCategory, onSelectCategory = timer::selectCategory,
                                    isSaving = timer.isSaving, onSaveSession = timer::saveSession, message = timer.message,
                                    onMessageShown = timer::consumeMessage,
                                    onCreateCategoryClick = { navigate(Destination.Categories) }, onBackClick = { navigate(Destination.Today) },
                                    keepRunningOnNavigate = true
                                )
                                Destination.Timesheet -> TimesheetScreen(
                                    categoriesState = categories, timesheetsState = entries, saveEntryState = saving,
                                    uploadImageState = uploading, selectedImageUri = selectedImageUri ?: vm.selectedImageUrl.takeIf(String::isNotBlank)?.let(Uri::parse),
                                    formResetKey = formResetKey, initialTab = entryTab, tabRequestKey = entryRequest,
                                    onBackClick = { navigate(Destination.Today) },
                                    onSelectImageClick = { imagePicker.launch(arrayOf("image/*")) },
                                    onRemoveImageClick = { selectedImageUri = null; vm.clearSelectedImage() },
                                    onSaveClick = vm::saveTimesheetEntry, onDeleteEntryClick = vm::deleteTimesheet,
                                    onExportCsvClick = { list -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, TimesheetCsv.export(list))
                                    }, "Share timesheet")) },
                                    editingEntry = vm.editingEntry,
                                    onEditEntryClick = { selectedImageUri = it.imageUrl.takeIf(String::isNotBlank)?.let(Uri::parse); vm.beginEdit(it) },
                                    onCancelEdit = { vm.cancelEdit(); selectedImageUri = null; formResetKey++ },
                                    onRepeatEntryClick = { timer.trackAgain(it.category); navigate(Destination.Timer) }
                                )
                                Destination.MyProgress -> ReportsScreen((entries as? Resource.Success)?.data.orEmpty(), (categories as? Resource.Success)?.data.orEmpty(), onBackClick = { navigate(Destination.Today) }, onStartSession = { navigate(Destination.Timer) })
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); if (requireAccount()) handleIntent(intent) }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("tab", tab.name); outState.putInt("entry_tab", entryTab); outState.putInt("entry_request", entryRequest); outState.putString("editing_id", entriesViewModel.editingEntry?.id); outState.putString("selected_image", selectedImageUri?.toString()); outState.putString("editing_image_url", entriesViewModel.selectedImageUrl); outState.putBoolean("upload_pending", entriesViewModel.uploadImageState.value is Resource.Loading); super.onSaveInstanceState(outState) }
    override fun onResume() { super.onResume(); if (requireAccount()) { profileViewModel.loadUserProfile(); refreshEntries() } }

    private fun requireAccount(): Boolean {
        if (authRepository.isAuthenticated) return true
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish(); return false
    }
    private fun handleIntent(source: Intent) {
        source.getStringExtra("open_tab")?.let { value -> runCatching { Destination.valueOf(value) }.getOrNull()?.let(::navigate) }
        when (source.getStringExtra("timer_action")) { "pause" -> timerViewModel.setRunning(false); "resume" -> timerViewModel.setRunning(true) }
        source.removeExtra("timer_action"); source.removeExtra("open_tab")
    }
    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    private fun refreshEntries() { entriesViewModel.loadTimesheets(); entriesViewModel.loadCategories() }
    private fun navigate(destination: Destination) {
        when (destination) {
            Destination.LogEntry -> {
                if (entriesViewModel.saveEntryState.value !is Resource.Loading) { entriesViewModel.cancelEdit(); selectedImageUri = null }
                entryTab = 0; entryRequest++; tab = Destination.Timesheet; refreshEntries() }
            Destination.Timesheet -> { entryTab = 1; entryRequest++; tab = destination; refreshEntries() }
            Destination.Today, Destination.Timer, Destination.MyProgress -> { tab = destination; refreshEntries() }
            Destination.Graph -> { tab = Destination.MyProgress; refreshEntries() }
            Destination.Profile -> startActivity(Intent(this, ProfileActivity::class.java))
            Destination.Categories -> startActivity(Intent(this, CategoriesActivity::class.java))
            Destination.Settings -> startActivity(Intent(this, SettingsActivity::class.java))
            Destination.About -> startActivity(Intent(this, AboutActivity::class.java))
            Destination.Logout -> { timerViewModel.pauseAll(); com.example.acetimetracker.service.TimerForegroundService.stop(this); authRepository.logout(); requireAccount() }
        }
    }
    private fun observeResults() {
        entriesViewModel.saveEntryState.observe(this) { result ->
            if (result is Resource.Success) {
                Toast.makeText(this, "Entry saved on this device", Toast.LENGTH_SHORT).show()
                entriesViewModel.consumeSaveState(); entriesViewModel.cancelEdit(); selectedImageUri = null; formResetKey++; entryTab = 1; entryRequest++; refreshEntries()
            } else if (result is Resource.Error) { Toast.makeText(this, result.message, Toast.LENGTH_LONG).show(); entriesViewModel.consumeSaveState() }
        }
        entriesViewModel.deleteEntryState.observe(this) { result ->
            if (result is Resource.Error) Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            if (result is Resource.Success || result is Resource.Error) entriesViewModel.consumeDeleteState()
        }
        entriesViewModel.uploadImageState.observe(this) { result ->
            if (result is Resource.Error) { Toast.makeText(this, result.message, Toast.LENGTH_LONG).show(); selectedImageUri = null; entriesViewModel.consumeUploadState() }
        }
    }
}
