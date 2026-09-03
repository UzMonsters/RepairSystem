import 'dart:async';
import 'dart:collection';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import 'api_client.dart';
import 'mobile_logger.dart';
import 'models.dart';

class MobileRealtimeClient {
  MobileRealtimeClient(this.authStore);

  final AuthStore authStore;
  final _eventsController =
      StreamController<RealtimeEnvelope<dynamic>>.broadcast();
  final _connectionStateController = StreamController<bool>.broadcast();
  final _reconnectedController = StreamController<void>.broadcast();

  final Queue<String> _seenEventKeys = Queue<String>();
  final Set<String> _seenEventSet = <String>{};
  static const int _maxSeenKeys = 500;

  StompClient? _client;
  bool _disposed = false;
  bool _connected = false;

  Stream<RealtimeEnvelope<dynamic>> get events => _eventsController.stream;
  Stream<bool> get connectionState => _connectionStateController.stream;
  Stream<void> get onReconnected => _reconnectedController.stream;
  bool get isConnected => _connected;

  Future<void> connect() async {
    if (_disposed || (_client != null && _connected)) return;
    final token = await authStore.accessToken;
    if (token == null || token.isEmpty) {
      MobileLog.info('Realtime connect skipped: no access token available');
      return;
    }

    await _connectWithToken(token);
  }

  Future<void> reconnect() async {
    if (_disposed) return;
    final token = await authStore.accessToken;
    if (token == null || token.isEmpty) return;
    await _connectWithToken(token);
  }

  Future<void> reconnectWithToken(String token) async {
    if (_disposed || token.isEmpty) return;
    await _connectWithToken(token);
  }

  Future<void> _connectWithToken(String token) async {
    await disconnect();

    final base = Uri.parse(apiBaseUrl);
    final scheme = base.scheme == 'https' ? 'wss' : 'ws';
    final path = '${base.path.replaceFirst(RegExp(r'/$'), '')}/ws';
    final url = Uri(
      scheme: scheme,
      host: base.host,
      port: base.hasPort ? base.port : null,
      path: path,
    ).toString();

    MobileLog.info('Realtime connecting to $url');

    late final StompClient client;
    client = StompClient(
      config: StompConfig(
        url: url,
        stompConnectHeaders: {'Authorization': 'Bearer $token'},
        webSocketConnectHeaders: {'Authorization': 'Bearer $token'},
        reconnectDelay: const Duration(seconds: 4),
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
        onConnect: (StompFrame frame) {
          MobileLog.info('Realtime STOMP connected');
          _connected = true;
          _connectionStateController.add(true);
          _reconnectedController.add(null);

          client.subscribe(
            destination: '/user/queue/events',
            callback: (StompFrame frame) => _handleFrame(frame, 'events'),
          );
          client.subscribe(
            destination: '/user/queue/chat',
            callback: (StompFrame frame) => _handleFrame(frame, 'chat'),
          );
        },
        onDisconnect: (StompFrame? frame) {
          MobileLog.info('Realtime STOMP disconnected');
          _connected = false;
          _connectionStateController.add(false);
        },
        onStompError: (StompFrame frame) {
          MobileLog.warning('Realtime STOMP error: ${frame.body}');
          _connected = false;
          _connectionStateController.add(false);
        },
        onWebSocketError: (dynamic error) {
          MobileLog.warning('Realtime WebSocket error: $error');
          _connected = false;
          _connectionStateController.add(false);
        },
      ),
    );
    _client = client;
    client.activate();
  }

  void _handleFrame(StompFrame frame, String queueName) {
    final body = frame.body;
    if (body == null || body.isEmpty) return;

    try {
      final decoded = jsonDecode(body);
      if (decoded is! Map) return;

      final jsonMap = Map<String, dynamic>.from(decoded);
      final envelope = RealtimeEnvelope.fromJson(jsonMap);

      final dedupKey = _generateDeduplicationKey(envelope, jsonMap);
      if (dedupKey != null && !_isNewEvent(dedupKey)) {
        MobileLog.info(
          'Realtime event deduplicated: type=${envelope.rawType} key=$dedupKey',
        );
        return;
      }

      MobileLog.info(
        'Realtime event received on queue=$queueName type=${envelope.rawType}',
      );
      _eventsController.add(envelope);
    } catch (e, st) {
      MobileLog.warning(
        'Realtime frame parsing failed: $e',
        error: e,
        stackTrace: st,
      );
    }
  }

  String? _generateDeduplicationKey(
    RealtimeEnvelope<dynamic> envelope,
    Map<String, dynamic> rawJson,
  ) {
    if (envelope.eventId != null && envelope.eventId!.isNotEmpty) {
      return 'id:${envelope.eventId}';
    }

    final payload = envelope.payload;
    if (payload is ChatMessageRealtimePayload) {
      return 'chat_msg:${payload.messageId}:${payload.clientMessageId ?? ''}';
    }
    if (payload is ChatReadRealtimePayload) {
      return 'chat_read:${payload.conversationId}:${payload.messageId}:${payload.readerId}';
    }
    if (payload is NotificationRealtimePayload) {
      return 'notif:${payload.notificationId}:${payload.read}';
    }

    final targetReqId = envelope.targetRequestId;
    if (targetReqId != null && targetReqId > 0) {
      return '${envelope.rawType}:$targetReqId:${envelope.timestamp ?? ''}';
    }

    return null;
  }

  bool _isNewEvent(String key) {
    if (_seenEventSet.contains(key)) {
      return false;
    }
    _seenEventSet.add(key);
    _seenEventKeys.addLast(key);
    while (_seenEventKeys.length > _maxSeenKeys) {
      final oldest = _seenEventKeys.removeFirst();
      _seenEventSet.remove(oldest);
    }
    return true;
  }

  void send(String destination, Map<String, dynamic> body) {
    if (_client == null || !_connected) return;
    try {
      _client?.send(destination: destination, body: jsonEncode(body));
    } catch (e) {
      MobileLog.warning('Realtime send failed to $destination: $e');
    }
  }

  Future<void> disconnect() async {
    _connected = false;
    try {
      _client?.deactivate();
    } catch (_) {}
    _client = null;
  }

  Future<void> dispose() async {
    _disposed = true;
    await disconnect();
    await _eventsController.close();
    await _connectionStateController.close();
    await _reconnectedController.close();
  }
}
