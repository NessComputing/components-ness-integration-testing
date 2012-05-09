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

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.nesscomputing.config.Config;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.logging.Log;

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
 */
public class ServiceDefinitionBuilder {
    private static final Log LOG = Log.findLog();

    private final List<Module> modules = Lists.newArrayList();
    private final Map<String, String> config = Maps.newHashMap();

    private boolean addJackson = true;
    private boolean addJersey = true;

    private Config baseConfig = Config.getEmptyConfig();

    public ServiceDefinitionBuilder() {
        config.put("galaxy.internal.port.http", "0");
        config.put("ness.httpserver.shutdown-timeout", "0s");
        config.put("org.quartz.threadPool.threadCount", "1");
        config.put("ness.jmx.enabled", "false");
    }

    public ServiceDefinition build() {
        return new ServiceDefinition() {
            @Override
            public Module getModule(final Map<String, String> configTweaks) {
                return new AbstractModule() {
                    @Override
                    protected void configure() {
                        for (Module module : modules) {
                            install (module);
                        }

                        if (addJackson) {
                            install (new NessJacksonModule());
                        }
                        if (addJersey) {
                            install (getJerseyModule(configTweaks));
                        }

                        install (getConfigModule(configTweaks));
                    }
                };
            }
        };
    }

    /**
     * Install a {@link Module} within the service at test startup
     */
    public ServiceDefinitionBuilder addModule(Module module) {
        modules.add(module);
        return this;
    }

    /**
     * Set a {@link Config} key within this service (but not others)
     */
    public ServiceDefinitionBuilder setConfig(String key, String value) {
        config.put(key, value);
        return this;
    }

    /**
     * Set a Config object to be used as the base.  Any overrides are stacked on top via
     * commons-configuration.
     */
    public ServiceDefinitionBuilder setConfig(Config config) {
        this.baseConfig = config;
        return this;
    }

    /**
     * Remove Jackson support
     */
    public ServiceDefinitionBuilder noJackson() {
        addJackson = false;
        return this;
    }

    /**
     * Remove Jersey support
     */
    public ServiceDefinitionBuilder noJersey() {
        addJersey = false;
        return this;
    }

    private Config getConfig(Map<String, String> configTweaks) {
        return Config.getFixedConfig(baseConfig.getConfiguration(), new MapConfiguration(configTweaks), new MapConfiguration(config));
    }

    private Module getConfigModule(final Map<String, String> configTweaks) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                Config builtConfig = getConfig(configTweaks);
                bind (Config.class).toInstance(builtConfig);
            }
        };
    }

    private Module getJerseyModule(Map<String, String> configTweaks) {
        // XXX: must reflectively discover Ness-Jersey, since we cannot add a build time dependency here.
        // Make it optional so that people can test non-Jersey packages
        final Class<?> klass;
        try {
            klass = Class.forName("com.nesscomputing.jersey.BasicJerseyServerModule");
        } catch (ClassNotFoundException e) {
            LOG.infoDebug(e, "Could not find Jersey module, will not be available to integration tests!");
            return Modules.EMPTY_MODULE;
        }

        try {
            return (Module) klass.getConstructor(Config.class).newInstance(getConfig(configTweaks));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
