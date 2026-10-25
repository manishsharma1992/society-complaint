package com.mycity.runwal.architecture;

import static com.mycity.runwal.architecture.ArchitectureConventions.APPLICATION;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN;
import static com.mycity.runwal.architecture.ArchitectureConventions.INFRASTRUCTURE;
import static com.mycity.runwal.architecture.ArchitectureConventions.INTERFACES;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Layered architecture inside one service (Evans ch. 4, Isolating the Domain). */
public final class LayerRules {

    @ArchTest
    public static final ArchRule LAYERED_ARCHITECTURE = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy(DOMAIN)
            .layer("Application").definedBy(APPLICATION)
            .layer("Infrastructure").definedBy(INFRASTRUCTURE)
            .layer("Interfaces").definedBy(INTERFACES)
            .whereLayer("Interfaces").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Interfaces", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .as("LAYER-01: Layered architecture interfaces -> application -> domain <- infrastructure")
            .because("""
                    WHY: Each layer depends only on the layers below it so the domain stays isolated and \
                    replaceable technology lives at the edges (Evans ch. 4, Layered Architecture).
                    FIX: Move the code to the layer that owns the concern, or invert the dependency with an \
                    interface (a port in application.port or a repository interface in domain.model) that \
                    the outer layer implements.
                    EXAMPLE: application.service.RaiseComplaintService using infrastructure.persistence.ComplaintJpaRepository \
                    -> use domain.model.complaint.ComplaintRepository, implemented by \
                    infrastructure.persistence.ComplaintRepositoryAdapter""");

    @ArchTest
    public static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_OUTER_LAYERS = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE, INTERFACES)
            .as("LAYER-02: Domain must not depend on application, infrastructure or interfaces")
            .because("""
                    WHY: The domain is the heart of the software and must not know who uses it or how it is \
                    stored or exposed (Evans ch. 4, Isolating the Domain).
                    FIX: Remove the import. If the domain needs something from outside, declare an interface \
                    in the domain (e.g. a repository) and implement it in infrastructure.
                    EXAMPLE: domain.model.complaint.Complaint importing interfaces.rest.ComplaintResponse \
                    -> let interfaces.rest map from an application query view instead""");

    @ArchTest
    public static final ArchRule APPLICATION_MUST_NOT_DEPEND_ON_INFRASTRUCTURE_OR_INTERFACES = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE, INTERFACES)
            .as("LAYER-03: Application must not depend on infrastructure or interfaces")
            .because("""
                    WHY: Application services describe use cases; they must not be tied to a database, \
                    HTTP client or REST DTO (Evans ch. 4, Layered Architecture).
                    FIX: Depend on a domain repository interface or an outbound port in application.port; \
                    accept a command record, not a REST request DTO.
                    EXAMPLE: RaiseComplaintService(RaiseComplaintRequest request) \
                    -> RaiseComplaintService.raise(RaiseComplaintCommand command)""");

    @ArchTest
    public static final ArchRule INTERFACES_MUST_NOT_DEPEND_ON_DOMAIN_OR_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage(INTERFACES)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRASTRUCTURE)
            .as("LAYER-04: Interfaces must not depend on domain or infrastructure")
            .because("""
                    WHY: Controllers talk only to application services and return DTOs, so the domain model \
                    can change without breaking the API (Evans ch. 4, Layered Architecture).
                    FIX: Call an application service with a command/query and map its result (a *View record \
                    from application.query) to a response DTO in interfaces.rest.
                    EXAMPLE: ComplaintController returning domain.model.complaint.Complaint \
                    -> return ComplaintResponse built from application.query.ComplaintView""");

    @ArchTest
    public static final ArchRule NO_CYCLES_BETWEEN_LAYERS = slices()
            .matching(ArchitectureConventions.BASE_PACKAGE + ".(*).(*)..")
            .should().beFreeOfCycles()
            .as("LAYER-05: No package cycles between the top-level packages of a service")
            .because("""
                    WHY: Cycles make layers inseparable and hide the real direction of dependencies \
                    (Evans ch. 4, Layered Architecture).
                    FIX: Break the cycle by moving the shared type to the lower package, or by introducing an \
                    interface in the lower package that the higher one implements.
                    EXAMPLE: domain -> application -> domain \
                    -> move the type the domain needs into domain, or drop the domain's dependency""");

    @ArchTest
    public static final ArchRule NO_CYCLES_BETWEEN_AGGREGATES = slices()
            .matching(ArchitectureConventions.BASE_PACKAGE + ".(*).domain.model.(*)..")
            .should().beFreeOfCycles()
            .as("LAYER-05: No package cycles between aggregates")
            .because("""
                    WHY: Aggregates are separate consistency boundaries and reference each other by ID only \
                    (Evans ch. 6, Aggregates); a cycle means they are tangled.
                    FIX: Replace the object reference to the other aggregate with its typed ID.
                    EXAMPLE: complaint.Complaint holding category.ComplaintCategory \
                    -> hold category.ComplaintCategoryId instead""");

    private LayerRules() {
    }
}
