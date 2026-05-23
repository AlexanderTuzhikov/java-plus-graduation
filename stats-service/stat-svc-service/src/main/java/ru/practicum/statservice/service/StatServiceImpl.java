package ru.practicum.statservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.mapper.EndpointHitMapper;
import ru.practicum.statservice.model.EndpointHit;
import ru.practicum.statservice.repository.StatRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatServiceImpl implements StatService {
    private final StatRepository repository;
    private final EndpointHitMapper mapper;

    @Override
    @Transactional
    public void saveHit(NewEndpointHitDto hitDto) {
        EndpointHit hit = mapper.mapToEndpointHit(hitDto);
        repository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris,
                                       boolean unique) {
        if (uris == null || uris.isEmpty()) {

            if (unique) {
                return repository.findUniqueHitsAll(start, end);
            } else {
                return repository.findAllHitsAll(start, end);
            }
        } else {

            if (unique) {
                return repository.findUniqueHitsByUris(start, end, uris);
            } else {
                return repository.findAllHitsByUris(start, end, uris);
            }
        }
    }
}