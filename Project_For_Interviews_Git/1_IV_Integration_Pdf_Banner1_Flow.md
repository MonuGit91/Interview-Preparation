# Operational Flow & Exception Handling: `/pdf/banner1`

This document details the complete end-to-end execution flow and exception propagation system for the `/pdf/banner1` endpoint.

---

## 1. Process Flow Diagram (Boxes & Arrows)

This flowchart traces the step-by-step process of the `/pdf/banner1` endpoint, highlighting decision gates (diamonds), logical operations (rectangles), and the execution path.

```mermaid
graph TD
    %% Define Styles
    classDef startEnd fill:#E6F2FF,stroke:#0066CC,stroke-width:2px,rx:10px,ry:10px;
    classDef process fill:#FFF2CC,stroke:#D6B656,stroke-width:1.5px;
    classDef decision fill:#F8CECC,stroke:#B85450,stroke-width:1.5px;
    classDef exception fill:#FADBD8,stroke:#C0392B,stroke-width:1.5px,stroke-dasharray: 5 5;
    classDef success fill:#D5E8D4,stroke:#82B366,stroke-width:2px;

    %% Elements
    Start((Client calls POST /pdf/banner1)):::startEnd
    
    subgraph Step 1: Input Validation & Templating
        V1[Log Payload & Parse Json]:::process
        D1{JSON Valid?}:::decision
        T1[Extract Placements & Heights]:::process
        T2[Build XmlBanner1 & Serialize via XmlMapper]:::process
        T3[Base64 Encode XML Content]:::process
        Err1[Throw RuntimeException: 400 Bad Request]:::exception
    end

    subgraph Step 2: Authentication & Token Exchange
        A1{Check Redis Cache for OTDS Ticket}:::decision
        A2[Fetch fresh OTDS Ticket]:::process
        A3[Save Ticket to Redis with TTL]:::process
        A4[Exchange OTDS Ticket for OTCS Session]:::process
        A5[Query OTCS for maxVersionNo of Node]:::process
        A6[Post tickets to IV and get Bearer Token + Session ID]:::process
    end

    subgraph Step 3: Async Transformation & Polling
        P1[Submit Publication POST with Base64 XML]:::process
        D2{Job ID returned?}:::decision
        Err2[Throw RuntimeException: Publication API failed]:::exception
        Poll1[Query IV Publication Status API]:::process
        D3{Status == Complete?}:::decision
        D4{Status == Failed?}:::decision
        Err3[Throw ExternalApiException: 502 Job Failed]:::exception
        PollWait[Wait 2 Seconds]:::process
    end

    subgraph Step 4: Download & Version Commit
        Down1[Download PDF Binary Bytes from IV]:::process
        DownRetry{Network Fail & Attempt < 3?}:::decision
        DownSleep[Sleep 2s & Increment Attempt]:::process
        Err4[Throw ExternalApiException: 502/Timeout]:::exception
        U1[Upload PDF to OTCS as New Version]:::process
        U2[Return uploaded node metadata to Controller]:::process
        EndResponse((Return 200 OK with metadata JSON)):::success
    end

    %% Flow Paths
    Start --> V1
    V1 --> D1
    D1 -- No --> Err1
    D1 -- Yes --> T1
    T1 --> T2
    T2 --> T3
    T3 --> A1
    
    A1 -- Cache Miss --> A2 --> A3 --> A4
    A1 -- Cache Hit --> A4
    A4 --> A5 --> A6 --> P1
    
    P1 --> D2
    D2 -- No --> Err2
    D2 -- Yes --> Poll1
    
    Poll1 --> D3
    D3 -- No --> D4
    D4 -- Yes --> Err3
    D4 -- No --> PollWait --> Poll1
    D3 -- Yes --> Down1
    
    Down1 --> DownRetry
    DownRetry -- Yes --> DownSleep --> Down1
    DownRetry -- No / Failed All --> Err4
    DownRetry -- Success --> U1
    
    U1 --> U2 --> EndResponse

    %% Global Exception Translation Gateway
    subgraph Exception Handling Gateway
        Err1 & Err2 & Err3 & Err4 --> GlobalAdvice[GlobalExceptionHandler intercepts]:::exception
        GlobalAdvice --> LogErr[Log diagnostics context & target URL]:::exception
        LogErr --> FormatResponse[Generate sanitized JSON response with matching HTTP Code]:::exception
        FormatResponse --> ErrResponse((Return Client Error Response <br> 400 / 401 / 404 / 502)):::startEnd
    end
```

---

