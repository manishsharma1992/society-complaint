package com.mycity.runwal.residency.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** All endpoints of this context live under /residency (so /api/residency/... behind the app's /api context path). */
@Configuration
class ResidencyWebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/residency", HandlerTypePredicate.forBasePackage("com.mycity.runwal.residency"));
    }
}
