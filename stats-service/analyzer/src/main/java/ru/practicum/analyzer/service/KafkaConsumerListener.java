package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerListener {
    private final UserActionService userActionService;
    private final EventSimilarityService eventSimilarityService;

    @KafkaListener(topics = "${kafka.topics.user-actions}", groupId = "${spring.kafka.consumer.group-id}")
    public void processUserAction(UserActionAvro action) {
        log.debug("Получено действие пользователя: {}", action);
        userActionService.process(action);
    }

    @KafkaListener(
            topics = "${kafka.topics.events-similarity}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void processEventSimilarity(EventSimilarityAvro similarity) {
        log.debug("Получена похожесть событий: {}", similarity);
        eventSimilarityService.process(similarity);
    }
}
