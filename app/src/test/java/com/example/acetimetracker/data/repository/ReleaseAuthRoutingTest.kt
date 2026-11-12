package com.example.acetimetracker.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Looper
import com.example.acetimetracker.AceTimeTrackerApp
import com.example.acetimetracker.util.Resource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
@LooperMode(LooperMode.Mode.PAUSED)
class ReleaseAuthRoutingTest {
    private lateinit var context: Context
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var server: ServerSocket
    private lateinit var serverThread: Thread
    private lateinit var repository: AuthRepository
    private val requestLine = AtomicReference<String?>()
    private var originalFlags = 0

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        originalFlags = context.applicationInfo.flags
        context.applicationInfo.flags = originalFlags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        serverThread = thread(isDaemon = true, name = "fake-auth-server") {
            try {
                while (!server.isClosed) {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                        requestLine.set(reader.readLine())
                        var contentLength = 0
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                                contentLength = line.substringAfter(':').trim().toInt()
                            }
                        }
                        repeat(contentLength) { reader.read() }
                        val body = "{\"error\":{\"code\":400,\"message\":\"EMAIL_NOT_FOUND\"}}"
                        val response = "HTTP/1.1 400 Bad Request\r\nContent-Type: application/json\r\n" +
                            "Content-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n" + body
                        socket.getOutputStream().write(response.toByteArray())
                    }
                }
            } catch (_: IOException) {
            }
        }
        firebaseApp = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:1234567890abcdef")
                .setApiKey("local-test-api-key-${UUID.randomUUID()}")
                .setProjectId("demo-acetimetracker")
                .build(),
            "auth-routing-${UUID.randomUUID()}"
        )
        val auth = FirebaseAuth.getInstance(firebaseApp)
        auth.useEmulator("127.0.0.1", server.localPort)
        repository = AuthRepository(auth)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.applicationInfo.flags = originalFlags
        firebaseApp.delete()
        server.close()
        serverThread.join(1000)
    }

    @Test
    fun releaseLoginWithTestCredentialsDoesNotCreateAMockSession() {
        val result = awaitResult { callback ->
            repository.login(AuthRepository.TEST_USER_EMAIL, AuthRepository.TEST_USER_PASSWORD, callback)
        }

        assertTrue(result is Resource.Error)
        assertTrue("request=${requestLine.get()} error=$result", requestLine.get()?.contains("/relyingparty/verifyPassword") == true)
        assertFalse(repository.isMockSession)
    }

    @Test
    fun releaseRegistrationWithTestEmailDoesNotCreateAMockSession() {
        val result = awaitResult { callback ->
            repository.register(AuthRepository.TEST_USER_EMAIL, "password123", callback)
        }

        assertTrue(result is Resource.Error)
        assertTrue("request=${requestLine.get()} error=$result", requestLine.get()?.contains("/relyingparty/signupNewUser") == true)
        assertFalse(repository.isMockSession)
    }

    @Test
    fun releasePasswordResetForTestEmailDoesNotReturnMockSuccess() {
        val result = awaitResult<Unit> { callback ->
            repository.sendPasswordReset(AuthRepository.TEST_USER_EMAIL, callback)
        }

        assertTrue(result is Resource.Error)
        assertTrue("request=${requestLine.get()} error=$result", requestLine.get()?.contains("/relyingparty/getOobConfirmationCode") == true)
    }

    private fun <T> awaitResult(start: (((Resource<T>) -> Unit) -> Unit)): Resource<T> {
        val result = AtomicReference<Resource<T>?>()
        start { state -> if (state !is Resource.Loading) result.set(state) }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (result.get() == null && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        shadowOf(Looper.getMainLooper()).idle()
        return requireNotNull(result.get()) { "Firebase Auth did not return a terminal result" }
    }
}
