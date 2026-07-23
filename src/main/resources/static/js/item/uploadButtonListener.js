export function uploadButtonListener(uploadBtn, fileInput){
    uploadBtn.addEventListener('click', function() {
        const files = fileInput.files;

        if (files.length === 0) {
            alert("Select at least an image before uploading.");
            return;
        }

        const file = files[0];
        const itemId = document.getElementById("main-content").getAttribute('data-item-id');
        const uploadUrl = `/api/items/${itemId}/images`;

        uploadBtn.disabled = true;
        uploadBtn.textContent = 'Uploading...';

        const formData = new FormData();
        formData.append('file', file);

        csrfFetch(uploadUrl, {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (response.ok) {
                    alert('Image uploaded correctly!');
                    window.location.reload();
                } else {
                    alert('Error while trying to upload');
                }
            })
            .catch(err => {
                console.error('Error:', err);
                alert('Network Error');
            })
            .finally(() => {
                uploadBtn.disabled = false;
                uploadBtn.textContent = 'Upload image';
                fileInput.value = '';
            });
    });
}