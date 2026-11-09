# Guía de Testing - BookHub API

Este documento explica los tipos de tests en el proyecto, qué testa cada uno y cómo ejecutarlos.

## Pirámide de tests

```
        /\
       /  \
      / E2E \         ← pocos, lentos, testean el sistema completo
     /--------\
    /Integration\     ← algunos, lentos, testean integración de capas
   /------------\
  / Controller   \    ← varios, rápidos, testean la capa web
 /----------------\
/   Unit Tests     \  ← muchos, muy rápidos, testean lógica de negocio
```

## Tests unitarios

**Ubicación**: `src/test/java/com/bookhub/api/service/`

**Herramientas**: JUnit 5 + Mockito

**¿Qué testean?**: La lógica de negocio de los servicios de forma **aislada**.
Todas las dependencias (repositorios, otros servicios) son **mocks** que devuelven datos controlados.

**Ventajas**:
- Muy rápidos (no levantan Spring, no usan BD)
- Aíslan exactamente qué está fallando
- Fáciles de mantener

**Ejemplo**: `AuthServiceTest`
- Testea que el registro hashea la contraseña
- Testea que lanza `BusinessException` si el email ya existe
- Testea que el login genera un JWT si las credenciales son correctas

**Estructura de cada test (AAA)**:
```java
@Test
void deberiaRegistrarUsuario() {
    // Arrange: preparar datos y configurar mocks
    when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
    when(passwordEncoder.encode("password")).thenReturn("$2a$...");

    // Act: ejecutar el código bajo prueba
    AuthResponse response = authService.register(request);

    // Assert: verificar el resultado
    assertThat(response.getToken()).isNotBlank();
    verify(userRepository).save(any(User.class));
}
```

---

## Tests de controller (capa web)

**Ubicación**: `src/test/java/com/bookhub/api/controller/`

**Herramientas**: `@WebMvcTest` + `MockMvc` + `@MockBean`

**¿Qué testean?**: La capa web: rutas, códigos HTTP, validaciones de entrada (@Valid), y respuestas JSON.
Los servicios son **mocks** (`@MockBean`), por lo que no se ejecuta lógica de negocio real.

**¿Qué NO testean?**: Lógica de negocio, acceso a BD, Spring Security completo.

**Ventajas**:
- Rápidos (no carga el contexto completo ni BD)
- Verifican exactamente el comportamiento HTTP de cada endpoint

**Ejemplo**: `AuthControllerTest`
- Testea que POST /auth/register devuelve 201
- Testea que POST /auth/register devuelve 400 con datos inválidos
- Testea que POST /auth/register devuelve 409 si el servicio lanza BusinessException

---

## Tests de repositorio

**Ubicación**: `src/test/java/com/bookhub/api/repository/`

**Herramientas**: `@DataJpaTest` + H2 en memoria

**¿Qué testean?**: Las queries JPA: que los Query Methods y las `@Query` JPQL funcionan correctamente.

**¿Qué NO testean?**: Lógica de negocio, controllers, Spring Security.

**Ventajas**:
- No necesita PostgreSQL real (usa H2)
- Cada test tiene un estado limpio (rollback automático)

**Ejemplo**: `BookRepositoryTest`
- Testea que `findByOlid` devuelve el libro correcto
- Testea que `searchByTitleOrAuthor` hace búsqueda insensible a mayúsculas
- Testea que `existsByOlid` devuelve true/false correctamente

---

## Tests de integración

**Ubicación**: `src/test/java/com/bookhub/api/integration/`

**Herramientas**: `@SpringBootTest` + `@AutoConfigureMockMvc` + H2

**¿Qué testean?**: El flujo completo de la aplicación: desde la request HTTP hasta la BD y de vuelta.
Todo el stack de Spring Boot levanta (incluyendo Spring Security) con H2 en lugar de PostgreSQL.

**Ventajas**:
- Verifica que todas las capas se integran correctamente
- Detecta problemas de configuración

**Desventajas**:
- Lentos (levantan todo el contexto de Spring)
- Más costosos de mantener

**Ejemplo**: `AuthIntegrationTest`
- Testea el flujo completo: registro → login → acceso con JWT → rechazo sin JWT

---

## Comandos para ejecutar los tests

```bash
# Todos los tests
mvn test

# Solo compilar (sin ejecutar tests)
mvn compile -DskipTests

# Con reporte de cobertura (JaCoCo)
mvn test jacoco:report
# Reporte HTML en: target/site/jacoco/index.html

# Ejecutar una clase de test específica
mvn test -Dtest=AuthServiceTest

# Ejecutar un test específico dentro de una clase
mvn test -Dtest=AuthServiceTest#deberiaRegistrarUsuario

# Mostrar output detallado de los tests
mvn test -Dsurefire.useFile=false
```

---

## Buenas prácticas de testing en el proyecto

### 1. Nombres descriptivos en castellano
```java
@DisplayName("Debería lanzar BusinessException si el email ya existe")
void register_deberiaLanzarExcepcionSiEmailExiste() { ... }
```

### 2. Un assert por concepto (no mezclar)
```java
// Bien: cada test verifica un comportamiento específico
@Test void deberiaRetornar201AlRegistrar() { ... }
@Test void deberiaRetornar400ConEmailInvalido() { ... }
@Test void deberiaRetornar409ConEmailDuplicado() { ... }
```

### 3. Datos de test descriptivos
```java
// Bien: el nombre del dato comunica su propósito
User adminUser = User.builder().role(Role.ROLE_ADMIN).build();
User blockedUser = User.builder().isActive(false).build();
```

### 4. Verificar tanto el happy path como los errores
- Happy path: el flujo normal y exitoso
- Edge cases: valores límite, nulos, vacíos
- Error cases: excepciones esperadas, acceso no autorizado

### 5. No testear la implementación, testear el comportamiento
```java
// Mal: testea que se llama a un método interno específico
verify(repository).findByEmail(email);  // acoplado a la implementación

// Bien: testea el resultado esperado del comportamiento
assertThat(response.getEmail()).isEqualTo(email);  // testea el resultado
```
