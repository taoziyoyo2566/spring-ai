package com.r3s7.springclaw;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

@ActiveProfiles("test")
@SpringBootTest(properties = {
		"spring.ai.vectorstore.pgvector.initialize-schema=false",
		"spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration,org.springframework.ai.ollama.autoconfigure.OllamaAutoConfiguration"
})
class SpringClawApplicationTests {

	@MockitoBean
	private VectorStore vectorStore;

	@TestConfiguration
	static class TestConfig {
		@Bean
		@Primary
		ChatClient.Builder testChatClientBuilder() {
			ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
			ChatClient chatClient = Mockito.mock(ChatClient.class);
			Mockito.when(builder.build()).thenReturn(chatClient);
			return builder;
		}
	}

	@Test
	void contextLoads() {
	}
}
