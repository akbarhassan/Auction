package com.ga.warehouse.controllers;


import com.ga.warehouse.models.User;
import com.ga.warehouse.response.SuccessResponse;
import com.ga.warehouse.services.UserService;
import com.ga.warehouse.utils.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }


    /**
     *
     * @param user
     * @return
     */
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<SuccessResponse> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseBuilder.success(HttpStatus.CREATED, "User created successfully", createdUser);
    }

    /**
     *
     * @return
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<SuccessResponse> getUsers() {
        return ResponseBuilder.success(HttpStatus.OK, "Users", userService.getAllUsers());
    }


    /**
     *
     * @param id
     * @return
     */
    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<SuccessResponse> getUser(@PathVariable long id) {
        User user = userService.getUserById(id);
        return ResponseBuilder.success(HttpStatus.OK, "User", user);
    }


    /**
     *
     * @param id
     * @param user
     * @return
     */
    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<SuccessResponse> updateUser(@PathVariable long id, @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseBuilder.success(HttpStatus.OK, "User updated successfully", updatedUser);
    }


    /**
     *
     * @param id
     * @return
     */
    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<SuccessResponse> deleteUser(@PathVariable long id) {
        userService.deleteUserById(id);
        return ResponseBuilder.success(HttpStatus.OK, "User", null);
    }
}
