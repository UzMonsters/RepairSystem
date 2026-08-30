part of 'main.dart';

class RequestDetails extends StatefulWidget {
  const RequestDetails({
    super.key,
    required this.repo,
    required this.chat,
    required this.item,
    this.realtime,
  });
  final CustomerRepository repo;
  final MobileChatRepository chat;
  final RequestItem item;
  final MobileRealtimeClient? realtime;

  @override
  State<RequestDetails> createState() => _RequestDetailsState();
}

class _RequestDetailsState extends State<RequestDetails> {
  bool uploading = false;
  late RequestItem currentItem;
  StreamSubscription<RealtimeEnvelope<dynamic>>? eventSubscription;

  @override
  void initState() {
    super.initState();
    currentItem = widget.item;
    eventSubscription = widget.realtime?.events.listen((envelope) {
      if (envelope.targetRequestId != currentItem.id) return;

      if (envelope.type == RealtimeEventType.requestDeleted) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Repair request was deleted')),
          );
          Navigator.of(context).pop();
        }
        return;
      }

      if (envelope.type.isRequestDomainEvent) {
        if (mounted) {
          _refreshRequest();
        }
      }
    });
  }

  @override
  void dispose() {
    eventSubscription?.cancel();
    super.dispose();
  }

  Future<void> _refreshRequest() async {
    try {
      final response = await widget.repo.requests();
      final updated = response.content.cast<RequestItem?>().firstWhere(
        (r) => r?.id == currentItem.id,
        orElse: () => null,
      );
      if (updated != null && mounted) {
        setState(() => currentItem = updated);
      }
    } catch (_) {}
  }

  Future<void> openChat() async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => MobileChatScreen(
          repo: widget.chat,
          requestId: currentItem.id,
          language: widget.chat.api.language,
          realtime: widget.realtime,
        ),
      ),
    );
  }

  Future<void> uploadProblemPhoto() async {
    final picked = await ImagePicker().pickImage(source: ImageSource.gallery);
    if (picked == null) return;
    setState(() => uploading = true);
    try {
      await widget.repo.uploadPhoto(currentItem.id, File(picked.path));
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Photo uploaded')));
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('$error')));
      }
    } finally {
      if (mounted) setState(() => uploading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(currentItem.number)),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(
              child: Text(
                currentItem.description,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            Chip(label: Text(currentItem.status)),
          ],
        ),
        if (currentItem.address != null)
          ListTile(
            leading: const Icon(Icons.location_on_outlined),
            title: Text(currentItem.address!),
          ),
        if (currentItem.location?.latitude != null &&
            currentItem.location?.longitude != null)
          ListTile(
            leading: const Icon(Icons.pin_drop_outlined),
            title: Text(
              '${currentItem.location!.latitude}, '
              '${currentItem.location!.longitude}',
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
            await widget.repo.submitReview(currentItem.id, 5, '');
            if (context.mounted) {
              ScaffoldMessenger.of(
                context,
              ).showSnackBar(const SnackBar(content: Text('Review sent')));
            }
          },
          child: const Text('Submit 5-star review'),
        ),
      ],
    ),
  );
}
