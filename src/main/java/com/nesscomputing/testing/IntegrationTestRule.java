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
import java.util.Map.Entry;

import org.junit.Rule;
import org.junit.rules.ExternalResource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.Stage;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.testing.lessio.AllowAll;


/**
 * A {@link Rule} which on startup creates a testing environment and on shutdown destroys it.
 * You may inspect bindings at runtime with {@link #exposeBinding(String, Key)}
 * @see IntegrationTestRuleBuilder
 */
@AllowAll
public class IntegrationTestRule extends ExternalResource
{
    private final Map<String, Module> services;
    private final Module testCaseModule;
    private final Object testCaseItself;

    private final Map<String, Injector> serviceInjectors = Maps.newHashMap();

    private final List<Lifecycle> lifecycles = Lists.newArrayList();

    IntegrationTestRule(final Map<String, Module> services,
                        final Module testCaseModule,
                        final Object testCaseItself)
    {
        this.services = services;
        this.testCaseModule = testCaseModule;
        this.testCaseItself = testCaseItself;
    }

    /**
     * Setup the various injectors. This is run when the rule triggers so that other rules (such as database or lifecycle) can
     * be ready by the time the injectors are created.
     */
    private void setup()
    {
        // For each registered service, set up an environment.
        for (final Entry<String, Module> service : services.entrySet()) {
            final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                           new GuiceDisableModule(),
                                                           service.getValue());

            serviceInjectors.put(service.getKey(), injector);

            final Binding<Lifecycle> lifecycleBinding = injector.getExistingBinding(Key.get(Lifecycle.class));
            if (lifecycleBinding != null) {
                lifecycles.add(injector.getInstance(Lifecycle.class));
            }
        }

        // Now create a lifecycle for the test case, so that it may get a HttpClient that can
        // interact via srvc:// URIs
        final Injector testInjector = Guice.createInjector(Stage.PRODUCTION,
                                                       new GuiceDisableModule(),
                                                       testCaseModule);

        testInjector.injectMembers(this);
        testInjector.injectMembers(testCaseItself);
    }

    @Override
    protected void before()
    {
        setup();

        // Start up all the lifecycles.
        for (Lifecycle lifecycle : lifecycles) {
            lifecycle.executeTo(LifecycleStage.ANNOUNCE_STAGE);
        }
    }

    @Override
    protected void after()
    {
        // Tear everything down.  Don't bother with error handling, any error here fails the tests.
        for (Lifecycle lifecycle : lifecycles) {
            lifecycle.execute(LifecycleStage.STOP_STAGE);
        }
    }

    /**
     * For a given service, ask the {@link Injector} for an instance and return it.  Lets you "peek inside"
     * and inspect internal state from tests
     * @param serviceName the name passed to {@link IntegrationTestRuleBuilder#addService(String, ServiceDefinition)}
     * @param key the {@link Key} to look up
     * @return the instance
     * @throws IllegalArgumentException if the service name does not exist
     * @throws ProvisionException if anything Guice-related goes wrong, e.g. the binding does not exist
     */
    public <T> T exposeBinding(String serviceName, Key<T> key) throws ProvisionException
    {
        final Injector injector = serviceInjectors.get(serviceName);
        Preconditions.checkState(injector != null, "Injector for service '%s' does not exist!", serviceName);
        return injector.getInstance(key);
    }
}
