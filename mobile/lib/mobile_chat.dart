part of 'main.dart';

class MobileChatMessage {
  MobileChatMessage({
    required this.id,
    required this.conversationId,
    required this.senderType,
    required this.senderId,
    this.clientMessageId,
    required this.messageType,
    required this.text,
    this.createdAt,
    this.isOutgoing = false,
    this.isPending = false,
    this.isFailed = false,
    this.isRead = false,
  });

  final int id;
  final int conversationId;
  final String senderType;
  final int senderId;
  final String? clientMessageId;
  final String messageType;
  final String text;
  final String? createdAt;
  final bool isOutgoing;
  bool isPending;
  bool isFailed;
  bool isRead;

  factory MobileChatMessage.fromJson(
    Map<String, dynamic> json, {
    int? currentActorId,
    String? currentActorType,
  }) {
    final senderId = (json['senderId'] as num?)?.toInt() ?? 0;
    final senderType = json['senderType']?.toString() ?? '';
    final isOutgoing =
        currentActorId != null &&
        senderId == currentActorId &&
        (currentActorType == null || senderType == currentActorType);

    return MobileChatMessage(
      id: (json['id'] ?? json['messageId'] as num?)?.toInt() ?? 0,
      conversationId: (json['conversationId'] as num?)?.toInt() ?? 0,
      senderType: senderType,
      senderId: senderId,
      clientMessageId: json['clientMessageId']?.toString(),
      messageType:
          json['messageType']?.toString() ?? json['type']?.toString() ?? 'TEXT',
      text: json['text']?.toString() ?? '',
      createdAt: json['createdAt']?.toString(),
      isOutgoing: isOutgoing,
      isRead: json['read'] as bool? ?? false,
    );
  }

  factory MobileChatMessage.fromRealtime(
    ChatMessageRealtimePayload payload, {
    int? currentActorId,
    String? currentActorType,
  }) {
    final isOutgoing =
        currentActorId != null &&
        payload.senderId == currentActorId &&
        (currentActorType == null || payload.senderType == currentActorType);

    return MobileChatMessage(
      id: payload.messageId,
      conversationId: payload.conversationId,
      senderType: payload.senderType,
      senderId: payload.senderId,
      clientMessageId: payload.clientMessageId,
      messageType: payload.messageType,
      text: payload.text ?? '',
      createdAt: payload.createdAt,
      isOutgoing: isOutgoing,
    );
  }
}

class MobileChatScreen extends StatefulWidget {
  const MobileChatScreen({
    super.key,
    required this.repo,
    required this.requestId,
    this.language = 'ru',
    this.realtime,
    this.currentActorId,
    this.currentActorType,
  });

  final MobileChatRepository repo;
  final int requestId;
  final String language;
  final MobileRealtimeClient? realtime;
  final int? currentActorId;
  final String? currentActorType;

  @override
  State<MobileChatScreen> createState() => _MobileChatScreenState();
}

class _MobileChatScreenState extends State<MobileChatScreen> {
  final textController = TextEditingController();
  final List<MobileChatMessage> _messages = [];
  final Set<int> _messageIds = {};
  final Set<String> _clientMessageIds = {};

  int? conversationId;
  bool loading = true;
  bool otherTyping = false;
  Timer? _typingTimer;
  Timer? _sendTypingThrottleTimer;
  StreamSubscription<RealtimeEnvelope<dynamic>>? _eventSubscription;
  StreamSubscription<void>? _reconnectSubscription;

  @override
  void initState() {
    super.initState();
    _initChat();
  }

