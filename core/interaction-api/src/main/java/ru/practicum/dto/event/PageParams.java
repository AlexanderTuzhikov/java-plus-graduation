package ru.practicum.dto.event;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PageParams {
    private Integer from = 0;
    private Integer size = 10;
}