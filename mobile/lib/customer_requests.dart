part of 'main.dart';

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

