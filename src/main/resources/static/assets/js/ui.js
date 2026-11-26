/**
 * Utilidades de interfaz de usuario, creación segura de DOM, iconos SVG, alertas y modales (PROMPT 09 - Secciones 17, 18, 20 y 21)
 */

import { createElement } from "./dom.js";

const SVG_NS = "http://www.w3.org/2000/svg";

const ICON_PATHS = Object.freeze({
  dashboard: ["M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"],
  users: ["M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"],
  badge: ["M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-8 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm6 11H6v-1c0-2 4-3.1 6-3.1s6 1.1 6 3.1v1z"],
  store: ["M20 4H4v2h16V4zm1 10v-2l-1-5H4l-1 5v2h1v6h10v-6h4v6h2v-6h1zm-9 4H6v-4h6v4z"],
  folder: ["M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"],
  clipboard: ["M19 3h-4.18C14.4 1.84 13.3 1 12 1s-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm-2 14-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"],
  certificate: ["M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"],
  payment: ["M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z"],
  bell: ["M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"],
  chart: ["M5 9.2h3V19H5V9.2zM10.6 5h2.8v14h-2.8V5zm5.6 8H19v6h-2.8v-6z"],
  shield: ["M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"],
  userCog: ["M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"],
  settings: ["M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"],
  logout: ["M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"],
  menu: ["M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"],
  close: ["M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"],
  chevronDown: ["M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"]
});

/**
 * Crea un elemento SVG accesible sin usar innerHTML.
 */
