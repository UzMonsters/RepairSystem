import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'https://repair-auto.onrender.com',
);

class ApiException implements Exception {
  const ApiException(
    this.statusCode,
    this.message, {
    this.code,
    this.fieldErrors = const [],
  });
  final int statusCode;
  final String message;
  final String? code;
  final List<ApiFieldError> fieldErrors;

  @override
  String toString() {
    final details = fieldErrors
        .map((error) => error.message)
        .where((message) => message.isNotEmpty)
        .join('\n');
    return details.isEmpty
        ? 'ApiException($statusCode, $code, $message)'
        : 'ApiException($statusCode, $code, $message\n$details)';
  }
}

class ApiFieldError {
  const ApiFieldError({this.field, this.code, required this.message});

  final String? field;
  final String? code;
  final String message;
}

class AuthStore {
  AuthStore([FlutterSecureStorage? storage])
    : _storage = storage ?? const FlutterSecureStorage();

  static const _accessKey = 'repairauto_access_token';
  static const _refreshKey = 'repairauto_refresh_token';
  final FlutterSecureStorage _storage;

  Future<String?> get accessToken => _readToken(_accessKey);
  Future<String?> get refreshToken => _readToken(_refreshKey);

  Future<String?> _readToken(String key) async {
    try {
      return await _storage.read(key: key);
    } on PlatformException catch (error) {
      final message = error.message ?? '';
      final details = '${error.details ?? ''}';
      if (error.code == 'read' ||
          message == 'read' ||
          details.contains('BadPaddingException') ||
          details.contains('BAD_DECRYPT')) {
        await clear();
        return null;
      }
      rethrow;
    }
  }

  Future<void> save({required String access, required String refresh}) async {
    await _storage.write(key: _accessKey, value: access);
    await _storage.write(key: _refreshKey, value: refresh);
  }

  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}

class ApiClient {
  ApiClient({http.Client? client, AuthStore? authStore})
    : _client = client ?? http.Client(),
      authStore = authStore ?? AuthStore();

  final http.Client _client;
  final AuthStore authStore;
  Future<bool>? _refreshing;
  String language = 'uz';
  void Function(String newAccessToken)? onTokenRefreshed;

  void setLanguage(String value) {
    final normalized = value.toLowerCase().split('-').first;
    if (normalized == 'ru' || normalized == 'en' || normalized == 'uz') {
      language = normalized;
    }
  }

  Uri _uri(String path, [Map<String, dynamic>? query]) {
    final base = Uri.parse(apiBaseUrl);
    final rootPath = path.startsWith('/root/')
        ? path.substring('/root'.length)
        : null;
    return base.replace(
      path:
          '${base.path}/api/v1${rootPath ?? '/mobile${path.startsWith('/') ? path : '/$path'}'}',
      queryParameters: query?.map((key, value) => MapEntry(key, '$value')),
    );
  }

  Future<Map<String, String>> _headers({
    String? language,
    String? token,
  }) async {
    final access = token ?? await authStore.accessToken;
    return {
      HttpHeaders.acceptHeader: 'application/json',
      HttpHeaders.contentTypeHeader: 'application/json',
      HttpHeaders.acceptLanguageHeader: language ?? this.language,
      if (access != null && access.isNotEmpty)
        HttpHeaders.authorizationHeader: 'Bearer $access',
    };
  }

  Future<dynamic> request(
    String method,
    String path, {
    Map<String, dynamic>? query,
    Object? body,
    Map<String, String>? headers,
    bool retryOn401 = true,
  }) async {
    final requestHeaders = await _headers();
    if (headers != null) requestHeaders.addAll(headers);
    final uri = _uri(path, query);
    final encoded = body == null ? null : jsonEncode(body);
    final response = await _send(method, uri, requestHeaders, encoded);

    if (response.statusCode == 401 && retryOn401 && await _refresh()) {
      return request(
        method,
        path,
        query: query,
        body: body,
        headers: headers,
        retryOn401: false,
      );
    }
    return _decode(response);
  }

