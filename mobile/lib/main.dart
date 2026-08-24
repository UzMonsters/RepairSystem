import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import 'api_client.dart';
import 'models.dart';
import 'realtime_client.dart';
import 'repositories.dart';

const demoModeEnabled = bool.fromEnvironment('DEMO_MODE');

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
  String? error;
  bool loading = false;

  Future<void> login(String role, String idToken) async {
    if (demoModeEnabled) {
      final actor = Actor(
        type: role,
        id: role == 'CUSTOMER' ? 900001 : 900002,
        fullName: role == 'CUSTOMER' ? 'Demo Customer' : 'Demo Technician',
        preferredLanguage: 'en',
      );
      if (!mounted) return;
      navigatorKey.currentState?.pushReplacement(
        MaterialPageRoute(
          builder: (_) => DemoHomePage(actor: actor),
        ),
      );
      return;
    }
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

  @override
  Widget build(BuildContext context) => MaterialApp(
    navigatorKey: navigatorKey,
    debugShowCheckedModeBanner: false,
    title: 'RepairAuto',
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff934316)),
      useMaterial3: true,
    ),
    home: LoginPage(onLogin: login, loading: loading, error: error),
  );
}

class LoginPage extends StatefulWidget {
  const LoginPage({
    super.key,
    required this.onLogin,
    required this.loading,
    this.error,
  });
  final Future<void> Function(String role, String idToken) onLogin;
  final bool loading;
  final String? error;
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final email = TextEditingController();
  final phone = TextEditingController();
  final password = TextEditingController();
  final name = TextEditingController();
  String role = 'CUSTOMER';
  String customerMethod = 'TELEGRAM';
  bool register = false;
  String language = 'ru';

