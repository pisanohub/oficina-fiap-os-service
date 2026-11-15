package br.com.fiap.soat15.osservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Recupera os atributos da requisição HTTP atual que chegou no os-service
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // Extrai o cabeçalho "Authorization" da requisição original
                    String authorizationHeader = request.getHeader("Authorization");

                    // Se o token existir, repassa ele para a requisição do Feign
                    if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
                        template.header("Authorization", authorizationHeader);
                    }
                }
            }
        };
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        // FULL faz o Feign logar headers, corpo e metadados completos da requisição e resposta
        return Logger.Level.FULL;
    }
}