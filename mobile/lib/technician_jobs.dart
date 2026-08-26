part of 'main.dart';

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

