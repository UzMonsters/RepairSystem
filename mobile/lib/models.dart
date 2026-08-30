class PageResponse<T> {
  const PageResponse({
    required this.content,
    this.page = 0,
    this.size = 20,
    this.totalElements = 0,
    this.totalPages = 0,
  });
  final List<T> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  factory PageResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) parse,
  ) => PageResponse(
    content: (json['content'] as List? ?? [])
        .whereType<Map<String, dynamic>>()
        .map(parse)
        .toList(),
    page: json['page'] as int? ?? 0,
    size: json['size'] as int? ?? 20,
    totalElements: json['totalElements'] as int? ?? 0,
    totalPages: json['totalPages'] as int? ?? 0,
  );
}

class Category {
  const Category({required this.id, required this.name});
  final int id;
  final String name;

  factory Category.fromJson(Map<String, dynamic> json) => Category(
    id: (json['id'] as num).toInt(),
    name: '${json['name'] ?? json['nameRu'] ?? json['nameEn'] ?? ''}',
  );
}

class Actor {
  const Actor({
    required this.type,
    required this.id,
    required this.fullName,
    this.phone,
    this.preferredLanguage,
  });
  final String type;
  final int id;
  final String fullName;
  final String? phone;
  final String? preferredLanguage;
  factory Actor.fromJson(Map<String, dynamic> json) => Actor(
    type: '${json['type'] ?? json['actorType']}',
    id: json['id'] as int,
    fullName: '${json['fullName']}',
    phone: json['phone']?.toString(),
    preferredLanguage: json['preferredLanguage']?.toString(),
  );
}

class RequestLocation {
  const RequestLocation({
    this.address,
    this.latitude,
    this.longitude,
    this.source,
  });

  final String? address;
  final double? latitude;
  final double? longitude;
  final String? source;

  factory RequestLocation.fromJson(Map<dynamic, dynamic> json) =>
      RequestLocation(
        address: json['address']?.toString(),
        latitude: (json['latitude'] as num?)?.toDouble(),
        longitude: (json['longitude'] as num?)?.toDouble(),
        source: json['source']?.toString(),
      );
}

class RequestItem {
  const RequestItem({
    required this.id,
    required this.number,
    required this.status,
    required this.description,
    this.statusLabel,
    this.categoryName,
    this.address,
    this.location,
    this.createdAt,
    this.customerFullName,
  });
  final int id;
  final String number;
  final String status;
  final String description;
  final String? statusLabel;
  final String? categoryName;
  final String? address;
  final RequestLocation? location;
  final String? createdAt;
  final String? customerFullName;
  factory RequestItem.fromJson(Map<String, dynamic> json) {
    final requestId = (json['id'] ?? json['requestId']) as int;
    final location = json['location'] as Map?;
    final category = json['category'] as Map?;
    return RequestItem(
      id: requestId,
      number: (json['requestNumber'] ?? json['number'] ?? '#$requestId')
          .toString(),
      status: '${json['status'] ?? json['requestStatus']}',
      description: '${json['description'] ?? ''}',
      statusLabel:
          json['statusLabel']?.toString() ??
          json['requestStatusLabel']?.toString(),
      categoryName: category?['name']?.toString(),
      address: json['address']?.toString() ?? location?['address']?.toString(),
      location: location == null ? null : RequestLocation.fromJson(location),
      createdAt: json['createdAt']?.toString(),
      customerFullName:
          json['customerFullName']?.toString() ??
          (json['customer'] as Map?)?['fullName']?.toString(),
    );
  }
}

class NotificationItem {
  const NotificationItem({
    required this.id,
    required this.title,
    required this.body,
    required this.createdAt,
    this.read = false,
    this.target,
    this.targetId,
    this.requestNumber,
  });
  final int id;
  final String title;
  final String body;
  final String createdAt;
  final bool read;
  final String? target;
  final int? targetId;
  final String? requestNumber;
  factory NotificationItem.fromJson(Map<String, dynamic> json) =>
      NotificationItem(
        id: json['id'] as int,
        title: '${json['title'] ?? ''}',
        body: '${json['body'] ?? json['message'] ?? ''}',
        createdAt: '${json['createdAt'] ?? ''}',
        read: json['read'] as bool? ?? false,
        target: json['target']?.toString(),
        targetId: json['targetId'] as int?,
        requestNumber: json['requestNumber']?.toString(),
      );
}

