// Admin panel behaviour.
//
// Category create/update/delete hit /api/admin/categories, user enable/disable
// hits /api/admin/users. Both are ADMIN-only and state-changing, so every
// request goes through csrfFetch() (see /js/csrf.js).

const CATEGORY_API = "/api/admin/categories";
const USER_API = "/api/admin/users";

function showError(elementId, message) {
    const box = document.getElementById(elementId);
    if (!box) {
        alert(message);
        return;
    }
    box.textContent = message;
    box.style.display = "block";
}

function clearError(elementId) {
    const box = document.getElementById(elementId);
    if (box) {
        box.textContent = "";
        box.style.display = "none";
    }
}

// GlobalExceptionHandler returns { timestamp, status, error, message } and, for
// validation failures, an extra "fields" map of field -> message. Prefer the
// field-level detail when present, since that is what the admin needs to fix.
function extractError(response, fallback) {
    return response.text().then(body => {
        if (!body) {
            return fallback;
        }
        try {
            const parsed = JSON.parse(body);

            if (parsed.fields) {
                const fieldMessages = Object.entries(parsed.fields)
                    .map(([field, message]) => `${field}: ${message}`);
                if (fieldMessages.length > 0) {
                    return fieldMessages.join("\n");
                }
            }
            return parsed.message || parsed.error || fallback;
        } catch (ignored) {
            return body;
        }
    });
}

function handleResponse(response, fallbackMessage) {
    if (!response.ok) {
        return extractError(response, fallbackMessage)
            .then(message => {
                throw new Error(message);
            });
    }
    return response.status === 204 ? null : response.json();
}

/* ---------------------------------------------------------- categories --- */

function createCategory(event) {
    event.preventDefault();
    clearError("category-error");

    const nameInput = document.getElementById("new-category-name");
    const descriptionInput = document.getElementById("new-category-description");

    csrfFetch(CATEGORY_API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            name: nameInput.value.trim(),
            description: descriptionInput.value.trim()
        })
    })
        .then(response => handleResponse(response, "Failed to add category"))
        .then(() => window.location.reload())
        .catch(error => showError("category-error", error.message));
}

function toggleCategoryEdit(row, editing) {
    row.querySelectorAll(".cell-view").forEach(el => el.hidden = editing);
    row.querySelectorAll(".cell-edit").forEach(el => el.hidden = !editing);
    row.querySelector(".btn-edit").hidden = editing;
    row.querySelector(".btn-delete").hidden = editing;
    row.querySelector(".btn-save").hidden = !editing;
    row.querySelector(".btn-cancel").hidden = !editing;
}

function saveCategory(row) {
    clearError("category-error");

    const id = row.dataset.categoryId;
    const name = row.querySelector(".category-name-input").value.trim();
    const description = row.querySelector(".category-description-input").value.trim();

    csrfFetch(`${CATEGORY_API}/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, description })
    })
        .then(response => handleResponse(response, "Failed to update category"))
        .then(() => window.location.reload())
        .catch(error => showError("category-error", error.message));
}

function deleteCategory(row) {
    clearError("category-error");

    const id = row.dataset.categoryId;
    const name = row.querySelector(".cell-view").textContent.trim();

    if (!confirm(`Delete category "${name}"?`)) {
        return;
    }

    csrfFetch(`${CATEGORY_API}/${id}`, { method: "DELETE" })
        .then(response => handleResponse(response, "Failed to delete category"))
        .then(() => window.location.reload())
        .catch(error => showError("category-error", error.message));
}

/* --------------------------------------------------------------- users --- */

function toggleUser(row, button) {
    clearError("user-error");

    const id = row.dataset.userId;
    const currentlyEnabled = button.dataset.enabled === "true";
    const action = currentlyEnabled ? "disable" : "enable";

    csrfFetch(`${USER_API}/${id}/${action}`, { method: "PATCH" })
        .then(response => handleResponse(response, `Failed to ${action} user`))
        .then(() => window.location.reload())
        .catch(error => showError("user-error", error.message));
}

/* ----------------------------------------------------------- wire it up --- */

document.addEventListener("DOMContentLoaded", () => {

    const createForm = document.getElementById("category-create-form");
    if (createForm) {
        createForm.addEventListener("submit", createCategory);
    }

    // Delegated so the handlers survive any future re-rendering of the rows.
    document.addEventListener("click", event => {
        const button = event.target.closest("button");
        if (!button) {
            return;
        }

        const categoryRow = button.closest("tr[data-category-id]");
        if (categoryRow) {
            if (button.classList.contains("btn-edit")) {
                toggleCategoryEdit(categoryRow, true);
            } else if (button.classList.contains("btn-cancel")) {
                toggleCategoryEdit(categoryRow, false);
            } else if (button.classList.contains("btn-save")) {
                saveCategory(categoryRow);
            } else if (button.classList.contains("btn-delete")) {
                deleteCategory(categoryRow);
            }
            return;
        }

        const userRow = button.closest("tr[data-user-id]");
        if (userRow && button.classList.contains("btn-toggle-user")) {
            toggleUser(userRow, button);
        }
    });
});
