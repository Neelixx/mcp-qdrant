package com.mcp.qdrant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * A request with no "id" must carry JSON null in the envelope, and numeric
 * parameters must clamp to their documented range instead of wrapping.
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

    @Test
    void testMissingIdYieldsJsonNull() {
        McpHttpController controller = new McpHttpController(new ObjectMapper(), 9091);
        java.lang.Object boxedMethod = 123;
        var request = Map.of("method", boxedMethod);

        var response = controller.handleMcpRequest(request);
        assertEquals("200 OK", response.getStatusCode().toString());
        var body = response.getBody();
        assertEquals("2.0", body.get("jsonrpc"));
        assertNull(body.get("id"));
        assertEquals(-32600, ((Map<String, Object>) body.get("error")).get("code"));
    }

    @Test
    void testIntegerParametersClampInsteadOfWrapping() {
        java.lang.Object oversized = 3000000000L;
        assertEquals(100, McpHttpController.toInt(oversized, 5, 1, 100));
        assertEquals(5, McpHttpController.toInt(null, 5, 1, 100));
        assertEquals(5, McpHttpController.toInt("x", 5, 1, 100));
    }
}
