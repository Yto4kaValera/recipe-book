package com.recipebook.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recipebook.domain.DataStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class JsonFileStorage {
    private final Path storagePath;
    private final ObjectMapper objectMapper;

    public JsonFileStorage(@Value("${app.storage-path}") String storagePath) {
        this.storagePath = Path.of(storagePath);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        initialize();
    }

    public synchronized DataStore read() {
        try {
            return objectMapper.readValue(Files.readString(storagePath), DataStore.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать хранилище.", exception);
        }
    }

    public synchronized void write(DataStore dataStore) {
        try {
            Files.writeString(storagePath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataStore));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сохранить хранилище.", exception);
        }
    }

    private void initialize() {
        try {
            Files.createDirectories(storagePath.getParent());
            if (Files.notExists(storagePath) || Files.size(storagePath) == 0) {
                write(new DataStore());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось инициализировать хранилище.", exception);
        }
    }
}