class Job extends RequestItem {
  const Job({
    required super.id,
    required super.number,
    required super.status,
    required super.description,
    super.statusLabel,
    super.categoryName,
    super.address,
    super.location,
    super.createdAt,
    super.customerFullName,
    this.availableActions = const [],
  });
  final List<String> availableActions;
  factory Job.fromJson(Map<String, dynamic> json) {
    final jobId = (json['id'] ?? json['requestId']) as int;
    final customer = json['customer'] as Map?;
    final location = json['location'] as Map?;
    final category = json['category'] as Map?;
    return Job(
      id: jobId,
      number: (json['requestNumber'] ?? json['number'] ?? '#$jobId').toString(),
      status: '${json['status'] ?? json['requestStatus']}',
      description: '${json['description'] ?? ''}',
      statusLabel:
          json['statusLabel']?.toString() ??
          json['requestStatusLabel']?.toString(),
      categoryName: category?['name']?.toString(),
      address: json['address']?.toString() ?? location?['address']?.toString(),
      location: location == null ? null : RequestLocation.fromJson(location),
      createdAt: json['createdAt']?.toString(),
      customerFullName: customer?['fullName']?.toString(),
      availableActions: (json['availableActions'] as List? ?? [])
          .map((e) => '$e')
          .toList(),
    );
  }
}

enum RealtimeEventType {
  requestCreated,
  requestUpdated,
  requestAssigned,
  requestAssignmentCreated,
  requestAssignmentAccepted,
  requestAssignmentRejected,
  requestReassigned,
  requestUnassigned,
  requestScheduleChanged,
  requestDiagnosisUpdated,
  requestAttachmentsChanged,
  requestStatusChanged,
  requestDeleted,
  dashboardInvalidated,
  notificationCreated,
  notificationRead,
  chatMessageCreated,
  chatMessageRead,
  chatTypingStarted,
  chatTypingStopped,
  unknown;

  static RealtimeEventType fromString(String? value) {
    if (value == null) return RealtimeEventType.unknown;
    switch (value) {
      case 'REQUEST_CREATED':
        return RealtimeEventType.requestCreated;
      case 'REQUEST_UPDATED':
        return RealtimeEventType.requestUpdated;
      case 'REQUEST_ASSIGNED':
        return RealtimeEventType.requestAssigned;
      case 'REQUEST_ASSIGNMENT_CREATED':
        return RealtimeEventType.requestAssignmentCreated;
      case 'REQUEST_ASSIGNMENT_ACCEPTED':
        return RealtimeEventType.requestAssignmentAccepted;
      case 'REQUEST_ASSIGNMENT_REJECTED':
        return RealtimeEventType.requestAssignmentRejected;
      case 'REQUEST_REASSIGNED':
        return RealtimeEventType.requestReassigned;
      case 'REQUEST_UNASSIGNED':
        return RealtimeEventType.requestUnassigned;
      case 'REQUEST_SCHEDULE_CHANGED':
        return RealtimeEventType.requestScheduleChanged;
      case 'REQUEST_DIAGNOSIS_UPDATED':
        return RealtimeEventType.requestDiagnosisUpdated;
      case 'REQUEST_ATTACHMENTS_CHANGED':
        return RealtimeEventType.requestAttachmentsChanged;
      case 'REQUEST_STATUS_CHANGED':
        return RealtimeEventType.requestStatusChanged;
      case 'REQUEST_DELETED':
        return RealtimeEventType.requestDeleted;
      case 'DASHBOARD_INVALIDATED':
        return RealtimeEventType.dashboardInvalidated;
      case 'NOTIFICATION_CREATED':
        return RealtimeEventType.notificationCreated;
      case 'NOTIFICATION_READ':
        return RealtimeEventType.notificationRead;
      case 'CHAT_MESSAGE_CREATED':
        return RealtimeEventType.chatMessageCreated;
      case 'CHAT_MESSAGE_READ':
        return RealtimeEventType.chatMessageRead;
      case 'CHAT_TYPING_STARTED':
        return RealtimeEventType.chatTypingStarted;
      case 'CHAT_TYPING_STOPPED':
        return RealtimeEventType.chatTypingStopped;
      default:
        return RealtimeEventType.unknown;
    }
  }

