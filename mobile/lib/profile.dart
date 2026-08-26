part of 'main.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.actor,
    required this.repo,
    required this.onLogout,
  });
  final Actor actor;
  final MobileProfileRepository repo;
  final VoidCallback onLogout;
  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  late final name = TextEditingController(text: widget.actor.fullName);
  late String language = widget.actor.preferredLanguage ?? 'uz';
  bool saving = false;

  @override
  void dispose() {
    name.dispose();
    super.dispose();
  }

  Future<void> save() async {
    setState(() => saving = true);
    try {
      widget.repo.setLanguage(language);
      await widget.repo.update(
        fullName: name.text.trim(),
        preferredLanguage: language,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(mobileText(language, 'profileSaved'))),
        );
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('$error')));
      }
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = widget.repo.api.language;
    final initials = name.text
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .map((part) => part[0])
        .take(2)
        .join()
        .toUpperCase();
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 30,
                  child: Text(initials.isEmpty ? '?' : initials),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    widget.actor.type,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: name,
          decoration: InputDecoration(
            labelText: mobileText(strings, 'fullName'),
            border: OutlineInputBorder(),
          ),
          onChanged: (_) => setState(() {}),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          initialValue: language,
          decoration: const InputDecoration(
            labelText: 'RU / UZ / EN',
            border: OutlineInputBorder(),
          ),
          items: const [
            DropdownMenuItem(value: 'uz', child: Text('O‘zbekcha')),
            DropdownMenuItem(value: 'ru', child: Text('Русский')),
            DropdownMenuItem(value: 'en', child: Text('English')),
          ],
          onChanged: (value) => setState(() => language = value ?? 'uz'),
        ),
        const SizedBox(height: 12),
        FilledButton(
          onPressed: saving ? null : save,
          child: saving
              ? const CircularProgressIndicator()
              : Text(mobileText(strings, 'save')),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: widget.onLogout,
          icon: const Icon(Icons.logout),
          label: Text(mobileText(strings, 'logout')),
        ),
      ],
    );
  }
}
