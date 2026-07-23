import {formListener} from "../utils/formListener.js";

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('createItemForm');

    if (form) {
        formListener(form, "POST")
    }
});