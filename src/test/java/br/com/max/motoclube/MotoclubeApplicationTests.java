package br.com.max.motoclube;

import br.com.max.motoclube.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestContainer.class)
class MotoclubeApplicationTests {

    @Test
    void contextLoads() {}
}
