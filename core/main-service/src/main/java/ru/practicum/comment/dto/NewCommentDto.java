package ru.practicum.comment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NewCommentDto {
    @NotBlank
    @Size(min = 10, max = 2000)
    private String comment;
}