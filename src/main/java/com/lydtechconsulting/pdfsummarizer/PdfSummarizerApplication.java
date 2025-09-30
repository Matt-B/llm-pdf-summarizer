package com.lydtechconsulting.pdfsummarizer;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;

@SpringBootApplication
public class PdfSummarizerApplication implements CommandLineRunner {

    private final ChatClient chatClient;

    public PdfSummarizerApplication(@Lazy ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(PdfSummarizerApplication.class, args).close();
    }

    @Override
    public void run(String... args) throws Exception {
        var pdfResource = new ClassPathResource("/Streambased-Reference-Architecture.pdf");
        System.out.println("Summary:\n" + this.summarize(new Media(Media.Format.DOC_PDF, pdfResource)));

    }

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            ChatMemory chatMemory,
            @Value("classpath:prompts/chatbot-system-prompt.st") org.springframework.core.io.Resource systemPrompt
    ) {
        return ChatClient
                .builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

    public String summarize(Media file) {
        return chatClient
                .prompt()
                .user(promptUserSpec ->
                        promptUserSpec
                                .text("Please summarise this file.")
                                .media(file))
                .call()
                .content();
    }
}
