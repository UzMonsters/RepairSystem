import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import 'api_client.dart';
import 'models.dart';
import 'repositories.dart';

void main() => runApp(const RepairAutoApp());

class RepairAutoApp extends StatefulWidget {
  const RepairAutoApp({super.key});
  @override
  State<RepairAutoApp> createState() => _RepairAutoAppState();
}

class _RepairAutoAppState extends State<RepairAutoApp> {
  final api = ApiClient();
  late final auth = AuthRepository(api);
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
      Navigator.of(context).pushReplacement(
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
  final token = TextEditingController();
  String role = 'CUSTOMER';

  @override
  void initState() {
    super.initState();
    token.addListener(_tokenChanged);
  }

  void _tokenChanged() => setState(() {});

  @override
  void dispose() {
    token.removeListener(_tokenChanged);
    token.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: Center(
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
                    segments: const [
                      ButtonSegment(value: 'CUSTOMER', label: Text('Customer')),
                      ButtonSegment(
                        value: 'TECHNICIAN',
                        label: Text('Technician'),
                      ),
                    ],
                    selected: {role},
                    onSelectionChanged: (value) =>
                        setState(() => role = value.first),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: token,
                    minLines: 3,
                    maxLines: 5,
                    decoration: const InputDecoration(
                      labelText: 'Telegram idToken',
                      border: OutlineInputBorder(),
                      helperText: 'Pass the token received from Telegram SDK.',
                    ),
                  ),
                  if (widget.error != null) ...[
                    const SizedBox(height: 12),
                    Text(
                      widget.error!,
                      style: const TextStyle(color: Colors.red),
                    ),
                  ],
                  const SizedBox(height: 18),
                  FilledButton(
                    onPressed: widget.loading || token.text.trim().isEmpty
                        ? null
                        : () => widget.onLogin(role, token.text.trim()),
                    child: widget.loading
                        ? const CircularProgressIndicator()
                        : const Text('Sign in'),
                  ),
                ],
              ),
            ),
          ),
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
            CustomerRequests(repo: customer),
            NotificationsScreen(repo: notifications),
            ProfileScreen(actor: widget.actor, repo: profile, onLogout: logout),
          ]
        : [
            TechnicianJobs(repo: technician),
            NotificationsScreen(repo: notifications),
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
  const CustomerRequests({super.key, required this.repo});
  final CustomerRepository repo;
  @override
  State<CustomerRequests> createState() => _CustomerRequestsState();
}

class _CustomerRequestsState extends State<CustomerRequests> {
  late Future<PageResponse<RequestItem>> future;
  final description = TextEditingController();
  final address = TextEditingController();

  @override
  void initState() {
    super.initState();
    future = widget.repo.requests();
  }

  @override
  void dispose() {
    description.dispose();
    address.dispose();
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
  const TechnicianJobs({super.key, required this.repo});
  final TechnicianRepository repo;
  @override
  State<TechnicianJobs> createState() => _TechnicianJobsState();
}

class _TechnicianJobsState extends State<TechnicianJobs> {
  late Future<PageResponse<Job>> future;

  @override
  void initState() {
    super.initState();
    future = widget.repo.jobs();
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

class NotificationsScreen extends StatelessWidget {
  const NotificationsScreen({super.key, required this.repo});
  final NotificationRepository repo;
  @override
  Widget build(
    BuildContext context,
  ) => FutureBuilder<PageResponse<NotificationItem>>(
    future: repo.list(),
    builder: (context, snapshot) {
      if (snapshot.connectionState == ConnectionState.waiting) {
        return const Center(child: CircularProgressIndicator());
      }
      if (snapshot.hasError) return Center(child: Text('${snapshot.error}'));
      return RefreshIndicator(
        onRefresh: () async {
          await repo.list();
        },
        child: ListView(
          children: [
            Align(
              alignment: Alignment.centerRight,
              child: TextButton(
                onPressed: repo.markAllRead,
                child: const Text('Mark all read'),
              ),
            ),
            ...(snapshot.data?.content ?? []).map(
              (n) => ListTile(
                title: Text(n.title),
                subtitle: Text(n.body),
                trailing: n.read ? null : const Icon(Icons.circle, size: 10),
                onTap: () async {
                  await repo.markRead(n.id);
                  if (context.mounted) Navigator.pop(context);
                },
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
