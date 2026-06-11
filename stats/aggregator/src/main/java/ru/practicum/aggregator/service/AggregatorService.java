package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    private final AggregationStorage storage;
    private final SimilarityCalculator similarityCalculator;
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity}")
    private String eventsSimilarityTopic;

    @KafkaListener(topics = "${kafka.topics.user-actions}", groupId = "${spring.kafka.consumer.group-id}")
    public void processUserAction(UserActionAvro action) {
        Long userId = action.getUserId();
        Long eventId = action.getEventId();
        double newWeight = getWeight(action.getActionType());

        log.info("Обработка действия пользователя: userId={}, eventId={}, типДействия={}",
                userId, eventId, action.getActionType()
        );

        Map<Long, Double> userEvents = storage.getUserEventWeights()
                .computeIfAbsent(userId, id -> new ConcurrentHashMap<>());
        Double oldWeight = userEvents.get(eventId);

        if (oldWeight != null && newWeight <= oldWeight) {

            log.debug("Вес события не изменился: userId={}, eventId={}", userId, eventId);

            return;
        }

        userEvents.put(eventId, newWeight);

        double deltaWeight = oldWeight == null
                        ? newWeight
                        : newWeight - oldWeight;

        storage.getEventTotalWeights()
                .merge(eventId, deltaWeight, Double::sum);

        if (oldWeight == null) {
            processNewEvent(userId, eventId, newWeight);
        } else {
            processUpdatedEvent(userId, eventId, oldWeight, newWeight);
        }
    }

    private void processNewEvent(Long userId, Long updatedEvent, double newWeight) {
        Map<Long, Double> userEvents = storage.getUserEventWeights().get(userId);

        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {
            Long otherEvent = entry.getKey();
            if (otherEvent.equals(updatedEvent)) {
                continue;
            }

            updatePairSimilarity(updatedEvent, otherEvent, Math.min(newWeight, entry.getValue()));
        }
    }

    private void processUpdatedEvent(Long userId, Long updatedEvent, double oldWeight, double newWeight) {
        Map<Long, Double> userEvents =
                storage.getUserEventWeights().get(userId);

        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {

            Long otherEvent = entry.getKey();

            if (otherEvent.equals(updatedEvent)) {
                continue;
            }

            double deltaMin = Math.min(newWeight, entry.getValue()) - Math.min(oldWeight, entry.getValue());

            if (deltaMin == 0) {
                continue;
            }

            updatePairSimilarity(updatedEvent, otherEvent, deltaMin);
        }
    }

    private void updatePairSimilarity(Long firstEvent, Long secondEvent, double deltaMin) {
        EventPair pair = buildPair(firstEvent, secondEvent);

        storage.getMinWeightsSums()
                .computeIfAbsent(pair.eventA(), id -> new ConcurrentHashMap<>())
                .merge(pair.eventB(), deltaMin, Double::sum);

        double similarity = similarityCalculator.calculate(pair.eventA(), pair.eventB());

        sendSimilarityUpdate(pair.eventA(), pair.eventB(), similarity);
    }

    private void sendSimilarityUpdate(Long eventA, Long eventB, double similarity) {
        EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                        .setEventA(eventA)
                        .setEventB(eventB)
                        .setScore(similarity)
                        .setTimestamp(System.currentTimeMillis())
                        .build();

        kafkaTemplate.send(eventsSimilarityTopic, String.valueOf(eventA), similarityAvro
        );

        log.debug("Отправлено сообщение о похожести событий: eventA={}, eventB={}, коэффициент={}",
                eventA, eventB, similarity);
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }

    private EventPair buildPair(Long firstEvent, Long secondEvent) {
        return new EventPair(Math.min(firstEvent, secondEvent), Math.max(firstEvent, secondEvent));
    }

    private record EventPair(Long eventA, Long eventB) {
    }
}