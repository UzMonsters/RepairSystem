import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:geolocator/geolocator.dart';

import 'api_client.dart';
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
  String? error;
  bool loading = false;

  Map<String, dynamic> _deviceContext() => {
    'platform': Platform.isAndroid ? 'ANDROID' : 'IOS',
    'appVersion': '1.0.0',
  };

  Future<void> login(String role, String idToken) async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final actor = role == 'CUSTOMER'
          ? await auth.loginCustomer(idToken, device: _deviceContext())
          : await auth.loginTechnician(idToken, device: _deviceContext());
      if (!mounted) return;
      navigatorKey.currentState?.pushReplacement(
        MaterialPageRoute(
          builder: (_) => HomePage(api: api, auth: auth, actor: actor),
        ),
      );
    } on ApiException catch (e) {
      setState(() => error = e.message);
    } catch (e) {
      setState(() => error = '$e');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<String> loginWithTelegram(String role) => telegram.login(role);

  Future<String> requestPhoneOtp(String role, String phone) async {
    final data = await auth.requestPhoneOtp(
      clientType: role == 'CUSTOMER' ? 'CUSTOMER_MOBILE' : 'TECHNICIAN_MOBILE',
      phone: phone,
    );
    return data['challengeId'] as String;
  }

  Future<void> verifyPhoneOtp(String role, String challengeId, String code) async {
    final actor = await auth.verifyPhoneOtp(challengeId, code);
    if (!mounted) return;
    navigatorKey.currentState?.pushReplacement(
      MaterialPageRoute(builder: (_) => HomePage(api: api, auth: auth, actor: actor)),
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
      onPendingTelegramRole: telegram.pendingRole,
      onRequestPhoneOtp: requestPhoneOtp,
      onVerifyPhoneOtp: verifyPhoneOtp,
      loading: loading,
      error: error,
    ),
  );
}
