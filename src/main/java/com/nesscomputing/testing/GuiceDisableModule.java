package com.nesscomputing.testing;

import com.google.inject.Binder;
import com.google.inject.Module;

public class GuiceDisableModule implements Module
{
    @Override
    public void configure(final Binder binder)
    {
        binder.requireExplicitBindings();
        binder.disableCircularProxies();
    }
}
