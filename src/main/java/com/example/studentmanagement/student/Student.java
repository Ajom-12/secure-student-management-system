package com.example.studentmanagement.student;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 30)
    private String registrationNumber;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String fullName;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false, length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 20)
    private String gender;
    private LocalDate dateOfBirth;
}