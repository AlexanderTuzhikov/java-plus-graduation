package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class StatClient {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Function<String, URI> uriFactory;
    private final RestClient restClient;

    public StatClient(Function<String, URI> uriFactory) {
        this.uriFactory = uriFactory;
        this.restClient = RestClient.create();
    }

    public void saveHit(NewEndpointHitDto hitDto) {
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(uriFactory.apply("/hit"))
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().isError()) {
                throw new RuntimeException(
                        "Failed to save hit: " + response.getStatusCode()
                );
            }

        } catch (Exception e) {
            log.error("Error calling Stat Client", e);
            throw new RuntimeException(
                    "Error while saving hit to stats service",
                    e
            );
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        try {
            String startStr = start.format(FORMATTER);
            String endStr = end.format(FORMATTER);

            URI uri = UriComponentsBuilder
                    .fromUri(uriFactory.apply("/stats"))
                    .queryParam("start", startStr)
                    .queryParam("end", endStr)
                    .queryParam("unique", unique)
                    .queryParam(
                            "uris",
                            uris == null ? null : uris.toArray()
                    )
                    .build()
                    .encode()
                    .toUri();

            log.info("Stats request URI: {}", uri);

            ResponseEntity<ViewStatsDto[]> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(ViewStatsDto[].class);

            ViewStatsDto[] body = response.getBody();

            return body != null
                    ? Arrays.asList(body)
                    : Collections.emptyList();

        } catch (Exception e) {
            log.error("Error calling Stat Client", e);

            throw new RuntimeException(
                    "Error while getting stats from stats service",
                    e
            );
        }
    }
}