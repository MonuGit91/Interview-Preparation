# Operational Flow & Exception Handling: JWT Filter & Invalidation

This document details the complete end-to-end execution flow and exception propagation system for JWT request authentication and token invalidation (blacklist) check.

---

## 1. Process Flow Diagram (Boxes & Arrows)

This flowchart traces the step-by-step process of the `JwtAuthFilter` interceptor, highlighting Redis blacklist verification, provider resolution, and context configuration.

```mermaid
graph TD
    %% Define Styles
    classDef startEnd fill:#E6F2FF,stroke:#0066CC,stroke-width:2px,rx:10px,ry:10px;
    classDef process fill:#FFF2CC,stroke:#D6B656,stroke-width:1.5px;
    classDef decision fill:#F8CECC,stroke:#B85450,stroke-width:1.5px;
    classDef exception fill:#FADBD8,stroke:#C0392B,stroke-width:1.5px,stroke-dasharray: 5 5;
    classDef success fill:#D5E8D4,stroke:#82B366,stroke-width:2px;

    %% Elements
    Start((Client issues REST Call)):::startEnd
    
    subgraph Step 1: Token Extraction & Signature Check
        Filter[JwtAuthFilter Intercepts Request]:::process
        GetHeader[Extract 'Authorization' header]:::process
        D1{Starts with 'Bearer '?}:::decision
        Validate[Verify JWT Signature using Secret Key]:::process
        D2{Signature Valid?}:::decision
        Reject1[Return 401 Unauthorized]:::exception
    end

    subgraph Step 2: Redis Blacklist Check
        CheckRedis[Query Redis Cache for JWT string]:::process
        D3{Is Token Blacklisted?}:::decision
        Reject2[Return 401: Token Blacklisted / Logged Out]:::exception
    end

    subgraph Step 3: Provider & UserDetails Resolution
        ReadClaims[Extract Username & Provider claims from payload]:::process
        D4{Provider == 'GOOGLE'?}:::decision
        LoadGoogle[Fetch UserDetails via OAuthUserService]:::process
        LoadLocal[Fetch UserDetails via UserDetailsService]:::process
    end

    subgraph Step 4: Security Context Configuration
        CreateAuth[Build UsernamePasswordAuthenticationToken]:::process
        SetContext[Inject Authentication into SecurityContextHolder]:::process
        PassChain[Forward request along FilterChain]:::process
        EndResponse((REST Controller processes Request)):::success
    end

    %% Flow Paths
    Start --> Filter
    Filter --> GetHeader
    GetHeader --> D1
    
    D1 -- No --> PassChain
    D1 -- Yes --> Validate
    
    Validate --> D2
    D2 -- No --> Reject1
    D2 -- Yes --> CheckRedis
    
    CheckRedis --> D3
    D3 -- Yes --> Reject2
    D3 -- No --> ReadClaims
    
    ReadClaims --> D4
    D4 -- Yes --> LoadGoogle --> CreateAuth
    D4 -- No --> LoadLocal --> CreateAuth
    
    CreateAuth --> SetContext --> PassChain
    PassChain --> EndResponse

    %% Exception Handling
    subgraph Exception Gateway
        Err[Catch Filter Exception]:::exception
        LogErr[Log details silently to SLF4J]:::exception
    end
    Validate & LoadGoogle & LoadLocal -.->|Throws Exception| Err
    Err --> LogErr --> PassChain
```

---

## 2. Happy Path Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Client as REST Client / Web App
    participant Filter as JwtAuthFilter
    participant JWT as JwtService
    participant Redis as Redis Cache (Blacklist)
    participant LocalProvider as UserDetailsService (Local)
    participant Context as SecurityContextHolder
    participant API as UserRESTController

    Client->>Filter: GET /api/user/profile (Headers: Authorization: Bearer JWT)
    Filter->>JWT: getJwtFromHeader(request)
    JWT-->>Filter: Return JWT String
    
    rect rgb(240, 248, 255)
        Note over Filter, JWT: Step 1: Token Validity Check
        Filter->>JWT: validateJwtToken(jwt)
        JWT-->>Filter: Return true (Valid Signature & Not Expired)
    end

    rect rgb(255, 240, 245)
        Note over Filter, Redis: Step 2: Blacklist Check
        Filter->>Redis: isBlacklisted(jwt)
        Redis-->>Filter: Return false (Token is clean)
    end

    rect rgb(245, 255, 250)
        Note over Filter, LocalProvider: Step 3: Extract & Load User
        Filter->>JWT: getUsernameFromJwtToken(jwt)
        JWT-->>Filter: Return username ("user1")
        Filter->>JWT: getProviderFromJwtToken(jwt)
        JWT-->>Filter: Return provider ("LOCAL")
        Filter->>LocalProvider: loadUserByUsername("user1")
        LocalProvider-->>Filter: Return UserDetails object (with roles)
    end

    rect rgb(255, 250, 240)
        Note over Filter, Context: Step 4: Inject Security Context
        Filter->>Context: setAuthentication(UsernamePasswordAuthenticationToken)
        Filter->>API: doFilter(request, response) (Forward request)
        API-->>Client: 200 OK (With Profile Data)
    end
```

---

## 3. Step-by-Step Execution Mechanics

1. **Request Interception (`OncePerRequestFilter`)**:
   - Every incoming HTTP call hits the `JwtAuthFilter#doFilterInternal` interceptor before reaching Spring's route mappings.
   - It checks the request headers for an `Authorization` key starting with `Bearer `.

2. **Validation & Blacklist Check**:
   - If a token is present, the key signature is validated using the application's HMAC SHA-512 signing secret key.
   - Checks if the token is registered in the Redis cache. If yes, it indicates the user has logged out. The filter short-circuits the request chain immediately, returning a `401 Unauthorized` status.

3. **Provider Resolution**:
   - Parses the token claims to retrieve the `username` and `provider` (e.g. `GOOGLE` vs `LOCAL`).
   - If the provider is `GOOGLE`, it loads the user profile via `oauth2UserService.loadUserByEmail`.
   - If local, it loads the profile via the default `userDetailsService.loadUserByUsername`.

4. **Security Context Updates**:
   - Builds a `UsernamePasswordAuthenticationToken` using the UserDetails entity and their list of roles/authorities.
   - Sets request details (IP, session info) and stores the authentication token in the `SecurityContextHolder`.
   - Calls `filterChain.doFilter(request, response)` to pass the request to the rest of the application.

---

## 4. Exception Handling & Silent Propagation

### Silent Filter Propagation
- Any exception occurring inside the validation filter (such as parser issues, malformed tokens, or expired JWT errors) is caught within a `try-catch` block inside the filter.
- Instead of crashing the request or breaking the filter chain, the filter logs the exception details silently (`log.error("Cannot set user authentication: ...")`).
- The security context remains empty. The request continues down the filter chain to Spring Security's authorization filters (like `FilterSecurityInterceptor`), which block the request and return a structured `401 Unauthorized` response via `AuthEntryPoint`.
