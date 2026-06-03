package ru.practicum.api.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@Slf4j
@Component
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public UserDto getUserById(Long userId) {
        log.warn("USER-SERVER unavailable for getUserById({})", userId);

        return new UserDto("USER-SERVER unavailable", userId, "USER-SERVER unavailable");
    }

    @Override
    public List<UserDto> getUsersByIds(List<Long> ids) {
        log.warn("USER-SERVER unavailable for batch users {}", ids);

        return ids.stream()
                .map(id -> new UserDto("USER-SERVER unavailable", id, "USER-SERVER unavailable"))
                .toList();
    }
}
