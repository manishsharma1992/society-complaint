package com.mycity.runwal.feed.infrastructure.config;

import com.mycity.runwal.feed.application.port.Translator;
import com.mycity.runwal.feed.application.service.TranslationUnavailableException;
import com.mycity.runwal.feed.infrastructure.client.AmazonTranslateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.translate.TranslateClient;

/**
 * Amazon Translate, with its own keys (an IAM user that may only translate; the bucket keys cannot). Without keys
 * the app still starts and the Translate button answers "not available".
 */
@Configuration
class FeedTranslationConfig {

    private static final Logger log = LoggerFactory.getLogger(FeedTranslationConfig.class);

    @Bean
    Translator feedTranslator(@Value("${feed.translate.region}") String region,
                              @Value("${feed.translate.access-key}") String accessKey,
                              @Value("${feed.translate.secret-key}") String secretKey) {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            log.warn("Post translation is off: TRANSLATE_ACCESS_KEY and TRANSLATE_SECRET_KEY are not set");
            return (text, target) -> {
                throw new TranslationUnavailableException("Translation is not set up on this server", null);
            };
        }
        return new AmazonTranslateClient(TranslateClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .httpClient(UrlConnectionHttpClient.create())
                .build());
    }
}
