/**
 * Componente accesible de Diálogos de Confirmación (PROMPT 11 - Sección 19)
 * Para confirmar desactivaciones, activaciones y cambios sensibles de seguridad.
 */
import { clearChildren, setButtonLoading } from "../ui.js";
import { openModal } from "./modal.js";

/**
 * Muestra un modal accesible de confirmación antes de ejecutar una operación importante.
 *
 * @param {Object} options
 * @param {string} options.title Título del diálogo
 * @param {string} options.message Mensaje principal de confirmación
 * @param {string} [options.detail] Detalle adicional o advertencia institucional
 * @param {string} [options.confirmText="Confirmar"] Etiqueta del botón de confirmación
 * @param {string} [options.cancelText="Cancelar"] Etiqueta del botón de cancelación
 * @param {"danger"|"primary"} [options.variant="danger"] Variante visual del botón principal
 * @param {() => Promise<void>|void} [options.onConfirm] Callback opcional asíncrono antes de cerrar
 * @returns {Promise<boolean>} Resuelve true si se confirmó, false si se canceló
 */
export function openConfirmDialog({
  title = "Confirmar operación administrativa",
  message = "¿Está seguro de continuar con esta operación?",
  detail = "",
  confirmText = "Confirmar",
  cancelText = "Cancelar",
  variant = "danger",
  onConfirm = null
} = {}) {
  return new Promise((resolve) => {
    let resolved = false;
    let confirmBtnRef = null;

    openModal({
      title,
      size: "md",
      onClose: () => {
        if (!resolved) {
          resolved = true;
          resolve(false);
        }
      },
      renderBody(bodyEl) {
        const msgEl = document.createElement("p");
        msgEl.style.margin = "0 0 0.75rem 0";
        msgEl.style.fontSize = "0.93rem";
        msgEl.style.color = "var(--color-text-primary)";
        msgEl.textContent = message;
        bodyEl.appendChild(msgEl);

        if (detail) {
          const detailBox = document.createElement("div");
          detailBox.className = "legal-disclaimer-box";
          detailBox.textContent = detail;
          bodyEl.appendChild(detailBox);
        }
      },
      renderFooter(footerEl, modalApi) {
        clearChildren(footerEl);

        const cancelBtn = document.createElement("button");
        cancelBtn.type = "button";
        cancelBtn.className = "btn btn-secondary";
        cancelBtn.textContent = cancelText;

        const confirmBtn = document.createElement("button");
        confirmBtn.type = "button";
        confirmBtn.className = `btn ${variant === "danger" ? "btn-danger" : "btn-primary"}`;
        confirmBtn.textContent = confirmText;
        confirmBtnRef = confirmBtn;

        cancelBtn.addEventListener("click", () => {
          modalApi.close();
        });

        confirmBtn.addEventListener("click", async () => {
          if (typeof onConfirm === "function") {
            modalApi.setBusy(true);
            cancelBtn.disabled = true;
            setButtonLoading(confirmBtn, true, "Procesando...");
            try {
              await onConfirm();
              resolved = true;
              modalApi.setBusy(false);
              modalApi.close();
              resolve(true);
            } catch (_err) {
              modalApi.setBusy(false);
              cancelBtn.disabled = false;
              setButtonLoading(confirmBtn, false);
            }
          } else {
            resolved = true;
            modalApi.close();
            resolve(true);
          }
        });

        footerEl.append(cancelBtn, confirmBtn);
      }
    });

    window.setTimeout(() => {
      if (confirmBtnRef instanceof HTMLElement) {
        confirmBtnRef.focus();
      }
    }, 30);
  });
}

export const confirmAction = openConfirmDialog;
