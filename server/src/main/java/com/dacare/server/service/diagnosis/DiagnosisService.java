package com.dacare.server.service.diagnosis;

import com.dacare.server.service.ReservationService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class DiagnosisService {

  private static final Logger log = LoggerFactory.getLogger(DiagnosisService.class);

  private final ObjectProvider<VectorStore> vectorStore;
  private final ObjectProvider<ChatClient.Builder> chatClientBuilder;
  private final ReservationService reservations;
  private final double similarityThreshold;
  private final String systemPrompt;

  public DiagnosisService(ObjectProvider<VectorStore> vectorStore,
      ObjectProvider<ChatClient.Builder> chatClientBuilder, ReservationService reservations,
      @Value("${app.diagnosis.similarity-threshold}") double similarityThreshold,
      @Value("classpath:prompts/diagnosis-system.txt") Resource systemPrompt) {
    this.vectorStore = vectorStore;
    this.chatClientBuilder = chatClientBuilder;
    this.reservations = reservations;
    this.similarityThreshold = similarityThreshold;
    this.systemPrompt = read(systemPrompt);
  }

  public DiagnosisResult diagnose(String question, List<ConversationTurn> history, String email) {
    return diagnose(question, history, email, ignored -> {
    });
  }

  public DiagnosisResult diagnose(String question, List<ConversationTurn> history, String email,
      Consumer<DiagnosisTools.ToolProgress> progress) {
    ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
    if (builder == null) {
      throw new DiagnosisUnavailableException();
    }
    DiagnosisTools tools = new DiagnosisTools(vectorStore.getIfAvailable(), reservations, email,
        similarityThreshold, progress);
    try {
      Answer answer = builder.build().prompt().system(systemPrompt)
          .messages(messages(question, history)).tools(tools).call()
          .entity(Answer.class, spec -> spec.useProviderStructuredOutput().validateSchema());
      if (answer == null || answer.answer() == null || answer.answer().isBlank()) {
        throw new DiagnosisUnavailableException();
      }
      return new DiagnosisResult(answer.answer(), tools.cards(), tools.executedTools());
    } catch (RuntimeException exception) {
      Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
      if (Thread.currentThread().isInterrupted() || cause instanceof DiagnosisCancelledException) {
        throw new DiagnosisCancelledException();
      }
      log.warn("진단 요청 실패: {} / {}", cause.getClass().getSimpleName(), cause.getMessage());
      throw new DiagnosisUnavailableException(exception);
    }
  }

  private static List<Message> messages(String question, List<ConversationTurn> history) {
    List<Message> messages = new ArrayList<>();
    if (history != null) {
      history.forEach(turn -> messages.add(turn.role() == Speaker.user
          ? new UserMessage(turn.text()) : new AssistantMessage(turn.text())));
    }
    messages.add(new UserMessage(question));
    return messages;
  }

  private static String read(Resource resource) {
    try {
      return resource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException("상담 시스템 프롬프트를 읽을 수 없습니다.", exception);
    }
  }

  public record Answer(String answer) {

  }

  /**
   * 대화 이력의 발화자. 웹 클라이언트가 보내는 role 값과 같다.
   */
  public enum Speaker {user, assistant}

  public record ConversationTurn(Speaker role, String text) {

  }

  public record DiagnosisResult(String answer, List<DiagnosisCard> cards,
                                List<String> executedTools) {

  }
}
