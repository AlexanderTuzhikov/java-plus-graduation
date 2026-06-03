package ru.practicum.dto.location;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto {
    @NotNull
    private Double lat;
    @NotNull
    private Double lon;
}