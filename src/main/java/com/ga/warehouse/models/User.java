package com.ga.warehouse.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import com.ga.warehouse.enums.UserStatus;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private String password;

    @Column(unique = true)
    private String email;

    @Column
    private Boolean emailVerified;

    @Column
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;


    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private UserProfile userProfile;

    @Column(nullable = false)
    private boolean deleted;

    public void setProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) userProfile.setUser(this);
    }
}
