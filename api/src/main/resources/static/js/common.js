function toggleSidebar() {
    const sidebar = document.getElementById("sidebar");
    if (sidebar) {
        sidebar.classList.toggle("open");
    }
}

document.addEventListener("click", function(event) {
    const sidebar = document.getElementById("sidebar");
    const mobileMenuBtn = document.querySelector(".mobile-menu-btn");

    if (sidebar && mobileMenuBtn && window.innerWidth <= 768) {
        if (!sidebar.contains(event.target) && !mobileMenuBtn.contains(event.target)) {
            sidebar.classList.remove("open");
        }
    }
});

document.addEventListener("DOMContentLoaded", function() {
    initializeSidebarGroups();
    initializeAdminLogout();
});

window.AppLoading = (function () {
    let activeCount = 0;
    let overlay = null;
    let messageText = null;

    function ensureOverlay() {
        if (overlay) {
            return overlay;
        }

        overlay = document.createElement("div");
        overlay.className = "global-loading";
        overlay.setAttribute("role", "status");
        overlay.setAttribute("aria-live", "polite");
        overlay.innerHTML = '<div class="global-loading-box">'
            + '<div class="global-progress"><span></span></div>'
            + '<strong>화면을 준비하고 있어요</strong>'
            + '<p>필요한 데이터를 불러오는 중입니다.</p>'
            + '</div>';
        document.body.appendChild(overlay);
        messageText = overlay.querySelector("strong");
        return overlay;
    }

    function show(message) {
        activeCount += 1;
        ensureOverlay();
        if (messageText && message) {
            messageText.textContent = message;
        }
        overlay.classList.add("open");
    }

    function hide() {
        activeCount = Math.max(activeCount - 1, 0);
        if (overlay && activeCount === 0) {
            overlay.classList.remove("open");
        }
    }

    async function track(promise, message) {
        show(message);
        try {
            return await promise;
        } finally {
            hide();
        }
    }

    return {
        show: show,
        hide: hide,
        track: track
    };
})();

function initializeSidebarGroups() {
    document.querySelectorAll(".nav-group").forEach(function(group) {
        const groupCode = group.dataset.groupCode;
        const hasActive = group.dataset.hasActive === "true";
        const toggle = group.querySelector(".nav-group-toggle");
        const storageKey = "sidebar-group-" + groupCode;
        const saved = localStorage.getItem(storageKey);
        const expanded = hasActive || saved !== "collapsed";

        setGroupExpanded(group, expanded);

        if (toggle) {
            toggle.addEventListener("click", function() {
                const nextExpanded = group.classList.contains("is-collapsed");
                setGroupExpanded(group, nextExpanded);
                localStorage.setItem(storageKey, nextExpanded ? "expanded" : "collapsed");
            });
        }
    });
}

function setGroupExpanded(group, expanded) {
    const toggle = group.querySelector(".nav-group-toggle");
    group.classList.toggle("is-collapsed", !expanded);
    if (toggle) {
        toggle.setAttribute("aria-expanded", String(expanded));
    }
}

function initializeAdminLogout() {
    const logoutBtn = document.getElementById("adminLogoutBtn");
    if (!logoutBtn) {
        return;
    }

    logoutBtn.addEventListener("click", async function() {
        await fetch("/api/admin/auth/logout", { method: "POST" });
        window.location.href = "/dashboard";
    });
}

async function parseApiResponse(response) {
    const text = await response.text();
    const data = text ? JSON.parse(text) : {};
    if (response.ok) {
        return data;
    }
    const fieldMessages = Array.isArray(data.fieldErrors) && data.fieldErrors.length > 0
        ? " " + data.fieldErrors.map(function (fieldError) {
            return fieldError.field + ": " + fieldError.message;
        }).join(" / ")
        : "";
    const requestId = data.requestId ? " (requestId: " + data.requestId + ")" : "";
    const error = new Error((data.message || "요청 처리에 실패했습니다.") + fieldMessages + requestId);
    error.response = data;
    throw error;
}
