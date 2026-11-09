# Conceptos clave - BookHub API

Glosario de anotaciones, patrones y tecnologías usadas en este proyecto.
Diseñado para repasar antes de una entrevista técnica.

---

## Spring Boot

### ¿Qué es Spring Boot?
Framework que simplifica el desarrollo de aplicaciones Spring eliminando configuración manual.
Principio: **convención sobre configuración** (convention over configuration).

**Auto-configuración**: Spring Boot detecta las dependencias en el classpath y configura beans automáticamente.
Por ejemplo, si `spring-boot-starter-web` está en el `pom.xml`, configura Tomcat embebido automáticamente.

---

## Spring MVC - Controllers

### `@RestController`
Marca la clase como controller REST. Combina:
- `@Controller`: componente MVC que maneja requests HTTP
- `@ResponseBody`: los métodos retornan objetos que Jackson serializa a JSON

### `@RequestMapping("/ruta")`
Define la ruta base. Todos los métodos del controller heredan este prefijo.

### `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
Atajos de `@RequestMapping(method = RequestMethod.GET/POST/PUT/DELETE)`.
Mapean un método HTTP específico a un método Java.

### `@PathVariable`
Extrae un segmento de la URL: `/books/{id}` → `@PathVariable Long id`

### `@RequestParam`
Extrae un parámetro de la query string: `/books?page=0&size=10` → `@RequestParam int page`

### `@RequestBody`
Deserializa el body JSON de la request al objeto Java: `{ "title": "..." }` → `BookRequest request`

### `@Valid`
Activa la validación de Bean Validation (Jakarta) sobre el objeto anotado.
Si alguna constraint falla, Spring lanza `MethodArgumentNotValidException` (400 Bad Request).

### `ResponseEntity<T>`
Permite controlar el código HTTP, los headers y el body de la respuesta:
```java
ResponseEntity.ok(data)                          // 200 OK
ResponseEntity.status(HttpStatus.CREATED).body() // 201 Created
ResponseEntity.noContent().build()               // 204 No Content
ResponseEntity.notFound().build()                // 404 Not Found
```

---

## Spring Data JPA

### ¿Qué es JPA?
Java Persistence API: estándar para mapear objetos Java a tablas relacionales.
Hibernate es la implementación más usada (Spring Boot la usa por defecto).

### `@Entity`
Marca la clase como entidad JPA. Hibernate crea/mapea una tabla para esta clase.

### `@Id` y `@GeneratedValue`
`@Id` define la clave primaria. `@GeneratedValue(strategy = IDENTITY)` delega la generación al motor de BD (SERIAL en PostgreSQL).

### `@Column`
Configura el mapeo de la columna: nombre, si puede ser null, longitud máxima, etc.

### `@ManyToOne` y `@OneToMany`
Relaciones entre entidades:
- `@ManyToOne`: muchas reseñas → un libro (la FK `book_id` está en la tabla `reviews`)
- `@OneToMany(mappedBy = "book")`: un libro → muchas reseñas (el lado inverso)

### `FetchType.LAZY` vs `FetchType.EAGER`
- `LAZY`: carga la entidad relacionada solo cuando se accede (por defecto en `@OneToMany`)
- `EAGER`: siempre carga la entidad relacionada con un JOIN (puede causar N+1 queries)

### JpaRepository
Interfaz de Spring Data que provee métodos CRUD automáticos:
```java
save(entity)         // INSERT o UPDATE
findById(id)         // SELECT WHERE id = ?
findAll()            // SELECT *
deleteById(id)       // DELETE WHERE id = ?
existsById(id)       // SELECT COUNT(*) > 0
```

### Query Methods
Spring Data genera queries SQL automáticamente del nombre del método:
```java
findByEmail(String email)                    // SELECT * WHERE email = ?
findByUsernameAndEmail(String u, String e)   // SELECT * WHERE username = ? AND email = ?
existsByEmail(String email)                  // SELECT COUNT(*) > 0 WHERE email = ?
```

### `@Query` con JPQL
Para queries más complejas, JPQL (Java Persistence Query Language) usa nombres de clases y campos Java:
```java
@Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))")
List<Book> searchByTitle(@Param("q") String query);
```

---

## Spring Security

### ¿Cómo funciona Spring Security?
Implementa seguridad como una cadena de filtros (Filter Chain). Cada request HTTP pasa por todos los filtros antes de llegar al controller.

### `SecurityFilterChain`
Define las reglas de seguridad HTTP:
- Qué URLs son públicas
- Qué URLs requieren autenticación
- Qué URLs requieren roles específicos
- Política de sesiones (STATELESS para APIs REST)

### `AuthenticationManager`
Orquesta el proceso de autenticación. Delega al `AuthenticationProvider` configurado.

### `DaoAuthenticationProvider`
Proveedor que carga el usuario desde una base de datos:
1. Llama a `UserDetailsService.loadUserByUsername(email)`
2. Verifica la contraseña con `PasswordEncoder.matches(raw, encoded)`
3. Si OK, crea un objeto `Authentication` con los roles del usuario

### `UserDetails`
Interfaz que Spring Security requiere para representar a un usuario autenticado.
Incluye: username, password, authorities (roles), y flags de estado.
En este proyecto, `User` implementa `UserDetails` directamente.

### `SecurityContext` y `SecurityContextHolder`
El `SecurityContextHolder` almacena la información de autenticación del thread actual.
Después de validar el JWT, `JwtAuthenticationFilter` coloca la autenticación aquí.
Los controllers pueden acceder al usuario con `@AuthenticationPrincipal`.

