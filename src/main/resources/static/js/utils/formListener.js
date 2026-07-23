import {handleValidationErrors} from "./handleValidationErrors.js";

export function formListener(form, method){
    form.addEventListener('submit', function(e) {
        e.preventDefault();

        document.querySelectorAll('.error-message').forEach(el => {
            el.style.display = 'none';
            el.innerText = '';
        });
        document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));

        const formData = new FormData(form);
        const data = Object.fromEntries(formData.entries());
        const id = form.getAttribute('data-item-id');

        data.pricePerDay = parseFloat(data.pricePerDay);
        data.depositAmount = parseFloat(data.depositAmount);
        data.finePerDay = parseFloat(data.finePerDay);

        csrfFetch('/api/items/'+ id, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        })
            .then(async response => {
                if (response.ok) {
                    const result = await response.json();
                    window.location.href = '/item/' + result.id;
                } else if (response.status === 400) {
                    const errors = await response.json();
                    handleValidationErrors(errors);
                } else {
                    alert("There was an unexpected error.");
                }
            })
            .catch(error => console.error("Network error:", error));
    });
}