  String get wireName {
    switch (this) {
      case RealtimeEventType.requestCreated:
        return 'REQUEST_CREATED';
      case RealtimeEventType.requestUpdated:
        return 'REQUEST_UPDATED';
      case RealtimeEventType.requestAssigned:
        return 'REQUEST_ASSIGNED';
      case RealtimeEventType.requestAssignmentCreated:
        return 'REQUEST_ASSIGNMENT_CREATED';
      case RealtimeEventType.requestAssignmentAccepted:
        return 'REQUEST_ASSIGNMENT_ACCEPTED';
      case RealtimeEventType.requestAssignmentRejected:
        return 'REQUEST_ASSIGNMENT_REJECTED';
      case RealtimeEventType.requestReassigned:
        return 'REQUEST_REASSIGNED';
      case RealtimeEventType.requestUnassigned:
        return 'REQUEST_UNASSIGNED';
      case RealtimeEventType.requestScheduleChanged:
        return 'REQUEST_SCHEDULE_CHANGED';
      case RealtimeEventType.requestDiagnosisUpdated:
        return 'REQUEST_DIAGNOSIS_UPDATED';
      case RealtimeEventType.requestAttachmentsChanged:
        return 'REQUEST_ATTACHMENTS_CHANGED';
      case RealtimeEventType.requestStatusChanged:
        return 'REQUEST_STATUS_CHANGED';
      case RealtimeEventType.requestDeleted:
        return 'REQUEST_DELETED';
      case RealtimeEventType.dashboardInvalidated:
        return 'DASHBOARD_INVALIDATED';
      case RealtimeEventType.notificationCreated:
        return 'NOTIFICATION_CREATED';
      case RealtimeEventType.notificationRead:
        return 'NOTIFICATION_READ';
      case RealtimeEventType.chatMessageCreated:
        return 'CHAT_MESSAGE_CREATED';
      case RealtimeEventType.chatMessageRead:
        return 'CHAT_MESSAGE_READ';
      case RealtimeEventType.chatTypingStarted:
        return 'CHAT_TYPING_STARTED';
      case RealtimeEventType.chatTypingStopped:
        return 'CHAT_TYPING_STOPPED';
      case RealtimeEventType.unknown:
        return 'UNKNOWN';
    }
  }

  bool get isChatEvent =>
      this == RealtimeEventType.chatMessageCreated ||
      this == RealtimeEventType.chatMessageRead ||
      this == RealtimeEventType.chatTypingStarted ||
      this == RealtimeEventType.chatTypingStopped;

  bool get isNotificationEvent =>
      this == RealtimeEventType.notificationCreated ||
      this == RealtimeEventType.notificationRead;

  bool get isRequestDomainEvent =>
      !isChatEvent && !isNotificationEvent && this != RealtimeEventType.unknown;
}

class RequestRealtimePayload {
  const RequestRealtimePayload({
    required this.requestId,
    this.requestNumber,
    this.customerId,
    this.technicianId,
    this.status,
    this.oldStatus,
    this.priority,
  });
  final int requestId;
  final String? requestNumber;
  final int? customerId;
  final int? technicianId;
  final String? status;
  final String? oldStatus;
  final String? priority;

  factory RequestRealtimePayload.fromJson(Map<String, dynamic> json) =>
      RequestRealtimePayload(
        requestId: (json['requestId'] as num?)?.toInt() ?? 0,
        requestNumber: json['requestNumber']?.toString(),
        customerId: (json['customerId'] as num?)?.toInt(),
        technicianId: (json['technicianId'] as num?)?.toInt(),
        status: json['status']?.toString(),
        oldStatus: json['oldStatus']?.toString(),
        priority: json['priority']?.toString(),
      );
}

