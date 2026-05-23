package ru.practicum.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class StatClient {
    private final Function<String, URI> uriFactory;
    private final RestClient restClient;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
                throw new RuntimeException("Failed to save hit: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while saving hit to stats service", e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        try {
            String startStr = start.format(FORMATTER);
            String endStr = end.format(FORMATTER);

            URI uri = UriComponentsBuilder
                    .fromUri(uriFactory.apply("/stats"))
                    .queryParam("start", startStr)
                    .queryParam("end", endStr)
                    .queryParam("unique", unique)
                    .build()
                    .toUri();

            if (uris != null && !uris.isEmpty()) {
                uri = UriComponentsBuilder.fromUri(uri)
                        .queryParam("uris", String.join(",", uris))
                        .build()
                        .toUri();
            }

            ResponseEntity<ViewStatsDto[]> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(ViewStatsDto[].class);

            return Arrays.asList(response.getBody() != null ? response.getBody() : new ViewStatsDto[0]);
        } catch (Exception e) {
            throw new RuntimeException("Error while getting stats from stats service", e);
        }
    }
}