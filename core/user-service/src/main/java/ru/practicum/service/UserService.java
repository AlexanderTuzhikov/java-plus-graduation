package ru.practicum.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto postUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);

    Page<UserDto> getUsers(List<Long> ids, Pageable pageable);

    Page<UserDto> getAllUsers(Pageable pageable);

    UserDto getUserById(Long userId);

    List<UserDto> getUsersByIds(List<Long> ids);
}