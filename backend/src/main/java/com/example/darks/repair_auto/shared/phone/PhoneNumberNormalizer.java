package com.example.darks.repair_auto.shared.phone;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberNormalizer {

    private static final Pattern NORMALIZED_UZ_PHONE = Pattern.compile("^\\+998\\d{9}$");

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        String trimmed = value.trim();
        String digits = trimmed.replaceAll("[\\s()\\-]", "");
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        String normalized;
        if (digits.startsWith("998") && digits.length() == 12) {
            normalized = "+" + digits;
        } else if (digits.length() == 9) {
            normalized = "+998" + digits;
        } else {
            throw invalid();
        }
        if (!NORMALIZED_UZ_PHONE.matcher(normalized).matches()) {
            throw invalid();
        }
        return normalized;
    }

    private BusinessRuleException invalid() {
        return new BusinessRuleException(
                "INVALID_PHONE_NUMBER",
                "Phone number must be a valid Uzbekistan number in +998XXXXXXXXX form.",
                400);
    }
}
