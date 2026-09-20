package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.ProductMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAllActive() {
        return productRepository.findByActiveTrue().stream()
            .map(productMapper::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Товар не найден: " + id));
        return productMapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getByCategoryId(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Категория не найдена: " + categoryId);
        }
        return productRepository.findByCategoryId(categoryId).stream()
            .map(productMapper::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> search(String query) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query).stream()
            .map(productMapper::toDto)
            .toList();
    }

    @Override
    @Transactional
    public ProductDto create(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new NotFoundException("Категория не найдена: " + request.categoryId()));

        Product product = productMapper.toEntity(request, category);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductDto update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Товар не найден: " + id));

        Category newCategory = null;
        if (request.categoryId() != null) {
            newCategory = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Категория не найдена: " + request.categoryId()));
        }

        productMapper.updateEntity(product, request, newCategory);
        return productMapper.toDto(productRepository.save(product));
    }
}