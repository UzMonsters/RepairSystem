import 'package:flutter/services.dart';

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
    final idToken = await _channel.invokeMethod<String>('login', {
      'clientId': clientId,
      'redirectUri': redirectUri,
      'scopes': const ['profile'],
    });
    if (idToken == null || idToken.isEmpty) {
      throw StateError('Telegram did not return an ID token.');
    }
    return idToken;
  }

  Future<bool> cancel() async {
    return await _channel.invokeMethod<bool>('cancel') ?? false;
  }
}
