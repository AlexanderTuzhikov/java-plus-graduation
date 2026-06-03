package ru.practicum.category.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.service.CategoryService;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/categories")
public class PublicCategoryController{
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(name = "from", defaultValue = "0")
            @PositiveOrZero Integer from,
            @RequestParam(name = "size", defaultValue = "10")
            @Positive Integer size) {

        int page = from / size;

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        return ResponseEntity.ok(
                categoryService.getCategories(pageable).getContent()
        );
    }

    @GetMapping("/{catId}")
    public ResponseEntity<CategoryDto> getCategory(
            @PathVariable("catId") Long catId) throws NotFoundException {

        try {
            return ResponseEntity.ok()
                    .body(categoryService.getCategory(catId));
        } catch (RuntimeException ex) {
            throw new NotFoundException(ex.getMessage());
        }
    }
}