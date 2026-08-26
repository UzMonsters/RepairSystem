const fs = require('fs');
let code = fs.readFileSync('lib/api_client.dart', 'utf8');

if (!code.includes("import 'dart:async';")) {
    code = code.replace("import 'dart:convert';", "import 'dart:async';\nimport 'dart:convert';");
}

code = code.replace(
`  Future<http.Response> _send(
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
  }`,
`  Future<http.Response> _send(
    String method,
    Uri uri,
    Map<String, String> headers,
    String? body,
  ) async {
    try {
      Future<http.Response> req;
      switch (method) {
        case 'GET':
          req = _client.get(uri, headers: headers);
          break;
        case 'POST':
          req = _client.post(uri, headers: headers, body: body);
          break;
        case 'PATCH':
          req = _client.patch(uri, headers: headers, body: body);
          break;
        case 'PUT':
          req = _client.put(uri, headers: headers, body: body);
          break;
        case 'DELETE':
          req = _client.delete(uri, headers: headers, body: body);
          break;
        default:
          throw ArgumentError('Unsupported HTTP method: $method');
      }
      return await req.timeout(const Duration(seconds: 15));
    } on TimeoutException {
      throw const ApiException(408, 'Превышено время ожидания ответа от сервера');
    } on SocketException {
      throw const ApiException(503, 'Ошибка сети. Проверьте подключение к интернету');
    }
  }`
);

fs.writeFileSync('lib/api_client.dart', code, 'utf8');
console.log("Updated api_client.dart properly");