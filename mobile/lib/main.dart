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

String mobileText(String language, String key) {
  const strings = <String, Map<String, String>>{
    'ru': {
      'requests': 'Заявки', 'createRequest': 'Создать заявку',
      'description': 'Описание проблемы', 'category': 'Категория',
      'chooseCategory': 'Выберите категорию', 'location': 'Геопозиция',
      'getLocation': 'Определить мою геопозицию', 'locationReady': 'Геопозиция добавлена',
      'create': 'Создать', 'noRequests': 'Заявок нет', 'filters': 'Фильтры',
      'all': 'Все', 'new': 'Новая', 'assigned': 'Назначена',
      'inProgress': 'В работе', 'completed': 'Завершена', 'cancelled': 'Отменена',
      'scheduled': 'Запланирована', 'waitingParts': 'Ожидание запчастей',
      'schedule': 'Расписание визитов', 'chat': 'Чат с техником',
      'message': 'Сообщение', 'sendFailed': 'Не удалось отправить сообщение',
      'notifications': 'Уведомления', 'profile': 'Профиль', 'save': 'Сохранить',
      'logout': 'Выйти', 'fullName': 'Имя и фамилия', 'markAllRead': 'Прочитать все',
    },
    'uz': {
      'requests': 'Arizalar', 'createRequest': 'Ariza yaratish',
      'description': 'Muammo tavsifi', 'category': 'Kategoriya',
      'chooseCategory': 'Kategoriyani tanlang', 'location': 'Geolokatsiya',
      'getLocation': 'Geolokatsiyamni aniqlash', 'locationReady': 'Geolokatsiya qo‘shildi',
      'create': 'Yaratish', 'noRequests': 'Arizalar yo‘q', 'filters': 'Filtrlar',
      'all': 'Barchasi', 'new': 'Yangi', 'assigned': 'Tayinlangan',
      'inProgress': 'Jarayonda', 'completed': 'Yakunlangan', 'cancelled': 'Bekor qilingan',
      'scheduled': 'Rejalashtirilgan', 'waitingParts': 'Ehtiyot qismlar kutilmoqda',
      'schedule': 'Tashrif jadvali', 'chat': 'Texnik bilan chat',
      'message': 'Xabar', 'sendFailed': 'Xabar yuborilmadi',
      'notifications': 'Bildirishnomalar', 'profile': 'Profil', 'save': 'Saqlash',
      'logout': 'Chiqish', 'fullName': 'Ism va familiya', 'markAllRead': 'Barchasini o‘qilgan qilish',
    },
    'en': {
      'requests': 'Requests', 'createRequest': 'Create repair request',
      'description': 'Problem description', 'category': 'Category',
      'chooseCategory': 'Choose a category', 'location': 'Location',
      'getLocation': 'Use my location', 'locationReady': 'Location added',
      'create': 'Create', 'noRequests': 'No requests', 'filters': 'Filters',
      'all': 'All', 'new': 'New', 'assigned': 'Assigned',
      'inProgress': 'In progress', 'completed': 'Completed', 'cancelled': 'Cancelled',
      'scheduled': 'Scheduled', 'waitingParts': 'Waiting for parts',
      'schedule': 'Visit schedule', 'chat': 'Chat with technician',
      'message': 'Message', 'sendFailed': 'Message could not be sent',
      'notifications': 'Notifications', 'profile': 'Profile', 'save': 'Save',
      'logout': 'Logout', 'fullName': 'Full name', 'markAllRead': 'Mark all read',
    },
  };
  return strings[language]?[key] ?? strings['en']![key] ?? key;
}

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

  Future<void> login(String role, String idToken) async {
    setState(() {
      loading = true;
      error = null;
    });
    try {
      final actor = role == 'CUSTOMER'
          ? await auth.loginCustomer(idToken)
          : await auth.loginTechnician(idToken);
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
      onRequestPhoneOtp: requestPhoneOtp,
      onVerifyPhoneOtp: verifyPhoneOtp,
      loading: loading,
      error: error,
    ),
  );
}

class LoginPage extends StatefulWidget {
  const LoginPage({
    super.key,
    required this.onLogin,
    required this.onTelegramLogin,
    required this.onRequestPhoneOtp,
    required this.onVerifyPhoneOtp,
    required this.loading,
    this.error,
  });
  final Future<void> Function(String role, String idToken) onLogin;
  final Future<String> Function(String role) onTelegramLogin;
  final Future<String> Function(String role, String phone) onRequestPhoneOtp;
  final Future<void> Function(String role, String challengeId, String code) onVerifyPhoneOtp;
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
  String language = 'ru';
  String? localError;

