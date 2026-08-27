import 'dart:convert';
import 'dart:io';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;

import 'mobile_logger.dart';

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

  Future<String?> get accessToken => _storage.read(key: _accessKey);
  Future<String?> get refreshToken => _storage.read(key: _refreshKey);

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
    bool authenticated = true,
  }) async {
    final access = authenticated ? token ?? await authStore.accessToken : token;
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
    bool authenticated = true,
  }) async {
    final requestHeaders = await _headers(authenticated: authenticated);
    if (headers != null) requestHeaders.addAll(headers);
    final uri = _uri(path, query);
    final encoded = body == null ? null : jsonEncode(body);
    final stopwatch = Stopwatch()..start();
    MobileLog.info(
      'HTTP request started method=$method path=${_safePath(uri)} '
      'bodyPresent=${body != null} authPresent=${requestHeaders.containsKey(HttpHeaders.authorizationHeader)}',
    );
    late final http.Response response;
    try {
      response = await _send(method, uri, requestHeaders, encoded);
      stopwatch.stop();
      MobileLog.info(
        'HTTP request completed method=$method path=${_safePath(uri)} '
        'status=${response.statusCode} durationMs=${stopwatch.elapsedMilliseconds} '
        'rndrId=${response.headers['rndr-id'] ?? 'none'} bodyLength=${response.body.length}',
      );
    } catch (error, stackTrace) {
      stopwatch.stop();
      MobileLog.severe(
        'HTTP request failed before response method=$method path=${_safePath(uri)} '
        'durationMs=${stopwatch.elapsedMilliseconds} error=${error.runtimeType}',
        error: error,
        stackTrace: stackTrace,
      );
      rethrow;
    }

    if (response.statusCode == 401 && retryOn401 && authenticated && await _refresh()) {
      MobileLog.info(
        'HTTP request retrying after refresh method=$method path=${_safePath(uri)}',
      );
      return request(
        method,
        path,
        query: query,
        body: body,
        headers: headers,
        retryOn401: false,
        authenticated: authenticated,
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
      MobileLog.warning(
        'HTTP response decoded as API error status=${response.statusCode} '
        'code=${data['code'] ?? 'unknown'} fieldErrorCount=${fieldErrors.length}',
      );
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
    if (refresh == null || refresh.isEmpty) {
      MobileLog.info('Token refresh skipped because no refresh token is stored');
      return false;
    }
    final uri = _uri('/auth/refresh');
    final stopwatch = Stopwatch()..start();
    MobileLog.info('Token refresh started path=${_safePath(uri)}');
    final response = await _client.post(
      uri,
      headers: {
        HttpHeaders.acceptHeader: 'application/json',
        HttpHeaders.contentTypeHeader: 'application/json',
      },
      body: jsonEncode({'refreshToken': refresh}),
    );
    stopwatch.stop();
    MobileLog.info(
      'Token refresh completed status=${response.statusCode} '
      'durationMs=${stopwatch.elapsedMilliseconds} rndrId=${response.headers['rndr-id'] ?? 'none'}',
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      MobileLog.warning('Token refresh failed, clearing stored tokens');
      await authStore.clear();
      return false;
    }
    final data = jsonDecode(response.body) as Map<String, dynamic>;
    await authStore.save(
      access: data['accessToken'] as String,
      refresh: data['refreshToken'] as String,
    );
    return true;
  }

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) =>
      request('GET', path, query: query);
  Future<dynamic> post(
    String path, {
    Object? body,
    Map<String, dynamic>? query,
    Map<String, String>? headers,
    bool retryOn401 = true,
    bool authenticated = true,
  }) => request(
    'POST',
    path,
    body: body,
    query: query,
    headers: headers,
    retryOn401: retryOn401,
    authenticated: authenticated,
  );
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
    final stopwatch = Stopwatch()..start();
    MobileLog.info(
      'Upload request started path=${_safePath(request.url)} '
      'fieldCount=${fields.length} authPresent=${token != null}',
    );
    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);
    stopwatch.stop();
    MobileLog.info(
      'Upload request completed path=${_safePath(request.url)} '
      'status=${response.statusCode} durationMs=${stopwatch.elapsedMilliseconds} '
      'rndrId=${response.headers['rndr-id'] ?? 'none'} bodyLength=${response.body.length}',
    );
    if (response.statusCode == 401 && await _refresh()) {
      MobileLog.info('Upload request retrying after refresh path=${_safePath(request.url)}');
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

  String _safePath(Uri uri) {
    if (uri.queryParameters.isEmpty) {
      return uri.path;
    }
    return '${uri.path}?queryKeys=${uri.queryParameters.keys.join(',')}';
  }
}
