package ru.practicum.contorller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateUserRequestController {
    private final RequestService requestService;

    @PostMapping("/requests")
    public ResponseEntity<ParticipationRequestDto> postRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.postRequest(userId, eventId));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(
            @PathVariable Long userId) {

        return ResponseEntity.ok()
                .body(requestService.getRequests(userId));
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {

        return ResponseEntity.ok()
                .body(requestService.patchRequest(userId, requestId));
    }

    @GetMapping("/events/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        return ResponseEntity.ok()
                .body(requestService.getEventRequests(userId, eventId));
    }

    @PatchMapping("/events/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResult> patchEventRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest updateDto) {

        return ResponseEntity.ok()
                .body(requestService.patchEventRequestsStatus(userId, eventId, updateDto));
    }
}