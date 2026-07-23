document.getElementById('signinForm').addEventListener('submit', async function(event) {
    event.preventDefault();

    const email = document.getElementById('email-input').value.trim().toLowerCase();
    const password = document.getElementById('password-input').value;

    try {
        const response = await csrfFetch('/api/auth/signin', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, password: password })
        });

        if (response.ok) {
            window.location.href = '/dashboard';
        } else {
            const errorData = await response.json();

            const errorDiv = document.getElementById('error-message');
            errorDiv.textContent = errorData.message || "Error during authentication";
            errorDiv.style.display = 'block';
        }
    } catch (error) {
        console.error("Network error:", error);
    }
});
