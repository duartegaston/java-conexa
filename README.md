# BookHub API

API REST de gestión de libros construida con Spring Boot, diseñada como proyecto de referencia para entrevistas técnicas. Demuestra los patrones y tecnologías más solicitadas en Java backend.

## Stack tecnológico

| Tecnología          | Versión  | Propósito                                      |
|---------------------|----------|------------------------------------------------|
| Java                | 17       | Lenguaje base                                  |
| Spring Boot         | 3.2.0    | Framework principal, auto-configuración        |
| Spring MVC          | 6.x      | API REST, controllers                          |
| Spring Data JPA     | 3.x      | Acceso a datos, repositorios                   |
| Spring Security     | 6.x      | Autenticación JWT, autorización por roles      |
| PostgreSQL          | 15       | Base de datos principal                        |
| H2                  | -        | Base de datos en memoria para tests            |
| jjwt                | 0.12.3   | Generación y validación de tokens JWT          |
| springdoc-openapi   | 2.3.0    | Documentación Swagger UI                       |
| Lombok              | -        | Reducción de boilerplate                       |
| JUnit 5             | -        | Framework de testing                           |
| Mockito             | -        | Mocking para tests unitarios                   |
| JaCoCo              | 0.8.11   | Cobertura de código                            |
| SonarQube           | 10.3     | Análisis estático de calidad de código         |

## Arquitectura

```
Cliente HTTP
    │
    ▼
[JWT Filter]           ← intercepta cada request, valida el token
    │
    ▼
[Controller]           ← recibe y valida la request (@Valid)
    │
    ▼
[Service]              ← lógica de negocio, reglas, transacciones
    │           ▼
[Repository]      [Open Library API]   ← integración externa
    │
    ▼
[PostgreSQL]
```

## Estructura del proyecto

```
src/
├── main/java/com/bookhub/api/
│   ├── config/             → SecurityConfig, OpenApiConfig, RestTemplateConfig
│   ├── controller/         → AuthController, BookController, ReviewController, ...
│   ├── service/            → AuthService, BookService, OpenLibraryService, ...
│   ├── repository/         → UserRepository, BookRepository, ...
│   ├── model/
│   │   ├── entity/         → User, Book, Review, ReadingListEntry
│   │   └── dto/
│   │       ├── request/    → RegisterRequest, LoginRequest, ReviewRequest, ...
│   │       └── response/   → AuthResponse, BookResponse, ReviewResponse, ...
│   ├── security/           → JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
│   └── exception/          → GlobalExceptionHandler, ResourceNotFoundException, ...
└── test/java/com/bookhub/api/
    ├── service/            → AuthServiceTest, BookServiceTest, ReviewServiceTest
    ├── controller/         → AuthControllerTest
    ├── repository/         → BookRepositoryTest
    └── integration/        → AuthIntegrationTest
```

## Endpoints de la API

### Autenticación (público)
```
POST /api/v1/auth/register   → registrar nuevo usuario
POST /api/v1/auth/login      → obtener JWT
```

### Libros
```
GET  /api/v1/books/search?q= → buscar en Open Library API (público)
POST /api/v1/books/import/{olid} → importar libro a la BD local (requiere auth)
GET  /api/v1/books           → listar libros importados, paginado (público)
GET  /api/v1/books/{id}      → detalle de libro (público)
```

### Reseñas
```
GET  /api/v1/reviews/book/{bookId}  → listar reseñas de un libro (público)
POST /api/v1/reviews/book/{bookId}  → crear reseña (requiere auth)
DELETE /api/v1/reviews/{id}         → eliminar reseña propia (requiere auth)
```

### Lista de lectura
```
POST   /api/v1/reading-list         → agregar libro (requiere auth)
GET    /api/v1/reading-list         → ver mi lista (requiere auth)
PUT    /api/v1/reading-list/{id}    → actualizar estado (requiere auth)
DELETE /api/v1/reading-list/{id}    → eliminar entrada (requiere auth)
```

### Administración (solo ADMIN)
```
GET    /api/v1/admin/users          → listar todos los usuarios
DELETE /api/v1/admin/users/{id}     → desactivar usuario
```

