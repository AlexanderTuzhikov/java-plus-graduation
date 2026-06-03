package ru.practicum.dto.user;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class UserDto {
    private String email;
    private Long id;
    private String name;
}