package com.nesscomputing.testing;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.collect.Iterables;
import com.google.inject.Injector;

import com.nesscomputing.httpserver.HttpConnector;
import com.nesscomputing.httpserver.HttpServer;

/**
 * Provides linking to ness-httpserver component to
 * retrieve the actual host / port used for a tested service.
 * Written as a separate class so that ness-httpserver can be optional
 * in case that this is not used at all.
 */
class NessHttpserverHelper
{

    private NessHttpserverHelper() { }

    static URI getServiceUri(Injector injector)
    {
        HttpServer server = injector.getInstance(HttpServer.class);
        HttpConnector connector = Iterables.getOnlyElement(server.getConnectors().values());
        try {
            return new URI(connector.getScheme(), null, connector.getAddress(), connector.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
