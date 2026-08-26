package com.example.mobile

import android.content.Intent
import android.net.Uri
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.android.FlutterActivity
import org.telegram.login.TelegramLogin

class MainActivity : FlutterActivity() {
    private val channelName = "repair_auto/telegram_auth"
    private var pendingLogin: MethodChannel.Result? = null
    private var handledCallback: String? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let(::handleTelegramCallback)
    }

    override fun configureFlutterEngine(
        flutterEngine: io.flutter.embedding.engine.FlutterEngine,
    ) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "login" -> startTelegramLogin(call, result)
                    "cancel" -> {
                        val pending = pendingLogin
                        pendingLogin = null
                        pending?.error("CANCELLED", "Telegram login cancelled", null)
                        result.success(pending != null)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun startTelegramLogin(call: MethodCall, result: MethodChannel.Result) {
        if (pendingLogin != null) {
            // A browser return can arrive after Android recreated the activity
            // without delivering the previous result. Release the stale request
            // so the user is not permanently locked out of Telegram login.
            pendingLogin?.error("LOGIN_REPLACED", "Previous Telegram login was reset", null)
            pendingLogin = null
        }
        val clientId = call.argument<String>("clientId")
        val redirectUri = call.argument<String>("redirectUri")
        val scopes = call.argument<List<String>>("scopes") ?: listOf("profile")
        if (clientId.isNullOrBlank() || redirectUri.isNullOrBlank()) {
            result.error("INVALID_CONFIGURATION", "Telegram clientId and redirectUri are required", null)
            return
        }

        pendingLogin = result
        TelegramLogin.init(clientId, redirectUri, scopes)
        TelegramLogin.startLogin(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(::handleTelegramCallback)
    }

    override fun onResume() {
        super.onResume()
        // Some Android versions deliver an App Link while resuming the
        // existing activity instead of calling onNewIntent.
        intent?.data?.let(::handleTelegramCallback)
    }

    private fun handleTelegramCallback(uri: Uri) {
        if (uri.host?.endsWith("-login.tg.dev") != true) return
        val callbackKey = uri.toString()
        if (handledCallback == callbackKey) return
        handledCallback = callbackKey
        try {
            TelegramLogin.handleLoginResponse(
                uri,
                onSuccess = { loginData ->
                    pendingLogin?.success(loginData.idToken)
                    pendingLogin = null
                },
                onError = { error ->
                    pendingLogin?.error("TELEGRAM_LOGIN_FAILED", error.message, null)
                    pendingLogin = null
                },
            )
        } catch (error: Exception) {
            pendingLogin?.error("TELEGRAM_CALLBACK_FAILED", error.message, null)
            pendingLogin = null
        } finally {
            // Prevent onResume from processing the same deep link repeatedly.
            intent?.data = null
        }
    }
}
