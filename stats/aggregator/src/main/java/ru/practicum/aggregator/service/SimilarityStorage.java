package ru.practicum.aggregator.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimilarityStorage {
    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventTotalWeights = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    public Map<Long, Double> getUserEvents(Long userId) {
        return userEventWeights.computeIfAbsent(userId, id -> new ConcurrentHashMap<>());
    }

    public Double getUserEventWeight(Long userId, Long eventId) {
        return userEventWeights.getOrDefault(userId, Map.of()).get(eventId);
    }

    public void saveUserEventWeight(Long userId, Long eventId, Double weight) {
        getUserEvents(userId).put(eventId, weight);
    }

    public void incrementEventTotalWeight(Long eventId, Double delta) {
        eventTotalWeights.merge(eventId, delta, Double::sum);
    }

    public Double getEventTotalWeight(Long eventId) {
        return eventTotalWeights.get(eventId);
    }

    public void incrementMinWeightSum(Long eventA, Long eventB, Double delta) {
        minWeightsSums.computeIfAbsent(eventA, id -> new ConcurrentHashMap<>())
                .merge(eventB, delta, Double::sum);
    }

    public Double getMinWeightSum(Long eventA, Long eventB) {
        return minWeightsSums.getOrDefault(eventA, Map.of()).get(eventB);
    }
}
