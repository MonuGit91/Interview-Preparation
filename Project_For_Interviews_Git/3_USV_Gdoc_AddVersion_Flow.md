# Operational Flow & Exception Handling: `/gdoc/addVersion`

This document details the complete end-to-end execution flow and exception propagation system for the `/gdoc/addVersion` endpoint.

---

## 1. Process Flow Diagram (Boxes & Arrows)

This flowchart traces the step-by-step process of the `/gdoc/addVersion` endpoint, highlighting decision gates (diamonds), logical operations (rectangles), and the execution path.

```mermaid
graph TD
    %% Define Styles
    classDef startEnd fill:#E6F2FF,stroke:#0066CC,stroke-width:2px,rx:10px,ry:10px;
    classDef process fill:#FFF2CC,stroke:#D6B656,stroke-width:1.5px;
    classDef decision fill:#F8CECC,stroke:#B85450,stroke-width:1.5px;
    classDef exception fill:#FADBD8,stroke:#C0392B,stroke-width:1.5px,stroke-dasharray: 5 5;
    classDef success fill:#D5E8D4,stroke:#82B366,stroke-width:2px;

    %% Elements
    Start((Client calls POST /gdoc/addVersion)):::startEnd
    
    subgraph Step 1: Editor Identity Verification
        V1[Log Payload & Set Thread Name to otcsDocId <br> for Splunk Correlation ID tracking]:::process
        GetCat[Fetch Category metadata details from OTCS]:::process
        ReadEditor[Extract EditorId from Category field]:::process
        D1{Does Requesting UserId == EditorId?}:::decision
        Err1[Throw PermissionException: 400 Bad Request]:::exception
    end

    subgraph Step 2: Unreserve & Version Check
        UnlockNode[Unreserve/Unlock Node in OTCS]:::process
        D2{isAddingVersion == true?}:::decision
        DownGdrive[Download updated file content from GDrive via pooled client]:::process
        UpOtcs[Upload file as a new version to OTCS]:::process
        SkipUpload[Skip download & version commit <br/> Discard changes message]:::process
    end

    subgraph Step 3: Cleanup & Delete
        DelCat[Delete EditorId & GDriveDocId Category attributes from Node]:::process
        DelGdrive[Delete document from Google Drive to prevent leak]:::process
    end

    subgraph Step 4: Response Dispatch
        EndResponse((Return 200 OK with success confirmation)):::success
    end

    %% Flow Paths
    Start --> V1
    V1 --> GetCat
    GetCat --> ReadEditor
    ReadEditor --> D1
    D1 -- No --> Err1
    D1 -- Yes --> UnlockNode
    UnlockNode --> D2
    
    %% Branching on addingVersion
    D2 -- Yes --> DownGdrive --> UpOtcs --> DelCat
    D2 -- No --> SkipUpload --> DelCat
    
    DelCat --> DelGdrive
    DelGdrive --> EndResponse

    %% Exception Handling
    subgraph Exception Gateway
        Err[Catch Exception]:::exception
        ErrLog[Log diagnostic error message to Splunk]:::exception
        ErrReturn((Return 400 Bad Request <br> with error details)):::startEnd
    end
    
    V1 & GetCat & UnlockNode & DownGdrive & UpOtcs & DelGdrive -.->|Throws Exception| Err
    Err --> ErrLog --> ErrReturn
```

---

## 2. Happy Path Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Client as REST Client / Web App
    participant GW as USV Middleware Pod (Docker)
    participant Cat as CatogeryApi (OTCS)
    participant Lock as ReserveToggle (OTCS)
    participant DownGDrive as GDriveDownloader (GDrive)
    participant UpOtcs as OtcsUploader (OTCS)
    participant DelGDrive as DeleteDocGDrive (GDrive)

    Client->>GW: POST /gdoc/addVersion (VersionRequest body)
    Note over GW: Sets thread name to request's otcsDocId (Splunk Correlation ID)
    GW->>Cat: getCatogeryDetailsById(docMetadata)
    Cat-->>GW: Return Category fields (containing Editor ID)
    Note over GW: Checks if requesting userId matches the category Editor ID

    rect rgb(255, 240, 245)
        Note over GW, Lock: Step 2: Unlock Document
        GW->>Lock: unreserveDoc(docMetadata)
        Lock-->>GW: Return Unlock confirmation
    end

    rect rgb(245, 255, 250)
        Note over GW, UpOtcs: Step 3: Fetch & Save New Version
        GW->>DownGDrive: getDriveDoc(gdriveDocId)
        Note over DownGDrive: Downloads file via Apache connection pool
        DownGDrive-->>GW: Return updated document bytes
        GW->>UpOtcs: addVersionToDoc(versionRequest, gdriveDocResponse)
        UpOtcs-->>GW: Return version upload confirmation status
    end

    rect rgb(255, 250, 240)
        Note over GW, DelGDrive: Step 4: Metadata Clear & File Deletion
        GW->>Cat: deleteCategory(dataMap)
        Cat-->>GW: Return delete confirmation
        GW->>DelGDrive: removeDriveDoc(gdriveDocId)
        DelGDrive-->>GW: Return delete confirmation
        GW-->>Client: 200 OK (Version confirmation message)
    end
```

---

## 3. Step-by-Step Execution Mechanics

1. **Entry Point (`ReqController.java#addVersion`)**:
   - Listens on `POST` requests at `/gdoc/addVersion`.
   - Modifies the execution thread name temporarily to `request.getOtcsDocId()` to track metrics (Correlation ID in Splunk/ELK).
   - Invokes `usvService.addVersionToDoc(request)`.

2. **Verify Editor ID (`UsvService.java#addVersionToDocument`)**:
   - Queries the document metadata from OTCS via `catogeryApi.getCatogeryDetailsById` to read the `EditorId` attribute.
   - Evaluates the Editor ID against the requesting user ID.
   - If they do **not** match, it blocks execution, logs a warning, and returns a `400 Bad Request` containing a `PermissionException` message.

3. **Unlock Document (`UsvService.java#addVersionToDocument`)**:
   - Releases the reservation lock on Content Server by executing `reserveToggle.unreserveDoc`.

4. **Add Version or Discard Changes**:
   - **Scenario A: `addingVersion` is True (Check-in edits)**:
     - Downloads the modified document version from Google Drive via `gdriveDownloader.getDriveDoc` (making a pooled HTTP connection).
     - Commits the downloaded document bytes as a new version on Content Server via `otcsUploader.addVersionToDoc`.
   - **Scenario B: `addingVersion` is False (Discard edits)**:
     - Skips the Google download and version upload.
     - Logs a discard action and prepares a discard confirmation.

5. **Metadata Cleanup & Google File Deletion**:
   - Invokes `categoryApi.deleteCategory` to clear the `EditorId` and `GDriveDocId` category fields from the OTCS node.
   - Invokes `deleteDocGDrive.removeDriveDoc` to delete the temporary document from Google Drive to satisfy data residency requirements.
   - Returns the confirmation response.

---

## 4. Exception Handling & Propagation Details

### Downstream Error Translation
1. **Downstream API Failures**:
   - HTTP clients capture non-2xx status codes (such as Google Drive access token expiration, network timeouts, or OTCS check-in failures).
2. **Controller Level Try-Catch Wrapper**:
   - `UsvService.java#addVersionToDoc` handles execution errors within a `try-catch` block.
   - If any downstream API error occurs, the code catches the exception and returns the error message directly inside a `ResponseEntity` with a status of `400 Bad Request`.
