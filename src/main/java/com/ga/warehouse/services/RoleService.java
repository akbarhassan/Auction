package com.ga.warehouse.services;


import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.Permission;
import com.ga.warehouse.models.Role;
import com.ga.warehouse.repositories.PermissionRepository;
import com.ga.warehouse.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public Role createRole(Role role) {
        if (roleRepository.existsByName(role.getName())) {
            throw new ResourceAlreadyExistsException(role.getName());
        }
        return roleRepository.save(role);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role findRoleById(Long roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("No role exists with current id"));
    }

    public Role updateRole(Long roleId, Role role) {
        Role existingRole = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("No role exists with current id : " + roleId));
        if (role.getName() != null && !role.getName().isBlank()) {
            if (!role.getName().equals(existingRole.getName())) {
                if (roleRepository.existsByName(role.getName())) {
                    throw new ResourceAlreadyExistsException(role.getName());
                }
            }
        }
        existingRole.setName(role.getName());

        // update permissions
        if (role.getPermissions() != null) {
            existingRole.setPermissions(role.getPermissions());
        }

        return roleRepository.save(existingRole);
    }


    @Transactional
    public Role addPermissionsToRole(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("No role exists with id: " + roleId));

        for (Long permId : permissionIds) {
            Permission permission = permissionRepository.findById(permId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission with ID " + permId + " not found"));
            role.getPermissions().add(permission);
        }
        return roleRepository.save(role);
    }

    @Transactional
    public Role removePermissionFromRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id: " + roleId + " not found"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission with id: " + permissionId + " not found"));

        role.getPermissions().remove(permission); // now works with @EqualsAndHashCode.Include
        return roleRepository.save(role);
    }

    public void deleteRoleById(Long roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role with id : " + roleId + " not found");
        }
        roleRepository.deleteById(roleId);
    }
}
