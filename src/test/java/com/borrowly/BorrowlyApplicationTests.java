package com.borrowly;

import com.borrowly.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
class BorrowlyApplicationTests extends AbstractPostgresTest {

	@Test
	void contextLoads() {
	}

}
