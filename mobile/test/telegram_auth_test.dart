import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:repair_mobile/main.dart';
import 'package:repair_mobile/telegram_auth.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('TelegramAuthService unit tests', () {
    late List<MethodCall> channelCalls;
    late String? mockIdTokenToReturn;
    late Exception? mockExceptionToThrow;

    setUp(() {
      channelCalls = [];
      mockIdTokenToReturn = 'mock.jwt.token';
      mockExceptionToThrow = null;

      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        const MethodChannel('repair_auto/telegram_auth'),
        (MethodCall methodCall) async {
          channelCalls.add(methodCall);
          if (mockExceptionToThrow != null) {
            throw mockExceptionToThrow!;
          }
          if (methodCall.method == 'login') {
            return mockIdTokenToReturn;
          }
          if (methodCall.method == 'cancel') {
            return true;
          }
          return null;
        },
      );
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        const MethodChannel('repair_auto/telegram_auth'),
        null,
      );
    });

    test('Customer role invokes channel with customer client configuration', () async {
      final service = TelegramAuthService();
      final token = await service.login('CUSTOMER');

      expect(token, 'mock.jwt.token');
      expect(channelCalls.length, 1);
      final call = channelCalls.first;
      expect(call.method, 'login');
      expect(call.arguments['role'], 'CUSTOMER');
      expect(call.arguments['clientId'], '8957154846');
      expect(call.arguments['redirectUri'], 'https://app1859875063-login.tg.dev/tglogin');
      expect(call.arguments['scopes'], ['profile']);
    });

    test('Technician role invokes channel with technician client configuration', () async {
      final service = TelegramAuthService();
      final token = await service.login('TECHNICIAN');

      expect(token, 'mock.jwt.token');
      expect(channelCalls.length, 1);
      final call = channelCalls.first;
      expect(call.method, 'login');
      expect(call.arguments['role'], 'TECHNICIAN');
      expect(call.arguments['clientId'], '8854105729');
      expect(call.arguments['redirectUri'], 'https://app1074067825-login.tg.dev/tglogin');
      expect(call.arguments['scopes'], ['profile']);
    });

    test('Throws StateError when native SDK returns empty token', () async {
      mockIdTokenToReturn = '';
      final service = TelegramAuthService();

      expect(() => service.login('CUSTOMER'), throwsA(isA<StateError>()));
    });

    test('Rethrows PlatformException on native failure', () async {
      mockExceptionToThrow = PlatformException(
        code: 'TELEGRAM_CALLBACK_FAILED',
        message: 'No active login session. Call startLogin() first.',
      );
      final service = TelegramAuthService();

      expect(
        () => service.login('CUSTOMER'),
        throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'TELEGRAM_CALLBACK_FAILED')),
      );
    });

    test('Cancel invokes cancel method on platform channel', () async {
      final service = TelegramAuthService();
      final result = await service.cancel();

      expect(result, isTrue);
      expect(channelCalls.any((c) => c.method == 'cancel'), isTrue);
    });
  });

  group('LoginPage Telegram UI & State Tests', () {
    testWidgets('Single-flight protection prevents duplicate concurrent login invocations', (tester) async {
      int telegramLoginCalls = 0;
      final completer = Completer<String>();

      await tester.pumpWidget(
        MaterialApp(
          home: LoginPage(
            onLogin: (_, _) async {},
            onTelegramLogin: (role) {
              telegramLoginCalls++;
              return completer.future;
            },
            onRequestPhoneOtp: (_, _) async => 'challenge-1',
            onVerifyPhoneOtp: (_, _, _) async {},
            loading: false,
          ),
        ),
      );

      // Find the submit button (FilledButton)
      final submitButton = find.byType(FilledButton);
      expect(submitButton, findsOneWidget);

      // First tap
      await tester.tap(submitButton);
      await tester.pump();

      expect(telegramLoginCalls, 1);

      // Second tap while login is in progress
      await tester.tap(submitButton);
      await tester.pump();

      // Should still be 1 (ignored duplicate tap)
      expect(telegramLoginCalls, 1);

      // Resolve login
      completer.complete('token-123');
      await tester.pumpAndSettle();
    });

    testWidgets('PlatformException maps to localized user-friendly error without raw platform strings', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: LoginPage(
            onLogin: (_, _) async {},
            onTelegramLogin: (role) async {
              throw PlatformException(
                code: 'TELEGRAM_CALLBACK_FAILED',
                message: 'No active login session. Call startLogin() first.',
              );
            },
            onRequestPhoneOtp: (_, _) async => 'challenge-1',
            onVerifyPhoneOtp: (_, _, _) async {},
            loading: false,
          ),
        ),
      );

      final submitButton = find.byType(FilledButton);
      await tester.tap(submitButton);
      await tester.pumpAndSettle();

      // In Russian (default), session expired maps to user-friendly message
      expect(find.text('Сессия входа через Telegram истекла. Попробуйте ещё раз.'), findsOneWidget);
      // Raw technical message must NOT appear
      expect(find.textContaining('No active login session'), findsNothing);
      expect(find.textContaining('PlatformException'), findsNothing);
    });

    testWidgets('Cancelled login maps to localized cancellation message', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: LoginPage(
            onLogin: (_, _) async {},
            onTelegramLogin: (role) async {
              throw PlatformException(
                code: 'CANCELLED',
                message: 'Telegram login cancelled by user',
              );
            },
            onRequestPhoneOtp: (_, _) async => 'challenge-1',
            onVerifyPhoneOtp: (_, _, _) async {},
            loading: false,
          ),
        ),
      );

      final submitButton = find.byType(FilledButton);
      await tester.tap(submitButton);
      await tester.pumpAndSettle();

      expect(find.text('Вход через Telegram отменен.'), findsOneWidget);
    });

    testWidgets('Successful token triggers onLogin exactly once', (tester) async {
      int backendLoginCalls = 0;
      String? receivedRole;
      String? receivedToken;

      await tester.pumpWidget(
        MaterialApp(
          home: LoginPage(
            onLogin: (role, token) async {
              backendLoginCalls++;
              receivedRole = role;
              receivedToken = token;
            },
            onTelegramLogin: (role) async => 'valid-oidc-id-token',
            onRequestPhoneOtp: (_, _) async => 'challenge-1',
            onVerifyPhoneOtp: (_, _, _) async {},
            loading: false,
          ),
        ),
      );

      final submitButton = find.byType(FilledButton);
      await tester.tap(submitButton);
      await tester.pumpAndSettle();

      expect(backendLoginCalls, 1);
      expect(receivedRole, 'CUSTOMER');
      expect(receivedToken, 'valid-oidc-id-token');
    });

    testWidgets('Failed Telegram auth does NOT invoke backend onLogin', (tester) async {
      int backendLoginCalls = 0;

      await tester.pumpWidget(
        MaterialApp(
          home: LoginPage(
            onLogin: (role, token) async {
              backendLoginCalls++;
            },
            onTelegramLogin: (role) async {
              throw PlatformException(code: 'TELEGRAM_LOGIN_FAILED');
            },
            onRequestPhoneOtp: (_, _) async => 'challenge-1',
            onVerifyPhoneOtp: (_, _, _) async {},
            loading: false,
          ),
        ),
      );

      final submitButton = find.byType(FilledButton);
      await tester.tap(submitButton);
      await tester.pumpAndSettle();

      expect(backendLoginCalls, 0);
      expect(find.text('Не удалось войти через Telegram. Попробуйте ещё раз.'), findsOneWidget);
    });

    testWidgets('Role toggle resets error message state', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: LoginPage(
            onLogin: (_, _) async {},
            onTelegramLogin: (role) async {
              throw PlatformException(code: 'TELEGRAM_LOGIN_FAILED');
            },
            onRequestPhoneOtp: (_, _) async => 'challenge-1',
            onVerifyPhoneOtp: (_, _, _) async {},
            loading: false,
          ),
        ),
      );

      // Trigger error
      final submitButton = find.byType(FilledButton);
      await tester.tap(submitButton);
      await tester.pumpAndSettle();
      expect(find.text('Не удалось войти через Telegram. Попробуйте ещё раз.'), findsOneWidget);

      // Toggle to Technician
      await tester.tap(find.text('Техник'));
      await tester.pumpAndSettle();

      // Error should be cleared
      expect(find.text('Не удалось войти через Telegram. Попробуйте ещё раз.'), findsNothing);
    });
  });
}
