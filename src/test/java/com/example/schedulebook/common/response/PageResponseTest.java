package com.example.schedulebook.common.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    @DisplayName("Page를 PageResponse로 변환하면 페이징 정보가 정상적으로 매핑된다")
    void givenPage_whenRegister_thenReturnPageResponse() {

        // given
        List<String> content =
                List.of(
                        "item1",
                        "item2",
                        "item3"
                );

        Pageable pageable =
                PageRequest.of(
                        1,
                        3
                );

        PageImpl<String> page =
                new PageImpl<>(
                        content,
                        pageable,
                        10
                );

        // when
        PageResponse<String> response =
                PageResponse.register(page);

        // then
        assertEquals(
                content,
                response.content()
        );

        // PageRequest는 0부터 시작하지만
        // API 응답은 1부터 시작
        assertEquals(
                2,
                response.currentPage()
        );

        assertEquals(
                4,
                response.totalPages()
        );

        assertEquals(
                10,
                response.totalElements()
        );

        assertEquals(
                3,
                response.size()
        );

        assertFalse(
                response.isLast()
        );
    }

    @Test
    @DisplayName("마지막 페이지이면 isLast가 true로 반환된다")
    void givenLastPage_whenRegister_thenIsLastTrue() {

        // given
        List<String> content =
                List.of(
                        "item1",
                        "item2"
                );

        Pageable pageable =
                PageRequest.of(
                        1,
                        2
                );

        PageImpl<String> page =
                new PageImpl<>(
                        content,
                        pageable,
                        4
                );

        // when
        PageResponse<String> response =
                PageResponse.register(page);

        // then
        assertEquals(
                2,
                response.currentPage()
        );

        assertEquals(
                2,
                response.totalPages()
        );

        assertEquals(
                4,
                response.totalElements()
        );

        assertEquals(
                2,
                response.size()
        );

        assertTrue(
                response.isLast()
        );
    }

    @Test
    @DisplayName("빈 Page를 변환하면 content가 빈 리스트로 반환된다")
    void givenEmptyPage_whenRegister_thenReturnEmptyContent() {

        // given
        Pageable pageable =
                PageRequest.of(
                        0,
                        10
                );

        PageImpl<String> page =
                new PageImpl<>(
                        List.of(),
                        pageable,
                        0
                );

        // when
        PageResponse<String> response =
                PageResponse.register(page);

        // then
        assertNotNull(response.content());

        assertTrue(
                response.content().isEmpty()
        );

        assertEquals(
                1,
                response.currentPage()
        );

        assertEquals(
                0,
                response.totalPages()
        );

        assertEquals(
                0,
                response.totalElements()
        );

        assertEquals(
                10,
                response.size()
        );

        assertTrue(
                response.isLast()
        );
    }

    @Test
    @DisplayName("PageResponse가 JSON 응답 계약에 맞게 직렬화된다")
    void givenPageResponse_whenSerialize_thenReturnExpectedJsonContract()
            throws Exception {

        // given
        List<String> content =
                List.of(
                        "item1",
                        "item2",
                        "item3"
                );

        Pageable pageable =
                PageRequest.of(
                        0,
                        3
                );

        PageImpl<String> page =
                new PageImpl<>(
                        content,
                        pageable,
                        10
                );

        PageResponse<String> response =
                PageResponse.register(page);

        ObjectMapper objectMapper =
                new ObjectMapper();

        // when
        String json =
                objectMapper.writeValueAsString(response);

        JsonNode root =
                objectMapper.readTree(json);

        // then
        assertTrue(
                root.get("content").isArray()
        );

        assertEquals(
                3,
                root.get("content").size()
        );

        assertEquals(
                "item1",
                root.get("content").get(0).asText()
        );

        assertEquals(
                "item2",
                root.get("content").get(1).asText()
        );

        assertEquals(
                "item3",
                root.get("content").get(2).asText()
        );

        assertEquals(
                1,
                root.get("currentPage").asInt()
        );

        assertEquals(
                4,
                root.get("totalPages").asInt()
        );

        assertEquals(
                10,
                root.get("totalElements").asLong()
        );

        assertEquals(
                3,
                root.get("size").asInt()
        );

        assertFalse(
                root.get("isLast").asBoolean()
        );
    }
}