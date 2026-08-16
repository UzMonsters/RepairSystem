import 'package:flutter/material.dart';

void main() => runApp(const RepairApp());

enum Role { customer, technician }
enum Lang { ru, uz, en }

const brown = Color(0xff934316);
const darkBrown = Color(0xff442719);
const cream = Color(0xfff7efe5);
const panel = Color(0xfffffbf6);
const muted = Color(0xff896c58);

const copy = {
  'ru': {'app': 'РемСервис', 'login': 'Войти', 'email': 'Email', 'password': 'Пароль', 'home': 'Главная', 'requests': 'Заявки', 'profile': 'Профиль', 'customer': 'Клиент', 'technician': 'Техник', 'newRequest': 'Новая заявка', 'myRequests': 'Мои заявки', 'assigned': 'Назначенные заявки', 'details': 'Детали заявки', 'description': 'Описание проблемы', 'category': 'Категория', 'address': 'Адрес', 'language': 'Язык', 'new': 'Новая', 'inWork': 'В работе', 'completed': 'Завершена', 'cancelled': 'Отменена', 'create': 'Создать заявку', 'start': 'Начать работу', 'finish': 'Завершить', 'notifications': 'Уведомления', 'demo': 'Демонстрационный режим', 'chooseRole': 'Выберите роль'},
  'uz': {'app': 'RemServis', 'login': 'Kirish', 'email': 'Email', 'password': 'Parol', 'home': 'Bosh sahifa', 'requests': 'So‘rovlar', 'profile': 'Profil', 'customer': 'Mijoz', 'technician': 'Usta', 'newRequest': 'Yangi so‘rov', 'myRequests': 'So‘rovlarim', 'assigned': 'Biriktirilgan so‘rovlar', 'details': 'So‘rov tafsilotlari', 'description': 'Muammo tavsifi', 'category': 'Kategoriya', 'address': 'Manzil', 'language': 'Til', 'new': 'Yangi', 'inWork': 'Ishda', 'completed': 'Tugallangan', 'cancelled': 'Bekor qilingan', 'create': 'So‘rov yaratish', 'start': 'Ishni boshlash', 'finish': 'Tugatish', 'notifications': 'Bildirishnomalar', 'demo': 'Demo rejim', 'chooseRole': 'Rolni tanlang'},
  'en': {'app': 'RepairService', 'login': 'Sign in', 'email': 'Email', 'password': 'Password', 'home': 'Home', 'requests': 'Requests', 'profile': 'Profile', 'customer': 'Customer', 'technician': 'Technician', 'newRequest': 'New request', 'myRequests': 'My requests', 'assigned': 'Assigned requests', 'details': 'Request details', 'description': 'Problem description', 'category': 'Category', 'address': 'Address', 'language': 'Language', 'new': 'New', 'inWork': 'In progress', 'completed': 'Completed', 'cancelled': 'Cancelled', 'create': 'Create request', 'start': 'Start work', 'finish': 'Finish', 'notifications': 'Notifications', 'demo': 'Demo mode', 'chooseRole': 'Choose a role'},
};

String tr(Lang lang, String key) => copy[lang.name]?[key] ?? key;

class RequestMock {
  const RequestMock(this.number, this.title, this.category, this.status, this.date, this.color);
  final String number, title, category, status, date;
  final Color color;
}

const requests = [
  RequestMock('#1257', 'Ремонт стиральной машины', 'Бытовая техника', 'work', '12 мая 2024', Color(0xffb56b35)),
  RequestMock('#1256', 'Ремонт холодильника', 'Холодильники', 'new', '10 мая 2024', Color(0xff6e8c76)),
  RequestMock('#1255', 'Установка розетки', 'Электрика', 'done', '8 мая 2024', Color(0xffb08a55)),
  RequestMock('#1254', 'Замена экрана', 'Смартфоны', 'cancelled', '5 мая 2024', Color(0xffb86455)),
];

