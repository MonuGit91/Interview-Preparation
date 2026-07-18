const API_BASE = 'http://localhost:8080';

var isRedirectingToLoginPage = false;
const loginPage = 'login.html';

// Helper function for smart navigation
function navigateTo(page, openInNewWindow = false) {
    try {
        const currentUrl = window.location.href;
        const lastSlashIndex = currentUrl.lastIndexOf('/');

        let targetUrl;
        if (lastSlashIndex !== -1) {
            const baseUrl = currentUrl.substring(0, lastSlashIndex + 1);
            targetUrl = baseUrl + page;
        } else {
            targetUrl = page;
        }

        console.log(`Navigating to: ${targetUrl} (New Window: ${openInNewWindow})`);

        if (openInNewWindow) {
            window.open(targetUrl, '_blank');
        } else {
            window.location.replace(targetUrl);
        }
    } catch (e) {
        console.error("Navigation error", e);
        if (!openInNewWindow) {
            window.location.href = page;
        }
    }
}

// Modal Functions
function showModal(title, message) {
    const modal = document.getElementById('errorModal');
    if (modal) {
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalMessage').textContent = message;
        modal.classList.add('show');
    } else {
        alert(`${title}: ${message}`);
    }
}

function closeModal() {
    const modal = document.getElementById('errorModal');
    if (modal) {
        modal.classList.remove('show');
    }
    if (isRedirectingToLoginPage) {
        navigateTo(loginPage);
        isRedirectingToLoginPage = false;
    }
}

// Close modal when clicking outside
const errorModal = document.getElementById('errorModal');
if (errorModal) {
    errorModal.addEventListener('click', (event) => {
        if (event.target === errorModal) {
            closeModal();
        }
    });
}

// Handle Login Form Submit
// Handle Login manually to prevent reload issues
window.handleLogin = async function () {
    const loginForm = document.getElementById('loginForm');
    if (loginForm && !loginForm.checkValidity()) {
        loginForm.reportValidity();
        return;
    }

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    try {
        const response = await fetch(`${API_BASE}/auth/login/token`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('jwtToken', data.jwtToken);
            localStorage.setItem('refreshToken', data.refreshToken);
            console.log("Login successful, opening new window...");
            navigateTo('index.html', false);
        } else {
            showModal('Login Failed', 'Invalid credentials');
        }
    } catch (error) {
        console.error('Error:', error);
        showModal('Login Error', 'Unable to connect to server');
    }
};

// Handle Google Login Button
const googleBtn = document.getElementById('googleBtn');
if (googleBtn) {
    googleBtn.addEventListener('click', () => {
        window.location.href = `${API_BASE}/oauth2/authorization/google`;
    });
}

// Check for tokens on Index Page (Welcome Page)
document.addEventListener("DOMContentLoaded", () => {
    // Only run this check if we are on a page that displays user info
    if (document.getElementById('usernameDisplay')) {
        console.log("Detected Index Page Elements");

        // Check if we just came back from Google Login (URL params)
        const urlParams = new URLSearchParams(window.location.search);
        const urlToken = urlParams.get('token');
        const urlRefreshToken = urlParams.get('refreshToken');

        if (urlToken && urlRefreshToken) {
            localStorage.setItem('jwtToken', urlToken);
            localStorage.setItem('refreshToken', urlRefreshToken);
            // Clean URL
            const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
            window.history.replaceState({ path: newUrl }, '', newUrl);
        }

        const token = localStorage.getItem('jwtToken');
        if (!token) {
            console.warn("No token, redirecting to login...");
            navigateTo('login.html');
        } else {
            // Display User Info
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                document.getElementById('usernameDisplay').textContent = payload.sub;
                const providerElem = document.getElementById('providerDisplay');
                if (providerElem) providerElem.textContent = payload.provider || 'UNKNOWN';

                const tokenElem = document.getElementById('tokenPreview');
                if (tokenElem) tokenElem.textContent = token;

                const refreshToken = localStorage.getItem('refreshToken');
                const refreshElem = document.getElementById('refreshTokenPreview');
                if (refreshElem) refreshElem.textContent = refreshToken || 'Not available';
            } catch (e) {
                console.error("Invalid token", e);
            }
        }
    }
});

// Handle Registration Form Submit
const registerForm = document.getElementById('registerForm');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (password !== confirmPassword) {
            showModal('Validation Error', "Passwords do not match!");
            return;
        }

        try {
            const response = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password, confirmPassword })
            });

            if (response.ok) {
                //alert('Registration Successful! Please login.');
                showModal('Registration Successful', 'Please login.');
                isRedirectingToLoginPage = true;
            } else {
                const data = await response.text();
                showModal('Registration Failed', data);
            }
        } catch (error) {
            console.error('Error:', error);
            showModal('Registration Error', 'Unable to connect to server');
        }
    });
}

// Handle Logout
const logoutBtn = document.getElementById('logoutBtn');
if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('refreshToken');
        navigateTo('login.html');
    });
}
