package com.example.schedulebook.common.security;

import com.example.schedulebook.common.consts.RedisConst;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);

        int length = request.getContentLength();

        if (length != -1 && length > RedisConst.MAX_BODY_SIZE) {
            throw new BaseException(ErrorEnum.REQUEST_BODY_TOO_LARGE);
        }

        try (ServletInputStream inputStream = request.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[RedisConst.BUFFER_SIZE];
            int nRead;
            int totalRead = 0;

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                totalRead += nRead;

                if (totalRead > RedisConst.MAX_BODY_SIZE) {
                    throw new BaseException(ErrorEnum.REQUEST_BODY_TOO_LARGE);
                }

                buffer.write(data, 0, nRead);
            }

            body = buffer.toByteArray();
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

        return new ServletInputStream() {

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {}

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    public byte[] getBody() {
        return body;
    }
}
