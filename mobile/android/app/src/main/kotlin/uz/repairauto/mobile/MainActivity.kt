package uz.repairauto.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.telegram.login.TelegramLogin
import java.util.UUID

class MainActivity : FlutterActivity() {
    companion object {
        private const val TAG = "RepairAutoMobile"
        private const val CHANNEL_NAME = "repair_auto/telegram_auth"
    }

    private enum class TelegramAuthSessionState {
        IDLE,
        LOGIN_STARTED,
        WAITING_FOR_CALLBACK,
        CONSUMING_CALLBACK,
        COMPLETED
    }

    private var sessionState: TelegramAuthSessionState = TelegramAuthSessionState.IDLE
    private var pendingLoginResult: MethodChannel.Result? = null
    private var activeAttemptId: String? = null
    private var activeRole: String? = null
    private var activeClientId: String? = null
    private var activeRedirectUri: String? = null
    private var flutterChannelReady: Boolean = false
    private var lastHandledCallbackSignature: String? = null

    private fun activityDiagnostics(): String =
        "activityHash=${Integer.toHexString(System.identityHashCode(this))} taskId=$taskId"

    private fun sanitizeUriForLogging(uri: Uri?): String {
        if (uri == null) return "null"
        return "${uri.scheme}://${uri.host}${uri.path}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUri = intent?.data
        Log.i(TAG, "[TELEGRAM_ACTIVITY_CREATE] ${activityDiagnostics()} initialData=${sanitizeUriForLogging(initialUri)}")

        // If the Activity was created with intent data, do NOT attempt to consume it from IDLE state.
        // There is no active TelegramLogin in-memory session in this newly initialized process.
        if (initialUri != null) {
            Log.w(
                TAG,
                "[TELEGRAM_CALLBACK_IGNORED_STALE] ${activityDiagnostics()} Callback present on onCreate with state=$sessionState. Clearing stale intent data."
            )
            intent?.data = null
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "login" -> startTelegramLogin(call, result)
                    "pendingRole" -> result.success(activeRole)
                    "cancel" -> handleCancelLogin(result)
                    else -> result.notImplemented()
                }
            }
        flutterChannelReady = true
        Log.i(TAG, "[TELEGRAM_ENGINE_CONFIGURED] ${activityDiagnostics()} Flutter channel ready.")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incomingUri = intent.data
        Log.i(TAG, "[TELEGRAM_ACTIVITY_NEW_INTENT] ${activityDiagnostics()} action=${intent.action} data=${sanitizeUriForLogging(incomingUri)}")
        if (incomingUri != null) {
            handleIncomingTelegramCallback(incomingUri, source = "onNewIntent")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[TELEGRAM_ACTIVITY_RESUME] ${activityDiagnostics()} state=$sessionState")
        val resumeUri = intent?.data
        if (resumeUri != null) {
            handleIncomingTelegramCallback(resumeUri, source = "onResume")
        }
    }

    @Synchronized
    private fun startTelegramLogin(call: MethodCall, result: MethodChannel.Result) {
        val role = call.argument<String>("role") ?: "CUSTOMER"
        val clientId = call.argument<String>("clientId")
        val redirectUri = call.argument<String>("redirectUri")
        val scopes = call.argument<List<String>>("scopes") ?: listOf("profile")

        Log.i(
            TAG,
            "[TELEGRAM_LOGIN_START] ${activityDiagnostics()} role=$role clientId=$clientId redirectUri=${sanitizeUriForLogging(redirectUri?.let(Uri::parse))} currentState=$sessionState"
        )

        // If an existing attempt is currently in-flight, cancel the previous pending result cleanly
        // to prevent duplicate hanging callbacks or deadlocks.
        if (pendingLoginResult != null || sessionState == TelegramAuthSessionState.WAITING_FOR_CALLBACK) {
            Log.w(TAG, "[TELEGRAM_LOGIN_REPLACED] ${activityDiagnostics()} Existing login attempt $activeAttemptId was active. Resetting previous request.")
            pendingLoginResult?.error("LOGIN_REPLACED", "Previous Telegram login was replaced by a new attempt", null)
            resetState()
        }

        if (clientId.isNullOrBlank() || redirectUri.isNullOrBlank()) {
            Log.e(TAG, "[TELEGRAM_LOGIN_ERROR] ${activityDiagnostics()} Invalid configuration: clientId or redirectUri is missing.")
            result.error("INVALID_CONFIGURATION", "Telegram clientId and redirectUri are required", null)
            return
        }

        val attemptId = UUID.randomUUID().toString()
        activeAttemptId = attemptId
        activeRole = role
        activeClientId = clientId
        activeRedirectUri = redirectUri
        pendingLoginResult = result
        sessionState = TelegramAuthSessionState.LOGIN_STARTED

        try {
            TelegramLogin.init(clientId, redirectUri, scopes)
            sessionState = TelegramAuthSessionState.WAITING_FOR_CALLBACK
            TelegramLogin.startLogin(this)
            Log.i(TAG, "[TELEGRAM_LOGIN_DISPATCHED] ${activityDiagnostics()} attemptId=$attemptId role=$role successfully launched TelegramLogin.startLogin")
        } catch (error: Exception) {
            Log.e(TAG, "[TELEGRAM_LOGIN_ERROR] ${activityDiagnostics()} startLogin failed: ${error.javaClass.simpleName} - ${error.message}", error)
            sessionState = TelegramAuthSessionState.IDLE
            pendingLoginResult = null
            result.error("TELEGRAM_START_FAILED", error.message ?: "Failed to start Telegram login", null)
            resetState()
        }
    }

