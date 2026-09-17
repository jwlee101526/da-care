package com.dacare.server.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ManualIngestionConfig {
    @Bean
    ApplicationRunner ingestManual(ObjectProvider<VectorStore> vectorStoreProvider, com.dacare.server.service.ManualIngestionService ingestionService, @Value("${OPENAI_API_KEY:}") String openAiApiKey) {
        return arguments -> {
            if (openAiApiKey.isBlank()) return;
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore == null) return;
            ingestionService.ingest(vectorStore);
        };
    }
}
