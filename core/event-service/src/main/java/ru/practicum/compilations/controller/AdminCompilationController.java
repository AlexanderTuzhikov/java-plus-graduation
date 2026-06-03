package ru.practicum.compilations.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilations.service.CompilationService;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationRequest;
import ru.practicum.dto.compilation.UpdateCompilationRequest;

@RestController
@RequestMapping(path = "/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {
    private static final String PATH = "comp-id";
    private final CompilationService compilationService;

    @PostMapping()
    public ResponseEntity<CompilationDto> add(
            @RequestBody @Valid NewCompilationRequest newCompilationRequest) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compilationService.add(newCompilationRequest));
    }

    @PatchMapping("/{comp-id}")
    public ResponseEntity<CompilationDto> update(
            @PathVariable(PATH) @Positive long compId,
            @RequestBody @Valid UpdateCompilationRequest updateCompilationRequest) {

        return ResponseEntity.ok()
                .body(compilationService.update(compId, updateCompilationRequest));
    }

    @DeleteMapping("/{comp-id}")
    public ResponseEntity<Void> delete(
            @PathVariable(PATH) @Positive long compId) {

        compilationService.delete(compId);

        return ResponseEntity.noContent()
                .build();
    }
}