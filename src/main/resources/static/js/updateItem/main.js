import {formListener} from "../utils/formListener.js";

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('updateItemForm');
    if (form) {
        formListener(form, "PATCH")
    }
});