class RepairApp extends StatefulWidget {
  const RepairApp({super.key});
  @override State<RepairApp> createState() => _RepairAppState();
}

class _RepairAppState extends State<RepairApp> {
  Lang lang = Lang.ru;
  Role role = Role.customer;
  bool loggedIn = false;

  @override
  Widget build(BuildContext context) {
    final text = (String key) => tr(lang, key);
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: text('app'),
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: cream,
        colorScheme: ColorScheme.fromSeed(seedColor: brown),
        fontFamily: 'Arial',
        inputDecorationTheme: InputDecorationTheme(
          filled: true, fillColor: panel,
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: Color(0xffe3cdb8))),
          enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: Color(0xffe3cdb8))),
        ),
      ),
      home: loggedIn
          ? AppShell(role: role, lang: lang, text: text, onLanguage: (v) => setState(() => lang = v))
          : LoginPage(role: role, lang: lang, text: text, onRole: (v) => setState(() => role = v), onLanguage: (v) => setState(() => lang = v), onLogin: () => setState(() => loggedIn = true)),
    );
  }
}

typedef TextOf = String Function(String key);

class LoginPage extends StatelessWidget {
  const LoginPage({super.key, required this.role, required this.lang, required this.text, required this.onRole, required this.onLanguage, required this.onLogin});
  final Role role; final Lang lang; final TextOf text; final ValueChanged<Role> onRole; final ValueChanged<Lang> onLanguage; final VoidCallback onLogin;
  @override
  Widget build(BuildContext context) => Scaffold(body: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.all(24), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 420), child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
    const CircleAvatar(radius: 34, backgroundColor: brown, child: Icon(Icons.build_rounded, color: Colors.white, size: 32)), const SizedBox(height: 16),
    Text(text('app'), textAlign: TextAlign.center, style: const TextStyle(fontSize: 30, fontWeight: FontWeight.w800, color: darkBrown)), const SizedBox(height: 6), Text(text('demo'), textAlign: TextAlign.center, style: const TextStyle(color: muted)), const SizedBox(height: 30),
    Text(text('chooseRole'), style: const TextStyle(fontWeight: FontWeight.w700)), const SizedBox(height: 10),
    SegmentedButton<Role>(segments: [ButtonSegment(value: Role.customer, label: Text(text('customer')), icon: const Icon(Icons.person_outline)), ButtonSegment(value: Role.technician, label: Text(text('technician')), icon: const Icon(Icons.handyman_outlined))], selected: {role}, onSelectionChanged: (v) => onRole(v.first)), const SizedBox(height: 18),
    TextField(decoration: InputDecoration(labelText: text('email'), prefixIcon: const Icon(Icons.mail_outline))), const SizedBox(height: 14), TextField(obscureText: true, decoration: InputDecoration(labelText: text('password'), prefixIcon: const Icon(Icons.lock_outline))), const SizedBox(height: 22),
    FilledButton(onPressed: onLogin, style: FilledButton.styleFrom(backgroundColor: brown, minimumSize: const Size.fromHeight(52)), child: Text(text('login'))), const SizedBox(height: 24),
    Text(text('language'), textAlign: TextAlign.center, style: const TextStyle(color: muted)), const SizedBox(height: 8), LanguageChips(value: lang, onChanged: onLanguage),
  ]))))));
}