  String tr(String key) {
    const values = {
      'ru': {'customer': 'Клиент', 'technician': 'Техник', 'signIn': 'Войти', 'register': 'Регистрация', 'phone': 'Телефон', 'fullName': 'Имя и фамилия', 'phoneNumber': 'Номер телефона', 'google': 'Google', 'telegram': 'Telegram', 'continueGoogle': 'Продолжить через Google', 'continueTelegram': 'Продолжить через Telegram', 'anotherWay': 'Другой способ входа'},
      'uz': {'customer': 'Mijoz', 'technician': 'Texnik', 'signIn': 'Kirish', 'register': 'Ro‘yxatdan o‘tish', 'phone': 'Telefon', 'fullName': 'Ism va familiya', 'phoneNumber': 'Telefon raqami', 'google': 'Google', 'telegram': 'Telegram', 'continueGoogle': 'Google orqali davom etish', 'continueTelegram': 'Telegram orqali davom etish', 'anotherWay': 'Boshqa kirish usuli'},
      'en': {'customer': 'Customer', 'technician': 'Technician', 'signIn': 'Sign in', 'register': 'Register', 'phone': 'Phone', 'fullName': 'Full name', 'phoneNumber': 'Phone number', 'google': 'Google', 'telegram': 'Telegram', 'continueGoogle': 'Continue with Google', 'continueTelegram': 'Continue with Telegram', 'anotherWay': 'Sign in another way'},
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
  }

  @override
  void dispose() {
    phone.dispose();
    name.dispose();
    super.dispose();
  }

  void showUnavailable() {
    setState(() => localError = 'This authentication method is not configured yet.');
  }

  Future<void> submit() async {
    setState(() => localError = null);
    if (role == 'TECHNICIAN' || customerMethod == 'TELEGRAM') {
      try {
        final idToken = await widget.onTelegramLogin(role);
        if (mounted) await widget.onLogin(role, idToken);
      } catch (e) {
        if (!mounted) return;
        setState(() => localError = e.toString().replaceFirst('StateError: ', ''));
      }
      return;
    }
    if (customerMethod == 'GOOGLE') {
      showUnavailable();
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
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Verify')),
        ],
      ),
    );
    if (verified == true && codeController.text.trim().isNotEmpty) {
      await widget.onVerifyPhoneOtp(role, challengeId, codeController.text.trim());
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
          colors: [
            Color(0xfffffcfa),
            Color(0xfffff8f5),
            Color(0xfffff1eb),
          ],
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
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.surfaceContainerHighest,
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
                    style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 24),
                  SegmentedButton<String>(
                    segments: [
                      ButtonSegment(value: 'CUSTOMER', label: Text(tr('customer'))),
                      ButtonSegment(
                        value: 'TECHNICIAN',
                        label: Text(tr('technician')),
                      ),
                    ],
                    selected: {role},
                    onSelectionChanged: (value) =>
                        setState(() {
                          role = value.first;
                          if (role == 'TECHNICIAN') {
                            customerMethod = 'TELEGRAM';
                            register = false;
                          }
                        }),
                  ),
                  const SizedBox(height: 16),
                  if (role == 'CUSTOMER') ...[
                    Row(
                      children: [
                        Expanded(child: Text(register ? tr('register') : '${tr('customer')} ${tr('signIn')}')),
                        Switch(value: register, onChanged: (value) => setState(() => register = value)),
                      ],
                    ),
                  ],
                  if (role == 'TECHNICIAN' || customerMethod == 'TELEGRAM' || customerMethod == 'GOOGLE')
                    Card(
                      margin: EdgeInsets.zero,
                      color: Theme.of(context).colorScheme.primaryContainer,
                      child: ListTile(
                        leading: Icon(customerMethod == 'GOOGLE' ? Icons.account_circle_outlined : Icons.telegram),
                        title: Text(customerMethod == 'GOOGLE' ? tr('continueGoogle') : tr('continueTelegram')),
                        subtitle: const Text('Secure sign-in for your account'),
                      ),
                    ),
                  if (role == 'CUSTOMER' && customerMethod == 'PHONE') ...[
                    if (register) ...[
                      const SizedBox(height: 12),
                      TextField(controller: name, decoration: InputDecoration(labelText: tr('fullName'))),
                    ],
                    const SizedBox(height: 12),
                    TextField(controller: phone, keyboardType: TextInputType.phone, decoration: InputDecoration(labelText: tr('phoneNumber'))),
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
                    onPressed: widget.loading ? null : submit,
                    icon: Icon(
                      role == 'TECHNICIAN' || customerMethod == 'TELEGRAM' || customerMethod == 'GOOGLE'
                          ? customerMethod == 'GOOGLE' ? Icons.account_circle_outlined : Icons.telegram
                          : register
                              ? Icons.person_add
                              : Icons.login,
                    ),
                    label: widget.loading
                        ? const CircularProgressIndicator()
                        : Text(
                            role == 'TECHNICIAN' || customerMethod == 'TELEGRAM' || customerMethod == 'GOOGLE'
                                ? customerMethod == 'GOOGLE' ? tr('continueGoogle') : tr('continueTelegram')
                                : register
                                    ? tr('register')
                                    : tr('signIn'),
                          ),
                  ),
                  if (role == 'CUSTOMER') ...[
                    const SizedBox(height: 18),
                    Text(
                      tr('anotherWay'),
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.labelMedium,
                    ),
                    const SizedBox(height: 6),
                    NavigationBar(
                      height: 72,
                      selectedIndex: ['GOOGLE', 'TELEGRAM', 'PHONE'].indexOf(customerMethod),
                      onDestinationSelected: (index) => setState(() {
                        customerMethod = ['GOOGLE', 'TELEGRAM', 'PHONE'][index];
                      }),
                      destinations: [
                        NavigationDestination(icon: const Icon(Icons.account_circle_outlined), label: tr('google')),
                        NavigationDestination(icon: const Icon(Icons.telegram), label: tr('telegram')),
                        NavigationDestination(icon: const Icon(Icons.phone_outlined), label: tr('phone')),
                      ],
                    ),
                  ],
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


class HomePage extends StatefulWidget {
  const HomePage({
    super.key,
    required this.api,
    required this.auth,
    required this.actor,
  });
  final ApiClient api;
  final AuthRepository auth;
  final Actor actor;
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int tab = 0;
  late final realtime = MobileRealtimeClient(widget.api.authStore);

