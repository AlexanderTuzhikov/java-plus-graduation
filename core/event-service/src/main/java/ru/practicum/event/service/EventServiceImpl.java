package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.api.request.RequestFeignClient;
import ru.practicum.api.user.UserFeignClient;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.RecommendationGrpcClient;
import ru.practicum.dto.event.*;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.stats.proto.ActionTypeProto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserFeignClient userFeignClient;
    private final RequestFeignClient requestFeignClient;
    private final EventMapper eventMapper;
    private final RecommendationGrpcClient recommendationGrpcClient;

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        log.info("GET events для user: {}", userId);

        checkUserExists(userId);
        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Найдено {} events для user: {}", events.getTotalElements(), userId);

        Map<Long, UserDto> usersMap = getUsersMap(events.getContent().stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toList()));

        return events.getContent().stream()
                .map(event -> {
                    Double rating = getEventRating(event.getId());
                    Long confirmed = getConfirmedRequests(event.getId());
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventShortDto(event, rating, confirmed, initiator);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto postEvent(Long userId, NewEventRequest newEventRequest) {
        log.info("POST event {} от user: {}",newEventRequest, userId);

        checkUserExists(userId);
        validateEventDate(newEventRequest.getEventDate(), 2);
        Category category = checkCategoryExists(newEventRequest.getCategory());
        Event event = eventMapper.toEvent(newEventRequest, category, userId);
        Event savedEvent = eventRepository.save(event);

        log.info("Event {} успешно сохранен", savedEvent);

        UserShortDto initiator = getInitiator(userId);
        Double rating = getEventRating(savedEvent.getId());

        return eventMapper.toEventFullDto(savedEvent, rating, 0L, initiator);
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        log.info("GET event {} для user: {}",eventId, userId);

        checkUserExists(userId);
        Event event = checkEventExists(eventId);
        UserShortDto userShortDto = getInitiator(userId);
        Long confirmedRequests = getConfirmedRequests(eventId);
        Double rating = getEventRating(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, rating, confirmedRequests, userShortDto);

        log.info("Event {} найден",eventFullDto);

        return eventFullDto;
    }

    @Override
    public EventFullDto getEventById(Long eventId, Long userId, HttpServletRequest request) {
        log.info("GET event by ID: eventId={}, userId={}", eventId, userId);

        Event event = checkEventExists(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event must be published");
        }

        if (userId != null && userId > 0) {
            try {
                recommendationGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
                log.debug("Sent VIEW action to recommendation service: userId={}, eventId={}", userId, eventId);
            } catch (Exception e) {
                log.error("Failed to send VIEW action to recommendation service: {}", e.getMessage(), e);
            }
        } else {
            log.debug("No userId provided, skipping VIEW action recording");
        }

        UserShortDto userShortDto = getInitiator(event.getInitiatorId());
        Long confirmedRequests = getConfirmedRequests(eventId);
        Double rating = getEventRating(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, rating, confirmedRequests, userShortDto);

        log.info("Event {} найден",eventFullDto);

        return eventFullDto;
    }

    @Override
    public EventFullDto getEventById(Long eventId) {
        log.info("GET event по id: {}",eventId);

        Event event = checkEventExists(eventId);
        UserShortDto userShortDto = getInitiator(event.getInitiatorId());
        Long confirmedRequests = getConfirmedRequests(eventId);
        Double rating = getEventRating(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, rating, confirmedRequests, userShortDto);

        log.info("Event {} найден",eventFullDto);

        return eventFullDto;
    }

    @Override
    @Transactional
    public EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("PATCH event {} от user {}",eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 2);
        }

        updateEventFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocationDto(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateActionUser.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else {
                event.setState(EventState.CANCELED);
            }
        }

        UserShortDto userShortDto = getInitiator(event.getInitiatorId());
        Long confirmedRequests = getConfirmedRequests(eventId);
        Double rating = getEventRating(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(eventRepository.save(event), rating,
                confirmedRequests, userShortDto);

        log.info("Event {} обновлен", eventFullDto);

        return eventFullDto;
    }

    @Override
    public List<EventFullDto> getEventsByAdminFilters(EventParams params) {
        log.info("GER event по фильтрам администратора {}", params);
        Pageable pageable = PageRequest.of(params.getPageParams().getFrom() / params.getPageParams().getSize(),
                params.getPageParams().getSize());

        Page<Event> events = eventRepository.findEventsByAdminFilters(
                params.getUsers(), params.getStates(), params.getCategories(),
                params.getRangeStart(), params.getRangeEnd(), pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Найдено {} events по фильтрам администратора", events.getTotalElements());

        List<Long> initiatorIds = events.getContent().stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserDto> usersMap = getUsersMap(initiatorIds);

        return events.stream()
                .map(event -> {
                    Double rating = getEventRating(event.getId());
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventFullDto(event, rating,
                            getConfirmedRequests(event.getId()), initiator);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("PATCH event {} администратором", eventId);
        Event event = checkEventExists(eventId);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 1);
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish event because it's not in PENDING state");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject event because it's already published");
                }
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());

        UserShortDto initiator = getInitiator(event.getInitiatorId());
        Long confirmedRequests = getConfirmedRequests(eventId);
        Double rating = getEventRating(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(eventRepository.save(event), rating,
                confirmedRequests, initiator);

        log.info("Событие обновлено: {}", eventFullDto);

        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, Long userId, HttpServletRequest request) {
        LocalDateTime start = params.getRangeStart();
        LocalDateTime end = params.getRangeEnd();

        if (start == null && end == null) {
            start = LocalDateTime.now();
        }

        String text = (params.getText() != null && !params.getText().isBlank())
                ? params.getText() : null;

        int pageNum = params.getPageParams().getFrom() / params.getPageParams().getSize();
        Pageable pageable = PageRequest.of(pageNum, params.getPageParams().getSize(),
                Sort.by(Sort.Direction.ASC, "eventDate"));

        if ("VIEWS".equals(params.getSort())) {

            log.debug("Sorting by rating (views)");

            return getEventsSortedByRating(params, start, userId);
        }

        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                text, params.getCategories(), params.getPaid(), start, end, pageable);

        List<Event> events = eventsPage.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserDto> usersMap = getUsersMap(initiatorIds);
        Map<Long, Double> ratingsMap = getEventsRatings(events);

        return events.stream()
                .map(event -> {
                    Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
                    Long confirmed = getConfirmedRequests(event.getId());
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventShortDto(event, rating, confirmed, initiator);
                })
                .collect(Collectors.toList());
    }

    private List<EventShortDto> getEventsSortedByRating(PublicEventParams params, LocalDateTime rangeStart, Long userId) {
        Pageable allRecords = PageRequest.of(0, Integer.MAX_VALUE);

        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                params.getText(), params.getCategories(), params.getPaid(),
                rangeStart, params.getRangeEnd(), allRecords);

        List<Event> events = eventsPage.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> ratingsMap = getEventsRatings(events);

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserDto> usersMap = getUsersMap(initiatorIds);

        return events.stream()
                .map(event -> {
                    Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventShortDto(event, rating,
                            getConfirmedRequests(event.getId()), initiator);
                })
                .sorted(Comparator.comparing(EventShortDto::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .skip(params.getPageParams().getFrom())
                .limit(params.getPageParams().getSize())
                .collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getRecommendationsForUser(Long userId, int size) {
        log.info("GET recommendations for user: userId={}, size={}", userId, size);

        List<Long> recommendedEventIds = recommendationGrpcClient.getRecommendationsForUser(userId, size);

        if (recommendedEventIds.isEmpty()) {
            log.info("No recommendations found for user: {}", userId);
            return Collections.emptyList();
        }

        log.debug("Received {} recommended event IDs for user: {}", recommendedEventIds.size(), userId);

        List<Event> events = eventRepository.findAllById(recommendedEventIds);

        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserDto> usersMap = getUsersMap(initiatorIds);
        Map<Long, Double> ratingsMap = getEventsRatings(events);

        List<EventShortDto> result = recommendedEventIds.stream()
                .filter(eventMap::containsKey)
                .map(eventId -> {
                    Event event = eventMap.get(eventId);
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    Double rating = ratingsMap.getOrDefault(eventId, 0.0);
                    Long confirmed = getConfirmedRequests(eventId);
                    return eventMapper.toEventShortDto(event, rating, confirmed, initiator);
                })
                .collect(Collectors.toList());

        log.info("Returning {} recommendations for user: {}", result.size(), userId);

        return result;
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        log.info("User liking event: userId={}, eventId={}", userId, eventId);

        Event event = checkEventExists(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            log.error("Cannot like unpublished event: eventId={}, state={}", eventId, event.getState());
            throw new BadRequestException("Cannot like unpublished event");
        }

        if (!hasUserInteractedWithEvent(userId, eventId)) {
            log.error("User {} has not interacted with event {}", userId, eventId);
            throw new BadRequestException("User must interact with event before liking");
        }

        try {
            recommendationGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
            log.info("Sent LIKE action to recommendation service: userId={}, eventId={}", userId, eventId);
        } catch (Exception e) {
            log.error("Failed to send LIKE action to recommendation service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process like", e);
        }
    }

    @Override
    public boolean hasUserInteractedWithEvent(Long userId, Long eventId) {
        log.debug("Checking if user interacted with event: userId={}, eventId={}", userId, eventId);

        try {
            Long confirmedRequests = requestFeignClient.countConfirmedRequestsByEventId(eventId);
            Event event = eventRepository.findById(eventId).orElse(null);
            boolean isInitiator = event != null && event.getInitiatorId().equals(userId);
            boolean hasRegistration = confirmedRequests != null && confirmedRequests > 0;
            boolean result = isInitiator || hasRegistration;

            log.debug("User interaction result: userId={}, eventId={}, isInitiator={}, hasRegistration={}, result={}",
                    userId, eventId, isInitiator, hasRegistration, result);

            return result;
        } catch (Exception e) {
            log.warn("Failed to check user interaction for userId={}, eventId={}: {}", userId, eventId, e.getMessage());
            return false;
        }
    }

    private Double getEventRating(Long eventId) {
        try {
            Double rating = recommendationGrpcClient.getInteractionsCount(eventId);
            log.debug("Got rating for eventId={}: {}", eventId, rating);
            return rating != null ? rating : 0.0;
        } catch (Exception e) {
            log.warn("Failed to get rating for eventId={}: {}", eventId, e.getMessage());
            return 0.0;
        }
    }

    private Map<Long, Double> getEventsRatings(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> ratingsMap = new HashMap<>();

        for (Event event : events) {
            Double rating = getEventRating(event.getId());
            ratingsMap.put(event.getId(), rating);
        }

        return ratingsMap;
    }

    private void updateEventFields(Event event, String annotation, Long categoryId,
                                   String description, LocalDateTime eventDate,
                                   LocationDto locationDto, Boolean paid,
                                   Integer participantLimit, Boolean requestModeration, String title) {

        if (annotation != null && !annotation.isBlank()) event.setAnnotation(annotation);

        if (categoryId != null) event.setCategory(checkCategoryExists(categoryId));

        if (description != null && !description.isBlank()) event.setDescription(description);

        if (eventDate != null) event.setEventDate(eventDate);

        if (locationDto != null)

            event.setLocation(new ru.practicum.location.model.LocationEntity(locationDto.getLat(), locationDto.getLon()));

        if (paid != null) event.setPaid(paid);

        if (participantLimit != null) event.setParticipantLimit(participantLimit);

        if (requestModeration != null) event.setRequestModeration(requestModeration);

        if (title != null && !title.isBlank()) event.setTitle(title);
    }

    private void checkUserExists(Long userId) {
        try {
            userFeignClient.getUserById(userId);
        } catch (Exception exception) {
            log.warn("User {} may not exist: {}", userId, exception.getMessage());
        }
    }

    private Category checkCategoryExists(Long catId) {

        return categoryRepository.findById(catId).orElseThrow(() -> new NotFoundException("Category " + catId + " not found"));
    }

    private Event checkEventExists(Long eventId) {

        return eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
    }

    private void validateEventDate(LocalDateTime eventDate, int hours) {

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new BadRequestException("Event date too early");
        }
    }

    private Map<Long, UserDto> getUsersMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<UserDto> users = userFeignClient.getUsersByIds(userIds);

            return users.stream().collect(Collectors.toMap(UserDto::getId, u -> u));
        } catch (Exception e) {
            log.warn("Failed to get users from user-service: {}", e.getMessage());
            Map<Long, UserDto> fallbackMap = new HashMap<>();

            for (Long id : userIds) {
                UserDto fallbackUser = UserDto.builder()
                        .email("unknown")
                        .id(-1L)
                        .name("unknown")
                        .build();

                fallbackMap.put(id, fallbackUser);
            }

            return fallbackMap;
        }
    }

    private UserShortDto getInitiator(Long initiatorId) {
        UserDto userDto = userFeignClient.getUserById(initiatorId);

        log.info("Feign returned user: {}", userDto);

        return userDto != null ?
                new UserShortDto(userDto.getId(), userDto.getName()) :
                new UserShortDto(initiatorId, "Unknown User");
    }

    private Long getConfirmedRequests(Long eventId) {
        try {
            return requestFeignClient.countConfirmedRequestsByEventId(eventId);
        } catch (Exception e) {
            log.warn("Request service unavailable for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }
}