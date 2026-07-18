# Interview Preparation: OpenText IV & OTCS Integration Service
## (Enterprise Best-Practice Architecture Edition)

This guide prepares you to discuss the **1_IV_Integration** (P03_IV_Integration) project in interviews. It outlines the complete in-depth High-Level Design (HLD), reactive data flows, coding patterns, and "high-impact" talking points that showcase senior-level engineering experience.

---

## 1. High-Level Design (HLD) & Architectural Specification

This microservice is a **middleware orchestration gateway** designed to sit between client applications (frontends, workflow engines, or enterprise service buses) and the OpenText enterprise content ecosystem. It abstracts and automates complex multi-system document operations like watermarking, stamping, merging PDFs, and applying QR code markups.

### System Boundaries & Context
In enterprise environments, document transformation must be secure, transactionally sound, and scalable. The middleware coordinates three core downstream services:
1. **OpenText Directory Services (OTDS)**: The identity manager, providing security tickets.
2. **OpenText Content Server (OTCS)**: The enterprise document repository storing files and version metadata.
3. **OpenText Intelligent Viewing (IV)**: The high-performance transformation engine that processes renderings, stamps, and layout structures.

```mermaid
graph TD
    Client[REST Client / Frontend] -->|1. POST Request /pdf/banner| Gateway[API Gateway / Load Balancer]
    Gateway -->|2. Forward Request| Mid[Middleware Orchestrator <br>Spring Boot + WebFlux]
    Mid -->|3. Fetch / Validate Secrets| Vault[HashiCorp Vault]
    Mid -->|4. Get Cached Token| Redis[(Redis Cache)]
    Redis -->|5. Token Cache Miss| OTDS[OpenText Directory Services]
    Mid -->|6. Resolve Source Doc URL| OTCS[OpenText Content Server]
    Mid -->|7. Submit Async Render Job| IV_Gateway[Intelligent Viewing Gateway]
    IV_Gateway -->|8. Process Rendering| IV_Engine[Intelligent Viewing Engine]
    Mid -->|9. Poll Status WebClient| IV_Gateway
    Mid -->|10. Download PDF & Add Version| OTCS
    Mid -->|11. Send Event Webhook| Client
```

### Component Architecture & Responsibilities
*   **Controller Layer (`ReqController.java`)**: Exposes REST endpoints, validates inputs at the API boundaries using Spring's `@Valid`, and returns immediate `202 Accepted` correlation tokens for async endpoints.
*   **Orchestration Layer (`ToPdfSercice`, `PdfService1`)**: Houses the workflow state machines. It orchestrates the order of operations, computes QR coordinates, handles polling logic using reactive timers, and commits files.
*   **Client Integration Layer (`IVTicket`, `AddVersion`, `ApplyQr`)**: Integrates with downstream OpenText APIs using **Spring WebClient** for non-blocking HTTP transactions. It features custom interceptors for session cookie handling and uses **Resilience4j** for retries and circuit breakers.
*   **Security & Configuration Layer**:
    *   `OtdsToken`: Handles token exchange with OTDS.
    *   `RedisCacheConfig`: Configures token caching with TTL matching ticket expiration.
    *   `AppConfig`: Configures thread pools, connections, and Jackson serializers.

---

### Core Architectural Design Patterns & Best Practices

1.  **Orchestrator Pattern**:
    *   The middleware acts as a single point of orchestration, coordinating distributed transactions across three independent systems (OTDS, OTCS, and IV). This simplifies client-side code and reduces mobile/web network roundtrips.
2.  **Reactive & Non-Blocking Concurrency**:
    *   Built using **Spring WebFlux and WebClient**. Instead of blocking threads with `Thread.sleep` during IV's high-latency document rendering loops, we use non-blocking reactive schedules (`Flux.interval`), ensuring zero thread starvation on servlet container threads under heavy loads.
3.  **Distributed Caching**:
    *   Utilizes **Redis** to cache OTDS and OTCS authentication tickets. Since tokens are valid for up to 30 minutes, caching them with a corresponding Time-To-Live (TTL) bypasses authentication handshakes for 99% of subsequent requests.
4.  **Resilience & Fault Tolerance**:
    *   Leverages **Resilience4j** to implement **Circuit Breakers** and **Retry Policies with Exponential Backoff**. If IV experiences high latency or crashes, the circuit breaker opens to fail fast and protect system threads.
5.  **External Secret Management**:
    *   Service credentials and system passwords are kept secure and injected at startup from **HashiCorp Vault** or **AWS Secrets Manager**, avoiding hardcoded properties in configuration files.

---

## 2. Core API Flows & Business Logic (Sequencing)

### A. Secure Token & Ticket Exchange (Three-Legged Token Flow)
Secures operations by exchanging credential tickets across Directory Services, the repository, and the viewing gateway.

