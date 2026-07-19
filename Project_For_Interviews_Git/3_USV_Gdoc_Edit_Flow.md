# Operational Flow & Exception Handling: `/gdoc/edit`

This document details the complete end-to-end execution flow and exception propagation system for the `/gdoc/edit` endpoint.

---

## 1. Process Flow Diagram (Boxes & Arrows)

This flowchart traces the step-by-step process of the `/gdoc/edit` endpoint, highlighting decision gates (diamonds), logical operations (rectangles), and the execution path.

```mermaid
graph TD
    %% Define Styles
    classDef startEnd fill:#E6F2FF,stroke:#0066CC,stroke-width:2px,rx:10px,ry:10px;
    classDef process fill:#FFF2CC,stroke:#D6B656,stroke-width:1.5px;
    classDef decision fill:#F8CECC,stroke:#B85450,stroke-width:1.5px;
    classDef exception fill:#FADBD8,stroke:#C0392B,stroke-width:1.5px,stroke-dasharray: 5 5;
    classDef success fill:#D5E8D4,stroke:#82B366,stroke-width:2px;

    %% Elements
    Start((Client calls POST /gdoc/edit)):::startEnd
    
    subgraph "Step 1: Request Initialization & Node Check"
        V1[Log Payload & Set Thread Name to otcsDocId <br> for Splunk Correlation ID tracking]:::process
        GetProps[Query OTCS for Node properties]:::process
        D1{Is Reserved/Locked?}:::decision
    end

    subgraph "Step 2: Path A - Document Already Reserved (ReadOnly Workflow)"
        GetCat[Fetch Category details from OTCS]:::process
        ReadGId[Extract GDriveDocId from Category]:::process
        D2{Is Current User the Reserving Editor?}:::decision
        ShareReader[Grant Google Drive READER permissions to Visitor]:::process
        ReturnGId[Return existing GDriveDocId]:::process
    end

    subgraph "Step 3: Path B - Document Open (Lock & Upload Workflow)"
        LockNode[Lock/Reserve Node in OTCS under Current Editor]:::process
        DownDoc[Download source file binary from OTCS]:::process
        CheckRedis{Access Token in Redis Cache?}:::decision
        FetchToken[Request Google Access Token via OAuth API]:::process
        SaveRedis[Save Access Token to Redis with 50-Min TTL]:::process
        UpGdrive[Upload document to Google Drive via Pooled HttpClient]:::process
        ShareWriter[Grant Google Drive WRITER permissions to Editor]:::process
        DelStale[Delete stale OTCS categories if present]:::process
        WriteCat[Apply Category: Save GDriveDocId & EditorId to Node]:::process
    end

    subgraph "Step 4: Response Dispatch"
        EndResponse((Return 200 OK with GDriveDocId JSON)):::success
    end

    %% Flow Paths
    Start --> V1
    V1 --> GetProps
    GetProps --> D1
    
    %% Path A (Reserved)
    D1 -- Yes --> GetCat
    GetCat --> ReadGId
    ReadGId --> D2
    D2 -- No --> ShareReader --> ReturnGId
    D2 -- Yes --> ReturnGId
    ReturnGId --> EndResponse

    %% Path B (Open)
    D1 -- No --> LockNode
    LockNode --> DownDoc
    DownDoc --> CheckRedis
    
    CheckRedis -- Cache Miss --> FetchToken --> SaveRedis --> UpGdrive
    CheckRedis -- Cache Hit --> UpGdrive
    
    UpGdrive --> ShareWriter
    ShareWriter --> DelStale
    DelStale --> WriteCat
    WriteCat --> EndResponse

    %% Exception Handling
    subgraph "Exception Gateway"
        Err[Catch Exception]:::exception
        ErrLog[Log diagnostic error message to Splunk]:::exception
        ErrReturn((Return 400 Bad Request <br> with error message JSON)):::startEnd
    end
    
    V1 & GetProps & GetCat & LockNode & DownDoc & UpGdrive & WriteCat -.->|Throws Exception| Err
    Err --> ErrLog --> ErrReturn
```

---