  @override
  void initState() {
    super.initState();
    widget.api.setLanguage(widget.actor.preferredLanguage ?? 'uz');
    unawaited(realtime.connect());
  }

  @override
  void dispose() {
    unawaited(realtime.dispose());
    super.dispose();
  }
  late final customer = CustomerRepository(widget.api);
  late final technician = TechnicianRepository(widget.api);
  late final notifications = NotificationRepository(widget.api);
  late final profile = MobileProfileRepository(widget.api);
  late final chat = MobileChatRepository(widget.api);
  late final categories = CategoryRepository(widget.api);

  Future<void> logout() async {
    await widget.auth.logout();
    if (mounted) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const RepairAutoApp()),
        (_) => false,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isCustomer = widget.actor.type == 'CUSTOMER';
    final language = widget.api.language;
    final pages = isCustomer
          ? [
            CustomerRequests(repo: customer, chat: chat, categories: categories, events: realtime.events),
            NotificationsScreen(repo: notifications, events: realtime.events),
            ProfileScreen(actor: widget.actor, repo: profile, onLogout: logout),
          ]
        : [
            TechnicianJobs(repo: technician, chat: chat, events: realtime.events),
            NotificationsScreen(repo: notifications, events: realtime.events),
            ProfileScreen(actor: widget.actor, repo: profile, onLogout: logout),
          ];
    return Scaffold(
      appBar: AppBar(
        title: Text(
          tab == 0
              ? (isCustomer ? mobileText(language, 'requests') : 'Assigned jobs')
              : tab == 1
              ? mobileText(language, 'notifications')
              : mobileText(language, 'profile'),
        ),
      ),
      body: IndexedStack(index: tab, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (value) => setState(() => tab = value),
        destinations: [
          NavigationDestination(
            icon: Icon(Icons.assignment_outlined),
            label: mobileText(language, 'requests'),
          ),
          NavigationDestination(
            icon: Icon(Icons.notifications_outlined),
            label: mobileText(language, 'notifications'),
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            label: mobileText(language, 'profile'),
          ),
        ],
      ),
    );
  }
}

