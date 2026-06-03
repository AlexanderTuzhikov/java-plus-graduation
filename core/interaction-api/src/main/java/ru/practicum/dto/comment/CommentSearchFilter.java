package ru.practicum.dto.comment;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CommentSearchFilter {
    private List<Long> userIds;
    private List<Long> eventIds;
    private List<CommentState> states;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
}