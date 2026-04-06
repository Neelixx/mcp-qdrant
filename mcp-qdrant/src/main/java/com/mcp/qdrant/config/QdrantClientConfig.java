package com.mcp.qdrant.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

@Configuration
public class QdrantClientConfig implements DisposableBean {

    private QdrantClient qdrantClient;

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

        qdrantClient = new QdrantClient(builder.build());
        return qdrantClient;
    }

    @Override
    public void destroy() {
        if (qdrantClient != null) {
            qdrantClient.close();
        }
    }
}
