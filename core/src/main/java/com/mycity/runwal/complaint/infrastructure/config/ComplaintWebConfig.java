package com.mycity.runwal.complaint.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import com.mycity.runwal.complaint.application.port.ComplaintsAddOn;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** All endpoints of this context live under /complaint (so /api/complaint/... behind the app's /api context path). */
@Configuration
class ComplaintWebConfig implements WebMvcConfigurer {

    private final ComplaintsAddOn addOn;

    ComplaintWebConfig(ComplaintsAddOn addOn) {
        this.addOn = addOn;
    }

    /** Requests about a society go through only while it has this context's add-on switched on. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ComplaintAddOnInterceptor(addOn)).addPathPatterns("/complaint/societies/*/**");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/complaint", HandlerTypePredicate.forBasePackage("com.mycity.runwal.complaint"));
    }
}
