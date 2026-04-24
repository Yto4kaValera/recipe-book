package com.recipebook;

import com.recipebook.domain.CookingState;
import com.recipebook.domain.DietFlag;
import com.recipebook.domain.Dish;
import com.recipebook.domain.DishCategory;
import com.recipebook.domain.DishIngredient;
import com.recipebook.domain.Nutrition;
import com.recipebook.domain.Product;
import com.recipebook.domain.ProductCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.http.client.ClientHttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API / интеграционные тесты
 *  Эквивалентное разбиение (EP): валидные и невалидные классы для:длины имени, суммы БЖУ, удаления продукта,
 * наличие макроса в названии блюда
 * Анализ граничных значений (BVA): проверка значений на границах: длина имени (1, 2), сумма БЖУ (100, 100.01),
 * количество ингредиента (0, 0.01)
 */
class RecipeApiIntegrationTest extends RecipeApiIntegrationTestSupport {
    @Test
    void health_ok() {
        ResponseEntity<Map> response = http.getForEntity(url("/health"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ok");
    }
}

abstract class RecipeApiIntegrationTestSupport {
    protected final RestTemplate http = new RestTemplate();

    protected final List<String> createdDishIds = new ArrayList<>();
    protected final List<String> createdProductIds = new ArrayList<>();

    protected String baseUrl;
    protected String runPrefix;

    @BeforeEach
    void initRunPrefix() {
        this.baseUrl = resolveBaseUrl();
        this.runPrefix = "it-" + UUID.randomUUID() + "-";
        this.http.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
            }
        });
    }

    protected String url(String path) {
        return baseUrl + "/api" + path;
    }

    protected static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> createDishOrFail(Dish dish) {
        ResponseEntity<Map> response = http.postForEntity(url("/dishes"), new HttpEntity<>(dish, jsonHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        createdDishIds.add((String) body.get("id"));
        return body;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> createProductOrFail(Product product) {
        ResponseEntity<Map> response = http.postForEntity(url("/products"), new HttpEntity<>(product, jsonHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        createdProductIds.add((String) body.get("id"));
        return body;
    }

    @AfterEach
    void cleanup() {
        for (int i = createdDishIds.size() - 1; i >= 0; i--) {
            safeDelete("/dishes/" + createdDishIds.get(i));
        }
        for (int i = createdProductIds.size() - 1; i >= 0; i--) {
            safeDelete("/products/" + createdProductIds.get(i));
        }
        createdDishIds.clear();
        createdProductIds.clear();
    }

    protected void safeDelete(String apiPath) {
        try {
            http.exchange(url(apiPath), HttpMethod.DELETE, null, Map.class);
        } catch (HttpStatusCodeException ignored) {
        }
    }
    protected String resolveBaseUrl() {
        String fromEnv = System.getenv("RECIPEBOOK_BASE_URL");
        if (fromEnv == null || fromEnv.isBlank()) {
            fromEnv = System.getProperty("recipebook.baseUrl");
        }
        if (fromEnv == null || fromEnv.isBlank()) {
            throw new IllegalStateException("Set RECIPEBOOK_BASE_URL (or -Drecipebook.baseUrl). Example: http://localhost:8080");
        }
        String normalized = fromEnv.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