  Future<void> _initChat() async {
    try {
      final conversation = await widget.repo.getOrCreateForRequest(
        widget.requestId,
      );
      conversationId = (conversation['id'] as num?)?.toInt();
      if (conversationId != null) {
        await _loadMessages(conversationId!);
        _subscribeRealtime();
      }
    } catch (e) {
      MobileLog.warning('Failed to initialize chat: $e');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _loadMessages(int convId) async {
    try {
      final page = await widget.repo.messages(convId);
      final fetched = page.content
          .map(
            (json) => MobileChatMessage.fromJson(
              json,
              currentActorId: widget.currentActorId,
              currentActorType: widget.currentActorType,
            ),
          )
          .toList();

      if (mounted) {
        setState(() {
          for (final msg in fetched) {
            if (_messageIds.add(msg.id)) {
              if (msg.clientMessageId != null) {
                _clientMessageIds.add(msg.clientMessageId!);
              }
              _messages.add(msg);
            }
          }
          _messages.sort(
            (a, b) => (b.createdAt ?? '').compareTo(a.createdAt ?? ''),
          );
        });

        if (fetched.isNotEmpty) {
          final maxId = fetched
              .map((m) => m.id)
              .reduce((a, b) => a > b ? a : b);
          unawaited(widget.repo.markRead(convId, maxId));
        }
      }
    } catch (e) {
      MobileLog.warning('Failed to load chat messages: $e');
    }
  }

  void _subscribeRealtime() {
    final stream = widget.realtime?.events;
    if (stream == null || conversationId == null) return;

    _eventSubscription = stream.listen((envelope) {
      if (!mounted) return;

      final payload = envelope.payload;
      if (envelope.type == RealtimeEventType.chatMessageCreated &&
          payload is ChatMessageRealtimePayload) {
        if (payload.conversationId == conversationId) {
          _handleIncomingMessage(payload);
        }
      } else if (envelope.type == RealtimeEventType.chatMessageRead &&
          payload is ChatReadRealtimePayload) {
        if (payload.conversationId == conversationId) {
          _handleReadReceipt(payload);
        }
      } else if ((envelope.type == RealtimeEventType.chatTypingStarted ||
              envelope.type == RealtimeEventType.chatTypingStopped) &&
          payload is ChatTypingRealtimePayload) {
        if (payload.conversationId == conversationId) {
          _handleTypingEvent(payload);
        }
      }
    });

    _reconnectSubscription = widget.realtime?.onReconnected.listen((_) {
      if (conversationId != null && mounted) {
        _loadMessages(conversationId!);
      }
    });
  }

  void _handleIncomingMessage(ChatMessageRealtimePayload payload) {
    setState(() {
      otherTyping = false;
      _typingTimer?.cancel();

      // Check if this matches a pending optimistic message by clientMessageId
      if (payload.clientMessageId != null &&
          _clientMessageIds.contains(payload.clientMessageId)) {
        final index = _messages.indexWhere(
          (m) => m.clientMessageId == payload.clientMessageId && m.isPending,
        );
        if (index != -1) {
          _messages[index] = MobileChatMessage.fromRealtime(
            payload,
            currentActorId: widget.currentActorId,
            currentActorType: widget.currentActorType,
          );
          _messageIds.add(payload.messageId);
          return;
        }
      }

      if (_messageIds.add(payload.messageId)) {
        if (payload.clientMessageId != null) {
          _clientMessageIds.add(payload.clientMessageId!);
        }
        final newMsg = MobileChatMessage.fromRealtime(
          payload,
          currentActorId: widget.currentActorId,
          currentActorType: widget.currentActorType,
        );
        _messages.insert(0, newMsg);
      }
    });

    if (conversationId != null &&
        (widget.currentActorId == null ||
            payload.senderId != widget.currentActorId)) {
      unawaited(widget.repo.markRead(conversationId!, payload.messageId));
    }
  }

  void _handleReadReceipt(ChatReadRealtimePayload payload) {
    setState(() {
      for (final msg in _messages) {
        if (msg.id <= payload.messageId) {
          msg.isRead = true;
        }
      }
    });
  }

  void _handleTypingEvent(ChatTypingRealtimePayload payload) {
    if (widget.currentActorId != null &&
        payload.actorId == widget.currentActorId) {
      return;
    }

    setState(() {
      otherTyping = payload.typing;
    });

    _typingTimer?.cancel();
    if (payload.typing) {
      _typingTimer = Timer(const Duration(seconds: 4), () {
        if (mounted) {
          setState(() => otherTyping = false);
        }
      });
    }
  }

  void _onTextChanged(String value) {
    if (conversationId == null || widget.realtime == null) return;
    if (_sendTypingThrottleTimer?.isActive == true) return;

    _sendTypingThrottleTimer = Timer(const Duration(milliseconds: 2500), () {});
    widget.realtime?.send('/app/chat.typing', {
      'conversationId': conversationId,
      'typing': value.isNotEmpty,
    });
  }

  Future<void> send() async {
    final value = textController.text.trim();
    if (value.isEmpty || conversationId == null) return;

    final clientMsgId = 'm_${DateTime.now().microsecondsSinceEpoch}';
    textController.clear();

    final optimistic = MobileChatMessage(
      id: -DateTime.now().millisecondsSinceEpoch,
      conversationId: conversationId!,
      senderType: widget.currentActorType ?? '',
      senderId: widget.currentActorId ?? 0,
      clientMessageId: clientMsgId,
      messageType: 'TEXT',
      text: value,
      createdAt: DateTime.now().toUtc().toIso8601String(),
      isOutgoing: true,
      isPending: true,
    );

    setState(() {
      _clientMessageIds.add(clientMsgId);
      _messages.insert(0, optimistic);
    });

    try {
      final response = await widget.repo.sendMessage(
        conversationId!,
        value,
        clientMessageId: clientMsgId,
      );

      final realId = (response['id'] ?? response['messageId'] as num?)?.toInt();
      if (mounted && realId != null) {
        setState(() {
          optimistic.isPending = false;
          _messageIds.add(realId);
        });
      }
    } catch (error) {
      MobileLog.warning('Failed to send chat message: $error');
      if (mounted) {
        setState(() {
          optimistic.isPending = false;
          optimistic.isFailed = true;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${mobileText(widget.language, 'sendFailed')}: $error',
            ),
          ),
        );
      }
    }
  }

  @override
  void dispose() {
    textController.dispose();
    _typingTimer?.cancel();
    _sendTypingThrottleTimer?.cancel();
    _eventSubscription?.cancel();
    _reconnectSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(mobileText(widget.language, 'chat'))),
    body: Column(
      children: [
        if (loading)
          const Expanded(child: Center(child: CircularProgressIndicator()))
        else
          Expanded(
            child: ListView.builder(
              reverse: true,
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length,
              itemBuilder: (_, index) {
                final message = _messages[index];
                return Align(
                  alignment: message.isOutgoing
                      ? Alignment.centerRight
                      : Alignment.centerLeft,
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 4),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 10,
                    ),
                    constraints: BoxConstraints(
                      maxWidth: MediaQuery.of(context).size.width * 0.75,
                    ),
                    decoration: BoxDecoration(
                      color: message.isOutgoing
                          ? Theme.of(context).colorScheme.primaryContainer
                          : Theme.of(context)
                                .colorScheme
                                .surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Column(
                      crossAxisAlignment: message.isOutgoing
                          ? CrossAxisAlignment.end
                          : CrossAxisAlignment.start,
                      children: [
                        Text(
                          message.text,
                          style: TextStyle(
                            color: message.isOutgoing
                                ? Theme.of(context)
                                      .colorScheme
                                      .onPrimaryContainer
                                : Theme.of(context).colorScheme.onSurface,
                            fontSize: 15,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              _formatTime(message.createdAt),
                              style: TextStyle(
                                fontSize: 11,
                                color: Theme.of(context)
                                    .colorScheme
                                    .onSurfaceVariant,
                              ),
                            ),
                            if (message.isOutgoing) ...[
                              const SizedBox(width: 4),
                              if (message.isPending)
                                const SizedBox(
                                  width: 12,
                                  height: 12,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 1.5,
                                  ),
                                )
                              else if (message.isFailed)
                                const Icon(
                                  Icons.error_outline,
                                  size: 14,
                                  color: Colors.red,
                                )
                              else if (message.isRead)
                                const Icon(
                                  Icons.done_all,
                                  size: 14,
                                  color: Colors.blue,
                                )
                              else
                                const Icon(
                                  Icons.done,
                                  size: 14,
                                  color: Colors.grey,
                                ),
                            ],
                          ],
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        if (otherTyping)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Row(
                children: [
                  const SizedBox(
                    width: 14,
                    height: 14,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'typing...',
                    style: TextStyle(
                      fontStyle: FontStyle.italic,
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: textController,
                    textInputAction: TextInputAction.send,
                    onChanged: _onTextChanged,
                    onSubmitted: (_) => send(),
                    decoration: InputDecoration(
                      hintText: mobileText(widget.language, 'message'),
                      border: const OutlineInputBorder(),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                IconButton.filled(
                  onPressed: send,
                  icon: const Icon(Icons.send),
                ),
              ],
            ),
          ),
        ),
      ],
    ),
  );

  String _formatTime(String? iso) {
    if (iso == null || iso.isEmpty) return '';
    try {
      final dt = DateTime.parse(iso).toLocal();
      final hour = dt.hour.toString().padLeft(2, '0');
      final minute = dt.minute.toString().padLeft(2, '0');
      return '$hour:$minute';
    } catch (_) {
      return '';
    }
  }
}
