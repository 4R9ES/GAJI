package com.GAJI.GAJI;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
// 2026-11-24 김성윤: 테스트용 H2 설정으로 애플리케이션 초기화를 확인
@ActiveProfiles("test")
class GajiApplicationTests {

	@Test
	void contextLoads() {
	}

}
