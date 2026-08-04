package com.example.darks.repair_auto.shared.pagination;

import com.example.darks.repair_auto.shared.error.InvalidRequestParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableRequestFactory {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageableRequestFactory() {
    }

    public static Pageable toPageable(
            Integer page,
            Integer size,
            List<String> sort,
            String defaultSort,
            Map<String, String> sortProperties) {
        int validatedPage = validatePage(page);
        int validatedSize = validateSize(size);
        List<String> requestedSort = sort == null || sort.isEmpty() ? List.of(defaultSort) : sort;
        return PageRequest.of(validatedPage, validatedSize, Sort.by(toOrders(requestedSort, sortProperties)));
    }

    private static int validatePage(Integer page) {
        int value = page == null ? DEFAULT_PAGE : page;
        if (value < 0) {
            throw invalid("page", "Page must be greater than or equal to 0.");
        }
        return value;
    }

    private static int validateSize(Integer size) {
        int value = size == null ? DEFAULT_SIZE : size;
        if (value < 1) {
            throw invalid("size", "Size must be greater than or equal to 1.");
        }
        if (value > MAX_SIZE) {
            throw invalid("size", "Size must be less than or equal to " + MAX_SIZE + ".");
        }
        return value;
    }

    private static List<Sort.Order> toOrders(List<String> sort, Map<String, String> sortProperties) {
        List<Sort.Order> orders = new ArrayList<>();
        List<String> tokens = sort.stream()
                .flatMap(expression -> java.util.Arrays.stream(splitExpression(expression)))
                .map(String::trim)
                .toList();
        for (int index = 0; index < tokens.size(); index++) {
            String requestedField = tokens.get(index);
            if (requestedField.isBlank()) {
                throw invalid("sort", "Sort expression must use '<field>,<asc|desc>'.");
            }
            String property = sortProperties.get(requestedField);
            if (property == null) {
                throw invalid("sort", "Unsupported sort field '" + requestedField + "'.");
            }
            Sort.Direction direction = Sort.Direction.DESC;
            if (index + 1 < tokens.size() && isDirectionToken(tokens.get(index + 1), sortProperties)) {
                String requestedDirection = tokens.get(++index).toLowerCase(Locale.ROOT);
                if (!"asc".equals(requestedDirection) && !"desc".equals(requestedDirection)) {
                    throw invalid("sort", "Sort direction must be 'asc' or 'desc'.");
                }
                direction = "asc".equals(requestedDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            }
            orders.add(new Sort.Order(direction, property));
        }
        return orders;
    }

    private static String[] splitExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            throw invalid("sort", "Sort expression must not be blank.");
        }
        String[] parts = expression.split(",", -1);
        if (parts.length > 2) {
            throw invalid("sort", "Sort expression must use '<field>,<asc|desc>'.");
        }
        return parts;
    }

    private static boolean isDirectionToken(String token, Map<String, String> sortProperties) {
        String value = token.toLowerCase(Locale.ROOT);
        if ("asc".equals(value) || "desc".equals(value)) {
            return true;
        }
        return !sortProperties.containsKey(token);
    }

    private static InvalidRequestParameterException invalid(String field, String message) {
        return new InvalidRequestParameterException(field, message);
    }
}
