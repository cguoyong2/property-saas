package com.yongquan.propertysaas.security;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashTests {

    private static final String DEMO_HASH = "$2a$10$0cHYTw8z6lWqatoUjQh/rOZAJxJiePlrFlCNCJts4KxHBAol0vizm";

    @Test
    void demoPasswordUsesBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        Assertions.assertThat(encoder.matches("Admin@123", DEMO_HASH)).isTrue();
    }
}