  String tr(String key) {
    const values = {
      'ru': {'customer': 'Клиент', 'technician': 'Техник', 'signIn': 'Войти', 'register': 'Регистрация', 'email': 'Email', 'phone': 'Телефон', 'fullName': 'Имя и фамилия', 'password': 'Пароль', 'phoneNumber': 'Номер телефона', 'telegram': 'Telegram', 'continueTelegram': 'Продолжить через Telegram', 'anotherWay': 'Другой способ входа'},
      'uz': {'customer': 'Mijoz', 'technician': 'Texnik', 'signIn': 'Kirish', 'register': 'Ro‘yxatdan o‘tish', 'email': 'Email', 'phone': 'Telefon', 'fullName': 'Ism va familiya', 'password': 'Parol', 'phoneNumber': 'Telefon raqami', 'telegram': 'Telegram', 'continueTelegram': 'Telegram orqali davom etish', 'anotherWay': 'Boshqa kirish usuli'},
      'en': {'customer': 'Customer', 'technician': 'Technician', 'signIn': 'Sign in', 'register': 'Register', 'email': 'Email', 'phone': 'Phone', 'fullName': 'Full name', 'password': 'Password', 'phoneNumber': 'Phone number', 'telegram': 'Telegram', 'continueTelegram': 'Continue with Telegram', 'anotherWay': 'Sign in another way'},
    };
    return values[language]?[key] ?? values['en']![key] ?? key;
  }

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    email.dispose();
    phone.dispose();
    password.dispose();
    name.dispose();
    super.dispose();
  }

  void showUnavailable() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('This authentication method will be connected to the API later.')),
    );
  }

  void submit() {
    if (demoModeEnabled) {
      widget.onLogin(role, 'demo');
      return;
    }
    if (role == 'TECHNICIAN' || customerMethod == 'TELEGRAM') {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Telegram login will open here after SDK connection.')),
      );
      return;
    }
    showUnavailable();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: const Color(0xff070d26),
    body: DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color(0xff111a3e),
            Color(0xff080e28),
            Color(0xff05091b),
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
                  if (demoModeEnabled)
                    const Padding(
                      padding: EdgeInsets.only(top: 8),
                      child: Text(
                        'Debug demo mode',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.orange),
                      ),
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
                  if (role == 'TECHNICIAN' || customerMethod == 'TELEGRAM')
                    Card(
                      margin: EdgeInsets.zero,
                      color: Theme.of(context).colorScheme.primaryContainer,
                      child: ListTile(
                        leading: const Icon(Icons.telegram),
                        title: Text(tr('continueTelegram')),
                        subtitle: const Text('Secure sign-in for your account'),
                      ),
                    ),
                  if (role == 'CUSTOMER' && customerMethod == 'EMAIL') ...[
                    if (register) ...[
                      const SizedBox(height: 12),
                      TextField(controller: name, decoration: InputDecoration(labelText: tr('fullName'))),
                    ],
                    const SizedBox(height: 12),
                    TextField(controller: email, keyboardType: TextInputType.emailAddress, decoration: InputDecoration(labelText: tr('email'))),
                    const SizedBox(height: 12),
                    TextField(controller: password, obscureText: true, decoration: InputDecoration(labelText: tr('password'))),
                  ],
                  if (role == 'CUSTOMER' && customerMethod == 'PHONE') ...[
                    if (register) ...[
                      const SizedBox(height: 12),
                      TextField(controller: name, decoration: InputDecoration(labelText: tr('fullName'))),
                    ],
                    const SizedBox(height: 12),
                    TextField(controller: phone, keyboardType: TextInputType.phone, decoration: InputDecoration(labelText: tr('phoneNumber'))),
                    const SizedBox(height: 12),
                    TextField(controller: password, obscureText: true, decoration: InputDecoration(labelText: tr('password'))),
                  ],
                  if (widget.error != null) ...[
                    const SizedBox(height: 12),
                    Text(
                      widget.error!,
                      style: const TextStyle(color: Colors.red),
                    ),
                  ],
                  const SizedBox(height: 18),
                  FilledButton.icon(
                    onPressed: widget.loading ? null : submit,
                    icon: Icon(
                      role == 'TECHNICIAN' || customerMethod == 'TELEGRAM'
                          ? Icons.telegram
                          : register
                              ? Icons.person_add
                              : Icons.login,
                    ),
                    label: widget.loading
                        ? const CircularProgressIndicator()
                        : Text(
                            role == 'TECHNICIAN' || customerMethod == 'TELEGRAM'
                                ? tr('continueTelegram')
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
                      selectedIndex: ['TELEGRAM', 'EMAIL', 'PHONE'].indexOf(customerMethod),
                      onDestinationSelected: (index) => setState(() {
                        customerMethod = ['TELEGRAM', 'EMAIL', 'PHONE'][index];
                      }),
                      destinations: [
                        NavigationDestination(icon: const Icon(Icons.telegram), label: tr('telegram')),
                        NavigationDestination(icon: const Icon(Icons.email_outlined), label: tr('email')),
                        NavigationDestination(icon: const Icon(Icons.phone_outlined), label: tr('phone')),
                      ],
                    ),
                  ],
                  if (demoModeEnabled) ...[
                    const SizedBox(height: 14),
                    const Divider(),
                    const SizedBox(height: 4),
                    const Text(
                      'Debug demo login',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.orange),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: widget.loading
                                ? null
                                : () => widget.onLogin('CUSTOMER', 'demo'),
                            icon: const Icon(Icons.person_outline),
                            label: const Text('Demo client'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: widget.loading
                                ? null
                                : () => widget.onLogin('TECHNICIAN', 'demo'),
                            icon: const Icon(Icons.build_outlined),
                            label: const Text('Demo technician'),
                          ),
                        ),
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

class DemoHomePage extends StatefulWidget {
  const DemoHomePage({super.key, required this.actor});
  final Actor actor;

  @override
  State<DemoHomePage> createState() => _DemoHomePageState();
}

class _DemoHomePageState extends State<DemoHomePage> {
  int tab = 0;
  String demoLanguage = 'English';
  int demoRequestCount = 3;

