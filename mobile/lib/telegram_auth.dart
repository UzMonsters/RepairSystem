import 'package:flutter/services.dart';

import 'mobile_logger.dart';

const telegramCustomerClientId = String.fromEnvironment(
  'TELEGRAM_CUSTOMER_CLIENT_ID',
  defaultValue: String.fromEnvironment(
    'APP_TELEGRAM_CUSTOMER_LOGIN_CLIENT_ID',
    defaultValue: '8957154846',
  ),
);
const telegramTechnicianClientId = String.fromEnvironment(
  'TELEGRAM_TECHNICIAN_CLIENT_ID',
  defaultValue: String.fromEnvironment(
    'APP_TELEGRAM_TECHNICIAN_LOGIN_CLIENT_ID',
    defaultValue: '8854105729',
  ),
);
const telegramCustomerRedirectUri = String.fromEnvironment(
  'TELEGRAM_CUSTOMER_REDIRECT_URI',
  defaultValue: String.fromEnvironment(
    'APP_TELEGRAM_CUSTOMER_LOGIN_APP_URL',
    defaultValue: 'https://app1859875063-login.tg.dev/tglogin',
  ),
);
const telegramTechnicianRedirectUri = String.fromEnvironment(
  'TELEGRAM_TECHNICIAN_REDIRECT_URI',
  defaultValue: String.fromEnvironment(
    'APP_TELEGRAM_TECHNICIAN_LOGIN_APP_URL',
    defaultValue: 'https://app1074067825-login.tg.dev/tglogin',
  ),
);

/// Telegram's native Login SDK returns a signed OIDC id_token.
/// The token is sent to the backend and is never decoded or trusted locally.
class TelegramAuthService {
  static const _channel = MethodChannel('repair_auto/telegram_auth');

  Future<String> login(String role) async {
    final clientId = role == 'TECHNICIAN'
        ? telegramTechnicianClientId
        : telegramCustomerClientId;
    if (clientId.isEmpty) {
      throw StateError(
        'Telegram Client ID is missing. Build with '
        '--dart-define=TELEGRAM_${role == 'TECHNICIAN' ? 'TECHNICIAN' : 'CUSTOMER'}_CLIENT_ID=...',
      );
    }

    final configuredRedirectUri = role == 'TECHNICIAN'
        ? telegramTechnicianRedirectUri
        : telegramCustomerRedirectUri;
    final redirectUri = configuredRedirectUri.isNotEmpty
        ? _normalizeTelegramRedirectUri(configuredRedirectUri)
        : (role == 'TECHNICIAN'
            ? 'https://app1074067825-login.tg.dev/tglogin'
            : 'https://app1859875063-login.tg.dev/tglogin');

    MobileLog.info(
      '[TELEGRAM_LOGIN_START] role=$role clientId=$clientId redirectHost=${Uri.tryParse(redirectUri)?.host ?? 'unknown'}',
    );

    late final String? idToken;
    try {
      idToken = await _channel.invokeMethod<String>('login', {
        'role': role,
        'clientId': clientId,
        'redirectUri': redirectUri,
        'scopes': const ['profile'],
      });
    } catch (error, stackTrace) {
      MobileLog.severe(
        '[TELEGRAM_LOGIN_ERROR] role=$role error=${error.runtimeType}',
        error: error,
        stackTrace: stackTrace,
      );
      rethrow;
    }

    if (idToken == null || idToken.isEmpty) {
      MobileLog.warning('[TELEGRAM_LOGIN_ERROR] role=$role reason=empty_token_returned');
      throw StateError('Telegram did not return an ID token.');
    }

    MobileLog.info(
      '[TELEGRAM_LOGIN_SUCCESS] role=$role tokenPresent=true tokenLength=${MobileLog.safeLength(idToken)}',
    );
    return idToken;
  }

  Future<bool> cancel() async {
    MobileLog.info('[TELEGRAM_LOGIN_CANCEL] cancel requested');
    final cancelled = await _channel.invokeMethod<bool>('cancel') ?? false;
    MobileLog.info('[TELEGRAM_LOGIN_CANCEL] cancel response=$cancelled');
    return cancelled;
  }

  String _normalizeTelegramRedirectUri(String value) {
    final uri = Uri.tryParse(value.trim());
    if (uri == null || uri.path.isNotEmpty) {
      return value.trim();
    }
    return uri.replace(path: '/tglogin').toString();
  }
}
