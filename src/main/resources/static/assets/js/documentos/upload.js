/**
 * Componente de Validación y Carga Segura de Archivos (PROMPT 12 - Secciones 14, 27 y 29)
 * Valida formato, extensión permitida (.pdf, .jpg, .jpeg, .png) y tamaño máximo (10 MB)
 * antes de enviar mediante FormData al backend.
 */
import { createElement } from "../dom.js";

export const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB (coincide con DocumentoValidator en backend)

export const ALLOWED_EXTENSIONS = Object.freeze([".pdf", ".jpg", ".jpeg", ".png"]);

export const ALLOWED_MIME_TYPES = Object.freeze([
  "application/pdf",
  "image/jpeg",
  "image/jpg",
  "image/png"
]);

export const FORBIDDEN_EXTENSIONS = Object.freeze([
  ".exe",
  ".bat",
  ".cmd",
  ".sh",
  ".js",
  ".msi",
  ".ps1",
  ".vbs",
  ".jar",
  ".com",
  ".scr",
  ".php",
  ".jsp",
  ".html",
  ".htm",
  ".svg"
]);

/**
 * Formatea un tamaño en bytes a una cadena legible (KB / MB).
 */
export function formatFileSize(bytes) {
  const num = Number(bytes);
  if (!Number.isFinite(num) || num < 0) {
    return "0 KB";
  }
  if (num < 1024) {
    return `${num} B`;
  }
  const kb = num / 1024;
  if (kb < 1024) {
    return `${kb.toFixed(1)} KB`;
  }
  const mb = kb / 1024;
  return `${mb.toFixed(2)} MB`;
}

/**
 * Extrae la extensión en minúsculas de un nombre de archivo.
 */
export function extractFileExtension(fileName = "") {
  const cleanName = String(fileName || "").trim().toLowerCase();
  const lastDot = cleanName.lastIndexOf(".");
  if (lastDot <= 0 || lastDot === cleanName.length - 1) {
    return "";
  }
  return cleanName.substring(lastDot);
}

/**
 * Valida un objeto File según las reglas municipales de seguridad documental.
 * Retorna { valid: boolean, error: string | null }.
 */
export function validateMunicipalFile(file) {
  if (!file || typeof file !== "object") {
    return { valid: false, error: "Debe seleccionar un archivo válido para adjuntar." };
  }

  const fileName = String(file.name || "").trim();
  if (!fileName) {
    return { valid: false, error: "El archivo seleccionado no tiene un nombre válido." };
  }

  if (!Number.isFinite(file.size) || file.size <= 0) {
    return { valid: false, error: "El archivo seleccionado está vacío (0 bytes)." };
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    return {
      valid: false,
      error: `El archivo "${fileName}" (${formatFileSize(file.size)}) supera el límite máximo permitido de 10 MB.`
    };
  }

  const lowerName = fileName.toLowerCase();
  for (const forbidden of FORBIDDEN_EXTENSIONS) {
    if (lowerName.endsWith(forbidden) || lowerName.includes(`${forbidden}.`)) {
      return {
        valid: false,
        error: `Por razones de seguridad municipal, no se permiten archivos ejecutables o scripts (${forbidden}).`
      };
    }
  }

  const ext = extractFileExtension(fileName);
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    return {
      valid: false,
      error: `La extensión "${ext || "sin extensión"}" no está permitida. Solo se aceptan archivos PDF, JPG, JPEG o PNG.`
    };
  }

  if (file.type && !ALLOWED_MIME_TYPES.includes(file.type.toLowerCase())) {
    return {
      valid: false,
      error: `El tipo de archivo (${file.type}) no corresponde a un formato PDF, JPG o PNG válido.`
    };
  }

  return { valid: true, error: null };
}

/**
 * Crea una zona interactiva de selección o arrastre de archivo con soporte de teclado y validación inmediata.
 */
