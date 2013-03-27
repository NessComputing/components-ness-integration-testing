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
