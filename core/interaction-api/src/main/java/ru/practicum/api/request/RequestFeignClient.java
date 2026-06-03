package ru.practicum.api.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "request-service", fallback = RequestClientFallBack.class)
public interface RequestFeignClient {
    @GetMapping("/events/{eventId}/requests/count")
    Long countConfirmedRequestsByEventId(@PathVariable("eventId") Long eventId);
}
