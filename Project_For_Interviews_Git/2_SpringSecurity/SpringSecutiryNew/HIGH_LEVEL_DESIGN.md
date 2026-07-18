# High Level Design: Spring Security System

## 1. System Overview
This system is a **Secure Authentication & User Management Platform** built using **Spring Boot**. It serves as a secure backend foundation for modern web and mobile applications, providing robust identity management, authentication, and authorization capabilities.

## 2. Architecture Style
The system follows a **Monolithic Architecture** with **Modular Vertical Slices** (Feature-Based).
- **Backend**: Spring Boot (Java)
- **Database**: PostgreSQL (Relational Data)
- **Security**: Spring Security + JWT (Stateless)
- **API Style**: RESTful JSON API

## 3. System Context Diagram (C1)

```mermaid
graph LR
    User[User / Client App] -- HTTP/JSON --> API[Spring Security API]
    API -- Reads/Writes --> DB[(PostgreSQL Database)]
    API -- Delegates --> OAuth[OAuth2 Providers\n(Google, GitHub)]
    
    subgraph "Internal System"
    API
    DB
    end
```

## 4. Container Design (C2)

The backend is composed of high-level functional containers:

1.  **Web Layer (Controllers)**
    *   Entry point for all HTTP requests.
    *   Handles JSON serialization/deserialization.
    *   Delegates to Services via **Interfaces**.

2.  **Security Layer (Filters & Config)**
    *   Intercepts requests *before* controllers using `JwtAuthFilter`.
    *   Enforces Role-Based Access Control (RBAC).
    *   Manages Session Policy (Stateless).

3.  **Service Layer (Business Logic)**
    *   **Interface-Driven**: All logic accessed via interfaces (`UserService`, `JwtService`).
    *   Contains core business rules (e.g., "User cannot register if username exists").
    *   Transactional boundaries.

4.  **Persistence Layer (Data Access)**
    *   Spring Data JPA Repositories.
    *   Direct mapping to Database Tables (`users`, `authorities`).

## 5. Security Architecture

### 5.1 Authentication Flow (Stateless JWT)
1.  **Login**: User sends `POST /auth/login/token` with credentials.
    *   System verifies password (BCrypt).
    *   System generates a **JWT** (Signed with HMAC SHA-256).
    *   Returns JWT to client.
2.  **Protected Request**: Client sends `GET /api/user/hello` with header `Authorization: Bearer <token>`.
    *   `JwtAuthFilter` intercepts request.
    *   Validates signature and expiration.
    *   Extracts user identity & roles.
    *   Sets `SecurityContext`.
    *   Controller executes if Role matches.

### 5.2 Roles & Permissions
*   **ADMIN**: Full system access (`/api/admin/**`).
*   **USER**: Restricted access (`/api/user/**`).
*   **PUBLIC**: Login, Register (`/auth/login/token`, `/auth/register`).

## 6. Key Design Decisions

1.  **Interface-Based Services**:
    *   We use `UserService` (Interface) -> `UserServiceImpl` (Class).
    *   **Reason**: Decoupling and Testability. Allows easy mocking of services.

2.  **Feature-Based Packaging**:
    *   Code organized by Feature (`com.app.security.jwt`) instead of Layer (`com.app.service`).
    *   **Reason**: High cohesion, easy navigation, and easier migration to microservices later.

3.  **Stateless Session**:
    *   No Server-Side Sessions (`HttpSession`).
    *   **Reason**: Scalability. The API can scale horizontally without sticky sessions.

## 7. Future Roadmap
*   **Refresh Tokens**: Persistent tokens in database for long-lived sessions.
*   **Multi-Factor Authentication (MFA)**: Adding OTP via Email/SMS.
