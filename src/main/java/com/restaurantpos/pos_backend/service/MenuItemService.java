package com.restaurantpos.pos_backend.service;

import com.restaurantpos.pos_backend.dto.MenuItemRequest;
import com.restaurantpos.pos_backend.dto.MenuItemResponse;
import com.restaurantpos.pos_backend.entity.MenuItem;
import com.restaurantpos.pos_backend.exception.ResourceNotFoundException;
import com.restaurantpos.pos_backend.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.restaurantpos.pos_backend.exception.BadRequestException;

import java.util.List;

@Service
@RequiredArgsConstructor // Lombok generates a constructor for the final field below (constructor injection)
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    public List<MenuItemResponse> getAll() {
        return menuItemRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MenuItemResponse> search(String keyword) {
        return menuItemRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MenuItemResponse getById(Long id) {
        MenuItem item = findEntityById(id);
        return toResponse(item);
    }

    public MenuItemResponse create(MenuItemRequest request) {
        MenuItem item = new MenuItem();
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        MenuItem saved = menuItemRepository.save(item);
        return toResponse(saved);
    }

    public MenuItemResponse update(Long id, MenuItemRequest request) {
        MenuItem item = findEntityById(id);
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        MenuItem saved = menuItemRepository.save(item);
        return toResponse(saved);
    }

    public void delete(Long id) {
        MenuItem item = findEntityById(id);
        try {
            menuItemRepository.delete(item);
            menuItemRepository.flush(); // force the SQL DELETE to run now, so the exception surfaces here
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BadRequestException(
                "Cannot delete '" + item.getName() + "' because it has already been used in one or more orders");
        }
    }

    // --- helpers ---

    private MenuItem findEntityById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getCreatedAt()
        );
    }
}