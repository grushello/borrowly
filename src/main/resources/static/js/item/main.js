import {cancelButtonListener} from "./cancelButtonListener.js";
import {requestButtonListener} from "./requestButtonListener.js";
import {favoriteButtonListener} from "./favoriteButtonListener.js";
import {archiveButtonListener} from "./archiveButtonListener.js";
import {uploadButtonListener} from "./uploadButtonListener.js";
import {deleteButtonListener} from "./deleteButtonListener.js";
import {loadReviews} from "./reviews.js";

document.addEventListener('DOMContentLoaded', function() {
    loadReviews(0);
    const uploadBtn = document.getElementById('upload-btn');
    const fileInput = document.getElementById('image-upload');

    if (uploadBtn && fileInput) {
        uploadButtonListener(uploadBtn, fileInput)
    }

    const mainImage = document.querySelector('.main-image');
    const thumbnails = document.querySelectorAll('.thumbnail');

    if (mainImage && thumbnails.length > 0) {

        thumbnails.forEach(thumbnail => {
            thumbnail.addEventListener('click', function() {

                mainImage.src = this.src;

                thumbnails.forEach(t => t.style.opacity = '1');
                this.style.opacity = '0.5';
            });
        });

    }

    const deleteButtons = document.querySelectorAll('.delete-image-btn');

    deleteButtons.forEach(btn => {
        deleteButtonListener(btn)
    });

    const archiveBtn = document.getElementById('archive-btn');

    if (archiveBtn) {
        archiveButtonListener(archiveBtn)
    }

    const favBtn = document.getElementById('favorite-btn');

    if (favBtn) {
        favoriteButtonListener(favBtn)
    }

    const requestBtn = document.getElementById('request-btn');

    if (requestBtn) {
        requestButtonListener(requestBtn)
    }

    const cancelBtn = document.getElementById("request-btn-already-requested");

    if (cancelBtn) {
        cancelButtonListener(cancelBtn)
    }
});