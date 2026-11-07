package com.mycity.runwal.feed.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** All endpoints of this context live under /feed (so /api/feed/... behind the app's /api context path). */
@Configuration
class FeedWebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/feed", HandlerTypePredicate.forBasePackage("com.mycity.runwal.feed"));
    }
}
