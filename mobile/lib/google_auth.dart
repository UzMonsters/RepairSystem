import 'dart:io';

import 'package:google_sign_in/google_sign_in.dart';

import 'mobile_logger.dart';

class GoogleAuthService {
  GoogleAuthService({GoogleSignIn? signIn})
    : _signIn = signIn ?? GoogleSignIn.instance;

  static const _serverClientId = String.fromEnvironment(
    'GOOGLE_SERVER_CLIENT_ID',
  );

  final GoogleSignIn _signIn;
  Future<void>? _initialization;

  Future<void> _ensureInitialized() {
    return _initialization ??= _signIn.initialize(
      serverClientId: _serverClientId.isEmpty ? null : _serverClientId,
    );
  }

  Future<String> login(String role) async {
    if (!Platform.isAndroid) {
      throw UnsupportedError('Google sign-in is configured for Android only.');
    }

    MobileLog.info(
      '[GOOGLE_LOGIN_START] role=$role serverClientIdConfigured=${_serverClientId.isNotEmpty}',
    );
    await _ensureInitialized();

    if (!_signIn.supportsAuthenticate()) {
      throw StateError('Google sign-in is not available on this device.');
    }

    final account = await _signIn.authenticate(
      scopeHint: const ['email', 'profile'],
    );
    final idToken = account.authentication.idToken;
    if (idToken == null || idToken.isEmpty) {
      throw StateError('Google sign-in did not return an ID token.');
    }

    MobileLog.info(
      '[GOOGLE_LOGIN_SUCCESS] role=$role tokenPresent=true tokenLength=${MobileLog.safeLength(idToken)}',
    );
    return idToken;
  }
}
