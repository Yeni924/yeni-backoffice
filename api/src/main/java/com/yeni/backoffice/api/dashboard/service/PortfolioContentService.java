package com.yeni.backoffice.api.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioContentService {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public List<Map<String, Object>> getProjects() {
        ClassPathResource resource = new ClassPathResource("portfolio/portfolio.yml");

        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, List<Map<String, Object>>> content = yamlMapper.readValue(inputStream, new TypeReference<>() {
            });
            return content.getOrDefault("projects", Collections.emptyList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load portfolio content YAML.", e);
        }
    }
}
