package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserActionService {
    private final UserActionRepository userActionRepository;

    private static final Map<ActionTypeAvro, Double> WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    public void process(UserActionAvro action) {Long userId = action.getUserId();Long eventId = action.getEventId();
        double weight = getWeight(action.getActionType());

        log.info("Обработка действия пользователя: userId={}, eventId={}, тип={}, вес={}",
                userId, eventId, action.getActionType(), weight);

        UserAction existingAction = userActionRepository.findByUserIdAndEventId(userId, eventId).orElse(null);

        if (existingAction == null) {
            saveNewAction(action, weight);
            return;
        }

        if (weight > existingAction.getWeight()) {
            updateAction(existingAction, action, weight);
        } else {
            log.debug("Вес действия не изменился: userId={}, eventId={}, текущийВес={}, новыйВес={}",
                    userId, eventId, existingAction.getWeight(), weight);
        }
    }

    private void saveNewAction(UserActionAvro action, double weight) {
        UserAction entity = UserAction.builder()
                .userId(action.getUserId())
                .eventId(action.getEventId())
                .weight(weight)
                .actionType(action.getActionType().name())
                .lastActionTime(toLocalDateTime(action.getTimestamp()))
                .build();

        userActionRepository.save(entity);

        log.info("Сохранено новое действие пользователя: userId={}, eventId={}", entity.getUserId(), entity.getEventId());
    }

    private void updateAction(UserAction existing, UserActionAvro action, double weight) {
        existing.setWeight(weight);
        existing.setActionType(action.getActionType().name());
        existing.setLastActionTime(toLocalDateTime(action.getTimestamp()));

        userActionRepository.save(existing);

        log.info("Обновлено действие пользователя: userId={}, eventId={}, новыйВес={}", existing.getUserId(), existing.getEventId(), weight);
    }

    private double getWeight(ActionTypeAvro actionType) {
        return WEIGHTS.getOrDefault(actionType, throwUnknownAction(actionType));
    }

    private Double throwUnknownAction(ActionTypeAvro actionType) {
        throw new IllegalArgumentException("Неизвестный тип действия: " + actionType);
    }

    private LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}
