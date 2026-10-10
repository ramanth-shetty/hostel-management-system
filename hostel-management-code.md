# Hostel Management Project Snapshot (fixed version)

Replace these files in your project. **Stop the app and delete the `data/` folder first** (the database tables changed).
Unchanged files not repeated: `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitattributes`, `HELP.md`, `.vscode/`.

## .gitignore

```
HELP.md
target/
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### Secrets & local data (never commit) ###
.env
data/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/
```

## .env.example

```
# Copy this file to ".env" (same folder as pom.xml) and change the values.
# Spring Boot loads it automatically (spring.config.import in application.properties).
HOSTEL_ADMIN_USERNAME=admin
HOSTEL_ADMIN_PASSWORD=change-me-please
HOSTEL_STANDARD_MONTHLY_FEE=5000.00
```

## pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.3</version>
        <relativePath />
    </parent>

    <groupId>com.hostel</groupId>
    <artifactId>hostel-management</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>hostel-management</name>
    <description>Hostel Management System REST API with H2 File Database</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## src/main/resources/application.properties

```properties
# ==========================================
# Application Configuration
# ==========================================
spring.application.name=hostel-management

# Loads a local ".env" file (KEY=value lines) if it exists. Real OS environment variables also work.
spring.config.import=optional:file:.env[.properties]

# ==========================================
# H2 Permanent File Database Configuration
# ==========================================
# 'file:./data/hosteldb' creates a 'data' folder in your project root folder
spring.datasource.url=jdbc:h2:file:./data/hosteldb;AUTO_SERVER=TRUE
spring.datasource.username=sa
spring.datasource.password=

# ==========================================
# H2 Console Configuration
# ==========================================
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=false

# ==========================================
# Admin credentials + fee (from .env or environment variables)
# ==========================================
hostel.admin.username=${HOSTEL_ADMIN_USERNAME}
hostel.admin.password=${HOSTEL_ADMIN_PASSWORD}
hostel.standard-monthly-fee=${HOSTEL_STANDARD_MONTHLY_FEE:0.00}

# ==========================================
# JPA / Hibernate
# ==========================================
# 'update' preserves persisted data across restarts
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
spring.jpa.defer-datasource-initialization=true

# ==========================================
# Errors & session
# ==========================================
server.error.include-stacktrace=never
server.error.include-message=never
server.servlet.session.cookie.same-site=lax
```

## src/main/java/com/hostel/config/SecurityConfig.java

```java
package com.hostel.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${hostel.admin.username}") String username,
            @Value("${hostel.admin.password}") String password) {
        UserDetails admin = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // API calls that are not logged in must get a clean 401 (not a redirect to the HTML login page,
        // which fetch() would silently follow and then fail to parse as JSON).
        RequestMatcher apiRequests = request -> request.getRequestURI().startsWith("/api/");

        http
                // CSRF for a JavaScript front end:
                //  - the token is stored in the readable XSRF-TOKEN cookie, app.js sends it back as X-XSRF-TOKEN
                //  - plain handler (no XOR encoding) so the raw cookie value is accepted
                //  - CsrfCookieFilter forces Spring to actually write the cookie (Spring Security 6 delays it otherwise)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(antMatcher("/h2-console/**")))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(ant("/login", "/css/**", "/js/**", "/images/**",
                                "/404", "/500", "/error", "/error/**"))
                        .permitAll()
                        .requestMatchers(antMatcher("/h2-console/**")).hasRole("ADMIN")
                        .requestMatchers(ant("/api/students/**", "/api/rooms/**", "/api/payments/**",
                                "/api/settings/**"))
                        .hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiRequests))
                .formLogin(form -> form
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /** Explicit Ant matchers avoid Spring Security's "MVC or not?" ambiguity when the H2 servlet is present. */
    private static RequestMatcher[] ant(String... patterns) {
        return Arrays.stream(patterns)
                .map(pattern -> antMatcher(pattern))
                .toArray(RequestMatcher[]::new);
    }

    /** Reads the deferred CSRF token on every request so the XSRF-TOKEN cookie is always present. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
```

## src/main/java/com/hostel/controller/GlobalExceptionHandler.java

```java
package com.hostel.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hostel.exception.ResourceNotFoundException;
import com.hostel.exception.RoomFullException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Only applies to controllers in com.hostel.controller. That is on purpose: a missing static
 * file or page (404) must reach Spring Boot's error page instead of being turned into a 500 here.
 */
@RestControllerAdvice(basePackages = "com.hostel.controller")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler({ ConstraintViolationException.class, HandlerMethodValidationException.class })
    public ResponseEntity<ApiError> handleConstraintViolation(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Validation failed: " + exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + exception.getName() + "'",
                request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedRequest(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is invalid", request, Map.of());
    }

    @ExceptionHandler(RoomFullException.class)
    public ResponseEntity<ApiError> handleRoomFull(RoomFullException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Data integrity violation on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(HttpStatus.CONFLICT, "The request conflicts with existing data", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        // Logged so the real cause is visible in the console instead of being hidden behind a 500.
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message,
            HttpServletRequest request, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    public record ApiError(Instant timestamp, int status, String error, String message,
            String path, Map<String, String> fieldErrors) {
    }
}
```

## src/main/java/com/hostel/controller/PaymentController.java

```java
package com.hostel.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostel.dto.PaymentRequestDTO;
import com.hostel.dto.PaymentResponseDTO;
import com.hostel.service.PaymentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@Validated
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> recordPayment(@Valid @RequestBody PaymentRequestDTO dto) {
        return new ResponseEntity<>(paymentService.recordPayment(dto), HttpStatus.CREATED);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsForStudent(@PathVariable @Positive Long studentId) {
        return ResponseEntity.ok(paymentService.getPaymentsForStudent(studentId));
    }
}
```

## src/main/java/com/hostel/controller/RoomController.java

```java
package com.hostel.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostel.dto.RoomResponseDTO;
import com.hostel.service.RoomService;

import jakarta.validation.constraints.Pattern;

@RestController
@Validated
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{roomNumber}")
    public ResponseEntity<RoomResponseDTO> getRoom(
            @PathVariable @Pattern(regexp = "^[A-Z]-[1-9][0-9]{2}$", message = "Room number must match the format A-101") String roomNumber) {
        return ResponseEntity.ok(roomService.getRoom(roomNumber));
    }
}
```

## src/main/java/com/hostel/controller/SettingController.java

```java
package com.hostel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hostel.dto.SettingRequestDTO;
import com.hostel.dto.SettingResponseDTO;
import com.hostel.service.SettingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/standard-monthly-fee")
    public ResponseEntity<SettingResponseDTO> getStandardMonthlyFee() {
        return ResponseEntity.ok(new SettingResponseDTO(
                SettingService.STANDARD_MONTHLY_FEE, settingService.getStandardMonthlyFee()));
    }

    @PutMapping("/standard-monthly-fee")
    public ResponseEntity<SettingResponseDTO> updateStandardMonthlyFee(@Valid @RequestBody SettingRequestDTO dto) {
        return ResponseEntity.ok(new SettingResponseDTO(
                SettingService.STANDARD_MONTHLY_FEE, settingService.updateStandardMonthlyFee(dto.value())));
    }
}
```

## src/main/java/com/hostel/controller/StudentController.java

```java
package com.hostel.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hostel.dto.StudentRequestDTO;
import com.hostel.dto.StudentResponseDTO;
import com.hostel.service.StudentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

@RestController
@Validated
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentResponseDTO> createStudent(@Valid @RequestBody StudentRequestDTO dto) {
        return new ResponseEntity<>(studentService.createStudent(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> getStudentById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> updateStudent(@PathVariable @Positive Long id,
            @Valid @RequestBody StudentRequestDTO dto) {
        return ResponseEntity.ok(studentService.updateStudent(id, dto));
    }

    /** Soft delete = check-out. History and payments are preserved. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable @Positive Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports/pending-fees")
    public ResponseEntity<List<StudentResponseDTO>> getPendingFeesReport(
            @RequestParam(defaultValue = "0.00") @DecimalMin("0.00") BigDecimal minAmount) {
        return ResponseEntity.ok(studentService.getPendingFeesReport(minAmount));
    }
}
```

