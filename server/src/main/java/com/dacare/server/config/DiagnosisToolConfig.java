package com.dacare.server.config;

import com.dacare.server.service.DiagnosisService;
import java.util.List;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiagnosisToolConfig {

  /**
   * 도구 오류는 모델에 전달하되, 상담 취소는 모델 호출을 이어가지 않도록 그대로 다시 던진다.
   */
  @Bean
  public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor(
      @Value("${spring.ai.tools.throw-exception-on-error:false}") boolean throwExceptionOnError) {
    return DefaultToolExecutionExceptionProcessor.builder()
        .alwaysThrow(throwExceptionOnError)
        .rethrowExceptions(List.of(DiagnosisService.DiagnosisCancelledException.class))
        .build();
  }
}
