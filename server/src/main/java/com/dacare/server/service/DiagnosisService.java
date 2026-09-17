package com.dacare.server.service;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DiagnosisService {
    private final ObjectProvider<VectorStore> vectorStore; private final ObjectProvider<ChatClient.Builder> chatClientBuilder;
    public DiagnosisService(ObjectProvider<VectorStore> vectorStore, ObjectProvider<ChatClient.Builder> chatClientBuilder) { this.vectorStore = vectorStore; this.chatClientBuilder = chatClientBuilder; }
    public DiagnosisResult diagnose(String question) {
        VectorStore store = vectorStore.getIfAvailable(); ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (store == null || builder == null) return DiagnosisResult.fallback();
        List<Document> documents = store.similaritySearch(question);
        if (documents == null || documents.isEmpty()) return DiagnosisResult.fallback();
        String context = documents.stream().limit(3).map(Document::getText).reduce("", (left, right) -> left + "\n\n" + right);
        try {
            String answer = builder.build().prompt().system("You are a device service assistant. Answer only from the supplied manual context. If the context does not answer the question, say that an engineer visit is needed.").user(user -> user.text("Manual context:\n{context}\n\nQuestion:\n{question}").param("context", context).param("question", question)).call().content();
            return new DiagnosisResult(answer, documents.stream().limit(3).map(Document::getId).toList(), false);
        } catch (RuntimeException exception) { return DiagnosisResult.fallback(); }
    }
    public record DiagnosisResult(String answer, List<String> sources, boolean reservationRecommended) { static DiagnosisResult fallback() { return new DiagnosisResult("매뉴얼 근거를 찾지 못했습니다. 안전을 위해 사용을 중지하고 출장 AS 예약을 신청해 주세요.", List.of(), true); } }
}
