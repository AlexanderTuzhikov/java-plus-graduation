package ru.practicum.aggregator.service;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Component
public class AggregationStorage {
    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventTotalWeights = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();
}
