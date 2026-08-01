package com.limelight.computers.model;

import java.net.URI;
import java.net.URISyntaxException;

/** Parses one user-entered host and port into a validated manual endpoint. */
public final class ManualHostEndpointParser {
    private ManualHostEndpointParser() {
    }

    public static HostEndpoint parse(
            String rawInput,
            int defaultPort) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return null;
        }
        if (defaultPort <= 0 || defaultPort > 65535) {
            throw new IllegalArgumentException("Invalid default port");
        }

        String input = rawInput.trim();
        URI uri = parseUri("moonlight://" + input);
        if (uri == null) {
            // An unbracketed IPv6 literal is ambiguous to URI. Bracket it and
            // retry while preserving the original accepted input contract.
            uri = parseUri("moonlight://[" + input + "]");
        }
        if (uri == null || !isEndpointOnly(uri)) {
            return null;
        }

        int port = uri.getPort() == -1 ? defaultPort : uri.getPort();
        try {
            return new HostEndpoint(
                    HostEndpoint.Kind.MANUAL,
                    uri.getHost(),
                    port);
        }
        catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static URI parseUri(String value) {
        try {
            URI uri = new URI(value);
            return uri.getHost() == null || uri.getHost().isEmpty()
                    ? null
                    : uri;
        }
        catch (URISyntaxException error) {
            return null;
        }
    }

    private static boolean isEndpointOnly(URI uri) {
        return uri.getHost() != null &&
                !uri.getHost().isEmpty() &&
                uri.getUserInfo() == null &&
                (uri.getPath() == null || uri.getPath().isEmpty()) &&
                uri.getQuery() == null &&
                uri.getFragment() == null;
    }
}
