export function deleteButtonListener(btn){
    btn.addEventListener('click', function(event) {
        event.stopPropagation();

        if (!confirm("Are you sure you want to delete this item?")) {
            return;
        }

        const itemId = document.getElementById("main-content").getAttribute('data-item-id');
        const imageId = event.target.getAttribute('data-image-id');
        const deleteUrl = `/api/items/${itemId}/images/${imageId}`;

        event.target.disabled = true;

        csrfFetch(deleteUrl, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    window.location.reload();

                } else {
                    alert("Error while trying to delete");
                    event.target.disabled = false;
                }
            })
            .catch(err => {
                console.error("Error:", err);
                alert("Network Error.");
                this.disabled = false;
            });
    });
}