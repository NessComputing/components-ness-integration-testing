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
