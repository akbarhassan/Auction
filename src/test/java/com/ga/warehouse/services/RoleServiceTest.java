package com.ga.warehouse.services;


import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.Permission;
import com.ga.warehouse.models.Role;
import com.ga.warehouse.repositories.PermissionRepository;
import com.ga.warehouse.repositories.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleService roleService;

    // ============ TEST DATA ============
    private Role role1;
    private Role role2;
    private Permission permission1;
    private Permission permission2;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        // Create test data
        role1 = new Role();
        role1.setId(1L);
        role1.setName("ADMIN");
        role1.setDescription("Administrator role");
        role1.setPermissions(new HashSet<>());

        role2 = new Role();
        role2.setId(2L);
        role2.setName("USER");
        role2.setDescription("User role");
        role2.setPermissions(new HashSet<>());

        permission1 = new Permission();
        permission1.setId(1L);
        permission1.setAction("CREATE_USER");

        permission2 = new Permission();
        permission2.setId(2L);
        permission2.setAction("DELETE_USER");
    }

    // ============ TEST: getAllRoles ============
    @Test
    @DisplayName("getAllRoles: returns list of all roles")
    void getAllRoles_returnsAllRoles() {
        List<Role> expectedRoles = Arrays.asList(role1, role2);

        when(roleRepository.findAll()).thenReturn(expectedRoles);

        // call service
        List<Role> result = roleService.getAllRoles();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(role1, role2);

        // Verify the repository method was called exactly once
        verify(roleRepository, times(1)).findAll();
        verifyNoMoreInteractions(roleRepository);
    }

    // ============ TEST: findRoleById ============
    @Test
    @DisplayName("findRoleById: returns a role based on ID")
    void findRoleById_returnsRole() {
        Role expectedRole = role1;

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role1));

        Role result = roleService.findRoleById(1L);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedRole);
        verify(roleRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("findRoleById: non-existing ID → throws ResourceNotFoundException")
    void findRoleById_nonExistingId_throwsResourceNotFoundException() {

        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.findRoleById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No role exists with current id");

        verify(roleRepository, times(1)).findById(999L);
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("createRole: unique name -> saves and returns role")
    void createRole_uniqueName_savesAndReturnsRole() {
        Role newRole = new Role();
        newRole.setName("MANAGER");
        newRole.setDescription("Manager role");

        when(roleRepository.existsByName("MANAGER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        Role result = roleService.createRole(newRole);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("MANAGER");
        assertThat(result.getDescription()).isEqualTo("Manager role");

        verify(roleRepository, times(1)).existsByName("MANAGER");
        verify(roleRepository, times(1)).save(newRole);
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("createRole: duplicate name → throws ResourceAlreadyExistsException")
    void createRole_duplicateName_throwsResourceAlreadyExistsException() {
        Role duplicateRole = new Role();
        duplicateRole.setName("ADMIN");

        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> roleService.createRole(duplicateRole))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("ADMIN");

        verify(roleRepository, times(1)).existsByName("ADMIN");
        verify(roleRepository, never()).save(any(Role.class));
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("updateRole: valid name + description → updates and returns role")
    void updateRole_validNameAndDescription_updatesAndReturnsRole() {
        Role updateRequest = new Role();
        updateRequest.setName("ADMIN_NEW");
        updateRequest.setDescription("Updated admin role");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(roleRepository.existsByName("ADMIN_NEW")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        Role result = roleService.updateRole(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("ADMIN_NEW");

        verify(roleRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).existsByName("ADMIN_NEW");
        verify(roleRepository, times(1)).save(role1);
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("updateRole: duplicate name → throws ResourceAlreadyExistsException")
    void updateRole_duplicateName_throwsResourceAlreadyExistsException() {
        Role duplicateRole = new Role();
        duplicateRole.setName("USER");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(roleRepository.existsByName("USER")).thenReturn(true);

        assertThatThrownBy(() -> roleService.updateRole(1L, duplicateRole))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("USER");

        verify(roleRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).existsByName("USER");
        verify(roleRepository, never()).save(any(Role.class));
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("addPermissionsToRole: valid role and permissions → adds permissions to role")
    void addPermissionsToRole_validRoleAndPermissions_addsPermissionsToRole() {
        Set<Long> permissionIds = new HashSet<>(Arrays.asList(1L, 2L));

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission2));
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        Role result = roleService.addPermissionsToRole(1L, permissionIds);

        assertThat(result).isNotNull();
        assertThat(result.getPermissions()).contains(permission1, permission2);

        verify(roleRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).findById(2L);
        verify(roleRepository, times(1)).save(role1);
        verifyNoMoreInteractions(roleRepository, permissionRepository);
    }

    @Test
    @DisplayName("addPermissionsToRole: non-existing role → throws ResourceNotFoundException")
    void addPermissionsToRole_nonExistingRole_throwsResourceNotFoundException() {
        Set<Long> permissionIds = new HashSet<>(Arrays.asList(1L));

        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.addPermissionsToRole(999L, permissionIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No role exists with id: 999");

        verify(roleRepository, times(1)).findById(999L);
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("addPermissionsToRole: non-existing permission → throws ResourceNotFoundException")
    void addPermissionsToRole_nonExistingPermission_throwsResourceNotFoundException() {
        Set<Long> permissionIds = new HashSet<>(Arrays.asList(999L));

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(permissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.addPermissionsToRole(1L, permissionIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Permission with ID 999 not found");

        verify(roleRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).findById(999L);
        verifyNoMoreInteractions(roleRepository, permissionRepository);
    }

    @Test
    @DisplayName("removePermissionFromRole: valid role and permission → removes permission from role")
    void removePermissionFromRole_validRoleAndPermission_removesPermissionFromRole() {
        role1.getPermissions().add(permission1);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role1));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        Role result = roleService.removePermissionFromRole(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getPermissions()).doesNotContain(permission1);

        verify(roleRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).save(role1);
        verifyNoMoreInteractions(roleRepository, permissionRepository);
    }

    @Test
    @DisplayName("deleteRoleById: existing ID → deletes role")
    void deleteRoleById_existingId_deletesRole() {
        when(roleRepository.existsById(1L)).thenReturn(true);
        
        roleService.deleteRoleById(1L);

        verify(roleRepository, times(1)).existsById(1L);
        verify(roleRepository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("deleteRoleById: missing ID → throws ResourceNotFoundException")
    void deleteRoleById_missingId_throwsResourceNotFoundException() {
        when(roleRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> roleService.deleteRoleById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role with id : 999 not found");

        verify(roleRepository, times(1)).existsById(999L);
        verify(roleRepository, never()).deleteById(anyLong());
        verifyNoMoreInteractions(roleRepository);
    }
}
