import 'dart:io';

import 'package:google_sign_in/google_sign_in.dart';

import 'mobile_logger.dart';

class GoogleAuthService {
  GoogleAuthService({GoogleSignIn? signIn})
    : _signIn =
          signIn ??
          GoogleSignIn(
            scopes: const ['email', 'profile'],
            serverClientId: _serverClientId.isEmpty ? null : _serverClientId,
          );

  static const _serverClientId = String.fromEnvironment(
    'GOOGLE_SERVER_CLIENT_ID',
    defaultValue: '1064569665321-rscanna9c4jc6o9jverqs6o3t1j91c67.apps.googleusercontent.com',
  );

  final GoogleSignIn _signIn;

  Future<String> login(String role) async {
    if (!Platform.isAndroid) {
      throw UnsupportedError('Google sign-in is configured for Android only.');
    }
    if (_serverClientId.isEmpty) {
      throw StateError(
        'Google sign-in requires GOOGLE_SERVER_CLIENT_ID for Android builds.',
      );
    }

    MobileLog.info(
      '[GOOGLE_LOGIN_START] role=$role serverClientIdConfigured=${_serverClientId.isNotEmpty}',
    );

    await _signIn.signOut();
    final account = await _signIn.signIn();
    if (account == null) {
      throw const GoogleSignInCancelledException();
    }
    final authentication = await account.authentication;
    final idToken = authentication.idToken;
    if (idToken == null || idToken.isEmpty) {
      throw StateError('Google sign-in did not return an ID token.');
    }

    MobileLog.info(
      '[GOOGLE_LOGIN_SUCCESS] role=$role tokenPresent=true tokenLength=${MobileLog.safeLength(idToken)}',
    );
    return idToken;
  }
}

class GoogleSignInCancelledException implements Exception {
  const GoogleSignInCancelledException();
}
