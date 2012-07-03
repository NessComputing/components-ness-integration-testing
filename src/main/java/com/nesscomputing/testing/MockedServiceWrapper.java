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

import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.inject.Module;
import com.nesscomputing.config.Config;
import com.nesscomputing.testing.tweaked.TweakedModule;

/**
 * Treat a {@link MockedService} exactly like a {@link TweakedModule}.
 */
@SuppressWarnings("deprecation")
final class MockedServiceWrapper extends TweakedModule
{
    private final MockedService mockedService;

    public MockedServiceWrapper(@Nonnull final MockedService mockedService)
    {
        Preconditions.checkArgument(mockedService != null, "the service can not be null");
        this.mockedService = mockedService;
    }

    public Map<String, String> getServiceConfigTweaks(final String serviceName)
    {
        return mockedService.getServiceConfigTweaks(serviceName);
    }

    public Map<String, String> getTestCaseConfigTweaks()
    {
        return mockedService.getTestCaseConfigTweaks();
    }

    public Module getServiceModule(final Config config, final String serviceName)
    {
        return mockedService.getServiceModule(serviceName);
    }

    public Module getTestCaseModule(final Config config)
    {
        return mockedService.getTestCaseModule();
    }
}
