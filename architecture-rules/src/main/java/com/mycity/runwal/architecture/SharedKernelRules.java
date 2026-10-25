package com.mycity.runwal.architecture;

import static com.mycity.runwal.architecture.ArchitectureConventions.SHARED_KERNEL;
import static com.mycity.runwal.architecture.ArchitectureConventions.SHARED_KERNEL_ANNOTATIONS;
import static com.mycity.runwal.architecture.ArchitectureConventions.VALUE_OBJECT;
import static com.mycity.runwal.architecture.ArchitectureConventions.haveOnlyFinalFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Rules for the shared kernel module itself (Evans ch. 14, Shared Kernel). */
public final class SharedKernelRules {

    private static final ArchCondition<JavaClass> HAVE_RUNTIME_RETENTION =
            new ArchCondition<>("have @Retention(RUNTIME)") {
                @Override
                public void check(JavaClass annotation, ConditionEvents events) {
                    boolean runtime = annotation.tryGetAnnotationOfType(Retention.class.getName())
                            .flatMap(retention -> retention.get("value"))
                            .filter(JavaEnumConstant.class::isInstance)
                            .map(value -> ((JavaEnumConstant) value).name().equals(RetentionPolicy.RUNTIME.name()))
                            .orElse(false);
                    events.add(new SimpleConditionEvent(annotation, runtime, annotation.getName()
                            + (runtime ? " has" : " does not have") + " @Retention(RUNTIME)"));
                }
            };

    @ArchTest
    public static final ArchRule SHARED_KERNEL_IS_PURE_JAVA = classes()
            .that().resideInAPackage(SHARED_KERNEL)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", SHARED_KERNEL)
            .as("BC-02: The shared kernel must be pure Java and must not depend on any service")
            .because("""
                    WHY: Every context depends on the shared kernel, so it must not drag in a framework or \
                    another context's model (Evans ch. 14, Shared Kernel).
                    FIX: Remove the dependency. Framework code belongs in a service's infrastructure; \
                    context-specific types belong in that context. Changes here must be agreed by all contexts.
                    EXAMPLE: @Embeddable on shared.id.FlatId \
                    -> plain record FlatId(long value), mapped in each service's infrastructure.persistence""");

    @ArchTest
    public static final ArchRule BUILDING_BLOCK_ANNOTATIONS_ARE_RUNTIME = classes()
            .that().resideInAPackage(SHARED_KERNEL_ANNOTATIONS)
            .and().areAnnotations()
            .should(HAVE_RUNTIME_RETENTION)
            .as("SK-01: Building-block annotations must have RUNTIME retention")
            .because("""
                    WHY: The building-block markers (Evans ch. 5 and 6) are what the ArchUnit rules check; \
                    without RUNTIME retention ArchUnit cannot see them and those rules silently match nothing.
                    FIX: Add @Retention(RetentionPolicy.RUNTIME) to the annotation.
                    EXAMPLE: public @interface ValueObject {} \
                    -> @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface ValueObject {}""");

    @ArchTest
    public static final ArchRule SHARED_VALUE_OBJECTS_ARE_IMMUTABLE = classes()
            .that().resideInAPackage(SHARED_KERNEL)
            .and().areAnnotatedWith(VALUE_OBJECT)
            .should(haveOnlyFinalFields())
            .as("SK-02: Shared kernel value objects must be immutable")
            .because("""
                    WHY: Cross-context IDs are value objects passed between contexts; they must never change \
                    after creation (Evans ch. 5, Value Objects).
                    FIX: Use a record, or make every field final.
                    EXAMPLE: class FlatId { long value; } -> record FlatId(long value)""");

    private SharedKernelRules() {
    }
}
