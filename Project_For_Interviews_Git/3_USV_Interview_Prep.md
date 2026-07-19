# Interview Preparation: OpenText OTCS & Google Drive Integration (USV)
## (Enterprise Architecture, Industry-Standard Tools & Technologies Edition)

This guide prepares you to discuss the **3_USV** (P02_DocXIntegration_USV) project in interviews for a **3+ Years of Experience** developer role. It highlights high-level industry architecture patterns, security compliance, modern tool stacks, and resilient coding patterns.

---

## 1. Technological Stack & Tools Specification

To build a secure, enterprise-grade gateway, we utilized industry-standard frameworks, libraries, and DevOps tooling:

*   **Backend Framework**: **Java 17** & **Spring Boot 3.x** (Web, JPA, validation API).
*   **API Clients & Serialization**: **Spring RestTemplate** decorated with connection pooling and **Jackson ObjectMapper** for JSON parsing and schema mappings.
*   **Security & OAuth2**: **Google OAuth2 Service Account** framework, Google Drive API v3, JWT (JSON Web Tokens), and **HashiCorp Vault** for secure key/credential management.
*   **Resilience & Reliability**: **Resilience4j** for implementing Retry policies and Rate Limiters on external API gateways.
*   **Caching Layer**: **Redis Cache** to cache Google OAuth access tokens, minimizing API roundtrips.
*   **Containerization & Deployment**: **Docker** for containerizing the microservice and **Kubernetes** for scaling and managing orchestration.
*   **CI/CD Pipeline**: **GitHub Actions** for automated build, test (JUnit 5 + Mockito), security vulnerability scanning (SonarQube), and deployment.
*   **Monitoring & Observability**: Centralized logging via **SLF4J + Logback** integrated with **Splunk** (utilizing request-scoped thread renaming as a correlation ID for end-to-end tracing).

---

## 2. High-Level Design (HLD) & Architectural Specification

The microservice is designed as a **Stateless Gateway Orchestrator** using the **Orchestration Saga Pattern** to handle file transfers, permissions, and locking between OpenText Content Server (OTCS) and Google Drive.

```mermaid
graph TD
    Client[Client Browser / Frontend] -->|1. Edit Request| Ingress[Kubernetes Ingress / ALB]
    Ingress -->|2. Route Request| Pod[USV Gateway Pods <br>Spring Boot + Docker]
    Pod -->|3. Fetch Secret Credentials| Vault[HashiCorp Vault / AWS Secrets]
    Pod -->|4. Query Cache for Access Token| Redis[(Redis Token Cache)]
    Redis -->|5. Token Cache Miss| OAuth[Google OAuth2 Service]
    Pod -->|6. Check Node & Lock Node| OTCS[OpenText Content Server Repository]
    Pod -->|7. Download File Bytes| OTCS
    Pod -->|8. Upload File & Manage Sharing| GDrive[Google Drive API v3]
    Pod -->|9. Write Category Metadata| OTCS
    Pod -->|10. Return Redirect Link| Client
```

### Key Architectural Patterns
1.  **Orchestrator Pattern**: Centralizes transaction steps (checking reservation, locking files, creating folders, setting user access permissions, writing category metadata) into a single Spring Service (`UsvService.java`), preventing partial-state failures on the client side.
2.  **Transient Collaboration Workspace**: Treats Google Drive as an ephemeral workspace. Files are created on GDrive dynamically for real-time collaboration and deleted immediately upon merge, keeping all long-term data in the central repository (OTCS) for compliance audits.
3.  **Distributed Token Caching**: Uses a Redis cluster to store Google Access Tokens with a 50-minute Time-to-Live (matching the Google OAuth 1-hour expiration window) to avoid redundant authorization token requests.
4.  **Logging Correlation ID**: Uses request-specific thread renaming (`Thread.currentThread().setName(request.getOtcsDocId())`) to tag all log entries with the document ID. This enables distributed tracing across log aggregators like Splunk or ELK.

---

## 3. Core API Flows (Sequencing)

### A. Document Checkout & Edit (`POST /gdoc/edit`)
Checks if a document is locked. If not, it reserves it in OTCS, downloads the file, uploads it to Google Drive, grants write permission to the editor, and saves the Google File ID back to OTCS categories.

