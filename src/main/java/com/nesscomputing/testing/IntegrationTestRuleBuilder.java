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

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.configuration.MapConfiguration;
import org.junit.Rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.testing.tweaked.TweakedModule;
import com.nesscomputing.testing.tweaked.TweakedModules;
import com.nesscomputing.testing.tweaked.TweakedModules.TweakEnabler;

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
 * <li> a Jetty Server bound on an unused localhost port
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
    @SuppressWarnings("deprecation")
    private final Map<String, ServiceDefinition> serviceDefinitions = Maps.newHashMap();
    private final Map<String, TweakedModule> serviceTweakedModules = Maps.newHashMap();
    private final Map<String, Config> serviceConfigs = Maps.newHashMap();
    private final List<TweakedModule> tweakedModules = Lists.newArrayList();

    private Config baseConfig = Config.getEmptyConfig();
    private Map<String, String> configKeys = Maps.newHashMap();

    /**
     * Return a default {@link IntegrationTestRuleBuilder}. This builder enables everything listed in {@link TweakedModules.TweakEnabler}.
     */
    @SuppressWarnings("unchecked")
    public static final IntegrationTestRuleBuilder defaultBuilder()
    {
        return IntegrationTestRuleBuilder.builderWith(TweakEnabler.lifecycleEnabled,
                                                      TweakEnabler.jacksonEnabled,
                                                      TweakEnabler.httpClientEnabled,
                                                      TweakEnabler.jerseyEnabled,
                                                      TweakEnabler.galaxyEnabled,
                                                      TweakEnabler.httpServerEnabled,
                                                      TweakEnabler.jmxEnabled,
                                                      TweakEnabler.metricsEnabled,
                                                      TweakEnabler.serviceTweaks);
    }

    /**
     * Returns an {@link IntegrationTestRuleBuilder} that has no modules enabled by default. New modules can be added using {@link IntegrationTestRuleBuilder#addService(String, TweakedModule)}
     * and the static getters from {@link TweakedModules}.
     */
    @SuppressWarnings("unchecked")
    public static final IntegrationTestRuleBuilder emptyBuilder()
    {
        return IntegrationTestRuleBuilder.builderWith();
    }

    /**
     * Returns a new {@link IntegrationTestRuleBuilder} with some services enabled. The services are a list of elements implementing {@link Provider<TweakedModule>}, e.g. the annotations
     * in {@link TweakedModules.TweakEnabler}.
     */
    public static final IntegrationTestRuleBuilder builderWith(final Provider<TweakedModule> ... tweakModuleProviders)
    {
        return new IntegrationTestRuleBuilder(tweakModuleProviders);
    }

    /**
     * @deprecated Use {@link IntegrationTestRuleBuilder#defaultBuilder()}.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public IntegrationTestRuleBuilder()
    {
        this(TweakEnabler.lifecycleEnabled,
             TweakEnabler.jacksonEnabled,
             TweakEnabler.httpClientEnabled,
             TweakEnabler.jerseyEnabled,
             TweakEnabler.galaxyEnabled,
             TweakEnabler.httpServerEnabled,
             TweakEnabler.jmxEnabled,
             TweakEnabler.metricsEnabled,
             TweakEnabler.serviceTweaks);
    }

    private IntegrationTestRuleBuilder(final Provider<TweakedModule> ... tweakedModuleProviders)
    {
        for (Provider<TweakedModule> tweakModuleProvider : tweakedModuleProviders) {
            tweakedModules.add(tweakModuleProvider.get());
        }
    }

    /**
     * @deprecated Use {@link IntegrationTestRuleBuilder#addTestCaseModule(Object)} to add a module by name, class or instance.
     */
    @Deprecated
    public IntegrationTestRuleBuilder addModule(final Module module)
    {
        return addTestCaseModules(module);
    }

    /**
     * Add a module that is available to all services configurated in the integration test. Used e.g. to
     * configure databases or other general facilities.
     *
     * @see TweakedModule#forServiceModule(Object).
     */
    public IntegrationTestRuleBuilder addServiceModules(final Object ... serviceModules)
    {
        for (final Object serviceModule : serviceModules) {
            tweakedModules.add(TweakedModule.forServiceModule(serviceModule));
        }
        return this;
    }

    /**
     * Add a module that is available to the test case code.
     *
     * @see TweakedModule#forTestCaseModule(Object).
     */
    public IntegrationTestRuleBuilder addTestCaseModules(final Object ... testCaseModules)
    {
        for (final Object testCaseModule: testCaseModules) {
            tweakedModules.add(TweakedModule.forTestCaseModule(testCaseModule));
        }
        return this;
    }


    /**
     * Add a new {@link TweakedModule} to the builder. A tweaked module can supply config changes and/or a module for the test case or the services controlled
     * by the integration test rule.
     *
     * This is a generalized version of {@link IntegrationTestRuleBuilder#addServiceModule(Object)} and {@link IntegrationTestRuleBuilder#addTestCaseModule(Object)}.
     */
    public IntegrationTestRuleBuilder addTweakedModules(final TweakedModule ... tweakedModules)
    {
        for (final TweakedModule tweakedModule : tweakedModules) {
            this.tweakedModules.add(tweakedModule);
        }
        return this;
    }

    /**
     * Register a service to be managed by this integration test rule
     * @param serviceName the name to expose to HttpClient as
     * @param definition the definition for this service environment
     * @return the builder
     * @deprecated Use {@link IntegrationTestRuleBuilder#addService(String, TweakedModule).
     */
    @Deprecated
    public IntegrationTestRuleBuilder addService(String serviceName, final ServiceDefinition definition)
    {
        serviceDefinitions.put(serviceName, definition);
        return this;
    }

    /**
     * Register a service to be managed by this integration test rule
     * @param serviceName the name to expose to HttpClient as
     * @param definition the definition for this service environment.
     * @return the builder
     */
    public IntegrationTestRuleBuilder addService(String serviceName, final TweakedModule tweakedModule)
    {
        serviceTweakedModules.put(serviceName, tweakedModule);
        return this;
    }

    /**
     * Register a service to be managed by this integration test rule. This service uses a diffent configuration than the
     * test case.
     * @param serviceName the name to expose to HttpClient as.
     * @param serviceConfig the configuration to use for this service.
     * @param definition the definition for this service environment.
     * @return the builder
     */
    public IntegrationTestRuleBuilder addService(String serviceName, final Config serviceConfig, final TweakedModule tweakedModule)
    {
        serviceTweakedModules.put(serviceName, tweakedModule);
        serviceConfigs.put(serviceName, serviceConfig);
        return this;
    }

    /**
     * Register a mocking service that is designed to be used for testing and mixes in functionality into
     * the environment
     * @param mockedService the service mocking provider.
     * @return the builder
     * @deprecated Use {@link IntegrationTestRuleBuilder#addTweakedModule(TweakedModule)}.
     */
    @Deprecated
    public IntegrationTestRuleBuilder addMockedService(MockedService mockedService)
    {
        addTweakedModules(new MockedServiceWrapper(mockedService));
        return this;
    }

    /**
     * Set the base configuration for the test case.  Mocked services may override some of these values.
     */
    public IntegrationTestRuleBuilder setTestConfig(final Config baseConfig)
    {
        this.baseConfig = baseConfig;
        return this;
    }

    /**
     * Set a single configuration option for the test case.
     */
    public IntegrationTestRuleBuilder setTestConfig(final String key, final String value)
    {
        configKeys.put(key, value);
        return this;
    }

    /**
     * Builds the rule so that JUnit may run it
     * @param testCaseItself pass in the test case object so that Guice may perform field injection
     * @return
     */
    public IntegrationTestRule build(final Object testCaseItself)
    {
        return build(testCaseItself, Modules.EMPTY_MODULE);
    }

    /**
     * Builds the rule so that JUnit may run it.
     *
     * If the testcase module requires the configuration object use {@link IntegrationTestRuleBuilder#build(Object)} and
     * install the module with
     *
     * <pre>
        addTweakedModule(TweakedModule.forTestModule(&lt;name or class or instance of test module&gt;));
        </pre>
     *
     * @param testCaseItself pass in the test case object so that Guice may perform field injection
     * @param testCaseModule any extra modules you would like injected into your test case
     * @return
     *
     */
    public IntegrationTestRule build(final Object testCaseItself, @Nonnull final Module testCaseModule)
    {
        //
        // Override the test case config with the tweaks exposed by the tweaked services.
        //
        final Map<String, String> testCaseConfigTweaks = Maps.newHashMap();
        final Map<String, String> serviceConfigTweaks = Maps.newHashMap();

        for (final TweakedModule tweakedModule : tweakedModules) {
            testCaseConfigTweaks.putAll(tweakedModule.getTestCaseConfigTweaks());
            serviceConfigTweaks.putAll(tweakedModule.getServiceConfigTweaks());
        }

        //
        // Build the test case module.
        //
        final Config testCaseConfig = Config.getOverriddenConfig(baseConfig, new MapConfiguration(testCaseConfigTweaks), new MapConfiguration(configKeys));
        final Module module = new Module() {
            @Override
            public void configure(final Binder binder) {
                for (TweakedModule tweakedModule : tweakedModules) {
                    binder.install(tweakedModule.getTestCaseModule(testCaseConfig));
                }

                binder.install(new ConfigModule(testCaseConfig));
                binder.install(testCaseModule);
            }
        };

        //
        // Build the service modules.
        //
        final Map<String, Module> serviceModules = Maps.newHashMap();
        addServiceDefinitions(serviceDefinitions, serviceConfigTweaks, serviceModules);
        addServiceModules(serviceTweakedModules, serviceConfigTweaks, serviceModules);

        return new IntegrationTestRule(serviceModules,
                                       module,
                                       testCaseItself);
    }

    @SuppressWarnings("deprecation")
    private void addServiceDefinitions(final Map<String, ServiceDefinition> serviceDefinitions,
                                       final Map<String, String> serviceConfigTweaks,
                                       final Map<String, Module> serviceModules)
    {
        for (final Map.Entry<String, ServiceDefinition> entry : serviceDefinitions.entrySet()) {
            // Reality is that this is the service configuration minus what a service definition might add as local tweaks
            // for a named service. So the instance of the named service might run with a slightly different config
            // than all the services that are also in the injector, because ServiceDefinition has no API to expose these
            // tweaks to the other services.
            //
            // That is an actual problem and the reason why ServiceDefinition does not work.
            //
            // For services defined through a ServiceDefinition, always use the baseConfig, because there is no way to register
            // such a service with its specific configuration. The usual way around this is that the service definition was supplied
            // a service specific configuration through other means. So the service runs a very different configuration than all its
            // additional services around it.
            //
            // That is an actual problem and another reason why ServiceDefinition does not work.
            //
            final Config serviceConfig = Config.getOverriddenConfig(baseConfig, new MapConfiguration(serviceConfigTweaks));

            final Module serviceModule = new Module() {
                @Override
                public void configure(final Binder binder) {
                    // This allows us to later ask an injector what it is named.
                    binder.bindConstant().annotatedWith(Names.named("SERVICE")).to(entry.getKey());

                    for (TweakedModule tweakedModule : tweakedModules) {
                        binder.install(tweakedModule.getServiceModule(serviceConfig));
                    }

                    binder.install(new ConfigModule(serviceConfig));

                    binder.install(entry.getValue().getModule(serviceConfigTweaks));
                }
            };

            serviceModules.put(entry.getKey(), serviceModule);
        }
    }

    private void addServiceModules(final Map<String, TweakedModule> serviceDefinitions,
                                   final Map<String, String> serviceConfigTweaks,
                                   final Map<String, Module> serviceModules)
    {
        for (final Map.Entry<String, TweakedModule> entry : serviceDefinitions.entrySet()) {

            // A tweaked service can be registered with its own configuration. Use that as the base for all the service
            // tweaks if present, otherwise use the base configuration.
            final Config serviceBaseConfig = serviceConfigs.containsKey(entry.getKey()) ? serviceConfigs.get(entry.getKey()) : baseConfig;

            final Config serviceConfig = Config.getOverriddenConfig(serviceBaseConfig,
                                                                    new MapConfiguration(serviceConfigTweaks),
                                                                    new MapConfiguration(entry.getValue().getServiceConfigTweaks()));

            final Module serviceModule = new Module() {
                @Override
                public void configure(final Binder binder) {
                    // This allows us to later ask an injector what it is named.
                    binder.bindConstant().annotatedWith(Names.named("SERVICE")).to(entry.getKey());

                    for (TweakedModule tweakedModule : tweakedModules) {
                        binder.install(tweakedModule.getServiceModule(serviceConfig));
                    }

                    binder.install(new ConfigModule(serviceConfig));

                    binder.install(entry.getValue().getServiceModule(serviceConfig));
                }
            };

            serviceModules.put(entry.getKey(), serviceModule);
        }
    }
}
