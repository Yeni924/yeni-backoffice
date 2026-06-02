document.addEventListener("DOMContentLoaded", function() {
    const form = document.getElementById("adminLoginForm");
    const errorBox = document.getElementById("loginError");

    form.addEventListener("submit", async function(event) {
        event.preventDefault();
        errorBox.hidden = true;

        const payload = {
            loginId: document.getElementById("loginId").value.trim(),
            password: document.getElementById("password").value
        };

        const response = await fetch("/api/admin/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const result = await response.json().catch(() => ({ message: "Login failed." }));
            errorBox.textContent = result.message || "Login failed.";
            errorBox.hidden = false;
            return;
        }

        window.location.href = "/dashboard";
    });
});
