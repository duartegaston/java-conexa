package com.bookhub.api.service;

import com.bookhub.api.exception.BusinessException;
import com.bookhub.api.model.dto.request.LoginRequest;
import com.bookhub.api.model.dto.request.RegisterRequest;
import com.bookhub.api.model.dto.response.AuthResponse;
import com.bookhub.api.model.entity.Role;
import com.bookhub.api.model.entity.User;
import com.bookhub.api.repository.UserRepository;
import com.bookhub.api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link AuthService}.
 *
 * <p>Los tests unitarios verifican la lógica de negocio de forma aislada,
 * mockeando todas las dependencias externas (repositorios, encoders, etc.).</p>
 *
 * <p>Estructura AAA (Arrange-Act-Assert): cada test tiene tres secciones:
 * <ul>
 *   <li>Arrange: preparar los datos y configurar los mocks</li>
 *   <li>Act: ejecutar el método bajo prueba</li>
 *   <li>Assert: verificar el resultado esperado</li>
 * </ul></p>
 */
// @ExtendWith(MockitoExtension.class): integra JUnit 5 con Mockito para crear mocks automáticamente
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del servicio de autenticación")
class AuthServiceTest {

    // @Mock: crea un mock (objeto falso) de la clase indicada usando Mockito
    // Un mock no hace nada por defecto; debemos configurar su comportamiento con when()/thenReturn()
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    // @InjectMocks: crea una instancia real de AuthService e inyecta los mocks anteriores
    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User testUser;

    // @BeforeEach: este método se ejecuta ANTES de cada test para preparar el estado inicial
    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$hashedPassword")
                .role(Role.ROLE_USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== TESTS DE REGISTRO =====

    @Test
    @DisplayName("Debería registrar un nuevo usuario exitosamente")
    void register_deberiaRegistrarUsuarioExitosamente() {
        // Arrange: configuramos los mocks para simular que el email/username no existen
        // when(...).thenReturn(...): "cuando se llame a este método, devolver este valor"
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateTokenFromEmail(testUser.getEmail())).thenReturn("jwt-token-test");

        // Act: ejecutamos el método bajo prueba
        AuthResponse response = authService.register(registerRequest);

        // Assert: verificamos que el resultado es el esperado
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-test");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_USER);

        // verify(): verifica que el método mock fue llamado con los argumentos esperados
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(registerRequest.getPassword());
    }

    @Test
    @DisplayName("Debería lanzar BusinessException si el email ya existe")
    void register_deberiaLanzarExcepcionSiEmailExiste() {
        // Arrange: simulamos que el email ya está registrado
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Assert + Act: verificamos que el método lanza la excepción esperada
        // assertThatThrownBy(): verifica que el código lanza una excepción
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("test@example.com");

        // verify que nunca se intentó guardar el usuario
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar BusinessException si el username ya existe")
    void register_deberiaLanzarExcepcionSiUsernameExiste() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // Assert + Act
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("testuser");

        verify(userRepository, never()).save(any());
    }

    // ===== TESTS DE LOGIN =====

    @Test
    @DisplayName("Debería autenticar al usuario y devolver JWT exitosamente")
    void login_deberiaAutenticarYDevolverJwt() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        // Creamos un token de autenticación de Spring Security con el usuario mockeado
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtTokenProvider.generateToken(authToken)).thenReturn("jwt-login-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-login-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Debería propagar BadCredentialsException si las credenciales son incorrectas")
    void login_deberiaPropararExcepcionConCredencialesInvalidas() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");

        // Simulamos que el AuthenticationManager lanza BadCredentialsException
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        // Assert + Act
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
