package com.example.schedulebook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ScheduleBookApplicationTests {

    @Test
    @DisplayName("Spring ApplicationContext가 정상적으로 로드된다")
    void contextLoads() {
    }
}