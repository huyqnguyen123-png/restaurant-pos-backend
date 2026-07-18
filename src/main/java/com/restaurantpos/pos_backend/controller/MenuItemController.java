package com.restaurantpos.pos_backend.controller;

import com.restaurantpos.pos_backend.dto.MenuItemRequest;
import com.restaurantpos.pos_backend.dto.MenuItemResponse;
import com.restaurantpos.pos_backend.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    // GET /api/menu-items          -> list all menu items
    // GET /api/menu-items?search=x -> search by name (used for the "add item" search box)
    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getAll(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(menuItemService.search(search));
        }
        return ResponseEntity.ok(menuItemService.getAll());
    }

    // GET /api/menu-items/{id} -> get one menu item
    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(menuItemService.getById(id));
    }

    // POST /api/menu-items -> create a new menu item
    @PostMapping
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        MenuItemResponse created = menuItemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/menu-items/{id} -> update an existing menu item
    @PutMapping("/{id}")
    public ResponseEntity<MenuItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuItemService.update(id, request));
    }

    // DELETE /api/menu-items/{id} -> remove a menu item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        menuItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}