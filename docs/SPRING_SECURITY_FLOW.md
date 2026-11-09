# Flujo de Spring Security en BookHub API

Este documento explica paso a paso cómo funciona el sistema de autenticación y autorización.

## Flujo completo de registro

```
POST /api/v1/auth/register
{ "username": "gaston", "email": "gaston@email.com", "password": "mipassword" }

1. JwtAuthenticationFilter → no hay token, continúa sin autenticar
2. SecurityConfig → /auth/** es público, permite la request
3. AuthController.register() → recibe el RegisterRequest
4. @Valid → Bean Validation verifica los campos (email válido, password >= 8 chars)
5. AuthService.register() →
   a. userRepository.existsByEmail() → verifica que el email no existe
   b. BCryptPasswordEncoder.encode("mipassword") → "$2a$10$hashedValue..."
   c. User.builder()...build() → crea la entidad
   d. userRepository.save(user) → INSERT INTO users (...) VALUES (...)
   e. JwtTokenProvider.generateTokenFromEmail("gaston@email.com") → JWT
6. Response: 201 Created
   { "token": "eyJ...", "email": "gaston@email.com", "role": "ROLE_USER" }
```

## Flujo completo de login

```
POST /api/v1/auth/login
{ "email": "gaston@email.com", "password": "mipassword" }

1. JwtAuthenticationFilter → no hay token, continúa sin autenticar
2. SecurityConfig → /auth/** es público, permite la request
3. AuthController.login() → recibe el LoginRequest
4. @Valid → verifica formato del email
5. AuthService.login() →
   a. authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken("gaston@email.com", "mipassword")
      )
   b. DaoAuthenticationProvider →
      - UserDetailsServiceImpl.loadUserByUsername("gaston@email.com")
        → SELECT * FROM users WHERE email = 'gaston@email.com'
      - BCryptPasswordEncoder.matches("mipassword", "$2a$10$hashedValue...")
        → true/false
   c. Si OK → Authentication object con el User como Principal
   d. JwtTokenProvider.generateToken(authentication) →
      Jwts.builder()
        .subject("gaston@email.com")
        .issuedAt(now)
        .expiration(now + 24h)
        .signWith(secretKey)
        .compact()
      → "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJnYXN0b25AZW1haWwuY29tIn0.FIRMA"
6. Response: 200 OK
   { "token": "eyJ...", "email": "gaston@email.com", "role": "ROLE_USER" }
```

## Flujo de request protegida con JWT

```
GET /api/v1/reading-list
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJnYXN0b24...

1. JwtAuthenticationFilter.doFilterInternal() →
   a. extractJwtFromRequest() → extrae "eyJhbGciOiJIUzI1NiJ9..."
   b. JwtTokenProvider.validateToken(token) →
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
      → verifica firma HMAC-SHA256 y que no expiró
      → true
   c. JwtTokenProvider.getEmailFromToken(token) →
      getPayload().getSubject() → "gaston@email.com"
   d. userDetailsService.loadUserByUsername("gaston@email.com")
      → SELECT * FROM users WHERE email = 'gaston@email.com' → User entity
   e. UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
   f. SecurityContextHolder.getContext().setAuthentication(auth)
      → el thread actual sabe que "gaston@email.com" está autenticado

2. SecurityConfig → /reading-list requiere autenticación → ✓ autenticado

3. ReadingListController.getMyList() →
   @PreAuthorize("isAuthenticated()") → ✓ autenticado
   @AuthenticationPrincipal UserDetails userDetails → el User cargado en paso 1d
   readingListService.getMyReadingList(userDetails.getUsername())
   → "gaston@email.com"

4. Response: 200 OK con la lista de lectura de gaston
```

## Flujo de request sin token (endpoint protegido)

```
GET /api/v1/reading-list
(sin header Authorization)

1. JwtAuthenticationFilter.doFilterInternal() →
   a. extractJwtFromRequest() → null (no hay header Authorization)
   b. el filtro NO establece autenticación en el SecurityContext
   c. filterChain.doFilter() → continúa la cadena sin autenticar

2. SecurityConfig → /reading-list requiere autenticación →
   SecurityContext no tiene autenticación → 401 Unauthorized
```

## Flujo de request con token válido pero sin el rol requerido

```
DELETE /api/v1/admin/users/5
Authorization: Bearer <token de usuario con ROLE_USER>

1. JwtAuthenticationFilter → token válido → user con ROLE_USER autenticado
2. SecurityConfig → /admin/** requiere ROLE_ADMIN
   → el usuario tiene ROLE_USER, no ROLE_ADMIN → 403 Forbidden
```

## Estructura del token JWT decodificado

```json
// Header (algoritmo y tipo)
{
  "alg": "HS256"
}

// Payload (claims)
{
  "sub": "gaston@email.com",  // subject: identificador del usuario
  "iat": 1705312200,          // issued at: fecha de emisión (Unix timestamp)
  "exp": 1705398600           // expiration: fecha de expiración (iat + 24h)
}

// Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secretKey)
```

## Componentes de seguridad y sus responsabilidades

| Componente | Responsabilidad |
|------------|-----------------|
| `SecurityConfig` | Reglas de acceso, configuración de filtros |
| `JwtTokenProvider` | Generar y validar tokens JWT |
| `JwtAuthenticationFilter` | Interceptar requests y procesar el JWT |
| `UserDetailsServiceImpl` | Cargar usuario de la BD por email |
| `DaoAuthenticationProvider` | Verificar credenciales (email + password) |
| `BCryptPasswordEncoder` | Hashear y verificar contraseñas |
| `AuthenticationManager` | Orquestar el proceso de autenticación |
