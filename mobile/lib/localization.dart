part of 'main.dart';

String mobileText(String language, String key) {
  const strings = <String, Map<String, String>>{
    'ru': {
      'requests': 'Заявки', 'createRequest': 'Создать заявку',
      'description': 'Описание проблемы', 'category': 'Категория',
      'chooseCategory': 'Выберите категорию', 'location': 'Геопозиция',
      'getLocation': 'Определить мою геопозицию', 'locationReady': 'Геопозиция добавлена',
      'create': 'Создать', 'noRequests': 'Заявок нет', 'filters': 'Фильтры',
      'all': 'Все', 'new': 'Новая', 'assigned': 'Назначена',
      'inProgress': 'В работе', 'completed': 'Завершена', 'cancelled': 'Отменена',
      'scheduled': 'Запланирована', 'waitingParts': 'Ожидание запчастей',
      'schedule': 'Расписание визитов', 'chat': 'Чат с техником',
      'message': 'Сообщение', 'sendFailed': 'Не удалось отправить сообщение',
      'notifications': 'Уведомления', 'profile': 'Профиль', 'save': 'Сохранить',
      'logout': 'Выйти', 'fullName': 'Имя и фамилия', 'markAllRead': 'Прочитать все',
    },
    'uz': {
      'requests': 'Arizalar', 'createRequest': 'Ariza yaratish',
      'description': 'Muammo tavsifi', 'category': 'Kategoriya',
      'chooseCategory': 'Kategoriyani tanlang', 'location': 'Geolokatsiya',
      'getLocation': 'Geolokatsiyamni aniqlash', 'locationReady': 'Geolokatsiya qo‘shildi',
      'create': 'Yaratish', 'noRequests': 'Arizalar yo‘q', 'filters': 'Filtrlar',
      'all': 'Barchasi', 'new': 'Yangi', 'assigned': 'Tayinlangan',
      'inProgress': 'Jarayonda', 'completed': 'Yakunlangan', 'cancelled': 'Bekor qilingan',
      'scheduled': 'Rejalashtirilgan', 'waitingParts': 'Ehtiyot qismlar kutilmoqda',
      'schedule': 'Tashrif jadvali', 'chat': 'Texnik bilan chat',
      'message': 'Xabar', 'sendFailed': 'Xabar yuborilmadi',
      'notifications': 'Bildirishnomalar', 'profile': 'Profil', 'save': 'Saqlash',
      'logout': 'Chiqish', 'fullName': 'Ism va familiya', 'markAllRead': 'Barchasini o‘qilgan qilish',
    },
    'en': {
      'requests': 'Requests', 'createRequest': 'Create repair request',
      'description': 'Problem description', 'category': 'Category',
      'chooseCategory': 'Choose a category', 'location': 'Location',
      'getLocation': 'Use my location', 'locationReady': 'Location added',
      'create': 'Create', 'noRequests': 'No requests', 'filters': 'Filters',
      'all': 'All', 'new': 'New', 'assigned': 'Assigned',
      'inProgress': 'In progress', 'completed': 'Completed', 'cancelled': 'Cancelled',
      'scheduled': 'Scheduled', 'waitingParts': 'Waiting for parts',
      'schedule': 'Visit schedule', 'chat': 'Chat with technician',
      'message': 'Message', 'sendFailed': 'Message could not be sent',
      'notifications': 'Notifications', 'profile': 'Profile', 'save': 'Save',
      'logout': 'Logout', 'fullName': 'Full name', 'markAllRead': 'Mark all read',
    },
  };
  return strings[language]?[key] ?? strings['en']![key] ?? key;
}