    @Synchronized
    private fun handleIncomingTelegramCallback(uri: Uri, source: String) {
        val host = uri.host
        if (host == null || !host.endsWith("-login.tg.dev")) {
            return
        }

        Log.i(TAG, "[TELEGRAM_CALLBACK_RECEIVED] ${activityDiagnostics()} source=$source host=$host state=$sessionState")

        // 1. Strict lifecycle guard: Never invoke handleLoginResponse unless we are actively waiting for callback!
        if (sessionState != TelegramAuthSessionState.WAITING_FOR_CALLBACK || pendingLoginResult == null) {
            Log.w(
                TAG,
                "[TELEGRAM_CALLBACK_IGNORED_STALE] ${activityDiagnostics()} source=$source Ignored callback because sessionState=$sessionState (not WAITING_FOR_CALLBACK) or pendingResult is null."
            )
            intent?.data = null
            return
        }

        // 2. Deduplication check: Avoid double-processing the exact same URI string across onNewIntent and onResume.
        val callbackString = uri.toString()
        if (lastHandledCallbackSignature == callbackString) {
            Log.w(TAG, "[TELEGRAM_CALLBACK_IGNORED_DUPLICATE] ${activityDiagnostics()} source=$source Callback signature already processed.")
            intent?.data = null
            return
        }

        // 3. Mark state as consuming
        sessionState = TelegramAuthSessionState.CONSUMING_CALLBACK
        lastHandledCallbackSignature = callbackString
        val currentPendingResult = pendingLoginResult
        val currentAttempt = activeAttemptId
        val currentRole = activeRole

        Log.i(TAG, "[TELEGRAM_CALLBACK_HANDLED] ${activityDiagnostics()} source=$source attemptId=$currentAttempt role=$currentRole starting SDK consumption.")

        try {
            TelegramLogin.handleLoginResponse(
                uri,
                onSuccess = { loginData ->
                    Log.i(
                        TAG,
                        "[TELEGRAM_LOGIN_SUCCESS] ${activityDiagnostics()} attemptId=$currentAttempt role=$currentRole idTokenPresent=${!loginData.idToken.isNullOrEmpty()}"
                    )
                    sessionState = TelegramAuthSessionState.COMPLETED
                    currentPendingResult?.success(loginData.idToken)
                    resetState()
                },
                onError = { error ->
                    Log.w(
                        TAG,
                        "[TELEGRAM_LOGIN_ERROR] ${activityDiagnostics()} attemptId=$currentAttempt role=$currentRole error=${error.message}"
                    )
                    sessionState = TelegramAuthSessionState.IDLE
                    currentPendingResult?.error("TELEGRAM_LOGIN_FAILED", error.message ?: "Telegram authorization failed", null)
                    resetState()
                }
            )
        } catch (error: Exception) {
            Log.e(
                TAG,
                "[TELEGRAM_LOGIN_ERROR] ${activityDiagnostics()} attemptId=$currentAttempt role=$currentRole exception=${error.javaClass.simpleName}: ${error.message}",
                error
            )
            sessionState = TelegramAuthSessionState.IDLE
            currentPendingResult?.error("TELEGRAM_CALLBACK_FAILED", error.message ?: "Telegram callback processing error", null)
            resetState()
        } finally {
            // Guarantee that the intent's data is cleared once handled so onResume cannot re-trigger it.
            intent?.data = null
        }
    }

    @Synchronized
    private fun handleCancelLogin(result: MethodChannel.Result) {
        val wasActive = (pendingLoginResult != null || sessionState == TelegramAuthSessionState.WAITING_FOR_CALLBACK)
        Log.i(TAG, "[TELEGRAM_LOGIN_CANCEL] ${activityDiagnostics()} attemptId=$activeAttemptId wasActive=$wasActive")
        pendingLoginResult?.error("CANCELLED", "Telegram login cancelled by user", null)
        resetState()
        result.success(wasActive)
    }

    private fun resetState() {
        sessionState = TelegramAuthSessionState.IDLE
        pendingLoginResult = null
        activeAttemptId = null
        activeRole = null
        activeClientId = null
        activeRedirectUri = null
    }

    private fun roleForCallback(uri: Uri): String? = when (uri.host) {
        "app1859875063-login.tg.dev" -> "CUSTOMER"
        "app1074067825-login.tg.dev" -> "TECHNICIAN"
        else -> null
    }
}
