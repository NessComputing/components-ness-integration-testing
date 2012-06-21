package com.nesscomputing.testing;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Disable explicit bindings and circular proxies.
 */
class GuiceDisableModule implements Module
{
    @Override
    public void configure(final Binder binder)
    {
        binder.requireExplicitBindings();
        binder.disableCircularProxies();
    }
}
