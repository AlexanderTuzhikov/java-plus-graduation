package ru.practicum.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class NewUserRequest {
    @NotBlank(message = "empty or null email")
    @Email(message = "invalid email")
    @Size(min = 6, max = 254)
    private String email;
    @NotBlank(message = "empty or null name")
    @Size(min = 2, max = 250)
    private String name;
}