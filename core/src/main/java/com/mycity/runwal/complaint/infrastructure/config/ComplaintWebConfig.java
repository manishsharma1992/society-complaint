package com.mycity.runwal.complaint.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** All endpoints of this context live under /complaint (so /api/complaint/... behind the app's /api context path). */
@Configuration
class ComplaintWebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/complaint", HandlerTypePredicate.forBasePackage("com.mycity.runwal.complaint"));
    }
}
