/**
 * Adaptador de utilidades de alertas y toasts para módulos frontend (PROMPT 12, 13)
 */
import {
  showToast as showToastUi,
  showInlineAlert,
  hideInlineAlert,
  confirmDialog
} from "./ui.js";
import { openConfirmDialog } from "./components/confirm-dialog.js";

/**
 * Muestra una notificación flotante accesible (Toast).
 * Admite tanto llamada posicional: showToast(mensaje, tipo, duracion)
 * como por objeto: showToast({ type, title, message, duration })
 */
export function showToast(firstArg = {}, secondArg = "info", thirdArg = 4500) {
  if (typeof firstArg === "object" && firstArg !== null) {
    return showToastUi(firstArg);
  }
  const message = typeof firstArg === "string" ? firstArg : "";
  const type = typeof secondArg === "string" ? secondArg : "info";
  const duration = typeof thirdArg === "number" ? thirdArg : 4500;
  return showToastUi({
    message,
    type,
    title: type === "success" ? "Operación exitosa" : type === "error" ? "Error" : "Aviso del sistema",
    duration
  });
}

export { showInlineAlert, hideInlineAlert, confirmDialog };
export const confirmAction = openConfirmDialog;
