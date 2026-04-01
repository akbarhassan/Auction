package com.ga.warehouse.repositories;


import com.ga.warehouse.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    // unique role names
    boolean existsByName(String name);
}
