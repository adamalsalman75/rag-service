# RAG Service

A Spring Boot application that implements Retrieval-Augmented Generation (RAG) to answer questions about Java code.

## Overview

This service uses OpenAI's embedding and chat models to:
1. Process Java files and store their embeddings in a vector database
2. Retrieve relevant code snippets based on user queries
3. Generate responses using the retrieved context

## Features

- Automatically indexes Java files from the project directory on startup
- Uses OpenAI's text-embedding-3-small model for generating embeddings
- Uses OpenAI's gpt-4o model for generating responses
- Uses Spring AI's SimpleVectorStore for storing and retrieving embeddings
- Provides a query interface to ask questions about the code

## Requirements

- Java 21
- Maven
- OpenAI API key

## Configuration

Set your OpenAI API key as an environment variable:

```bash
export OPENAI_API_KEY=your-api-key
```

The application is configured in `application.yaml`:
```yaml
spring:
  application:
    name: rag-service
  main:
    web-application-type: none
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
      embedding:
        options:
          model: text-embedding-3-small
```

## Usage

### Running the Application

```bash
mvn spring-boot:run
```

### Using the RAG Service

The `RagService` class provides the following methods:

#### Adding Java Files to the Vector Store

```java
// Add Java files from a directory
ragService.addJavaFilesToVectorStore("path/to/java/files");
```

#### Querying the RAG Service

```java
// Ask a question about the code
String answer = ragService.query("What does the RagService class do?");
```

## How It Works

1. **Indexing**: Java files are read, processed, and stored in the vector database with their embeddings.
2. **Retrieval**: When a query is received, the service finds the top 3 most relevant documents based on embedding similarity.
3. **Generation**: The retrieved documents are used as context for the LLM to generate an accurate response.

## Technical Details

- Built with Spring Boot 3.5.3
- Uses Spring AI 1.0.0 for AI model integration
- Configured as a non-web application
- Automatically initializes by indexing the project's Java files

## Extending the Service

You can extend this service to:
- Support other file types
- Use different embedding or chat models
- Implement more advanced vector stores
- Add a REST API for remote querying
