# API Documentation

## Base URL
The application runs on `http://localhost:8080`.

## Authentication Endpoints (`/auth`)

### 1. Register User
Registers a new user in the system.
- **URL**: `/auth/register`
- **Method**: `POST`
- **Request Body**: JSON
  ```json
  {
    "username": "user1",
    "password": "password123",
    "email": "user1@example.com",
    "roles": ["ROLE_USER"]
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "message": "User registered successfully",
    "success": true,
    "username": "user1"
  }
  ```

### 2. Login
Authenticates a user and returns JWT tokens.
- **URL**: `/auth/login/token`
- **Method**: `POST`
- **Request Body**: JSON
  ```json
  {
    "username": "user1",
    "password": "password123"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "username": "user1",
    "jwtToken": "eyJhbGciOi...",
    "refreshToken": "d8e3b...",
    "roles": ["ROLE_USER"]
  }
  ```

### 3. Refresh Token
Obtains a new Access Token using a valid Refresh Token.
- **URL**: `/auth/login/refreshtoken`
- **Method**: `POST`
- **Request Body**: JSON
  ```json
  {
    "refreshToken": "d8e3b..."
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "d8e3b...",
    "tokenType": "Bearer"
  }
  ```

### 4. Refresh Token via Cookie
Obtains a new Access Token using a HttpOnly `refresh_token` cookie.
- **URL**: `/auth/refresh-token-cookie`
- **Method**: `POST`
- **Cookies**: `refresh_token=<token>`
- **Response**: `200 OK`
  ```json
  {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "d8e3b...",
    "tokenType": "Bearer"
  }
  ```

### 5. Logout (Revoke Token)
 logs out the user by invalidating the refresh token and blacklisting the access token.
- **URL**: `/auth/refreshtoken/revoke`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <access_token>`
- **Request Body**: JSON
  ```json
  {
    "refreshToken": "d8e3b...",
    "allDevices": false
  }
  ```
- **Response**: `200 OK` ("Logged out successfully")

---

## OAuth2 Endpoints

### 1. Initiate Google Login
Redirects the browser to Google for authentication.
- **URL**: `/oauth2/authorization/google`
- **Method**: `GET` (Browser)

### 2. Login Success
After Google authentication, the server **redirects** the browser back to the frontend application.
- **Response**: `302 Found` (Redirect)
- **Target URL**: `http://localhost:5500?token=<jwt_token>&refreshToken=<refresh_token>`
- **Cookies**: Sets a `refresh_token` HttpOnly cookie.

---

## Protected Resources (`/api`)
All endpoints below require the `Authorization: Bearer <access_token>` header.

### 1. Common Endpoint
Accessible by any authenticated user.
- **URL**: `  `
- **Method**: `GET`
- **Response**: `200 OK` ("Common Endpoint...")

### 2. User Endpoint
Accessible only by users with `ROLE_USER`.
- **URL**: `/api/user/hello`
- **Method**: `GET`
- **Response**: `200 OK` ("Hello, user1! (User Role)")

### 3. Admin Endpoint
Accessible only by users with `ROLE_ADMIN`.
- **URL**: `/api/admin/hello`
- **Method**: `GET`
- **Response**: `200 OK` ("Hello, admin! (Admin Role)")
