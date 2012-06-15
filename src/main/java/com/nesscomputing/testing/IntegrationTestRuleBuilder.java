/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nesscomputing.testing;

import static com.nesscomputing.testing.ModuleEnabler.ModulesEnable.httpClientEnabled;
import static com.nesscomputing.testing.ModuleEnabler.ModulesEnable.jacksonEnabled;
import static com.nesscomputing.testing.ModuleEnabler.ModulesEnable.serviceLifecycleEnabled;

import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.MapConfiguration;
import org.eclipse.jetty.server.Server;
import org.junit.Rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Binder;
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
public class IntegrationTestRuleBuilder
{
    /** All services registered with the builder */
    private final Map<String, ServiceDefinition> installedServices = Maps.newHashMap();
    private final List<MockedService> mockedServices = Lists.newArrayList();
    private final List<Module> modules = Lists.newArrayList();

    private Config baseConfig = Config.getEmptyConfig();
    private Map<String, String> configKeys = Maps.newHashMap();

    public static final IntegrationTestRuleBuilder defaultBuilder()
    {
        return IntegrationTestRuleBuilder.builderWith(serviceLifecycleEnabled, jacksonEnabled, httpClientEnabled);
    }

    public static final IntegrationTestRuleBuilder emptyBuilder()
    {
        return IntegrationTestRuleBuilder.builderWith();
    }

    public static final IntegrationTestRuleBuilder builderWith(final ModuleEnabler ... modules)
    {
        return new IntegrationTestRuleBuilder(modules);
    }

    /**
     * @deprecated Use {@link IntegrationTestRuleBuilder#defaultBuilder()}.
     */
    @Deprecated
    public IntegrationTestRuleBuilder()
    {
        this(serviceLifecycleEnabled,
             jacksonEnabled,
             httpClientEnabled);
    }

    private IntegrationTestRuleBuilder(final ModuleEnabler ... modulesEnables)
    {
        this.baseConfig = Config.getEmptyConfig();
        for (ModuleEnabler enable : modulesEnables) {
            modules.add(enable.getModule());
        }
    }

    public IntegrationTestRuleBuilder clearModules()
    {
        modules.clear();
        return this;
    }

    public IntegrationTestRuleBuilder addModule(final Module module)
    {
        modules.add(module);
        return this;
    }

    /**
     * Register a service to be managed by this integration test rule
     * @param serviceName the name to expose to {@link HttpClient} as
     * @param definition the definition for this service environment
     * @return the builder
     */
    public IntegrationTestRuleBuilder addService(String serviceName, final ServiceDefinition definition)
    {
        installedServices.put(serviceName, definition);
        return this;
    }

    /**
     * Register a mocking service that is designed to be used for testing and mixes in functionality into
     * the environment
     * @param mockedService the service mocking provider
     * @return the builder
     */
    public IntegrationTestRuleBuilder addMockedService(MockedService mockedService)
    {
        mockedServices.add(mockedService);
        return this;
    }

    /**
     * Set the base configuration for the test case.  Mocked services may override some of these values.
     * Does not affect services spun up as part of the test, only the test case itself
     */
    public IntegrationTestRuleBuilder setTestConfig(Config baseConfig)
    {
        this.baseConfig = baseConfig;
        return this;
    }

    /**
     * Set a single configuration option for the test case.
     * Does not affect services spun up as part of the test, only the test case itself
     */
    public IntegrationTestRuleBuilder setTestConfig(String key, String value)
    {
        configKeys.put(key, value);
        return this;
    }

    /**
     * Builds the rule so that JUnit may run it
     * @param testCaseItself pass in the test case object so that Guice may perform field injection
     * @return
     */
    public IntegrationTestRule build(Object testCaseItself)
    {
        return build(testCaseItself, Modules.EMPTY_MODULE);
    }

    /**
     * Builds the rule so that JUnit may run it
     * @param testCaseItself pass in the test case object so that Guice may perform field injection
     * @param testCaseModule any extra modules you would like injected into your test case
     * @return
     */
    public IntegrationTestRule build(final Object testCaseItself, final Module testCaseModule)
    {
        //
        // Override the test case config with the tweaks passed in by the mocked services.
        //
        final Map<String, String> configTweaks = Maps.newHashMap();
        for (MockedService mockedService : mockedServices) {
            configTweaks.putAll(mockedService.getTestCaseConfigTweaks());
        }

        final Config testCaseConfig = Config.getOverriddenConfig(baseConfig, new MapConfiguration(configTweaks), new MapConfiguration(configKeys));
        final Module testCaseServicesModule = new Module() {
            @Override
            public void configure(final Binder binder) {
                for (MockedService mockedService : mockedServices) {
                    binder.install(mockedService.getTestCaseModule());
                }

                for (Module module : modules) {
                    binder.install(module);
                }
            }
        };

        return new IntegrationTestRule(installedServices,
                                       testCaseModule,
                                       testCaseServicesModule,
                                       testCaseConfig,
                                       testCaseItself);
    }
}
