import 'dart:io';

import 'api_client.dart';
import 'models.dart';

class AuthRepository {
  AuthRepository(this.api);
  final ApiClient api;
  Actor? actor;

  Future<Actor> loginCustomer(String idToken) =>
      _login('/auth/telegram/customer', idToken);
  Future<Actor> loginTechnician(String idToken) =>
      _login('/auth/telegram/technician', idToken);

  Future<Actor> _login(String path, String idToken) async {
    final data = await api.post(
      path,
      body: {'idToken': idToken},
    ) as Map<String, dynamic>;
    await api.authStore.save(
      access: data['accessToken'] as String,
      refresh: data['refreshToken'] as String,
    );
    return actor = Actor.fromJson(data['actor'] as Map<String, dynamic>);
  }

  Future<void> logout() async {
    final refresh = await api.authStore.refreshToken;
    if (refresh != null) {
      try {
        await api.post('/auth/logout', body: {'refreshToken': refresh});
      } catch (_) {}
    }
    await api.authStore.clear();
    actor = null;
  }
}

class CustomerRepository {
  CustomerRepository(this.api);
  final ApiClient api;

  Future<PageResponse<RequestItem>> requests({int page = 0}) async =>
      PageResponse.fromJson(
        await api.get('/me/repair-requests', query: {'page': page, 'size': 20})
            as Map<String, dynamic>,
        RequestItem.fromJson,
      );
  Future<RequestItem> request(int id) async => RequestItem.fromJson(
    await api.get('/me/repair-requests/$id') as Map<String, dynamic>,
  );
  Future<dynamic> createRequest({
    required String description,
    required int categoryId,
    String? address,
    double? latitude,
    double? longitude,
    String? locationSource,
    String? idempotencyKey,
  }) {
    final location = <String, dynamic>{
      'address': ?address,
      'latitude': ?latitude,
      'longitude': ?longitude,
      'source': ?locationSource,
    };
    return api.post(
      '/me/repair-requests',
      body: {
        'description': description,
        'categoryId': categoryId,
        if (location.isNotEmpty) 'location': location,
      },
      headers: {
        'Idempotency-Key':
            idempotencyKey ?? DateTime.now().microsecondsSinceEpoch.toString(),
      },
    );
  }
  Future<dynamic> uploadPhoto(int id, File file) => api.upload(
    '/me/repair-requests/$id/attachments',
    file,
    fieldName: 'file',
  );
  Future<List<Map<String, dynamic>>> timeline(int id) async =>
      (await api.get('/me/repair-requests/$id/timeline') as List<dynamic>? ??
              [])
          .whereType<Map<String, dynamic>>()
          .toList();
  Future<List<Map<String, dynamic>>> attachments(int id) async =>
      (await api.get('/me/repair-requests/$id/attachments') as List<dynamic>? ??
              [])
          .whereType<Map<String, dynamic>>()
          .toList();
  Future<dynamic> submitReview(int id, int rating, String comment) => api.post(
    '/me/repair-requests/$id/review',
    body: {'rating': rating, 'comment': comment},
  );
}

class TechnicianRepository {
  TechnicianRepository(this.api);
  final ApiClient api;

  Future<PageResponse<Job>> jobs({String view = 'ACTIVE'}) async =>
      PageResponse.fromJson(
        await api.get('/me/jobs', query: {'view': view, 'page': 0, 'size': 20})
            as Map<String, dynamic>,
        Job.fromJson,
      );
  Future<Job> job(int id) async =>
      Job.fromJson(await api.get('/me/jobs/$id') as Map<String, dynamic>);
  Future<dynamic> action(int id, String action, {Map<String, dynamic>? body}) =>
      api.post('/me/jobs/$id/$action', body: body);
  Future<dynamic> diagnosis(int id, String text) =>
      api.patch('/me/jobs/$id/diagnosis', body: {'diagnosis': text});
  Future<dynamic> uploadPhoto(int id, File file, String attachmentType) =>
      api.upload(
        '/me/jobs/$id/attachments',
        file,
        fields: {'attachmentType': attachmentType},
        fieldName: 'file',
      );
  Future<List<Map<String, dynamic>>> schedule({
    required String from,
    required String to,
  }) async =>
      (await api.get('/me/schedule', query: {'from': from, 'to': to})
                  as List<dynamic>? ??
              [])
          .whereType<Map<String, dynamic>>()
          .toList();
}

class NotificationRepository {
  NotificationRepository(this.api);
  final ApiClient api;
  Future<PageResponse<NotificationItem>> list({bool? unread}) async =>
      PageResponse.fromJson(
        await api.get(
          '/me/notifications',
          query: {'page': 0, 'size': 20, 'unread': ?unread},
        ) as Map<String, dynamic>,
        NotificationItem.fromJson,
      );
  Future<int> unreadCount() async =>
      ((await api.get('/me/notifications/unread-count')
              as Map<String, dynamic>)['unreadCount']
          as int? ??
      0);
  Future<void> markRead(int id) async {
    await api.patch('/me/notifications/$id/read');
  }

  Future<void> markAllRead() async {
    await api.post('/me/notifications/read-all');
  }
}

class MobileProfileRepository {
  MobileProfileRepository(this.api);
  final ApiClient api;

  Future<Actor> get() async =>
      Actor.fromJson(await api.get('/me') as Map<String, dynamic>);

  Future<Actor> update({String? fullName, String? preferredLanguage}) async =>
      Actor.fromJson(
        await api.patch(
          '/me',
          body: {
            'fullName': ?fullName,
            'preferredLanguage': ?preferredLanguage,
          },
        ) as Map<String, dynamic>,
      );
}
