package com.suga.googlegenai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GooglegenaiApplicationTests {

	@Test
	void contextLoads() {
	}
	@Test
	void testAdd() {
		GooglegenaiApplication calculator = new GooglegenaiApplication();
	        int result = calculator.add(2, 3);
	        assertEquals(5, result);
	}

}
