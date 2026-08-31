import 'dart:io';

import 'api_client.dart';
import 'models.dart';

class CategoryRepository {
  CategoryRepository(this.api);
  final ApiClient api;

  Future<List<Category>> list() async {
    final data = await api.get('/root/categories', query: {'active': true, 'size': 100});
    final content = data is Map ? data['content'] : data;
    return (content as List? ?? [])
        .whereType<Map<String, dynamic>>()
        .map(Category.fromJson)
        .toList();
  }
}

class AuthRepository {
  AuthRepository(this.api);
  final ApiClient api;
  Actor? actor;

  Future<Actor> loginCustomer(String idToken, {Map<String, dynamic>? device}) =>
      loginTelegram('CUSTOMER', idToken, device: device);

  Future<Actor> loginTechnician(String idToken, {Map<String, dynamic>? device}) =>
      loginTelegram('TECHNICIAN', idToken, device: device);

  Future<Actor> loginTelegram(
    String role,
    String idToken, {
    Map<String, dynamic>? device,
  }) => _login(
        role == 'CUSTOMER'
            ? '/auth/telegram/customer'
            : '/auth/telegram/technician',
        {'idToken': idToken, if (device != null) 'device': device},
      );

  Future<Actor> loginGoogle(
    String clientType,
    String idToken, {
    Map<String, dynamic>? device,
  }) => _login('/auth/google', {
        'clientType': clientType,
        'idToken': idToken,
        if (device != null) 'device': device,
      });

  Future<Map<String, dynamic>> requestPhoneOtp({
    required String clientType,
    required String phone,
  }) async => (await api.post(
        '/auth/phone/request-otp',
        body: {
          'clientType': clientType,
          'phone': phone,
        },
      ) as Map).cast<String, dynamic>();

  Future<Actor> verifyPhoneOtp(
    String challengeId,
    String code, {
    Map<String, dynamic>? device,
  }) => _login('/auth/phone/verify-otp', {
        'challengeId': challengeId,
        'code': code,
        if (device != null) 'device': device,
      });