## 2. Happy Path Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Client as REST Client / Web App
    participant Ctrl as ReqController
    participant Service as UsvService (Orchestrator)
    participant Node as NodeApi (OTCS)
    participant Lock as ReserveToggle (OTCS)
    participant Down as DownloadDoc (OTCS)
    participant Redis as Redis Cache
    participant UpGDrive as UploadFileContentGDrive (GDrive)
    participant Share as PermissionApiGdrive (GDrive)
    participant Cat as CatogeryApi (OTCS)

    Client->>Ctrl: POST /gdoc/edit (DocumentRequest body)
    Note over Ctrl: Sets thread name to request's otcsDocId (Splunk trace)
    Ctrl->>Service: DocExportOtcsToGDrive(documentRequest)
    Service->>Service: exportOtcsToGDrive(documentRequest)
    
    rect rgb(240, 248, 255)
        Note over Service, Node: Step 1: Check Node Status
        Service->>Node: getNodesProperty(docMetadata)
        Node-->>Service: Return Node Properties (reserved = false)
    end

    rect rgb(255, 240, 245)
        Note over Service, Down: Step 2: Lock Document & Download
        Service->>Lock: reserveDoc(documentRequest)
        Lock-->>Service: Return Lock Success
        Service->>Down: GetDoc(documentRequest, name)
        Down-->>Service: Return file binary bytes (ResponseEntity<byte[]>)
    end

    rect rgb(245, 255, 250)
        Note over Service, UpGDrive: Step 3: Google Drive Upload & Permissioning
        Service->>Redis: Query Access Token
        alt Cache Miss
            Service->>UpGDrive: Retrieve fresh token from Google OAuth
            UpGDrive->>Redis: Save token (TTL = 50 Mins)
        else Cache Hit
            Redis-->>Service: Return Cached Access Token
        end
        Service->>UpGDrive: uploadToGdrive(docResponse, name, rootFolder, documentRequest)
        Note over UpGDrive: Submits file via Apache HttpClient connection pool
        UpGDrive-->>Service: Return uploadResponse (containing GDrive File ID)
        Service->>Share: grantPermission(fileId, email, isWriter = true)
        Share-->>Service: Return share confirmation
    end

    rect rgb(255, 250, 240)
        Note over Service, Cat: Step 4: Metadata Binding & Return
        Service->>Cat: deleteCategory(dataMap)
        Cat-->>Service: Return Delete Confirmation
        Service->>Cat: applyCategory(documentRequest, fileId)
        Cat-->>Service: Return Category Confirmation
        Service-->>Ctrl: Return uploadResponse (GDrive JSON)
        Ctrl-->>Client: 200 OK (with GDrive File ID)
    end
```

---

## 3. Step-by-Step Execution Mechanics

1. **Entry Point (`ReqController.java#getDocumentById`)**:
   - Accepts a `POST` request at `/gdoc/edit` containing `DocumentRequest` parameters.
   - Modifies the execution thread name temporarily to `request.getOtcsDocId()` to assist in logging analysis (Correlation ID).
   - Delegates execution to `usvService.DocExportOtcsToGDrive(request)`.

2. **Check Reservation Status (`UsvService.java#exportOtcsToGDrive`)**:
   - Queries the document metadata from OTCS via `nodeApi.getNodesProperty` to retrieve `reserved` (boolean) status and the lock owner (`reserved_user_id`).
   - Sets the local filename using the retrieved properties.

3. **Check-out Strategy - Branching Execution**:
   - **Scenario A: The Document is Reserved (Locked)**:
     - Fetches existing category details on Content Server using `catogeryApi.getCatogeryDetailsById`.
     - Extracts the `gdriveDocId` stored under the designated category field ID.
     - Checks if the user requesting the edit matches the reserving user. If they do **not** match, it executes `permissionApiGdrive.grantPermission` to dynamically assign a reader role (`isWriter = false`) to the new visitor's email.
     - Directly returns the existing `gdriveDocId` to the client.
   - **Scenario B: The Document is Open (Available)**:
     - Issues a lock command via `reserveToggle.reserveDoc` to lock the document in OTCS.
     - Downloads the file binary content from Content Server using `downloadDoc.GetDoc`.
     - Queries Redis for the Google Access Token; if expired or not present, it fetches a new token from Google OAuth and caches it in Redis with a 50-minute TTL.
     - Uploads the downloaded document to Google Drive under the defined workspace folder via `uploadFileContentGDrive.uploadToGdrive` using pooled HTTP connections.
     - Grants edit permissions (`role: writer`) to the editor's email via `permissionApiGdrive.grantPermission(..., true)`.
     - Wipes any stale categories and applies fresh attributes storing `GDriveDocId` and the editor's `userId` onto the OTCS node via `categoryApi.applyCategory`.
     - Returns the new Google Drive File ID to the client.

---

## 4. Exception Handling & Propagation Details

### Downstream Error Translation
1. **Downstream API Exception Catching**:
   - Client wrappers (`CallingOtcsApi`, `CallingGDriveApi`) monitor HTTP requests.
   - If REST requests return non-2xx status codes (such as a 403 Forbidden or 404 Not Found), custom exception handlers wrap the response.
2. **Global Controller Level Wrapper**:
   - `UsvService.java#DocExportOtcsToGDrive` surrounds execution with a `try-catch` block.
   - If any exception is thrown (e.g., `IllegalClientSecretException` or `PermissionException`), the service catches the error, parses the details into a standard JSON message body using `jsonObj.getJson`, and returns it inside a `ResponseEntity` with an HTTP status of `400 Bad Request`.