class CustomerRequests extends StatefulWidget {
  const CustomerRequests({super.key, required this.repo, required this.chat, required this.categories, this.events});
  final CustomerRepository repo;
  final MobileChatRepository chat;
  final CategoryRepository categories;
  final Stream<Map<String, dynamic>>? events;
  @override
  State<CustomerRequests> createState() => _CustomerRequestsState();
}

class _CustomerRequestsState extends State<CustomerRequests> {
  late Future<PageResponse<RequestItem>> future;
  late Future<List<Category>> categories;
  int selectedCategoryId = 1;
  double? latitude;
  double? longitude;
  bool locating = false;
  StreamSubscription<Map<String, dynamic>>? eventSubscription;
  final description = TextEditingController();
  final address = TextEditingController();

  @override
  void initState() {
    super.initState();
    future = widget.repo.requests();
    categories = loadCategories();
    eventSubscription = widget.events?.listen((_) {
      if (mounted) setState(() => future = widget.repo.requests());
    });
  }

  Future<List<Category>> loadCategories() async {
    try {
      final result = await widget.categories.list();
      if (result.isNotEmpty) selectedCategoryId = result.first.id;
      return result;
    } catch (_) {
      return const [];
    }
  }

  @override
  void dispose() {
    description.dispose();
    address.dispose();
    eventSubscription?.cancel();
    super.dispose();
  }

  Future<void> createRequest() async {
    if (description.text.trim().isEmpty) return;
    if (selectedCategoryId <= 0) return;
    await widget.repo.createRequest(
      description: description.text.trim(),
      categoryId: selectedCategoryId,
      address: address.text.trim().isEmpty ? null : address.text.trim(),
      latitude: latitude,
      longitude: longitude,
      locationSource: latitude == null ? null : 'MOBILE',
    );
    description.clear();
    address.clear();
    latitude = null;
    longitude = null;
    setState(() => future = widget.repo.requests());
  }

  Future<void> useCurrentLocation() async {
    setState(() => locating = true);
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        throw StateError('Location services are disabled');
      }
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        throw StateError('Location permission was not granted');
      }
      final position = await Geolocator.getCurrentPosition();
      if (!mounted) return;
      setState(() {
        latitude = position.latitude;
        longitude = position.longitude;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(mobileText(widget.repo.api.language, 'locationReady'))),
      );
    } catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$error')));
    } finally {
      if (mounted) setState(() => locating = false);
    }
  }

  Future<void> chooseCategory(List<Category> items) async {
    final selected = await showModalBottomSheet<int>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: ListView(
          shrinkWrap: true,
          children: items.map((item) => ListTile(
            leading: const Icon(Icons.build_outlined),
            title: Text(item.name),
            trailing: item.id == selectedCategoryId ? const Icon(Icons.check) : null,
            onTap: () => Navigator.pop(sheetContext, item.id),
          )).toList(),
        ),
      ),
    );
    if (selected != null && mounted) setState(() => selectedCategoryId = selected);
  }

  @override
  Widget build(BuildContext context) => RefreshIndicator(
    onRefresh: () async => setState(() => future = widget.repo.requests()),
    child: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  mobileText(widget.repo.api.language, 'createRequest'),
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: description,
                  maxLines: 3,
                  decoration: InputDecoration(
                    labelText: mobileText(widget.repo.api.language, 'description'),
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 10),
                FutureBuilder<List<Category>>(
                  future: categories,
                  builder: (context, snapshot) {
                    final items = snapshot.data ?? const <Category>[];
                    if (items.isEmpty) return const SizedBox.shrink();
                    return OutlinedButton.icon(
                      onPressed: () => chooseCategory(items),
                      icon: const Icon(Icons.add),
                      label: Text(
                        items.firstWhere(
                          (item) => item.id == selectedCategoryId,
                          orElse: () => items.first,
                        ).name,
                      ),
                    );
                  },
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: address,
                  maxLength: 500,
                  decoration: InputDecoration(
                    labelText: mobileText(widget.repo.api.language, 'location'),
                    border: OutlineInputBorder(),
                    prefixIcon: IconButton(
                      tooltip: mobileText(widget.repo.api.language, 'getLocation'),
                      onPressed: locating ? null : useCurrentLocation,
                      icon: const Icon(Icons.location_on_outlined),
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                OutlinedButton.icon(
                  onPressed: locating ? null : useCurrentLocation,
                  icon: locating
                      ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.my_location),
                  label: Text(
                    latitude == null
                        ? mobileText(widget.repo.api.language, 'getLocation')
                        : '${latitude!.toStringAsFixed(5)}, ${longitude!.toStringAsFixed(5)}',
                  ),
                ),
                const SizedBox(height: 10),
                FilledButton(
                  onPressed: createRequest,
                  child: Text(mobileText(widget.repo.api.language, 'create')),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        FutureBuilder<PageResponse<RequestItem>>(
          future: future,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return Text(
                '${snapshot.error}',
                style: const TextStyle(color: Colors.red),
              );
            }
            final items = snapshot.data?.content ?? [];
            if (items.isEmpty) return Center(child: Text(mobileText(widget.repo.api.language, 'noRequests')));
            return Column(
              children: items
                  .map(
                    (item) => Card(
                      child: ListTile(
                        title: Text(item.number),
                        subtitle: Text(item.description),
                        trailing: Text(item.status),
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) =>
                                RequestDetails(repo: widget.repo, chat: widget.chat, item: item),
                          ),
                        ),
                      ),
                    ),
                  )
                  .toList(),
            );
          },
        ),
      ],
    ),
  );
}

