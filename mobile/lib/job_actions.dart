part of 'main.dart';

class JobActions extends StatelessWidget {
  const JobActions({super.key, required this.repo, required this.chat, required this.job});
  final TechnicianRepository repo;
  final MobileChatRepository chat;
  final Job job;
  Future<void> uploadEvidence(
    BuildContext context,
    String attachmentType,
  ) async {
    final picked = await ImagePicker().pickImage(source: ImageSource.gallery);
    if (picked == null) return;
    await repo.uploadPhoto(job.id, File(picked.path), attachmentType);
    if (context.mounted) Navigator.pop(context);
  }

  Future<String?> askText(BuildContext context, String title, String label) async {
    final controller = TextEditingController();
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          maxLines: 4,
          decoration: InputDecoration(labelText: label),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, controller.text.trim()),
            child: const Text('Save'),
          ),
        ],
      ),
    );
    controller.dispose();
    return value == null || value.isEmpty ? null : value;
  }

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Wrap(
      children: [
        ListTile(
          title: const Text('Chat with customer'),
          leading: const Icon(Icons.chat_outlined),
          onTap: () async {
            Navigator.pop(context);
            await Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => MobileChatScreen(repo: chat, requestId: job.id, language: chat.api.language),
              ),
            );
          },
        ),
        ...job.availableActions.map((action) {
        if (action == 'UPLOAD_DIAGNOSIS_PHOTO') {
          return ListTile(
            title: Text(action),
            leading: const Icon(Icons.photo_camera),
            onTap: () => uploadEvidence(context, 'DIAGNOSIS_PHOTO'),
          );
        }
        if (action == 'UPLOAD_COMPLETION_PHOTO') {
          return ListTile(
            title: Text(action),
            leading: const Icon(Icons.photo_camera),
            onTap: () => uploadEvidence(context, 'COMPLETION_PHOTO'),
          );
        }
        return ListTile(
          title: Text(action),
          leading: const Icon(Icons.play_arrow),
          onTap: () async {
            if (action == 'ACCEPT_ASSIGNMENT') {
              await repo.accept(job.id);
            } else if (action == 'START_REPAIR') {
              await repo.start(job.id);
            } else if (action == 'RESUME_REPAIR') {
              await repo.resume(job.id);
            } else if (action == 'REJECT_ASSIGNMENT') {
              final reason = await askText(context, 'Reject assignment', 'Reason');
              if (reason != null) await repo.reject(job.id, reason);
            } else if (action == 'UPDATE_DIAGNOSIS') {
              final diagnosis = await askText(context, 'Diagnosis', 'Diagnosis');
              if (diagnosis != null) await repo.diagnosis(job.id, diagnosis);
            } else if (action == 'WAIT_FOR_PARTS') {
              final reason = await askText(context, 'Wait for parts', 'Reason');
              if (reason != null) await repo.waitForParts(job.id, reason);
            } else if (action == 'COMPLETE_REPAIR') {
              final work = await askText(context, 'Complete repair', 'Work performed');
              if (work != null) await repo.complete(job.id, work);
            }
            if (context.mounted) Navigator.pop(context);
          },
        );
      }).toList(),
      ],
    ),
  );
}

