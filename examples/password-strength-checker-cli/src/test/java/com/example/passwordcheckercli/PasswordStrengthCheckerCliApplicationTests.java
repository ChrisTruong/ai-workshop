package com.example.passwordcheckercli;

import com.example.passwordcheckercli.cli.PasswordCheckerCli;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PasswordStrengthCheckerCliApplicationTests {

    /**
     * Mock the interactive CLI runner so the context test does not block on stdin.
     * The CommandLineRunner is replaced with a no-op mock for this test only.
     */
    @MockitoBean
    PasswordCheckerCli passwordCheckerCli;

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts cleanly (no web server, no stdin blocking).
    }
}