  String dt(String key) {
    const text = {
      'English': {'work': 'Work', 'notifications': 'Notifications', 'profile': 'Profile', 'requests': 'My requests', 'jobs': 'Assigned jobs', 'add': 'Add request', 'open': 'Open', 'completed': 'Completed', 'language': 'Language'},
      'Русский': {'work': 'Работа', 'notifications': 'Уведомления', 'profile': 'Профиль', 'requests': 'Мои заявки', 'jobs': 'Назначенные заявки', 'add': 'Добавить заявку', 'open': 'Открытые', 'completed': 'Завершено', 'language': 'Язык'},
      'O‘zbekcha': {'work': 'Ish', 'notifications': 'Bildirishnomalar', 'profile': 'Profil', 'requests': 'Mening so‘rovlarim', 'jobs': 'Tayinlangan so‘rovlar', 'add': 'So‘rov qo‘shish', 'open': 'Ochiq', 'completed': 'Yakunlangan', 'language': 'Til'},
    };
    return text[demoLanguage]?[key] ?? text['English']![key] ?? key;
  }

  @override
  Widget build(BuildContext context) {
    final technician = widget.actor.type == 'TECHNICIAN';
    final pages = [
      _demoWorkPage(context, technician),
      _demoNotificationsPage(context),
      _demoProfilePage(context),
    ];
    return Scaffold(
      appBar: AppBar(
        title: Text(technician ? 'Demo Technician' : 'Demo Customer'),
        actions: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 12),
            child: Center(child: Text('DEMO', style: TextStyle(color: Colors.orange))),
          ),
          IconButton(
            onPressed: () => Navigator.of(context).pushReplacement(
              MaterialPageRoute(builder: (_) => const RepairAutoApp()),
            ),
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: pages[tab],
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (value) => setState(() => tab = value),
        destinations: [
          NavigationDestination(icon: Icon(Icons.dashboard_outlined), label: dt('work')),
          NavigationDestination(icon: Icon(Icons.notifications_outlined), label: dt('notifications')),
          NavigationDestination(icon: Icon(Icons.person_outline), label: dt('profile')),
        ],
      ),
    );
  }

  Widget _demoWorkPage(BuildContext context, bool technician) => ListView(
    padding: const EdgeInsets.all(16),
    children: [
      Row(
        children: [
          Expanded(child: Text(technician ? dt('jobs') : dt('requests'), style: Theme.of(context).textTheme.headlineSmall)),
          if (!technician)
            FilledButton.icon(
              onPressed: _createDemoRequest,
              icon: const Icon(Icons.add),
              label: Text(dt('add')),
            ),
        ],
      ),
      const SizedBox(height: 16),
      Row(
        children: [
          Expanded(child: _demoStat(context, dt('open'), technician ? '2' : '$demoRequestCount', Icons.assignment_outlined)),
          const SizedBox(width: 12),
          Expanded(child: _demoStat(context, dt('completed'), '5', Icons.check_circle_outline)),
        ],
      ),
      const SizedBox(height: 16),
      ...[
        ('REP-2026-000021', technician ? 'Screen replacement' : 'Phone repair', 'In progress'),
        ('REP-2026-000020', technician ? 'Battery diagnostics' : 'Laptop repair', 'New'),
        ('REP-2026-000019', 'Device inspection', 'Completed'),
      ].map((item) => Card(
        child: ListTile(
          leading: const CircleAvatar(child: Icon(Icons.build_outlined)),
          title: Text(item.$2),
          subtitle: Text(item.$1),
          trailing: technician && item.$3 != 'Completed'
              ? PopupMenuButton<String>(
                  onSelected: (action) {
                    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$action: ${item.$1}')));
                  },
                  itemBuilder: (_) => const [
                    PopupMenuItem(value: 'Accept', child: Text('Accept assignment')),
                    PopupMenuItem(value: 'Start', child: Text('Start repair')),
                    PopupMenuItem(value: 'Complete', child: Text('Complete repair')),
                  ],
                )
              : Chip(label: Text(item.$3)),
        ),
      )),
    ],
  );

  Future<void> _createDemoRequest() async {
    final controller = TextEditingController();
    final created = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Create request'),
        content: TextField(controller: controller, decoration: const InputDecoration(labelText: 'Description')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, controller.text.trim().isNotEmpty), child: const Text('Create')),
        ],
      ),
    );
    controller.dispose();
    if (created == true && mounted) {
      setState(() => demoRequestCount++);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Demo request created')));
    }
  }

  Widget _demoStat(BuildContext context, String title, String value, IconData icon) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Icon(icon, color: Theme.of(context).colorScheme.primary),
        const SizedBox(height: 8),
        Text(value, style: Theme.of(context).textTheme.headlineMedium),
        Text(title),
      ]),
    ),
  );

  Widget _demoNotificationsPage(BuildContext context) => ListView(
    padding: const EdgeInsets.all(16),
    children: [
      Text(dt('notifications'), style: Theme.of(context).textTheme.headlineSmall),
      const SizedBox(height: 12),
      for (final item in ['Repair started', 'New assignment', 'Repair completed'])
        Card(child: ListTile(leading: const Icon(Icons.notifications_outlined), title: Text(item), subtitle: const Text('Today, 10:30'))),
    ],
  );

  Widget _demoProfilePage(BuildContext context) => ListView(
    padding: const EdgeInsets.all(16),
    children: [
      Text(dt('profile'), style: Theme.of(context).textTheme.headlineSmall),
      const SizedBox(height: 16),
      Center(child: CircleAvatar(radius: 42, child: Text(widget.actor.fullName.substring(0, 1)))),
      const SizedBox(height: 16),
      Card(child: ListTile(title: Text(widget.actor.fullName), subtitle: Text(widget.actor.type))),
      Card(
        child: ListTile(
          leading: const Icon(Icons.language),
          title: Text(dt('language')),
          trailing: PopupMenuButton<String>(
            initialValue: demoLanguage,
            onSelected: (value) => setState(() => demoLanguage = value),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(demoLanguage == 'Русский' ? 'RU' : demoLanguage == 'O‘zbekcha' ? 'UZ' : 'EN'),
                const Icon(Icons.keyboard_arrow_down),
              ],
            ),
            itemBuilder: (_) => const [
              PopupMenuItem(value: 'Русский', child: Text('RU')),
              PopupMenuItem(value: 'O‘zbekcha', child: Text('UZ')),
              PopupMenuItem(value: 'English', child: Text('EN')),
            ],
          ),
        ),
      ),
    ],
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
    final pages = isCustomer
          ? [
            CustomerRequests(repo: customer, events: realtime.events),
            NotificationsScreen(repo: notifications, events: realtime.events),
            ProfileScreen(actor: widget.actor, repo: profile, onLogout: logout),
          ]
        : [
            TechnicianJobs(repo: technician, events: realtime.events),
            NotificationsScreen(repo: notifications, events: realtime.events),
            ProfileScreen(actor: widget.actor, repo: profile, onLogout: logout),
          ];
    return Scaffold(
      appBar: AppBar(
        title: Text(
          tab == 0
              ? (isCustomer ? 'My requests' : 'Assigned jobs')
              : tab == 1
              ? 'Notifications'
              : 'Profile',
        ),
      ),
      body: IndexedStack(index: tab, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: tab,
        onDestinationSelected: (value) => setState(() => tab = value),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.assignment_outlined),
            label: 'Requests',
          ),
          NavigationDestination(
            icon: Icon(Icons.notifications_outlined),
            label: 'Notifications',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            label: 'Profile',
          ),
        ],
      ),
    );
  }
}

