package com.example.darks.repair_auto.telegram.customer.application;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.review.application.EligibleReviewRequest;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TelegramKeyboards {

    public String language() {
        return inline(List.of(
                List.of(button("English", "lang:EN"), button("Русский", "lang:RU"), button("O'zbek", "lang:UZ"))));
    }

    public String main(TelegramMessages messages, LanguageCode language) {
        return inline(List.of(
                List.of(button(messages.get(language, "create_request"), "menu:create")),
                List.of(button(messages.get(language, "my_requests"), "menu:history")),
                List.of(button(messages.get(language, "leave_review"), "menu:review")),
                List.of(button(messages.get(language, "profile"), "menu:profile")),
                List.of(button(messages.get(language, "change_language"), "menu:language")),
                List.of(button(messages.get(language, "help"), "menu:help"))));
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

    public String history(Long requestId, int page, boolean hasNext, TelegramMessages messages, LanguageCode language) {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of(button("Open", "req:" + requestId)));
        if (page > 0 || hasNext) {
            List<String> paging = new java.util.ArrayList<>();
            if (page > 0) {
                paging.add(button("Previous", "hist:" + (page - 1)));
            }
            if (hasNext) {
                paging.add(button("Next", "hist:" + (page + 1)));
            }
            rows.add(paging);
        }
        rows.add(List.of(button(messages.get(language, "back"), "menu:back")));
        return inline(rows);
    }

    public String requestDetails(Long requestId, boolean canReview, TelegramMessages messages, LanguageCode language) {
        List<List<String>> rows = new java.util.ArrayList<>();
        if (canReview) {
            rows.add(List.of(button(messages.get(language, "leave_review"), "revreq:" + requestId)));
        }
        rows.add(List.of(button(messages.get(language, "back"), "menu:back")));
        return inline(rows);
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
                List.of(button("Name", "profile:name")),
                List.of(button("Phone", "profile:phone")),
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
        return request.requestNumber() + " - " + category;
    }

    private String inline(List<List<String>> rows) {
        return "{\"inline_keyboard\":[" + rows.stream()
                .map(row -> "[" + String.join(",", row) + "]")
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "]}";
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