class RequestDetails extends StatefulWidget {
  const RequestDetails({super.key, required this.repo, required this.chat, required this.item});
  final CustomerRepository repo;
  final MobileChatRepository chat;
  final RequestItem item;
  @override
  State<RequestDetails> createState() => _RequestDetailsState();
}

class _RequestDetailsState extends State<RequestDetails> {
  bool uploading = false;

  Future<void> openChat() async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => MobileChatScreen(repo: widget.chat, requestId: widget.item.id, language: widget.chat.api.language),
      ),
    );
  }

  Future<void> uploadProblemPhoto() async {
    final picked = await ImagePicker().pickImage(source: ImageSource.gallery);
    if (picked == null) return;
    setState(() => uploading = true);
    try {
      await widget.repo.uploadPhoto(widget.item.id, File(picked.path));
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Photo uploaded')));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('$error')));
      }
    } finally {
      if (mounted) setState(() => uploading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(widget.item.number)),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          widget.item.description,
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
        ),
        if (widget.item.address != null)
          ListTile(
            leading: const Icon(Icons.location_on_outlined),
            title: Text(widget.item.address!),
          ),
        if (widget.item.location?.latitude != null &&
            widget.item.location?.longitude != null)
          ListTile(
            leading: const Icon(Icons.pin_drop_outlined),
            title: Text(
              '${widget.item.location!.latitude}, '
              '${widget.item.location!.longitude}',
            ),
          ),
        const SizedBox(height: 16),
        OutlinedButton.icon(
          onPressed: uploading ? null : uploadProblemPhoto,
          icon: const Icon(Icons.photo_camera),
          label: Text(uploading ? 'Uploading...' : 'Upload problem photo'),
        ),
        const SizedBox(height: 8),
        OutlinedButton.icon(
          onPressed: openChat,
          icon: const Icon(Icons.chat_outlined),
          label: const Text('Chat with technician'),
        ),
        const SizedBox(height: 20),
        const Text('Review after completion'),
        FilledButton(
          onPressed: () async {
            await widget.repo.submitReview(widget.item.id, 5, '');
            if (context.mounted) {
              ScaffoldMessenger.of(context)
                  .showSnackBar(const SnackBar(content: Text('Review sent')));
            }
          },
          child: const Text('Submit 5-star review'),
        ),
      ],
    ),
  );
}

class MobileChatScreen extends StatefulWidget {
  const MobileChatScreen({super.key, required this.repo, required this.requestId, this.language = 'ru'});
  final MobileChatRepository repo;
  final int requestId;
  final String language;

  @override
  State<MobileChatScreen> createState() => _MobileChatScreenState();
}

class _MobileChatScreenState extends State<MobileChatScreen> {
  final text = TextEditingController();
  late Future<PageResponse<Map<String, dynamic>>> future;
  int? conversationId;
  bool sending = false;

  @override
  void initState() {
    super.initState();
    future = loadMessages();
  }

  Future<PageResponse<Map<String, dynamic>>> loadMessages() async {
    final conversation = await widget.repo.getOrCreateForRequest(widget.requestId);
    conversationId = (conversation['id'] as num?)?.toInt();
    if (conversationId == null) return const PageResponse(content: []);
    return widget.repo.messages(conversationId!);
  }