export function createUploadDropzone({
  idPrefix = "upload-doc",
  labelText = "Haga clic para seleccionar o arrastre un archivo aquí",
  hintText = "Formatos permitidos: PDF, JPG, JPEG, PNG — Tamaño máximo: 10 MB",
  disabled = false,
  onFileSelected = null,
  onValidationError = null
} = {}) {
  const wrapper = createElement("div", { className: "upload-dropzone-wrapper" });

  const fileInput = createElement("input", {
    type: "file",
    id: `${idPrefix}-input`,
    className: "sr-only",
    accept: ".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
  });
  if (disabled) {
    fileInput.disabled = true;
  }

  const dropzone = createElement("div", {
    className: "upload-dropzone",
    attrs: {
      role: "button",
      tabindex: disabled ? "-1" : "0",
      "aria-label": labelText,
      "aria-disabled": disabled ? "true" : "false"
    }
  });

  const titleEl = createElement("div", {
    className: "upload-dropzone__title",
    textContent: labelText
  });

  const hintEl = createElement("div", {
    className: "upload-dropzone__hint",
    textContent: hintText
  });

  const selectedFileEl = createElement("div", {
    className: "upload-dropzone__file",
    attrs: { hidden: "hidden" }
  });

  const errorEl = createElement("p", {
    className: "field-error",
    attrs: { role: "alert", hidden: "hidden" }
  });

  dropzone.append(titleEl, hintEl, selectedFileEl);
  wrapper.append(fileInput, dropzone, errorEl);

  let currentFile = null;

  function setErrorMessage(msg) {
    if (msg) {
      errorEl.textContent = msg;
      errorEl.hidden = false;
    } else {
      errorEl.textContent = "";
      errorEl.hidden = true;
    }
  }

  function handleCandidateFile(file) {
    if (!file) {
      return;
    }
    const validation = validateMunicipalFile(file);
    if (!validation.valid) {
      currentFile = null;
      selectedFileEl.hidden = true;
      selectedFileEl.textContent = "";
      setErrorMessage(validation.error);
      if (typeof onValidationError === "function") {
        onValidationError(validation.error);
      }
      return;
    }

    currentFile = file;
    setErrorMessage(null);
    selectedFileEl.textContent = `Archivo preparado: ${file.name} (${formatFileSize(file.size)})`;
    selectedFileEl.hidden = false;

    if (typeof onFileSelected === "function") {
      onFileSelected(file);
    }
  }

  dropzone.addEventListener("click", () => {
    if (!fileInput.disabled) {
      fileInput.click();
    }
  });

  dropzone.addEventListener("keydown", (event) => {
    if (!fileInput.disabled && (event.key === "Enter" || event.key === " ")) {
      event.preventDefault();
      fileInput.click();
    }
  });

  fileInput.addEventListener("change", () => {
    const file = fileInput.files && fileInput.files[0] ? fileInput.files[0] : null;
    if (file) {
      handleCandidateFile(file);
    }
  });

  dropzone.addEventListener("dragover", (event) => {
    if (fileInput.disabled) return;
    event.preventDefault();
    dropzone.classList.add("is-dragover");
  });

  dropzone.addEventListener("dragleave", () => {
    dropzone.classList.remove("is-dragover");
  });

  dropzone.addEventListener("drop", (event) => {
    dropzone.classList.remove("is-dragover");
    if (fileInput.disabled) return;
    event.preventDefault();
    const file =
      event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files[0]
        ? event.dataTransfer.files[0]
        : null;
    if (file) {
      handleCandidateFile(file);
    }
  });

  return {
    element: wrapper,
    getSelectedFile() {
      return currentFile;
    },
    clear() {
      currentFile = null;
      fileInput.value = "";
      selectedFileEl.textContent = "";
      selectedFileEl.hidden = true;
      setErrorMessage(null);
    },
    setError(message) {
      setErrorMessage(message);
    }
  };
}
