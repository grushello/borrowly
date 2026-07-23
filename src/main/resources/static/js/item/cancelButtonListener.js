export function cancelButtonListener(cancelBtn){
    cancelBtn.addEventListener("click", async (e) => {
        e.preventDefault();

        if (!confirm("Are you sure you want to cancel this request?")) {
            return;
        }

        const rentalRequestId = cancelBtn.getAttribute("data-rental-request-id");

        const originalText = cancelBtn.textContent;
        cancelBtn.disabled = true;
        cancelBtn.textContent = "Cancelling...";

        try {
            const response = await csrfFetch(`/api/rental-requests/${rentalRequestId}/cancel`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                window.location.reload();
            } else {
                alert("There was an error. Try again later.");
                restoreButton();
            }
        } catch (error) {
            console.error("Network error:", error);
            alert("Connection refused. Try again later.");
            restoreButton();
        }

        function restoreButton() {
            cancelBtn.disabled = false;
            cancelBtn.textContent = originalText;
        }
    });
}