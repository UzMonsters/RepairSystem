package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TelegramClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(TelegramBotClient.class)
    TelegramBotClient telegramBotClient(TelegramProperties properties) {
        if (!properties.isEnabled()) {
            return new NoopTelegramBotClient();
        }
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(properties.getRequestTimeout());
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        return new HttpTelegramBotClient(properties, restClient);
    }
}
