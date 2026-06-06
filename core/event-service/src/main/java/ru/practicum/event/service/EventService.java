package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.dto.event.*;

import java.util.List;

public interface EventService {
    List<EventShortDto> getEvents(Long userId, Pageable pageable);

    EventFullDto postEvent(Long userId, NewEventRequest newEventRequest);

    EventFullDto getEvent(Long userId, Long eventId);

    EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<EventFullDto> getEventsByAdminFilters(EventParams params);

    EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, HttpServletRequest request);

    EventFullDto getEventById(Long eventId, HttpServletRequest request);

    EventFullDto getEventById(Long eventId);

    void saveStats(HttpServletRequest request);
}