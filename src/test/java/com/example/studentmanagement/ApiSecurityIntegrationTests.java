package com.example.studentmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticationAuthorizationCrudAndRefreshRotationWork() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String studentUsername = "student" + suffix;
        String studentEmail = studentUsername + "@example.com";
        String studentPassword = "StudentPassword#2026";
        register(studentUsername, studentEmail, studentPassword);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(studentUsername, studentEmail, studentPassword)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration("duplicate" + suffix, studentEmail.toUpperCase(), studentPassword)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        String studentAccess = token(login(studentUsername, studentPassword), "accessToken");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(studentUsername, "incorrect-password")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/students").header("Authorization", "Bearer " + studentAccess))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + studentAccess))
                .andExpect(status().isForbidden());

        String teacherUsername = "teacher" + suffix;
        String teacherPassword = "TeacherPassword#2026";
        String teacherJson = register(teacherUsername, "teacher" + suffix + "@example.com", teacherPassword);
        long teacherId = Long.parseLong(value(teacherJson, "id"));

        String adminAccess = token(login("test-admin", "TestAdminPassword#2026"), "accessToken");
        mockMvc.perform(put("/api/users/{id}/role", teacherId)
                        .header("Authorization", "Bearer " + adminAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk());

        String teacherAccess = token(login(teacherUsername, teacherPassword), "accessToken");
        String studentRecord = "{\"registrationNumber\":\"REG-" + suffix.substring(0, 20)
                + "\",\"fullName\":\"Demo Student\",\"email\":\"record-" + suffix
                + "@example.com\",\"dateOfBirth\":\"2005-06-15\"}";
        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + teacherAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentRecord))
                .andExpect(status().isCreated())
                .andReturn();
        long recordId = Long.parseLong(value(created.getResponse().getContentAsString(), "id"));
        mockMvc.perform(put("/api/students/{id}", recordId)
                        .header("Authorization", "Bearer " + teacherAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentRecord.replace("Demo Student", "Updated Demo Student")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/students/{id}", recordId)
                        .header("Authorization", "Bearer " + teacherAccess))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/students/{id}", recordId)
                        .header("Authorization", "Bearer " + adminAccess))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/students/{id}", recordId)
                        .header("Authorization", "Bearer " + studentAccess))
                .andExpect(status().isNotFound());

        String initialRefresh = token(login(studentUsername, studentPassword), "refreshToken");
        String refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + initialRefresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String rotatedRefresh = token(refreshed, "refreshToken");
        assertNotEquals(initialRefresh, rotatedRefresh);
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedRefresh + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals("http://localhost:3000", mockMvc.perform(options("/api/students")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andReturn().getResponse().getHeader("Access-Control-Allow-Origin")
        );
    }

    private String register(String username, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration(username, email, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String registration(String username, String email, String password) {
        return "{\"username\":\"" + username + "\",\"email\":\"" + email
                + "\",\"password\":\"" + password + "\"}";
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String token(String json, String name) {
        return value(json, name);
    }

    private String value(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\":(?:\"([^\"]+)\"|(\\d+))")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Missing " + name + " in response: " + json);
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }
}
