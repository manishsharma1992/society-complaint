package com.mycity.runwal.workforce.infrastructure.config;

import com.mycity.runwal.shared.id.SocietyId;
import com.mycity.runwal.workforce.application.port.SocietySites;
import com.mycity.runwal.workforce.domain.model.shift.GeoPoint;
import com.mycity.runwal.workforce.domain.model.shift.Site;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/** Each society's site from application.yml ({@code workforce.sites.<societyId>.latitude|longitude|radius-meters}). */
@Component
class ConfiguredSocietySites implements SocietySites {

    private final Map<Long, Site> sites;

    ConfiguredSocietySites(SitesProperties properties) {
        this.sites = properties.sites().entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                e -> new Site(new GeoPoint(e.getValue().latitude(), e.getValue().longitude()),
                        e.getValue().radiusMeters())));
    }

    @Override
    public Site siteOf(SocietyId societyId) {
        Site site = sites.get(societyId.value());
        if (site == null) {
            throw new IllegalStateException("No workforce.sites." + societyId.value() + " configured");
        }
        return site;
    }

    @ConfigurationProperties("workforce")
    record SitesProperties(Map<Long, SiteProperties> sites) {
        SitesProperties {
            sites = sites == null ? Map.of() : Map.copyOf(sites);
        }
    }

    record SiteProperties(double latitude, double longitude, double radiusMeters) {
    }

    @Configuration
    @EnableConfigurationProperties(SitesProperties.class)
    static class Enable {
    }
}
