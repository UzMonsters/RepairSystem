part of 'main.dart';

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

class _HomePageState extends State<HomePage> with WidgetsBindingObserver {
  int tab = 0;
  late final realtime = MobileRealtimeClient(widget.api.authStore);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    widget.api.setLanguage(widget.actor.preferredLanguage ?? 'uz');
    widget.api.onTokenRefreshed = (newAccessToken) {
      MobileLog.info('API client refreshed token, reconnecting realtime');
      unawaited(realtime.reconnectWithToken(newAccessToken));
    };
    unawaited(realtime.connect());
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      MobileLog.info('App resumed, ensuring realtime connection');
      unawaited(realtime.connect());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    widget.api.onTokenRefreshed = null;
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
            CustomerRequests(
              repo: customer,
              chat: chat,
              categories: categories,
              realtime: realtime,
            ),
            NotificationsScreen(
              repo: notifications,
              realtime: realtime,
            ),
            ProfileScreen(
              actor: widget.actor,
              repo: profile,
              onLogout: logout,
            ),
          ]
        : [
            TechnicianJobs(
              repo: technician,
              chat: chat,
              realtime: realtime,
            ),
            NotificationsScreen(
              repo: notifications,
              realtime: realtime,
            ),
            ProfileScreen(
              actor: widget.actor,
              repo: profile,
              onLogout: logout,
            ),
          ];
    return Scaffold(
      appBar: AppBar(
        title: Text(
          tab == 0
              ? (isCustomer
                  ? mobileText(language, 'requests')
                  : 'Assigned jobs')
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
            icon: const Icon(Icons.assignment_outlined),
            label: mobileText(language, 'requests'),
          ),
          NavigationDestination(
            icon: const Icon(Icons.notifications_outlined),
            label: mobileText(language, 'notifications'),
          ),
          NavigationDestination(
            icon: const Icon(Icons.person_outline),
            label: mobileText(language, 'profile'),
          ),
        ],
      ),
    );
  }
}
