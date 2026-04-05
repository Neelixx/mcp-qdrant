package com.mcp.qdrant.config;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mcp.qdrant.service.GrpcMcpService;

import io.grpc.Server;
import io.grpc.ServerBuilder;

@Configuration
public class GrpcServerConfig {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerConfig.class);

    @Bean
    public Server grpcServer(GrpcMcpService grpcMcpService) throws IOException {
        Executor executor = createExecutor();
        return ServerBuilder.forPort(9090)
                .addService(grpcMcpService)
                .executor(executor)
                .build()
                .start();
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
