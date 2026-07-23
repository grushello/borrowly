export function archiveButtonListener(archiveBtn){
    archiveBtn.addEventListener('click', (event) => {
        event.stopPropagation();

        const itemId = document.getElementById("main-content").getAttribute('data-item-id');

        const isArchiving = event.target.textContent === "Archive";
        const method = isArchiving ? "DELETE" : "PATCH";
        const url = isArchiving ? "/api/items/" + itemId : "/api/items/" + itemId + "/unarchive";

        event.target.disabled = true;

        csrfFetch(url, {
            method: method,
        }).then(response => {
            if (response.ok) {
                event.target.textContent = isArchiving ? 'Unarchive' : 'Archive';
            } else {
                alert(isArchiving ? 'Error trying to archive item.' : 'Error trying to unarchive item.');
            }
        }).catch(err => {
            console.error('Error:', err);
            alert('Network error.');
        }).finally(() => {
            event.target.disabled = false;
        });
    })
}