package com.example.darks.repair_auto.telegram.customer.application;

import com.example.darks.repair_auto.repair.attachment.application.AttachmentService;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramCustomerPhotoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramCustomerPhotoService.class);

    private final TelegramBotClient botClient;
    private final AttachmentService attachmentService;

    public TelegramCustomerPhotoService(TelegramBotClient botClient, AttachmentService attachmentService) {
        this.botClient = botClient;
        this.attachmentService = attachmentService;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean attachProblemPhotos(Long requestId, Long customerId, List<String> fileIds) {
        boolean failed = false;
        for (String fileId : fileIds) {
            try {
                TelegramFileMetadata metadata = botClient.getFile(fileId);
                if (metadata.fileSize() <= 0) {
                    throw new TelegramApiException("Telegram file size is unavailable.");
                }
                try (InputStream inputStream = botClient.downloadFile(metadata.filePath(), metadata.fileSize())) {
                    attachmentService.uploadFromCustomer(
                            requestId,
                            AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                            "telegram-photo.jpg",
                            null,
                            metadata.fileSize(),
                            inputStream,
                            customerId);
                }
            } catch (BusinessRuleException exception) {
                LOGGER.warn(
                        "Telegram photo attachment failed requestId={} errorType={} code={}",
                        requestId,
                        exception.getClass().getSimpleName(),
                        exception.code());
                failed = true;
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn(
                        "Telegram photo attachment failed requestId={} errorType={}",
                        requestId,
                        exception.getClass().getSimpleName());
                failed = true;
            }
        }
        return failed;
    }
}
