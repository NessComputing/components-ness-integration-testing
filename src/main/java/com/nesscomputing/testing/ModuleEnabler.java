package com.nesscomputing.testing;

import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Throwables;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.nesscomputing.config.Config;
import com.nesscomputing.httpclient.guice.HttpClientModule;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.lifecycle.ServiceDiscoveryLifecycle;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.logging.Log;

public interface ModuleEnabler
{
    Module getModule();

    public enum ModulesEnable implements ModuleEnabler
    {
        /** Enable standard lifecycle. */
        lifecycleEnabled(new LifecycleModule()),

        /** Enable service discovery lifecycle. */
        serviceLifecycleEnabled(new LifecycleModule(ServiceDiscoveryLifecycle.class)),

        /** Enable the jackson module. */
        jacksonEnabled(new NessJacksonModule()),

        /** Enable an unnabled http client. */
        httpClientEnabled(new HttpClientModule());

        private final Module module;

        ModulesEnable(final Module module)
        {
            this.module = module;
        }

        @Override
        public Module getModule()
        {
            return module;
        }
    }

    public final class CustomModuleEnablers
    {
        private static final Log LOG = Log.findLog();

        private CustomModuleEnablers()
        {
        }

        public static final ModuleEnabler enableJersey(final Config config)
        {
            return enableByName("com.nesscomputing.jersey.BasicJerseyServerModule", config);
        }

        public static final ModuleEnabler enableByName(final String klassName, final Config config)
        {
            final Class<?> klass;
            final AtomicReference<Module> ref = new AtomicReference<Module>(Modules.EMPTY_MODULE);

            try {
                klass = Class.forName(klassName);

                try {
                    ref.set(Module.class.cast(klass.getConstructor(Config.class).newInstance(config)));
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }

            } catch (ClassNotFoundException e) {
                LOG.infoDebug(e, "Could not find '%s'; the module, will not be available to integration tests!", klassName);
            }

            return new ModuleEnabler() {
                @Override
                public Module getModule() {
                    return ref.get();
                }
            };
        }
    }
}
