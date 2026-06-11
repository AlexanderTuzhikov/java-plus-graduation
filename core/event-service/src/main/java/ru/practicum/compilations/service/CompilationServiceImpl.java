package ru.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.api.user.UserFeignClient;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CompilationSearchParam;
import ru.practicum.dto.compilation.NewCompilationRequest;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final UserFeignClient userFeignClient;

    @Override
    @Transactional
    public CompilationDto add(NewCompilationRequest newCompilationRequest) {
        log.info("Adding new compilation: {}", newCompilationRequest.getTitle());

        Compilation compilation = compilationMapper.toCompilation(newCompilationRequest);

        if (newCompilationRequest.getEvents() != null && !newCompilationRequest.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(newCompilationRequest.getEvents()));
            compilation.setEvents(events);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Compilation saved with id: {}", savedCompilation.getId());

        return buildCompilationDto(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto update(long compId, UpdateCompilationRequest updateRequest) {
        log.info("Updating compilation with id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateRequest.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Compilation updated: {}", updatedCompilation.getId());

        return buildCompilationDto(updatedCompilation);
    }

    @Override
    @Transactional
    public void delete(long compId) {
        log.info("Deleting compilation with id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }

        compilationRepository.deleteById(compId);
        log.info("Compilation deleted: {}", compId);
    }

    @Override
    public CompilationDto get(long compId) {
        log.info("Getting compilation with id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        return buildCompilationDto(compilation);
    }

    @Override
    public List<CompilationDto> getCompilations(CompilationSearchParam params) {
        log.info("Getting compilations with params: pinned={}, from={}, size={}",
                params.getPinned(), params.getFrom(), params.getSize());

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        List<Compilation> compilations = compilationRepository.findAllByPinned(params.getPinned(), pageable);

        log.info("Found {} compilations", compilations.size());

        return compilations.stream()
                .map(this::buildCompilationDto)
                .collect(Collectors.toList());
    }

    private CompilationDto buildCompilationDto(Compilation compilation) {
        List<Event> events = new ArrayList<>(compilation.getEvents());

        if (events.isEmpty()) {
            return compilationMapper.toCompilationDto(compilation, Collections.emptyList());
        }

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequests(events);

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserShortDto> initiatorsMap = getInitiatorsMap(initiatorIds);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(event -> {
                    Double rating = 0.0;
                    Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    UserShortDto initiator = initiatorsMap.get(event.getInitiatorId());
                    if (initiator == null) {
                        initiator = new UserShortDto(event.getInitiatorId(), "Unknown User");
                    }
                    return eventMapper.toEventShortDto(event, rating, confirmedRequests, initiator);
                })
                .collect(Collectors.toList());

        return compilationMapper.toCompilationDto(compilation, eventShortDtos);
    }

    private Map<Long, UserShortDto> getInitiatorsMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<UserDto> users = userFeignClient.getUsersByIds(userIds);
            return users.stream()
                    .collect(Collectors.toMap(
                            UserDto::getId,
                            u -> new UserShortDto(u.getId(), u.getName())
                    ));
        } catch (Exception e) {
            log.warn("Failed to get users from user-service: {}", e.getMessage());
            Map<Long, UserShortDto> fallbackMap = new HashMap<>();
            for (Long id : userIds) {
                fallbackMap.put(id, new UserShortDto(id, "Unknown User"));
            }

            return fallbackMap;
        }
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        Map<Long, Long> confirmedRequests = new HashMap<>();

        for (Event event : events) {
            confirmedRequests.put(event.getId(), 0L);
        }
        return confirmedRequests;
    }
}