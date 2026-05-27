package ru.practicum.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCommentDto {
    @NotBlank
    @Size(min = 10, max = 2000)
    private String comment;
}