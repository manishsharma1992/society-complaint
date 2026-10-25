package com.mycity.runwal.architecture;

import static com.mycity.runwal.architecture.ArchitectureConventions.BASE_PACKAGE;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN;
import static com.mycity.runwal.architecture.ArchitectureConventions.contextOf;
import static com.mycity.runwal.architecture.ArchitectureConventions.isInPackageTree;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependenciesWhere;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The domain model is pure Java and speaks only the domain language (Evans ch. 4, 5, 10). */
public final class DomainPurityRules {

    private static final DescribedPredicate<Dependency> ON_JDK_SHARED_KERNEL_OR_OWN_DOMAIN =
            DescribedPredicate.describe("the target is in java.., the shared kernel or the class's own domain",
                    dependency -> {
                        JavaClass target = dependency.getTargetClass().getBaseComponentType();
                        if (target.isPrimitive()) {
                            return true;
                        }
                        String targetPackage = target.getPackageName();
                        String ownDomain = BASE_PACKAGE + "."
                                + contextOf(dependency.getOriginClass().getPackageName()) + ".domain";
                        return isInPackageTree(targetPackage, "java")
                                || isInPackageTree(targetPackage, BASE_PACKAGE + ".shared")
                                || isInPackageTree(targetPackage, ownDomain);
                    });

    private static final DescribedPredicate<JavaConstructorCall> RAW_JDK_EXCEPTION =
            DescribedPredicate.describe("the target is RuntimeException, IllegalStateException or IllegalArgumentException",
                    call -> {
                        JavaClass owner = call.getTargetOwner();
                        return owner.isEquivalentTo(RuntimeException.class)
                                || owner.isEquivalentTo(IllegalStateException.class)
                                || owner.isEquivalentTo(IllegalArgumentException.class);
                    });

    @ArchTest
    public static final ArchRule DOMAIN_DEPENDS_ONLY_ON_JDK_AND_SHARED_KERNEL = classes()
            .that().resideInAPackage(DOMAIN)
            .should(onlyHaveDependenciesWhere(ON_JDK_SHARED_KERNEL_OR_OWN_DOMAIN))
            .as("DDD-01: Domain must depend only on java.., the shared kernel and its own domain")
            .because("""
                    WHY: The domain model expresses business rules only; every library it touches couples \
                    those rules to technology (Evans ch. 4, Isolating the Domain).
                    FIX: Remove the dependency. Keep framework code in infrastructure and map to and from \
                    plain domain objects; use java.time, java.util and shared-kernel types in the domain.
                    EXAMPLE: domain.model.complaint.Complaint using org.apache.commons.lang3.StringUtils \
                    -> use String.isBlank() from the JDK""");

    @ArchTest
    public static final ArchRule DOMAIN_MUST_NOT_USE_SPRING = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .as("DDD-02: Domain must not use Spring")
            .because("""
                    WHY: The domain model must be isolated from frameworks so it can be understood and \
                    tested on its own (Evans ch. 4, Isolating the Domain).
                    FIX: Remove Spring annotations and types from the domain class. Wire domain services as \
                    beans in infrastructure.config, and put @Transactional on application services.
                    EXAMPLE: @Service on domain.service.SlaCalculator \
                    -> plain class + @Bean SlaCalculator slaCalculator() in infrastructure.config""");

    @ArchTest
    public static final ArchRule DOMAIN_MUST_NOT_USE_JPA = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "javax.persistence..", "org.hibernate..")
            .as("DDD-03: Domain must not use JPA or Hibernate")
            .because("""
                    WHY: The domain model must be isolated from persistence concerns (Evans ch. 4, \
                    Isolating the Domain).
                    FIX: Remove JPA annotations from the domain class. Create a separate JPA entity in \
                    infrastructure.persistence and map it in a <Aggregate>Mapper.
                    EXAMPLE: domain.model.complaint.Complaint (no annotations) \
                    -> infrastructure.persistence.ComplaintJpaEntity (@Entity) + ComplaintMapper""");

    @ArchTest
    public static final ArchRule DOMAIN_MUST_NOT_USE_JACKSON_VALIDATION_OR_LOMBOK = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.fasterxml.jackson..", "jakarta.validation..", "javax.validation..", "lombok..")
            .as("DDD-04: Domain must not use Jackson, Bean Validation or Lombok")
            .because("""
                    WHY: Serialization and input validation belong to the edges of the system; domain \
                    invariants are enforced by the model itself (Evans ch. 5 and 6).
                    FIX: Put @JsonProperty/@NotBlank on request/response DTOs in interfaces.rest. Validate \
                    invariants in the domain constructor or method and throw a DomainException subclass. \
                    Write constructors and accessors by hand (or use records) instead of Lombok.
                    EXAMPLE: record Title(@NotBlank String value) in domain \
                    -> record Title(String value) { Title { if (value.isBlank()) throw new InvalidTitleException(); } }""");

    @ArchTest
    public static final ArchRule DOMAIN_MUST_NOT_HAVE_SETTERS = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN)
            .and().arePublic()
            .should().haveNameMatching("set[A-Z].*")
            .as("DDD-05: Domain classes must not have public setters")
            .because("""
                    WHY: Setters let callers bypass invariants and hide intent; use intention-revealing \
                    operations instead (Evans ch. 10, Supple Design).
                    FIX: Replace the setter with a method named after the business action that checks the \
                    rules and records what happened.
                    EXAMPLE: complaint.setStatus(ASSIGNED); complaint.setAssignedTo(staffId) \
                    -> complaint.assignTo(staffId)""");

    @ArchTest
    public static final ArchRule DOMAIN_MUST_THROW_DOMAIN_EXCEPTIONS = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().callConstructorWhere(RAW_JDK_EXCEPTION)
            .as("DDD-06: Domain must throw DomainException subclasses, not raw JDK exceptions")
            .because("""
                    WHY: A broken business rule is part of the domain language and deserves a name that \
                    callers can handle (Evans ch. 2, Ubiquitous Language; ch. 10, Assertions).
                    FIX: Create a specific exception in domain.exception that extends \
                    com.mycity.runwal.shared.ddd.DomainException and throw that.
                    EXAMPLE: throw new IllegalStateException("cannot resolve") \
                    -> throw new InvalidStatusTransitionException(status, ComplaintStatus.RESOLVED)""");

    private DomainPurityRules() {
    }
}
