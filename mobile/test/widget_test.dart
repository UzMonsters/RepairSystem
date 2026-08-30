import 'package:flutter_test/flutter_test.dart';
import 'package:repair_mobile/main.dart';

void main() {
  testWidgets('shows the login screen with initial role and title', (WidgetTester tester) async {
    await tester.pumpWidget(const RepairAutoApp());

    expect(find.text('RepairAuto'), findsOneWidget);
    expect(find.text('Клиент'), findsWidgets);
    expect(find.text('Техник'), findsOneWidget);
    expect(find.text('Продолжить через Telegram'), findsWidgets);
  });
}