## src/main/java/com/hostel/dto/PaymentRequestDTO.java

```java
package com.hostel.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PaymentRequestDTO(

        @NotNull(message = "Student is required")
        @Positive(message = "Student id must be positive")
        Long studentId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "Payment mode is required")
        @Pattern(regexp = "^(CASH|UPI|CARD|BANK_TRANSFER)$", message = "Payment mode must be CASH, UPI, CARD or BANK_TRANSFER")
        String paymentMode) {
}
```

## src/main/java/com/hostel/dto/PaymentResponseDTO.java

```java
package com.hostel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDTO(
        Long id,
        Long studentId,
        BigDecimal amount,
        LocalDateTime paymentDate,
        String paymentMode) {
}
```

## src/main/java/com/hostel/dto/RoomResponseDTO.java

```java
package com.hostel.dto;

import java.util.List;

public record RoomResponseDTO(
        String roomNumber,
        int capacity,
        int currentOccupancy,
        String status,
        List<String> residents) {
}
```

## src/main/java/com/hostel/dto/SettingRequestDTO.java

```java
package com.hostel.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record SettingRequestDTO(

        @NotNull(message = "Fee is required")
        @DecimalMin(value = "0.00", message = "Fee cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal value) {
}
```

## src/main/java/com/hostel/dto/SettingResponseDTO.java

```java
package com.hostel.dto;

import java.math.BigDecimal;

public record SettingResponseDTO(String name, BigDecimal value) {
}
```

## src/main/java/com/hostel/dto/StudentRequestDTO.java

```java
package com.hostel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/students and PUT /api/students/{id}.
 * Fees are NOT editable here. initialPayment/paymentMode are only used on create
 * (they become a PaymentRecord); later payments go through POST /api/payments.
 */
public record StudentRequestDTO(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Room number is required")
        @Pattern(regexp = "^[A-Z]-[1-9][0-9]{2}$", message = "Room number must match the format A-101")
        String roomNumber,

        @NotNull(message = "Admission date is required")
        LocalDate admissionDate,

        @DecimalMin(value = "0.00", message = "Initial payment cannot be negative")
        @Digits(integer = 10, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal initialPayment,

        @Pattern(regexp = "^(CASH|UPI|CARD|BANK_TRANSFER)$", message = "Payment mode must be CASH, UPI, CARD or BANK_TRANSFER")
        String paymentMode) {
}
```

## src/main/java/com/hostel/dto/StudentResponseDTO.java

```java
package com.hostel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentResponseDTO(
        Long id,
        String name,
        String roomNumber,
        LocalDate admissionDate,
        BigDecimal feesPaid,
        BigDecimal pendingFees,
        String status) {
}
```

## src/main/java/com/hostel/entity/PaymentRecord.java

```java
package com.hostel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "payment_records", indexes = @Index(name = "idx_payment_student", columnList = "student_id"))
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "amount_paid", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @NotNull
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @NotBlank
    @Column(name = "payment_mode", length = 32, nullable = false)
    private String paymentMode;

    public PaymentRecord() {
    }

    public PaymentRecord(Long studentId, BigDecimal amount, LocalDateTime paymentDate, String paymentMode) {
        this.studentId = studentId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMode = paymentMode;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getPaymentMode() {
        return paymentMode;
    }
}
```

## src/main/java/com/hostel/entity/Room.java

```java
package com.hostel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "rooms")
public class Room {

    public static final int MAX_CAPACITY = 4;
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_FULL = "FULL";

    @Id
    @NotBlank
    @Pattern(regexp = "^[A-Z]-[1-9][0-9]{2}$")
    @Column(name = "room_number", length = 5, unique = true, nullable = false, updatable = false)
    private String roomNumber;

    @Column(name = "capacity", nullable = false, updatable = false)
    private int capacity = MAX_CAPACITY;

    @Column(name = "status", length = 9, nullable = false)
    private String status = STATUS_AVAILABLE;

    /** Not stored: filled in by the service from the count of ACTIVE residents. */
    @Transient
    private int currentOccupancy;

    public Room() {
    }

    public Room(String roomNumber) {
        this.roomNumber = roomNumber;
        this.capacity = MAX_CAPACITY;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentOccupancy() {
        return currentOccupancy;
    }

    public void setCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = Math.max(0, Math.min(currentOccupancy, MAX_CAPACITY));
    }
}
```

## src/main/java/com/hostel/entity/Student.java

```java
package com.hostel.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A resident. NOTE: feesPaid / pendingFees are NOT stored here any more.
 * They are always calculated from PaymentRecord rows + the standard monthly fee
 * (see StudentService), exactly as System.md requires.
 */
@Entity
@Table(name = "students")
public class Student {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @NotBlank(message = "Room number is required")
    @Pattern(regexp = "^[A-Z]-[1-9][0-9]{2}$", message = "Room number must match the format A-101")
    @Column(name = "room_number", length = 5, nullable = false)
    private String roomNumber;

    @NotNull(message = "Admission date is required")
    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "status", length = 12, nullable = false)
    private String status = STATUS_ACTIVE;

    public Student() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
```

## src/main/java/com/hostel/entity/SystemSetting.java

```java
package com.hostel.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_name", unique = true, nullable = false)
    private String settingName;

    @Column(name = "setting_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal settingValue;

    public SystemSetting() {
    }

    public SystemSetting(String settingName, BigDecimal settingValue) {
        this.settingName = settingName;
        this.settingValue = settingValue;
    }

    public Long getId() {
        return id;
    }

    public String getSettingName() {
        return settingName;
    }

    public BigDecimal getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(BigDecimal settingValue) {
        this.settingValue = settingValue;
    }
}
```

## src/main/java/com/hostel/exception/ResourceNotFoundException.java

```java
package com.hostel.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

## src/main/java/com/hostel/exception/RoomFullException.java

```java
package com.hostel.exception;

public class RoomFullException extends RuntimeException {

    public RoomFullException(String message) {
        super(message);
    }
}
```

## src/main/java/com/hostel/hostel_management/HostelManagementApplication.java

```java
package com.hostel.hostel_management;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.hostel.entity.SystemSetting;
import com.hostel.repository.SystemSettingRepository;
import com.hostel.service.SettingService;

@SpringBootApplication(scanBasePackages = "com.hostel")
@EnableJpaRepositories(basePackages = "com.hostel.repository")
@EntityScan(basePackages = "com.hostel.entity")
public class HostelManagementApplication {

	/** Creates the standard_monthly_fee row on first start. Later changes are made through /api/settings. */
	@Bean
	CommandLineRunner initializeStandardMonthlyFee(
			SystemSettingRepository systemSettingRepository,
			@Value("${hostel.standard-monthly-fee}") BigDecimal standardMonthlyFee) {
		return args -> {
			if (systemSettingRepository.findBySettingName(SettingService.STANDARD_MONTHLY_FEE).isEmpty()) {
				systemSettingRepository.save(new SystemSetting(
						SettingService.STANDARD_MONTHLY_FEE,
						standardMonthlyFee.setScale(2, RoundingMode.HALF_UP)));
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(HostelManagementApplication.class, args);
	}

}
```

## src/main/java/com/hostel/repository/PaymentRecordRepository.java

```java
package com.hostel.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostel.entity.PaymentRecord;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    List<PaymentRecord> findByStudentIdOrderByPaymentDateDescIdDesc(Long studentId);

    /** Returns null when the student has no payments yet (the service converts that to zero). */
    @Query("select sum(p.amount) from PaymentRecord p where p.studentId = :studentId")
    BigDecimal sumAmountByStudentId(@Param("studentId") Long studentId);

    /** One row per student: [studentId, totalPaid]. Used by list screens to avoid one query per student. */
    @Query("select p.studentId, sum(p.amount) from PaymentRecord p group by p.studentId")
    List<Object[]> sumAmountGroupedByStudent();
}
```

## src/main/java/com/hostel/repository/RoomRepository.java

```java
package com.hostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostel.entity.Room;

import jakarta.persistence.LockModeType;

public interface RoomRepository extends JpaRepository<Room, String> {

