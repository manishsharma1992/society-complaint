package com.mycity.runwal.workforce.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** All endpoints of this context live under /workforce (so /api/workforce/... behind the app's /api context path). */
@Configuration
class WorkforceWebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/workforce", HandlerTypePredicate.forBasePackage("com.mycity.runwal.workforce"));
    }
}
