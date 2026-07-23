import {handleRequestAction} from "./handleRequestAction.js";
import {cancelRequest} from "./cancelRequest.js";

document.addEventListener("DOMContentLoaded", function() {
    const approveButtons = document.querySelectorAll('.approve-btn');
    const rejectButtons = document.querySelectorAll('.reject-btn');

    approveButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            if(confirm("Are you sure?")) {
                handleRequestAction(this, 'approve');
            }
        });
    });

    rejectButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            if(confirm("Are you sure you want to reject this request")) {
                handleRequestAction(this, 'reject');
            }
        });
    });

    const cancelBtn = document.getElementById("cancel-request-button");

    if (cancelBtn) {
        cancelRequest(cancelBtn);
    }
});