class LanguageChips extends StatelessWidget {
  const LanguageChips({super.key, required this.value, required this.onChanged});
  final Lang value; final ValueChanged<Lang> onChanged;
  @override Widget build(BuildContext context) => Wrap(alignment: WrapAlignment.center, spacing: 8, children: Lang.values.map((item) => ChoiceChip(label: Text(item == Lang.ru ? 'Русский' : item == Lang.uz ? 'O‘zbek' : 'English'), selected: value == item, onSelected: (_) => onChanged(item))).toList());
}

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.role, required this.lang, required this.text, required this.onLanguage});
  final Role role; final Lang lang; final TextOf text; final ValueChanged<Lang> onLanguage;
  @override State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int index = 0;
  @override Widget build(BuildContext context) {
    final isCustomer = widget.role == Role.customer;
    final pages = isCustomer ? [CustomerHome(text: widget.text, onNew: () => Navigator.push(context, MaterialPageRoute(builder: (_) => CreateRequestPage(text: widget.text)))), RequestListPage(text: widget.text, technician: false), ProfilePage(text: widget.text, lang: widget.lang, onLanguage: widget.onLanguage)] : [TechnicianHome(text: widget.text), RequestListPage(text: widget.text, technician: true), ProfilePage(text: widget.text, lang: widget.lang, onLanguage: widget.onLanguage)];
    return Scaffold(
      appBar: AppBar(title: Text(index == 0 ? widget.text('home') : index == 1 ? (isCustomer ? widget.text('myRequests') : widget.text('assigned')) : widget.text('profile')), actions: [IconButton(onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => NotificationsPage(text: widget.text))), icon: const Icon(Icons.notifications_none))]),
      body: IndexedStack(index: index, children: pages),
      bottomNavigationBar: NavigationBar(selectedIndex: index, onDestinationSelected: (v) => setState(() => index = v), destinations: [NavigationDestination(icon: const Icon(Icons.home_outlined), selectedIcon: const Icon(Icons.home), label: widget.text('home')), NavigationDestination(icon: const Icon(Icons.assignment_outlined), selectedIcon: const Icon(Icons.assignment), label: widget.text('requests')), NavigationDestination(icon: const Icon(Icons.person_outline), selectedIcon: const Icon(Icons.person), label: widget.text('profile'))]),
    );
  }
}

class CustomerHome extends StatelessWidget {
  const CustomerHome({super.key, required this.text, required this.onNew}); final TextOf text; final VoidCallback onNew;
  @override Widget build(BuildContext context) => ListView(padding: const EdgeInsets.all(18), children: [Text('Добро пожаловать, Иван!', style: const TextStyle(fontSize: 25, fontWeight: FontWeight.w800, color: darkBrown)), const SizedBox(height: 18), FilledButton.icon(onPressed: onNew, icon: const Icon(Icons.add), label: Text(text('newRequest')), style: FilledButton.styleFrom(backgroundColor: brown, minimumSize: const Size.fromHeight(52))), const SizedBox(height: 22), SectionTitle(text('myRequests')), const SizedBox(height: 10), Row(children: [Metric('3', text('new'), const Color(0xff6e8c76)), Metric('2', text('inWork'), const Color(0xffc58a48)), Metric('5', 'Всего', brown)]), const SizedBox(height: 22), SectionTitle('Популярные услуги'), const SizedBox(height: 10), Wrap(spacing: 8, runSpacing: 8, children: ['Ремонт телефонов', 'Холодильники', 'Электрика', 'Сантехника'].map((v) => Chip(avatar: const Icon(Icons.build, size: 16), label: Text(v))).toList()), const SizedBox(height: 22), Card(child: ListTile(leading: const CircleAvatar(backgroundColor: Color(0xfff2dfca), child: Icon(Icons.local_offer, color: brown)), title: const Text('Скидка 10%', style: TextStyle(fontWeight: FontWeight.w700)), subtitle: const Text('На первый заказ через приложение')))]);
}

class TechnicianHome extends StatelessWidget {
  const TechnicianHome({super.key, required this.text}); final TextOf text;
  @override Widget build(BuildContext context) => ListView(padding: const EdgeInsets.all(18), children: [Text('Добро пожаловать, Алексей!', style: const TextStyle(fontSize: 25, fontWeight: FontWeight.w800, color: darkBrown)), const SizedBox(height: 20), Row(children: [Metric('4', 'Назначено', const Color(0xff829b87)), Metric('2', text('inWork'), const Color(0xffc58a48)), Metric('1', text('completed'), brown)]), const SizedBox(height: 20), Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [const Text('Мои показатели', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700)), const SizedBox(height: 16), const LinearProgressIndicator(value: .75, minHeight: 10, color: brown, backgroundColor: Color(0xfff0dfcf)), const SizedBox(height: 10), const Text('75% выполнено вовремя')]))) ]);
}

