package com.ga.warehouse.services;


import com.ga.warehouse.exceptions.ResourceAlreadyExistsException;
import com.ga.warehouse.exceptions.ResourceNotFoundException;
import com.ga.warehouse.models.Permission;
import com.ga.warehouse.repositories.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    // ============ TEST DATA ============
    private Permission permission1;
    private Permission permission2;

    // ============ SETUP ============
    @BeforeEach
    void setUp() {
        // Create test data
        permission1 = new Permission();
        permission1.setId(1L);
        permission1.setAction("user:create");

        permission2 = new Permission();
        permission2.setId(2L);
        permission2.setAction("user:delete");
    }

    // ============ TEST: findAllPermissions ============
    @Test
    @DisplayName("findAllPermissions: returns list of all permissions")
    void findAllPermissions_returnsAllPermissions() {
        List<Permission> expectedPermissions = Arrays.asList(permission1, permission2);

        when(permissionRepository.findAll()).thenReturn(expectedPermissions);

        // call service
        List<Permission> result = permissionService.findAllPermissions();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(permission1, permission2);

        // Verify the repository method was called exactly once
        verify(permissionRepository, times(1)).findAll();
        verifyNoMoreInteractions(permissionRepository);
    }

    // ============ TEST: findPermissionById ============
    @Test
    @DisplayName("findPermissionById: returns a permission based on ID")
    void findPermissionById_returnsPermission() {
        Permission expectedPermission = permission1;

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));

        Permission result = permissionService.findPermissionById(1L);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedPermission);
        verify(permissionRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("findPermissionById: non-existing ID → throws ResourceNotFoundException")
    void findPermissionById_nonExistingId_throwsResourceNotFoundException() {

        when(permissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.findPermissionById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Permission not found");

        verify(permissionRepository, times(1)).findById(999L);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("createPermission: unique action -> saves and returns permission")
    void createPermission_uniqueAction_savesAndReturnsPermission() {
        Permission newPermission = new Permission();
        newPermission.setAction("user:update");

        when(permissionRepository.existsByAction("user:update")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenAnswer(i -> i.getArgument(0));

        Permission result = permissionService.createPermission(newPermission);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo("user:update");

        verify(permissionRepository, times(1)).existsByAction("user:update");
        verify(permissionRepository, times(1)).save(newPermission);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("createPermission: null action -> still attempts to save")
    void createPermission_nullAction_attemptsToSave() {
        Permission newPermission = new Permission();
        newPermission.setAction(null);

        when(permissionRepository.save(any(Permission.class))).thenAnswer(i -> i.getArgument(0));

        Permission result = permissionService.createPermission(newPermission);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isNull();

        verify(permissionRepository, times(1)).save(newPermission);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("createPermission: duplicate action → throws ResourceAlreadyExistsException")
    void createPermission_duplicateAction_throwsResourceAlreadyExistsException() {
        Permission duplicatePermission = new Permission();
        duplicatePermission.setAction("user:create");

        when(permissionRepository.existsByAction("user:create")).thenReturn(true);

        assertThatThrownBy(() -> permissionService.createPermission(duplicatePermission))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Permission already exists");

        verify(permissionRepository, times(1)).existsByAction("user:create");
        verify(permissionRepository, never()).save(any(Permission.class));
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("updatePermission: valid action → updates and returns permission")
    void updatePermission_validAction_updatesAndReturnsPermission() {
        Permission updateRequest = new Permission();
        updateRequest.setAction("user:update");

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(i -> i.getArgument(0));

        Permission result = permissionService.updatePermission(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAction()).isEqualTo("user:update");

        verify(permissionRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).save(permission1);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("updatePermission: same action → no change but still saves")
    void updatePermission_sameAction_noChange() {
        Permission updateRequest = new Permission();
        updateRequest.setAction("user:create");

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(i -> i.getArgument(0));

        Permission result = permissionService.updatePermission(1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo("user:create");

        verify(permissionRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).save(permission1);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("updatePermission: non-existing ID → throws ResourceNotFoundException")
    void updatePermission_nonExistingId_throwsResourceNotFoundException() {
        Permission updateRequest = new Permission();
        updateRequest.setAction("auction:create");

        when(permissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.updatePermission(999L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Not found");

        verify(permissionRepository, times(1)).findById(999L);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("updatePermission: duplicate action on save → throws ResourceAlreadyExistsException")
    void updatePermission_duplicateActionOnSave_throwsResourceAlreadyExistsException() {
        Permission updateRequest = new Permission();
        updateRequest.setAction("role:delete");

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(permissionRepository.save(any(Permission.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> permissionService.updatePermission(1L, updateRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Permission action must be unique");

        verify(permissionRepository, times(1)).findById(1L);
        verify(permissionRepository, times(1)).save(permission1);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("deletePermissionById: existing ID → deletes permission")
    void deletePermissionById_existingId_deletesPermission() {
        when(permissionRepository.existsById(1L)).thenReturn(true);
        
        permissionService.deletePermissionById(1L);

        verify(permissionRepository, times(1)).existsById(1L);
        verify(permissionRepository, times(1)).deleteById(1L);
        verifyNoMoreInteractions(permissionRepository);
    }

    @Test
    @DisplayName("deletePermissionById: missing ID → throws ResourceNotFoundException")
    void deletePermissionById_missingId_throwsResourceNotFoundException() {
        when(permissionRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> permissionService.deletePermissionById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Permission not found");

        verify(permissionRepository, times(1)).existsById(999L);
        verify(permissionRepository, never()).deleteById(anyLong());
        verifyNoMoreInteractions(permissionRepository);
    }
}
