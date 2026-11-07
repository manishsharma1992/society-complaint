package com.mycity.runwal.feed.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import com.mycity.runwal.feed.application.port.FeedAddOn;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** All endpoints of this context live under /feed (so /api/feed/... behind the app's /api context path). */
@Configuration
class FeedWebConfig implements WebMvcConfigurer {

    private final FeedAddOn addOn;

    FeedWebConfig(FeedAddOn addOn) {
        this.addOn = addOn;
    }

    /** Requests about a society go through only while it has this context's add-on switched on. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new FeedAddOnInterceptor(addOn)).addPathPatterns("/feed/societies/*/**", "/feed/public/societies/*/**");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/feed", HandlerTypePredicate.forBasePackage("com.mycity.runwal.feed"));
    }
}
