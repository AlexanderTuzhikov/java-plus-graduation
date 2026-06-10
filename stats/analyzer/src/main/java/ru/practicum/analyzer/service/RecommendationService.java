package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.RecommendationDto;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {
    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    public List<RecommendationDto> getRecommendationsForUser(Long userId, int maxResults) {

        log.info("Получение рекомендаций для пользователя: userId={}, limit={}",
                userId, maxResults);

        List<UserAction> userActions = userActionRepository.findTopByUserIdOrderByLastActionTimeDesc(userId, maxResults);

        if (userActions.isEmpty()) {

            log.info("Для пользователя {} не найдено действий", userId);

            return List.of();
        }

        Set<Long> interactedEvents = userActionRepository.findEventIdsByUserId(userId);
        Map<Long, Double> candidateScores = new HashMap<>();

        for (UserAction action : userActions) {
            List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEventsOrderByScoreDesc(action.getEventId());

            for (EventSimilarity similarity : similarities) {
                Long candidateId = similarity.getEventA().equals(action.getEventId())
                                ? similarity.getEventB()
                                : similarity.getEventA();

                if (!interactedEvents.contains(candidateId)) {
                    candidateScores.merge(candidateId, similarity.getScore(), Double::sum);
                }
            }
        }

        log.info("Найдено {} кандидатов для рекомендаций", candidateScores.size());

        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendationDto.builder()
                        .eventId(entry.getKey())
                        .score(entry.getValue())
                        .build())
                .toList();
    }

    public List<RecommendationDto> getSimilarEvents(Long eventId, Long userId, int maxResults) {

        log.info("Поиск похожих событий: eventId={}, userId={}, limit={}",
                eventId, userId, maxResults);

        Set<Long> seenEvents = userId > 0
                ? userActionRepository.findEventIdsByUserId(userId)
                : Set.of();

        return eventSimilarityRepository.findSimilarEventsOrderByScoreDesc(eventId).stream()
                .map(similarity -> RecommendationDto.builder()
                        .eventId(similarity.getEventA().equals(eventId)
                                        ? similarity.getEventB()
                                        : similarity.getEventA())
                        .score(similarity.getScore())
                        .build())
                .filter(dto -> !seenEvents.contains(dto.getEventId()))
                .sorted(Comparator.comparing(RecommendationDto::getScore).reversed())
                .limit(maxResults)
                .toList();
    }

    public List<RecommendationDto> getInteractionsCount(List<Long> eventIds) {

        log.info("Получение статистики для {} событий", eventIds.size());

        return eventIds.stream()
                .map(eventId -> RecommendationDto.builder()
                        .eventId(eventId)
                        .score(Optional.ofNullable(userActionRepository.sumWeightsByEventId(eventId)).orElse(0.0))
                        .build())
                .toList();
    }
}
