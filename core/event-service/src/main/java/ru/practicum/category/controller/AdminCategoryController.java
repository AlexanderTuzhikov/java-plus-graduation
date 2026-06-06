package ru.practicum.category.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.service.CategoryService;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryRequest;
import ru.practicum.dto.category.UpdateCategoryRequest;

@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/admin/categories")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDto> postCategory(
            @Valid @RequestBody NewCategoryRequest newCategoryRequest) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.postCategory(newCategoryRequest));
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable("catId") Long catId) {

        categoryService.deleteCategory(catId);

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> patchCategory(
            @PathVariable("catId") Long catId,
            @Valid @RequestBody UpdateCategoryRequest updateCategoryRequest) {

        return ResponseEntity.ok()
                .body(categoryService.patchCategory(catId, updateCategoryRequest));
    }
}