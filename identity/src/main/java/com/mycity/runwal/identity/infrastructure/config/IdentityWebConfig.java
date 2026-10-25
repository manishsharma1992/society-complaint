package com.mycity.runwal.identity.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** All endpoints of this context live under /identity (so /api/identity/... behind the app's /api context path). */
@Configuration
class IdentityWebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/identity", HandlerTypePredicate.forBasePackage("com.mycity.runwal.identity"));
    }
}
