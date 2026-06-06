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
import ru.practicum.client.StatClient;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
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

import java.time.LocalDateTime;
import java.util.*;
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
    private final StatClient statClient;

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
                    Long views = getViews(event.getId());
                    Long confirmed = getConfirmedRequests(event.getId());
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventShortDto(event, views, confirmed, initiator);
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

        return eventMapper.toEventFullDto(savedEvent, 0L, 0L, initiator);
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        log.info("GET event {} для user: {}",eventId, userId);

        checkUserExists(userId);
        Event event = checkEventExists(eventId);
        UserShortDto userShortDto = getInitiator(userId);
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = getViews(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, views, confirmedRequests, userShortDto);

        log.info("Event {} найден",eventFullDto);

        return eventFullDto;
    }

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        log.info("GET event {} по request: {}",eventId, request);

        Event event = checkEventExists(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event must be published");
        }

        UserShortDto userShortDto = getInitiator(event.getInitiatorId());
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = getViews(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, views, confirmedRequests, userShortDto);

        log.info("Event {} найден",eventFullDto);

        return eventFullDto;
    }

    @Override
    public EventFullDto getEventById(Long eventId) {
        log.info("GET event по id: {}",eventId);

        Event event = checkEventExists(eventId);
        UserShortDto userShortDto = getInitiator(event.getInitiatorId());
        Long confirmedRequests = getConfirmedRequests(eventId);
        Long views = getViews(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(event, views, confirmedRequests, userShortDto);

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
        Long views = getViews(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(eventRepository.save(event), views,
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
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventFullDto(event, getViews(event.getId()),
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
        Long views = getViews(eventId);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(eventRepository.save(event), views,
                confirmedRequests, initiator);

        log.info("Событие обновлено: {}", eventFullDto);

        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, HttpServletRequest request) {
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
            return getEventsSortedByViews(params, start);
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
        Map<Long, Long> viewsMap = getEventsViews(events);

        return events.stream()
                .map(event -> {
                    Long confirmed = getConfirmedRequests(event.getId());
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventShortDto(event, viewsMap.getOrDefault(event.getId(), 0L),
                            confirmed, initiator);
                })
                .collect(Collectors.toList());
    }

    private List<EventShortDto> getEventsSortedByViews(PublicEventParams params, LocalDateTime rangeStart) {
        Pageable allRecords = PageRequest.of(0, Integer.MAX_VALUE);

        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                params.getText(), params.getCategories(), params.getPaid(),
                rangeStart, params.getRangeEnd(), allRecords);

        List<Event> events = eventsPage.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> viewsMap = getEventsViews(events);

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserDto> usersMap = getUsersMap(initiatorIds);

        return events.stream()
                .map(event -> {
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortDto initiator = userDto != null ?
                            new UserShortDto(userDto.getId(), userDto.getName()) :
                            new UserShortDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventShortDto(event, viewsMap.getOrDefault(event.getId(), 0L),
                            getConfirmedRequests(event.getId()), initiator);
                })
                .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                .skip(params.getPageParams().getFrom())
                .limit(params.getPageParams().getSize())
                .collect(Collectors.toList());
    }

    @Override
    public void saveStats(HttpServletRequest request) {
        try {
            statClient.saveHit(new NewEndpointHitDto(
                    "main-service",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики: {}", e.getMessage());
        }
    }

    private Long getViews(Long eventId) {
        Event event = new Event();
        event.setId(eventId);

        Map<Long, Long> map = getViewsBatch(List.of(event));

        return map.getOrDefault(eventId, 0L);
    }

    private Map<Long, Long> getViewsBatch(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now().plusYears(100);

        try {
            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, true);
            Map<Long, Long> result = new HashMap<>();

            for (ViewStatsDto dto : stats) {
                String uri = dto.getUri();

                try {
                    Long id = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                    result.put(id, dto.getHits());
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse event ID from URI: {}", uri);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Error calling Stat Client", e);
            return Collections.emptyMap();
        }
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

    private Map<Long, Long> getEventsViews(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStatsDto> stats = statClient.getStats(
                    LocalDateTime.now().minusYears(10), // Берем большой интервал
                    LocalDateTime.now().plusYears(1),
                    uris,
                    true);

            return stats.stream()
                    .filter(s -> s.getUri().contains("/events/"))
                    .collect(Collectors.toMap(
                            s -> Long.parseLong(s.getUri().substring(s.getUri().lastIndexOf("/") + 1)),
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
            return Collections.emptyMap();
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