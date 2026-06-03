package ru.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventRequest;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateUserEventController {
    private final EventService eventService;

    @GetMapping("/events")
    public ResponseEntity<List<EventShortDto>> getEvents(
            @PathVariable Long userId,
            @RequestParam(name = "from", defaultValue = "0") Integer from,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        return ResponseEntity.ok()
                .body(eventService.getEvents(userId, pageable));
    }

    @PostMapping("/events")
    public ResponseEntity<EventFullDto> postEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventRequest newEventDto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.postEvent(userId, newEventDto));
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventFullDto> getEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        return ResponseEntity.ok()
                .body(eventService.getEvent(userId, eventId));
    }

    @PatchMapping("/events/{eventId}")
    public ResponseEntity<EventFullDto> patchEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {

        return ResponseEntity.ok()
                .body(eventService.patchEventByUser(userId, eventId, updateEventUserRequest));
    }
}