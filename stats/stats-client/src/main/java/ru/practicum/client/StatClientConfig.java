package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import ru.practicum.handler.exception.StatsServerUnavailable;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class StatClientConfig {
    private final RetryTemplate retryTemplate;
    private final DiscoveryClient discoveryClient;

    @Value("${stats-server.service-id}")
    private String statsServiceId;

    @Bean
    public StatClient statClient() {
        return new StatClient(this::makeUri);
    }

    private URI makeUri(String path) {
        ServiceInstance instance =
                retryTemplate.execute(ctx -> getInstance());

        return URI.create(
                "http://" +
                        instance.getHost() +
                        ":" +
                        instance.getPort() +
                        path
        );
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }
}