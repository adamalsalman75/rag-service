
package dev.alsalman.ragservice;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
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

    public Flux<String> query(String message) {

        var system = """
                You are a helpful assistant that answers questions about Java code.
                
                If you don't know the answer based on the provided context, say so.
                """;


        // Get response from LLM
        return chatClient
                .prompt()
                .system(system)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .user(message)
                .stream()
                .content();
    }
}