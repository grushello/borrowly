document.getElementById('signup-form').addEventListener('submit', async function(event) {
    event.preventDefault();

    const payload = {
        firstName: document.getElementById('first-name-input').value,
        lastName: document.getElementById('last-name-input').value,
        email: document.getElementById('email-input').value.trim().toLowerCase(),
        phone: document.getElementById('phone-input').value || null,
        password: document.getElementById('password-input').value,
    }

    try {
        const response = await csrfFetch('/api/auth/signup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            const errorDiv = document.getElementById('error-message');
            errorDiv.style.display = 'hide';
            window.location.href = '/dashboard';
        } else {
            const errorData = await response.json();
            const errorDiv = document.getElementById('error-message');

            if (response.status === 400 && errorData.fields) {
                let validationErrors = "Attention:\n";
                for (const [field, errorMessage] of Object.entries(errorData.fields)) {
                    validationErrors += `- ${field}: ${errorMessage}\n`;
                }
                errorDiv.innerText = validationErrors;

            } else {
                errorDiv.textContent = errorData.message || "Error during registration";
            }

            errorDiv.style.display = 'block';
        }
    } catch (error) {
        console.error("Network error:", error);
    }
});