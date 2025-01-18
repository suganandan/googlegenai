package com.suga.googlegenai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class GooglegenaiApplicationTests {

	@Test
	void contextLoads()  {
		log.info("Context Loads");
	}
	@Test
	void testAdd() {
		GooglegenaiApplication calculator = new GooglegenaiApplication();
	        int result = calculator.add(2, 3);
	        assertEquals(5, result);
	}

}
