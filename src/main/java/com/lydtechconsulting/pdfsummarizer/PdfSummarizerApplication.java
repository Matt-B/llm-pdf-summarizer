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
        String fileName = "/Streambased-Reference-Architecture.pdf";
        String summary;
        if(getNumberOfPages(new ClassPathResource(fileName).getFile()) > 100) {
            // PDF has more than 100 pages so we need to extract the text and send that to Claude
            System.out.println("Extracting text from PDF.");
            var pdfFile = new ClassPathResource(fileName).getFile();
            summary = this.summarize(this.getTextFromPdf(pdfFile));
        } else {
            // Send the PDF directly to Claude to summarise
            System.out.println("Sending PDF directly to Claude.");
            var pdfResource = new ClassPathResource(fileName);
            summary = this.summarize(new Media(Media.Format.DOC_PDF, pdfResource));
        }
        System.out.println("Summary: " + summary);
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
        try {
            return chatClient
                    .prompt()
                    .user(promptUserSpec ->
                            promptUserSpec
                                    .text("Please summarise this file.")
                                    .media(file))
                    .call()
                    .content();
        } catch (Exception e) {
            return "Error encountered: " + e.getMessage();
        }
    }

    public String summarize(String text) {
        try {
            return chatClient
                    .prompt()
                    .user(promptUserSpec ->
                            promptUserSpec
                                    .text("Please summarise this text: " + text))
                    .call()
                    .content();
        } catch (Exception e) {
            return "Error encountered: " + e.getMessage();
        }
    }

    private String getTextFromPdf(File pdfFile) throws IOException {
        PDDocument document = Loader.loadPDF(pdfFile);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }

    private int getNumberOfPages(File pdfFile) throws IOException {
        PDDocument document = Loader.loadPDF(pdfFile);
        int numberOfPages = document.getNumberOfPages();
        document.close();
        return numberOfPages;
    }
}
