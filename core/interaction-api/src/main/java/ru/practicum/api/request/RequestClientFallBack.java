package ru.practicum.api.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class RequestClientFallBack implements RequestFeignClient{
    @Override
    public Long countConfirmedRequestsByEventId(Long eventId) {
        log.warn("REQUEST-SERVER unavailable for countConfirmedRequestsByEventId({})", eventId);

        return 0L;
    }
}