```mermaid
sequenceDiagram
    autonumber
    actor User as REST Client / Web App
    participant GW as USV Middleware Pod (Docker)
    participant Redis as Redis Cache
    participant OTCS as OpenText Content Server
    participant GDrive as Google Drive API v3

    User->>GW: POST /gdoc/edit (DocumentRequest payload)
    Note over GW: Thread renamed to otcsDocId (Correlation ID)
    GW->>OTCS: GET /api/v2/nodes/{id}?fields=properties
    OTCS-->>GW: Return properties (reserved = false)
    
    GW->>OTCS: POST /api/v2/nodes/{id} (Reserve/Lock Node)
    OTCS-->>GW: Return Lock Confirmation
    GW->>OTCS: GET /api/v1/nodes/{id}/content (Download File)
    OTCS-->>GW: Return file binary bytes
    
    GW->>Redis: Get Google Access Token
    alt Cache Miss
        GW->>GDrive: POST oauth2/v4/token (Exchange Refresh Token)
        GDrive-->>GW: Return Access Token
        GW->>Redis: Cache Access Token (TTL = 50 Mins)
    else Cache Hit
        Redis-->>GW: Return Cached Access Token
    end
    
    GW->>GDrive: POST /drive/v3/files (Upload file to GDrive folder)
    GDrive-->>GW: Return GDrive fileId
    GW->>GDrive: POST /drive/v3/files/{fileId}/permissions (Grant WRITER role to user email)
    GDrive-->>GW: Return Permission confirmation
    
    GW->>OTCS: DELETE /nodes/{id}/categories (Clear stale attributes)
    GW->>OTCS: POST /nodes/{id}/categories (Write GDriveDocId & EditorId)
    OTCS-->>GW: Return Metadata confirmation
    
    GW-->>User: Return 200 OK (with GDrive doc link)
```

---

### B. Document Check-in & Version Save (`POST /gdoc/addVersion`)
Verifies editor identity, releases the document lock, downloads the modified file from Google Drive, uploads it back to OTCS as a new version, and deletes the temporary GDrive file.

```mermaid
sequenceDiagram
    autonumber
    actor User as REST Client / Web App
    participant GW as USV Middleware Pod (Docker)
    participant OTCS as OpenText Content Server
    participant GDrive as Google Drive API v3

    User->>GW: POST /gdoc/addVersion (VersionRequest payload)
    GW->>OTCS: GET /nodes/{id}/categories (Retrieve metadata attributes)
    OTCS-->>GW: Return Category metadata (containing EditorId)
    
    alt User is NOT the original Editor
        GW-->>User: Return 400 Bad Request (Permission Exception)
    else User is the original Editor
        GW->>OTCS: PUT /nodes/{id} (Unreserve/Unlock Node)
        OTCS-->>GW: Return Unlock success
        
        alt addingVersion == true
            GW->>GDrive: GET /drive/v3/files/{fileId}?alt=media (Download updated file)
            GDrive-->>GW: Return updated file bytes
            GW->>OTCS: POST /nodes/{id}/versions (Upload as new version)
            OTCS-->>GW: Return Confirmation
        end
        
        GW->>OTCS: DELETE /nodes/{id}/categories (Clear GDriveDocId & EditorId attributes)
        OTCS-->>GW: Return Confirmation
        GW->>GDrive: DELETE /drive/v3/files/{fileId} (Delete temporary GDrive file)
        GDrive-->>GW: Return Delete Success
        
        GW-->>User: Return 200 OK (Success Message)
    end
```

---

## 4. Key Performance and Resilience Strategies (3+ Years Level)

1.  **Connection Pooling with HTTP Clients**:
    - Instantiated `RestTemplate` with a custom `HttpComponentsClientHttpRequestFactory` wrapping an Apache `HttpClient` connection pool configured for a maximum of 200 total connections and 50 connections per route. This prevents TCP socket starvation under concurrent workloads.
2.  **Resilience4j Integration**:
    - Wrapped outgoing HTTP REST requests in **Resilience4j Retries** (3 retries with exponential backoff) to automatically handle transient network glitches when communicating with Google and OpenText gateways.
3.  **Clean Containerized Deployments**:
    - Packaged using a multi-stage **Docker build** to create lightweight runtime containers (~250MB) and configured Kubernetes liveness/readiness probes targeting a Spring Boot Actuator endpoint (`/actuator/health`).
4.  **Unit and Integration Testing**:
    - Wrote comprehensive unit tests using **JUnit 5** and **Mockito** to mock remote REST API calls, ensuring a target of **80%+ code coverage**.
