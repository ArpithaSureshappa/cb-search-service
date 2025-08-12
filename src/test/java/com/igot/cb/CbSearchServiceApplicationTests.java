package com.igot.cb;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class CbSearchServiceApplicationTests {

	@Test
	void mainMethodTest() {
		try (MockedStatic<SpringApplication> mockedSpringApp = Mockito.mockStatic(SpringApplication.class)) {
			CbSearchServiceApplication.main(new String[]{});
			mockedSpringApp.verify(() -> SpringApplication.run(eq(CbSearchServiceApplication.class), any(String[].class)));
		}
	}
}
