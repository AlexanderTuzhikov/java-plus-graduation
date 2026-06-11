package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventSimilarityService {
    private final EventSimilarityRepository eventSimilarityRepository;

    public void process(EventSimilarityAvro similarity) {
        Long eventA = similarity.getEventA();
        Long eventB = similarity.getEventB();

        log.info("Обработка похожести событий: eventA={}, eventB={}, score={}",
                eventA, eventB, similarity.getScore());

        EventSimilarity entity = eventSimilarityRepository.findByEventAAndEventB(eventA, eventB).orElse(null);

        if (entity == null) {
            saveNewSimilarity(similarity);
            return;
        }

        entity.setScore(similarity.getScore());
        entity.setUpdated(toLocalDateTime(similarity.getTimestamp()));

        eventSimilarityRepository.save(entity);

        log.info("Обновлена похожесть событий: eventA={}, eventB={}, score={}",
                eventA, eventB, similarity.getScore());
    }

    private void saveNewSimilarity(EventSimilarityAvro similarity) {
        EventSimilarity entity = EventSimilarity.builder()
                .eventA(similarity.getEventA())
                .eventB(similarity.getEventB())
                .score(similarity.getScore())
                .updated(toLocalDateTime(similarity.getTimestamp()))
                .build();

        eventSimilarityRepository.save(entity);

        log.info("Сохранена новая похожесть событий: eventA={}, eventB={}",
                entity.getEventA(), entity.getEventB());
    }

    private LocalDateTime toLocalDateTime(Instant timestamp) {
        return LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);
    }
}
