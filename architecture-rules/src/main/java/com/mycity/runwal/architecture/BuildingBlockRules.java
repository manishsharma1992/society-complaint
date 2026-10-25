package com.mycity.runwal.architecture;

import static com.mycity.runwal.architecture.ArchitectureConventions.AGGREGATE_ROOT;
import static com.mycity.runwal.architecture.ArchitectureConventions.APPLICATION_SERVICE;
import static com.mycity.runwal.architecture.ArchitectureConventions.APPLICATION_SERVICE_ANNOTATION;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN_ENTITY;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN_EVENT;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN_EVENT_TYPE;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN_MODEL;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN_REPOSITORY;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN_SERVICE;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN_SERVICE_ANNOTATION;
import static com.mycity.runwal.architecture.ArchitectureConventions.INFRASTRUCTURE_PERSISTENCE;
import static com.mycity.runwal.architecture.ArchitectureConventions.VALUE_OBJECT;
import static com.mycity.runwal.architecture.ArchitectureConventions.beRecords;
import static com.mycity.runwal.architecture.ArchitectureConventions.haveOnlyFinalFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** DDD building blocks are used where they belong (Evans ch. 5 and 6). */
public final class BuildingBlockRules {

    private static final String REPOSITORY_SUFFIX = "Repository";
    private static final String SPRING_TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";
    private static final String JAKARTA_TRANSACTIONAL = "jakarta.transaction.Transactional";
    private static final String JPA_ENTITY = "jakarta.persistence.Entity";
    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";
    private static final String PAST_TENSE_NAME =
            ".*(ed|Sent|Built|Made|Done|Undone|Taken|Given|Withdrawn|Written|Held|Paid|Begun)";

    private static final ArchCondition<JavaClass> BE_NAMED_AFTER_AN_AGGREGATE_ROOT_IN_THE_SAME_PACKAGE =
            new ArchCondition<>("be named <AggregateRoot>Repository after an @AggregateRoot in the same package") {
                @Override
                public void check(JavaClass repository, ConditionEvents events) {
                    String name = repository.getSimpleName();
                    String rootName = name.endsWith(REPOSITORY_SUFFIX)
                            ? name.substring(0, name.length() - REPOSITORY_SUFFIX.length())
                            : null;
                    boolean satisfied = rootName != null && repository.getPackage().getClasses().stream()
                            .anyMatch(c -> c.getSimpleName().equals(rootName) && c.isAnnotatedWith(AGGREGATE_ROOT));
                    events.add(new SimpleConditionEvent(repository, satisfied, repository.getName()
                            + (satisfied ? " is" : " is not")
                            + " named after an @AggregateRoot in package " + repository.getPackageName()));
                }
            };

