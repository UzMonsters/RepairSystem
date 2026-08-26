part of 'main.dart';

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