class AssignmentRealtimePayload {
  const AssignmentRealtimePayload({
    required this.requestId,
    this.requestNumber,
    this.technicianId,
    this.assignmentId,
    this.customerId,
    this.previousTechnicianId,
    this.action,
    this.status,
    this.reason,
  });
  final int requestId;
  final String? requestNumber;
  final int? technicianId;
  final int? assignmentId;
  final int? customerId;
  final int? previousTechnicianId;
  final String? action;
  final String? status;
  final String? reason;

  factory AssignmentRealtimePayload.fromJson(Map<String, dynamic> json) =>
      AssignmentRealtimePayload(
        requestId: (json['requestId'] as num?)?.toInt() ?? 0,
        requestNumber: json['requestNumber']?.toString(),
        technicianId: (json['technicianId'] as num?)?.toInt(),
        assignmentId: (json['assignmentId'] as num?)?.toInt(),
        customerId: (json['customerId'] as num?)?.toInt(),
        previousTechnicianId:
            (json['previousTechnicianId'] as num?)?.toInt() ??
            (json['oldTechnicianId'] as num?)?.toInt(),
        action: json['action']?.toString(),
        status: json['status']?.toString(),
        reason: json['reason']?.toString(),
      );
}

class ScheduleRealtimePayload {
  const ScheduleRealtimePayload({
    required this.requestId,
    this.requestNumber,
    this.assignmentId,
    this.technicianId,
    this.customerId,
    this.scheduledStart,
    this.scheduledEnd,
    this.scheduledVisitAt,
    this.scheduleAction,
  });
  final int requestId;
  final String? requestNumber;
  final int? assignmentId;
  final int? technicianId;
  final int? customerId;
  final String? scheduledStart;
  final String? scheduledEnd;
  final String? scheduledVisitAt;
  final String? scheduleAction;

  factory ScheduleRealtimePayload.fromJson(Map<String, dynamic> json) =>
      ScheduleRealtimePayload(
        requestId: (json['requestId'] as num?)?.toInt() ?? 0,
        requestNumber: json['requestNumber']?.toString(),
        assignmentId: (json['assignmentId'] as num?)?.toInt(),
        technicianId: (json['technicianId'] as num?)?.toInt(),
        customerId: (json['customerId'] as num?)?.toInt(),
        scheduledStart: json['scheduledStart']?.toString(),
        scheduledEnd: json['scheduledEnd']?.toString(),
        scheduledVisitAt:
            json['scheduledVisitAt']?.toString() ??
            json['scheduledStart']?.toString(),
        scheduleAction: json['scheduleAction']?.toString(),
      );
}

class DiagnosisRealtimePayload {
  const DiagnosisRealtimePayload({
    required this.requestId,
    this.requestNumber,
    this.executionId,
    this.technicianId,
    this.customerId,
    this.diagnosis,
  });
  final int requestId;
  final String? requestNumber;
  final int? executionId;
  final int? technicianId;
  final int? customerId;
  final String? diagnosis;

  factory DiagnosisRealtimePayload.fromJson(Map<String, dynamic> json) =>
      DiagnosisRealtimePayload(
        requestId: (json['requestId'] as num?)?.toInt() ?? 0,
        requestNumber: json['requestNumber']?.toString(),
        executionId: (json['executionId'] as num?)?.toInt(),
        technicianId: (json['technicianId'] as num?)?.toInt(),
        customerId: (json['customerId'] as num?)?.toInt(),
        diagnosis: json['diagnosis']?.toString(),
      );
}

class AttachmentRealtimePayload {
  const AttachmentRealtimePayload({
    required this.requestId,
    this.requestNumber,
    this.attachmentId,
    this.changeType,
    this.customerId,
    this.technicianId,
    this.type,
    this.fileName,
  });
  final int requestId;
  final String? requestNumber;
  final int? attachmentId;
  final String? changeType;
  final int? customerId;
  final int? technicianId;
  final String? type;
  final String? fileName;

