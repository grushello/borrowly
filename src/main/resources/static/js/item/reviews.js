export function loadReviews(page) {
    var itemId = document.getElementById('main-content').getAttribute('data-item-id');
    var reviewsList = document.getElementById('reviews-list');
    var reviewsEmpty = document.getElementById('reviews-empty');
    var reviewsLoading = document.getElementById('reviews-loading');
    var reviewsPagination = document.getElementById('reviews-pagination');

    reviewsLoading.style.display = 'block';
    reviewsEmpty.style.display = 'none';
    reviewsList.innerHTML = '';

    fetch('/api/items/' + itemId + '/reviews?page=' + page + '&size=5&sort=createdAt,desc')
        .then(function(response) { return response.json(); })
        .then(function(data) {
            reviewsLoading.style.display = 'none';

            if (data.totalElements === 0) {
                reviewsEmpty.style.display = 'block';
                reviewsPagination.style.display = 'none';
                return;
            }

            data.content.forEach(function(review) {
                var card = document.createElement('div');
                card.className = 'review-card';

                var stars = renderStars(review.rating);
                var date = new Date(review.createdAt);
                var formattedDate = date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
                var initial = review.reviewer.firstName ? review.reviewer.firstName.charAt(0).toUpperCase() : '?';
                var reviewerName = escapeHtml(review.reviewer.firstName + ' ' + review.reviewer.lastName);

                card.innerHTML =
                    '<div class="review-card-header">' +
                    '<a href="/user/' + review.reviewer.id + '" class="review-author">' +
                    '<div class="review-avatar"><span>' + initial + '</span></div>' +
                    '<div class="review-author-info">' +
                    '<strong class="review-author-name">' + reviewerName + '</strong>' +
                    '<span class="review-date">' + formattedDate + '</span>' +
                    '</div>' +
                    '</a>' +
                    '<div class="review-stars">' + stars + '</div>' +
                    '</div>' +
                    (review.comment ? '<p class="review-comment">' + escapeHtml(review.comment) + '</p>' : '');

                reviewsList.appendChild(card);
            });

            if (data.totalPages > 1) {
                renderReviewsPagination(data.number, data.totalPages);
                reviewsPagination.style.display = 'flex';
            } else {
                reviewsPagination.style.display = 'none';
            }
        })
        .catch(function(err) {
            console.error('Error loading reviews:', err);
            reviewsLoading.style.display = 'none';
            reviewsEmpty.style.display = 'block';
            reviewsEmpty.textContent = 'Failed to load reviews.';
        });
}

function renderStars(rating) {
    var stars = '';
    for (var i = 1; i <= 5; i++) { stars += i <= rating ? '★' : '☆'; }
    return stars;
}

function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function renderReviewsPagination(currentPage, totalPages) {
    var list = document.getElementById('reviews-pagination-list');
    list.innerHTML = '';

    var prevLi = document.createElement('li');
    prevLi.className = 'page-item' + (currentPage === 0 ? ' disabled' : '');
    var prevLink = document.createElement('a');
    prevLink.className = 'page-link';
    prevLink.href = '#reviews-section';
    prevLink.textContent = '‹';
    if (currentPage > 0) {
        prevLink.addEventListener('click', function(e) { e.preventDefault(); loadReviews(currentPage - 1); });
    }
    prevLi.appendChild(prevLink);
    list.appendChild(prevLi);

    for (var i = 0; i < totalPages; i++) {
        var li = document.createElement('li');
        li.className = 'page-item' + (i === currentPage ? ' active' : '');
        var link = document.createElement('a');
        link.className = 'page-link';
        link.href = '#reviews-section';
        link.textContent = (i + 1).toString();
        if (i !== currentPage) {
            (function(pageNum) {
                link.addEventListener('click', function(e) { e.preventDefault(); loadReviews(pageNum); });
            })(i);
        }
        li.appendChild(link);
        list.appendChild(li);
    }

    var nextLi = document.createElement('li');
    nextLi.className = 'page-item' + (currentPage === totalPages - 1 ? ' disabled' : '');
    var nextLink = document.createElement('a');
    nextLink.className = 'page-link';
    nextLink.href = '#reviews-section';
    nextLink.textContent = '›';
    if (currentPage < totalPages - 1) {
        nextLink.addEventListener('click', function(e) { e.preventDefault(); loadReviews(currentPage + 1); });
    }
    nextLi.appendChild(nextLink);
    list.appendChild(nextLi);
}