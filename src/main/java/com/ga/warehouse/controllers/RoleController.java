package com.ga.warehouse.controllers;

import com.ga.warehouse.models.Permission;
import com.ga.warehouse.models.Role;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.RoleService;
import com.ga.warehouse.utils.ResponseBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     *
     * @param role
     * @return
     */
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<SuccessResponse> createRole(@Valid @RequestBody Role role) {
        Role createdRole = roleService.createRole(role);
        return ResponseBuilder.success(HttpStatus.CREATED, "Role created successfully", createdRole);
    }

    /**
     *
     * @return
     */
    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<SuccessResponse> getAllRoles() {
        List<Role> allRoles = roleService.getAllRoles();
        return ResponseBuilder.success(HttpStatus.OK, "All roles retrieved successfully", allRoles);
    }

    /**
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<SuccessResponse> getRoleById(@PathVariable Long id) {
        Role role = roleService.findRoleById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Role retrieved successfully", role);
    }

    /**
     *
     * @param id
     * @param role
     * @return
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public ResponseEntity<SuccessResponse> updateRole(@PathVariable Long id, @Valid @RequestBody Role role) {
        Role updatedRole = roleService.updateRole(id, role);
        return ResponseBuilder.success(HttpStatus.OK, "Role updated successfully", updatedRole);
    }

    /**
     *
     * @param id
     * @param permissionIds
     * @return
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:update')")
    public ResponseEntity<SuccessResponse> addPermission(@PathVariable Long id, @RequestBody
    @NotEmpty(message = "At least one permission ID is required") Set<Long> permissionIds) {
        Role updatedRole = roleService.addPermissionsToRole(id, permissionIds);
        return ResponseBuilder.success(HttpStatus.OK, "Permissions added successfully", updatedRole);
    }

    /**
     *
     * @param id
     * @param permissionId
     * @return
     */
    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<SuccessResponse> removePermission(
            @PathVariable Long id,
            @PathVariable Long permissionId) {

        Role updatedRole = roleService.removePermissionFromRole(id, permissionId);
        return ResponseBuilder.success(HttpStatus.OK, "Permission removed successfully", updatedRole);
    }

    /**
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<SuccessResponse> deleteRole(@PathVariable Long id) {
        roleService.deleteRoleById(id);
        return ResponseBuilder.success(HttpStatus.OK, "Role deleted successfully", null);
    }
}