    private static final ArchCondition<JavaClass> USE_TRANSACTIONAL =
            new ArchCondition<>("use @Transactional") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    if (javaClass.isAnnotatedWith(SPRING_TRANSACTIONAL)
                            || javaClass.isAnnotatedWith(JAKARTA_TRANSACTIONAL)) {
                        events.add(SimpleConditionEvent.satisfied(javaClass,
                                javaClass.getName() + " is annotated with @Transactional"));
                    }
                    for (JavaMethod method : javaClass.getMethods()) {
                        if (method.isAnnotatedWith(SPRING_TRANSACTIONAL)
                                || method.isAnnotatedWith(JAKARTA_TRANSACTIONAL)) {
                            events.add(SimpleConditionEvent.satisfied(method,
                                    method.getFullName() + " is annotated with @Transactional"));
                        }
                    }
                }
            };

    @ArchTest
    public static final ArchRule AGGREGATE_ROOTS_LIVE_IN_DOMAIN_MODEL = classes()
            .that().areAnnotatedWith(AGGREGATE_ROOT)
            .should().resideInAPackage(DOMAIN_MODEL)
            .as("BB-01: @AggregateRoot classes must reside in domain.model")
            .because("""
                    WHY: An aggregate is part of the domain model and owns the package of its members \
                    (Evans ch. 6, Aggregates; ch. 5, Modules).
                    FIX: Move the class into domain.model.<aggregate>, together with its entities, value \
                    objects, repository interface and factory.
                    EXAMPLE: application.service.Complaint \
                    -> domain.model.complaint.Complaint""");

    @ArchTest
    public static final ArchRule VALUE_OBJECTS_ARE_IMMUTABLE = classes()
            .that().areAnnotatedWith(VALUE_OBJECT)
            .should(haveOnlyFinalFields())
            .as("BB-02: @ValueObject classes must be records or have only final fields")
            .because("""
                    WHY: A value object has no identity; it is defined by its attributes and must never \
                    change after creation (Evans ch. 5, Value Objects).
                    FIX: Turn the class into a record, or make every field final and return a new instance \
                    from operations that "change" it.
                    EXAMPLE: class Sla { int hours; void setHours(int h) } \
                    -> record Sla(int hours) { Sla extendBy(int h) { return new Sla(hours + h); } }""");

    @ArchTest
    public static final ArchRule INNER_ENTITIES_ARE_NOT_PUBLIC = classes()
            .that().areAnnotatedWith(DOMAIN_ENTITY)
            .should().notBePublic()
            .as("BB-03: @DomainEntity classes (non-root entities) must not be public")
            .because("""
                    WHY: Only the aggregate root may be referenced from outside the aggregate; inner \
                    entities are reached through the root so it can enforce invariants (Evans ch. 6, Aggregates).
                    FIX: Remove the public modifier and expose behaviour through the aggregate root. Return \
                    value objects or read-only views if callers need the data.
                    EXAMPLE: public class ComplaintComment \
                    -> class ComplaintComment + complaint.addComment(authorId, text)""")
            // inner entities are optional: an aggregate made only of value objects (e.g. Identity) is valid
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule REPOSITORIES_ARE_DOMAIN_INTERFACES_FOR_AGGREGATE_ROOTS = classes()
            .that().areAnnotatedWith(DOMAIN_REPOSITORY)
            .should().beInterfaces()
            .andShould().resideInAPackage(DOMAIN_MODEL)
            .andShould(BE_NAMED_AFTER_AN_AGGREGATE_ROOT_IN_THE_SAME_PACKAGE)
            .as("BB-04: @DomainRepository must be an interface in domain.model named <AggregateRoot>Repository")
            .because("""
                    WHY: Repositories give access to aggregate roots only, and their contract belongs to \
                    the domain while the implementation is technical (Evans ch. 6, Repositories).
                    FIX: Declare the repository as an interface next to its aggregate root, name it after \
                    the root, and implement it in infrastructure.persistence. Inner entities get no repository.
                    EXAMPLE: class CommentRepository in infrastructure \
                    -> interface domain.model.complaint.ComplaintRepository + ComplaintRepositoryAdapter""");

    @ArchTest
    public static final ArchRule DOMAIN_EVENTS_ARE_PAST_TENSE_RECORDS_IN_DOMAIN_EVENT = classes()
            .that().implement(DOMAIN_EVENT_TYPE)
            .should().resideInAPackage(DOMAIN_EVENT)
            .andShould(beRecords())
            .andShould().haveNameMatching(PAST_TENSE_NAME)
            .as("BB-05: Domain events must be past-tense records in domain.event")
            .because("""
                    WHY: A domain event is an immutable fact about something that already happened \
                    (Evans, DDD Reference: Domain Events).
                    FIX: Make it a record in domain.event and name it <Subject><PastTenseVerb>. Allowed \
                    endings: -ed, Sent, Built, Made, Done, Undone, Taken, Given, Withdrawn, Written, Held, Paid, Begun.
                    EXAMPLE: class RaiseComplaintEvent in domain.model \
                    -> record domain.event.ComplaintRaised(ComplaintId complaintId, Instant occurredOn)""");

    @ArchTest
    public static final ArchRule DOMAIN_SERVICES_ARE_STATELESS_AND_IN_DOMAIN_SERVICE = classes()
            .that().areAnnotatedWith(DOMAIN_SERVICE_ANNOTATION)
            .should().resideInAPackage(DOMAIN_SERVICE)
            .andShould(haveOnlyFinalFields())
            .as("BB-06: @DomainService classes must reside in domain.service and be stateless")
            .because("""
                    WHY: A domain service is a stateless operation from the domain that does not belong to \
                    a single entity or value object (Evans ch. 5, Services).
                    FIX: Move it to domain.service and make all fields final dependencies; keep changing \
                    state in the aggregates it works on.
                    EXAMPLE: class SlaCalculator { private Duration lastResult; } \
                    -> final class SlaCalculator { Duration dueIn(ComplaintCategory c) {...} }""")
            // domain services are optional: a context without any (e.g. Residency) is valid
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule APPLICATION_SERVICES_LIVE_IN_APPLICATION_SERVICE = classes()
            .that().areAnnotatedWith(APPLICATION_SERVICE_ANNOTATION)
            .should().resideInAPackage(APPLICATION_SERVICE)
            .as("BB-07: @ApplicationService classes must reside in application.service")
            .because("""
                    WHY: Application services coordinate use cases and form the application layer \
                    (Evans ch. 4, Layered Architecture).
                    FIX: Move the class to application.service. If it holds business rules, move those \
                    rules into the aggregate or a domain service first.
                    EXAMPLE: domain.service.RaiseComplaintService (@ApplicationService) \
                    -> application.service.RaiseComplaintService""");

    @ArchTest
    public static final ArchRule TRANSACTIONS_ONLY_IN_APPLICATION_SERVICES = noClasses()
            .that().resideOutsideOfPackage(APPLICATION_SERVICE)
            .should(USE_TRANSACTIONAL)
            .as("BB-08: @Transactional may only be used in application.service")
            .because("""
                    WHY: A use case is the unit of work, and one transaction should change one aggregate; \
                    the application layer owns that boundary (Evans ch. 4 and ch. 6, Aggregates).
                    FIX: Remove @Transactional from the controller, adapter or domain class and put it on \
                    the application service method that runs the use case.
                    EXAMPLE: @Transactional on ComplaintRepositoryAdapter.save(...) \
                    -> @Transactional on RaiseComplaintService.raise(...)""");

    @ArchTest
    public static final ArchRule JPA_ENTITIES_ARE_INFRASTRUCTURE = classes()
            .that().areAnnotatedWith(JPA_ENTITY)
            .should().resideInAPackage(INFRASTRUCTURE_PERSISTENCE)
            .andShould().haveSimpleNameEndingWith("JpaEntity")
            .as("BB-09: JPA @Entity classes must reside in infrastructure.persistence and be named *JpaEntity")
            .because("""
                    WHY: The persistence model is a technical detail kept apart from the domain model \
                    (Evans ch. 4, Isolating the Domain; ch. 6, Repositories).
                    FIX: Move the @Entity class to infrastructure.persistence, suffix it with JpaEntity, \
                    and map it to and from the domain aggregate in a mapper.
                    EXAMPLE: domain.model.complaint.Complaint (@Entity) \
                    -> infrastructure.persistence.ComplaintJpaEntity + ComplaintMapper""");

    @ArchTest
    public static final ArchRule SPRING_DATA_REPOSITORIES_STAY_IN_PERSISTENCE = CompositeArchRule
            .of(classes()
                    .that().areAssignableTo(SPRING_DATA_REPOSITORY)
                    .should().resideInAPackage(INFRASTRUCTURE_PERSISTENCE))
            .and(noClasses()
                    .that().resideOutsideOfPackage(INFRASTRUCTURE_PERSISTENCE)
                    .should().dependOnClassesThat().areAssignableTo(SPRING_DATA_REPOSITORY))
            .as("BB-10: Spring Data repositories must reside in and only be used by infrastructure.persistence")
            .because("""
                    WHY: Spring Data repositories work on JPA entities; the rest of the service must use \
                    the domain repository interface (Evans ch. 6, Repositories).
                    FIX: Keep the Spring Data interface in infrastructure.persistence and call it only from \
                    the <Aggregate>RepositoryAdapter that implements the domain repository.
                    EXAMPLE: RaiseComplaintService using ComplaintJpaRepository \
                    -> RaiseComplaintService using domain.model.complaint.ComplaintRepository""");

    private BuildingBlockRules() {
    }
}
