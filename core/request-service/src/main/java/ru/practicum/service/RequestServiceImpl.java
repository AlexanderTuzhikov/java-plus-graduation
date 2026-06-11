package ru.practicum.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.api.event.EventFeignClient;
import ru.practicum.api.user.UserFeignClient;
import ru.practicum.client.RecommendationGrpcClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestState;
import ru.practicum.dto.user.UserDto;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final UserFeignClient userFeignClient;
    private final EventFeignClient eventFeignClient;
    private final RecommendationGrpcClient recommendationGrpcClient;

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId) {
        log.info("GET requests: user ID={}", userId);

        checkUserExists(userId);

        List<Request> requests = requestRepository.findByRequesterId(userId);
        log.debug("FIND requests: size={}", requests.size());

        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto postRequest(Long userId, Long eventId) {
        checkUserExists(userId);
        EventFullDto event = getEventFullDto(eventId);

        checkDoubleRequest(userId, eventId);
        checkEventInitiator(userId, event);
        checkEventStatus(event);

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);

        if (event.getParticipantLimit() != 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        RequestState status = RequestState.PENDING;

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestState.CONFIRMED;
        }

        Request request = Request.builder()
                .requesterId(userId)
                .eventId(event.getId())
                .status(status)
                .created(LocalDateTime.now())
                .build();

        Request savedRequest = requestRepository.save(request);

        try {
            recommendationGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
            log.info("Sent REGISTER action to recommendation service: userId={}, eventId={}", userId, eventId);
        } catch (Exception e) {
            log.error("Failed to send REGISTER action to recommendation service: {}", e.getMessage(), e);
        }

        return requestMapper.mapToRequestDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto patchRequest(Long userId, Long requestId) {
        log.info("PATCH cancel request ID={} by user ID={}", requestId, userId);

        checkUserExists(userId);
        Request request = checkRequestExists(requestId);

        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new ConflictException("User ID=" + userId + " is not the requester of ID=" + requestId);
        }

        if (request.getStatus().equals(RequestState.CONFIRMED)) {
            throw new ConflictException("Cannot cancel a confirmed request. Status is already CONFIRMED.");
        }

        request.setStatus(RequestState.CANCELED);
        Request patchedRequest = requestRepository.save(request);

        return requestMapper.mapToRequestDto(patchedRequest);
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("GET event ID={} requests", eventId);

        checkUserExists(userId);
        checkEventExists(eventId);

        List<Request> requests = requestRepository.findByEventId(eventId);
        log.info("FIND requests size={} requests", requests.size());

        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult patchEventRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateDto) {
        checkUserExists(userId);
        EventFullDto event = getEventFullDto(eventId);

        List<Long> ids = statusUpdateDto.getRequestIds();
        List<Request> requests = requestRepository.findByIdIn(ids);

        if (requests.isEmpty()) {
            return new EventRequestStatusUpdateResult();
        }

        checkRequestStatusForPatch(requests);

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
        int limit = event.getParticipantLimit();

        if (limit != 0 && confirmedCount >= limit) {
            throw new ConflictException("The participant limit has been reached. Cannot confirm more requests.");
        }

        RequestState newStatus = RequestState.valueOf(statusUpdateDto.getStatus());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Request request : requests) {
            if (newStatus == RequestState.REJECTED) {
                request.setStatus(RequestState.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(request));
            } else if (newStatus == RequestState.CONFIRMED) {

                if (limit == 0 || confirmedCount < limit) {
                    request.setStatus(RequestState.CONFIRMED);
                    confirmedCount++;
                    confirmed.add(requestMapper.mapToRequestDto(request));
                } else {

                    request.setStatus(RequestState.REJECTED);
                    rejected.add(requestMapper.mapToRequestDto(request));
                }
            }
        }

        requestRepository.saveAll(requests);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);

        return result;
    }

    @Override
    public Long countConfirmedRequestsByEventId(Long eventId) {
        log.info("Counting confirmed requests for event ID={}", eventId);

        return requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
    }

    private EventFullDto getEventFullDto(Long eventId) {
        EventFullDto event = eventFeignClient.getEventById(eventId);
        checkEventStatus(event);

        return event;
    }

    private void checkUserExists(Long userId) {
        try {
            UserDto user = userFeignClient.getUserById(userId);
            if (user == null) {
                log.warn("User {} not found", userId);
            }
        } catch (Exception e) {
            log.warn("Пользовательский сервис не доступен {}: {}", userId, e.getMessage());
        }
    }

    private Request checkRequestExists(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Request {} not found", requestId);
                    return new NotFoundException("Request ID=" + requestId + " not found");
                });
    }

    private void checkEventExists(Long eventId) {
        try {
            eventFeignClient.getEventById(eventId);
        } catch (Exception exception) {
            log.warn("Сервис событий не доступен {}: {}", eventId, exception.getMessage());
        }
    }

    private void checkEventInitiator(Long userId, EventFullDto event) {
        if (event.getInitiator().getId().equals(userId)) {
            log.error("User ID={} initiator event ID={}", userId, event.getId());
            throw new ConflictException("Initiator cannot participate in own event");
        }
    }

    private void checkDoubleRequest(Long userId, Long eventId) {
        Optional<Request> request = requestRepository.findByRequesterIdAndEventId(userId, eventId);

        if (request.isPresent()) {
            log.error("Try double request user ID={}, for event ID={}=", userId, eventId);
            throw new ConflictException("Duplicate requests are not allowed.");
        }
    }

    private void checkEventStatus(EventFullDto event) {
        if (event.getState() != EventState.PUBLISHED) {
            log.error("Event ID={} unpublished", event.getId());
            throw new ConflictException("Cannot participate in unpublished event");
        }
    }

    private void checkRequestStatusForPatch(List<Request> requests) {
        for (Request request : requests) {
            if (!request.getStatus().equals(RequestState.PENDING)) {
                log.error("Request ID={} none of the specified requests are in PENDING state", request.getId());
                throw new ConflictException("Cannot change status: none of the specified requests are in PENDING state");
            }
        }
    }
}