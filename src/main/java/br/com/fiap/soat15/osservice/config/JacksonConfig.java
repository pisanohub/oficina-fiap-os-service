package br.com.fiap.soat15.osservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Importante para serializar datas do Java 8 (como Instant, LocalDateTime) que você usa no código
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}