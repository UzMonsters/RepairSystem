part of 'main.dart';

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