  factory AttachmentRealtimePayload.fromJson(Map<String, dynamic> json) =>
      AttachmentRealtimePayload(
        requestId: (json['requestId'] as num?)?.toInt() ?? 0,
        requestNumber: json['requestNumber']?.toString(),
        attachmentId: (json['attachmentId'] as num?)?.toInt(),
        changeType: json['changeType']?.toString(),
        customerId: (json['customerId'] as num?)?.toInt(),
        technicianId: (json['technicianId'] as num?)?.toInt(),
        type: json['type']?.toString(),
        fileName: json['fileName']?.toString(),
      );
}

class RequestDeletedRealtimePayload {
  const RequestDeletedRealtimePayload({
    required this.requestId,
    this.requestNumber,
    this.customerId,
    this.technicianId,
  });
  final int requestId;
  final String? requestNumber;
  final int? customerId;
  final int? technicianId;

  factory RequestDeletedRealtimePayload.fromJson(Map<String, dynamic> json) =>
      RequestDeletedRealtimePayload(
        requestId: (json['requestId'] as num?)?.toInt() ?? 0,
        requestNumber: json['requestNumber']?.toString(),
        customerId: (json['customerId'] as num?)?.toInt(),
        technicianId: (json['technicianId'] as num?)?.toInt(),
      );
}

class NotificationRealtimePayload {
  const NotificationRealtimePayload({
    required this.notificationId,
    this.notificationType,
    this.targetId,
    this.target,
    this.read = false,
    this.title,
    this.body,
  });
  final int notificationId;
  final String? notificationType;
  final int? targetId;
  final String? target;
  final bool read;
  final String? title;
  final String? body;

  factory NotificationRealtimePayload.fromJson(Map<String, dynamic> json) =>
      NotificationRealtimePayload(
        notificationId: (json['notificationId'] as num?)?.toInt() ?? 0,
        notificationType:
            json['notificationType']?.toString() ?? json['type']?.toString(),
        targetId: (json['targetId'] as num?)?.toInt(),
        target: json['target']?.toString(),
        read: json['read'] as bool? ?? false,
        title: json['title']?.toString(),
        body: json['body']?.toString(),
      );
}

class ChatMessageRealtimePayload {
  const ChatMessageRealtimePayload({
    required this.messageId,
    required this.conversationId,
    required this.senderType,
    required this.senderId,
    this.clientMessageId,
    required this.messageType,
    this.text,
    this.attachmentId,
    this.replyToMessageId,
    this.createdAt,
  });
  final int messageId;
  final int conversationId;
  final String senderType;
  final int senderId;
  final String? clientMessageId;
  final String messageType;
  final String? text;
  final int? attachmentId;
  final int? replyToMessageId;
  final String? createdAt;

  factory ChatMessageRealtimePayload.fromJson(Map<String, dynamic> json) =>
      ChatMessageRealtimePayload(
        messageId: (json['messageId'] as num?)?.toInt() ?? 0,
        conversationId: (json['conversationId'] as num?)?.toInt() ?? 0,
        senderType: json['senderType']?.toString() ?? '',
        senderId: (json['senderId'] as num?)?.toInt() ?? 0,
        clientMessageId: json['clientMessageId']?.toString(),
        messageType: json['messageType']?.toString() ?? 'TEXT',
        text: json['text']?.toString(),
        attachmentId: (json['attachmentId'] as num?)?.toInt(),
        replyToMessageId: (json['replyToMessageId'] as num?)?.toInt(),
        createdAt: json['createdAt']?.toString(),
      );
}

class ChatReadRealtimePayload {
  const ChatReadRealtimePayload({
    required this.conversationId,
    required this.messageId,
    required this.readerType,
    required this.readerId,
    this.readAt,
  });
  final int conversationId;
  final int messageId;
  final String readerType;
  final int readerId;
  final String? readAt;

  factory ChatReadRealtimePayload.fromJson(Map<String, dynamic> json) =>
      ChatReadRealtimePayload(
        conversationId: (json['conversationId'] as num?)?.toInt() ?? 0,
        messageId: (json['messageId'] as num?)?.toInt() ?? 0,
        readerType: json['readerType']?.toString() ?? '',
        readerId: (json['readerId'] as num?)?.toInt() ?? 0,
        readAt: json['readAt']?.toString(),
      );
}

