package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TelegramClientConfiguration {

    @Bean("customerTelegramBotClient")
    @ConditionalOnMissingBean(name = "customerTelegramBotClient")
    TelegramBotClient customerTelegramBotClient(TelegramProperties properties) {
        return telegramBotClient(properties, properties.getCustomer());
    }

    @Bean("technicianTelegramBotClient")
    @ConditionalOnMissingBean(name = "technicianTelegramBotClient")
    TelegramBotClient technicianTelegramBotClient(TelegramProperties properties) {
        return telegramBotClient(properties, properties.getTechnician());
    }

    @Bean
    @ConditionalOnMissingBean(name = "telegramBotClient")
    TelegramBotClient telegramBotClient(
            @Qualifier("customerTelegramBotClient") TelegramBotClient customerTelegramBotClient) {
        return customerTelegramBotClient;
    }

    private TelegramBotClient telegramBotClient(TelegramProperties properties, TelegramProperties.Bot bot) {
        if (!properties.isEnabled() || bot.getBotToken().isBlank()) {
            return new NoopTelegramBotClient();
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(properties.getRequestTimeout());
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        return new HttpTelegramBotClient(properties, bot, restClient);
    }
}
