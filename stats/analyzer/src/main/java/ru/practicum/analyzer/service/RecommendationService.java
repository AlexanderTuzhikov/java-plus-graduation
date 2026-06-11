package ru.practicum.analyzer.service;

import ru.practicum.stats.proto.RecommendedEventProto;

import java.util.List;

public interface RecommendationService {
    List<RecommendedEventProto> getRecommendations(long userId, int maxResults);

    List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults);

    List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds);
}