class ChatTypingRealtimePayload {
  const ChatTypingRealtimePayload({
    required this.conversationId,
    required this.actorType,
    required this.actorId,
    this.typing = false,
  });
  final int conversationId;
  final String actorType;
  final int actorId;
  final bool typing;

  factory ChatTypingRealtimePayload.fromJson(Map<String, dynamic> json) =>
      ChatTypingRealtimePayload(
        conversationId: (json['conversationId'] as num?)?.toInt() ?? 0,
        actorType: json['actorType']?.toString() ?? '',
        actorId: (json['actorId'] as num?)?.toInt() ?? 0,
        typing: json['typing'] as bool? ?? false,
      );
}

class RealtimeEnvelope<T> {
  const RealtimeEnvelope({
    required this.type,
    required this.rawType,
    required this.payload,
    this.rawPayload = const {},
    this.occurredAt,
    this.timestamp,
    this.eventId,
  });
  final RealtimeEventType type;
  final String rawType;
  final T payload;
  final Map<String, dynamic> rawPayload;
  final String? occurredAt;
  final String? timestamp;
  final String? eventId;

  int? get targetRequestId {
    final p = payload;
    if (p is RequestRealtimePayload) return p.requestId;
    if (p is AssignmentRealtimePayload) return p.requestId;
    if (p is ScheduleRealtimePayload) return p.requestId;
    if (p is DiagnosisRealtimePayload) return p.requestId;
    if (p is AttachmentRealtimePayload) return p.requestId;
    if (p is RequestDeletedRealtimePayload) return p.requestId;
    return (rawPayload['requestId'] as num?)?.toInt();
  }

  static RealtimeEnvelope<dynamic> fromJson(Map<String, dynamic> json) {
    final rawType = json['type']?.toString() ?? '';
    final type = RealtimeEventType.fromString(rawType);
    final rawPayload = json['payload'] is Map
        ? Map<String, dynamic>.from(json['payload'] as Map)
        : <String, dynamic>{};
    final occurredAt =
        json['occurredAt']?.toString() ?? json['timestamp']?.toString();
    final eventId = json['eventId']?.toString();

    dynamic parsedPayload;
    switch (type) {
      case RealtimeEventType.requestCreated:
      case RealtimeEventType.requestUpdated:
      case RealtimeEventType.requestStatusChanged:
        parsedPayload = RequestRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.requestAssigned:
      case RealtimeEventType.requestAssignmentCreated:
      case RealtimeEventType.requestAssignmentAccepted:
      case RealtimeEventType.requestAssignmentRejected:
      case RealtimeEventType.requestReassigned:
      case RealtimeEventType.requestUnassigned:
        parsedPayload = AssignmentRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.requestScheduleChanged:
        parsedPayload = ScheduleRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.requestDiagnosisUpdated:
        parsedPayload = DiagnosisRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.requestAttachmentsChanged:
        parsedPayload = AttachmentRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.requestDeleted:
        parsedPayload = RequestDeletedRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.notificationCreated:
      case RealtimeEventType.notificationRead:
        parsedPayload = NotificationRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.chatMessageCreated:
        parsedPayload = ChatMessageRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.chatMessageRead:
        parsedPayload = ChatReadRealtimePayload.fromJson(rawPayload);
        break;
      case RealtimeEventType.chatTypingStarted:
        parsedPayload = ChatTypingRealtimePayload.fromJson({
          ...rawPayload,
          'typing': true,
        });
        break;
      case RealtimeEventType.chatTypingStopped:
        parsedPayload = ChatTypingRealtimePayload.fromJson({
          ...rawPayload,
          'typing': false,
        });
        break;
      default:
        parsedPayload = rawPayload;
        break;
    }

    return RealtimeEnvelope<dynamic>(
      type: type,
      rawType: rawType,
      payload: parsedPayload,
      rawPayload: rawPayload,
      occurredAt: occurredAt,
      timestamp: occurredAt,
      eventId: eventId,
    );
  }
}
