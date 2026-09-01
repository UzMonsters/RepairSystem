part of 'main.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({
    super.key,
    required this.onLogin,
    required this.onTelegramLogin,
    required this.onGoogleLogin,
    required this.onPendingTelegramRole,
    required this.onRequestPhoneOtp,
    required this.onVerifyPhoneOtp,
    required this.loading,
    this.error,
  });

  final Future<void> Function(String role, String idToken) onLogin;
  final Future<String> Function(String role) onTelegramLogin;
  final Future<void> Function(String role) onGoogleLogin;
  final Future<String?> Function() onPendingTelegramRole;
  final Future<String> Function(String role, String phone) onRequestPhoneOtp;
  final Future<void> Function(String role, String challengeId, String code)
  onVerifyPhoneOtp;
  final bool loading;
  final String? error;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final phone = TextEditingController();
  final name = TextEditingController();
  String role = 'CUSTOMER';
  String customerMethod = 'TELEGRAM';
  bool register = false;
  bool telegramLoginInProgress = false;
  bool googleLoginInProgress = false;
  String language = 'ru';
  String? localError;

  String tr(String key) {
    const values = {
      'ru': {
        'customer': 'Клиент',
        'technician': 'Техник',
        'signIn': 'Войти',
        'register': 'Регистрация',
        'phone': 'Телефон',
        'fullName': 'Имя и фамилия',
        'phoneNumber': 'Номер телефона',
        'google': 'Google',
        'telegram': 'Telegram',
        'continueGoogle': 'Продолжить через Google',
        'continueTelegram': 'Продолжить через Telegram',
        'anotherWay': 'Другой способ входа',
        'telegramFailed':
            'Не удалось войти через Telegram. Попробуйте ещё раз.',
        'telegramCancelled': 'Вход через Telegram отменен.',
        'telegramExpired':
            'Сессия входа через Telegram истекла. Попробуйте ещё раз.',
        'googleFailed': 'Не удалось войти через Google. Попробуйте ещё раз.',
        'googleCancelled': 'Вход через Google отменен.',
        'googleConfiguration': 'Google вход настроен неправильно. Проверьте OAuth client ID, package name и SHA-1.',
        'googleUnavailable':
            'Google вход пока доступен только в Android приложении.',
      },
      'uz': {
        'customer': 'Mijoz',
        'technician': 'Texnik',
        'signIn': 'Kirish',
        'register': 'Ro‘yxatdan o‘tish',
        'phone': 'Telefon',
        'fullName': 'Ism va familiya',
        'phoneNumber': 'Telefon raqami',
        'google': 'Google',
        'telegram': 'Telegram',
        'continueGoogle': 'Google orqali davom etish',
        'continueTelegram': 'Telegram orqali davom etish',
        'anotherWay': 'Boshqa kirish usuli',
        'telegramFailed':
            'Telegram orqali kirish amalga oshmadi. Qayta urinib ko‘ring.',
        'telegramCancelled': 'Telegram orqali kirish bekor qilindi.',
        'telegramExpired':
            'Telegram sessiyasi muddati tugadi. Qayta urinib ko‘ring.',
        'googleFailed':
            'Google orqali kirish amalga oshmadi. Qayta urinib ko‘ring.',
        'googleCancelled': 'Google orqali kirish bekor qilindi.',
        'googleConfiguration': 'Google orqali kirish noto‘g‘ri sozlangan. OAuth client ID, package name va SHA-1 ni tekshiring.',
        'googleUnavailable':
            'Google orqali kirish hozircha faqat Android ilovasida mavjud.',
      },
      'en': {
        'customer': 'Customer',
        'technician': 'Technician',
        'signIn': 'Sign in',
        'register': 'Register',
        'phone': 'Phone',
        'fullName': 'Full name',
        'phoneNumber': 'Phone number',
        'google': 'Google',
        'telegram': 'Telegram',
        'continueGoogle': 'Continue with Google',
        'continueTelegram': 'Continue with Telegram',
        'anotherWay': 'Sign in another way',
        'telegramFailed': 'Telegram sign-in failed. Please try again.',
        'telegramCancelled': 'Telegram sign-in cancelled.',
        'telegramExpired': 'Telegram login session expired. Please try again.',
        'googleFailed': 'Google sign-in failed. Please try again.',
        'googleCancelled': 'Google sign-in cancelled.',
        'googleConfiguration': 'Google sign-in is not configured correctly. Check OAuth client ID, package name, and SHA-1.',
        'googleUnavailable':
            'Google sign-in is only available in the Android app for now.',
      },
    };
    return values[language]?[key] ?? values['en']![key] ?? key;
  }

  @override
  void initState() {
    super.initState();
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
        systemNavigationBarColor: Color(0xfffff8f5),
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
    );
    _resumePendingTelegramLogin();
  }

  Future<void> _resumePendingTelegramLogin() async {
    try {
      final pendingRole = await widget.onPendingTelegramRole();
      if (!mounted || pendingRole == null) return;
      setState(() {
        role = pendingRole;
        customerMethod = 'TELEGRAM';
      });
      await _runTelegramLogin(pendingRole);
    } catch (e) {
      if (mounted) {
        setState(() => localError = _telegramErrorMessage(e));
      }
    }
  }

  @override
  void dispose() {
    phone.dispose();
    name.dispose();
    super.dispose();
  }

  void showUnavailable() {
    setState(
      () => localError = 'This authentication method is not configured yet.',
    );
  }

  Future<void> _runTelegramLogin(String selectedRole) async {
    if (telegramLoginInProgress) return;
    setState(() {
      telegramLoginInProgress = true;
      localError = null;
    });
    try {
      final idToken = await widget.onTelegramLogin(selectedRole);
      if (mounted) await widget.onLogin(selectedRole, idToken);
    } catch (e) {
      if (!mounted) return;
      setState(() => localError = _telegramErrorMessage(e));
    } finally {
      if (mounted) {
        setState(() => telegramLoginInProgress = false);
      }
    }
  }

  Future<void> _runGoogleLogin(String selectedRole) async {
    if (googleLoginInProgress) return;
    setState(() {
      googleLoginInProgress = true;
      localError = null;
    });
    try {
      await widget.onGoogleLogin(selectedRole);
    } catch (e) {
      if (!mounted) return;
      setState(() => localError = _googleErrorMessage(e));
    } finally {
      if (mounted) {
        setState(() => googleLoginInProgress = false);
      }
    }
  }

  String _telegramErrorMessage(Object error) {
    if (error is PlatformException) {
      if (error.code == 'CANCELLED') {
        return tr('telegramCancelled');
      }
      if (error.code == 'TELEGRAM_SESSION_EXPIRED' ||
          error.code == 'LOGIN_REPLACED' ||
          (error.message?.contains('No active login session') ?? false)) {
        return tr('telegramExpired');
      }
      return tr('telegramFailed');
    }
    return error.toString().replaceFirst('StateError: ', '');
  }

  String _googleErrorMessage(Object error) {
    if (error is GoogleSignInException) {
      if (error.code == GoogleSignInExceptionCode.canceled ||
          error.code == GoogleSignInExceptionCode.interrupted) {
        return tr('googleCancelled');
      }
      if (error.code == GoogleSignInExceptionCode.uiUnavailable) {
        return tr('googleUnavailable');
      }
      if (error.code == GoogleSignInExceptionCode.clientConfigurationError) {
        return tr('googleConfiguration');
      }
      final description = error.description;
      if (description != null && description.isNotEmpty) {
        return '${tr('googleFailed')} ($description)';
      }
    }
    if (error is UnsupportedError) {
      return tr('googleUnavailable');
    }
    if (error is StateError) {
      return tr('googleConfiguration');
    }
    return tr('googleFailed');
  }

  Future<void> submit() async {
    setState(() => localError = null);
    if (customerMethod == 'TELEGRAM') {
      await _runTelegramLogin(role);
      return;
    }
    if (customerMethod == 'GOOGLE') {
      await _runGoogleLogin(role);
      return;
    }
    final phoneValue = phone.text.trim();
    if (phoneValue.isEmpty) {
      showUnavailable();
      return;
    }
    final challengeId = await widget.onRequestPhoneOtp(role, phoneValue);
    if (!mounted) return;
    final codeController = TextEditingController();
    final verified = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Verify phone'),
        content: TextField(
          controller: codeController,
          keyboardType: TextInputType.number,
          maxLength: 6,
          decoration: const InputDecoration(labelText: '6-digit code'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Verify'),
          ),
        ],
      ),
    );
    if (verified == true && codeController.text.trim().isNotEmpty) {
      await widget.onVerifyPhoneOtp(
        role,
        challengeId,
        codeController.text.trim(),
      );
    }
    codeController.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: const Color(0xfffff8f5),
    body: DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xfffffcfa), Color(0xfffff8f5), Color(0xfffff1eb)],
          stops: [0, .52, 1],
        ),
      ),
      child: SafeArea(
        child: Stack(
          children: [
            Positioned(
              top: 8,
              right: 16,
              child: PopupMenuButton<String>(
                initialValue: language,
                offset: const Offset(0, 42),
                onSelected: (value) => setState(() => language = value),
                itemBuilder: (_) => const [
                  PopupMenuItem(value: 'ru', child: Text('RU')),
                  PopupMenuItem(value: 'uz', child: Text('UZ')),
                  PopupMenuItem(value: 'en', child: Text('EN')),
                ],
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: Theme.of(context)
                        .colorScheme
                        .surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(language.toUpperCase()),
                      const SizedBox(width: 4),
                      const Icon(Icons.keyboard_arrow_down, size: 18),
                    ],
                  ),
                ),
              ),
            ),
            Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 440),
                  child: Card(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const Icon(
                            Icons.build_circle,
                            size: 64,
                            color: Color(0xff934316),
                          ),
                          const SizedBox(height: 12),
                          const Text(
                            'RepairAuto',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 28,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 24),
                          SegmentedButton<String>(
                            segments: [
                              ButtonSegment(
                                value: 'CUSTOMER',
                                label: Text(tr('customer')),
                              ),
                              ButtonSegment(
                                value: 'TECHNICIAN',
                                label: Text(tr('technician')),
                              ),
                            ],
                            selected: {role},
                            onSelectionChanged: (value) => setState(() {
                              role = value.first;
                              localError = null;
                              if (role == 'TECHNICIAN' &&
                                  customerMethod == 'PHONE') {
                                customerMethod = 'TELEGRAM';
                                register = false;
                              }
                            }),
                          ),
                          const SizedBox(height: 16),
                          if (role == 'CUSTOMER') ...[
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    register
                                        ? tr('register')
                                        : '${tr('customer')} ${tr('signIn')}',
                                  ),
                                ),
                                Switch(
                                  value: register,
                                  onChanged: (value) =>
                                      setState(() => register = value),
                                ),
                              ],
                            ),
                          ],
                          if (customerMethod == 'TELEGRAM' ||
                              customerMethod == 'GOOGLE')
                            Card(
                              margin: EdgeInsets.zero,
                              color: Theme.of(context)
                                  .colorScheme
                                  .primaryContainer,
                              child: ListTile(
                                leading: Icon(
                                  customerMethod == 'GOOGLE'
                                      ? Icons.account_circle_outlined
                                      : Icons.telegram,
                                ),
                                title: Text(
                                  customerMethod == 'GOOGLE'
                                      ? tr('continueGoogle')
                                      : tr('continueTelegram'),
                                ),
                                subtitle: const Text(
                                  'Secure sign-in for your account',
                                ),
                              ),
                            ),
                          if (role == 'CUSTOMER' &&
                              customerMethod == 'PHONE') ...[
                            if (register) ...[
                              const SizedBox(height: 12),
                              TextField(
                                controller: name,
                                decoration: InputDecoration(
                                  labelText: tr('fullName'),
                                ),
                              ),
                            ],
                            const SizedBox(height: 12),
                            TextField(
                              controller: phone,
                              keyboardType: TextInputType.phone,
                              decoration: InputDecoration(
                                labelText: tr('phoneNumber'),
                              ),
                            ),
                          ],
                          if (widget.error != null || localError != null) ...[
                            const SizedBox(height: 12),
                            Text(
                              localError ?? widget.error!,
                              style: const TextStyle(color: Colors.red),
                            ),
                          ],
                          const SizedBox(height: 18),
                          FilledButton.icon(
                            onPressed:
                                widget.loading ||
                                    telegramLoginInProgress ||
                                    googleLoginInProgress
                                ? null
                                : submit,
                            icon: Icon(
                              customerMethod == 'TELEGRAM' ||
                                      customerMethod == 'GOOGLE'
                                  ? customerMethod == 'GOOGLE'
                                        ? Icons.account_circle_outlined
                                        : Icons.telegram
                                  : register
                                  ? Icons.person_add
                                  : Icons.login,
                            ),
                            label:
                                widget.loading ||
                                    telegramLoginInProgress ||
                                    googleLoginInProgress
                                ? const CircularProgressIndicator()
                                : Text(
                                    customerMethod == 'TELEGRAM' ||
                                            customerMethod == 'GOOGLE'
                                        ? customerMethod == 'GOOGLE'
                                              ? tr('continueGoogle')
                                              : tr('continueTelegram')
                                        : register
                                        ? tr('register')
                                        : tr('signIn'),
                                  ),
                          ),
                          const SizedBox(height: 18),
                          Text(
                            tr('anotherWay'),
                            textAlign: TextAlign.center,
                            style: Theme.of(context).textTheme.labelMedium,
                          ),
                          const SizedBox(height: 6),
                          NavigationBar(
                            height: 72,
                            selectedIndex: role == 'TECHNICIAN'
                                ? (customerMethod == 'GOOGLE' ? 0 : 1)
                                : [
                                    'GOOGLE',
                                    'TELEGRAM',
                                    'PHONE',
                                  ].indexOf(customerMethod),
                            onDestinationSelected: (index) {
                              if (role == 'CUSTOMER') {
                                setState(() {
                                  customerMethod = [
                                    'GOOGLE',
                                    'TELEGRAM',
                                    'PHONE',
                                  ][index];
                                });
                              } else if (index < 2) {
                                setState(() {
                                  customerMethod = [
                                    'GOOGLE',
                                    'TELEGRAM',
                                  ][index];
                                });
                              }
                            },
                            destinations: [
                              NavigationDestination(
                                icon: const Icon(Icons.account_circle_outlined),
                                label: tr('google'),
                              ),
                              NavigationDestination(
                                icon: const Icon(Icons.telegram),
                                label: tr('telegram'),
                              ),
                              if (role == 'CUSTOMER')
                                NavigationDestination(
                                  icon: const Icon(Icons.phone_outlined),
                                  label: tr('phone'),
                                ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    ),
  );
}
