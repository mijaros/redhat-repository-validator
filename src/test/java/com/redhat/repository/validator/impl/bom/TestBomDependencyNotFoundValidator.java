package com.redhat.repository.validator.impl.bom;

import static com.redhat.repository.validator.impl.TestUtil.dependency;
import static com.redhat.repository.validator.impl.TestUtil.pom;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.model.Model;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.redhat.repository.validator.impl.AbstractTest;
import com.redhat.repository.validator.impl.bom.BomDependencyNotFoundException;

@ContextConfiguration
public class TestBomDependencyNotFoundValidator extends AbstractTest {

    @Configuration
    public static class TestConfiguration {

        @Bean
        public IOFileFilter dependenciesValidatorFilter() {
            return FileFilterUtils.trueFileFilter();
        }

        @Bean
        public IOFileFilter bomDependencyNotFoundValidatorFilter() {
            return FileFilterUtils.trueFileFilter();
        }

    }

    @Test
    public void shouldResolveBomDependency() {
        Model bar = pom().artifactId("bar").create(repoBarDir);

        pom().artifactId("foo-bom").
                packaging("pom").
                dependencyManagement(bar).
                create(repoFooDir);

        validationExecutor.execute(ctx);

        assertSuccess();
        assertLocalRepoContains(bar);
    }

    @Test
    public void shouldResolveBomTransitiveDependency() {
        Model barApi = pom().artifactId("bar-api").create(repoBarDir);
        Model barImpl = pom().artifactId("bar-impl").dependency(barApi).create(repoBarDir);

        pom().artifactId("foo-bom").
                packaging("pom").
                dependencyManagement(barImpl).
                create(repoFooDir);

        validationExecutor.execute(ctx);

        assertSuccess();
        assertLocalRepoContains(barApi);
        assertLocalRepoContains(barImpl);
    }

    @Test
    public void shouldFindMissingBomDependency() {
        Model bar = pom().artifactId("bar").model();

        pom().artifactId("foo-bom").
                packaging("pom").
                dependencyManagement(bar).
                create(repoFooDir);

        validationExecutor.execute(ctx);

        assertExpectedException(BomDependencyNotFoundException.class, "com.acme:bar:jar:1.0");
    }

    @Test
    public void shouldFindMissingBomTransitiveDependency() {
        Model barApi = pom().artifactId("bar-api").model();
        Model barImpl = pom().artifactId("bar-impl").dependency(barApi).create(repoBarDir);

        pom().artifactId("foo-bom").
                packaging("pom").
                dependencyManagement(barImpl).
                create(repoFooDir);

        validationExecutor.execute(ctx);

        assertExpectedException(BomDependencyNotFoundException.class, "com.acme:bar-api:jar:1.0");
    }

    @Test
    public void shouldIgnoreBomExcludedDependency() {
        Model barApi = pom().artifactId("bar-api").model();
        Model barImpl = pom().artifactId("bar-impl").dependency(barApi).create(repoBarDir);

        pom().artifactId("foo-bom").
                packaging("pom").
                dependencyManagement(dependency().to(barImpl).exclude(barApi).build()).
                create(repoFooDir);

        validationExecutor.execute(ctx);

        assertSuccess();
        assertLocalRepoContains(barImpl);
    }

}