    /** Row-locks the room so two simultaneous registrations cannot both take the last bed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.roomNumber = :roomNumber")
    Optional<Room> findForUpdate(@Param("roomNumber") String roomNumber);

    List<Room> findAllByOrderByRoomNumberAsc();
}
```

## src/main/java/com/hostel/repository/StudentRepository.java

```java
package com.hostel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByStatus(String status);

    List<Student> findByRoomNumberAndStatus(String roomNumber, String status);

    long countByRoomNumberAndStatus(String roomNumber, String status);
}
```

## src/main/java/com/hostel/repository/SystemSettingRepository.java

```java
package com.hostel.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hostel.entity.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingName(String settingName);
}
```

## src/main/java/com/hostel/service/PaymentService.java

```java
package com.hostel.service;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.dto.PaymentRequestDTO;
import com.hostel.dto.PaymentResponseDTO;
import com.hostel.entity.PaymentRecord;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.PaymentRecordRepository;
import com.hostel.repository.StudentRepository;

@Service
public class PaymentService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final StudentRepository studentRepository;

    public PaymentService(PaymentRecordRepository paymentRecordRepository, StudentRepository studentRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public PaymentResponseDTO recordPayment(PaymentRequestDTO dto) {
        requireStudent(dto.studentId());
        PaymentRecord saved = paymentRecordRepository.save(new PaymentRecord(
                dto.studentId(),
                dto.amount().setScale(2, RoundingMode.HALF_UP),
                LocalDateTime.now(),
                dto.paymentMode()));
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsForStudent(Long studentId) {
        requireStudent(studentId);
        return paymentRecordRepository.findByStudentIdOrderByPaymentDateDescIdDesc(studentId).stream()
                .map(this::toDTO)
                .toList();
    }

    private void requireStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
    }

    private PaymentResponseDTO toDTO(PaymentRecord record) {
        return new PaymentResponseDTO(record.getId(), record.getStudentId(), record.getAmount(),
                record.getPaymentDate(), record.getPaymentMode());
    }
}
```

## src/main/java/com/hostel/service/RoomService.java

```java
package com.hostel.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.dto.RoomResponseDTO;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    public RoomService(RoomRepository roomRepository, StudentRepository studentRepository) {
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getAllRooms() {
        Map<String, List<String>> residentsByRoom = studentRepository.findByStatus(Student.STATUS_ACTIVE).stream()
                .collect(Collectors.groupingBy(Student::getRoomNumber,
                        Collectors.mapping(Student::getName, Collectors.toList())));
        return roomRepository.findAllByOrderByRoomNumberAsc().stream()
                .map(room -> toDTO(room, residentsByRoom.getOrDefault(room.getRoomNumber(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponseDTO getRoom(String roomNumber) {
        Room room = roomRepository.findById(roomNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomNumber));
        List<String> residents = studentRepository.findByRoomNumberAndStatus(roomNumber, Student.STATUS_ACTIVE)
                .stream().map(Student::getName).toList();
        return toDTO(room, residents);
    }

    private RoomResponseDTO toDTO(Room room, List<String> residents) {
        int occupancy = residents.size();
        String status = occupancy >= Room.MAX_CAPACITY ? Room.STATUS_FULL : Room.STATUS_AVAILABLE;
        return new RoomResponseDTO(room.getRoomNumber(), room.getCapacity(), occupancy, status, residents);
    }
}
```

## src/main/java/com/hostel/service/SettingService.java

```java
package com.hostel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.entity.SystemSetting;
import com.hostel.repository.SystemSettingRepository;

@Service
public class SettingService {

    public static final String STANDARD_MONTHLY_FEE = "standard_monthly_fee";

    private final SystemSettingRepository systemSettingRepository;

    public SettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal getStandardMonthlyFee() {
        return systemSettingRepository.findBySettingName(STANDARD_MONTHLY_FEE)
                .map(SystemSetting::getSettingValue)
                .orElseThrow(() -> new IllegalStateException("Standard monthly fee not configured in system settings"));
    }

    @Transactional
    public BigDecimal updateStandardMonthlyFee(BigDecimal value) {
        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP);
        SystemSetting setting = systemSettingRepository.findBySettingName(STANDARD_MONTHLY_FEE)
                .orElseGet(() -> new SystemSetting(STANDARD_MONTHLY_FEE, scaled));
        setting.setSettingValue(scaled);
        return systemSettingRepository.save(setting).getSettingValue();
    }
}
```

## src/main/java/com/hostel/service/StudentService.java

```java
package com.hostel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hostel.dto.StudentRequestDTO;
import com.hostel.dto.StudentResponseDTO;
import com.hostel.entity.PaymentRecord;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.exception.RoomFullException;
import com.hostel.repository.PaymentRecordRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;

@Service
public class StudentService {

    private static final Pattern ROOM_NUMBER_PATTERN = Pattern.compile("^[A-Z]-[1-9][0-9]{2}$");

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final SettingService settingService;

    public StudentService(StudentRepository studentRepository, RoomRepository roomRepository,
            PaymentRecordRepository paymentRecordRepository, SettingService settingService) {
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.settingService = settingService;
    }

    // ------------------------------------------------------------------ create

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO dto) {
        requireValidRoomNumber(dto.roomNumber());

        Room room = findOrCreateRoomForUpdate(dto.roomNumber());
        ensureRoomHasCapacity(room);

        Student student = new Student();
        student.setName(dto.name().trim());
        student.setRoomNumber(dto.roomNumber());
        student.setAdmissionDate(dto.admissionDate());
        student.setStatus(Student.STATUS_ACTIVE);
        Student saved = studentRepository.save(student);

        // An initial payment becomes a real PaymentRecord, so "fees paid" and "pending"
        // are always derived from the same source of truth.
        BigDecimal initial = dto.initialPayment();
        if (initial != null && initial.compareTo(BigDecimal.ZERO) > 0) {
            String mode = dto.paymentMode() == null ? "CASH" : dto.paymentMode();
            paymentRecordRepository.save(new PaymentRecord(
                    saved.getId(), initial.setScale(2, RoundingMode.HALF_UP), LocalDateTime.now(), mode));
        }

        refreshRoomState(room);
        return toDTO(saved);
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getAllStudents() {
        BigDecimal fee = settingService.getStandardMonthlyFee();
        Map<Long, BigDecimal> paid = paidTotalsByStudent();
        return studentRepository.findAll(Sort.by("id")).stream()
                .map(s -> toDTO(s, fee, paid.getOrDefault(s.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentById(Long id) {
        return toDTO(findStudent(id));
    }

    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getPendingFeesReport(BigDecimal minAmount) {
        return getAllStudents().stream()
                .filter(s -> s.pendingFees().compareTo(minAmount) > 0)
                .toList();
    }

    // ------------------------------------------------------------------ update

    @Transactional
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO dto) {
        Student student = findStudent(id);
        requireValidRoomNumber(dto.roomNumber());

        if (!Student.STATUS_ACTIVE.equals(student.getStatus())) {
            throw new IllegalArgumentException("Checked-out residents cannot be edited");
        }

        Room previousRoom = null;
        Room destinationRoom = null;
        if (!student.getRoomNumber().equals(dto.roomNumber())) {
            destinationRoom = findOrCreateRoomForUpdate(dto.roomNumber());
            ensureRoomHasCapacity(destinationRoom);
            previousRoom = roomRepository.findForUpdate(student.getRoomNumber()).orElse(null);
            student.setRoomNumber(dto.roomNumber());
        }

        student.setName(dto.name().trim());
        student.setAdmissionDate(dto.admissionDate());
        Student updated = studentRepository.save(student);

        refreshRoomState(previousRoom);
        refreshRoomState(destinationRoom);
        return toDTO(updated);
    }

    // ---------------------------------------------------------------- checkout

    /** DELETE /api/students/{id} is a soft delete: the profile and payments stay for audit. */
    @Transactional
    public void deleteStudent(Long id) {
        checkoutStudent(id);
    }

