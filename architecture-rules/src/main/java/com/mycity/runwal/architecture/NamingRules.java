package com.mycity.runwal.architecture;

import static com.mycity.runwal.architecture.ArchitectureConventions.APPLICATION_COMMAND;
import static com.mycity.runwal.architecture.ArchitectureConventions.APPLICATION_QUERY;
import static com.mycity.runwal.architecture.ArchitectureConventions.DOMAIN_REPOSITORY;
import static com.mycity.runwal.architecture.ArchitectureConventions.INFRASTRUCTURE_CLIENT;
import static com.mycity.runwal.architecture.ArchitectureConventions.INFRASTRUCTURE_PERSISTENCE;
import static com.mycity.runwal.architecture.ArchitectureConventions.INTERFACES_REST;
import static com.mycity.runwal.architecture.ArchitectureConventions.RECORDS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;

/** Names tell the reader which building block a class is (Evans ch. 2, Ubiquitous Language). */
public final class NamingRules {

    private static final DescribedPredicate<JavaClass> IMPLEMENT_A_DOMAIN_REPOSITORY =
            DescribedPredicate.describe("implement a @DomainRepository interface",
                    javaClass -> javaClass.getAllRawInterfaces().stream()
                            .anyMatch(i -> i.isAnnotatedWith(DOMAIN_REPOSITORY)));

    @ArchTest
    public static final ArchRule REST_CONTROLLERS_ARE_NAMED_CONTROLLER = classes()
            .that().resideInAPackage(INTERFACES_REST)
            .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().haveSimpleNameEndingWith("Controller")
            .as("NAME-01: @RestController classes in interfaces.rest must be named *Controller")
            .because("""
                    WHY: Consistent names show which layer and role a class has at a glance \
                    (Evans ch. 4, Layered Architecture).
                    FIX: Rename the class so it ends with Controller.
                    EXAMPLE: ComplaintResource -> ComplaintController""");

    @ArchTest
    public static final ArchRule COMMANDS_AND_QUERIES_ARE_NAMED_BY_ROLE = CompositeArchRule
            .of(classes()
                    .that().resideInAPackage(APPLICATION_COMMAND).and(RECORDS)
                    .should().haveSimpleNameEndingWith("Command"))
            .and(classes()
                    .that().resideInAPackage(APPLICATION_QUERY).and(RECORDS)
                    .should().haveSimpleNameEndingWith("Query")
                    .orShould().haveSimpleNameEndingWith("View"))
            .as("NAME-02: Records in application.command must be *Command; in application.query *Query or *View")
            .because("""
                    WHY: Separating inputs that change state from inputs and results that only read state \
                    keeps use cases explicit (Evans ch. 10, Side-Effect-Free Functions; Command-Query Separation).
                    FIX: Name state-changing inputs <Verb><Noun>Command, read inputs <Noun>Query and read \
                    models <Noun>View.
                    EXAMPLE: application.command.NewComplaint -> RaiseComplaintCommand; \
                    application.query.ComplaintDto -> ComplaintView""");

    @ArchTest
    public static final ArchRule CLIENTS_ARE_NAMED_CLIENT_OR_ADAPTER = classes()
            .that().resideInAPackage(INFRASTRUCTURE_CLIENT)
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Client")
            .orShould().haveSimpleNameEndingWith("Adapter")
            .as("NAME-03: Classes in infrastructure.client must be named *Client or *Adapter")
            .because("""
                    WHY: Clients form the anticorruption layer to other contexts and should be recognisable \
                    as such (Evans ch. 14, Anticorruption Layer).
                    FIX: Rename the class to <OtherContext>Client or <Port>Adapter. Put the other service's \
                    DTOs as nested records inside the client.
                    EXAMPLE: ResidencyService in infrastructure.client \
                    -> ResidencyClient implements application.port.FlatDirectory""")
            // only contexts that call other services have clients
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule REPOSITORY_ADAPTERS_ARE_NAMED_REPOSITORY_ADAPTER = classes()
            .that().resideInAPackage(INFRASTRUCTURE_PERSISTENCE)
            .and(IMPLEMENT_A_DOMAIN_REPOSITORY)
            .should().haveSimpleNameEndingWith("RepositoryAdapter")
            .as("NAME-04: Domain repository implementations must be named *RepositoryAdapter")
            .because("""
                    WHY: The adapter translates between the domain repository and the persistence \
                    technology; the name keeps it distinct from the Spring Data repository (Evans ch. 6, Repositories).
                    FIX: Rename the implementation to <AggregateRoot>RepositoryAdapter.
                    EXAMPLE: JpaComplaintRepositoryImpl -> ComplaintRepositoryAdapter implements ComplaintRepository""");

    private NamingRules() {
    }
}