class RequestListPage extends StatelessWidget {
  const RequestListPage({super.key, required this.text, required this.technician}); final TextOf text; final bool technician;
  @override Widget build(BuildContext context) => ListView(padding: const EdgeInsets.all(16), children: [Wrap(spacing: 8, children: [ChoiceChip(label: const Text('Все'), selected: true, onSelected: (_) {}), ChoiceChip(label: Text(text('new')), selected: false, onSelected: (_) {}), ChoiceChip(label: Text(text('inWork')), selected: false, onSelected: (_) {})]), const SizedBox(height: 14), ...requests.map((r) => RequestCard(request: r, onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => RequestDetailsPage(text: text, request: r, technician: technician))))) ]);
}

class RequestCard extends StatelessWidget {
  const RequestCard({super.key, required this.request, this.onTap}); final RequestMock request; final VoidCallback? onTap;
  @override
  Widget build(BuildContext context) => Card(
    margin: const EdgeInsets.only(bottom: 12),
    child: InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Padding(
        padding: const EdgeInsets.all(15),
        child: Row(children: [
          CircleAvatar(backgroundColor: request.color.withOpacity(.12), child: Icon(Icons.build, color: request.color)),
          const SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(request.number, style: const TextStyle(fontSize: 12, color: muted)),
            const SizedBox(height: 3),
            Text(request.title, style: const TextStyle(fontWeight: FontWeight.w700)),
            Text(request.category, style: const TextStyle(color: muted)),
            const SizedBox(height: 7),
            Text(request.date, style: const TextStyle(fontSize: 12, color: muted)),
          ])),
          StatusChip(request.status),
        ]),
      ),
    ),
  );
}

class RequestDetailsPage extends StatelessWidget {
  const RequestDetailsPage({super.key, required this.text, required this.request, required this.technician}); final TextOf text; final RequestMock request; final bool technician;
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(text('details'))),
    body: ListView(padding: const EdgeInsets.all(18), children: [
      Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Text(request.number, style: const TextStyle(color: muted)), StatusChip(request.status)]),
        const SizedBox(height: 14),
        Text(request.title, style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w800, color: darkBrown)),
        const Divider(height: 28),
        const ListTile(contentPadding: EdgeInsets.zero, leading: Icon(Icons.location_on_outlined), title: Text('Адрес'), subtitle: Text('ул. Ленина, 45, кв. 12')),
        const ListTile(contentPadding: EdgeInsets.zero, leading: Icon(Icons.calendar_today_outlined), title: Text('Дата'), subtitle: Text('13 мая 2024, 10:00')),
      ]))),
      const SizedBox(height: 14),
      Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(text('description'), style: const TextStyle(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        const Text('Стиральная машина не сливает воду после стирки. Нужна диагностика и ремонт.'),
      ]))),
      if (technician) ...[
        const SizedBox(height: 20),
        Row(children: [
          Expanded(child: OutlinedButton.icon(onPressed: () {}, icon: const Icon(Icons.play_arrow), label: Text(text('start')))),
          const SizedBox(width: 10),
          Expanded(child: FilledButton.icon(onPressed: () {}, icon: const Icon(Icons.check), label: Text(text('finish')))),
        ]),
      ],
    ]),
  );
}

