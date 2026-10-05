package com.example.studentmanagement.student;

import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    @Transactional
    public Student create(StudentRequest request) {
        if (studentRepository.findByRegistrationNumber(request.registrationNumber()).isPresent()) {
            throw new DuplicateResourceException("Registration number is already in use");
        }
        if (studentRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Student email is already in use");
        }
        return studentRepository.save(toEntity(request, new Student()));
    }

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Student findById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    @Transactional
    public Student update(Long id, StudentRequest request) {
        Student student = findById(id);
        if (studentRepository.existsByRegistrationNumberAndIdNot(request.registrationNumber(), id)) {
            throw new DuplicateResourceException("Registration number is already in use");
        }
        if (studentRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Student email is already in use");
        }
        return studentRepository.save(toEntity(request, student));
    }

    @Transactional
    public void delete(Long id) {
        Student student = findById(id);
        studentRepository.delete(student);
    }

    private Student toEntity(StudentRequest request, Student student) {
        student.setRegistrationNumber(request.registrationNumber().trim());
        student.setFullName(request.fullName().trim());
        student.setEmail(request.email().trim().toLowerCase());
        student.setPhone(request.phone());
        student.setGender(request.gender());
        student.setDateOfBirth(request.dateOfBirth());
        return student;
    }
}