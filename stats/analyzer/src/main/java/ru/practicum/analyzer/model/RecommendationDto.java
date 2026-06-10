package ru.practicum.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecommendationDto {
    private Long eventId;
    private Double score;
}