  Future<http.Response> _send(
    String method,
    Uri uri,
    Map<String, String> headers,
    String? body,
  ) {
    switch (method) {
      case 'GET':
        return _client.get(uri, headers: headers);
      case 'POST':
        return _client.post(uri, headers: headers, body: body);
      case 'PATCH':
        return _client.patch(uri, headers: headers, body: body);
      case 'PUT':
        return _client.put(uri, headers: headers, body: body);
      case 'DELETE':
        return _client.delete(uri, headers: headers, body: body);
      default:
        throw ArgumentError('Unsupported HTTP method: $method');
    }
  }

  dynamic _decode(http.Response response) {
    dynamic value;
    try {
      value = response.body.isEmpty ? null : jsonDecode(response.body);
    } catch (_) {
      value = response.body;
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final data = value is Map ? value : <String, dynamic>{};
      final fieldErrors = (data['fieldErrors'] as List<dynamic>? ?? [])
          .whereType<Map>()
          .map(
            (error) => ApiFieldError(
              field: error['field']?.toString(),
              code: error['code']?.toString(),
              message: error['message']?.toString() ?? '',
            ),
          )
          .toList(growable: false);
      throw ApiException(
        response.statusCode,
        '${data['message'] ?? 'Request failed'}',
        code: data['code']?.toString(),
        fieldErrors: fieldErrors,
      );
    }
    return value;
  }

  Future<bool> _refresh() {
    return _refreshing ??= _doRefresh().whenComplete(() => _refreshing = null);
  }

  Future<bool> _doRefresh() async {
    final refresh = await authStore.refreshToken;
    if (refresh == null || refresh.isEmpty) return false;
    final response = await _client.post(
      _uri('/auth/refresh'),
      headers: {
        HttpHeaders.acceptHeader: 'application/json',
        HttpHeaders.contentTypeHeader: 'application/json',
      },
      body: jsonEncode({'refreshToken': refresh}),
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      await authStore.clear();
      return false;
    }
    final data = jsonDecode(response.body) as Map<String, dynamic>;
    final newAccess = data['accessToken'] as String;
    final newRefresh = data['refreshToken'] as String;
    await authStore.save(access: newAccess, refresh: newRefresh);
    onTokenRefreshed?.call(newAccess);
    return true;
  }

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) =>
      request('GET', path, query: query);
  Future<dynamic> post(
    String path, {
    Object? body,
    Map<String, dynamic>? query,
    Map<String, String>? headers,
  }) => request('POST', path, body: body, query: query, headers: headers);
  Future<dynamic> patch(
    String path, {
    Object? body,
    Map<String, dynamic>? query,
  }) => request('PATCH', path, body: body, query: query);

  Future<dynamic> put(
    String path, {
    Object? body,
    Map<String, dynamic>? query,
    Map<String, String>? headers,
  }) => request('PUT', path, body: body, query: query, headers: headers);
  Future<dynamic> delete(String path, {Object? body}) =>
      request('DELETE', path, body: body);

  Future<dynamic> upload(
    String path,
    File file, {
    Map<String, String> fields = const {},
    String fieldName = 'file',
    Map<String, String> headers = const {},
  }) async {
    final request = http.MultipartRequest('POST', _uri(path));
    final token = await authStore.accessToken;
    request.headers.addAll({
      HttpHeaders.acceptLanguageHeader: language,
      if (token != null) HttpHeaders.authorizationHeader: 'Bearer $token',
      ...headers,
    });
    request.fields.addAll(fields);
    request.files.add(await http.MultipartFile.fromPath(fieldName, file.path));
    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);
    if (response.statusCode == 401 && await _refresh()) {
      return upload(
        path,
        file,
        fields: fields,
        fieldName: fieldName,
        headers: headers,
      );
    }
    return _decode(response);
  }
}