## 2. Happy Path Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Client as REST Client / Caller
    participant Ctrl as ReqController
    participant Service as PdfService1 (Orchestrator)
    participant XmlFac as XmlObjFactory & XmlMapper
    participant OTDS as OpenText Directory Services
    participant OTCS as Content Server (OTCS)
    participant IV as Intelligent Viewing (IV)
    participant GlobalHandler as GlobalExceptionHandler

    Client->>Ctrl: POST /pdf/banner1 (PdfRequest1 payload)
    
    rect rgb(240, 248, 255)
        Note over Ctrl, XmlFac: Step 1: Input & Templating
        Ctrl->>Service: pdfBannerAgent(pdfRequest1)
        Service->>Service: getXmlContent(banner1Json, includePageNo)
        Note over Service: Parses map details (horizontal/vertical layout & key inclusions)
        Service->>XmlFac: xmlMapper.writeValueAsString(xmlBanner)
        XmlFac-->>Service: Return XML String
        Service->>XmlFac: xmlBase64Incoded(xmlContent)
        XmlFac-->>Service: Return Base64 Encoded XML string
    end

    rect rgb(255, 240, 245)
        Note over Service, IV: Step 2: Authentication & Token Exchange
        Service->>OTDS: getOtdsToken() (Retrieve Ticket)
        OTDS-->>Service: Return OTDS Ticket
        Service->>OTCS: getOtcsTicketJson() (Authenticate Repository)
        OTCS-->>Service: Return OTCS Session Ticket
        Service->>OTCS: getMaxVersion(nodeId, otcsTicket)
        OTCS-->>Service: Return Max Version No (e.g., 2)
        Service->>IV: callPubApi(baseUrl, otdsTicket, nodeId, maxVersionNo)
        IV-->>Service: Return IV Bearer Token & pubId (Session ID)
    end

    rect rgb(245, 255, 250)
        Note over Service, IV: Step 3: Transformation Pipeline
        Service->>IV: callPdfPublicatinApi_(nodeId, bearerToken, maxVersionNo, base64XML)
        IV-->>Service: Return Publication ID (jobId)
        
        loop Status Polling (Every 2 seconds)
            Service->>IV: callPublicationStatus(jobId, bearerToken)
            IV-->>Service: Return status (In-Progress / Complete)
        end
        
        Service->>IV: downloadDoc(jobId, bearerToken)
        Note over Service, IV: Downloads PDF binary bytes (Includes 3x automatic retries)
        IV-->>Service: Return PDF binary bytes
    end

    rect rgb(255, 250, 240)
        Note over Service, OTCS: Step 4: Version Commit & Response
        Service->>OTCS: addVersion(nodeId, otdsTicket, fileData, filename)
        OTCS-->>Service: Return uploaded version metadata JSON
        Service-->>Ctrl: Return metadata JSON node
        Ctrl-->>Client: 200 OK (With upload metadata JSON)
    end
```

---

## 3. Step-by-Step Execution Mechanics

1.  **Entry Point (`ReqController.java#convertToPDF1`)**:
    *   Exposes a HTTP `POST` mapping at `/pdf/banner1`.
    *   Receives `PdfRequest1` as the JSON request body (deserialized automatically by Jackson).
    *   Attempts to serialize the request object to log the payload. If an error occurs, it throws a `RuntimeException`. Otherwise, it invokes `pdfService1.pdfBannerAgent(request)`.
2.  **XML Generation (`PdfService1.java#getXmlContent`)**:
    *   Extracts location-specific text fields (`TopCenter`, `BottomLeft`, etc.) and font heights from the payload.
    *   Helper `getContentOfLocation` parses key-value maps inside each banner block:
        *   If `horizontal == true`: Formats map values separated by `" | "`.
        *   If `horizontal == false`: Formats map values separated by `"&#10;"` (numeric entity for line break).
        *   If `includesKey == true`: Prepends the key name to the value (e.g. `"Printed By : user1"`).
    *   Builds the `XmlBanner1` structural object and converts it to an XML String using `XmlMapper`.
    *   Post-processes the string replacing double-escaped entities (`&amp;#` back to `&#`) and base64 encodes the XML.
3.  **Token Exchange & Document Validation**:
    *   Calls `OtdsToken#getOtdsToken` to retrieve the active OTDS Ticket.
    *   Calls `OtcsToken#getOtcsTicketJson` to retrieve the active OTCS Session Ticket.
    *   Invokes `AllVersions#getMaxVersion` to fetch the highest current version of the node in Content Server.
    *   Sends a POST to IV's `/pub` ticket endpoint via `IVTicket#callPubApi`, obtaining an IV Bearer token and rendering session ID.
4.  **IV Document Publication & Polling**:
    *   Submits the transformation request to IV containing the document URL in OTCS and the base64 XML banner config.
    *   Receives a `Publication ID` and calls `checkStatus()`.
    *   Executes a polling loop every 2 seconds, querying `PublicationStatus` until the job returns `"Complete"`.
5.  **Download & Versioning**:
    *   Downloads the generated PDF bytes from IV using the Bearer Token.
    *   Calls `AddVersion#addVersion` to upload the bytes to OTCS as a new version under `request.getFinalDocName()`.
    *   Returns the upload metadata JSON response back to the REST client.

---

## 4. Exception Handling & Propagation Details

### Downstream Error Translation Flow
1.  **Downstream API Call Interception**:
    *   HTTP clients (e.g. `OtdsToken`, `IVTicket`, `DownloadArtifact`, `AddVersion`) execute HTTP queries.
    *   If a request fails with an HTTP error code (e.g., 401 or 404), the wrapper catches the response, reads the raw body, logs it, and constructs an `ExternalApiException`.
2.  **Exception Metadata Construction**:
    *   The thrown `ExternalApiException` includes:
        *   `statusCode`: The exact HTTP code returned by the downstream API.
        *   `errorBody`: The JSON error details returned by the OpenText server.
        *   `apiContext`: String explaining what action failed (e.g., `"OTCS Add Version"`).
        *   `url`: The exact API URL invoked.
3.  **Self-Healing Resilience**:
    *   Inside `DownloadArtifact#downloadDoc`, if a network I/O error or downstream exception occurs, a catch block increments an attempt counter.
    *   It logs a warning and sleeps the thread for 2 seconds. It retries up to 3 times before finally propagating the `ExternalApiException`.
4.  **Global Mapping (`GlobalExceptionHandler.java`)**:
    *   The advice class intercepts `ExternalApiException` using `@ExceptionHandler`.
    *   It extracts `errorBody`, inserts diagnostic properties (`apiContext`, `url`), and returns it to the caller in a `ResponseEntity` with the corresponding HTTP code.
    *   General exceptions (e.g., `NullPointerException`) are caught, logged privately, and return a generic `500 Internal Server Error` with a sanitized message, protecting the codebase from leaking internal stack traces.
