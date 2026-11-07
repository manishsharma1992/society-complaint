package com.mycity.runwal.architecture;

import static com.mycity.runwal.architecture.ArchitectureConventions.APPLICATION;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN;
import static com.mycity.runwal.architecture.ArchitectureConventions.INFRASTRUCTURE_MEDIA;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.GeneralCodingRules.ACCESS_STANDARD_STREAMS;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** General coding conventions for every service. */
public final class CodingRules {

    @ArchTest
    public static final ArchRule NO_FIELD_INJECTION = noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .orShould().beAnnotatedWith("jakarta.inject.Inject")
            .orShould().beAnnotatedWith("javax.inject.Inject")
            .as("CODE-01: No field injection")
            .because("""
                    WHY: Constructor injection makes dependencies explicit, allows final fields and lets \
                    classes be created in plain unit tests (Evans ch. 10, Supple Design: Intention-Revealing Interfaces).
                    FIX: Make the field private final and take it as a constructor parameter; a single \
                    constructor needs no @Autowired.
                    EXAMPLE: @Autowired private ComplaintRepository repository; \
                    -> private final ComplaintRepository repository; + constructor parameter""");

    @ArchTest
    public static final ArchRule NO_STANDARD_STREAMS = noClasses()
            .should(ACCESS_STANDARD_STREAMS)
            .as("CODE-02: No System.out or System.err")
            .because("""
                    WHY: Standard streams bypass log levels, formatting and log shipping on the server.
                    FIX: Use an SLF4J logger. Domain code should not log at all; record a domain event or \
                    throw a domain exception instead.
                    EXAMPLE: System.out.println("raised " + id) \
                    -> private static final Logger log = LoggerFactory.getLogger(X.class); log.info("raised {}", id)""");

    @ArchTest
    public static final ArchRule USE_JAVA_TIME = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION)
            .should().dependOnClassesThat().belongToAnyOf(java.util.Date.class, java.sql.Timestamp.class)
            .as("CODE-03: No java.util.Date or java.sql.Timestamp in domain or application")
            .because("""
                    WHY: Date and Timestamp are mutable and time-zone ambiguous; time values in the model \
                    must be immutable value objects (Evans ch. 5, Value Objects).
                    FIX: Use java.time: Instant for points in time, LocalDate for calendar dates, Duration \
                    for SLAs. Convert at the persistence edge if needed.
                    EXAMPLE: private Date raisedAt; -> private final Instant raisedAt;""");

    @ArchTest
    public static final ArchRule PROCESSES_ONLY_IN_INFRASTRUCTURE_MEDIA = noClasses()
            .that().resideOutsideOfPackage(INFRASTRUCTURE_MEDIA)
            .should().dependOnClassesThat().belongToAnyOf(ProcessBuilder.class)
            .orShould().callMethodWhere(target(name("exec")).and(target(owner(type(Runtime.class)))))
            .as("CODE-04: ProcessBuilder and Runtime.exec only in infrastructure.media")
            .because("""
                    WHY: External tools (libvips, ffmpeg) parse untrusted files; starting processes from \
                    anywhere else spreads that risk. Keeping it in one adapter package keeps it auditable \
                    (Evans ch. 4, Isolating the Domain: technical concerns live in infrastructure).
                    FIX: Put an outbound port (e.g. ImageProcessor) in application.port and implement it in \
                    infrastructure.media with ProcessBuilder and an argument list, never a shell.
                    EXAMPLE: application.service.X calls new ProcessBuilder("vips", ...) \
                    -> application.port.ImageProcessor + infrastructure.media.VipsImageProcessor""");

    private CodingRules() {
    }
}
