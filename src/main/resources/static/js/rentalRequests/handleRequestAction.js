export function handleRequestAction(button, action) {
    const requestId = button.getAttribute('data-id');
    const url = `/api/rental-requests/${requestId}/${action}`;

    button.disabled = true;
    const originalText = button.textContent;
    button.textContent = "...";


    csrfFetch(url, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (response.ok) {
                window.location.reload();
            } else {
                alert("Cannot complete request. Please try again.");
                button.disabled = false;
                button.textContent = originalText;
            }
        })
        .catch(error => {
            console.error("Error:", error);
            alert("Network error");
            button.disabled = false;
            button.textContent = originalText;
        });
}