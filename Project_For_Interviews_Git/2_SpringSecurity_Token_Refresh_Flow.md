# Operational Flow & Exception Handling: Refresh Token & Cookie Integration

This document details the complete end-to-end execution flow and exception propagation system for the token refresh pipelines (both JSON payload-based and secure HttpOnly cookie-based).

---

## 1. Process Flow Diagram (Boxes & Arrows)

This flowchart traces the step-by-step process of the token refresh endpoints, highlighting database checking, expiration validation, and security exception gates.

```mermaid
graph TD
    %% Define Styles
    classDef startEnd fill:#E6F2FF,stroke:#0066CC,stroke-width:2px,rx:10px,ry:10px;
    classDef process fill:#FFF2CC,stroke:#D6B656,stroke-width:1.5px;
    classDef decision fill:#F8CECC,stroke:#B85450,stroke-width:1.5px;
    classDef exception fill:#FADBD8,stroke:#C0392B,stroke-width:1.5px,stroke-dasharray: 5 5;
    classDef success fill:#D5E8D4,stroke:#82B366,stroke-width:2px;

    %% Elements
    Start1((Client POSTs JSON /auth/login/refreshtoken)):::startEnd
    Start2((Client POSTs Cookie /auth/refresh-token-cookie)):::startEnd
    
    subgraph "Step 1: Extraction & Database Check"
        GetPayload[Read 'refreshToken' from JSON Request Body]:::process
        GetCookie[Scan HttpOnly Cookies for 'refresh_token']:::process
        D1{Token Found in Request?}:::decision
        ReturnErr1[Return 400 Bad Request: Token is Empty]:::exception
        QueryDB[Query PostgreSQL for RefreshToken entity]:::process
        D2{Token exists in Database?}:::decision
        ReturnErr2[Throw TokenRefreshException: 400 Bad Request]:::exception
    end

    subgraph "Step 2: Verification of Expiration"
        CheckExpiry[Compare current timestamp with expiryDate claim]:::process
        D3{Token Expired?}:::decision
        DeleteToken[Delete RefreshToken from DB]:::process
        ReturnErr3[Throw TokenRefreshException: Token Expired]:::exception
    end

    subgraph "Step 3: Provider & UserDetails Load"
        ReadUser[Extract username & provider fields from RefreshToken]:::process
        D4{Provider == 'GOOGLE'?}:::decision
        LoadGoogle[Fetch UserDetails via OAuthUserService]:::process
        LoadLocal[Fetch UserDetails via UserDetailsService]:::process
    end

    subgraph "Step 4: Access Token Generation"
        GenJWT[Generate brand new JWT access token]:::process
        EndResponse((Return 200 OK with new JWT Access Token)):::success
    end

    %% Flow Paths
    Start1 --> GetPayload
    Start2 --> GetCookie
    
    GetPayload & GetCookie --> D1
    D1 -- No --> ReturnErr1
    D1 -- Yes --> QueryDB
    
    QueryDB --> D2
    D2 -- No --> ReturnErr2
    D2 -- Yes --> CheckExpiry
    
    CheckExpiry --> D3
    D3 -- Yes --> DeleteToken --> ReturnErr3
    D3 -- No --> ReadUser
    
    ReadUser --> D4
    D4 -- Yes --> LoadGoogle --> GenJWT
    D4 -- No --> LoadLocal --> GenJWT
    
    GenJWT --> EndResponse

    %% Exception Gateway
    subgraph "Exception Gateway"
        Err[Catch TokenRefreshException]:::exception
        LogErr[Log refresh failure message]:::exception
        ErrResponse((Return 400 Bad Request JSON: <br/> Error + Message)):::startEnd
    end
    
    ReturnErr2 & ReturnErr3 -.-> Err
    Err --> LogErr --> ErrResponse
```

---

## 2. Happy Path Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Client as REST Client / Web App
    participant Ctrl as AuthController
    participant Service as RefreshTokenServiceImpl (JPA)
    participant DB as PostgreSQL Database
    participant Providers as User Details Providers
    participant JWT as JwtService

    Client->>Ctrl: POST /auth/login/refreshtoken (TokenRefreshRequest body)
    Ctrl->>Service: findByToken(requestRefreshToken)
    Service->>DB: SELECT * FROM refresh_tokens WHERE token = :token
    DB-->>Service: Return RefreshToken Entity
    Service-->>Ctrl: Return Optional.of(RefreshToken)
    
    rect rgb(240, 248, 255)
        Note over Ctrl, Service: Step 1: Verification of Expiration
        Ctrl->>Service: verifyExpiration(refreshToken)
        Note over Service: Checks if expiryDate > Instant.now()
        Service-->>Ctrl: Return verified RefreshToken
    end

    rect rgb(255, 240, 245)
        Note over Ctrl, Providers: Step 2: Load UserDetails
        Ctrl->>Providers: loadUserByUsername("user1")
        Providers-->>Ctrl: Return UserDetails object
    end

    rect rgb(245, 255, 250)
        Note over Ctrl, JWT: Step 3: Access Token Issuance
        Ctrl->>JWT: generateTokenFromUsername(userDetails, provider = "LOCAL")
        JWT-->>Ctrl: Return new JWT Access Token
        Ctrl-->>Client: 200 OK (with new JWT)
    end
```

---

## 3. Step-by-Step Execution Mechanics

1. **Extraction & DB Fetching**:
   - The user triggers refresh using either payload extraction (`/auth/login/refreshtoken`) or browser cookies extraction (`/auth/refresh-token-cookie`).
   - The system queries PostgreSQL to retrieve the `RefreshToken` matching the token UUID.
   - If the token does not exist in the database, a `TokenRefreshException` is thrown.

2. **Expiration Validation**:
   - Compares the `expiryDate` attribute against the current system timestamp.
   - If the current time is greater than the expiry time, the token is expired. The system immediately deletes the token record from PostgreSQL (`refreshTokenRepository.delete(...)`) to prevent reuse, and throws a `TokenRefreshException`.

3. **User Loading**:
   - Reads the token's associated `username` and login `provider` attributes.
   - Loads the user profile from the database (`userDetailsService`) or the OAuth mapping provider (`oauth2UserService`).

4. **Token Generation**:
   - Passes the UserDetails context to `jwtService.generateTokenFromUsername` to sign a new access token.
   - Returns the new JWT access token to the client.

---

## 4. Exception Handling & Controller Advice Mapping

### Global Exception Boundary Mapping
- Custom exceptions (specifically `TokenRefreshException`) are handled by Spring's `@RestControllerAdvice` in `GlobalExceptionHandler.java`.
- When a `TokenRefreshException` is thrown, the advice intercepts the request, blocks downstream propagation, logs the failure, and returns a structured JSON payload:
  ```json
  {
    "message": "Failed to refresh token: [Details of error]",
    "status": false
  }
  ```
  This is returned to the client with an HTTP status of `400 Bad Request`, preventing raw stack traces from exposing database details.