  Future<Actor> _login(String path, Map<String, dynamic> body) async {
    final data = await api.post(
      path,
      body: body,
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

  Future<void> logoutAll() async {
    await api.post('/auth/logout-all');
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
  Future<Map<String, dynamic>> attachmentDownloadUrl(int attachmentId) async =>
      (await api.get('/attachments/$attachmentId/download-url') as Map)
          .cast<String, dynamic>();
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
  Future<dynamic> accept(int id) => api.post('/me/jobs/$id/accept');
  Future<void> reject(int id, String reason) async {
    await api.post('/me/jobs/$id/reject', body: {'reason': reason});
  }
  Future<dynamic> start(int id) => api.post('/me/jobs/$id/start');
  Future<dynamic> waitForParts(int id, String reason) =>
      api.post('/me/jobs/$id/wait-for-parts', body: {'reason': reason});
  Future<dynamic> resume(int id) => api.post('/me/jobs/$id/resume');
  Future<dynamic> complete(int id, String workPerformed, {String? completionNote}) =>
      api.post('/me/jobs/$id/complete', body: {
        'workPerformed': workPerformed,
        if (completionNote != null && completionNote.trim().isNotEmpty)
          'completionNote': completionNote,
      });
  Future<dynamic> diagnosis(int id, String text) =>
      api.patch('/me/jobs/$id/diagnosis', body: {'diagnosis': text});
  Future<dynamic> uploadPhoto(int id, File file, String attachmentType) =>
      api.upload(
        '/me/jobs/$id/attachments',
        file,
        fields: {'attachmentType': attachmentType},
        fieldName: 'file',
      );
  Future<List<Map<String, dynamic>>> attachments(int id) async =>
      (await api.get('/me/jobs/$id/attachments') as List<dynamic>? ?? [])
          .whereType<Map<String, dynamic>>()
          .toList();
  Future<List<Map<String, dynamic>>> schedule({
    required String from,
    required String to,
  }) async =>
      (await api.get('/me/schedule', query: {'from': from, 'to': to})
                  as List<dynamic>? ??
              [])
          .whereType<Map<String, dynamic>>()
          .toList();
  Future<Map<String, dynamic>> attachmentDownloadUrl(int attachmentId) async =>
      (await api.get('/attachments/$attachmentId/download-url') as Map)
          .cast<String, dynamic>();
}

class MobileChatRepository {
  MobileChatRepository(this.api);
  final ApiClient api;

  Future<PageResponse<Map<String, dynamic>>> conversations({int page = 0}) async =>
      PageResponse.fromJson(
        await api.get('/me/conversations', query: {'page': page, 'size': 20})
            as Map<String, dynamic>,
        (item) => item,
      );

  Future<Map<String, dynamic>> conversation(int id) async =>
    (await api.get('/me/conversations/$id') as Map).cast<String, dynamic>();

  Future<PageResponse<Map<String, dynamic>>> messages(
    int conversationId, {
    int page = 0,
    int? beforeId,
  }) async => PageResponse.fromJson(
        await api.get('/me/conversations/$conversationId/messages', query: {
          'page': page,
          'size': 20,
          if (beforeId != null) 'beforeId': beforeId,
        }) as Map<String, dynamic>,
        (item) => item,
      );

  Future<Map<String, dynamic>> getOrCreateForRequest(int requestId) async =>
      (await api.post('/me/conversations/requests/$requestId') as Map)
          .cast<String, dynamic>();

  Future<Map<String, dynamic>> sendMessage(
    int conversationId,
    String text, {
    String type = 'TEXT',
    int? attachmentId,
    String? clientMessageId,
    int? replyToMessageId,
  }) async => (await api.post('/me/conversations/$conversationId/messages', body: {
        'conversationId': conversationId,
        'clientMessageId': clientMessageId,
        'type': type,
        'text': text,
        'attachmentId': attachmentId,
        'replyToMessageId': replyToMessageId,
      }) as Map).cast<String, dynamic>();

  Future<void> markRead(int conversationId, int messageId) async {
    await api.post('/me/conversations/$conversationId/read', body: {
      'messageId': messageId,
    });
  }
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

  void setLanguage(String language) => api.setLanguage(language);

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

  Future<List<Map<String, dynamic>>> authMethods() async =>
      (await api.get('/me/auth-methods') as List<dynamic>? ?? [])
          .whereType<Map<String, dynamic>>()
          .toList();

  Future<List<Map<String, dynamic>>> sessions() async =>
      (await api.get('/me/sessions') as List<dynamic>? ?? [])
          .whereType<Map<String, dynamic>>()
          .toList();

  Future<void> revokeSession(String sessionId) async {
    await api.delete('/me/sessions/$sessionId');
  }

  Future<void> linkGoogle(String idToken) async {
    await api.post('/me/auth-methods/google', body: {'idToken': idToken});
  }

  Future<void> unlinkAuthMethod(String provider) async {
    await api.delete('/me/auth-methods/$provider');
  }

  Future<Map<String, dynamic>> requestEmailVerification({
    required String email,
  }) async => (await api.post(
        '/me/email/request-verification',
        body: {'email': email},
      ) as Map).cast<String, dynamic>();

  Future<void> verifyEmail(String challengeId, String code) async {
    await api.post(
      '/me/email/verify',
      body: {'challengeId': challengeId, 'code': code},
    );
  }

  Future<void> removeEmail() => api.delete('/me/email');

  Future<Map<String, dynamic>> requestPhoneVerification({
    required String phone,
  }) async => (await api.post(
        '/me/phone/request-verification',
        body: {'phone': phone},
      ) as Map).cast<String, dynamic>();

  Future<void> verifyPhone(String challengeId, String code) async {
    await api.post(
      '/me/phone/verify',
      body: {'challengeId': challengeId, 'code': code},
    );
  }

  Future<void> removePhone() => api.delete('/me/phone');

  Future<void> registerPushEndpoint({
    required String fcmRegistrationToken,
    required String clientType,
    required String platform,
    required String firebaseAppKey,
    String? appVersion,
  }) async {
    await api.put(
      '/push-endpoints',
      body: {
        'fcmRegistrationToken': fcmRegistrationToken,
        'clientType': clientType,
        'platform': platform,
        'firebaseAppKey': firebaseAppKey,
        if (appVersion != null) 'appVersion': appVersion,
      },
    );
  }

  Future<void> unregisterPushEndpoint({
    required String fcmRegistrationToken,
    required String firebaseAppKey,
  }) async {
    await api.delete(
      '/push-endpoints',
      body: {
        'fcmRegistrationToken': fcmRegistrationToken,
        'firebaseAppKey': firebaseAppKey,
      },
    );
  }
}