class CreateRequestPage extends StatelessWidget {
  const CreateRequestPage({super.key, required this.text}); final TextOf text;
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(text('create'))),
    body: ListView(padding: const EdgeInsets.all(18), children: [
      TextField(maxLines: 4, decoration: InputDecoration(labelText: text('description'), alignLabelWithHint: true)),
      const SizedBox(height: 14),
      DropdownButtonFormField<String>(
        decoration: InputDecoration(labelText: text('category')),
        items: ['Ремонт телефонов', 'Бытовая техника', 'Электрика', 'Сантехника'].map((v) => DropdownMenuItem(value: v, child: Text(v))).toList(),
        onChanged: (_) {},
      ),
      const SizedBox(height: 14),
      TextField(decoration: InputDecoration(labelText: text('address'), prefixIcon: const Icon(Icons.location_on_outlined))),
      const SizedBox(height: 22),
      FilledButton.icon(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.send), label: Text(text('create')), style: FilledButton.styleFrom(backgroundColor: brown, minimumSize: const Size.fromHeight(52))),
    ]),
  );
}

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key, required this.text, required this.lang, required this.onLanguage}); final TextOf text; final Lang lang; final ValueChanged<Lang> onLanguage;
  @override
  Widget build(BuildContext context) => ListView(
    padding: const EdgeInsets.all(18),
    children: [
      const Card(child: ListTile(
        contentPadding: EdgeInsets.all(14),
        leading: CircleAvatar(radius: 30, backgroundColor: brown, child: Text('АП', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700))),
        title: Text('Алексей Петров', style: TextStyle(fontWeight: FontWeight.w800)),
        subtitle: Text('Repair specialist'),
      )),
      const SizedBox(height: 14),
      Card(child: Column(children: [
        ListTile(leading: const Icon(Icons.person_outline), title: Text(text('profile')), trailing: const Icon(Icons.chevron_right)),
        ListTile(leading: const Icon(Icons.notifications_none), title: Text(text('notifications')), trailing: const Icon(Icons.chevron_right)),
        const Divider(height: 1),
        Padding(padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(text('language'), style: const TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(height: 10),
          LanguageChips(value: lang, onChanged: onLanguage),
        ])),
      ])),
    ],
  );
}

class NotificationsPage extends StatelessWidget {
  const NotificationsPage({super.key, required this.text}); final TextOf text;
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(text('notifications'))),
    body: ListView(padding: const EdgeInsets.all(16), children: const [
      Card(child: ListTile(leading: CircleAvatar(backgroundColor: Color(0xffe5f1e5), child: Icon(Icons.check, color: Color(0xff587e5d))), title: Text('Заявка обновлена'), subtitle: Text('Техник начал работу над заявкой #1257'))),
      Card(child: ListTile(leading: CircleAvatar(backgroundColor: Color(0xfffff0dc), child: Icon(Icons.schedule, color: Color(0xffb56b35))), title: Text('Напоминание'), subtitle: Text('Визит запланирован на 13 мая, 10:00'))),
    ]),
  );
}

class SectionTitle extends StatelessWidget { const SectionTitle(this.value, {super.key}); final String value; @override Widget build(BuildContext context) => Text(value, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: darkBrown)); }
class Metric extends StatelessWidget { const Metric(this.value, this.label, this.color, {super.key}); final String value, label; final Color color; @override Widget build(BuildContext context) => Expanded(child: Card(child: Padding(padding: const EdgeInsets.symmetric(vertical: 14), child: Column(children: [Text(value, style: TextStyle(fontSize: 25, fontWeight: FontWeight.w800, color: color)), const SizedBox(height: 3), Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 12, color: muted))])))); }
class StatusChip extends StatelessWidget { const StatusChip(this.status, {super.key}); final String status; @override Widget build(BuildContext context) { final item = {'work': ('В работе', const Color(0xffc58a48), const Color(0xfffff0dc)), 'new': ('Новая', const Color(0xff527c92), const Color(0xffe6f0f4)), 'done': ('Завершена', const Color(0xff587e5d), const Color(0xffe4f1e5)), 'cancelled': ('Отменена', const Color(0xffa04e43), const Color(0xffffe9e4))}[status] ?? ('', Colors.grey, Colors.grey.shade100); return Container(padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5), decoration: BoxDecoration(color: item.$3, borderRadius: BorderRadius.circular(8)), child: Text(item.$1, style: TextStyle(color: item.$2, fontSize: 11, fontWeight: FontWeight.w700))); } }