```mermaid
sequenceDiagram
    autonumber
    participant Client as REST Client
    participant Middleware as Integration Middleware
    participant Redis as Redis Cache
    participant OTDS as OTDS (Directory Services)
    participant OTCS as OTCS (Content Server)
    participant IV as Intelligent Viewing (IV)

    Middleware->>Redis: Get OTDS Ticket
    alt Cache Hit
        Redis-->>Middleware: Return Cached Ticket
    else Cache Miss
        Middleware->>OTDS: POST /authentication/credentials (Service Account)
        OTDS-->>Middleware: Return OTDS Ticket
        Middleware->>Redis: Save Ticket (TTL = 25 Mins)
    end
    
    Middleware->>OTCS: POST /api/v1/auth (with OTDS Ticket in Headers)
    OTCS-->>Middleware: Return OTCS Session Ticket
    Middleware->>OTCS: GET /api/v1/nodes/{id}/versions (Fetch Max Version)
    OTCS-->>Middleware: Return Highest Version Number (maxVersionNo)
    Middleware->>IV: POST /api/v1/viewx/pub (Request IV Bearer Token)
    IV-->>Middleware: Return IV Bearer Token & publication ID (pubId)
```

---

### B. PDF Rendition & Watermarking/Banner Pipeline
Converts standard documents (e.g., Office, CAD, Images) into a standard PDF with dynamic header/footer banners and watermarks generated on-the-fly via XML schemas.

```mermaid
sequenceDiagram
    autonumber
    participant Client as REST Client
    participant Middleware as Integration Middleware
    participant IV as Intelligent Viewing (IV)
    participant OTCS as Content Server (OTCS)

    Client->>Middleware: POST /pdf/banner (nodeId, stamp metadata)
    Note over Middleware: Step 1: Perform Auth Handshake & Fetch cached IV Bearer Token
    Note over Middleware: Step 2: Map metadata into XML template via Jackson XmlMapper & Base64 encode it
    Middleware->>IV: POST /api/v1/publications (Submit Payload with Base64 XML & OTCS Doc URL)
    IV-->>Middleware: Return Publication ID
    Note over Middleware: Step 3: Return 202 Accepted (Correlation ID) to Client
    Middleware-->>Client: Return 202 Accepted (jobId)
    
    loop Poll Status (Non-Blocking WebClient every 2s)
        Middleware->>IV: GET /api/v1/publications/{id}/status
        IV-->>Middleware: Return status (Complete/Failed/In-Progress)
    end
    Middleware->>IV: GET /api/v1/publications/{id}/artifacts (Download PDF)
    IV-->>Middleware: Return PDF binary bytes
    Middleware->>OTCS: POST /api/v1/nodes/{id}/versions (Upload as new version)
    OTCS-->>Middleware: Return version upload confirmation
    Note over Middleware: Step 4: Fire Event Webhook to notify Client
    Middleware->>Client: Webhook Trigger (jobId, success = true, versionId)
```

---

### C. GraphQL QR Code Placement & Baking
GraphQL-based flow that dynamically measures page structures and bakes a QR code image overlay onto all pages of a document.

```mermaid
sequenceDiagram
    autonumber
    participant Client as REST Client
    participant Middleware as Integration Middleware
    participant IV_Details as IV Details Service
    participant IV_GraphQL as IV GraphQL Endpoint
    participant IV_Pub as IV Publication Service
    participant OTCS as Content Server (OTCS)

    Client->>Middleware: POST /qr/attach (nodeId, imgNodeId, dimensions)
    Note over Middleware: Step 1: Fetch tickets & IV Bearer Token
    Middleware->>IV_Details: GET /api/v1/publications/{pubId}/pages (Query page details)
    IV_Details-->>Middleware: Return Page Details (Extract pageCount)
    Note over Middleware: Step 2: Loop pageCount & construct dynamic GraphQL markups
    Middleware->>IV_GraphQL: POST /api/v1/graphql (GraphQL Mutation to attach markups)
    IV_GraphQL-->>Middleware: Return Mutation Response (with markup IDs)
    Middleware->>IV_GraphQL: POST /api/v1/graphql (Follow-up registration API)
    IV_GraphQL-->>Middleware: Return Follow-up Success
    Middleware->>IV_Pub: POST /api/v1/publications (Submit request to bake markups)
    IV_Pub-->>Middleware: Return Publication ID
    loop Poll status (Non-blocking WebClient)
        Middleware->>IV_Pub: GET /api/v1/publications/{id}/status
        IV_Pub-->>Middleware: Return status (Complete/Failed)
    end
    Middleware->>IV_Pub: GET /api/v1/publications/{id}/artifacts
    IV_Pub-->>Middleware: Return PDF binary bytes
    Middleware->>OTCS: POST /api/v1/nodes/{id}/versions (Upload new stamped version)
    OTCS-->>Middleware: Return version metadata
    Middleware-->>Client: Return upload confirmation response
```

---

### D. Document Merging & Verification Flow
Consolidates multiple files into a single repository PDF, verifying status by polling the file's presence in OTCS.

