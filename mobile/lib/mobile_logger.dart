import 'dart:developer' as developer;

class MobileLog {
  static const _loggerName = 'RepairAutoMobile';

  const MobileLog._();

  static void info(String message) {
    developer.log(message, name: _loggerName);
  }

  static void warning(String message, {Object? error, StackTrace? stackTrace}) {
    developer.log(
      message,
      name: _loggerName,
      level: 900,
      error: error,
      stackTrace: stackTrace,
    );
  }

  static void severe(String message, {Object? error, StackTrace? stackTrace}) {
    developer.log(
      message,
      name: _loggerName,
      level: 1000,
      error: error,
      stackTrace: stackTrace,
    );
  }

  static bool present(String? value) => value != null && value.isNotEmpty;

  static int safeLength(String? value) => value?.length ?? 0;
}
