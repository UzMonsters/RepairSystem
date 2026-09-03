part of 'main.dart';

class CustomerRequests extends StatefulWidget {
  const CustomerRequests({
    super.key,
    required this.repo,
    required this.chat,
    required this.categories,
    this.realtime,
    this.events,
  });
  final CustomerRepository repo;
  final MobileChatRepository chat;
  final CategoryRepository categories;
  final MobileRealtimeClient? realtime;
  final Stream<RealtimeEnvelope<dynamic>>? events;

  @override
  State<CustomerRequests> createState() => _CustomerRequestsState();
}

class _CustomerRequestsState extends State<CustomerRequests> {
  late Future<PageResponse<RequestItem>> future;
  String? statusFilter;
  StreamSubscription<RealtimeEnvelope<dynamic>>? eventSubscription;
  StreamSubscription<void>? reconnectSubscription;

  @override
  void initState() {
    super.initState();
    _load();
    final stream = widget.events ?? widget.realtime?.events;
    eventSubscription = stream?.listen((envelope) {
      if (!envelope.type.isRequestDomainEvent) return;
      if (mounted) _load();
    });
    reconnectSubscription = widget.realtime?.onReconnected.listen((_) {
      if (mounted) _load();
    });
  }

  void _load() {
    setState(() => future = widget.repo.requests(status: statusFilter));
  }

  @override
  void dispose() {
    eventSubscription?.cancel();
    reconnectSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    floatingActionButton: FloatingActionButton(
      onPressed: () async {
        final result = await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => CreateRequestScreen(
              repo: widget.repo,
              categories: widget.categories,
            ),
          ),
        );
        if (result == true && mounted) {
          _load();
        }
      },
      child: const Icon(Icons.add),
    ),
    body: Column(
      children: [
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              _buildFilterChip(
                mobileText(widget.repo.api.language, 'all'),
                null,
              ),
              const SizedBox(width: 8),
              _buildFilterChip(
                mobileText(widget.repo.api.language, 'new'),
                'NEW',
              ),
              const SizedBox(width: 8),
              _buildFilterChip(
                mobileText(widget.repo.api.language, 'inProgress'),
                'IN_PROGRESS',
              ),
              const SizedBox(width: 8),
              _buildFilterChip(
                mobileText(widget.repo.api.language, 'completed'),
                'COMPLETED',
              ),
            ],
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () async => _load(),
            child: FutureBuilder<PageResponse<RequestItem>>(
              future: future,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snapshot.hasError) {
                  return ListView(
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(16),
                        child: Text(
                          '${snapshot.error}',
                          style: const TextStyle(color: Colors.red),
                        ),
                      ),
                    ],
                  );
                }
                final items = snapshot.data?.content ?? [];
                if (items.isEmpty) {
                  return ListView(
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(32),
                        child: Center(
                          child: Text(
                            mobileText(widget.repo.api.language, 'noRequests'),
                          ),
                        ),
                      ),
                    ],
                  );
                }
                return ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: items.length,
                  itemBuilder: (context, index) {
                    final item = items[index];
                    return Card(
                      child: ListTile(
                        title: Text(item.number),
                        subtitle: Text(item.description),
                        trailing: Text(item.status),
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => RequestDetails(
                              repo: widget.repo,
                              chat: widget.chat,
                              item: item,
                              realtime: widget.realtime,
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ),
      ],
    ),
  );

  Widget _buildFilterChip(String label, String? value) {
    return FilterChip(
      label: Text(label),
      selected: statusFilter == value,
      onSelected: (selected) {
        setState(() {
          statusFilter = selected ? value : null;
          _load();
        });
      },
    );
  }
}

class CreateRequestScreen extends StatefulWidget {
  const CreateRequestScreen({
    super.key,
    required this.repo,
    required this.categories,
  });
  final CustomerRepository repo;
  final CategoryRepository categories;

  @override
  State<CreateRequestScreen> createState() => _CreateRequestScreenState();
}

class _CreateRequestScreenState extends State<CreateRequestScreen> {
  late Future<List<Category>> categoriesFuture;
  int selectedCategoryId = 1;
  double? latitude;
  double? longitude;
  bool locating = false;
  final description = TextEditingController();
  final address = TextEditingController();

  @override
  void initState() {
    super.initState();
    categoriesFuture = loadCategories();
  }

  Future<List<Category>> loadCategories() async {
    try {
      final list = await widget.categories.list();
      if (list.isNotEmpty) selectedCategoryId = list.first.id;
      return list;
    } catch (_) {
      return [];
    }
  }

  Future<void> createRequest() async {
    if (description.text.trim().isEmpty) return;
    if (selectedCategoryId <= 0) return;
    try {
      await widget.repo.createRequest(
        description: description.text.trim(),
        categoryId: selectedCategoryId,
        address: address.text.trim().isEmpty ? null : address.text.trim(),
        latitude: latitude,
        longitude: longitude,
        locationSource: latitude == null ? null : 'MOBILE',
      );
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
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
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('$error')));
      }
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
          children: items
              .map(
                (item) => ListTile(
                  leading: const Icon(Icons.build_outlined),
                  title: Text(item.name),
                  trailing: item.id == selectedCategoryId
                      ? const Icon(Icons.check)
                      : null,
                  onTap: () => Navigator.pop(sheetContext, item.id),
                ),
              )
              .toList(),
        ),
      ),
    );
    if (selected != null && mounted) {
      setState(() => selectedCategoryId = selected);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(mobileText(widget.repo.api.language, 'createRequest')),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextField(
            controller: description,
            maxLines: 3,
            decoration: InputDecoration(
              labelText: mobileText(widget.repo.api.language, 'description'),
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 10),
          FutureBuilder<List<Category>>(
            future: categoriesFuture,
            builder: (context, snapshot) {
              final items = snapshot.data ?? const <Category>[];
              if (items.isEmpty) return const SizedBox.shrink();
              return OutlinedButton.icon(
                onPressed: () => chooseCategory(items),
                icon: const Icon(Icons.add),
                label: Text(
                  items
                      .firstWhere(
                        (item) => item.id == selectedCategoryId,
                        orElse: () => items.first,
                      )
                      .name,
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
              border: const OutlineInputBorder(),
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
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
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
    );
  }
}
