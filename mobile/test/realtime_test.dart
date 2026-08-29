import 'package:flutter_test/flutter_test.dart';
import 'package:repair_mobile/api_client.dart';
import 'package:repair_mobile/models.dart';
import 'package:repair_mobile/realtime_client.dart';

class FakeAuthStore extends AuthStore {
  FakeAuthStore([this._token = 'test_token']);
  String? _token;

  @override
  Future<String?> get accessToken async => _token;

  @override
  Future<String?> get refreshToken async => 'test_refresh_token';

  @override
  Future<void> save({required String access, required String refresh}) async {
    _token = access;
  }

  @override
  Future<void> clear() async {
    _token = null;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('RealtimeEventType & RealtimeEnvelope Model Parsing', () {
    test('parses REQUEST_CREATED envelope correctly with occurredAt', () {
      final json = {
        'type': 'REQUEST_CREATED',
        'eventId': 'evt-101',
        'occurredAt': '2026-08-29T10:00:00Z',
        'payload': {
          'requestId': 42,
          'requestNumber': 'REQ-42',
          'customerId': 7,
          'technicianId': null,
          'status': 'NEW',
          'priority': 'HIGH',
        },
      };

      final envelope = RealtimeEnvelope.fromJson(json);

      expect(envelope.type, RealtimeEventType.requestCreated);
      expect(envelope.rawType, 'REQUEST_CREATED');
      expect(envelope.eventId, 'evt-101');
      expect(envelope.occurredAt, '2026-08-29T10:00:00Z');
      expect(envelope.targetRequestId, 42);
      expect(envelope.type.isRequestDomainEvent, isTrue);
      expect(envelope.type.isChatEvent, isFalse);
      expect(envelope.type.isNotificationEvent, isFalse);

      final payload = envelope.payload as RequestRealtimePayload;
      expect(payload.requestId, 42);
      expect(payload.requestNumber, 'REQ-42');
      expect(payload.customerId, 7);
      expect(payload.status, 'NEW');
      expect(payload.priority, 'HIGH');
    });

    test('parses REQUEST_ASSIGNED, ASSIGNMENT_CREATED, and REASSIGNED envelopes', () {
      final assignedJson = {
        'type': 'REQUEST_ASSIGNED',
        'eventId': 'evt-assigned',
        'occurredAt': '2026-08-29T10:05:00Z',
        'payload': {
          'requestId': 50,
          'requestNumber': 'REQ-50',
          'assignmentId': 200,
          'technicianId': 10,
          'customerId': 5,
          'action': 'ASSIGNED',
          'status': 'ASSIGNED',
        },
      };
      final assignedEnv = RealtimeEnvelope.fromJson(assignedJson);
      expect(assignedEnv.type, RealtimeEventType.requestAssigned);
      expect(assignedEnv.targetRequestId, 50);

      final json = {
        'type': 'REQUEST_REASSIGNED',
        'eventId': 'evt-102',
        'occurredAt': '2026-08-29T10:10:00Z',
        'payload': {
          'requestId': 55,
          'requestNumber': 'REQ-55',
          'technicianId': 12,
          'previousTechnicianId': 9,
          'assignmentId': 301,
          'action': 'REASSIGNED',
          'status': 'ASSIGNED',
          'reason': 'Workload rebalance',
        },
      };

      final envelope = RealtimeEnvelope.fromJson(json);
      expect(envelope.type, RealtimeEventType.requestReassigned);
      expect(envelope.targetRequestId, 55);

      final payload = envelope.payload as AssignmentRealtimePayload;
      expect(payload.requestId, 55);
      expect(payload.technicianId, 12);
      expect(payload.previousTechnicianId, 9);
      expect(payload.action, 'REASSIGNED');
      expect(payload.status, 'ASSIGNED');
      expect(payload.reason, 'Workload rebalance');
    });

    test('parses CHAT_MESSAGE_CREATED envelope with messageId', () {
      final json = {
        'type': 'CHAT_MESSAGE_CREATED',
        'eventId': 'evt-chat-1',
        'occurredAt': '2026-08-29T10:15:00Z',
        'payload': {
          'messageId': 501,
          'conversationId': 88,
          'senderType': 'CUSTOMER',
          'senderId': 7,
          'clientMessageId': 'm_123456789',
          'messageType': 'TEXT',
          'text': 'Hello, when are you arriving?',
          'createdAt': '2026-08-29T10:15:00Z',
        },
      };

      final envelope = RealtimeEnvelope.fromJson(json);
      expect(envelope.type, RealtimeEventType.chatMessageCreated);
      expect(envelope.type.isChatEvent, isTrue);
      expect(envelope.type.isRequestDomainEvent, isFalse);

      final payload = envelope.payload as ChatMessageRealtimePayload;
      expect(payload.messageId, 501);
      expect(payload.conversationId, 88);
      expect(payload.senderType, 'CUSTOMER');
      expect(payload.clientMessageId, 'm_123456789');
      expect(payload.text, 'Hello, when are you arriving?');
    });

    test('parses CHAT_MESSAGE_READ and CHAT_TYPING envelopes', () {
      final readJson = {
        'type': 'CHAT_MESSAGE_READ',
        'occurredAt': '2026-08-29T10:16:00Z',
        'payload': {
          'conversationId': 88,
          'messageId': 501,
          'readerType': 'TECHNICIAN',
          'readerId': 12,
          'readAt': '2026-08-29T10:16:00Z',
        },
      };
      final readEnv = RealtimeEnvelope.fromJson(readJson);
      expect(readEnv.type, RealtimeEventType.chatMessageRead);
      final readPayload = readEnv.payload as ChatReadRealtimePayload;
      expect(readPayload.conversationId, 88);
      expect(readPayload.messageId, 501);

      final typingJson = {
        'type': 'CHAT_TYPING_STARTED',
        'occurredAt': '2026-08-29T10:17:00Z',
        'payload': {
          'conversationId': 88,
          'actorType': 'CUSTOMER',
          'actorId': 7,
        },
      };
      final typingEnv = RealtimeEnvelope.fromJson(typingJson);
      expect(typingEnv.type, RealtimeEventType.chatTypingStarted);
      final typingPayload = typingEnv.payload as ChatTypingRealtimePayload;
      expect(typingPayload.conversationId, 88);
      expect(typingPayload.typing, isTrue);
    });

    test('parses NOTIFICATION_CREATED envelope', () {
      final json = {
        'type': 'NOTIFICATION_CREATED',
        'occurredAt': '2026-08-29T10:20:00Z',
        'payload': {
          'notificationId': 99,
          'notificationType': 'ASSIGNMENT',
          'targetId': 42,
          'target': 'REPAIR_REQUEST',
          'title': 'New Job Assigned',
          'body': 'You have been assigned to REQ-42',
          'read': false,
        },
      };

      final envelope = RealtimeEnvelope.fromJson(json);
      expect(envelope.type, RealtimeEventType.notificationCreated);
      expect(envelope.type.isNotificationEvent, isTrue);
      final payload = envelope.payload as NotificationRealtimePayload;
      expect(payload.notificationId, 99);
      expect(payload.notificationType, 'ASSIGNMENT');
      expect(payload.targetId, 42);
      expect(payload.title, 'New Job Assigned');
      expect(payload.read, isFalse);
    });

    test('gracefully ignores unknown event types without throwing', () {
      final json = {
        'type': 'FUTURE_UNSUPPORTED_EVENT',
        'payload': {'foo': 'bar'},
      };

      final envelope = RealtimeEnvelope.fromJson(json);
      expect(envelope.type, RealtimeEventType.unknown);
      expect(envelope.rawType, 'FUTURE_UNSUPPORTED_EVENT');
      expect(envelope.type.isRequestDomainEvent, isFalse);
      expect(envelope.type.isChatEvent, isFalse);
      expect(envelope.type.isNotificationEvent, isFalse);
      expect(envelope.payload, isA<Map<String, dynamic>>());
    });
  });

  group('MobileRealtimeClient lifecycle', () {
    test('initializes and disposes gracefully with fake auth store', () async {
      final fakeAuth = FakeAuthStore('jwt_token_abc');
      final client = MobileRealtimeClient(fakeAuth);

      expect(client.isConnected, isFalse);

      await client.dispose();
      expect(client.isConnected, isFalse);
    });
  });
}
