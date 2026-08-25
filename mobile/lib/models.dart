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