    @Transactional
    public StudentResponseDTO checkoutStudent(Long id) {
        Student student = findStudent(id);
        if (Student.STATUS_ACTIVE.equals(student.getStatus())) {
            Room room = roomRepository.findForUpdate(student.getRoomNumber()).orElse(null);
            student.setStatus(Student.STATUS_CHECKED_OUT);
            studentRepository.save(student);
            refreshRoomState(room);
        }
        return toDTO(student);
    }

    // ----------------------------------------------------------------- helpers

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    private StudentResponseDTO toDTO(Student student) {
        return toDTO(student, settingService.getStandardMonthlyFee(), totalPaid(student.getId()));
    }

    private StudentResponseDTO toDTO(Student student, BigDecimal fee, BigDecimal paid) {
        BigDecimal pending = fee.subtract(paid).max(BigDecimal.ZERO);
        return new StudentResponseDTO(student.getId(), student.getName(), student.getRoomNumber(),
                student.getAdmissionDate(), paid, pending, student.getStatus());
    }

    private BigDecimal totalPaid(Long studentId) {
        BigDecimal total = paymentRecordRepository.sumAmountByStudentId(studentId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private Map<Long, BigDecimal> paidTotalsByStudent() {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Object[] row : paymentRecordRepository.sumAmountGroupedByStudent()) {
            totals.put(((Number) row[0]).longValue(), new BigDecimal(row[1].toString()));
        }
        return totals;
    }

    private Room findOrCreateRoomForUpdate(String roomNumber) {
        return roomRepository.findForUpdate(roomNumber)
                .orElseGet(() -> roomRepository.saveAndFlush(new Room(roomNumber)));
    }

    private void ensureRoomHasCapacity(Room room) {
        if (activeOccupancy(room.getRoomNumber()) >= Room.MAX_CAPACITY) {
            throw new RoomFullException("Room " + room.getRoomNumber() + " is full. Cannot register resident.");
        }
    }

    /** Recounts ACTIVE residents and updates the stored AVAILABLE / FULL status. */
    private void refreshRoomState(Room room) {
        if (room == null) {
            return;
        }
        int occupancy = activeOccupancy(room.getRoomNumber());
        room.setCurrentOccupancy(occupancy);
        room.setStatus(occupancy >= Room.MAX_CAPACITY ? Room.STATUS_FULL : Room.STATUS_AVAILABLE);
        roomRepository.save(room);
    }

    private int activeOccupancy(String roomNumber) {
        return (int) studentRepository.countByRoomNumberAndStatus(roomNumber, Student.STATUS_ACTIVE);
    }

    private void requireValidRoomNumber(String roomNumber) {
        if (roomNumber == null || !ROOM_NUMBER_PATTERN.matcher(roomNumber).matches()) {
            throw new IllegalArgumentException("Invalid room number format. Expected format: A-101, B-205, etc.");
        }
    }
}
```

## src/main/resources/static/app.js

```javascript
// ---------------------------------------------------------------------------
// Hostel dashboard front end. Talks to the Spring Boot API:
//   /api/students  /api/payments  /api/rooms  /api/settings
// Money is NEVER calculated here: feesPaid / pendingFees always come from the server.
// ---------------------------------------------------------------------------
const API = {
  students: "/api/students",
  payments: "/api/payments",
  rooms: "/api/rooms",
  fee: "/api/settings/standard-monthly-fee",
};
const ROOM_PATTERN = /^[A-Z]-[1-9][0-9]{2}$/;
const ROOM_ERROR = "Room number must match the format A-101";

const state = {
  students: [],
  pendingOnly: false,
  query: "",
  checkoutId: null,
  paymentStudentId: null,
};

const $ = (id) => document.getElementById(id);

document.addEventListener("DOMContentLoaded", () => {
  $("student-form").addEventListener("submit", handleFormSubmit);
  $("btn-cancel").addEventListener("click", resetForm);
  $("btn-refresh").addEventListener("click", () => {
    fetchStudents();
    loadFee();
  });
  $("btn-pending").addEventListener("click", togglePendingFilter);
  $("student-search").addEventListener("input", (event) => {
    state.query = event.target.value.trim().toLowerCase();
    renderAll();
  });
  $("student-table-body").addEventListener("click", handleTableAction);

  // Real-time room number check (A-101 style)
  $("roomNumber").addEventListener("input", (event) => {
    event.target.value = event.target.value.toUpperCase();
    if (ROOM_PATTERN.test(event.target.value)) clearFieldError("roomNumber");
  });
  $("roomNumber").addEventListener("blur", (event) => {
    const value = event.target.value.trim();
    if (value && !ROOM_PATTERN.test(value)) {
      renderFieldErrors({ roomNumber: ROOM_ERROR });
    }
  });

  $("btn-confirm-action").addEventListener("click", confirmCheckout);
  $("payment-form").addEventListener("submit", handlePaymentSubmit);
  $("btn-rooms").addEventListener("click", openRooms);
  $("btn-fee").addEventListener("click", openFeeModal);
  $("fee-form").addEventListener("submit", handleFeeSubmit);
  $("btn-logout").addEventListener("click", logout);
  $("btn-mobile-register").addEventListener("click", () => {
    $("student-form").scrollIntoView({ behavior: "smooth" });
    $("name").focus();
  });

  document.querySelectorAll("[data-close-modal]").forEach((button) =>
    button.addEventListener("click", () =>
      closeModal(button.closest(".modal-overlay").id),
    ),
  );
  document.querySelectorAll(".modal-overlay").forEach((overlay) =>
    overlay.addEventListener("click", (event) => {
      if (event.target === overlay) closeModal(overlay.id);
    }),
  );
  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    document
      .querySelectorAll(".modal-overlay:not(.hidden)")
      .forEach((overlay) => closeModal(overlay.id));
  });

  fetchStudents();
  loadFee();
});

// ------------------------------------------------------------------ HTTP

async function apiRequest(url, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = { Accept: "application/json", ...(options.headers || {}) };
  if (method !== "GET" && method !== "HEAD") {
    const csrfToken = getCookie("XSRF-TOKEN");
    if (csrfToken) headers["X-XSRF-TOKEN"] = decodeURIComponent(csrfToken);
  }

  const response = await fetch(url, { ...options, headers });

  // Session expired: the server answers 401 (or redirects to the HTML login page).
  if (
    response.status === 401 ||
    (response.redirected && response.url.includes("/login"))
  ) {
    window.location.href = "/login";
    throw new Error("Session expired. Redirecting to login...");
  }

  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : null;

  if (!response.ok) {
    const fallback =
      response.status === 403
        ? "Security token missing or expired. Refresh the page and try again."
        : `Request failed (${response.status})`;
    const error = new Error(body?.message || fallback);
    error.status = response.status;
    error.fieldErrors = body?.fieldErrors || {};
    throw error;
  }
  return body;
}

// -------------------------------------------------------------- residents

async function fetchStudents() {
  setTableLoading();
  try {
    // Always load everyone; the "pending only" toggle filters on screen so the stat cards stay correct.
    state.students = (await apiRequest(API.students)) || [];
    renderAll();
  } catch (error) {
    renderTableError(error.message);
    showToast(error.message, "error");
  }
}

function renderAll() {
  renderTable(getVisibleStudents());
  updateStats(state.students);
}

