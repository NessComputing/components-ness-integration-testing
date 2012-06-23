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

import java.util.Collections;
import java.util.Map;

import com.google.inject.Module;
import com.nesscomputing.config.Config;

/**
 * Core class for the integration test pieces. A tweaked module can supply config changes and modules for either a test case, a service that should be spun up and shut down
 * or both. Services should be instantiated using the supplied config objects.
 *
 * This class should either be used with the {@link TweakedModule#forServiceModule(Object)} or {@link TweakedModule#forTestCaseModule(Object)} methods or it should be extended
 * and the various methods are overridden (see {@link TweakedModules} for examples).
 */
public class TweakedModule
{
    /**
     * Returns a TweakedModule wrapper for the passed in object. This object could be:
     * <ul>
     * <li>A string representing the class name of the module. In this case the module is instantiated using the final config
     *     object if the c'tor requires a config object or using the empty c'tor.</li>
     * <li>A class object which must represent a class extending module. Instantiation happens as described above.</li>
     * <li>An instance object implementing the {@link Module}. This object is returned "as is".
     *
     * The module instantiated from this method will be present for the test case only.
     */
    public static final TweakedModule forTestCaseModule(final Object testModuleObject)
    {
        return new TweakedModule(ModuleProvider.forModule(testModuleObject), ModuleProvider.EMPTY_MODULE_PROVIDER);
    }

    /**
     * Returns a TweakedModule wrapper for the passed in object. This object could be:
     * <ul>
     * <li>A string representing the class name of the module. In this case the module is instantiated using the final config
     *     object if the c'tor requires a config object or using the empty c'tor.</li>
     * <li>A class object which must represent a class extending module. Instantiation happens as described above.</li>
     * <li>An instance object implementing the {@link Module}. This object is returned "as is".
     *
     * The module instantiated from this method will be present for services started by the integration test rule.
     */
    public static final TweakedModule forServiceModule(final Object serviceModuleObject)
    {
        return new TweakedModule(ModuleProvider.EMPTY_MODULE_PROVIDER, ModuleProvider.forModule(serviceModuleObject));
    }

    private final ModuleProvider testModuleProvider;
    private final ModuleProvider serviceModuleProvider;

    protected TweakedModule()
    {
        this(ModuleProvider.EMPTY_MODULE_PROVIDER, ModuleProvider.EMPTY_MODULE_PROVIDER);
    }

    private TweakedModule(final ModuleProvider testModuleProvider,
                          final ModuleProvider serviceModuleProvider)
    {
        this.testModuleProvider = testModuleProvider;
        this.serviceModuleProvider = serviceModuleProvider;
    }

    /**
     * Return a set of key-value pairs which are merged into the configuration for services driven by the integration test rule. If
     * an instance of TweakedModule is added as a named service, the tweaks are only present on the named service, otherwise on all
     * services.
     */
    public Map<String, String> getServiceConfigTweaks()
    {
        return Collections.emptyMap();
    }

    /**
     * Return a set of key-value pairs which are merged into the configuration for the test case itself.
     */
    public Map<String, String> getTestCaseConfigTweaks()
    {
        return Collections.emptyMap();
    }

    /**
     * Create an instance of a service based off the passed in Configuration. This service is available for all services driven by the
     * integration rule unless it was added as a named service.
     */
    public Module getServiceModule(final Config config)
    {
        return serviceModuleProvider.getModule(config);
    }

    /**
     * Create an instance of a service based off the passed in Configuration. This service is available for the test case.
     */
    public Module getTestCaseModule(final Config config)
    {
        return testModuleProvider.getModule(config);
    }
}
