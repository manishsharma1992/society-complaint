package com.mycity.runwal.shared;

import com.mycity.runwal.architecture.SharedKernelRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.mycity.runwal.shared", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests sharedKernelRules = ArchTests.in(SharedKernelRules.class);
}
