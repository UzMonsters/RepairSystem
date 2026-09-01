import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_sign_in/google_sign_in.dart';

import 'api_client.dart';
<<<<<<< HEAD
=======
import 'google_auth.dart';
import 'mobile_logger.dart';
>>>>>>> 34738de22e72e7c92683512af93719228b8641e6
import 'models.dart';
import 'realtime_client.dart';
import 'repositories.dart';
import 'telegram_auth.dart';

part 'localization.dart';
part 'auth_page.dart';
part 'home_page.dart';
part 'customer_requests.dart';
part 'request_details.dart';
part 'mobile_chat.dart';
part 'technician_jobs.dart';
part 'schedule.dart';
part 'job_actions.dart';
part 'notifications.dart';
part 'profile.dart';

void main() => runApp(const RepairAutoApp());

class RepairAutoApp extends StatefulWidget {
  const RepairAutoApp({super.key});
  @override
  State<RepairAutoApp> createState() => _RepairAutoAppState();
}

class _RepairAutoAppState extends State<RepairAutoApp> {
  final api = ApiClient();
  final navigatorKey = GlobalKey<NavigatorState>();
  late final auth = AuthRepository(api);
  final telegram = TelegramAuthService();
  final google = GoogleAuthService();
  String? error;
  bool loading = false;

  Map<String, dynamic> _deviceContext() => {
    'platform': Platform.isAndroid ? 'ANDROID' : 'IOS',
    'appVersion': '1.0.0',
  };

  Future<void> login(String role, String idToken) async {
<<<<<<< HEAD
=======
    MobileLog.info(
      '[TELEGRAM_BACKEND_AUTH_START] role=$role tokenPresent=${MobileLog.present(idToken)} '
      'tokenLength=${MobileLog.safeLength(idToken)}',
    );
>>>>>>> 34738de22e72e7c92683512af93719228b8641e6
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final actor = role == 'CUSTOMER'
          ? await auth.loginCustomer(idToken, device: _deviceContext())
          : await auth.loginTechnician(idToken, device: _deviceContext());
      MobileLog.info(
        '[TELEGRAM_BACKEND_AUTH_SUCCESS] role=$role actorType=${actor.type} actorId=${actor.id}',
      );
      if (!mounted) return;
      navigatorKey.currentState?.pushReplacement(
        MaterialPageRoute(
          builder: (_) => HomePage(api: api, auth: auth, actor: actor),
        ),
      );
<<<<<<< HEAD
    } on ApiException catch (e) {
      setState(() => error = e.message);
    } catch (e) {
=======
      MobileLog.info(
        '[NAVIGATE_HOME] role=$role actorType=${actor.type} actorId=${actor.id}',
      );
    } on ApiException catch (e) {
      MobileLog.warning(
        '[TELEGRAM_BACKEND_AUTH_ERROR] API failure role=$role status=${e.statusCode} code=${e.code ?? 'unknown'}',
      );
      setState(() => error = e.message);
    } catch (e) {
      MobileLog.severe(
        '[TELEGRAM_BACKEND_AUTH_ERROR] unexpected failure role=$role error=${e.runtimeType}',
        error: e,
      );
>>>>>>> 34738de22e72e7c92683512af93719228b8641e6
      setState(() => error = '$e');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<String> loginWithTelegram(String role) => telegram.login(role);

  Future<void> loginWithGoogle(String role) async {
    MobileLog.info('[GOOGLE_BACKEND_AUTH_START] role=$role');
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final idToken = await google.login(role);
      final actor = await auth.loginGoogle(
        role == 'CUSTOMER' ? 'CUSTOMER_MOBILE' : 'TECHNICIAN_MOBILE',
        idToken,
        device: _deviceContext(),
      );
      MobileLog.info(
        '[GOOGLE_BACKEND_AUTH_SUCCESS] role=$role actorType=${actor.type} actorId=${actor.id}',
      );
      if (!mounted) return;
      navigatorKey.currentState?.pushReplacement(
        MaterialPageRoute(
          builder: (_) => HomePage(api: api, auth: auth, actor: actor),
        ),
      );
      MobileLog.info(
        '[NAVIGATE_HOME] role=$role actorType=${actor.type} actorId=${actor.id}',
      );
    } on ApiException catch (e) {
      MobileLog.warning(
        '[GOOGLE_BACKEND_AUTH_ERROR] API failure role=$role status=${e.statusCode} code=${e.code ?? 'unknown'}',
      );
      if (mounted) setState(() => error = e.message);
    } catch (e) {
      MobileLog.severe(
        '[GOOGLE_BACKEND_AUTH_ERROR] unexpected failure role=$role error=${e.runtimeType}',
        error: e,
      );
      rethrow;
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<String> requestPhoneOtp(String role, String phone) async {
    final data = await auth.requestPhoneOtp(
      clientType: role == 'CUSTOMER' ? 'CUSTOMER_MOBILE' : 'TECHNICIAN_MOBILE',
      phone: phone,
    );
<<<<<<< HEAD
    return data['challengeId'] as String;
  }

  Future<void> verifyPhoneOtp(String role, String challengeId, String code) async {
=======
    MobileLog.info(
      'Login flow phone OTP request completed challengeId=${data['challengeId']}',
    );
    return data['challengeId'] as String;
  }

  Future<void> verifyPhoneOtp(
    String role,
    String challengeId,
    String code,
  ) async {
    MobileLog.info(
      'Login flow phone OTP verify started role=$role challengeId=$challengeId '
      'codePresent=${MobileLog.present(code)} codeLength=${MobileLog.safeLength(code)}',
    );
>>>>>>> 34738de22e72e7c92683512af93719228b8641e6
    final actor = await auth.verifyPhoneOtp(challengeId, code);
    if (!mounted) return;
    navigatorKey.currentState?.pushReplacement(
      MaterialPageRoute(
        builder: (_) => HomePage(api: api, auth: auth, actor: actor),
      ),
    );
    MobileLog.info(
      'Login flow phone OTP verify completed role=$role actorType=${actor.type} actorId=${actor.id}',
    );
  }

  @override
  Widget build(BuildContext context) => MaterialApp(
    navigatorKey: navigatorKey,
    debugShowCheckedModeBanner: false,
    title: 'RepairAuto',
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff934316)),
      useMaterial3: true,
    ),
    home: LoginPage(
      onLogin: login,
      onTelegramLogin: loginWithTelegram,
<<<<<<< HEAD
      onPendingTelegramRole: telegram.pendingRole,
=======
      onGoogleLogin: loginWithGoogle,
>>>>>>> 34738de22e72e7c92683512af93719228b8641e6
      onRequestPhoneOtp: requestPhoneOtp,
      onVerifyPhoneOtp: verifyPhoneOtp,
      loading: loading,
      error: error,
    ),
  );
}
