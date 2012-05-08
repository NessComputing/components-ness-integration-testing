package ness.testing;

import java.util.Map;

import com.google.inject.Module;

/**
 * Integration testing extension that mocks out a platform service, for example discovery
 */
public interface MockedService {

    Map<String, String> getServiceConfigTweaks(String serviceName);
    Map<String, String> getTestCaseConfigTweaks();

    /**
     * Create a Guice module that represents the "server end" mocked implementation
     * @param serviceName the service name we are creating the module for
     */
    Module getServiceModule(String serviceName);

    /**
     * Create a Guice module that represents the mocked implementation bindings for the
     * test case itself
     */
    Module getTestCaseModule();

}
