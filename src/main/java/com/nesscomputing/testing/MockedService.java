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

import com.google.inject.Module;
import com.nesscomputing.testing.tweaked.TweakedModule;

/**
 * Integration testing extension that mocks out a platform service, for example discovery.
 *
 * @deprecated Does not allow passing of configuration objects to created services. Use {@link TweakedModule} instead.
 */
@Deprecated
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
