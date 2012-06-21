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

import org.apache.commons.configuration.MapConfiguration;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.testing.tweaked.TweakedModule;

/**
 * Build a ServiceDefinition to use in test cases.
 *
 * By default, this will provide
 * <ul>
 * <li> Configuration to point ness-sql at <code>trumpet_test</code>
 * <li> Jetty configuration to bind to an arbitrary port
 * <li> Jersey
 * <li> Jackson
 * </ul>
 *
 * It tries to emulate {@link ness.jersey.BasicJerseyServerModule} as much as possible, but instead
 * of e.g. exporting JMX it will ignore all JMX requests.
 *
 * Instance methods can modify either the configuration or the provided services
 *
 * @deprecated Use a tweaked module.
 */
@Deprecated
public class ServiceDefinitionBuilder
{
    private final List<Module> modules = Lists.newArrayList();
    private final List<TweakedModule> tweakedModules = Lists.newArrayList();

    private Config baseConfig = Config.getEmptyConfig();
    private Map<String, String> configKeys = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    public static final ServiceDefinitionBuilder builder()
    {
        return ServiceDefinitionBuilder.builderWith();
    }

    public static final ServiceDefinitionBuilder builderWith(final Provider<TweakedModule> ... tweakModuleProviders)
    {
        return new ServiceDefinitionBuilder(tweakModuleProviders);
    }

    /**
     * @deprecated Use {@link ServiceDefinitionBuilder#defaultBuilder()}.
     */
    public ServiceDefinitionBuilder()
    {
    }

    private ServiceDefinitionBuilder(final Provider<TweakedModule> ... tweakedModuleProviders)
    {
        for (Provider<TweakedModule> tweakModuleProvider : tweakedModuleProviders) {
            tweakedModules.add(tweakModuleProvider.get());
        }
    }

    /**
     * Install a {@link Module} within the service at test startup
     */
    public ServiceDefinitionBuilder addModule(final Module module)
    {
        modules.add(module);
        return this;
    }

    public ServiceDefinitionBuilder addTweakedModule(final TweakedModule tweakedModule)
    {
        tweakedModules.add(tweakedModule);
        return this;
    }

    /**
     * Set a {@link Config} key within this service (but not others)
     */
    public ServiceDefinitionBuilder setConfig(String key, String value)
    {
        configKeys.put(key, value);
        return this;
    }

    /**
     * Set a Config object to be used as the base.  Any overrides are stacked on top via
     * commons-configuration.
     */
    public ServiceDefinitionBuilder setConfig(final Config baseConfig)
    {
        this.baseConfig = baseConfig;
        return this;
    }

    private Config getConfig(final Map<String, String> config)
    {
        final Map<String, String> configTweaks = Maps.newHashMap();
        for (TweakedModule tweakedModule : tweakedModules) {
            configTweaks.putAll(tweakedModule.getServiceConfigTweaks());
        }

        return Config.getOverriddenConfig(baseConfig, new MapConfiguration(config), new MapConfiguration(configKeys));
    }

    /**
     * Remove Jackson support.
     *
     * @deprecated Use {@link ServiceDefinitionBuilder#builderWith()} and omit the {TweakEnabler#enableJackson} element.
     */
    public ServiceDefinitionBuilder noJackson()
    {
        throw new UnsupportedOperationException("rewrite the test to use ServiceDefinitionBuilder#builderWith() and omit the TweakEnabler#enableJackson element");
    }

    /**
     * Remove Jersey support.
     *
     * @deprecated Use {@link ServiceDefinitionBuilder#builderWith()} and omit the {TweakEnabler#enableJersey} element.
     */
    public ServiceDefinitionBuilder noJersey()
    {
        throw new UnsupportedOperationException("rewrite the test to use ServiceDefinitionBuilder#builderWith() and omit the TweakEnabler#enableJersey element");
    }

    public ServiceDefinition build() {
        return new ServiceDefinition() {
            @Override
            public Module getModule(final Map<String, String> configTweaks) {
                final Config config = getConfig(configTweaks);

                return new AbstractModule() {
                    @Override
                    protected void configure() {
                        for (TweakedModule tweakedModule : tweakedModules) {
                            install(tweakedModule.getServiceModule(config));
                        }

                        for (Module module : modules) {
                            install (module);
                        }

                        install (new ConfigModule(config));
                    }
                };
            }
        };
    }
}
