import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import 'api_client.dart';

class MobileRealtimeClient {
  MobileRealtimeClient(this.authStore);

  final AuthStore authStore;
  final _events = StreamController<Map<String, dynamic>>.broadcast();
  StompClient? _client;
  bool _disposed = false;

  Stream<Map<String, dynamic>> get events => _events.stream;

  Future<void> connect() async {
    if (_disposed || _client?.isActive == true) return;
    final token = await authStore.accessToken;
    if (token == null || token.isEmpty) return;

    final base = Uri.parse(apiBaseUrl);
    final scheme = base.scheme == 'https' ? 'wss' : 'ws';
    final path = '${base.path.replaceFirst(RegExp(r'/$'), '')}/ws';
    final url = Uri(scheme: scheme, host: base.host, port: base.hasPort ? base.port : null, path: path).toString();

    late final StompClient client;
    client = StompClient(
      config: StompConfig(
        url: url,
        stompConnectHeaders: {'Authorization': 'Bearer $token'},
        reconnectDelay: const Duration(seconds: 5),
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
        onConnect: (_) {
          client.subscribe(
            destination: '/user/queue/events',
            callback: _handleFrame,
          );
          client.subscribe(
            destination: '/user/queue/chat',
            callback: _handleFrame,
          );
        },
        onStompError: (_) {},
        onWebSocketError: (_) {},
      ),
    );
    _client = client;
    client.activate();
  }

  void _handleFrame(StompFrame frame) {
    final body = frame.body;
    if (body == null || body.isEmpty) return;
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map) {
        _events.add(Map<String, dynamic>.from(decoded));
      }
    } catch (_) {
      // Ignore malformed broker messages and keep the connection alive.
    }
  }

  Future<void> disconnect() async {
    _client?.deactivate();
    _client = null;
  }

  Future<void> dispose() async {
    _disposed = true;
    await disconnect();
    await _events.close();
  }
}
