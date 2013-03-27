package com.nesscomputing.testing.tweaked;

import java.lang.reflect.Array;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import com.nesscomputing.logging.Log;

public class UnnamedHttpClientModule extends AbstractModule
{
    private static final Log LOG = Log.findLog();
    private static final String TEST_NAME = "__test";

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void configure()
    {
        try {
            Class<?> groupClass = Class.forName("com.nesscomputing.httpclient.HttpClientObserverGroup");
            Object[] observerGroups = (Object[]) Array.newInstance(groupClass, 1);
            Class<?> groupsType = observerGroups.getClass();

            observerGroups[0] = groupClass.getField("PLATFORM_INTERNAL").get(null);

            install ((Module) Class.forName("com.nesscomputing.httpclient.guice.HttpClientModule").getConstructor(String.class, groupsType).newInstance(TEST_NAME, observerGroups));

            Class httpClientClass = Class.forName("com.nesscomputing.httpclient.HttpClient");
            bind (httpClientClass).to(Key.get(httpClientClass, Names.named(TEST_NAME)));
        } catch (Exception e) {
            LOG.infoDebug(e, "Could not create httpclient module");
        }
    }
}
