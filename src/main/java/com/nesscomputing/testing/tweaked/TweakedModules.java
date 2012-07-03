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
package com.nesscomputing.testing.tweaked;

import java.util.Map;

import javax.management.MBeanServer;

import org.weakref.jmx.testing.TestingMBeanServer;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.nesscomputing.config.Config;
import com.nesscomputing.lifecycle.ServiceDiscoveryLifecycle;
import com.nesscomputing.lifecycle.guice.LifecycleModule;

/**
 * All tweaked modules available by default.
 */
public final class TweakedModules
{
    public enum TweakEnabler implements Provider<TweakedModule>
    {
        /** Enable service discovery lifecycle. Services only.*/
        lifecycleEnabled(TweakedModules.getLifecycleModule()),

        /** Enable the jackson module. Testcase and services. */
        jacksonEnabled(TweakedModules.getJacksonModule()),

        /** Enable the jersey module. Testcase and services. */
        jerseyEnabled(TweakedModules.getJerseyModule()),

        /** Enable an unnamed http client. Testcase only. */
        httpClientEnabled(TweakedModules.getHttpClientModule()),

        /** Enable the http service. Services only. */
        httpServerEnabled(TweakedModules.getHttpServerModule()),

        /** Enable galaxy. Testcase and services. */
        galaxyEnabled(TweakedModules.getGalaxyModule()),

        /** Metrics. Services only. */
        metricsEnabled(TweakedModules.getMetricsModule()),

        /** JMX dummy. Testcase and services. */
        jmxEnabled(TweakedModules.getJmxModule()),

        /** default service tweaks.Testcase and services. */
        serviceTweaks(TweakedModules.getServiceTweaks());

        private final TweakedModule module;

        TweakEnabler(final TweakedModule module)
        {
            this.module = module;
        }

        @Override
        public TweakedModule get()
        {
            return module;
        }
    }

    private static final ModuleProvider JERSEY_PROVIDER = ModuleProvider.forModule("com.nesscomputing.jersey.BasicJerseyServerModule");
    private static final ModuleProvider JACKSON_PROVIDER = ModuleProvider.forModule("com.nesscomputing.jackson.NessJacksonModule");
    private static final ModuleProvider HTTPCLIENT_PROVIDER = ModuleProvider.forModule("com.nesscomputing.httpclient.guice.HttpClientModule");
    private static final ModuleProvider HTTPSERVER_PROVIDER = ModuleProvider.forModule("com.nesscomputing.httpserver.HttpServerModule");
    private static final ModuleProvider GALAXY_PROVIDER = ModuleProvider.forModule("com.nesscomputing.galaxy.GalaxyConfigModule");
    private static final ModuleProvider METRICS_PROVIDER = ModuleProvider.forModule("com.yammer.metrics.guice.InstrumentationModule");

    private TweakedModules()
    {
    }

    public static TweakedModule getLifecycleModule()
    {
        return new TweakedModule() {
            @Override
            public Module getServiceModule(final Config config) {
                return new LifecycleModule(ServiceDiscoveryLifecycle.class);
            }
        };
    }

    public static TweakedModule getJerseyModule()
    {
        return new TweakedModule() {
            @Override
            public Map<String, String> getTestCaseConfigTweaks() {
                return getServiceConfigTweaks();
            }

            @Override
            public Map<String, String> getServiceConfigTweaks() {
                return ImmutableMap.of("ness.jmx.enabled", "false");
            }

            @Override
            public Module getTestCaseModule(final Config config) {
                return getServiceModule(config);
            }

            @Override
            public Module getServiceModule(final Config config) {
                return JERSEY_PROVIDER.getSafeModule(config);
            }
};
    }

    public static TweakedModule getJacksonModule()
    {
        return new TweakedModule() {
            @Override
            public Module getTestCaseModule(final Config config) {
                return getServiceModule(config);
            }

            @Override
            public Module getServiceModule(final Config config) {
                return JACKSON_PROVIDER.getSafeModule(config);
            }
        };
    }

    public static TweakedModule getHttpClientModule()
    {
        return new TweakedModule() {
            @Override
            public Module getTestCaseModule(final Config config) {
                return HTTPCLIENT_PROVIDER.getSafeModule(config);
            }
        };
    }

    public static TweakedModule getHttpServerModule()
    {
        return new TweakedModule() {
            @Override
            public Map<String, String> getServiceConfigTweaks() {
                return ImmutableMap.of("ness.httpserver.shutdown-timeout", "0s");
            }

            @Override
            public Module getServiceModule(final Config config) {
                return HTTPSERVER_PROVIDER.getSafeModule(config);
            }
        };
    }

    public static TweakedModule getGalaxyModule()
    {
        return new TweakedModule() {
            @Override
            public Map<String, String> getTestCaseConfigTweaks() {
                return getServiceConfigTweaks();
            }

            @Override
            public Module getTestCaseModule(final Config config) {
                return getServiceModule(config);
            }

            @Override
            public Map<String, String> getServiceConfigTweaks() {
                return ImmutableMap.of("galaxy.internal.port.http", "0");
            }

            @Override
            public Module getServiceModule(final Config config) {
                return GALAXY_PROVIDER.getSafeModule(config);
            }
        };
    }

    public static TweakedModule getMetricsModule()
    {
        return new TweakedModule() {
            @Override
            public Module getServiceModule(final Config config) {
                return METRICS_PROVIDER.getSafeModule(config);
            }
        };
    }

    public static TweakedModule getJmxModule()
    {
        return new TweakedModule() {
            @Override
            public Module getTestCaseModule(final Config config) {
                return getServiceModule(config);
            }

            @Override
            public Module getServiceModule(final Config config) {
                return new Module() {
                    @Override
                    public void configure(final Binder binder) {
                        binder.bind (MBeanServer.class).to(TestingMBeanServer.class).in(Scopes.SINGLETON);
                    }
                };
            }
        };
    }

    public static TweakedModule getServiceTweaks()
    {
        return new TweakedModule() {
            @Override
            public Map<String, String> getTestCaseConfigTweaks() {
                return getServiceConfigTweaks();
            }

            @Override
            public Map<String, String> getServiceConfigTweaks() {
                return ImmutableMap.of("org.quartz.threadPool.threadCount", "1");
            }
        };
    }
}