class CustomerRequests extends StatefulWidget {
  const CustomerRequests({super.key, required this.repo, this.events});
  final CustomerRepository repo;
  final Stream<Map<String, dynamic>>? events;
  @override
  State<CustomerRequests> createState() => _CustomerRequestsState();
}

class _CustomerRequestsState extends State<CustomerRequests> {
  late Future<PageResponse<RequestItem>> future;
  StreamSubscription<Map<String, dynamic>>? eventSubscription;
  final description = TextEditingController();
  final address = TextEditingController();

  @override
  void initState() {
    super.initState();
    future = widget.repo.requests();
    eventSubscription = widget.events?.listen((_) {
      if (mounted) setState(() => future = widget.repo.requests());
    });
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
    await widget.repo.createRequest(
      description: description.text.trim(),
      categoryId: 1,
      address: address.text.trim().isEmpty ? null : address.text.trim(),
      locationSource:
          address.text.trim().isEmpty ? null : 'MANUAL',
    );
    description.clear();
    address.clear();
    setState(() => future = widget.repo.requests());
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
                const Text(
                  'Create repair request',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: description,
                  maxLines: 3,
                  decoration: const InputDecoration(
                    labelText: 'Problem description',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: address,
                  maxLength: 500,
                  decoration: const InputDecoration(
                    labelText: 'Address (optional)',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.location_on_outlined),
                  ),
                ),
                const SizedBox(height: 10),
                FilledButton(
                  onPressed: createRequest,
                  child: const Text('Create'),
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
            if (items.isEmpty) return const Center(child: Text('No requests'));
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
                                RequestDetails(repo: widget.repo, item: item),
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
  const RequestDetails({super.key, required this.repo, required this.item});
  final CustomerRepository repo;
  final RequestItem item;
  @override
  State<RequestDetails> createState() => _RequestDetailsState();
}

class _RequestDetailsState extends State<RequestDetails> {
  bool uploading = false;

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

class TechnicianJobs extends StatefulWidget {
  const TechnicianJobs({super.key, required this.repo, this.events});
  final TechnicianRepository repo;
  final Stream<Map<String, dynamic>>? events;
  @override
  State<TechnicianJobs> createState() => _TechnicianJobsState();
}

class _TechnicianJobsState extends State<TechnicianJobs> {
  late Future<PageResponse<Job>> future;
  StreamSubscription<Map<String, dynamic>>? eventSubscription;

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
        final jobs = snapshot.data?.content ?? [];
        return ListView(
          padding: const EdgeInsets.all(16),
          children: jobs
              .map(
                (job) => Card(
                  child: ListTile(
                    title: Text(job.number),
                    subtitle: Text(job.description),
                    trailing: Text(job.status),
                    onTap: () => showModalBottomSheet(
                      context: context,
                      builder: (_) => JobActions(repo: widget.repo, job: job),
                    ),
                  ),
                ),
              )
              .toList(),
        );
      },
    ),
  );
}