  Future<void> send() async {
    final value = text.text.trim();
    if (value.isEmpty || sending) return;
    if (conversationId == null) await future;
    if (conversationId == null || !mounted) return;
    setState(() => sending = true);
    try {
      await widget.repo.sendMessage(
        conversationId!,
        value,
        clientMessageId: '${DateTime.now().microsecondsSinceEpoch}',
      );
      text.clear();
      setState(() => future = widget.repo.messages(conversationId!));
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('${mobileText(widget.language, 'sendFailed')}: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => sending = false);
    }
  }

  @override
  void dispose() {
    text.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(mobileText(widget.language, 'chat'))),
    body: FutureBuilder<PageResponse<Map<String, dynamic>>>(
      future: future,
      builder: (context, snapshot) {
        final messages = snapshot.data?.content ?? const <Map<String, dynamic>>[];
        return Column(
          children: [
            Expanded(
              child: snapshot.connectionState == ConnectionState.waiting
                  ? const Center(child: CircularProgressIndicator())
                  : ListView.builder(
                      reverse: true,
                      padding: const EdgeInsets.all(16),
                      itemCount: messages.length,
                      itemBuilder: (_, index) {
                        final message = messages[index];
                        return Card(
                          child: ListTile(
                            title: Text('${message['text'] ?? ''}'),
                            subtitle: Text('${message['createdAt'] ?? ''}'),
                          ),
                        );
                      },
                    ),
            ),
            SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Row(
                  children: [
                    Expanded(child: TextField(controller: text, textInputAction: TextInputAction.send, onSubmitted: (_) => send(), decoration: InputDecoration(hintText: mobileText(widget.language, 'message')))),
                    IconButton(onPressed: sending ? null : send, icon: sending ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.send)),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    ),
  );
}

class TechnicianJobs extends StatefulWidget {
  const TechnicianJobs({super.key, required this.repo, required this.chat, this.events});
  final TechnicianRepository repo;
  final MobileChatRepository chat;
  final Stream<Map<String, dynamic>>? events;
  @override
  State<TechnicianJobs> createState() => _TechnicianJobsState();
}

class _TechnicianJobsState extends State<TechnicianJobs> {
  late Future<PageResponse<Job>> future;
  StreamSubscription<Map<String, dynamic>>? eventSubscription;
  String statusFilter = 'ALL';

  @override
  void initState() {
    super.initState();
    future = widget.repo.jobs();
    eventSubscription = widget.events?.listen((_) {
      if (mounted) setState(() => future = widget.repo.jobs());
    });
  }

  @override
  void dispose() {
    eventSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => RefreshIndicator(
    onRefresh: () async => setState(() => future = widget.repo.jobs()),
    child: FutureBuilder<PageResponse<Job>>(
      future: future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) return Center(child: Text('${snapshot.error}'));
        final allJobs = snapshot.data?.content ?? [];
        final jobs = statusFilter == 'ALL'
            ? allJobs
            : allJobs.where((job) => job.status == statusFilter).toList();
        final language = widget.repo.api.language;
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            OutlinedButton.icon(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => ScheduleScreen(repo: widget.repo),
                ),
              ),
              icon: const Icon(Icons.calendar_month_outlined),
              label: Text(mobileText(language, 'schedule')),
            ),
            const SizedBox(height: 8),
            Text(mobileText(language, 'filters'), style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 6),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: SegmentedButton<String>(
                showSelectedIcon: false,
                segments: [
                  ButtonSegment(value: 'ALL', label: Text(mobileText(language, 'all'))),
                  ButtonSegment(value: 'NEW', label: Text(mobileText(language, 'new'))),
                  ButtonSegment(value: 'ASSIGNED', label: Text(mobileText(language, 'assigned'))),
                  ButtonSegment(value: 'IN_PROGRESS', label: Text(mobileText(language, 'inProgress'))),
                  ButtonSegment(value: 'SCHEDULED', label: Text(mobileText(language, 'scheduled'))),
                  ButtonSegment(value: 'WAITING_FOR_PARTS', label: Text(mobileText(language, 'waitingParts'))),
                  ButtonSegment(value: 'COMPLETED', label: Text(mobileText(language, 'completed'))),
                  ButtonSegment(value: 'CANCELLED', label: Text(mobileText(language, 'cancelled'))),
                ],
                selected: {statusFilter},
                onSelectionChanged: (value) => setState(() => statusFilter = value.first),
              ),
            ),
            const SizedBox(height: 8),
            ...jobs.map(
                (job) => Card(
                  child: ListTile(
                    title: Text(job.number),
                    subtitle: Text(job.description),
                    trailing: Text(job.status),
                    onTap: () => showModalBottomSheet(
                      context: context,
                      builder: (_) => JobActions(repo: widget.repo, chat: widget.chat, job: job),
                    ),
                  ),
                ),
              ),
          ],
        );
      },
    ),
  );
}

