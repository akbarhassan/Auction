package com.ga.warehouse.seeders;


import com.ga.warehouse.models.Permission;
import com.ga.warehouse.models.Role;
import com.ga.warehouse.repositories.PermissionRepository;
import com.ga.warehouse.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;


    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting database seeding...");

        seedPermissions();
        seedRoles();

        log.info("Database seeding completed!");

    }

    private void seedPermissions() {
        log.info("Seeding permissions...");

        // Check if permissions already exist
        if (permissionRepository.count() > 0) {
            log.info("Permissions already exist. Skipping permission seeding.");
            return;
        }

        String[] models = {"Auction", "AuctionItem", "Permission", "Role", "User", "Category", "UserProfile", "BID"};

        String[] actions = {"create", "update", "delete", "read"};

        List<Permission> permissions = new ArrayList<>();

        for (String model : models) {
            for (String action : actions) {
                Permission permission = Permission.builder().action(model.toLowerCase() + ":" + action).build();
                permissions.add(permission);
            }
        }

        permissionRepository.saveAll(permissions);
        log.info("Created {} permissions", permissions.size());
    }

    private void seedRoles() {
        log.info("Seeding roles...");

        if (roleRepository.count() > 0) {
            log.info("Roles already exist. Skipping role seeding.");
            return;
        }

        // Get all permissions
        List<Permission> allPermissions = permissionRepository.findAll();
        Map<String, Permission> permissionMap = new HashMap<>();
        for (Permission p : allPermissions) {
            permissionMap.put(p.getAction(), p);
        }

        // Create Admin Role - has all permissions
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setPermissions(new HashSet<>(allPermissions));
        roleRepository.save(adminRole);
        log.info("Created ADMIN role with {} permissions", allPermissions.size());


        // Create Customer Role
        Role customerRole = new Role();
        customerRole.setName("CUSTOMER");
        Set<Permission> customerPermissions = new HashSet<>();

        // Customer permissions
        addPermissionsForModel(customerPermissions, permissionMap, "UserProfile", Arrays.asList("create", "update", "read"));
        addPermissionsForModel(customerPermissions, permissionMap, "BID", Arrays.asList("create", "read"));
        addPermissionsForModel(customerPermissions, permissionMap, "Auction", List.of("read"));
        addPermissionsForModel(customerPermissions, permissionMap, "AuctionItem", List.of("read"));
        addPermissionsForModel(customerPermissions, permissionMap, "Category", List.of("read"));

        roleRepository.save(customerRole);
    }


    private void addPermissionsForModel(Set<Permission> permissions, Map<String, Permission> permissionMap, String model, List<String> actions) {
        for (String action : actions) {
            String key = model + ":" + action;
            Permission permission = permissionMap.get(key);
            if (permission != null) {
                permissions.add(permission);
            }
        }
    }
}