class JobActions extends StatelessWidget {
  const JobActions({super.key, required this.repo, required this.job});
  final TechnicianRepository repo;
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

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Wrap(
      children: job.availableActions.map((action) {
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
            final endpoint = {
              'ACCEPT_ASSIGNMENT': 'accept',
              'REJECT_ASSIGNMENT': 'reject',
              'START_REPAIR': 'start',
              'WAIT_FOR_PARTS': 'wait-for-parts',
              'RESUME_REPAIR': 'resume',
              'COMPLETE_REPAIR': 'complete',
            }[action];
            if (endpoint != null) await repo.action(job.id, endpoint);
            if (context.mounted) Navigator.pop(context);
          },
        );
      }).toList(),
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
                label: const Text('Mark all read'),
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
  String language = 'uz';
  bool saving = false;

  @override
  void dispose() {
    name.dispose();
    super.dispose();
  }

  Future<void> save() async {
    setState(() => saving = true);
    try {
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
          decoration: const InputDecoration(
            labelText: 'Full name',
            border: OutlineInputBorder(),
          ),
          onChanged: (_) => setState(() {}),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          initialValue: language,
          decoration: const InputDecoration(
            labelText: 'Language',
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
              : const Text('Save profile'),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: widget.onLogout,
          icon: const Icon(Icons.logout),
          label: const Text('Logout'),
        ),
      ],
    );
  }
}
