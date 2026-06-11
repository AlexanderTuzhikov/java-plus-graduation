package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SimilarityCalculator {
    private final AggregationStorage storage;

    public double calculate(Long eventA, Long eventB) {
        Double totalWeightA = storage.getEventTotalWeights().get(eventA);
        Double totalWeightB = storage.getEventTotalWeights().get(eventB);
        Double sMin = storage.getMinWeightsSums()
                .getOrDefault(eventA, Map.of())
                .get(eventB);

        if (totalWeightA == null
                || totalWeightB == null
                || sMin == null
                || totalWeightA == 0
                || totalWeightB == 0) {
            return 0.0;
        }

        return sMin / (Math.sqrt(totalWeightA) * Math.sqrt(totalWeightB));
    }
}
