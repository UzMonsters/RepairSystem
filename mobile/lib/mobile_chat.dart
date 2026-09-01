part of 'main.dart';

class MobileChatScreen extends StatefulWidget {
  const MobileChatScreen({super.key, required this.repo, required this.requestId, this.language = 'ru'});
  final MobileChatRepository repo;
  final int requestId;
  final String language;

  @override
  State<MobileChatScreen> createState() => _MobileChatScreenState();
}

class _MobileChatScreenState extends State<MobileChatScreen> {
  final text = TextEditingController();
  late Future<PageResponse<Map<String, dynamic>>> future;
  int? conversationId;
  bool sending = false;

  @override
  void initState() {
    super.initState();
    future = loadMessages();
  }

  Future<PageResponse<Map<String, dynamic>>> loadMessages() async {
    final conversation = await widget.repo.getOrCreateForRequest(widget.requestId);
    conversationId = (conversation['id'] as num?)?.toInt();
    if (conversationId == null) return const PageResponse(content: []);
    return widget.repo.messages(conversationId!);
  }

  Future<void> send() async {
    final value = text.text.trim();
    if (value.isEmpty || sending) return;
    if (conversationId == null) await future;
    if (conversationId == null || !mounted) return;
    setState(() => sending = true);
    try {
      await widget.repo.sendMessage(
        conversationId!,
        value,
        clientMessageId: '${DateTime.now().microsecondsSinceEpoch}',
      );
      text.clear();
      setState(() => future = widget.repo.messages(conversationId!));
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('${mobileText(widget.language, 'sendFailed')}: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => sending = false);
    }
  }

  @override
  void dispose() {
    text.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(mobileText(widget.language, 'chat'))),
    body: FutureBuilder<PageResponse<Map<String, dynamic>>>(
      future: future,
      builder: (context, snapshot) {
        final messages = snapshot.data?.content ?? const <Map<String, dynamic>>[];
        return Column(
          children: [
            Expanded(
              child: snapshot.connectionState == ConnectionState.waiting
                  ? const Center(child: CircularProgressIndicator())
                  : ListView.builder(
                      reverse: true,
                      padding: const EdgeInsets.all(16),
                      itemCount: messages.length,
                      itemBuilder: (_, index) {
                        final message = messages[index];
                        return Card(
                          child: ListTile(
                            title: Text('${message['text'] ?? ''}'),
                            subtitle: Text('${message['createdAt'] ?? ''}'),
                          ),
                        );
                      },
                    ),
            ),
            SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Row(
                  children: [
                    Expanded(child: TextField(controller: text, textInputAction: TextInputAction.send, onSubmitted: (_) => send(), decoration: InputDecoration(hintText: mobileText(widget.language, 'message')))),
                    IconButton(onPressed: sending ? null : send, icon: sending ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.send)),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    ),
  );
}

