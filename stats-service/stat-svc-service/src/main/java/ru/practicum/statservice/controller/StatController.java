package ru.practicum.statservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.handler.BadRequestException;
import ru.practicum.statservice.service.StatService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping
@RequiredArgsConstructor
public class StatController {
    private final StatService statService;

    @PostMapping("/hit")
    public ResponseEntity<Void> hit(@Valid @RequestBody NewEndpointHitDto hitDto) {
        log.info("Получен запрос на сохранение hit: {}", hitDto);
        statService.saveHit(hitDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                       LocalDateTime start,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                                       @RequestParam(required = false) List<String> uris,
                                                       @RequestParam(defaultValue = "false") boolean unique) {
        if (start.isAfter(end)) {
            throw new BadRequestException("Время начала не может быть после окончания выборки");
        }

        log.info("Получен запрос на статистику: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        return ResponseEntity.ok(
                statService.getStats(start, end, uris, unique)
        );
    }
}
