package ru.practicum.api.event;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.*;


@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventFeignClient {
    @GetMapping("/internal/events/{eventId}")
    EventFullDto getEventById(@PathVariable("eventId") @Positive Long eventId);
}
