package com.mcp.qdrant.config;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mcp.qdrant.service.GrpcMcpService;

import io.grpc.Server;
import io.grpc.ServerBuilder;

@Configuration
public class GrpcServerConfig implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerConfig.class);
    private Server grpcServer;

    @Bean
    public Server grpcServer(GrpcMcpService grpcMcpService) throws IOException {
        Executor executor = createExecutor();
        grpcServer = ServerBuilder.forPort(9090)
                .addService(grpcMcpService)
                .executor(executor)
                .build()
                .start();
        return grpcServer;
    }

    @Override
    public void destroy() {
        if (grpcServer != null) {
            log.info("Shutting down gRPC server...");
            grpcServer.shutdown();
            try {
                if (!grpcServer.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("gRPC server did not terminate in 10 seconds, forcing shutdown");
                    grpcServer.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for gRPC server shutdown", e);
                grpcServer.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("gRPC server shutdown complete");
        }
    }

    private Executor createExecutor() {
        int javaVersion = Runtime.version().feature();
        if (javaVersion >= 21) {
            log.info("Using virtual thread executor (Java {})", javaVersion);
            return Executors.newVirtualThreadPerTaskExecutor();
        } else {
            log.info("Using cached thread pool (Java {})", javaVersion);
            return Executors.newCachedThreadPool();
        }
    }
}
