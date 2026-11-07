package com.mycity.runwal.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Package layout and shared kernel type names that every rule class relies on.
 * The shared kernel is referenced by name (not by class) so this module does not depend on it.
 */
final class ArchitectureConventions {

    static final String BASE_PACKAGE = "com.mycity.runwal";

    static final String SHARED_KERNEL = BASE_PACKAGE + ".shared..";
    static final String SHARED_KERNEL_ANNOTATIONS = BASE_PACKAGE + ".shared.ddd.annotation..";

    static final String DOMAIN = BASE_PACKAGE + "..domain..";
    static final String DOMAIN_MODEL = BASE_PACKAGE + "..domain.model..";
    static final String DOMAIN_SERVICE = BASE_PACKAGE + "..domain.service";
    static final String DOMAIN_EVENT = BASE_PACKAGE + "..domain.event";

    static final String APPLICATION = BASE_PACKAGE + "..application..";
    static final String APPLICATION_COMMAND = BASE_PACKAGE + "..application.command..";
    static final String APPLICATION_QUERY = BASE_PACKAGE + "..application.query..";
    static final String APPLICATION_SERVICE = BASE_PACKAGE + "..application.service";
    static final String APPLICATION_PORT = BASE_PACKAGE + "..application.port..";

    static final String INFRASTRUCTURE = BASE_PACKAGE + "..infrastructure..";
    static final String INFRASTRUCTURE_PERSISTENCE = BASE_PACKAGE + "..infrastructure.persistence..";
    static final String INFRASTRUCTURE_CLIENT = BASE_PACKAGE + "..infrastructure.client..";
    static final String INFRASTRUCTURE_MEDIA = BASE_PACKAGE + "..infrastructure.media..";

    static final String INTERFACES = BASE_PACKAGE + "..interfaces..";
    static final String INTERFACES_REST = BASE_PACKAGE + "..interfaces.rest..";

    private static final String ANNOTATIONS = BASE_PACKAGE + ".shared.ddd.annotation.";
    static final String AGGREGATE_ROOT = ANNOTATIONS + "AggregateRoot";
    static final String DOMAIN_ENTITY = ANNOTATIONS + "DomainEntity";
    static final String VALUE_OBJECT = ANNOTATIONS + "ValueObject";
    static final String DOMAIN_SERVICE_ANNOTATION = ANNOTATIONS + "DomainService";
    static final String DOMAIN_REPOSITORY = ANNOTATIONS + "DomainRepository";
    static final String APPLICATION_SERVICE_ANNOTATION = ANNOTATIONS + "ApplicationService";
    static final String DOMAIN_EVENT_TYPE = BASE_PACKAGE + ".shared.ddd.DomainEvent";

    static final DescribedPredicate<JavaClass> RECORDS =
            DescribedPredicate.describe("are records", ArchitectureConventions::isRecord);

    private ArchitectureConventions() {
    }

    /** The bounded context of a package, e.g. {@code complaint} for {@code com.mycity.runwal.complaint.domain}. */
    static String contextOf(String packageName) {
        if (!packageName.startsWith(BASE_PACKAGE + ".")) {
            return "";
        }
        String rest = packageName.substring(BASE_PACKAGE.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    static boolean isInPackageTree(String packageName, String root) {
        return packageName.equals(root) || packageName.startsWith(root + ".");
    }

    static boolean isRecord(JavaClass javaClass) {
        return javaClass.getRawSuperclass().map(s -> s.isEquivalentTo(Record.class)).orElse(false);
    }

    static ArchCondition<JavaClass> beRecords() {
        return new ArchCondition<>("be records") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean record = isRecord(javaClass);
                events.add(new SimpleConditionEvent(javaClass, record,
                        javaClass.getName() + (record ? " is" : " is not") + " a record"));
            }
        };
    }

    static ArchCondition<JavaClass> haveOnlyFinalFields() {
        return new ArchCondition<>("have only final fields") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaField field : javaClass.getFields()) {
                    if (!field.getModifiers().contains(JavaModifier.FINAL)) {
                        events.add(SimpleConditionEvent.violated(field,
                                "Field " + field.getFullName() + " is not final"));
                    }
                }
            }
        };
    }
}
