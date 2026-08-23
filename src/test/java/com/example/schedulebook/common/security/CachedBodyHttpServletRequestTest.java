package com.example.schedulebook.common.security;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachedBodyHttpServletRequestTest {

    @Test
    @DisplayName("Request Body를 정상적으로 캐싱한다")
    void givenRequestBody_whenCreate_thenCacheBody() throws Exception {

        // given
        String requestBody = """
                {
                    "loginId": "test123",
                    "password": "password123"
                }
                """;

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setContent(
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        // when
        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        // then
        assertArrayEquals(
                requestBody.getBytes(StandardCharsets.UTF_8),
                cachedRequest.getBody()
        );
    }

    @Test
    @DisplayName("getBody()는 캐싱된 Request Body를 반환한다")
    void givenCachedRequest_whenGetBody_thenReturnOriginalBody() throws Exception {

        // given
        String requestBody = "test request body";

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setContent(
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        // when
        byte[] body = cachedRequest.getBody();

        // then
        assertEquals(
                requestBody,
                new String(body, StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("getInputStream()으로 Request Body를 읽을 수 있다")
    void givenCachedRequest_whenGetInputStream_thenReadBody() throws Exception {

        // given
        String requestBody = "hello schedulebook";

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setContent(
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        // when
        String result = new String(
                cachedRequest
                        .getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        // then
        assertEquals(requestBody, result);
    }

    @Test
    @DisplayName("getInputStream()을 두 번 호출해도 동일한 Request Body를 읽을 수 있다")
    void givenCachedRequest_whenReadInputStreamTwice_thenReturnSameBody() throws Exception {

        // given
        String requestBody = """
                {
                    "name": "schedulebook"
                }
                """;

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setContent(
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        // when
        String firstRead = new String(
                cachedRequest
                        .getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        String secondRead = new String(
                cachedRequest
                        .getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );

        // then
        assertEquals(requestBody, firstRead);
        assertEquals(requestBody, secondRead);
    }

    @Test
    @DisplayName("Content-Length가 최대 Body 크기를 초과하면 예외가 발생한다")
    void givenContentLengthExceedsMaxSize_whenCreate_thenThrowBodyTooLarge() throws IOException {

        // given
        HttpServletRequest request =
                mock(HttpServletRequest.class);

        when(request.getContentLength())
                .thenReturn(RedisConst.MAX_BODY_SIZE + 1);

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> new CachedBodyHttpServletRequest(request)
        );

        assertEquals(
                ErrorEnum.REQUEST_BODY_TOO_LARGE,
                exception.getErrorEnum()
        );

        verify(request)
                .getContentLength();

        verify(request, never())
                .getInputStream();
    }

    @Test
    @DisplayName("실제 Request Body 크기가 최대 크기를 초과하면 예외가 발생한다")
    void givenActualBodyExceedsMaxSize_whenCreate_thenThrowBodyTooLarge()
            throws Exception {

        // given
        HttpServletRequest request =
                mock(HttpServletRequest.class);

        ServletInputStream inputStream =
                new MockServletInputStream(
                        new byte[RedisConst.MAX_BODY_SIZE + 1]
                );

        when(request.getContentLength())
                .thenReturn(-1);

        when(request.getInputStream())
                .thenReturn(inputStream);

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> new CachedBodyHttpServletRequest(request)
        );

        assertEquals(
                ErrorEnum.REQUEST_BODY_TOO_LARGE,
                exception.getErrorEnum()
        );

        verify(request)
                .getContentLength();

        verify(request)
                .getInputStream();
    }

    @Test
    @DisplayName("빈 Request Body를 정상적으로 처리한다")
    void givenEmptyRequestBody_whenCreate_thenReturnEmptyBody() throws Exception {

        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setContent(new byte[0]);

        // when
        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        // then
        assertNotNull(cachedRequest.getBody());

        assertEquals(
                0,
                cachedRequest.getBody().length
        );

        assertEquals(
                0,
                cachedRequest
                        .getInputStream()
                        .readAllBytes()
                        .length
        );
    }

    @Test
    @DisplayName("Request Body를 모두 읽으면 isFinished()가 true를 반환한다")
    void givenRequestBody_whenReadAll_thenInputStreamIsFinished() throws Exception {

        // given
        String requestBody = "schedulebook";

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setContent(
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        ServletInputStream inputStream =
                cachedRequest.getInputStream();

        // then
        assertFalse(inputStream.isFinished());

        // when
        inputStream.readAllBytes();

        // then
        assertTrue(inputStream.isFinished());
    }

    @Test
    @DisplayName("ServletInputStream은 항상 읽기 준비 상태를 반환한다")
    void givenCachedRequest_whenGetInputStream_thenIsReadyReturnsTrue()
            throws Exception {

        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setContent(
                "test".getBytes(StandardCharsets.UTF_8)
        );

        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        // when
        ServletInputStream inputStream =
                cachedRequest.getInputStream();

        // then
        assertTrue(inputStream.isReady());
    }

    private static class MockServletInputStream
            extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        private MockServletInputStream(byte[] body) {
            this.inputStream =
                    new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(
                ReadListener readListener
        ) {
        }
    }
}