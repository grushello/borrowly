export function requestButtonListener(requestBtn){
    requestBtn.addEventListener('click', function(event) {
        const startDateInput = document.getElementById('start-date').value;
        const endDateInput = document.getElementById('end-date').value;
        const itemId = document.getElementById("main-content").getAttribute('data-item-id');

        if (!startDateInput || !endDateInput) {
            alert("Please select both start and end dates.");
            return;
        }

        if (new Date(endDateInput) <= new Date(startDateInput)) {
            alert("End date must be after start date.");
            return;
        }

        const payload = {
            itemId: itemId,
            startDate: startDateInput,
            endDate: endDateInput
        };

        requestBtn.disabled = true;
        requestBtn.textContent = "Sending...";

        csrfFetch('/api/rental-requests', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
            .then(async response => {
                if (response.ok) {
                    alert("Request sent successfully!");
                    window.location.href = "/rental-requests";
                } else {
                    const errorData = await response.json();
                    const errorBox = document.getElementById('booking-error');
                    if (errorBox) errorBox.style.display = 'none';

                    if (errorBox) {
                        errorBox.textContent = errorData.message || "There was an error with the request...";
                        console.log(errorData)

                        if (errorData.fields) {
                            const fieldErrors = Object.entries(errorData.fields)
                                .map(([field, msg]) => `${field}: ${msg}`)
                                .join(" | ");
                            errorBox.textContent += " - " + fieldErrors;
                        }

                        errorBox.style.display = 'block';
                    } else {
                        alert(errorData.message || "Error during the request");
                    }
                }
            })
            .catch(error => {
                console.error("Error:", error);
                alert("Network error.");
            })
            .finally(() => {
                requestBtn.disabled = false;
                requestBtn.textContent = "Request";
            });
    });
}