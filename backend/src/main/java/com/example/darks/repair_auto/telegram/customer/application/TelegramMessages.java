package com.example.darks.repair_auto.telegram.customer.application;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessages {

    private final Map<LanguageCode, Map<String, String>> messages = new EnumMap<>(LanguageCode.class);

    public TelegramMessages() {
        messages.put(LanguageCode.EN, Map.ofEntries(
                entry("choose_language", "Choose a language."),
                entry("send_name", "Please send your full name."),
                entry("send_contact", "Please share your own Telegram contact."),
                entry("invalid_contact", "Please share the contact that belongs to this Telegram account."),
                entry("link_conflict", "This profile could not be linked. Please contact support."),
                entry("archived_customer", "This customer profile is archived. Please contact support."),
                entry("main_menu", "Main menu"),
                entry("create_request", "Create request"),
                entry("my_requests", "My requests"),
                entry("profile", "Profile"),
                entry("change_language", "Change language"),
                entry("help", "Send /cancel to reset the current draft or /menu to return to the menu."),
                entry("choose_category", "Choose a repair category."),
                entry("send_description", "Describe the problem."),
                entry("photo_prompt", "Send up to 3 problem photos, or skip."),
                entry("location_prompt", "Share a location or send the address."),
                entry("confirm_prompt", "Confirm request"),
                entry("request_created", "Request created: %s"),
                entry("photo_failed", "Request was created, but one photo could not be attached."),
                entry("cancelled", "Draft cancelled."),
                entry("invalid_action", "This action is no longer available."),
                entry("temporary_error", "Temporary service issue. Please try again."),
                entry("empty_history", "You do not have repair requests yet."),
                entry("profile_name", "Name: %s\nPhone: %s\nLanguage: %s"),
                entry("send_new_name", "Send the new full name."),
                entry("send_new_phone", "Share the new Telegram contact for the phone update."),
                entry("updated", "Updated."),
                entry("customer_error", "We could not complete this action. Please check the details and try again."),
                entry("invalid_category", "This category is no longer available. Please choose another category."),
                entry("invalid_request_data", "Please check the request description and location, then try again."),
                entry("leave_review", "Leave a review"),
                entry("eligible_reviews", "Choose a completed request to review."),
                entry("no_eligible_reviews", "There are no completed requests waiting for a review."),
                entry("select_rating", "Rate the completed repair from 1 to 5."),
                entry("optional_comment", "Send an optional comment, or skip."),
                entry("skip_comment", "Skip comment"),
                entry("review_confirmation", "Review for %s\nCategory: %s\nRating: %d/5\nComment: %s"),
                entry("change_rating", "Change rating"),
                entry("change_comment", "Change comment"),
                entry("submit_review", "Submit review"),
                entry("thank_you_review", "Thank you for your review."),
                entry("already_reviewed", "This repair request has already been reviewed."),
                entry("request_not_completed", "Only completed repair requests can be reviewed."),
                entry("review_access_denied", "This repair request cannot be reviewed from this profile."),
                entry("invalid_rating", "Choose a rating from 1 to 5."),
                entry("comment_too_long", "The comment is too long."),
                entry("no_comment", "No comment"),
                entry("your_review", "Your review: %d/5\nComment: %s"),
                entry("telegram.request.status.NEW", "New"),
                entry("telegram.request.status.ASSIGNED", "Assigned"),
                entry("telegram.request.status.SCHEDULED", "Scheduled"),
                entry("telegram.request.status.IN_PROGRESS", "In progress"),
                entry("telegram.request.status.WAITING_FOR_PARTS", "Waiting for parts"),
                entry("telegram.request.status.COMPLETED", "Completed"),
                entry("telegram.request.status.CANCELLED", "Cancelled"),
                entry("back", "Back"),
                entry("confirm", "Confirm"),
                entry("edit", "Edit"),
                entry("skip", "Skip")));
        messages.put(LanguageCode.RU, Map.ofEntries(
                entry("choose_language", "Выберите язык."),
                entry("send_name", "Отправьте ваше полное имя."),
                entry("send_contact", "Поделитесь своим Telegram контактом."),
                entry("invalid_contact", "Поделитесь контактом, который принадлежит этому Telegram аккаунту."),
                entry("link_conflict", "Профиль не удалось связать. Обратитесь в поддержку."),
                entry("archived_customer", "Профиль клиента архивирован. Обратитесь в поддержку."),
                entry("main_menu", "Главное меню"),
                entry("create_request", "Создать заявку"),
                entry("my_requests", "Мои заявки"),
                entry("profile", "Профиль"),
                entry("change_language", "Изменить язык"),
                entry("help", "Отправьте /cancel для сброса черновика или /menu для меню."),
                entry("choose_category", "Выберите категорию ремонта."),
                entry("send_description", "Опишите проблему."),
                entry("photo_prompt", "Отправьте до 3 фото проблемы или пропустите."),
                entry("location_prompt", "Отправьте геолокацию или адрес."),
                entry("confirm_prompt", "Подтвердите заявку"),
                entry("request_created", "Заявка создана: %s"),
                entry("photo_failed", "Заявка создана, но одно фото не удалось прикрепить."),
                entry("cancelled", "Черновик отменен."),
                entry("invalid_action", "Это действие больше недоступно."),
                entry("temporary_error", "Временная ошибка сервиса. Попробуйте еще раз."),
                entry("empty_history", "У вас пока нет заявок."),
                entry("profile_name", "Имя: %s\nТелефон: %s\nЯзык: %s"),
                entry("send_new_name", "Отправьте новое полное имя."),
                entry("send_new_phone", "Поделитесь новым Telegram контактом для смены телефона."),
                entry("updated", "Обновлено."),
                entry("customer_error", "Не удалось выполнить действие. Проверьте данные и попробуйте снова."),
                entry("invalid_category", "Эта категория больше недоступна. Выберите другую категорию."),
                entry("invalid_request_data", "Проверьте описание заявки и адрес, затем попробуйте снова."),
                entry("leave_review", "Оставить отзыв"),
                entry("eligible_reviews", "Выберите завершенную заявку для отзыва."),
                entry("no_eligible_reviews", "Нет завершенных заявок, ожидающих отзыв."),
                entry("select_rating", "Оцените завершенный ремонт от 1 до 5."),
                entry("optional_comment", "Отправьте комментарий или пропустите."),
                entry("skip_comment", "Пропустить комментарий"),
                entry("review_confirmation", "Отзыв для %s\nКатегория: %s\nОценка: %d/5\nКомментарий: %s"),
                entry("change_rating", "Изменить оценку"),
                entry("change_comment", "Изменить комментарий"),
                entry("submit_review", "Отправить отзыв"),
                entry("thank_you_review", "Спасибо за отзыв."),
                entry("already_reviewed", "По этой заявке отзыв уже оставлен."),
                entry("request_not_completed", "Отзыв можно оставить только по завершенной заявке."),
                entry("review_access_denied", "Эта заявка недоступна для отзыва из этого профиля."),
                entry("invalid_rating", "Выберите оценку от 1 до 5."),
                entry("comment_too_long", "Комментарий слишком длинный."),
                entry("no_comment", "Без комментария"),
                entry("your_review", "Ваш отзыв: %d/5\nКомментарий: %s"),
                entry("telegram.request.status.NEW", "Новая"),
                entry("telegram.request.status.ASSIGNED", "Назначена"),
                entry("telegram.request.status.SCHEDULED", "Запланирована"),
                entry("telegram.request.status.IN_PROGRESS", "В работе"),
                entry("telegram.request.status.WAITING_FOR_PARTS", "Ожидает запчасти"),
                entry("telegram.request.status.COMPLETED", "Завершена"),
                entry("telegram.request.status.CANCELLED", "Отменена"),
                entry("back", "Назад"),
                entry("confirm", "Подтвердить"),
                entry("edit", "Изменить"),
                entry("skip", "Пропустить")));
        messages.put(LanguageCode.UZ, Map.ofEntries(
                entry("choose_language", "Tilni tanlang."),
                entry("send_name", "To'liq ismingizni yuboring."),
                entry("send_contact", "O'zingizning Telegram kontaktingizni ulashing."),
                entry("invalid_contact", "Shu Telegram akkauntiga tegishli kontaktni ulashing."),
                entry("link_conflict", "Profilni bog'lab bo'lmadi. Yordam xizmatiga murojaat qiling."),
                entry("archived_customer", "Mijoz profili arxivlangan. Yordam xizmatiga murojaat qiling."),
                entry("main_menu", "Asosiy menyu"),
                entry("create_request", "Ariza yaratish"),
                entry("my_requests", "Mening arizalarim"),
                entry("profile", "Profil"),
                entry("change_language", "Tilni o'zgartirish"),
                entry("help", "/cancel qoralamani tozalaydi, /menu menyuga qaytaradi."),
                entry("choose_category", "Ta'mirlash kategoriyasini tanlang."),
                entry("send_description", "Muammoni tasvirlab bering."),
                entry("photo_prompt", "3 tagacha muammo fotosini yuboring yoki o'tkazib yuboring."),
                entry("location_prompt", "Geolokatsiya yoki manzil yuboring."),
                entry("confirm_prompt", "Arizani tasdiqlang"),
                entry("request_created", "Ariza yaratildi: %s"),
                entry("photo_failed", "Ariza yaratildi, lekin bitta foto biriktirilmadi."),
                entry("cancelled", "Qoralama bekor qilindi."),
                entry("invalid_action", "Bu amal endi mavjud emas."),
                entry("temporary_error", "Vaqtinchalik xizmat xatosi. Qayta urinib ko'ring."),
                entry("empty_history", "Sizda hali arizalar yo'q."),
                entry("profile_name", "Ism: %s\nTelefon: %s\nTil: %s"),
                entry("send_new_name", "Yangi to'liq ismni yuboring."),
                entry("send_new_phone", "Telefonni yangilash uchun yangi Telegram kontaktni ulashing."),
                entry("updated", "Yangilandi."),
                entry("customer_error", "Amalni bajarib bo'lmadi. Ma'lumotlarni tekshirib, qayta urinib ko'ring."),
                entry("invalid_category", "Bu kategoriya endi mavjud emas. Boshqa kategoriyani tanlang."),
                entry("invalid_request_data", "Ariza tavsifi va manzilni tekshirib, qayta urinib ko'ring."),
                entry("leave_review", "Sharh qoldirish"),
                entry("eligible_reviews", "Sharh qoldirish uchun yakunlangan arizani tanlang."),
                entry("no_eligible_reviews", "Sharh kutilayotgan yakunlangan arizalar yo'q."),
                entry("select_rating", "Yakunlangan ta'mirlashni 1 dan 5 gacha baholang."),
                entry("optional_comment", "Ixtiyoriy izoh yuboring yoki o'tkazib yuboring."),
                entry("skip_comment", "Izohsiz o'tkazish"),
                entry("review_confirmation", "%s uchun sharh\nKategoriya: %s\nBaho: %d/5\nIzoh: %s"),
                entry("change_rating", "Bahoni o'zgartirish"),
                entry("change_comment", "Izohni o'zgartirish"),
                entry("submit_review", "Sharhni yuborish"),
                entry("thank_you_review", "Sharhingiz uchun rahmat."),
                entry("already_reviewed", "Bu ariza uchun sharh allaqachon qoldirilgan."),
                entry("request_not_completed", "Sharh faqat yakunlangan ariza uchun qoldiriladi."),
                entry("review_access_denied", "Bu profil orqali ushbu arizaga sharh qoldirib bo'lmaydi."),
                entry("invalid_rating", "1 dan 5 gacha baho tanlang."),
                entry("comment_too_long", "Izoh juda uzun."),
                entry("no_comment", "Izoh yo'q"),
                entry("your_review", "Sharhingiz: %d/5\nIzoh: %s"),
                entry("telegram.request.status.NEW", "Yangi"),
                entry("telegram.request.status.ASSIGNED", "Biriktirilgan"),
                entry("telegram.request.status.SCHEDULED", "Rejalashtirilgan"),
                entry("telegram.request.status.IN_PROGRESS", "Jarayonda"),
                entry("telegram.request.status.WAITING_FOR_PARTS", "Ehtiyot qismlar kutilmoqda"),
                entry("telegram.request.status.COMPLETED", "Yakunlangan"),
                entry("telegram.request.status.CANCELLED", "Bekor qilingan"),
                entry("back", "Orqaga"),
                entry("confirm", "Tasdiqlash"),
                entry("edit", "Tahrirlash"),
                entry("skip", "O'tkazish")));
    }

    public String get(LanguageCode language, String key) {
        Map<String, String> localized = messages.getOrDefault(language, messages.get(LanguageCode.UZ));
        String message = localized.get(key);
        if (message != null) {
            return message;
        }
        return messages.get(LanguageCode.UZ).getOrDefault(key, messages.get(LanguageCode.UZ).get("customer_error"));
    }

    public String format(LanguageCode language, String key, Object... args) {
        return get(language, key).formatted(args);
    }

    public String requestStatus(RepairRequestStatus status, LanguageCode language) {
        if (status == null) {
            return get(language, "customer_error");
        }
        return get(language, "telegram.request.status." + status.name());
    }

    public String businessError(LanguageCode language, String code) {
        return get(language, switch (code) {
            case "INVALID_CATEGORY",
                    "CATEGORY_NOT_ACTIVE",
                    "REPAIR_CATEGORY_INACTIVE",
                    "REPAIR_REQUEST_CATEGORY_INACTIVE" -> "invalid_category";
            case "VALIDATION_FAILED",
                    "REPAIR_REQUEST_DESCRIPTION_INVALID",
                    "REPAIR_REQUEST_LOCATION_REQUIRED",
                    "REPAIR_REQUEST_LOCATION_INVALID" -> "invalid_request_data";
            case "TELEGRAM_CUSTOMER_ARCHIVED" -> "archived_customer";
            case "INVALID_CALLBACK" -> "invalid_action";
            case "REVIEW_ALREADY_EXISTS" -> "already_reviewed";
            case "REVIEW_REQUEST_NOT_COMPLETED" -> "request_not_completed";
            case "REVIEW_REQUEST_NOT_OWNED",
                    "REVIEW_NOT_ELIGIBLE",
                    "REVIEW_TECHNICIAN_NOT_RESOLVED",
                    "REVIEW_CUSTOMER_INACTIVE" -> "review_access_denied";
            case "REVIEW_RATING_INVALID" -> "invalid_rating";
            case "REVIEW_COMMENT_TOO_LONG" -> "comment_too_long";
            case "ATTACHMENT_FILE_TOO_LARGE",
                    "ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED",
                    "ATTACHMENT_CONTENT_MISMATCH",
                    "ATTACHMENT_EMPTY",
                    "ATTACHMENT_LIMIT_EXCEEDED",
                    "ATTACHMENT_STORAGE_FAILED" -> "photo_failed";
            default -> "customer_error";
        });
    }

    private static Map.Entry<String, String> entry(String key, String value) {
        return Map.entry(key, value);
    }
}
