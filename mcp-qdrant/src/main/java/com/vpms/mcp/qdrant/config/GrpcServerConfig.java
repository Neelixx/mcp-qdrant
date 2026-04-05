package com.vpms.mcp.qdrant.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import com.vpms.mcp.qdrant.service.GrpcMcpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.Executors;

@Configuration
public class GrpcServerConfig {

    @Bean
    public Server grpcServer(GrpcMcpService grpcMcpService) throws IOException {
        return ServerBuilder.forPort(9090)
                .addService(grpcMcpService)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build()
                .start();
    }
}
