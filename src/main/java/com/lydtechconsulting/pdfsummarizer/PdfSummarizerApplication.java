package com.lydtechconsulting.pdfsummarizer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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

import java.io.File;
import java.io.IOException;

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

        // Send the PDF directly to Claude to summarise
        var pdfResource = new ClassPathResource("/Streambased-Reference-Architecture.pdf");
        System.out.println("Summary:\n" + this.summarize(new Media(Media.Format.DOC_PDF, pdfResource)));

        // Extract the text from the PDF using PDFBox and then send the text to Claude to be summarised
        var pdfFile = new ClassPathResource("/Streambased-Reference-Architecture.pdf").getFile();
        System.out.println("Summary of extracted text:\n" + this.summarize(this.getTextFromPdf(pdfFile)));
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

    public String summarize(String text) {
        return chatClient
                .prompt()
                .user(promptUserSpec ->
                        promptUserSpec
                                .text("Please summarise this text: " + text))
                .call()
                .content();
    }

    private String getTextFromPdf(File pdfFile) throws IOException {
        PDDocument document = Loader.loadPDF(pdfFile);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }
}
