package ec.edu.uce.meditrack.identity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Keeps the dependencies pointing inwards: adapters depend on the core, never the other way round.
 *
 * <p>Empty matches are allowed because the core packages are filled by later tasks.
 */
@AnalyzeClasses(
        packages = "ec.edu.uce.meditrack.identity",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "..identity.domain..";
    private static final String APPLICATION = "..identity.application..";
    private static final String PORTS = "..identity.ports..";
    private static final String ADAPTERS = "..identity.adapters..";

    @ArchTest
    static final ArchRule domainDependsOnNoOtherLayer = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, PORTS, ADAPTERS)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule coreDoesNotDependOnAdapters = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION, PORTS)
            .should().dependOnClassesThat().resideInAPackage(ADAPTERS)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .that().resideInAnyPackage(DOMAIN, APPLICATION, PORTS)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule portsDoNotDependOnUseCases = noClasses()
            .that().resideInAPackage(PORTS)
            .should().dependOnClassesThat().resideInAPackage(APPLICATION)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adaptersAreIndependentFromEachOther = slices()
            .matching("..identity.adapters.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true);
}
