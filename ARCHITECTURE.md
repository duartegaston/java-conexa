# Arquitectura de BookHub API

Este documento describe las decisiones de diseño, patrones y flujos de la aplicación.
Es el contexto principal para entender cómo están conectadas las capas del sistema.

## Diagrama de arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTE (Postman / Swagger UI)           │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP Request
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              SPRING SECURITY FILTER CHAIN                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  JwtAuthenticationFilter                                  │  │
│  │  1. Extrae el token del header "Authorization: Bearer..." │  │
│  │  2. Valida la firma y expiración del JWT                  │  │
│  │  3. Carga el usuario desde la BD                          │  │
│  │  4. Establece la autenticación en el SecurityContext      │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │ Request autenticada
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE CONTROLLERS                          │
│  AuthController  BookController  ReviewController  AdminCtrl    │
│  - Valida el body con @Valid                                    │
│  - Extrae el usuario autenticado con @AuthenticationPrincipal  │
│  - Convierte excepciones a respuestas HTTP                      │
│  - Documenta los endpoints con @Operation (Swagger)            │
└────────────────────────────┬────────────────────────────────────┘
                             │ DTO request
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE SERVICIOS                            │
│  AuthService  BookService  ReviewService  ReadingListService    │
│  - Implementa las reglas de negocio                            │
│  - Gestiona transacciones (@Transactional)                     │
│  - Lanza excepciones de negocio (BusinessException, etc.)      │
│  - Mapea entidades a DTOs                                      │
│                    ┌──────────────────┐                        │
│                    │ OpenLibraryService│  → API Externa         │
│                    └──────────────────┘                        │
└──────────────┬─────────────────────────────────────────────────┘
               │ Entidades JPA
               ▼
┌─────────────────────────────────────────────────────────────────┐
│                   CAPA DE REPOSITORIOS                          │
│  UserRepository  BookRepository  ReviewRepository               │
│  - Spring Data JPA: métodos CRUD gratuitos                     │
│  - Query Methods: Spring genera SQL del nombre del método       │
│  - @Query JPQL: queries personalizadas con lenguaje Java-like  │
└────────────────────────────┬────────────────────────────────────┘
                             │ SQL
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      POSTGRESQL                                 │
│  Tablas: users, books, reviews, reading_list                    │
│  Hibernate gestiona el esquema con ddl-auto=update             │
└─────────────────────────────────────────────────────────────────┘
```

## Flujo de autenticación JWT

```
REGISTRO:
1. POST /auth/register { username, email, password }
2. AuthService verifica unicidad de email y username
3. BCryptPasswordEncoder.encode(password) → hash
4. UserRepository.save(user) → persiste en BD
5. JwtTokenProvider.generateTokenFromEmail(email) → JWT
6. Respuesta: { token, userId, email, role }

LOGIN:
1. POST /auth/login { email, password }
2. AuthenticationManager.authenticate(email, password)
   → DaoAuthenticationProvider
   → UserDetailsServiceImpl.loadUserByUsername(email)
   → BCrypt.matches(password, hashedPassword)
3. Si OK → JwtTokenProvider.generateToken(authentication) → JWT
4. Respuesta: { token, userId, email, role }

REQUEST PROTEGIDA:
1. GET /reading-list (Header: Authorization: Bearer <token>)
2. JwtAuthenticationFilter.doFilterInternal()
   → extrae token del header
   → JwtTokenProvider.validateToken(token) → verifica firma y expiración
   → JwtTokenProvider.getEmailFromToken(token) → extrae email del subject
   → UserDetailsService.loadUserByUsername(email) → carga usuario de BD
   → SecurityContextHolder.setAuthentication(auth)
3. Spring Security permite la request (usuario autenticado)
4. @PreAuthorize("isAuthenticated()") → pasa
5. @AuthenticationPrincipal UserDetails → inyecta el usuario en el controller
```

## Patrones de diseño utilizados

### 1. MVC (Model-View-Controller)
- **Model**: entidades JPA + DTOs
- **View**: JSON serializado por Jackson (no hay HTML)
- **Controller**: controllers REST que procesan requests y devuelven responses

### 2. Repository Pattern
- Los repositorios abstraen el acceso a datos
- Los servicios nunca usan SQL directo, siempre a través del repositorio
- Facilita el testing: en tests se mockea el repositorio

### 3. DTO Pattern (Data Transfer Object)
- Los DTOs separan la representación de la API de las entidades internas
- Los `Request` DTO contienen los datos que envía el cliente
- Los `Response` DTO contienen los datos que devuelve la API
- Las entidades JPA nunca se exponen directamente en la API (evita exponer datos sensibles)

### 4. Builder Pattern
- Usado en todas las entidades y DTOs (Lombok @Builder)
- Más legible que constructores con muchos parámetros
- Ejemplo: `User.builder().username("x").email("y").build()`

### 5. Filter Chain (Chain of Responsibility)
- Spring Security implementa la seguridad como una cadena de filtros
- Cada filtro puede procesar o rechazar la request
- `JwtAuthenticationFilter` se inserta antes del filtro estándar

### 6. Manejo centralizado de excepciones
- `GlobalExceptionHandler` con `@RestControllerAdvice` centraliza todos los errores
- Los services lanzan excepciones de dominio (no HTTP)
- El handler las convierte al código HTTP apropiado

## Decisiones técnicas y justificación

### ¿Por qué JWT en lugar de sesiones?
- Las APIs REST son stateless: el servidor no guarda estado del cliente
- JWT permite escalar horizontalmente (varios servidores comparten la misma clave)
- El token contiene todo lo necesario para autenticar al usuario

### ¿Por qué BCrypt para contraseñas?
- Unidireccional: no se puede revertir el hash
- Agrega sal (salt) automáticamente: previene rainbow tables
- Es lento intencionalmente: dificulta ataques de fuerza bruta

### ¿Por qué FetchType.LAZY en relaciones JPA?
- LAZY: no carga la entidad relacionada hasta que se accede explícitamente
- EAGER: siempre carga la entidad relacionada (puede causar N+1 queries)
- Ejemplo: cargar una lista de reseñas NO debe cargar todos los usuarios/libros asociados automáticamente

### ¿Por qué H2 para tests?
- No requiere PostgreSQL instalado para ejecutar tests
- Los tests son reproducibles en cualquier máquina (CI/CD incluido)
- @DataJpaTest hace rollback automático: cada test tiene un estado limpio

### ¿Por qué @Transactional(readOnly = true)?
- Optimización: Hibernate no hace dirty checking en transacciones de solo lectura
- El driver JDBC puede optimizar queries en modo lectura
- Comunicación de intención clara: este método no modifica datos

## Estructura de errores de la API

Todos los errores tienen este formato JSON:
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Libro no encontrado con id: '99'",
  "path": "/api/v1/books/99"
}
```

### Mapa de excepciones → códigos HTTP

| Excepción | Código HTTP | Ejemplo |
|-----------|-------------|---------|
| `ResourceNotFoundException` | 404 | Libro no existe |
| `BusinessException` | 409 | Email duplicado |
| `UnauthorizedException` | 403 | Intentar borrar reseña ajena |
| `BadCredentialsException` | 401 | Contraseña incorrecta |
| `MethodArgumentNotValidException` | 400 | @Valid falla |
| `Exception` (genérica) | 500 | Error inesperado |
