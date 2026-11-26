/**
 * Componente reutilizable de Ventana Modal Accesible (PROMPT 11 - Secciones 18, 19 y 24)
 */
import { clearChildren } from "../ui.js";

let modalCounter = 0;

/**
 * Abre un diálogo modal accesible con gestión de foco y soporte de tecla Escape.
 *
 * @param {Object} options
 * @param {string} options.title Título principal del modal
 * @param {string} [options.subtitle] Subtítulo descriptivo opcional
 * @param {"md"|"lg"|"xl"} [options.size="lg"] Tamaño del modal
 * @param {(bodyEl: HTMLElement, api: { close: () => void, setBusy: (busy: boolean) => void }) => void} options.renderBody
 * @param {(footerEl: HTMLElement, api: { close: () => void, setBusy: (busy: boolean) => void }) => void} [options.renderFooter]
 * @param {() => void} [options.onClose]
 */
export function openModal({
  title = "Detalle del registro",
  subtitle = "",
  size = "lg",
  renderBody,
  renderFooter,
  onClose
} = {}) {
  modalCounter += 1;
  const titleId = `municipal-modal-title-${modalCounter}`;
  const subtitleId = `municipal-modal-subtitle-${modalCounter}`;
  const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;

  let isBusy = false;

  const backdrop = document.createElement("div");
  backdrop.className = "modal-backdrop";
  backdrop.setAttribute("role", "presentation");

  const modal = document.createElement("div");
  modal.className = `modal modal-${size}`;
  modal.setAttribute("role", "dialog");
  modal.setAttribute("aria-modal", "true");
  modal.setAttribute("aria-labelledby", titleId);
  if (subtitle) {
    modal.setAttribute("aria-describedby", subtitleId);
  }

  // Encabezado
  const header = document.createElement("div");
  header.className = "modal-header";

  const headerMain = document.createElement("div");
  headerMain.className = "modal-header-main";

  const titleEl = document.createElement("h2");
  titleEl.id = titleId;
  titleEl.className = "modal-title";
  titleEl.textContent = title;
  headerMain.appendChild(titleEl);

  if (subtitle) {
    const subEl = document.createElement("p");
    subEl.id = subtitleId;
    subEl.className = "modal-subtitle";
    subEl.textContent = subtitle;
    headerMain.appendChild(subEl);
  }

  const closeBtn = document.createElement("button");
  closeBtn.type = "button";
  closeBtn.className = "modal-close-btn";
  closeBtn.setAttribute("aria-label", "Cerrar ventana modal");
  closeBtn.textContent = "×";

  header.append(headerMain, closeBtn);

  // Cuerpo
  const body = document.createElement("div");
  body.className = "modal-body modal-body-scrollable";

  // Pie
  const footer = document.createElement("div");
  footer.className = "modal-footer";

  const close = () => {
    if (isBusy) return;
    document.removeEventListener("keydown", handleKeyDown);
    if (backdrop.isConnected) {
      backdrop.remove();
    }
    if (previouslyFocused && typeof previouslyFocused.focus === "function") {
      previouslyFocused.focus();
    }
    if (typeof onClose === "function") {
      onClose();
    }
  };

  const setBusy = (busy) => {
    isBusy = Boolean(busy);
    closeBtn.disabled = isBusy;
  };

  const modalApi = Object.freeze({
    close,
    setBusy,
    bodyElement: body,
    footerElement: footer
  });

  if (typeof renderBody === "function") {
    renderBody(body, modalApi);
  }

  if (typeof renderFooter === "function") {
    renderFooter(footer, modalApi);
  } else {
    clearChildren(footer);
    const defaultCloseBtn = document.createElement("button");
    defaultCloseBtn.type = "button";
    defaultCloseBtn.className = "btn btn-secondary";
    defaultCloseBtn.textContent = "Cerrar";
    defaultCloseBtn.addEventListener("click", close);
    footer.appendChild(defaultCloseBtn);
  }

  const handleKeyDown = (event) => {
    if (event.key === "Escape") {
      event.preventDefault();
      close();
      return;
    }
    if (event.key === "Tab") {
      const focusables = modal.querySelectorAll(
        'button:not([disabled]), [href], input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );
      if (focusables.length === 0) return;
      const first = focusables[0];
      const last = focusables[focusables.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  };

  closeBtn.addEventListener("click", close);
  backdrop.addEventListener("click", (event) => {
    if (event.target === backdrop) {
      close();
    }
  });

  document.addEventListener("keydown", handleKeyDown);

  modal.append(header, body, footer);
  backdrop.appendChild(modal);
  document.body.appendChild(backdrop);

  // Enfocar primer campo interactivo o botón de cierre
  window.setTimeout(() => {
    const firstInput = body.querySelector(
      'input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), button:not([disabled])'
    );
    if (firstInput instanceof HTMLElement) {
      firstInput.focus();
    } else {
      closeBtn.focus();
    }
  }, 20);

  return modalApi;
}