## Requisitos previos

- Java 17+
- Maven 3.8+
- Docker y Docker Compose

## Levantar el entorno de desarrollo

### 1. Clonar y compilar

```bash
git clone <repo-url>
cd bookhub-api
mvn clean compile
```

### 2. Levantar la base de datos

```bash
# Solo PostgreSQL (para desarrollo)
docker-compose up -d postgres

# PostgreSQL + SonarQube (para análisis de calidad)
docker-compose up -d
```

### 3. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

La API estará disponible en: `http://localhost:8080/api/v1`

### 4. Acceder a Swagger UI

Abre en el navegador: `http://localhost:8080/api/v1/swagger-ui.html`

Para testear endpoints protegidos:
1. Hacer `POST /auth/login` con credenciales válidas
2. Copiar el token del campo `token` en la respuesta
3. Hacer clic en el botón **"Authorize"** (candado en Swagger UI)
4. Pegar el token y confirmar

## Ejecutar los tests

```bash
# Todos los tests (unitarios + integración)
mvn test

# Solo tests unitarios (más rápido)
mvn test -Dgroups="unit"

# Con reporte de cobertura JaCoCo
mvn test jacoco:report
# Ver reporte en: target/site/jacoco/index.html
```

## Análisis con SonarQube

### 1. Levantar SonarQube

```bash
docker-compose up -d sonarqube
```

Esperar ~1-2 minutos hasta que esté listo. Acceder a `http://localhost:9000`.

Credenciales por defecto: `admin` / `admin` (te pedirá cambiar la contraseña).

### 2. Crear un token de acceso

En SonarQube: **Administration → Security → Users → Generate Token**

### 3. Ejecutar el análisis

```bash
# Ejecuta los tests, genera reporte JaCoCo y envía métricas a SonarQube
mvn clean verify sonar:sonar \
    -Dsonar.host.url=http://localhost:9000 \
    -Dsonar.login=<tu-token>
```

### 4. Ver los resultados

Acceder a `http://localhost:9000` y buscar el proyecto "bookhub-api".

**Métricas que analiza SonarQube:**
- **Bugs**: errores potenciales en el código
- **Vulnerabilidades**: problemas de seguridad
- **Code Smells**: código que funciona pero es difícil de mantener
- **Coverage**: porcentaje de código cubierto por tests
- **Duplications**: código duplicado
- **Quality Gate**: si el proyecto pasa o falla los umbrales definidos

## Seguridad: flujo JWT

```
1. Cliente envía POST /auth/login con email + password
2. Spring Security → DaoAuthenticationProvider → BCrypt.matches()
3. Si OK → JwtTokenProvider.generateToken() → JWT firmado con HMAC-SHA256
4. Cliente recibe el JWT (válido por 24 horas)
5. En requests siguientes: header "Authorization: Bearer <token>"
6. JwtAuthenticationFilter intercepta cada request:
   a. Extrae el token del header
   b. Valida firma y expiración
   c. Carga el usuario desde la BD
   d. Establece autenticación en SecurityContext
7. Controller recibe la request con el usuario autenticado
```

## Variables de configuración

Las siguientes propiedades se configuran en `application.yml`:

| Propiedad           | Descripción                     | Default            |
|---------------------|---------------------------------|--------------------|
| `jwt.secret`        | Clave de firma del JWT          | (ver application.yml) |
| `jwt.expiration`    | Expiración en ms (24h = 86400000) | 86400000         |
| `spring.datasource.url` | URL de conexión PostgreSQL  | localhost:5432     |

En producción, usar variables de entorno:
```bash
JWT_SECRET=<clave-segura> \
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db> \
mvn spring-boot:run
```

## Conceptos de entrevista cubiertos

Ver el archivo [CONCEPTS.md](CONCEPTS.md) para una explicación detallada de:
- Todas las anotaciones Spring usadas en el proyecto
- Los patrones de diseño aplicados
- Los flujos de Spring Security
- Los tipos de tests y cuándo usar cada uno
