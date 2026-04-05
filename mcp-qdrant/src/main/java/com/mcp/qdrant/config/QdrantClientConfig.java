package com.mcp.qdrant.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantClientConfig {

    @Bean
    public QdrantClient qdrantClient(QdrantProperties properties) {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                properties.getHost(),
                properties.getPort(),
                properties.isUseTls()
        );

        if (properties.getApiKey() != null && !properties.getApiKey().isEmpty()) {
            builder.withApiKey(properties.getApiKey());
        }

        return new QdrantClient(builder.build());
    }
}
