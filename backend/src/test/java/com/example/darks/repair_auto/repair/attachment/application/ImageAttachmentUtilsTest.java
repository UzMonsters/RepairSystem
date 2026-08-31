package com.example.darks.repair_auto.repair.attachment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ImageAttachmentUtilsTest {

    @Test
    void isImagePreviewable_returnsExpectedForSupportedAndUnsupportedTypes() {
        assertThat(ImageAttachmentUtils.isImagePreviewable("image/jpeg")).isTrue();
        assertThat(ImageAttachmentUtils.isImagePreviewable("image/png")).isTrue();
        assertThat(ImageAttachmentUtils.isImagePreviewable("image/webp")).isTrue();
        assertThat(ImageAttachmentUtils.isImagePreviewable("image/gif")).isTrue();
        assertThat(ImageAttachmentUtils.isImagePreviewable("IMAGE/JPEG")).isTrue();

        assertThat(ImageAttachmentUtils.isImagePreviewable("application/pdf")).isFalse();
        assertThat(ImageAttachmentUtils.isImagePreviewable("text/plain")).isFalse();
        assertThat(ImageAttachmentUtils.isImagePreviewable("")).isFalse();
        assertThat(ImageAttachmentUtils.isImagePreviewable(null)).isFalse();
    }

    @Test
    void toAvatarResponse_convertsAvailableAttachment() {
        RepairAttachment attachment = mock(RepairAttachment.class);
        OffsetDateTime now = OffsetDateTime.now();
        when(attachment.isAvailable()).thenReturn(true);
        when(attachment.getId()).thenReturn(123L);
        when(attachment.getOriginalFileName()).thenReturn("avatar.png");
        when(attachment.getContentType()).thenReturn("image/png");
        when(attachment.getSizeBytes()).thenReturn(4096L);
        when(attachment.getUploadedAt()).thenReturn(now);

        AvatarResponse response = ImageAttachmentUtils.toAvatarResponse(attachment, "/api/v1/customers/42/avatar");

        assertThat(response).isNotNull();
        assertThat(response.attachmentId()).isEqualTo(123L);
        assertThat(response.fileName()).isEqualTo("avatar.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.sizeBytes()).isEqualTo(4096L);
        assertThat(response.downloadUrl()).isEqualTo("/api/v1/customers/42/avatar");
        assertThat(response.uploadedAt()).isEqualTo(now);
    }

    @Test
    void toAvatarResponse_returnsNullForNullOrNonAvailable() {
        assertThat(ImageAttachmentUtils.toAvatarResponse(null, "/url")).isNull();

        RepairAttachment attachment = mock(RepairAttachment.class);
        when(attachment.isAvailable()).thenReturn(false);
        assertThat(ImageAttachmentUtils.toAvatarResponse(attachment, "/url")).isNull();
    }

    @Test
    void urlHelpers_returnExpectedPaths() {
        assertThat(ImageAttachmentUtils.staffAvatarDownloadUrl(1L)).isEqualTo("/api/v1/users/1/avatar");
        assertThat(ImageAttachmentUtils.currentStaffAvatarDownloadUrl()).isEqualTo("/api/v1/me/avatar");
        assertThat(ImageAttachmentUtils.customerAvatarDownloadUrl(2L)).isEqualTo("/api/v1/customers/2/avatar");
        assertThat(ImageAttachmentUtils.technicianAvatarDownloadUrl(3L)).isEqualTo("/api/v1/technicians/3/avatar");
        assertThat(ImageAttachmentUtils.requestAttachmentDownloadUrl(4L)).isEqualTo("/api/v1/attachments/4/download");
        assertThat(ImageAttachmentUtils.mobileSelfAvatarDownloadUrl()).isEqualTo("/api/v1/mobile/me/avatar");
        assertThat(ImageAttachmentUtils.mobileAttachmentDownloadUrl(5L)).isEqualTo("/api/v1/mobile/me/attachments/5/download");
    }
}