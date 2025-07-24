package binhnvh.vaap.promptbuilder.service.impl;

import binhnvh.vaap.promptbuilder.dto.PromptResponseDTO;
import binhnvh.vaap.promptbuilder.service.ElasticsearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticsearchServiceImpl implements ElasticsearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${elasticsearch.index.prompts:prompts}")
    private String promptIndex;

    @Override
    public void indexPrompt(PromptResponseDTO prompt) {
        try {
            Map<String, Object> document = convertPromptToDocument(prompt);

            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index(promptIndex)
                    .id(prompt.id().toString())
                    .document(document)
            );

            elasticsearchClient.index(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to index prompt", e);
        }
    }

    @Override
    public void updatePromptIndex(PromptResponseDTO prompt) {
        try {
            Map<String, Object> document = convertPromptToDocument(prompt);

            UpdateRequest<Map<String, Object>, Map<String, Object>> request =
                    UpdateRequest.of(u -> u
                            .index(promptIndex)
                            .id(prompt.id().toString())
                            .doc(document)
                    );

            elasticsearchClient.update(request, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update prompt index", e);
        }
    }

    @Override
    public void deletePromptFromIndex(Long promptId) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d
                    .index(promptIndex)
                    .id(promptId.toString())
            );

            elasticsearchClient.delete(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete prompt from index", e);
        }
    }

    @Override
    public List<PromptResponseDTO> searchPrompts(String query, Map<String, Object> filters) {
        try {
            List<Query> mustQueries = new ArrayList<>();

            // Add text search query
            if (query != null && !query.isEmpty()) {
                mustQueries.add(QueryBuilders.multiMatch(m -> m
                        .query(query)
                        .fields(List.of("name^3", "description^2", "template", "variables.name"))
                ));
            }

            // Add filters
            if (filters != null) {
                filters.forEach((field, value) -> {
                    mustQueries.add(QueryBuilders.term(t -> t
                            .field(field)
                            .value(value.toString())
                    ));
                });
            }

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(promptIndex)
                    .query(q -> q
                            .bool(b -> b
                                    .must(mustQueries)
                            )
                    )
                    .size(100)
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            return response.hits().hits().stream()
                    .map(this::convertHitToPromptDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to search prompts", e);
        }
    }

    @Override
    public List<PromptResponseDTO> searchByTemplate(String templateQuery) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(promptIndex)
                    .query(q -> q
                            .match(m -> m
                                    .field("template")
                                    .query(templateQuery)
                            )
                    )
                    .size(50)
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            return response.hits().hits().stream()
                    .map(this::convertHitToPromptDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by template", e);
        }
    }

    @Override
    public List<PromptResponseDTO> searchByVariables(List<String> variableNames) {
        try {
            List<Query> shouldQueries = variableNames.stream()
                    .map(varName -> QueryBuilders.term(t -> t
                            .field("variables.name")
                            .value(varName)
                    ))
                    .collect(Collectors.toList());

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(promptIndex)
                    .query(q -> q
                            .bool(b -> b
                                    .should(shouldQueries)
                                    .minimumShouldMatch("1")
                            )
                    )
                    .size(100)
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            return response.hits().hits().stream()
                    .map(this::convertHitToPromptDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by variables", e);
        }
    }

    private Map<String, Object> convertPromptToDocument(PromptResponseDTO prompt) {
        Map<String, Object> document = new HashMap<>();
        document.put("id", prompt.id());
        document.put("name", prompt.name());
        document.put("description", prompt.description());
        document.put("template", prompt.template());
        document.put("llmProviderName", prompt.llmProviderName());
        document.put("modelName", prompt.modelName());
        document.put("active", prompt.active());
        document.put("currentVersion", prompt.currentVersion());
        document.put("createdAt", prompt.createdAt().toString());
        document.put("updatedAt", prompt.updatedAt().toString());
        document.put("createdBy", prompt.createdBy());

        // Convert variables
        List<Map<String, Object>> variablesList = prompt.variables().stream()
                .map(var -> {
                    Map<String, Object> varMap = new HashMap<>();
                    varMap.put("name", var.variableName());
                    varMap.put("defaultValue", var.defaultValue());
                    varMap.put("description", var.description());
                    varMap.put("required", var.required());
                    varMap.put("dataType", var.dataType().toString());
                    return varMap;
                })
                .collect(Collectors.toList());

        document.put("variables", variablesList);
        return document;
    }

    private PromptResponseDTO convertHitToPromptDTO(Hit<Map> hit) {
        try {
            Map source = hit.source();
            return objectMapper.convertValue(source, PromptResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Elasticsearch hit to DTO", e);
        }
    }
}