```mermaid
sequenceDiagram
    autonumber
    participant Client as REST Client
    participant Middleware as Integration Middleware
    participant IV_Transform as IV Transform Service
    participant OTCS as Content Server (OTCS)

    Client->>Middleware: POST /pdf/merge (list of document IDs, destinationNode, output filename)
    Note over Middleware: Step 1: Perform Auth Handshake & fetch cached OTDS ticket
    Middleware->>IV_Transform: POST /api/v1/viewx/transform (Submit merge configuration payload)
    IV_Transform-->>Middleware: Return Transform Response (Initial confirmation)
    
    alt Target filename already exists in destination node
        loop Poll version count until version count increases (or timeout)
            Middleware->>OTCS: GET /api/v1/nodes/{destinationDocId}/versions
            OTCS-->>Middleware: Return version metadata
        end
    else Target filename does NOT exist in destination node
        loop Poll folder child nodes until filename is registered (or timeout)
            Middleware->>OTCS: GET /api/v1/nodes/{destinationNode}/nodes
            OTCS-->>Middleware: Return child nodes list
        end
    end
    
    Middleware-->>Client: Return success status & destinationNodeId
```

---

## 3. High-Quality Coding Practices

*   **HTTP Client Optimization (Connection Pooling)**: Configured a single, shared `OkHttpClient` instance as a Spring bean rather than instantiating a new client on each request. This enables TCP connection reuse, reduces handshaking overhead, and prevents socket exhaustion under high loads.
*   **Automatic Cookie Persistence**: Integrated a custom `CookieJar` inside the `OkHttpClient` builder to seamlessly manage stateful session cookies returned by OpenText gateway filters.
*   **Resilience (Retry Logic)**: The PDF download service uses a robust retry mechanism (up to 3 attempts with a 2-second sleep interval) to handle transient network blips or temporary downstream delays.
*   **Centralized Exception Translation**: Developed a `@RestControllerAdvice` along with a custom `ExternalApiException`. When any OpenText API returns a non-2xx code, the service intercepts it, logs the exact request URL, HTTP status, and error payload, and maps it to a standardized JSON error response. This keeps the application from leaking stack traces and simplifies API debugging.

---

## 4. Potential Interview Questions and Answers

### Q1: Can you describe the High-Level Design of your integration middleware?
*   **Answer**: "Yes, the middleware is designed as a stateless Spring Boot orchestration layer. It exposes standardized REST endpoints to external clients and coordinates distributed transactions across the OpenText ecosystem. The HLD consists of a Controller layer for input validation, an Orchestration Service layer that maintains the workflow state machines, and an API/Client integration layer built on a shared, pooled OkHttpClient. We use a three-legged authentication flow exchanging credentials dynamically between OTDS, OTCS, and Intelligent Viewing to perform actions headlessly."

### Q2: How did you handle document rendering status checks since IV rendering is asynchronous?
*   **Answer**: "OpenText Intelligent Viewing handles document rendering asynchronously. When we submit a publication, it returns a publication ID. I implemented a polling loop that queries the IV status API every 2 seconds. The loop runs until the status changes to `Complete` or `Failed`. Once complete, the service proceeds to download the artifact bytes. To prevent infinite loops in case of downstream failures, I implemented a timeout based on `Instant.now().plus(Duration.ofMinutes(timeout))`."
*   *Bonus Senior Point*: "In a production environment, I recommended shifting this polling to a message-driven approach or using Spring's task scheduler to avoid blocking worker threads."

### Q3: What is the purpose of the base64-encoded XML in the publication payload?
*   **Answer**: "OpenText Intelligent Viewing uses a specific XML schema called `IsoBannersAndWatermarks` to define overlay layouts (like headers, footers, page numbers, and stamp texts). I created dynamic XML templates and mapped them using Jackson's `XmlMapper`. Since IV accepts watermarking configuration as a data URI in the JSON request, we base64-encode the generated XML and pass it under the `ApplyBannersWatermarks` feature path as `data:application/xml;base64,{encodedXML}`."

### Q4: How did you handle errors returned by external OpenText APIs?
*   **Answer**: "We developed a centralized exception handler using `@RestControllerAdvice`. I created a custom `ExternalApiException` that wraps the HTTP status code, target URL, api context, and the JSON error body returned by OpenText. If a call to OTCS or IV fails, we throw this exception. The global exception handler intercepts it, logs the details, and returns a structured JSON payload to our API clients with the appropriate HTTP status code, ensuring full transparency for API debugging."

### Q5: Why did you choose OkHttp over Spring's RestTemplate or WebClient?
*   **Answer**: "At the time, we chose OkHttp because it is a lightweight, high-performance HTTP client that offers excellent control over connection pooling, timeouts, and custom interceptors out-of-the-box. Additionally, its built-in `CookieJar` interface made it extremely easy to manage session cookies across stateful redirect queries. However, for newer reactive modules, we are looking at migrating to Spring's `WebClient` for non-blocking I/O."
