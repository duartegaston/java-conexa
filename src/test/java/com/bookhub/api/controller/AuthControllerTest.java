package com.bookhub.api.controller;

import com.bookhub.api.exception.BusinessException;
import com.bookhub.api.model.dto.request.LoginRequest;
import com.bookhub.api.model.dto.request.RegisterRequest;
import com.bookhub.api.model.dto.response.AuthResponse;
import com.bookhub.api.model.entity.Role;
import com.bookhub.api.security.JwtAuthenticationFilter;
import com.bookhub.api.security.JwtTokenProvider;
import com.bookhub.api.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests del controller de autenticación usando MockMvc.
 *
 * <p>@WebMvcTest carga solo la capa web (controllers, filtros, security config),
 * sin levantar el contexto completo ni la base de datos.
 * Es más rápido que @SpringBootTest y permite testear el comportamiento HTTP.</p>
 *
 * <p>Todas las dependencias del controller deben ser mockeadas con @MockBean.</p>
 */
// @WebMvcTest: carga solo el controller especificado y su infraestructura web
@WebMvcTest(AuthController.class)
@DisplayName("Tests del AuthController")
class AuthControllerTest {

    // MockMvc: cliente HTTP simulado para testear controllers sin levantar un servidor real
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper: serializa/deserializa objetos Java a JSON (incluido en Spring Boot)
    @Autowired
    private ObjectMapper objectMapper;

    // @MockBean: crea un mock de Spring y lo registra en el contexto de la aplicación
    // Reemplaza el bean real por un mock durante el test
    @MockBean
    private AuthService authService;

    // Necesitamos estos beans aunque no los usemos directamente (Spring Security los requiere)
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /auth/register debería devolver 201 con el token JWT")
    void register_deberiaDevolver201ConToken() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        AuthResponse mockResponse = AuthResponse.builder()
                .token("jwt-token-test")
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        // Act + Assert: construimos la request y verificamos la respuesta
        mockMvc.perform(
                // post(): crea una request HTTP POST
                post("/auth/register")
                // contentType: establece el header Content-Type: application/json
                .contentType(MediaType.APPLICATION_JSON)
                // content: el body de la request como JSON
                .content(objectMapper.writeValueAsString(request))
            )
            // andExpect: verifica condiciones sobre la respuesta
            // status().isCreated(): verifica que el código HTTP es 201
            .andExpect(status().isCreated())
            // jsonPath: verifica el valor de un campo en el JSON de respuesta
            // $.token: expresión JsonPath que accede al campo "token" del objeto raíz
            .andExpect(jsonPath("$.token").value("jwt-token-test"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("POST /auth/register debería devolver 400 con datos inválidos")
    void register_deberiaDevolver400ConDatosInvalidos() throws Exception {
        // Arrange: request con email inválido
        RegisterRequest invalidRequest = new RegisterRequest("u", "not-an-email", "short");

        // Act + Assert
        mockMvc.perform(
                post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
            )
            // status().isBadRequest(): verifica HTTP 400
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register debería devolver 409 si el email ya existe")
    void register_deberiaDevolver409SiEmailExiste() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "existing@example.com", "password123");

        // Simulamos que el service lanza BusinessException (email duplicado)
        when(authService.register(any())).thenThrow(
            new BusinessException("Ya existe una cuenta con el email: existing@example.com")
        );

        // Act + Assert
        mockMvc.perform(
                post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
            )
            // status().isConflict(): verifica HTTP 409
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("existing@example.com")));
    }

    @Test
    @DisplayName("POST /auth/login debería devolver 200 con el token JWT")
    void login_deberiaDevolver200ConToken() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        AuthResponse mockResponse = AuthResponse.builder()
                .token("jwt-login-token")
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        // Act + Assert
        mockMvc.perform(
                post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-login-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
