package ness.testing;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.MapConfiguration;
import org.eclipse.jetty.server.Server;
import org.junit.Rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.nesscomputing.config.Config;
import com.nesscomputing.httpclient.HttpClient;
import com.nesscomputing.lifecycle.Lifecycle;

/**
 * Builds a {@link Rule} for use in tests which spins up one or more Jetty services, and exposes their HTTP servers
 * to each other and your test case.  Each service you want to spin up is built with a {@link ServiceDefinitionBuilder}.
 * The result of this must be assigned to a public field in your test case annotated as such:
 * <pre> @Rule
 * public IntegrationTestRule testRule = new IntegrationTestBuilder().addService(...</pre>
 *
 * When your test case is run, the rule will spin up a separate {@link Injector} for each service you have configured.
 * This injector will be configured per the {@link ServiceDefinition} provided.  You may specify any number of "mocked"
 * services in the builder, and each of those will have the opportunity to customize both the service and test case
 * environment.  The most commonly used mocked service is the MockedDiscoveryService, which provides read-write discovery
 * abilities within a testing environment.
 *
 * Each injector is created with:
 * <ul>
 * <li> a managed {@link Lifecycle}
 * <li> HttpClient bindings for the <code>srvc</code> protocol to emulate discovery
 * <li> a Jetty {@link Server} bound on an unused localhost port
 * <li> a Config built from the {@link ServiceDefinition}
 * </ul>
 *
 * While the tests are running, you may call {@link IntegrationTestRule#exposeBinding} to retrieve objects
 * from each service's {@link Injector} to inspect internal state of each service.
 *
 * When the test ends, each {@link Injector}'s {@link Lifecycle} will be torn down cleanly.
 */
public class IntegrationTestRuleBuilder {

    /** All services registered with the builder */
    private final Map<String, ServiceDefinition> installedServices = Maps.newHashMap();
    private final List<MockedService> mockedServices = Lists.newArrayList();
    private Config baseConfig = Config.getEmptyConfig();

    /**
     * Register a service to be managed by this integration test rule
     * @param serviceName the name to expose to {@link HttpClient} as
     * @param definition the definition for this service environment
     * @return the builder
     */
    public IntegrationTestRuleBuilder addService(String serviceName, final ServiceDefinition definition) {
        installedServices.put(serviceName, definition);
        return this;
    }

    /**
     * Register a mocking service that is designed to be used for testing and mixes in functionality into
     * the environment
     * @param mockedService the service mocking provider
     * @return the builder
     */
    public IntegrationTestRuleBuilder addMockedService(MockedService mockedService) {
        mockedServices.add(mockedService);
        return this;
    }

    /**
     * Set the base configuration for the test case.  Mocked services may override some of these values.
     * Does not affect services spun up as part of the test, only the test case itself
     */
    public IntegrationTestRuleBuilder setTestConfig(Config baseConfig) {
        this.baseConfig = baseConfig;
        return this;
    }

    /**
     * Set a single configuration option for the test case.
     * Does not affect services spun up as part of the test, only the test case itself
     */
    public IntegrationTestRuleBuilder setTestConfig(String key, String value) {
        this.baseConfig = Config.getOverriddenConfig(baseConfig, new MapConfiguration(Collections.singletonMap(key, value)));
        return this;
    }

    /**
     * Builds the rule so that JUnit may run it
     * @param testCaseItself pass in the test case object so that Guice may perform field injection
     * @return
     */
    public IntegrationTestRule build(Object testCaseItself) {
        return build(testCaseItself, Modules.EMPTY_MODULE);
    }

    /**
     * Builds the rule so that JUnit may run it
     * @param testCaseItself pass in the test case object so that Guice may perform field injection
     * @param testCaseModule any extra modules you would like injected into your test case
     * @return
     */
    public IntegrationTestRule build(Object testCaseItself, Module testCaseModule) {
        return new IntegrationTestRule(installedServices, mockedServices, testCaseModule, testCaseItself, baseConfig);
    }
}
