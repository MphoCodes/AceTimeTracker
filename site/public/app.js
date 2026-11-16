const links=[...document.querySelectorAll('[data-screen]')];
const panels=[...document.querySelectorAll('[data-panel]')];
function select(name){for(const link of links) link.setAttribute('aria-current',String(link.dataset.screen===name));for(const panel of panels) panel.hidden=panel.dataset.panel!==name;}
if(links.length&&panels.length){document.documentElement.classList.add('enhanced');select('timer');for(const link of links)link.addEventListener('click',event=>{event.preventDefault();select(link.dataset.screen);});}
if(!matchMedia('(prefers-reduced-motion: reduce)').matches && 'IntersectionObserver' in window){const observer=new IntersectionObserver(items=>{for(const item of items)if(item.isIntersecting){item.target.classList.add('enter');observer.unobserve(item.target);}},{threshold:.12});document.querySelectorAll('h1,h2,.section-intro p,.hero-copy>p,.hero-screen,.screen-picker a,video,.install-layout,details,.footer-bottom').forEach(el=>observer.observe(el));}
