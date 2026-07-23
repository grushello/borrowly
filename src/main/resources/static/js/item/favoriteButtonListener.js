export function favoriteButtonListener(favBtn){
    favBtn.addEventListener('click', function() {
        const itemId = document.getElementById("main-content").getAttribute('data-item-id');
        const isFavorite = this.getAttribute('data-is-favorite') === 'true';

        const method = isFavorite ? 'DELETE' : 'POST';

        const url = '/api/favorites/' + itemId;

        favBtn.disabled = true;

        csrfFetch(url, {
            method: method,
        })
            .then(response => {
                if (response.ok) {
                    const newState = !isFavorite;
                    this.setAttribute('data-is-favorite', newState);
                    this.innerText = newState ? 'Remove from favorites' : 'Add to favorites';

                    this.classList.toggle('btn-add-fav', !newState);
                    this.classList.toggle('btn-remove-fav', newState);
                } else {
                    alert('Error during the update of favorites.');
                }
            })
            .catch(err => {
                console.error('Error:', err);
                alert('Network error.');
            })
            .finally(() => {
                favBtn.disabled = false;
            });
    });
}