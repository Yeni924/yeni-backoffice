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