class ScheduleScreen extends StatefulWidget {
  const ScheduleScreen({super.key, required this.repo});
  final TechnicianRepository repo;

  @override
  State<ScheduleScreen> createState() => _ScheduleScreenState();
}

class _ScheduleScreenState extends State<ScheduleScreen> {
  late Future<List<Map<String, dynamic>>> future;

  @override
  void initState() {
    super.initState();
    final today = DateTime.now();
    final end = today.add(const Duration(days: 60));
    future = widget.repo.schedule(
      from: today.toIso8601String().substring(0, 10),
      to: end.toIso8601String().substring(0, 10),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Visit schedule')),
    body: FutureBuilder<List<Map<String, dynamic>>>(
      future: future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) return Center(child: Text('${snapshot.error}'));
        final items = snapshot.data ?? const <Map<String, dynamic>>[];
        if (items.isEmpty) return const Center(child: Text('No scheduled visits'));
        return ListView.builder(
          padding: const EdgeInsets.all(16),
          itemCount: items.length,
          itemBuilder: (_, index) {
            final item = items[index];
            return Card(
              child: ListTile(
                leading: const Icon(Icons.event_outlined),
                title: Text('${item['requestNumber'] ?? item['number'] ?? 'Request'}'),
                subtitle: Text('${item['scheduledVisitAt'] ?? item['visitAt'] ?? ''}'),
              ),
            );
          },
        );
      },
    ),
  );
}

class JobActions extends StatelessWidget {
  const JobActions({super.key, required this.repo, required this.chat, required this.job});
  final TechnicianRepository repo;
  final MobileChatRepository chat;
  final Job job;
  Future<void> uploadEvidence(
    BuildContext context,
    String attachmentType,
  ) async {
    final picked = await ImagePicker().pickImage(source: ImageSource.gallery);
    if (picked == null) return;
    await repo.uploadPhoto(job.id, File(picked.path), attachmentType);
    if (context.mounted) Navigator.pop(context);
  }

