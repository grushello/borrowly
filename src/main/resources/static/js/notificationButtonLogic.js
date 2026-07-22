document.addEventListener('DOMContentLoaded', () => {
    const bellButton = document.getElementById('notifications-button');
    const panel = document.getElementById('notifications-panel');

    if (bellButton && panel) {
        bellButton.addEventListener('click', (e) => {
            e.stopPropagation();
            panel.classList.toggle('active');
        });

        document.addEventListener('click', (e) => {
            if (!panel.contains(e.target) && e.target !== bellButton) {
                panel.classList.remove('active');
            }
        });
    }
})