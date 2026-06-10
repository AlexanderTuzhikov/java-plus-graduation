package ru.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Map;

@Slf4j
@Service
public class AggregatorService {
    private static final Map<ActionTypeAvro, Double> WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    private final SimilarityStorage storage;
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity:stats.events-similarity.v1}")
    private String eventsSimilarityTopic;

    public AggregatorService(SimilarityStorage storage, KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate) {
        this.storage = storage;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${kafka.topics.user-actions:stats.user-actions.v1}", groupId = "aggregator-group")
    public void processUserAction(UserActionAvro action) {
        Long userId = action.getUserId();
        Long eventId = action.getEventId();
        double newWeight = getWeight(action.getActionType());

        log.info("Обработка действия пользователя: userId={}, eventId={}, тип={}, вес={}",
                userId, eventId, action.getActionType(), newWeight);

        Double oldWeight =
                storage.getUserEventWeight(userId, eventId);

        if (oldWeight != null && newWeight <= oldWeight) {

            log.debug("Вес не изменился: userId={}, eventId={}, старыйВес={}, новыйВес={}",
                    userId, eventId, oldWeight, newWeight);

            return;
        }

        storage.saveUserEventWeight(userId, eventId, newWeight);
        double deltaWeight = (oldWeight == null)
                ? newWeight
                : (newWeight - oldWeight);
        storage.incrementEventTotalWeight(eventId, deltaWeight);
        recalculateSimilarities(eventId, userId, oldWeight, newWeight);
    }

    private void recalculateSimilarities(Long updatedEvent, Long userId, Double oldWeight, Double newWeight) {
        Map<Long, Double> userEvents =
                storage.getUserEvents(userId);

        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {

            Long otherEvent = entry.getKey();

            if (otherEvent.equals(updatedEvent)) {
                continue;
            }

            double otherWeight = entry.getValue();
            EventPair pair = EventPair.of(updatedEvent, otherEvent);
            double oldPairMin = (oldWeight == null)
                    ? 0.0
                    : Math.min(oldWeight, otherWeight);
            double newPairMin = Math.min(newWeight, otherWeight);
            double deltaMin = newPairMin - oldPairMin;

            if (deltaMin == 0) {
                continue;
            }

            storage.incrementMinWeightSum(pair.eventA(), pair.eventB(), deltaMin);

            double similarity = calculateSimilarity(pair.eventA(), pair.eventB());

            sendSimilarityUpdate(pair.eventA(), pair.eventB(), similarity);
        }
    }

    private double calculateSimilarity(Long eventA, Long eventB) {
        Double totalWeightA = storage.getEventTotalWeight(eventA);
        Double totalWeightB = storage.getEventTotalWeight(eventB);
        Double sMin = storage.getMinWeightSum(eventA, eventB);

        if (totalWeightA == null
                || totalWeightB == null
                || sMin == null
                || totalWeightA == 0
                || totalWeightB == 0) {
            return 0.0;
        }

        double similarity = sMin / (Math.sqrt(totalWeightA) * Math.sqrt(totalWeightB));

        log.debug("Рассчитана похожесть событий ({}, {}): {}",
                eventA, eventB, similarity);

        return similarity;
    }

    private void sendSimilarityUpdate(Long eventA, Long eventB, double similarity) {
        EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
                        .setEventA(eventA)
                        .setEventB(eventB)
                        .setScore(similarity)
                        .setTimestamp(System.currentTimeMillis())
                        .build();

        kafkaTemplate.send(eventsSimilarityTopic, String.valueOf(eventA), message);

        log.debug("Отправлено обновление похожести в Kafka: eventA={}, eventB={}, похожесть={}",
                eventA, eventB, similarity);
    }

    private double getWeight(ActionTypeAvro actionType) {
        return WEIGHTS.getOrDefault(actionType, 0.0);
    }

    private record EventPair(Long eventA, Long eventB) {
        static EventPair of(Long first, Long second) {
            return (first < second)
                    ? new EventPair(first, second)
                    : new EventPair(second, first);
        }
    }
}