package ru.practicum.api.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventFullDto;

@Slf4j
@Component
public class EventClientFallback implements EventFeignClient {
    @Override
    public EventFullDto getEventById(Long eventId) {
        log.warn("EVENT-SERVER unavailable for getEventById({})", eventId);

        return EventFullDto.builder()
                .id(eventId)
                .annotation("EVENT-SERVER unavailable")
                .title("EVENT-SERVER unavailable")
                .description("EVENT-SERVER unavailable")
                .build();
    }
}

