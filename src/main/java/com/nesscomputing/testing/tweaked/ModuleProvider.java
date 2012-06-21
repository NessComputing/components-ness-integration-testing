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

import javax.annotation.Nonnull;

import com.google.common.base.Throwables;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.nesscomputing.config.Config;
import com.nesscomputing.logging.Log;

/**
 * Finds the right constructor for a given module and returns an instance either using the c'tor that takes a config object or the empty c'tor.
 *
 * This class could be replaced with a Guice pre-flight injector that would make everyone's head explode. Sounds awesome. :-)
 */
final class ModuleProvider
{
    public static final ModuleProvider EMPTY_MODULE_PROVIDER = ModuleProvider.forModule(Modules.EMPTY_MODULE);

    private static final Log LOG = Log.findLog();

    private String moduleClassName = null;
    private Class<?> moduleClass = null;
    private Module module = null;

    static ModuleProvider forModule(final Object moduleObject)
    {
        return new ModuleProvider(moduleObject);
    }

    private ModuleProvider(@Nonnull final Object moduleObject)
    {
        if (moduleObject instanceof String) {
            moduleClassName = (String) moduleObject;
        }
        else if (moduleObject instanceof Class) {
            moduleClass = (Class<?>) moduleObject;
        }
        else if (moduleObject instanceof Module) {
            module = (Module) moduleObject;
        }
        else {
            throw new IllegalArgumentException("parameter must be a string, class or Module object!");
        }
    }

    public Module getModule(@Nonnull final Config config)
    {
        if (module != null) {
            return module;
        }

        try {
            if (this.moduleClass == null) {
                this.moduleClass = Class.forName(moduleClassName);
            }

            try {
                // try the <module>(Config) c'tor first.
                return Module.class.cast(moduleClass.getConstructor(Config.class).newInstance(config));
            }
            catch (NoSuchMethodException nsme) {
                // now try a no-args c"tor.
                return Module.class.cast(moduleClass.getConstructor().newInstance());
            }
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public Module getSafeModule(@Nonnull final Config config)
    {
        try {
            return getModule(config);
        }
        catch (Exception e) {
            LOG.infoDebug(e, "Could not find '%s'; using empty module!", moduleClass);
            return Modules.EMPTY_MODULE;
        }
    }
}
