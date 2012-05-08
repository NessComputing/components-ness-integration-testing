package ness.testing;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.MBeanServer;

import org.apache.commons.configuration.MapConfiguration;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.ConfigModule;
import com.nesscomputing.galaxy.GalaxyConfig;
import com.nesscomputing.httpclient.guice.HttpClientModule;
import com.nesscomputing.httpserver.HttpServerModule;
import com.nesscomputing.jackson.NessJacksonModule;
import com.nesscomputing.lifecycle.Lifecycle;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.ServiceDiscoveryLifecycle;
import com.nesscomputing.lifecycle.guice.LifecycleModule;
import com.nesscomputing.testing.lessio.AllowDNSResolution;
import com.nesscomputing.testing.lessio.AllowNetworkAccess;
import com.nesscomputing.testing.lessio.AllowNetworkListen;

/**
 * A {@link Rule} which on startup creates a testing environment and on shutdown destroys it.
 * You may inspect bindings at runtime with {@link #exposeBinding(String, Key)}
 * @see IntegrationTestRuleBuilder
 */
@AllowNetworkListen(ports= {0})
@AllowNetworkAccess(endpoints= {"127.0.0.1:*"})
@AllowDNSResolution
public class IntegrationTestRule extends ExternalResource {

    private final Map<String, ServiceDefinition> services;
    private final List<MockedService> mockedServices;
    private final Module testCaseModule;
    private final Object testCaseItself;
    private final Config testCaseConfig;
    private final List<Injector> injectors = new CopyOnWriteArrayList<Injector>();
    private Lifecycle testCaseLifecycle;

    IntegrationTestRule(Map<String, ServiceDefinition> services, List<MockedService> mockedServices, Module testCaseModule, Object testCaseItself, Config testCaseConfig) {
        this.testCaseConfig = testCaseConfig;
        this.mockedServices = ImmutableList.copyOf(mockedServices);
        this.services = ImmutableMap.copyOf(services);
        this.testCaseModule = testCaseModule;
        this.testCaseItself = testCaseItself;
    }

    @Override
    protected void before() throws Throwable {
        // For each registered service, set up an environment
        for (final Entry<String, ServiceDefinition> service : services.entrySet()) {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    // Slather special sauce everywhere
                    binder().requireExplicitBindings();
                    binder().disableCircularProxies();

                    // This allows us to later ask an injector what it is named
                    bindConstant().annotatedWith(Names.named("SERVICE")).to(service.getKey());

                    // Serve up some Jetty
                    install (new HttpServerModule(Config.getEmptyConfig()));

                    // Pretend to provide a Galaxy environment.  This will be configured to an arbitrary port.
                    bind (GalaxyConfig.class);

                    Map<String, String> configTweaks = Maps.newHashMap();

                    for (MockedService mockedService : mockedServices) {
                        install (mockedService.getServiceModule(service.getKey()));
                        configTweaks.putAll(mockedService.getServiceConfigTweaks(service.getKey()));
                    }

                    install (new LifecycleModule(ServiceDiscoveryLifecycle.class));
                    install (service.getValue().getModule(configTweaks));

                    MBeanServer mbs = EasyMock.createNiceMock(MBeanServer.class);
                    EasyMock.replay(mbs);
                    bind (MBeanServer.class).toInstance(mbs);
                }
            });
            injectors.add(injector);
        }

        // Start up all the lifecycles
        for (Injector i : injectors) {
            i.getInstance(Lifecycle.class).executeTo(LifecycleStage.ANNOUNCE_STAGE);
        }

        // Now create a lifecycle for the test case, so that it may get a HttpClient that can
        // interact via srvc:// URIs
        testCaseLifecycle = Guice.createInjector(new AbstractModule() {
           @Override
            protected void configure() {
               binder().requireExplicitBindings();
               binder().disableCircularProxies();

               Map<String, String> configTweaks = Maps.newHashMap();

               for (MockedService mockedService : mockedServices) {
                   install (mockedService.getTestCaseModule());
                   configTweaks.putAll(mockedService.getTestCaseConfigTweaks());
               }

               install (new ConfigModule(Config.getOverriddenConfig(testCaseConfig, new MapConfiguration(configTweaks))));
               install (testCaseModule);

               install (new LifecycleModule(ServiceDiscoveryLifecycle.class));
               install (new NessJacksonModule());
               install (new HttpClientModule());

               requestInjection(testCaseItself);
            }
        }).getInstance(Lifecycle.class);

        testCaseLifecycle.executeTo(LifecycleStage.ANNOUNCE_STAGE);
    }

    @Override
    protected void after() {
        // Tear everything down.  Don't bother with error handling, any error here fails the tests.
        testCaseLifecycle.execute(LifecycleStage.STOP_STAGE);
        for (Injector i : injectors) {
            i.getInstance(Lifecycle.class).execute(LifecycleStage.STOP_STAGE);
        }
    }

    /**
     * For a given service, ask the {@link Injector} for an instance and return it.  Lets you "peek inside"
     * and inspect internal state from tests
     * @param serviceName the name passed to {@link IntegrationTestRuleBuilder#addService(String, ServiceDefinition)}
     * @param key the {@link Key} to look up
     * @return the instance
     * @throws IllegalArgumentException if the service name does not exist
     * @throws ProvisionException if anything Guice-related goes wrong, e.g. the binding does not exist
     */
    public <T> T exposeBinding(String serviceName, Key<T> key) throws ProvisionException {
        for (Injector injector : injectors) {
            if (serviceName.equals(injector.getInstance(Key.get(String.class, Names.named("SERVICE"))))) {
                return injector.getInstance(key);
            }
        }

        throw new IllegalArgumentException();
    }
}