function getVisibleStudents() {
  return state.students.filter((student) => {
    if (state.pendingOnly && Number(student.pendingFees || 0) <= 0)
      return false;
    if (!state.query) return true;
    return [student.name, student.roomNumber, student.status].some((value) =>
      String(value || "")
        .toLowerCase()
        .includes(state.query),
    );
  });
}

function renderTable(students) {
  const tbody = $("student-table-body");
  if (!students.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="py-10 text-center text-slate-400"><i class="fa-solid fa-inbox text-2xl mb-2 block"></i>No residents match this view.</td></tr>`;
    return;
  }

  tbody.innerHTML = students
    .map((student) => {
      const pending = Number(student.pendingFees || 0);
      const active = student.status === "ACTIVE";
      const disabledEdit = active ? "" : "disabled";
      const dimmed = active ? "" : "opacity-40 cursor-not-allowed";
      return `
        <tr class="hover:bg-slate-50/80 transition">
          <td class="py-3 px-4 font-mono text-xs text-slate-500">#${escapeHtml(student.id)}</td>
          <td class="py-3 px-4 font-medium text-slate-900">${escapeHtml(student.name)}</td>
          <td class="py-3 px-4"><span class="px-2 py-1 bg-slate-100 rounded text-xs font-semibold text-slate-700">${escapeHtml(student.roomNumber)}</span></td>
          <td class="py-3 px-4 text-slate-600">${escapeHtml(student.admissionDate)}</td>
          <td class="py-3 px-4 text-right font-medium text-emerald-600">${formatCurrency(student.feesPaid)}</td>
          <td class="py-3 px-4 text-right font-medium ${pending > 0 ? "text-rose-600" : "text-slate-400"}">${formatCurrency(pending)}</td>
          <td class="py-3 px-4 text-center whitespace-nowrap">
            <span class="mr-2 px-2 py-1 rounded-full text-[10px] font-bold ${active ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"}">${escapeHtml(student.status)}</span>
            <button type="button" data-action="pay" data-id="${escapeHtml(student.id)}" class="p-1.5 text-slate-500 hover:text-emerald-600 hover:bg-emerald-50 rounded transition" title="Record payment / history"><i class="fa-solid fa-indian-rupee-sign"></i></button>
            <button type="button" data-action="edit" data-id="${escapeHtml(student.id)}" class="p-1.5 text-slate-500 hover:text-brand-600 hover:bg-slate-100 rounded transition ${dimmed}" title="Edit resident" ${disabledEdit}><i class="fa-solid fa-pen-to-square"></i></button>
            <button type="button" data-action="checkout" data-id="${escapeHtml(student.id)}" class="p-1.5 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded transition ${dimmed}" title="Check out resident" ${disabledEdit}><i class="fa-solid fa-right-from-bracket"></i></button>
          </td>
        </tr>`;
    })
    .join("");
}

function updateStats(students) {
  const active = students.filter((student) => student.status === "ACTIVE");
  const collected = students.reduce(
    (sum, student) => sum + Number(student.feesPaid || 0),
    0,
  );
  const pending = students.reduce(
    (sum, student) => sum + Number(student.pendingFees || 0),
    0,
  );
  $("stat-total").textContent = active.length;
  $("stat-collected").textContent = formatCurrency(collected);
  $("stat-pending").textContent = formatCurrency(pending);
}

async function handleFormSubmit(event) {
  event.preventDefault();
  clearFieldErrors();

  const id = $("student-id").value;
  const roomNumber = $("roomNumber").value.trim().toUpperCase();
  if (!ROOM_PATTERN.test(roomNumber)) {
    renderFieldErrors({ roomNumber: ROOM_ERROR });
    return;
  }

  const payload = {
    name: $("name").value.trim(),
    roomNumber,
    admissionDate: $("admissionDate").value,
  };
  if (!id) {
    const initial = $("initialPayment").value;
    payload.initialPayment = initial === "" ? 0 : Number(initial);
    payload.paymentMode = $("paymentMode").value;
  }

  const button = $("btn-submit");
  setButtonLoading(button, true, id ? "Updating..." : "Registering...");
  let success = false;
  try {
    await apiRequest(id ? `${API.students}/${id}` : API.students, {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    success = true;
  } catch (error) {
    renderFieldErrors(error.fieldErrors);
    showToast(error.message, "error");
  } finally {
    setButtonLoading(button, false);
  }

  if (success) {
    resetForm();
    showToast(id ? "Resident updated." : "Resident registered.", "success");
    await fetchStudents();
  }
}

async function editStudent(id) {
  try {
    const student = await apiRequest(`${API.students}/${id}`);
    $("student-id").value = student.id;
    $("name").value = student.name || "";
    $("roomNumber").value = student.roomNumber || "";
    $("admissionDate").value = student.admissionDate || "";
    $("initial-payment-block").classList.add("hidden");
    $("form-title").textContent = "Edit Resident Details";
    $("edit-badge").classList.remove("hidden");
    $("btn-cancel").classList.remove("hidden");
    $("btn-submit").innerHTML =
      '<i class="fa-solid fa-floppy-disk"></i> Update Resident';
    $("student-form").scrollIntoView({ behavior: "smooth" });
    $("name").focus();
  } catch (error) {
    showToast(error.message, "error");
  }
}

function resetForm() {
  $("student-form").reset();
  $("student-id").value = "";
  $("initial-payment-block").classList.remove("hidden");
  $("form-title").textContent = "Add New Resident";
  $("edit-badge").classList.add("hidden");
  $("btn-cancel").classList.add("hidden");
  $("btn-submit").innerHTML =
    '<i class="fa-solid fa-plus"></i> Register Resident';
  clearFieldErrors();
}

function handleTableAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button || button.disabled) return;
  const id = button.dataset.id;
  if (button.dataset.action === "edit") editStudent(id);
  if (button.dataset.action === "checkout") requestCheckout(id);
  if (button.dataset.action === "pay") openPayment(id);
}

function togglePendingFilter() {
  state.pendingOnly = !state.pendingOnly;
  const button = $("btn-pending");
  button.classList.toggle("ring-2", state.pendingOnly);
  button.classList.toggle("ring-rose-300", state.pendingOnly);
  button.setAttribute("aria-pressed", String(state.pendingOnly));
  renderAll();
}

// --------------------------------------------------------------- checkout

function requestCheckout(id) {
  state.checkoutId = id;
  openModal("action-modal");
}

async function confirmCheckout() {
  if (!state.checkoutId) return;
  const button = $("btn-confirm-action");
  setButtonLoading(button, true, "Checking out...");
  let success = false;
  try {
    await apiRequest(`${API.students}/${state.checkoutId}`, {
      method: "DELETE",
    });
    success = true;
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setButtonLoading(button, false);
  }
  if (success) {
    closeModal("action-modal");
    showToast("Resident checked out. History preserved.", "success");
    await fetchStudents();
  }
}

// --------------------------------------------------------------- payments

function openPayment(id) {
  const student = state.students.find((s) => String(s.id) === String(id));
  if (!student) return;
  state.paymentStudentId = student.id;
  $("pay-student-name").textContent = student.name;
  $("pay-pending").textContent = formatCurrency(student.pendingFees);
  $("pay-amount").value = "";
  $("pay-mode").value = "CASH";
  setInlineError("pay-error", "");
  openModal("payment-modal");
  loadPaymentHistory(student.id);
  $("pay-amount").focus();
}

async function loadPaymentHistory(studentId) {
  const list = $("pay-history");
  list.innerHTML = `<li class="py-2 text-slate-400">Loading...</li>`;
  try {
    const payments = (await apiRequest(`${API.payments}/student/${studentId}`)) || [];
    if (!payments.length) {
      list.innerHTML = `<li class="py-2 text-slate-400">No payments recorded yet.</li>`;
      return;
    }
    list.innerHTML = payments
      .map(
        (payment) => `
        <li class="py-2 flex items-center justify-between">
          <span class="text-slate-600">${escapeHtml(formatDateTime(payment.paymentDate))}
            <span class="ml-2 px-1.5 py-0.5 bg-slate-100 rounded text-[10px] font-semibold">${escapeHtml(payment.paymentMode)}</span></span>
          <span class="font-medium text-emerald-600">${formatCurrency(payment.amount)}</span>
        </li>`,
      )
      .join("");
  } catch (error) {
    list.innerHTML = `<li class="py-2 text-rose-500">${escapeHtml(error.message)}</li>`;
  }
}

async function handlePaymentSubmit(event) {
  event.preventDefault();
  setInlineError("pay-error", "");
  const amount = Number($("pay-amount").value);
  if (!(amount > 0)) {
    setInlineError("pay-error", "Enter an amount greater than 0.");
    return;
  }

  const button = $("btn-pay-submit");
  setButtonLoading(button, true, "Saving...");
  let success = false;
  try {
    await apiRequest(API.payments, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        studentId: state.paymentStudentId,
        amount,
        paymentMode: $("pay-mode").value,
      }),
    });
    success = true;
  } catch (error) {
    const detail = Object.values(error.fieldErrors || {})[0];
    setInlineError("pay-error", detail || error.message);
  } finally {
    setButtonLoading(button, false);
  }

  if (success) {
    showToast("Payment recorded.", "success");
    $("pay-amount").value = "";
    await fetchStudents();
    const student = state.students.find(
      (s) => String(s.id) === String(state.paymentStudentId),
    );
    if (student) $("pay-pending").textContent = formatCurrency(student.pendingFees);
    loadPaymentHistory(state.paymentStudentId);
  }
}

// ------------------------------------------------------------------ rooms

async function openRooms() {
  const body = $("rooms-body");
  body.innerHTML = `<p class="text-slate-400 text-sm">Loading rooms...</p>`;
  openModal("rooms-modal");
  try {
    const rooms = (await apiRequest(API.rooms)) || [];
    if (!rooms.length) {
      body.innerHTML = `<p class="text-slate-400 text-sm">No rooms yet. Rooms are created when you register the first resident in them.</p>`;
      return;
    }
    body.innerHTML = rooms
      .map((room) => {
        const full = room.status === "FULL";
        const residents = room.residents.length
          ? room.residents.map((name) => `<li>${escapeHtml(name)}</li>`).join("")
          : `<li class="text-slate-400">Empty</li>`;
        return `
          <div class="border border-slate-200 rounded-lg p-3">
            <div class="flex items-center justify-between">
              <span class="font-bold text-slate-900">${escapeHtml(room.roomNumber)}</span>
              <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${full ? "bg-rose-100 text-rose-700" : "bg-emerald-100 text-emerald-700"}">${escapeHtml(room.status)}</span>
            </div>
            <p class="text-xs text-slate-500 mt-1">${room.currentOccupancy} / ${room.capacity} beds used</p>
            <ul class="mt-2 text-sm text-slate-700 list-disc list-inside">${residents}</ul>
          </div>`;
      })
      .join("");
  } catch (error) {
    body.innerHTML = `<p class="text-rose-500 text-sm">${escapeHtml(error.message)}</p>`;
  }
}

// ---------------------------------------------------------- monthly fee

async function loadFee() {
  try {
    const setting = await apiRequest(API.fee);
    state.fee = setting?.value;
    $("fee-chip").textContent = formatCurrency(setting?.value);
  } catch (error) {
    $("fee-chip").textContent = "?";
  }
}

function openFeeModal() {
  $("fee-input").value = state.fee ?? "";
  setInlineError("fee-error", "");
  openModal("fee-modal");
  $("fee-input").focus();
}

async function handleFeeSubmit(event) {
  event.preventDefault();
  setInlineError("fee-error", "");
  const raw = $("fee-input").value;
  const value = Number(raw);
  if (raw === "" || Number.isNaN(value) || value < 0) {
    setInlineError("fee-error", "Enter a fee of 0 or more.");
    return;
  }
  const button = $("btn-fee-submit");
  setButtonLoading(button, true, "Saving...");
  let success = false;
  try {
    await apiRequest(API.fee, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ value }),
    });
    success = true;
  } catch (error) {
    const detail = Object.values(error.fieldErrors || {})[0];
    setInlineError("fee-error", detail || error.message);
  } finally {
    setButtonLoading(button, false);
  }
  if (success) {
    closeModal("fee-modal");
    showToast("Monthly fee updated.", "success");
    await Promise.all([loadFee(), fetchStudents()]);
  }
}

// ----------------------------------------------------------------- logout

function logout() {
  // POST /logout needs the CSRF token; a plain form submit works with the redirect.
  const form = document.createElement("form");
  form.method = "POST";
  form.action = "/logout";
  const input = document.createElement("input");
  input.type = "hidden";
  input.name = "_csrf";
  input.value = decodeURIComponent(getCookie("XSRF-TOKEN") || "");
  form.appendChild(input);
  document.body.appendChild(form);
  form.submit();
}

// ------------------------------------------------------------- UI helpers

function openModal(id) {
  $(id).classList.remove("hidden");
}

function closeModal(id) {
  $(id).classList.add("hidden");
  if (id === "action-modal") state.checkoutId = null;
  if (id === "payment-modal") state.paymentStudentId = null;
}

function setTableLoading() {
  $("student-table-body").innerHTML = Array.from(
    { length: 4 },
    () =>
      `<tr class="animate-pulse"><td colspan="7" class="py-4 px-4"><div class="h-4 bg-slate-100 rounded"></div></td></tr>`,
  ).join("");
}

function renderTableError(message) {
  $("student-table-body").innerHTML =
    `<tr><td colspan="7" class="py-10 text-center text-rose-500"><i class="fa-solid fa-circle-exclamation text-2xl mb-2 block"></i>${escapeHtml(message)}</td></tr>`;
}

function setButtonLoading(button, loading, label) {
  if (loading) {
    button.dataset.original = button.innerHTML;
    button.disabled = true;
    button.innerHTML = `<i class="fa-solid fa-circle-notch fa-spin"></i> ${escapeHtml(label)}`;
  } else {
    button.disabled = false;
    if (button.dataset.original) button.innerHTML = button.dataset.original;
  }
}

function setInlineError(id, message) {
  const element = $(id);
  element.textContent = message;
  element.classList.toggle("hidden", !message);
}

function renderFieldErrors(errors) {
  Object.entries(errors || {}).forEach(([field, message]) => {
    const input = $(field);
    const error = document.querySelector(`[data-error-for="${field}"]`);
    if (input) input.classList.add("border-rose-500", "ring-1", "ring-rose-200");
    if (error) {
      error.textContent = message;
      error.classList.remove("hidden");
    }
  });
}

function clearFieldError(field) {
  const input = $(field);
  const error = document.querySelector(`[data-error-for="${field}"]`);
  if (input) input.classList.remove("border-rose-500", "ring-1", "ring-rose-200");
  if (error) {
    error.textContent = "";
    error.classList.add("hidden");
  }
}

function clearFieldErrors() {
  document
    .querySelectorAll("[data-error-for]")
    .forEach((error) => clearFieldError(error.dataset.errorFor));
}

function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.className = `pointer-events-auto flex items-start gap-3 rounded-lg border bg-white px-4 py-3 shadow-lg ${type === "error" ? "border-rose-200 text-rose-800" : "border-emerald-200 text-emerald-800"}`;
  toast.innerHTML = `<i class="fa-solid ${type === "error" ? "fa-circle-exclamation" : "fa-circle-check"} mt-0.5"></i><span class="text-sm font-medium">${escapeHtml(message)}</span>`;
  $("toast-container").appendChild(toast);
  setTimeout(() => toast.remove(), 4500);
}

function formatCurrency(value) {
  return `₹${Number(value || 0).toFixed(2)}`;
}

function formatDateTime(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value ?? "") : date.toLocaleString();
}

function getCookie(name) {
  const prefix = `${name}=`;
  const row = document.cookie.split("; ").find((item) => item.startsWith(prefix));
  return row ? row.substring(prefix.length) : undefined;
}

function escapeHtml(value) {
  return String(value ?? "").replace(
    /[&<>"']/g,
    (character) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;",
      })[character],
  );
}
```

## src/main/resources/static/error/404.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 - Page not found</title>
    <style>
        body { margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
               font-family: system-ui, sans-serif; background: #f8fafc; color: #0f172a; }
        .card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 40px; text-align: center; max-width: 380px; }
        h1 { font-size: 56px; margin: 0; color: #0284c7; }
        p { color: #475569; }
        a { display: inline-block; margin-top: 12px; padding: 10px 18px; background: #0284c7; color: #fff; border-radius: 8px; text-decoration: none; }
    </style>
</head>
<body>
    <div class="card">
        <h1>404</h1>
        <h2>Page not found</h2>
        <p>The page you are looking for does not exist.</p>
        <a href="/">Back to dashboard</a>
    </div>
</body>
</html>
```

## src/main/resources/static/error/500.html

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>500 - Something went wrong</title>
    <style>
        body { margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
               font-family: system-ui, sans-serif; background: #f8fafc; color: #0f172a; }
        .card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 40px; text-align: center; max-width: 380px; }
        h1 { font-size: 56px; margin: 0; color: #0284c7; }
        p { color: #475569; }
        a { display: inline-block; margin-top: 12px; padding: 10px 18px; background: #0284c7; color: #fff; border-radius: 8px; text-decoration: none; }
    </style>
</head>
<body>
    <div class="card">
        <h1>500</h1>
        <h2>Something went wrong</h2>
        <p>An unexpected error occurred on the server. Please try again.</p>
        <a href="/">Back to dashboard</a>
    </div>
</body>
</html>
```

## src/main/resources/static/index.html

```html
<!DOCTYPE html>
<html lang="en" class="h-full">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hostel Operations Dashboard</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        brand: { 50: '#f0f7ff', 500: '#0284c7', 600: '#0284c7', 700: '#0369a1', 900: '#0c4a6e' }
                    }
                }
            }
        }
    </script>
</head>

<body class="bg-slate-50 font-sans text-slate-800 antialiased min-h-screen lg:h-screen lg:overflow-hidden flex flex-col">

    <!-- Header -->
    <header class="bg-slate-900 text-white shadow-md flex-shrink-0">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex flex-wrap gap-3 justify-between items-center">
            <div class="flex items-center space-x-3">
                <div class="p-2 bg-brand-600 rounded-lg"><i class="fa-solid fa-building-user text-xl text-white"></i></div>
                <h1 class="text-xl font-bold tracking-tight">Hostel Management System</h1>
            </div>
            <div class="flex flex-wrap items-center gap-2">
                <button id="btn-fee" type="button" title="Change standard monthly fee"
                    class="text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 px-3 py-2 rounded-md font-medium border border-slate-700 transition flex items-center gap-2">
                    <i class="fa-solid fa-indian-rupee-sign"></i> Monthly fee: <span id="fee-chip">...</span>
                </button>
                <button id="btn-rooms" type="button"
                    class="text-xs bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white px-3 py-2 rounded-md font-medium border border-slate-700 transition flex items-center gap-2">
                    <i class="fa-solid fa-door-open"></i> Rooms
                </button>
                <a href="/h2-console" target="_blank"
                    class="text-xs bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white px-3 py-2 rounded-md font-medium border border-slate-700 transition flex items-center gap-2">
                    <i class="fa-solid fa-database"></i> H2 Console
                </a>
                <button id="btn-logout" type="button"
                    class="text-xs bg-rose-600 hover:bg-rose-700 text-white px-3 py-2 rounded-md font-medium transition flex items-center gap-2">
                    <i class="fa-solid fa-right-from-bracket"></i> Logout
                </button>
            </div>
        </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-5 flex-1 grid grid-cols-1 lg:grid-cols-12 gap-5 lg:min-h-0 w-full">

        <!-- LEFT: form -->
        <div class="lg:col-span-4 bg-white rounded-xl border border-slate-200 shadow-sm p-5 flex flex-col lg:h-full lg:overflow-y-auto">
            <div class="flex items-center justify-between border-b border-slate-100 pb-3 mb-4 flex-shrink-0">
                <h2 id="form-title" class="text-lg font-bold text-slate-900">Add New Resident</h2>
                <span id="edit-badge" class="hidden px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">Edit Mode</span>
            </div>

            <form id="student-form" class="space-y-4 flex-1 flex flex-col justify-between" novalidate>
                <input type="hidden" id="student-id">

                <div class="space-y-4">
                    <div>
                        <label for="name" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">Full Name</label>
                        <input type="text" id="name" required maxlength="100"
                            class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none text-sm transition"
                            placeholder="e.g. John Doe">
                        <p data-error-for="name" class="hidden mt-1 text-xs text-rose-600"></p>
                    </div>

                    <div>
                        <label for="roomNumber" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">Room Number</label>
                        <input type="text" id="roomNumber" required maxlength="5"
                            class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none text-sm transition"
                            placeholder="e.g. B-204">
                        <p data-error-for="roomNumber" class="hidden mt-1 text-xs text-rose-600"></p>
                    </div>

                    <div>
                        <label for="admissionDate" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">Admission Date</label>
                        <input type="date" id="admissionDate" required
                            class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none text-sm transition">
                        <p data-error-for="admissionDate" class="hidden mt-1 text-xs text-rose-600"></p>
                    </div>

                    <!-- Only shown when registering. Later payments use the "Record payment" button in the table. -->
                    <div id="initial-payment-block" class="grid grid-cols-2 gap-3">
                        <div>
                            <label for="initialPayment" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">Initial Payment (₹)</label>
                            <input type="number" step="0.01" min="0" id="initialPayment"
                                class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none text-sm transition"
                                placeholder="0.00">
                            <p data-error-for="initialPayment" class="hidden mt-1 text-xs text-rose-600"></p>
                        </div>
                        <div>
                            <label for="paymentMode" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">Mode</label>
                            <select id="paymentMode"
                                class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 outline-none text-sm bg-white">
                                <option value="CASH">Cash</option>
                                <option value="UPI">UPI</option>
                                <option value="CARD">Card</option>
                                <option value="BANK_TRANSFER">Bank transfer</option>
                            </select>
                            <p data-error-for="paymentMode" class="hidden mt-1 text-xs text-rose-600"></p>
                        </div>
                    </div>
                </div>

                <div class="pt-4 flex items-center space-x-3 flex-shrink-0">
                    <button type="submit" id="btn-submit"
                        class="flex-1 bg-brand-600 hover:bg-brand-700 text-white font-medium py-2.5 px-4 rounded-lg shadow-sm transition text-sm flex items-center justify-center gap-2">
                        <i class="fa-solid fa-plus"></i> Register Resident
                    </button>
                    <button type="button" id="btn-cancel"
                        class="hidden bg-slate-100 hover:bg-slate-200 text-slate-600 font-medium py-2.5 px-4 rounded-lg transition text-sm">
                        Cancel
                    </button>
                </div>
            </form>
        </div>

        <!-- RIGHT: stats + table -->
        <div class="lg:col-span-8 flex flex-col lg:min-h-0 lg:h-full gap-5">

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 flex-shrink-0">
                <div class="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                    <div>
                        <p class="text-xs font-medium text-slate-500">Active Residents</p>
                        <p id="stat-total" class="text-2xl font-bold text-slate-900 mt-1">0</p>
                    </div>
                    <div class="p-2.5 bg-blue-50 text-brand-600 rounded-lg"><i class="fa-solid fa-users text-xl"></i></div>
                </div>
                <div class="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                    <div>
                        <p class="text-xs font-medium text-slate-500">Collected Fees</p>
                        <p id="stat-collected" class="text-2xl font-bold text-emerald-600 mt-1">₹0.00</p>
                    </div>
                    <div class="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg"><i class="fa-solid fa-circle-check text-xl"></i></div>
                </div>
                <div class="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
                    <div>
                        <p class="text-xs font-medium text-slate-500">Pending Dues</p>
                        <p id="stat-pending" class="text-2xl font-bold text-rose-600 mt-1">₹0.00</p>
                    </div>
                    <div class="p-2.5 bg-rose-50 text-rose-600 rounded-lg"><i class="fa-solid fa-triangle-exclamation text-xl"></i></div>
                </div>
            </div>

            <div class="bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col lg:flex-1 lg:min-h-0 overflow-hidden">
                <div class="p-4 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 flex-shrink-0">
                    <h2 class="text-base font-bold text-slate-900">Resident Directory</h2>
                    <div class="flex flex-wrap items-center gap-2">
                        <input id="student-search" type="search" placeholder="Search residents"
                            class="w-40 px-3 py-1.5 text-xs border border-slate-300 rounded-lg outline-none focus:ring-2 focus:ring-brand-500">
                        <button id="btn-refresh" type="button"
                            class="px-3 py-1.5 text-xs font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg transition flex items-center gap-1.5">
                            <i class="fa-solid fa-rotate text-slate-500"></i> Refresh
                        </button>
                        <button id="btn-pending" type="button" aria-pressed="false"
                            class="px-3 py-1.5 text-xs font-medium bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg transition flex items-center gap-1.5">
                            <i class="fa-solid fa-filter text-rose-500"></i> Pending Dues Only
                        </button>
                    </div>
                </div>

                <div class="overflow-auto lg:flex-1 lg:min-h-0 max-h-[70vh] lg:max-h-none">
                    <table class="w-full min-w-[44rem] text-left text-sm relative">
                        <thead class="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase text-xs tracking-wider sticky top-0 z-10">
                            <tr>
                                <th class="py-3 px-4 font-semibold">ID</th>
                                <th class="py-3 px-4 font-semibold">Name</th>
                                <th class="py-3 px-4 font-semibold">Room</th>
                                <th class="py-3 px-4 font-semibold">Admission Date</th>
                                <th class="py-3 px-4 font-semibold text-right">Paid</th>
                                <th class="py-3 px-4 font-semibold text-right">Pending</th>
                                <th class="py-3 px-4 font-semibold text-center">Status / Actions</th>
                            </tr>
                        </thead>
                        <tbody id="student-table-body" class="divide-y divide-slate-100 text-slate-700">
                            <tr><td colspan="7" class="text-center py-8 text-slate-400">Loading residents...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>

    <div id="toast-container" class="fixed top-4 right-4 z-50 flex w-[min(22rem,calc(100vw-2rem))] flex-col gap-2 pointer-events-none"></div>

    <!-- Checkout confirm -->
    <div id="action-modal" class="modal-overlay hidden fixed inset-0 z-40 bg-slate-900/40 p-4 flex items-center justify-center">
        <div class="w-full max-w-sm rounded-xl bg-white p-5 shadow-xl">
            <h2 class="text-lg font-bold text-slate-900">Check out resident?</h2>
            <p class="mt-2 text-sm text-slate-600">This frees one bed in the room. The profile and payment history will be preserved.</p>
            <div class="mt-5 flex justify-end gap-2">
                <button type="button" data-close-modal class="px-3 py-2 text-sm font-medium text-slate-600 bg-slate-100 rounded-lg">Cancel</button>
                <button type="button" id="btn-confirm-action" class="px-3 py-2 text-sm font-medium text-white bg-rose-600 rounded-lg">
                    <i class="fa-solid fa-right-from-bracket"></i> Check out resident</button>
            </div>
        </div>
    </div>

    <!-- Record payment -->
    <div id="payment-modal" class="modal-overlay hidden fixed inset-0 z-40 bg-slate-900/40 p-4 flex items-center justify-center">
        <div class="w-full max-w-md rounded-xl bg-white p-5 shadow-xl max-h-[90vh] overflow-y-auto">
            <div class="flex items-start justify-between">
                <div>
                    <h2 class="text-lg font-bold text-slate-900">Record Payment</h2>
                    <p class="text-sm text-slate-600"><span id="pay-student-name" class="font-medium"></span> &middot; Pending:
                        <span id="pay-pending" class="font-semibold text-rose-600"></span></p>
                </div>
                <button type="button" data-close-modal class="text-slate-400 hover:text-slate-700" aria-label="Close"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <form id="payment-form" class="mt-4 space-y-3" novalidate>
                <div class="grid grid-cols-2 gap-3">
                    <div>
                        <label for="pay-amount" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">Amount (₹)</label>
                        <input type="number" step="0.01" min="0.01" id="pay-amount"
                            class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 outline-none text-sm" placeholder="0.00">
                    </div>
                    <div>
                        <label for="pay-mode" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">Mode</label>
                        <select id="pay-mode" class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 outline-none text-sm bg-white">
                            <option value="CASH">Cash</option>
                            <option value="UPI">UPI</option>
                            <option value="CARD">Card</option>
                            <option value="BANK_TRANSFER">Bank transfer</option>
                        </select>
                    </div>
                </div>
                <p id="pay-error" class="hidden text-xs text-rose-600"></p>
                <button type="submit" id="btn-pay-submit"
                    class="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-medium py-2.5 rounded-lg text-sm flex items-center justify-center gap-2">
                    <i class="fa-solid fa-check"></i> Save payment
                </button>
            </form>
            <h3 class="mt-5 text-xs font-semibold uppercase tracking-wider text-slate-500">Payment history</h3>
            <ul id="pay-history" class="mt-2 divide-y divide-slate-100 text-sm"></ul>
        </div>
    </div>

    <!-- Rooms -->
    <div id="rooms-modal" class="modal-overlay hidden fixed inset-0 z-40 bg-slate-900/40 p-4 flex items-center justify-center">
        <div class="w-full max-w-2xl rounded-xl bg-white p-5 shadow-xl max-h-[90vh] overflow-y-auto">
            <div class="flex items-center justify-between">
                <h2 class="text-lg font-bold text-slate-900">Room Details</h2>
                <button type="button" data-close-modal class="text-slate-400 hover:text-slate-700" aria-label="Close"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <div id="rooms-body" class="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3"></div>
        </div>
    </div>

    <!-- Monthly fee -->
    <div id="fee-modal" class="modal-overlay hidden fixed inset-0 z-40 bg-slate-900/40 p-4 flex items-center justify-center">
        <div class="w-full max-w-sm rounded-xl bg-white p-5 shadow-xl">
            <div class="flex items-center justify-between">
                <h2 class="text-lg font-bold text-slate-900">Standard Monthly Fee</h2>
                <button type="button" data-close-modal class="text-slate-400 hover:text-slate-700" aria-label="Close"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <p class="mt-1 text-sm text-slate-600">Pending = this fee minus all recorded payments. Changing it updates every resident immediately.</p>
            <form id="fee-form" class="mt-4 space-y-3" novalidate>
                <input type="number" step="0.01" min="0" id="fee-input"
                    class="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 outline-none text-sm" placeholder="0.00">
                <p id="fee-error" class="hidden text-xs text-rose-600"></p>
                <button type="submit" id="btn-fee-submit"
                    class="w-full bg-brand-600 hover:bg-brand-700 text-white font-medium py-2.5 rounded-lg text-sm flex items-center justify-center gap-2">
                    <i class="fa-solid fa-floppy-disk"></i> Save fee
                </button>
            </form>
        </div>
    </div>

    <button type="button" id="btn-mobile-register"
        class="fixed bottom-4 right-4 z-30 rounded-full bg-brand-600 px-4 py-3 text-sm font-semibold text-white shadow-lg md:hidden">
        <i class="fa-solid fa-plus"></i> Register resident
    </button>

    <script src="app.js"></script>
</body>

</html>
```
