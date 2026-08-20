package lk.ac.nsbm.bookwise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the whole Spring context - entities, repositories, services,
 * controllers, security filter chain and the seeder - wires up without error.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookwiseApplicationTests {

    @Test
    @DisplayName("Application context loads")
    void contextLoads() {
        // Fails the build if any bean cannot be created or injected.
    }
}
