package ru.practicum.api.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {
    @GetMapping("/users/{userId}")
    UserDto getUserById(@PathVariable("userId") Long userId);

    @PostMapping("/users/batch")
    List<UserDto> getUsersByIds(@RequestBody List<Long> ids);
}