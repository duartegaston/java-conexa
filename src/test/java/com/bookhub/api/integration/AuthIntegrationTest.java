package com.bookhub.api.integration;

import com.bookhub.api.model.dto.request.LoginRequest;
import com.bookhub.api.model.dto.request.RegisterRequest;
import com.bookhub.api.model.dto.response.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración end-to-end para el flujo de autenticación.
 *
 * <p>@SpringBootTest carga el contexto completo de la aplicación (todos los beans),
 * incluyendo Spring Security, JPA (con H2 en memoria para tests) y todos los servicios.</p>
 *
 * <p>Este test verifica el flujo completo:
 * HTTP Request → Security Filter → Controller → Service → Repository → H2</p>
 *
 * <p>Es más lento que los tests unitarios pero verifica la integración de todas las capas.</p>
 */
// @SpringBootTest: levanta el contexto completo de Spring Boot para el test
// webEnvironment = RANDOM_PORT: inicia un servidor real en un puerto aleatorio
@SpringBootTest
// @AutoConfigureMockMvc: configura MockMvc automáticamente para testear con el contexto completo
@AutoConfigureMockMvc
// @ActiveProfiles("test"): usa application-test.yml (H2 en lugar de PostgreSQL)
@ActiveProfiles("test")
@DisplayName("Tests de integración de autenticación")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Flujo completo: registro → login → acceso a endpoint protegido")
    void flujoCompletoRegistroLoginAcceso() throws Exception {

        // ===== PASO 1: REGISTRO =====
        RegisterRequest registerRequest = new RegisterRequest(
            "integrationuser",
            "integration@test.com",
            "password123"
        );

        MvcResult registerResult = mockMvc.perform(
                post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value("integration@test.com"))
            .andReturn();

        // Extraemos el token de la respuesta del registro
        String registerBody = registerResult.getResponse().getContentAsString();
        AuthResponse registerResponse = objectMapper.readValue(registerBody, AuthResponse.class);
        assertThat(registerResponse.getToken()).isNotBlank();

        // ===== PASO 2: LOGIN =====
        LoginRequest loginRequest = new LoginRequest("integration@test.com", "password123");

        MvcResult loginResult = mockMvc.perform(
                post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        AuthResponse loginResponse = objectMapper.readValue(loginBody, AuthResponse.class);
        String jwtToken = loginResponse.getToken();
        assertThat(jwtToken).isNotBlank();

        // ===== PASO 3: ACCESO A ENDPOINT PROTEGIDO CON JWT =====
        // Verificamos que podemos acceder a la lista de lectura (endpoint protegido)
        // usando el token obtenido en el login
        mockMvc.perform(
                get("/reading-list")
                // Incluimos el token en el header Authorization con el formato Bearer
                .header("Authorization", "Bearer " + jwtToken)
            )
            .andExpect(status().isOk());

        // ===== PASO 4: VERIFICAR QUE SIN TOKEN SE RECHAZA =====
        mockMvc.perform(get("/reading-list"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login con credenciales incorrectas debería devolver 401")
    void login_conCredencialesIncorrectas_deberiaDevolver401() throws Exception {
        // Primero registramos el usuario
        RegisterRequest registerRequest = new RegisterRequest(
            "wrongpassuser",
            "wrongpass@test.com",
            "correctpassword"
        );

        mockMvc.perform(
                post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
            )
            .andExpect(status().isCreated());

        // Intentamos login con contraseña incorrecta
        LoginRequest wrongLogin = new LoginRequest("wrongpass@test.com", "wrongpassword");

        mockMvc.perform(
                post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongLogin))
            )
            .andExpect(status().isUnauthorized());
    }
}
