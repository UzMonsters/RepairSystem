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
    defaultValue: 'https://app2962537527-login.tg.dev/tglogin',
  ),
);
const telegramTechnicianRedirectUri = String.fromEnvironment(
  'TELEGRAM_TECHNICIAN_REDIRECT_URI',
  defaultValue: String.fromEnvironment(
    'APP_TELEGRAM_TECHNICIAN_LOGIN_APP_URL',
    defaultValue: 'https://app2657113889-login.tg.dev/tglogin',
  ),
);

/// Telegram's native Login SDK returns a signed OIDC id_token.
/// The token is sent to the backend and is never decoded or trusted locally.
class TelegramAuthService {
  static const _channel = MethodChannel('repair_auto/telegram_auth');

  Future<String?> pendingRole() => _channel.invokeMethod<String>('pendingRole');

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

    // BotFather provisions this native-app URL for the client ID. The plugin
    // The official Android SDK expects Telegram's native App Link callback path.
    final configuredRedirectUri = role == 'TECHNICIAN'
        ? telegramTechnicianRedirectUri
        : telegramCustomerRedirectUri;
    final redirectUri = configuredRedirectUri.isNotEmpty
        ? configuredRedirectUri
        : 'https://app${clientId}-login.tg.dev/tglogin';
    MobileLog.info(
      'Telegram native login started role=$role clientId=$clientId redirectHost=${Uri.parse(redirectUri).host}',
    );
    late final String? idToken;
    try {
      idToken = await _channel.invokeMethod<String>('login', {
        'clientId': clientId,
        'redirectUri': redirectUri,
        'scopes': const ['profile'],
      });
    } catch (error, stackTrace) {
      MobileLog.severe(
        'Telegram native login failed role=$role error=${error.runtimeType}',
        error: error,
        stackTrace: stackTrace,
      );
      rethrow;
    }
    if (idToken == null || idToken.isEmpty) {
      MobileLog.warning('Telegram native login returned empty token role=$role');
      throw StateError('Telegram did not return an ID token.');
    }
    MobileLog.info(
      'Telegram native login returned token role=$role tokenLength=${MobileLog.safeLength(idToken)}',
    );
    return idToken;
  }

  Future<bool> cancel() async {
    final cancelled = await _channel.invokeMethod<bool>('cancel') ?? false;
    MobileLog.info('Telegram native login cancel requested cancelled=$cancelled');
    return cancelled;
  }
}
