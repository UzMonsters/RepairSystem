part of 'main.dart';

class TechnicianJobs extends StatefulWidget {
  const TechnicianJobs({
    super.key,
    required this.repo,
    required this.chat,
    this.realtime,
    this.events,
  });
  final TechnicianRepository repo;
  final MobileChatRepository chat;
  final MobileRealtimeClient? realtime;
  final Stream<RealtimeEnvelope<dynamic>>? events;

  @override
  State<TechnicianJobs> createState() => _TechnicianJobsState();
}

class _TechnicianJobsState extends State<TechnicianJobs> {
  late Future<PageResponse<Job>> future;
  StreamSubscription<RealtimeEnvelope<dynamic>>? eventSubscription;
  StreamSubscription<void>? reconnectSubscription;
  String statusFilter = 'ALL';

  @override
  void initState() {
    super.initState();
    future = widget.repo.jobs();
    final stream = widget.events ?? widget.realtime?.events;
    eventSubscription = stream?.listen((envelope) {
      if (!envelope.type.isRequestDomainEvent) return;
      if (mounted) setState(() => future = widget.repo.jobs());
    });
    reconnectSubscription = widget.realtime?.onReconnected.listen((_) {
      if (mounted) setState(() => future = widget.repo.jobs());
    });
  }

  @override
  void dispose() {
    eventSubscription?.cancel();
    reconnectSubscription?.cancel();
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
            Text(
              mobileText(language, 'filters'),
              style: Theme.of(context).textTheme.titleSmall,
            ),
            const SizedBox(height: 6),
            SizedBox(
              height: 42,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.only(right: 8),
                children: [
                  ['ALL', 'all'],
                  ['NEW', 'new'],
                  ['ASSIGNED', 'assigned'],
                  ['IN_PROGRESS', 'inProgress'],
                  ['SCHEDULED', 'scheduled'],
                  ['WAITING_FOR_PARTS', 'waitingParts'],
                  ['COMPLETED', 'completed'],
                  ['CANCELLED', 'cancelled'],
                ].map((filter) => Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(mobileText(language, filter[1])),
                    selected: statusFilter == filter[0],
                    onSelected: (_) => setState(() => statusFilter = filter[0]),
                  ),
                )).toList(),
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
                    builder: (_) => JobActions(
                      repo: widget.repo,
                      chat: widget.chat,
                      job: job,
                      realtime: widget.realtime,
                    ),
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
