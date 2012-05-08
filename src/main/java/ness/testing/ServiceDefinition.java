package ness.testing;

import java.util.Map;

import com.google.inject.Module;

/**
 * Encapsulate configuration and Guice bindings for an individual service
 */
public interface ServiceDefinition {
    Module getModule(Map<String, String> configTweaks);
}
