package com.mcp.qdrant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.mcp.qdrant.controller.McpHttpController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.Object;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;

/**
 * Regression tests for the JSON-RPC bridge: a request whose "method" or
 * "params" have the wrong JSON type must produce a JSON-RPC error envelope
 * (HTTP 200), not an uncaught ClassCastException escaping to an HTTP 500.
 */
public class HttpBridgeUnitTest {

    @Test
    void testNonStringMethodReturnsJsonRpcError() {
        McpHttpController controller = new McpHttpController(new ObjectMapper(), 9091);
        java.lang.Object boxedMethod = 123;
        var request = Map.of("method", boxedMethod);

        var response = controller.handleMcpRequest(request);
        assertEquals("200 OK", response.getStatusCode().toString());
        var body = response.getBody();
        assertEquals("2.0", body.get("jsonrpc"));
        assertEquals(-32600, ((Map<String, Object>) body.get("error")).get("code"));
    }

    @Test
    void testNonObjectParamsReturnsJsonRpcError() {
        McpHttpController controller = new McpHttpController(new ObjectMapper(), 9091);
        java.lang.Object boxedParams = List.of(1, 2);
        var request = Map.of("method", "hybridSearch", "params", boxedParams);

        var response = controller.handleMcpRequest(request);
        assertEquals("200 OK", response.getStatusCode().toString());
        var body = response.getBody();
        assertEquals("2.0", body.get("jsonrpc"));
        assertEquals(-32600, ((Map<String, Object>) body.get("error")).get("code"));
    }
}
