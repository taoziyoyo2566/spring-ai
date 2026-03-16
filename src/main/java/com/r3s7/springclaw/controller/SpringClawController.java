package com.r3s7.springclaw.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/spring-claw")
public class SpringClawController {

    private static final Logger log = LoggerFactory.getLogger(SpringClawController.class);

    private static final int MAX_USER_INPUT_LENGTH = 4096;
    private static final int MAX_SESSION_ID_LENGTH = 128;
    private static final int MEMORY_TOP_K = 3;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public SpringClawController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseBodyEmitter chat(@Valid @RequestBody ChatRequest request) {
        long startNanos = System.nanoTime();
        String sessionId = request.sessionId();
        String userInput = request.userInput();
        // 【1. 潜意识检索】按会话隔离，检索当前会话最相关的历史记忆
        var filterBuilder = new FilterExpressionBuilder();
        var sessionFilter = filterBuilder.eq("sessionId", sessionId).build();

        List<Document> pastMemories = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userInput)
                        .topK(MEMORY_TOP_K)
                        .filterExpression(sessionFilter)
                        .build()
        );

        // 【2. 形成记忆】将用户当前输入存入向量库
        Map<String, Object> metadata = Map.of("sessionId", sessionId);
        Document newMemory = new Document(userInput, metadata);
        vectorStore.add(List.of(newMemory));

        // 【3. 组装意识】将历史记忆拼成上下文
        String historyContext = pastMemories.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        String promptText = """
                你是一个正在成长中的 AI 代理，名字叫 Spring-Claw。
                以下是你脑海中刚刚浮现的、与当前对话相关的历史记忆：
                %s
                
                请结合这些记忆，回答用户的最新问题。
                """.formatted(historyContext);

        // 【4. 流式输出】调用模型并将结果逐步返回
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L);
        StringBuilder responseBuffer = new StringBuilder();
        chatClient.prompt()
                .system(promptText)
                .user(userInput)
                .stream()
                .content()
                .subscribe(
                        token -> {
                            responseBuffer.append(token);
                            try {
                                emitter.send(token);
                            } catch (IOException ex) {
                                throw new UncheckedIOException(ex);
                            }
                        },
                        ex -> {
                            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                            log.warn(
                                    "chat request failed sessionId={} userInput={} memories={} partialResponse={} elapsedMs={}",
                                    sessionId,
                                    escapeForLog(userInput),
                                    pastMemories.size(),
                                    escapeForLog(responseBuffer.toString()),
                                    elapsedMs,
                                    ex
                            );
                            emitter.completeWithError(ex);
                        },
                        () -> {
                            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                            log.info(
                                    "chat request ok sessionId={} userInput={} memories={} response={} elapsedMs={}",
                                    sessionId,
                                    escapeForLog(userInput),
                                    pastMemories.size(),
                                    escapeForLog(responseBuffer.toString()),
                                    elapsedMs
                            );
                            emitter.complete();
                        }
                );
        return emitter;
    }

    private static String escapeForLog(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\n", "\\n").replace("\r", "\\r");
    }

    public record ChatRequest(
            @NotBlank @Size(max = MAX_SESSION_ID_LENGTH) String sessionId,
            @NotBlank @Size(max = MAX_USER_INPUT_LENGTH) String userInput
    ) {}
}
