package ru.practicum.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class CommentShortDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private UserShortDto author;
    private Long eventId;
    private String comment;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedOn;
}