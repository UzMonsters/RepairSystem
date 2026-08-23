package com.example.darks.repair_auto.telegram.customer.application;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCategorySummary;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestSummaryResponse;
import com.example.darks.repair_auto.review.application.EligibleReviewRequest;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TelegramKeyboards {

    public String language() {
        return inline(List.of(
                List.of(button("English", "lang:EN"), button("Русский", "lang:RU"), button("O'zbek", "lang:UZ"))));
    }

    public String main(TelegramMessages messages, LanguageCode language) {
        return reply(List.of(
                List.of(messages.get(language, "create_request"), messages.get(language, "my_requests")),
                List.of(messages.get(language, "leave_review"), messages.get(language, "profile")),
                List.of(messages.get(language, "change_language"), messages.get(language, "help_button"))));
    }

    public String categories(List<RepairCategory> categories, LanguageCode language) {
        List<List<String>> rows = categories.stream()
                .map(category -> List.of(button(label(category, language), "cat:" + category.getId())))
                .toList();
        return inline(rows);
    }

    public String photos(TelegramMessages messages, LanguageCode language) {
        return inline(List.of(List.of(button(messages.get(language, "skip"), "photo:skip"))));
    }

    public String location(TelegramMessages messages, LanguageCode language) {
        String sendCurrentText = messages.get(language, "request.location.send_current");
        String enterAddressText = messages.get(language, "request.location.enter_address");
        String skipText = messages.get(language, "request.location.skip");
        return "{\"keyboard\":["
                + "[{\"text\":\"" + json(sendCurrentText) + "\",\"request_location\":true}],"
                + "[{\"text\":\"" + json(enterAddressText) + "\"},{\"text\":\"" + json(skipText) + "\"}]"
                + "],\"resize_keyboard\":true,\"one_time_keyboard\":true}";
    }

    public String contact(TelegramMessages messages, LanguageCode language) {
        return "{\"keyboard\":[[{\"text\":\""
                + json(messages.get(language, "share_contact_button"))
                + "\",\"request_contact\":true}]],\"resize_keyboard\":true,\"one_time_keyboard\":true}";
    }

    public String removeReplyKeyboard() {
        return "{\"remove_keyboard\":true}";
    }

    public String confirm(TelegramMessages messages, LanguageCode language) {
        return inline(List.of(
                List.of(button(messages.get(language, "confirm"), "confirm:create")),
                List.of(button(messages.get(language, "edit"), "confirm:edit")),
                List.of(button(messages.get(language, "back"), "menu:back"))));
    }

    public String history(
            List<RepairRequestSummaryResponse> requests,
            int page,
            boolean hasNext,
            TelegramMessages messages,
            LanguageCode language,
            DateTimeFormatter dateFormatter) {
        List<List<String>> rows = new java.util.ArrayList<>();
        for (RepairRequestSummaryResponse request : requests) {
            String label = requestButtonLabel(request, messages, language, dateFormatter);
            rows.add(List.of(button(label, "req:" + request.id() + ":" + page)));
        }
        if (page > 0 || hasNext) {
            List<String> paging = new java.util.ArrayList<>();
            if (page > 0) {
                paging.add(button(messages.get(language, "previous"), "hist:" + (page - 1)));
            }
            if (hasNext) {
                paging.add(button(messages.get(language, "next"), "hist:" + (page + 1)));
            }
            rows.add(paging);
        }
        rows.add(List.of(button(messages.get(language, "main_menu_button"), "menu:back")));
        return inline(rows);
    }

    public String emptyHistory(TelegramMessages messages, LanguageCode language) {
        return inline(List.of(List.of(button(messages.get(language, "main_menu_button"), "menu:back"))));
    }

    public String requestDetails(
            Long requestId,
            int page,
            boolean canReview,
            TelegramMessages messages,
            LanguageCode language) {
        return requestDetails(requestId, page, canReview, null, null, messages, language);
    }

    public String requestDetails(
            Long requestId,
            int page,
            boolean canReview,
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            TelegramMessages messages,
            LanguageCode language) {
        List<List<String>> rows = new java.util.ArrayList<>();
        if (latitude != null && longitude != null) {
            String mapUrl = mapUrl(latitude, longitude);
            rows.add(List.of(urlButton(messages.get(language, "open_on_map"), mapUrl)));
        }
        if (canReview) {
            rows.add(List.of(button(messages.get(language, "leave_review_detail"), "revreq:" + requestId)));
        }
        rows.add(List.of(button(messages.get(language, "back_to_requests"), "hist:" + page)));
        rows.add(List.of(button(messages.get(language, "main_menu_button"), "menu:back")));
        return inline(rows);
    }

    public static String mapUrl(java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        return "https://maps.google.com/?q=" + latitude.toPlainString() + "," + longitude.toPlainString();
    }

    public String urlButton(String text, String url) {
        return "{\"text\":\"" + json(text) + "\",\"url\":\"" + json(url) + "\"}";
    }

    public String requestButtonLabel(
            RepairRequestSummaryResponse request,
            TelegramMessages messages,
            LanguageCode language,
            DateTimeFormatter dateFormatter) {
        String icon = messages.statusIcon(request.status());
        String category = categorySummaryLabel(request.category(), language);
        String date = "";
        if (request.createdAt() != null && dateFormatter != null) {
            date = dateFormatter.format(Instant.from(request.createdAt()));
        }
        String label = icon + " " + truncate(category, 28);
        if (!date.isBlank()) {
            label += " · " + date;
        }
        return label;
    }

    public String categorySummaryLabel(RepairRequestCategorySummary category, LanguageCode language) {
        if (category == null) {
            return "";
        }
        String name = switch (language) {
            case EN -> category.nameEn();
            case RU -> category.nameRu();
            case UZ -> category.nameUz();
        };
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (category.name() != null && !category.name().isBlank()) {
            return category.name();
        }
        if (category.nameUz() != null && !category.nameUz().isBlank()) {
            return category.nameUz();
        }
        if (category.nameRu() != null && !category.nameRu().isBlank()) {
            return category.nameRu();
        }
        if (category.nameEn() != null && !category.nameEn().isBlank()) {
            return category.nameEn();
        }
        return "";
    }

    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        int codePointCount = text.codePointCount(0, text.length());
        if (codePointCount <= maxLength) {
            return text;
        }
        int target = maxLength - 1;
        int offset = text.offsetByCodePoints(0, target);
        return text.substring(0, offset) + "…";
    }

    public String eligibleReviewRequests(List<EligibleReviewRequest> requests, LanguageCode language) {
        List<List<String>> rows = requests.stream()
                .map(request -> List.of(button(reviewRequestLabel(request, language), "revreq:" + request.requestId())))
                .toList();
        return inline(rows);
    }

    public String reviewRating(TelegramMessages messages, LanguageCode language) {
        return inline(List.of(
                List.of(button("1", "revrate:1"), button("2", "revrate:2"), button("3", "revrate:3")),
                List.of(button("4", "revrate:4"), button("5", "revrate:5")),
                List.of(button(messages.get(language, "back"), "menu:review"))));
    }

    public String reviewComment(TelegramMessages messages, LanguageCode language) {
        return inline(List.of(List.of(button(messages.get(language, "skip_comment"), "revcomment:skip"))));
    }

    public String reviewConfirm(TelegramMessages messages, LanguageCode language) {
        return inline(List.of(
                List.of(button(messages.get(language, "submit_review"), "review:submit")),
                List.of(
                        button(messages.get(language, "change_rating"), "review:rating"),
                        button(messages.get(language, "change_comment"), "review:comment")),
                List.of(button(messages.get(language, "back"), "review:cancel"))));
    }

    public String profile(TelegramMessages messages, LanguageCode language) {
        return inline(List.of(
                List.of(button(messages.get(language, "field.name"), "profile:name")),
                List.of(button(messages.get(language, "field.phone"), "profile:phone")),
                List.of(button(messages.get(language, "change_language"), "menu:language")),
                List.of(button(messages.get(language, "back"), "menu:back"))));
    }

    public String label(RepairCategory category, LanguageCode language) {
        return switch (language) {
            case EN -> category.getNameEn();
            case RU -> category.getNameRu();
            case UZ -> category.getNameUz();
        };
    }

    private String reviewRequestLabel(EligibleReviewRequest request, LanguageCode language) {
        String category = switch (language) {
            case EN -> request.categoryNameEn();
            case RU -> request.categoryNameRu();
            case UZ -> request.categoryNameUz();
        };
        return category;
    }

    private String inline(List<List<String>> rows) {
        return "{\"inline_keyboard\":[" + rows.stream()
                .map(row -> "[" + String.join(",", row) + "]")
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "]}";
    }

    private String reply(List<List<String>> rows) {
        return "{\"keyboard\":[" + rows.stream()
                .map(row -> "[" + row.stream()
                        .map(text -> "{\"text\":\"" + json(text) + "\"}")
                        .reduce((left, right) -> left + "," + right)
                        .orElse("") + "]")
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "],\"resize_keyboard\":true,\"is_persistent\":true}";
    }

    private String button(String text, String callbackData) {
        return "{\"text\":\"" + json(text) + "\",\"callback_data\":\"" + json(callbackData) + "\"}";
    }

    private String json(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
