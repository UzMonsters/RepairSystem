import 'package:telegram_login/telegram_login.dart';

const telegramCustomerClientId = String.fromEnvironment(
  'TELEGRAM_CUSTOMER_CLIENT_ID',
  defaultValue: String.fromEnvironment(
    'APP_TELEGRAM_CUSTOMER_LOGIN_CLIENT_ID',
  ),
);
const telegramTechnicianClientId = String.fromEnvironment(
  'TELEGRAM_TECHNICIAN_CLIENT_ID',
  defaultValue: String.fromEnvironment(
    'APP_TELEGRAM_TECHNICIAN_LOGIN_CLIENT_ID',
  ),
);

/// Telegram's native Login SDK returns a signed OIDC id_token.
/// The token is sent to the backend and is never decoded or trusted locally.
class TelegramAuthService {
  final TelegramLogin _telegram = TelegramLogin();

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

    // BotFather provisions this native-app URL for the client ID.
    final redirectUri = 'https://app${clientId}-login.tg.dev';
    await _telegram.configure(
      TelegramLoginConfiguration(
        clientId: clientId,
        redirectUri: redirectUri,
        scopes: const ['openid', 'profile'],
      ),
    );
    final result = await _telegram.login();
    return result.idToken;
  }

  Future<bool> cancel() => _telegram.cancelLogin();
}
