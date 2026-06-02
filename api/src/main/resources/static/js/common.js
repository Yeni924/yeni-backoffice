// Mobile sidebar toggle
function toggleSidebar() {
    const sidebar = document.getElementById("sidebar");
    if (sidebar) {
        sidebar.classList.toggle("open");
    }
}

// Close sidebar when clicking outside on mobile
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
