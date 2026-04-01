package com.ga.warehouse.repositories;


import com.ga.warehouse.models.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByAction(String action);

    boolean existsByAction(String action);
}
