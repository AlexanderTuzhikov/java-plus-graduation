package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    public List<RecommendedEventProto> getRecommendations(long userId, int maxResults) {
        List<UserAction> userActions = userActionRepository.findTopByUserIdOrderByLastActionTimeDesc(userId, maxResults);

        if (userActions.isEmpty()) {
            return List.of();
        }

        Set<Long> interactedEvents = userActionRepository.findEventIdsByUserId(userId);
        Map<Long, Double> candidateScores = new HashMap<>();

        for (UserAction action : userActions) {
            List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEventsOrderByScoreDesc(action.getEventId());

            for (EventSimilarity similarity : similarities) {
                Long candidateId = getRelatedEventId(similarity, action.getEventId());

                if (!interactedEvents.contains(candidateId)) {
                    candidateScores.merge(candidateId, similarity.getScore(), Double::sum);
                }
            }
        }

        return candidateScores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue()
                        .reversed())
                .limit(maxResults)
                .map(this::toRecommendation)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        Set<Long> seenEvents = userId > 0
                ? userActionRepository.findEventIdsByUserId(userId)
                : Collections.emptySet();

        return eventSimilarityRepository.findSimilarEventsOrderByScoreDesc(eventId).stream()
                .map(similarity -> Map.entry(
                        getRelatedEventId(similarity, eventId),
                        similarity.getScore()
                ))
                .filter(entry -> !seenEvents.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue()
                        .reversed())
                .limit(maxResults)
                .map(this::toRecommendation)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {

        return eventIds.stream()
                .map(eventId -> RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(
                                Optional.ofNullable(
                                        userActionRepository
                                                .sumWeightsByEventId(eventId)
                                ).orElse(0.0)
                        )
                        .build())
                .toList();
    }

    private Long getRelatedEventId(EventSimilarity similarity, Long sourceEventId) {

        return similarity.getEventA().equals(sourceEventId)
                ? similarity.getEventB()
                : similarity.getEventA();
    }

    private RecommendedEventProto toRecommendation(Map.Entry<Long, Double> entry) {

        return RecommendedEventProto.newBuilder()
                .setEventId(entry.getKey())
                .setScore(entry.getValue())
                .build();
    }
}