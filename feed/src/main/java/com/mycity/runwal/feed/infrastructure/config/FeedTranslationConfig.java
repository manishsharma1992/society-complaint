package com.mycity.runwal.feed.infrastructure.config;

import com.mycity.runwal.feed.application.port.Translator;
import com.mycity.runwal.feed.application.service.TranslationUnavailableException;
import com.mycity.runwal.feed.domain.model.translation.Language;
import com.mycity.runwal.feed.infrastructure.client.AmazonTranslateClient;
import com.mycity.runwal.feed.infrastructure.client.GoogleTranslateClient;
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
 * Marathi goes to Google Cloud Translation (Amazon dropped words from Marathi), English and Hindi to Amazon
 * Translate (its own IAM user; the bucket keys cannot translate). Without a Google key Marathi uses Amazon too;
 * without Amazon keys the app still starts and the Translate button answers "not available".
 */
@Configuration
class FeedTranslationConfig {

    private static final Logger log = LoggerFactory.getLogger(FeedTranslationConfig.class);

    @Bean
    Translator feedTranslator(@Value("${feed.translate.region}") String region,
                              @Value("${feed.translate.access-key}") String accessKey,
                              @Value("${feed.translate.secret-key}") String secretKey,
                              @Value("${feed.translate.google-api-key}") String googleApiKey) {
        Translator amazon = amazon(region, accessKey, secretKey);
        if (googleApiKey.isBlank()) {
            log.warn("Marathi translation uses Amazon Translate: GOOGLE_TRANSLATE_API_KEY is not set");
            return amazon;
        }
        Translator google = new GoogleTranslateClient(googleApiKey);
        return (text, target) -> (target == Language.MR ? google : amazon).translate(text, target);
    }

    private static Translator amazon(String region, String accessKey, String secretKey) {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            log.warn("Amazon Translate is off: TRANSLATE_ACCESS_KEY and TRANSLATE_SECRET_KEY are not set");
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
