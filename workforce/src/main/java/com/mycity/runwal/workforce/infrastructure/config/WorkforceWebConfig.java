package com.mycity.runwal.workforce.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import com.mycity.runwal.workforce.application.port.ComplaintsAddOn;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** All endpoints of this context live under /workforce (so /api/workforce/... behind the app's /api context path). */
@Configuration
class WorkforceWebConfig implements WebMvcConfigurer {

    private final ComplaintsAddOn addOn;

    WorkforceWebConfig(ComplaintsAddOn addOn) {
        this.addOn = addOn;
    }

    /** Requests about a society go through only while it has this context's add-on switched on. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WorkforceAddOnInterceptor(addOn)).addPathPatterns("/workforce/societies/*/**");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/workforce", HandlerTypePredicate.forBasePackage("com.mycity.runwal.workforce"));
    }
}
