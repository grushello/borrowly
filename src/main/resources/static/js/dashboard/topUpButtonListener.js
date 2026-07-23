export function topUpButtonListener(topUpButton){
    topUpButton.addEventListener('click', async () => {

        const amountInput = document.getElementById('topUpAmount');
        const amount = Number(amountInput.value);

        if (!amount || amount <= 0) {
            alert('Amount must be positive.');
            return;
        }

        const response = await csrfFetch('/api/transactions/top-up', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                amount: amount
            })
        });

        if (response.ok) {
            alert('Balance refilled successfully.');
            window.location.reload();
        } else {
            const error = await response.json();
            alert(error.message || 'Failed to refill balance.');
        }
    });
}