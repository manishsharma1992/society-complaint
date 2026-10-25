package com.mycity.runwal.architecture;

import static com.mycity.runwal.architecture.ArchitectureConventions.APPLICATION_PORT;
import static com.mycity.runwal.architecture.ArchitectureConventions.BASE_PACKAGE;
import static com.mycity.runwal.architecture.ArchitectureConventions.INFRASTRUCTURE_CLIENT;
import static com.mycity.runwal.architecture.ArchitectureConventions.contextOf;
import static com.mycity.runwal.architecture.ArchitectureConventions.isInPackageTree;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependenciesWhere;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Bounded contexts stay separate and integrate only through explicit seams (Evans Part IV). */
public final class BoundedContextRules {

    private static final DescribedPredicate<Dependency> STAY_INSIDE_OWN_CONTEXT_OR_SHARED_KERNEL =
            DescribedPredicate.describe("the target is outside com.mycity.runwal, in the shared kernel or in the same context",
                    dependency -> {
                        String targetPackage = dependency.getTargetClass().getBaseComponentType().getPackageName();
                        if (!isInPackageTree(targetPackage, BASE_PACKAGE)
                                || isInPackageTree(targetPackage, BASE_PACKAGE + ".shared")) {
                            return true;
                        }
                        return contextOf(targetPackage)
                                .equals(contextOf(dependency.getOriginClass().getPackageName()));
                    });

    @ArchTest
    public static final ArchRule SERVICES_DO_NOT_DEPEND_ON_OTHER_SERVICES = classes()
            .should(onlyHaveDependenciesWhere(STAY_INSIDE_OWN_CONTEXT_OR_SHARED_KERNEL))
            .as("BC-01: A service must not depend on another service's packages")
            .because("""
                    WHY: Each bounded context has its own model and ubiquitous language; sharing classes \
                    across contexts blurs their boundaries (Evans ch. 14, Bounded Context).
                    FIX: Reference the other context by a shared-kernel ID only. If you need its data, \
                    declare an outbound port in application.port and implement it with a client in \
                    infrastructure.client that translates the other service's DTOs into your own types.
                    EXAMPLE: complaint code importing com.mycity.runwal.residency.domain.model.flat.Flat \
                    -> hold a shared FlatId and ask application.port.FlatDirectory for what you need""");

    @ArchTest
    public static final ArchRule PORTS_ARE_INTERFACES = classes()
            .that().resideInAPackage(APPLICATION_PORT)
            .should().beInterfaces()
            .as("BC-03: Classes in application.port must be interfaces")
            .because("""
                    WHY: A port states what the application needs from another context or system in its \
                    own language; the anticorruption layer implements it (Evans ch. 14, Anticorruption Layer).
                    FIX: Turn the class into an interface and move the implementation to \
                    infrastructure.client.
                    EXAMPLE: class application.port.ResidencyLookup (calls HTTP) \
                    -> interface application.port.FlatDirectory + infrastructure.client.ResidencyClient""");

    @ArchTest
    public static final ArchRule ONLY_CLIENTS_CALL_OTHER_SERVICES_OVER_HTTP = noClasses()
            .that().resideOutsideOfPackage(INFRASTRUCTURE_CLIENT)
            .should().dependOnClassesThat(nameMatching(
                    "org\\.springframework\\.web\\.client\\.(RestClient|RestTemplate)(\\$.*)?"
                            + "|org\\.springframework\\.web\\.reactive\\.function\\.client\\.WebClient(\\$.*)?"))
            .as("BC-04: Only infrastructure.client may call other services over HTTP")
            .because("""
                    WHY: All translation between contexts goes through the anticorruption layer, so the \
                    other service's model never leaks into ours (Evans ch. 14, Anticorruption Layer).
                    FIX: Move the RestClient/WebClient/RestTemplate call into a *Client in \
                    infrastructure.client that implements a port from application.port.
                    EXAMPLE: RaiseComplaintService calling restClient.get().uri("/api/residency/...") \
                    -> RaiseComplaintService calling flatDirectory.find(flatId), implemented by ResidencyClient""");

    private BoundedContextRules() {
    }
}