  Future<String?> askText(BuildContext context, String title, String label) async {
    final controller = TextEditingController();
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          maxLines: 4,
          decoration: InputDecoration(labelText: label),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, controller.text.trim()),
            child: const Text('Save'),
          ),
        ],
      ),
    );
    controller.dispose();
    return value == null || value.isEmpty ? null : value;
  }

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Wrap(
      children: [
        ListTile(
          title: const Text('Chat with customer'),
          leading: const Icon(Icons.chat_outlined),
          onTap: () async {
            Navigator.pop(context);
            await Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => MobileChatScreen(repo: chat, requestId: job.id, language: chat.api.language),
              ),
            );
          },
        ),
        ...job.availableActions.map((action) {
        if (action == 'UPLOAD_DIAGNOSIS_PHOTO') {
          return ListTile(
            title: Text(action),
            leading: const Icon(Icons.photo_camera),
            onTap: () => uploadEvidence(context, 'DIAGNOSIS_PHOTO'),
          );
        }
        if (action == 'UPLOAD_COMPLETION_PHOTO') {
          return ListTile(
            title: Text(action),
            leading: const Icon(Icons.photo_camera),
            onTap: () => uploadEvidence(context, 'COMPLETION_PHOTO'),
          );
        }
        return ListTile(
          title: Text(action),
          leading: const Icon(Icons.play_arrow),
          onTap: () async {
            if (action == 'ACCEPT_ASSIGNMENT') {
              await repo.accept(job.id);
            } else if (action == 'START_REPAIR') {
              await repo.start(job.id);
            } else if (action == 'RESUME_REPAIR') {
              await repo.resume(job.id);
            } else if (action == 'REJECT_ASSIGNMENT') {
              final reason = await askText(context, 'Reject assignment', 'Reason');
              if (reason != null) await repo.reject(job.id, reason);
            } else if (action == 'UPDATE_DIAGNOSIS') {
              final diagnosis = await askText(context, 'Diagnosis', 'Diagnosis');
              if (diagnosis != null) await repo.diagnosis(job.id, diagnosis);
            } else if (action == 'WAIT_FOR_PARTS') {
              final reason = await askText(context, 'Wait for parts', 'Reason');
              if (reason != null) await repo.waitForParts(job.id, reason);
            } else if (action == 'COMPLETE_REPAIR') {
              final work = await askText(context, 'Complete repair', 'Work performed');
              if (work != null) await repo.complete(job.id, work);
            }
            if (context.mounted) Navigator.pop(context);
          },
        );
      }).toList(),
      ],
    ),
  );
}

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key, required this.repo, this.events});
  final NotificationRepository repo;
  final Stream<Map<String, dynamic>>? events;
  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  late Future<PageResponse<NotificationItem>> future;
  StreamSubscription<Map<String, dynamic>>? eventSubscription;

  @override
  void initState() {
    super.initState();
    future = widget.repo.list();
    eventSubscription = widget.events?.listen((_) {
      if (mounted) unawaited(reload());
    });
  }

  @override
  void dispose() {
    eventSubscription?.cancel();
    super.dispose();
  }

  Future<void> reload() async {
    setState(() => future = widget.repo.list());
    await future;
  }

  Future<void> markAllRead() async {
    await widget.repo.markAllRead();
    await reload();
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<PageResponse<NotificationItem>>(
    future: future,
    builder: (context, snapshot) {
      final language = widget.repo.api.language;
      if (snapshot.connectionState == ConnectionState.waiting) {
        return const Center(child: CircularProgressIndicator());
      }
      if (snapshot.hasError) return Center(child: Text('${snapshot.error}'));
      return RefreshIndicator(
        onRefresh: reload,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Align(
              alignment: Alignment.centerRight,
              child: FilledButton.tonalIcon(
                onPressed: markAllRead,
                icon: const Icon(Icons.done_all),
                label: Text(mobileText(language, 'markAllRead')),
              ),
            ),
            ...(snapshot.data?.content ?? []).map(
              (n) => Card(
                margin: const EdgeInsets.only(top: 10),
                child: ListTile(
                  leading: CircleAvatar(
                    child: Icon(n.read ? Icons.notifications_none : Icons.notifications_active),
                  ),
                  title: Text(n.title, style: const TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: Text(n.body),
                  trailing: n.read ? null : const Icon(Icons.circle, size: 10),
                  onTap: () async {
                    if (!n.read) await widget.repo.markRead(n.id);
                    await reload();
                  },
                ),
              ),
            ),
          ],
        ),
      );
    },
  );
}

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.actor,
    required this.repo,
    required this.onLogout,
  });
  final Actor actor;
  final MobileProfileRepository repo;
  final VoidCallback onLogout;
  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  late final name = TextEditingController(text: widget.actor.fullName);
  late String language = widget.actor.preferredLanguage ?? 'uz';
  bool saving = false;

  @override
  void dispose() {
    name.dispose();
    super.dispose();
  }

  Future<void> save() async {
    setState(() => saving = true);
    try {
      widget.repo.setLanguage(language);
      await widget.repo.update(
        fullName: name.text.trim(),
        preferredLanguage: language,
      );
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Profile saved')));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('$error')));
      }
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = widget.repo.api.language;
    final initials = name.text
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .map((part) => part[0])
        .take(2)
        .join()
        .toUpperCase();
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 30,
                  child: Text(initials.isEmpty ? '?' : initials),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    widget.actor.type,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: name,
          decoration: InputDecoration(
            labelText: mobileText(strings, 'fullName'),
            border: OutlineInputBorder(),
          ),
          onChanged: (_) => setState(() {}),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          initialValue: language,
          decoration: const InputDecoration(
            labelText: 'RU / UZ / EN',
            border: OutlineInputBorder(),
          ),
          items: const [
            DropdownMenuItem(value: 'uz', child: Text('O‘zbekcha')),
            DropdownMenuItem(value: 'ru', child: Text('Русский')),
            DropdownMenuItem(value: 'en', child: Text('English')),
          ],
          onChanged: (value) => setState(() => language = value ?? 'uz'),
        ),
        const SizedBox(height: 12),
        FilledButton(
          onPressed: saving ? null : save,
          child: saving
              ? const CircularProgressIndicator()
              : Text(mobileText(strings, 'save')),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: widget.onLogout,
          icon: const Icon(Icons.logout),
          label: Text(mobileText(strings, 'logout')),
        ),
      ],
    );
  }
}
