/**
 * Utilidades DOM reutilizables: createElement y clearChildren (PROMPT 12, 13)
 */

/**
 * Limpia todos los nodos hijos de un contenedor.
 * @param {HTMLElement} element
 */
export function clearChildren(element) {
  if (!element) return;
  while (element.firstChild) {
    element.removeChild(element.firstChild);
  }
}

/**
 * Crea un elemento DOM de forma segura con propiedades, atributos y eventos.
 *
 * @param {string} tag
 * @param {Object} [options]
 * @returns {HTMLElement}
 */
export function createElement(tag, options = {}) {
  const el = document.createElement(tag);
  if (!options || typeof options !== "object") {
    return el;
  }

  for (const [key, val] of Object.entries(options)) {
    if (val === undefined || val === null) continue;

    if (key === "className" || key === "class") {
      el.className = String(val);
    } else if (key === "textContent" || key === "text") {
      el.textContent = String(val);
    } else if (key === "innerHTML" || key === "html") {
      el.innerHTML = String(val);
    } else if (key === "attrs" && typeof val === "object") {
      for (const [attrName, attrVal] of Object.entries(val)) {
        if (attrVal !== undefined && attrVal !== null) {
          el.setAttribute(attrName, String(attrVal));
        }
      }
    } else if (key === "style" && typeof val === "object") {
      Object.assign(el.style, val);
    } else if (key === "style" && typeof val === "string") {
      el.style.cssText = val;
    } else if (key === "events" && typeof val === "object") {
      for (const [evtName, handler] of Object.entries(val)) {
        if (typeof handler === "function") {
          el.addEventListener(evtName, handler);
        }
      }
    } else if (key === "children" && Array.isArray(val)) {
      for (const child of val) {
        if (child instanceof Node) {
          el.appendChild(child);
        } else if (typeof child === "string" || typeof child === "number") {
          el.appendChild(document.createTextNode(String(child)));
        }
      }
    } else if (key === "for") {
      el.setAttribute("for", String(val));
    } else if (key in el) {
      try {
        el[key] = val;
      } catch (_e) {
        el.setAttribute(key, String(val));
      }
    } else {
      el.setAttribute(key, String(val));
    }
  }

  return el;
}
