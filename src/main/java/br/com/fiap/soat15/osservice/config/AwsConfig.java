package br.com.fiap.soat15.osservice.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
@ConditionalOnProperty(
        name = "spring.cloud.aws.sqs.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AwsConfig {

    @Value("${spring.cloud.aws.sqs.endpoint}")
    private String awsEndpoint;

    @Value("${spring.cloud.aws.region.static}")
    private String awsRegion;

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    private AwsCredentialsProvider credentialsProvider() {
        if (accessKey == null || accessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object>
    defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {

        return SqsMessageListenerContainerFactory.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}
