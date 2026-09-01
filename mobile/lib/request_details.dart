part of 'main.dart';

class RequestDetails extends StatefulWidget {
  const RequestDetails({super.key, required this.repo, required this.chat, required this.item});
  final CustomerRepository repo;
  final MobileChatRepository chat;
  final RequestItem item;
  @override
  State<RequestDetails> createState() => _RequestDetailsState();
}

class _RequestDetailsState extends State<RequestDetails> {
  bool uploading = false;

  Future<void> openChat() async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => MobileChatScreen(repo: widget.chat, requestId: widget.item.id, language: widget.chat.api.language),
      ),
    );
  }

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

