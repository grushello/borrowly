export function returnItemButtonListener(returnItemButton){
    returnItemButton.addEventListener('click', (e) => {
        const rentalId = e.target.getAttribute('data-rental-id');
        const url = `/api/rentals/${rentalId}/return`;
        console.log(url)
        csrfFetch(url, {
            method: 'PATCH'})
            .then(async res => {
                if (res.ok) {
                    alert('Item returned correctly.');
                    window.location.reload();
                } else {
                    const error = await res.json();
                    console.log(error.message)
                    alert(error.message);
                }
            })
    })
}