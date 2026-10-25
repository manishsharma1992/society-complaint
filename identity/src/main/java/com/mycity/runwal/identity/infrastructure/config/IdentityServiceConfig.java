package com.mycity.runwal.identity.infrastructure.config;

import com.mycity.runwal.shared.ddd.annotation.ApplicationService;
import com.mycity.runwal.shared.ddd.annotation.DomainService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Registers application and domain services as beans by their building-block annotation,
 * so those layers need no Spring stereotype annotations.
 */
@Configuration
@ComponentScan(
        basePackages = "com.mycity.runwal.identity",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION, classes = {ApplicationService.class, DomainService.class}))
class IdentityServiceConfig {
}
