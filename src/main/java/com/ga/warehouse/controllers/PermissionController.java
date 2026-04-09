package com.ga.warehouse.controllers;


import com.ga.warehouse.models.Permission;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.PermissionService;
import com.ga.warehouse.utils.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/permissions")
public class PermissionController {

    private PermissionService permissionService;

    @Autowired
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     *
     * @param permission
     * @return
     */
    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    public ResponseEntity<SuccessResponse> createPermission(@RequestBody Permission permission) {
        Permission createdPermission = permissionService.createPermission(permission);
        return ResponseBuilder.success(HttpStatus.CREATED, "Permission created successfully", createdPermission);

    }

    /**
     *
     * @return
     */
    @GetMapping
    @PreAuthorize("hasAuthority('permission:view')")
    public ResponseEntity<SuccessResponse> getAllPermissions() {
        List<Permission> allPermissions = permissionService.findAllPermissions();
        return ResponseBuilder.success(HttpStatus.OK, "All Permissions retrieved successfully", allPermissions);
    }

    /**
     *
     * @param permissionId
     * @return
     */
    @GetMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('permission:view')")
    public ResponseEntity<SuccessResponse> getPermissionById(@PathVariable("permissionId") Long permissionId) {
        Permission permission = permissionService.findPermissionById(permissionId);
        return ResponseBuilder.success(HttpStatus.OK, "Permission retrieved successfully", permission);
    }

    /**
     *
     * @param permissionId
     * @param permission
     * @return
     */
    @PutMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('permission:update')")
    public ResponseEntity<SuccessResponse> updatePermission(@PathVariable("permissionId") Long permissionId, @RequestBody Permission permission) {
        Permission updatedPermission = permissionService.updatePermission(permissionId, permission);
        return ResponseBuilder.success(HttpStatus.OK, "Permission updated successfully", updatedPermission);
    }

    /**
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public ResponseEntity<SuccessResponse> deletePermissionById(@PathVariable Long id) {
        permissionService.deletePermissionById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Role deleted successfully", null);

    }


}
