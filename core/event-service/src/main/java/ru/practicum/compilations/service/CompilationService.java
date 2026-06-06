package ru.practicum.compilations.service;

import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CompilationSearchParam;
import ru.practicum.dto.compilation.NewCompilationRequest;
import ru.practicum.dto.compilation.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto add(NewCompilationRequest newCompilationRequest);

    CompilationDto update(long compId, UpdateCompilationRequest updateCompilationRequest);

    CompilationDto get(long compId);

    List<CompilationDto> getCompilations(CompilationSearchParam params);

    void delete(long compId);
}