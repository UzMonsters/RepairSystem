package com.example.darks.repair_auto.repair.attachment.application;

import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageProperties;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentValidator {

    private static final int MAX_ORIGINAL_FILE_NAME_LENGTH = 255;
    private static final int SIGNATURE_BYTES = 16;
    private static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf");

    private final StorageProperties properties;

    public AttachmentValidator(StorageProperties properties) {
        this.properties = properties;
    }

    public String validateOriginalFileName(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessRuleException("ATTACHMENT_EMPTY", "Attachment file is required and must be non-empty.", 400);
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new BusinessRuleException("ATTACHMENT_FILE_TOO_LARGE", "Attachment file exceeds the configured limit.", 400);
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            original = "attachment";
        }
        String trimmed = original.trim();
        if (trimmed.length() > MAX_ORIGINAL_FILE_NAME_LENGTH || hasControlCharacter(trimmed)) {
            throw new BusinessRuleException("VALIDATION_FAILED", "Original filename is invalid.", 400);
        }
        return trimmed;
    }

    public String validateOriginalFileName(String originalFileName, long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new BusinessRuleException("ATTACHMENT_EMPTY", "Attachment file is required and must be non-empty.", 400);
        }
        if (sizeBytes > properties.maxFileSize().toBytes()) {
            throw new BusinessRuleException("ATTACHMENT_FILE_TOO_LARGE", "Attachment file exceeds the configured limit.", 400);
        }
        String original = originalFileName;
        if (original == null || original.isBlank()) {
            original = "attachment";
        }
        String trimmed = original.trim();
        if (trimmed.length() > MAX_ORIGINAL_FILE_NAME_LENGTH || hasControlCharacter(trimmed)) {
            throw new BusinessRuleException("VALIDATION_FAILED", "Original filename is invalid.", 400);
        }
        return trimmed;
    }

    public DetectedFile detectAndValidate(AttachmentType type, InputStream inputStream) throws IOException {
        byte[] header = inputStream.readNBytes(SIGNATURE_BYTES);
        DetectedFile detected = detect(header);
        if (detected == null) {
            throw new BusinessRuleException(
                    "ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED",
                    "Attachment content type is not supported.",
                    400);
        }
        Set<String> allowed = type == AttachmentType.GENERAL_DOCUMENT ? DOCUMENT_TYPES : PHOTO_TYPES;
        if (!allowed.contains(detected.contentType())) {
            throw new BusinessRuleException(
                    "ATTACHMENT_TYPE_NOT_ALLOWED",
                    "Attachment type does not allow this content type.",
                    400);
        }
        return detected;
    }

    public String extensionFor(String contentType) {
        return EXTENSIONS.get(contentType);
    }

    private DetectedFile detect(byte[] header) {
        if (header.length >= 3
                && unsigned(header[0]) == 0xff
                && unsigned(header[1]) == 0xd8
                && unsigned(header[2]) == 0xff) {
            return new DetectedFile("image/jpeg", EXTENSIONS.get("image/jpeg"));
        }
        if (header.length >= 8
                && unsigned(header[0]) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4e
                && header[3] == 0x47
                && header[4] == 0x0d
                && header[5] == 0x0a
                && header[6] == 0x1a
                && header[7] == 0x0a) {
            return new DetectedFile("image/png", EXTENSIONS.get("image/png"));
        }
        if (header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50) {
            return new DetectedFile("image/webp", EXTENSIONS.get("image/webp"));
        }
        if (header.length >= 5
                && header[0] == 0x25
                && header[1] == 0x50
                && header[2] == 0x44
                && header[3] == 0x46
                && header[4] == 0x2d) {
            return new DetectedFile("application/pdf", EXTENSIONS.get("application/pdf"));
        }
        return null;
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    private boolean hasControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
