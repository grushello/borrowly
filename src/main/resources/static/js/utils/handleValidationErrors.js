export function handleValidationErrors(errors) {
    Object.entries(errors.fields).forEach(([field, message]) => {
        const errorDiv = document.getElementById('error-' + field);
        const inputField = document.getElementById(field);

        if (errorDiv) {
            errorDiv.innerText = message;
            errorDiv.style.display = 'block';
        }

        if (inputField) {
            inputField.classList.add('is-invalid');
        }
    });
}