### `@PreAuthorize`
Verifica el permiso antes de ejecutar el método:
```java
@PreAuthorize("isAuthenticated()")           // cualquier usuario autenticado
@PreAuthorize("hasRole('ADMIN')")            // solo ADMIN
@PreAuthorize("hasAnyRole('USER', 'ADMIN')") // USER o ADMIN
```

### BCrypt
Algoritmo de hash para contraseñas. Características clave para la entrevista:
- **Unidireccional**: no se puede revertir (comparación siempre con `matches()`)
- **Salt automático**: cada hash es único aunque las contraseñas sean iguales
- **Ajustable**: el factor de costo define cuántas rondas se aplican
- **Lento intencionalmente**: dificulta ataques de fuerza bruta

---

## JWT (JSON Web Token)

### Estructura del JWT
El token tiene 3 partes separadas por puntos: `header.payload.signature`

```
eyJhbGciOiJIUzI1NiJ9    ← Header (algoritmo, tipo)
.
eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSIsImlhdCI6MTcwMH0  ← Payload (claims: sub, iat, exp)
.
FIRMA_HMAC_SHA256        ← Signature (header + payload firmado con la clave secreta)
```

### Claims del JWT en este proyecto
- `sub` (subject): email del usuario
- `iat` (issued at): fecha de emisión
- `exp` (expiration): fecha de expiración (24 horas)

### ¿Por qué JWT es stateless?
El servidor no necesita guardar el token. Solo necesita:
1. La clave secreta para verificar la firma
2. Saber cómo decodificar el payload

---

## Testing

### Tipos de tests y cuándo usar cada uno

| Tipo | Anotación | Velocidad | Qué testea |
|------|-----------|-----------|------------|
| Unitario | `@ExtendWith(MockitoExtension.class)` | ⚡ Rápido | Una clase con dependencias mockeadas |
| Controller | `@WebMvcTest` | ⚡ Rápido | Capa web: rutas, validaciones, códigos HTTP |
| Repositorio | `@DataJpaTest` | 🔶 Medio | Queries JPA con H2 en memoria |
| Integración | `@SpringBootTest` | 🔴 Lento | Flujo completo con todas las capas |

### Mockito
Framework para crear mocks en tests unitarios:
```java
@Mock UserRepository repo;              // objeto falso
when(repo.findById(1L)).thenReturn(Optional.of(user)); // configurar comportamiento
verify(repo).save(any(User.class));     // verificar que se llamó al método
verify(repo, never()).delete(any());    // verificar que NUNCA se llamó
```

### AssertJ (recomendado sobre JUnit assertions)
```java
assertThat(result).isNotNull();
assertThat(result.getEmail()).isEqualTo("test@example.com");
assertThatThrownBy(() -> service.method()).isInstanceOf(BusinessException.class);
```

### Estructura AAA (Arrange-Act-Assert)
Patrón estándar para organizar cada test:
- **Arrange**: preparar los datos y configurar los mocks
- **Act**: ejecutar el método bajo prueba
- **Assert**: verificar el resultado esperado

---

## SonarQube

### ¿Qué analiza SonarQube?
- **Bugs**: errores potenciales en el código
- **Vulnerabilidades**: problemas de seguridad (ej: contraseñas en código)
- **Code Smells**: código que funciona pero es difícil de mantener
- **Coverage**: porcentaje de código cubierto por tests
- **Duplications**: código duplicado

### Quality Gate
Conjunto de condiciones que el proyecto debe cumplir para considerarse "saludable".
Si alguna condición falla, el Quality Gate marca el análisis como **FAILED**.
En CI/CD se puede configurar para bloquear el merge si falla.

### JaCoCo
Plugin Maven que mide la cobertura de código y genera reportes.
SonarQube lee el reporte XML de JaCoCo para calcular la cobertura:
```bash
mvn clean test            # ejecuta tests + genera reporte JaCoCo
mvn sonar:sonar           # envía métricas a SonarQube
```

---

## Preguntas frecuentes en entrevistas

**¿Diferencia entre @Component, @Service, @Repository y @Controller?**
Son todos estereotipos de @Component (Spring los trata igual para detección automática).
La diferencia es semántica: indica en qué capa está el componente.
@Repository tiene un extra: traduce excepciones de BD a DataAccessException.

**¿Qué es la inyección de dependencias?**
El contenedor de IoC (Inversion of Control) de Spring crea y gestiona los objetos (beans)
y los inyecta donde se necesitan. Ventajas: menor acoplamiento, más testeable.

**¿Diferencia entre @Autowired y @RequiredArgsConstructor?**
Ambos inyectan dependencias. Inyección por constructor (@RequiredArgsConstructor)
es preferida porque: las dependencias son explícitas, el objeto es inmutable
y es más fácil de testear (puedes crear la clase directamente en tests sin Spring).

**¿Qué es una transacción en JPA?**
Un conjunto de operaciones que se ejecutan como una unidad atómica:
- Si todo sale bien: se hace COMMIT (los cambios se persisten)
- Si algo falla: se hace ROLLBACK (todos los cambios se deshacen)

**¿Qué es el problema N+1 en JPA?**
Cuando para cargar N entidades, se ejecutan N+1 queries (1 para la lista + 1 por cada relación).
Solución: usar `@EntityGraph` o `JOIN FETCH` en la query para cargar las relaciones en una sola query.

**¿Diferencia entre PUT y PATCH?**
- `PUT`: reemplaza el recurso completo (se envían todos los campos)
- `PATCH`: actualiza solo los campos enviados (actualización parcial)
