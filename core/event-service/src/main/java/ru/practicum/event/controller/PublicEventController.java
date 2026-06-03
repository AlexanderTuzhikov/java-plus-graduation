package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.PageParams;
import ru.practicum.dto.event.PublicEventParams;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.BadRequestException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PublicEventController {
    private final EventService eventService;

    @GetMapping("/events")
    public ResponseEntity<List<EventShortDto>> getEventsByPublicFilters(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("Range end cannot be before range start");
        }

        PublicEventParams params = new PublicEventParams(text, categories, paid, rangeStart,
                rangeEnd, onlyAvailable, sort, new PageParams(from, size));

        List<EventShortDto> events = eventService.getEventsByPublicFilters(params, request);

        eventService.saveStats(request);

        return ResponseEntity.ok()
                .body(events);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventFullDto> getEventById(@PathVariable("eventId") Long eventId,
                                                     HttpServletRequest request) {
        EventFullDto event = eventService.getEventById(eventId, request);
        eventService.saveStats(request);

        return ResponseEntity.ok().body(event);
    }

    @GetMapping("/internal/events/{eventId}")
    public ResponseEntity<EventFullDto> getEventById(@PathVariable("eventId") Long eventId) {

        return ResponseEntity.ok().body(eventService.getEventById(eventId));
    }
}