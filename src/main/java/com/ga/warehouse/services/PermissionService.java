package com.ga.warehouse.services;

import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.Permission;
import com.ga.warehouse.repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public Permission createPermission(Permission permission) {
        if (permission.getAction() != null && permissionRepository.existsByAction(permission.getAction()))
            throw new ResourceAlreadyExistsException("Permission already exists");
        return permissionRepository.save(permission);
    }


    public List<Permission> findAllPermissions() {
        return permissionRepository.findAll();
    }

    public Permission findPermissionById(Long permissionId) {
        return permissionRepository.findById(permissionId).orElseThrow(
                () -> new ResourceNotFoundException("Permission not found")
        );
    }

    @Transactional
    public Permission updatePermission(Long id, Permission updates) {
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found"));

        if (updates.getAction() != null && !updates.getAction().equals(existing.getAction())) {
            existing.setAction(updates.getAction());
        }

        try {
            return permissionRepository.save(existing);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("Permission action must be unique");
        }
    }

    public void deletePermissionById(Long permissionId) {
        if (!permissionRepository.existsById(permissionId)) {
            throw new ResourceNotFoundException("Permission not found");
        }
        permissionRepository.deleteById(permissionId);
    }


}
