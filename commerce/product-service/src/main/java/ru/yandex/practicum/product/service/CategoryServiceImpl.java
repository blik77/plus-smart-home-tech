package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.CategoryMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream()
            .map(categoryMapper::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Категория не найдена: " + id));
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        return categoryMapper.toDto(categoryRepository.save(category));
    }
}