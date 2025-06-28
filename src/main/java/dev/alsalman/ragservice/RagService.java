
package dev.alsalman.ragservice;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RagService {

    private final Logger log = LoggerFactory.getLogger(RagService.class);
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        try {
            // Add some Java files to the vector store
            addJavaFilesToVectorStore("src/main/java/dev/alsalman/ragservice");
        } catch (Exception e) {
            log.error("Error initializing RAG service", e);
        }
    }

    /**
     * Add Java files from a directory to the vector store
     * @param directoryPath the directory path to scan for Java files
     * @throws IOException if there's an error reading the files
     */
    public void addJavaFilesToVectorStore(String directoryPath) throws IOException {
        log.info("Adding Java files from {} to vector store", directoryPath);

        List<Document> documents = new ArrayList<>();
        Path dir = Paths.get(directoryPath);

        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                String fileName = javaFile.getFileName().toString();

                Document document = new Document(content, Map.of(
                        "filename", fileName,
                        "path", javaFile.toString()
                ));

                documents.add(document);
                log.info("Added {} to vector store", fileName);
            }
        }

        vectorStore.add(documents);
        log.info("Added {} Java files to vector store", documents.size());
    }

    /**
     * Query the RAG service with a user question
     * @param userQuestion the user's question
     * @return the response from the LLM
     */
    public Flux<String> query(String userQuestion) {
        log.info("Querying RAG service with: {}", userQuestion);

        // Search for relevant documents
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userQuestion)
                        .topK(3)
                        .build());

        // Extract content from relevant documents
        String contextContent = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        log.info("Found {} relevant documents", relevantDocs.size());

        String systemPromptContent = String.format("""
                You are a helpful assistant that answers questions about Java code.
                Use the following code context to answer the user's question:

                %s

                If you don't know the answer based on the provided context, say so.
                """, contextContent);

        Message systemMessage = new SystemMessage(systemPromptContent);
        Message userMessage = new UserMessage(userQuestion);

        // Create prompt with system and user messages
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        // Get response from LLM
        return chatClient.prompt(prompt).stream().content();
    }
}