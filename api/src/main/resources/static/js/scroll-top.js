(function () {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "scroll-top-btn";
    button.setAttribute("aria-label", "맨 위로 이동");
    button.textContent = "↑";
    document.body.appendChild(button);

    const toggleButton = function () {
        button.classList.toggle("is-visible", window.scrollY > 360);
    };

    button.addEventListener("click", function () {
        const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        window.scrollTo({
            top: 0,
            behavior: reducedMotion ? "auto" : "smooth"
        });
    });

    window.addEventListener("scroll", toggleButton, {passive: true});
    toggleButton();
})();
