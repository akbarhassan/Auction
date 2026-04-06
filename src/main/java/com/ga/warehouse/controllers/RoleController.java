package com.ga.warehouse.controllers;


import com.ga.warehouse.models.Role;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.RoleService;
import com.ga.warehouse.utils.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/roles")
public class RoleController {
    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse> createRole(@Valid @RequestBody Role role) {
        Role createdRole = roleService.createRole(role);
        return ResponseBuilder.success(HttpStatus.CREATED, "Role created successfully", createdRole);
    }

    @GetMapping
    public ResponseEntity<SuccessResponse> getAllRoles() {
        List<Role> allRoles = roleService.getAllRoles();
        return ResponseBuilder.success(HttpStatus.OK, "All roles retrieved successfully", allRoles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse> getRoleById(@PathVariable Long id) {
        Role role = roleService.findRoleById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Role retrieved successfully", role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse> deleteRole(@PathVariable Long id) {
        roleService.deleteRoleById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Role deleted successfully", null);
    }
}
