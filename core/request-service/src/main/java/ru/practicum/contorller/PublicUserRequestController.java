package ru.practicum.contorller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.service.RequestService;

@RestController
@AllArgsConstructor
public class PublicUserRequestController {
    private final RequestService requestService;

    @GetMapping("/events/{eventId}/requests/count")
    public ResponseEntity<Long> countConfirmedRequestsByEventId(@PathVariable Long eventId) {
        return ResponseEntity.ok(requestService.countConfirmedRequestsByEventId(eventId));
    }
}
