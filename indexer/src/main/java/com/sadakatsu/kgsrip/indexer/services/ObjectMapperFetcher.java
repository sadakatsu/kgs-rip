package com.sadakatsu.kgsrip.indexer.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class ObjectMapperFetcher {
    private static ObjectMapperFetcher instance;

    public static ObjectMapper fetch() {
        if (instance == null) {
            throw new IllegalStateException("ObjectMapperFetcher was not instantiated.");
        }
        return instance.objectMapper;
    }

    private final ObjectMapper objectMapper;

    public ObjectMapperFetcher(ObjectMapper objectMapper) {
        ObjectMapperFetcher.instance = this;
        this.objectMapper = objectMapper;
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
    }
}
