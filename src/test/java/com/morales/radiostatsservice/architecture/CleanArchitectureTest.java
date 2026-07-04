package com.morales.radiostatsservice.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class CleanArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .importPackages("com.morales.radiostatsservice");
    }

    @Test
    void domainShouldHaveNoSpringOrJakartaImports() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().accessClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "javax.."
                );
        rule.check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnAdapterOrInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter..",
                        "..infrastructure.."
                );
        rule.check(importedClasses);
    }

    @Test
    void adaptersShouldNotCrossReference() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out..");
        rule.check(importedClasses);
    }

    @Test
    void domainServicesShouldOnlyDependOnDomainPorts() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter..",
                        "..infrastructure.."
                );
        rule.check(importedClasses);
    }

    @Test
    void layeredArchitectureShouldBeRespected() {
        ArchRule rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("..domain..")
                .layer("Adapter").definedBy("..adapter..")
                .layer("Infrastructure").definedBy("..infrastructure..")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Adapter").mayOnlyAccessLayers("Domain", "Infrastructure")
                .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Adapter");
        rule.check(importedClasses);
    }
}
