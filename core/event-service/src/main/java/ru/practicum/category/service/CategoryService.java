package ru.practicum.category.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryRequest;
import ru.practicum.dto.category.UpdateCategoryRequest;


public interface CategoryService {
    CategoryDto postCategory(NewCategoryRequest newCategoryRequest);

    void deleteCategory(Long catId);

    CategoryDto patchCategory(Long catId, UpdateCategoryRequest updateCategoryRequest);

    CategoryDto getCategory(Long catId);

    Page<CategoryDto> getCategories(Pageable pageable);
}