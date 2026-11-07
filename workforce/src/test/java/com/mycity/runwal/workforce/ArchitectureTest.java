package com.mycity.runwal.workforce;

import com.mycity.runwal.architecture.BoundedContextRules;
import com.mycity.runwal.architecture.BuildingBlockRules;
import com.mycity.runwal.architecture.CodingRules;
import com.mycity.runwal.architecture.DomainPurityRules;
import com.mycity.runwal.architecture.LayerRules;
import com.mycity.runwal.architecture.NamingRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.mycity.runwal.workforce", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests layerRules = ArchTests.in(LayerRules.class);

    @ArchTest
    static final ArchTests domainPurityRules = ArchTests.in(DomainPurityRules.class);

    @ArchTest
    static final ArchTests buildingBlockRules = ArchTests.in(BuildingBlockRules.class);

    @ArchTest
    static final ArchTests namingRules = ArchTests.in(NamingRules.class);

    @ArchTest
    static final ArchTests boundedContextRules = ArchTests.in(BoundedContextRules.class);

    @ArchTest
    static final ArchTests codingRules = ArchTests.in(CodingRules.class);
}