export function createIcon(name, className = "nav-icon") {
  const svg = document.createElementNS(SVG_NS, "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("fill", "currentColor");
  svg.setAttribute("aria-hidden", "true");
  if (className) {
    svg.setAttribute("class", className);
  }
  const paths = ICON_PATHS[name] || ICON_PATHS.dashboard;
  for (const d of paths) {
    const pathEl = document.createElementNS(SVG_NS, "path");
    pathEl.setAttribute("d", d);
    svg.appendChild(pathEl);
  }
  return svg;
}

/**
 * Limpia todos los nodos hijos de un contenedor.
 */
export function clearChildren(element) {
  if (!element) return;
  while (element.firstChild) {
    element.removeChild(element.firstChild);
  }
}

/**
 * Activa o desactiva el estado de carga de un botón, evitando envíos repetidos.
 */
export function setButtonLoading(button, isLoading, loadingText = "Procesando...") {
  if (!button) return;
  if (isLoading) {
    if (!button.dataset.originalLabel) {
      button.dataset.originalLabel = button.textContent || "";
    }
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    clearChildren(button);

    const spinner = document.createElement("span");
    spinner.className = "spinner";
    spinner.setAttribute("aria-hidden", "true");

    const label = document.createElement("span");
    label.textContent = loadingText;

    button.append(spinner, label);
  } else {
    button.disabled = false;
    button.removeAttribute("aria-busy");
    clearChildren(button);
    button.textContent = button.dataset.originalLabel || "Continuar";
  }
}

/**
 * Muestra una alerta accesible dentro de un contenedor específico.
 */
export function showInlineAlert(container, message, type = "error") {
  if (!container) return;
  clearChildren(container);
  container.className = `alert alert-${type}`;
  container.setAttribute("role", "alert");
  container.hidden = false;

  const textSpan = document.createElement("span");
  textSpan.textContent = message;
  container.appendChild(textSpan);
}

export function hideInlineAlert(container) {
  if (!container) return;
  clearChildren(container);
  container.hidden = true;
}

export { createElement };

/**
 * Muestra una notificación flotante accesible (Toast) para eventos del sistema.
 */
export function showToast(firstArg = {}, secondArg = "info", thirdArg = 4500) {
  let type = "info";
  let title = "Aviso del sistema";
  let message = "";
  let duration = 4500;

  if (typeof firstArg === "string") {
    message = firstArg;
    type = typeof secondArg === "string" ? secondArg : "info";
    title = type === "success" ? "Operación exitosa" : type === "error" ? "Error" : "Aviso del sistema";
    duration = typeof thirdArg === "number" ? thirdArg : 4500;
  } else if (typeof firstArg === "object" && firstArg !== null) {
    type = firstArg.type || "info";
    title = firstArg.title || (type === "success" ? "Operación exitosa" : type === "error" ? "Error" : "Aviso del sistema");
    message = firstArg.message || "";
    duration = typeof firstArg.duration === "number" ? firstArg.duration : 4500;
  }
  let container = document.getElementById("toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "toast-container";
    container.className = "toast-container";
    container.setAttribute("aria-live", "polite");
    container.setAttribute("aria-atomic", "false");
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.setAttribute("role", type === "error" ? "alert" : "status");

  const body = document.createElement("div");
  body.className = "toast-body";

  const titleEl = document.createElement("strong");
  titleEl.className = "toast-title";
  titleEl.textContent = title;

  const msgEl = document.createElement("p");
  msgEl.className = "toast-message";
  msgEl.textContent = message;

  body.append(titleEl, msgEl);

  const closeBtn = document.createElement("button");
  closeBtn.type = "button";
  closeBtn.className = "toast-close";
  closeBtn.setAttribute("aria-label", "Cerrar notificación");
  closeBtn.textContent = "×";
  closeBtn.addEventListener("click", () => toast.remove());

  toast.append(body, closeBtn);
  container.appendChild(toast);

  if (duration > 0) {
    window.setTimeout(() => {
      if (toast.isConnected) {
        toast.remove();
      }
    }, duration);
  }
}

/**
 * Muestra un cuadro de diálogo modal accesible de confirmación.
 */
export function confirmDialog({
  title = "Confirmar operación",
  message = "¿Está seguro de continuar?",
  confirmText = "Confirmar",
  cancelText = "Cancelar",
  danger = false
} = {}) {
  return new Promise((resolve) => {
    const backdrop = document.createElement("div");
    backdrop.className = "modal-backdrop";
    backdrop.setAttribute("role", "presentation");

    const modal = document.createElement("div");
    modal.className = "modal";
    modal.setAttribute("role", "dialog");
    modal.setAttribute("aria-modal", "true");
    modal.setAttribute("aria-labelledby", "modal-dialog-title");

    const header = document.createElement("div");
    header.className = "modal-header";

    const titleEl = document.createElement("h2");
    titleEl.id = "modal-dialog-title";
    titleEl.className = "modal-title";
    titleEl.textContent = title;

    header.appendChild(titleEl);

    const body = document.createElement("div");
    body.className = "modal-body";
    const msgP = document.createElement("p");
    msgP.textContent = message;
    body.appendChild(msgP);

    const footer = document.createElement("div");
    footer.className = "modal-footer";

    const cancelBtn = document.createElement("button");
    cancelBtn.type = "button";
    cancelBtn.className = "btn btn-secondary";
    cancelBtn.textContent = cancelText;

    const confirmBtn = document.createElement("button");
    confirmBtn.type = "button";
    confirmBtn.className = `btn ${danger ? "btn-danger" : "btn-primary"}`;
    confirmBtn.textContent = confirmText;

    const close = (result) => {
      document.removeEventListener("keydown", onKeyDown);
      backdrop.remove();
      resolve(result);
    };

    const onKeyDown = (e) => {
      if (e.key === "Escape") {
        close(false);
      }
    };

    cancelBtn.addEventListener("click", () => close(false));
    confirmBtn.addEventListener("click", () => close(true));
    backdrop.addEventListener("click", (e) => {
      if (e.target === backdrop) close(false);
    });

    document.addEventListener("keydown", onKeyDown);

    footer.append(cancelBtn, confirmBtn);
    modal.append(header, body, footer);
    backdrop.appendChild(modal);
    document.body.appendChild(backdrop);
    confirmBtn.focus();
  });
}
