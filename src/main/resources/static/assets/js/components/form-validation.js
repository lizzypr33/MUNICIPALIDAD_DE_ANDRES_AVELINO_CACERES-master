/**
 * Validaciones de Formularios, Reglas de Documentos Peruanos y Selectores Dinámicos Remotos
 * (PROMPT 11 - Secciones 6, 9, 12, 14, 15, 18 y 24)
 */
import { clearChildren } from "../ui.js";

export const TIPO_DOCUMENTO_OPTIONS = Object.freeze([
  { value: "DNI", label: "DNI (8 dígitos)" },
  { value: "RUC", label: "RUC (11 dígitos)" },
  { value: "CARNE_EXTRANJERIA", label: "Carné de Extranjería" },
  { value: "PASAPORTE", label: "Pasaporte" },
  { value: "OTRO", label: "Otro documento" }
]);

const RUC_VALID_PREFIXES = new Set(["10", "15", "17", "20"]);
const RUC_FACTORS = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];

/**
 * Valida un número de RUC peruano de 11 dígitos con prefijo y dígito verificador Módulo 11.
 * Nota: Esta validación verifica únicamente la estructura y dígito verificador;
 * no consulta a SUNAT ni acredita estado activo o condición de habido.
 */
export function validateRucPeruano(rucRaw, required = false) {
  const clean = typeof rucRaw === "string" ? rucRaw.trim() : "";
  if (!clean) {
    return required ? "El número de RUC es obligatorio." : null;
  }
  if (!/^\d{11}$/.test(clean)) {
    return "El RUC debe contener exactamente 11 dígitos numéricos.";
  }
  const prefix = clean.substring(0, 2);
  if (!RUC_VALID_PREFIXES.has(prefix)) {
    return "El RUC debe iniciar con un prefijo válido (10, 15, 17 o 20).";
  }
  let sum = 0;
  for (let i = 0; i < 10; i += 1) {
    sum += Number(clean.charAt(i)) * RUC_FACTORS[i];
  }
  const remainder = 11 - (sum % 11);
  const expectedCheck = remainder === 10 ? 0 : remainder === 11 ? 1 : remainder;
  const actualCheck = Number(clean.charAt(10));
  if (expectedCheck !== actualCheck) {
    return "El RUC ingresado tiene un dígito verificador inválido.";
  }
  return null;
}

/**
 * Valida tipo y número de documento según las reglas exactas de DocumentoIdentidadValidator.java.
 * Nota: La validación de formato de DNI no confirma la identidad en RENIEC.
 */
export function validateDocumentoIdentidad(tipoDocumento, numeroRaw) {
  const tipo = typeof tipoDocumento === "string" ? tipoDocumento.trim().toUpperCase() : "";
  const numero = typeof numeroRaw === "string" ? numeroRaw.trim().toUpperCase() : "";

  if (!tipo) {
    return "Seleccione el tipo de documento de identidad.";
  }
  if (!numero) {
    return "El número de documento es obligatorio.";
  }

  switch (tipo) {
    case "DNI":
      if (!/^\d{8}$/.test(numero)) {
        return "El DNI peruano debe contener exactamente 8 dígitos numéricos.";
      }
      return null;
    case "CARNE_EXTRANJERIA":
      if (!/^[A-Z0-9]{9,12}$/.test(numero)) {
        return "El Carné de Extranjería debe tener entre 9 y 12 caracteres alfanuméricos.";
      }
      return null;
    case "PASAPORTE":
      if (!/^[A-Z0-9]{6,15}$/.test(numero)) {
        return "El Pasaporte debe tener entre 6 y 15 caracteres alfanuméricos.";
      }
      return null;
    case "RUC":
      return validateRucPeruano(numero, true);
    case "OTRO":
      if (!/^[A-Z0-9-]{4,20}$/.test(numero)) {
        return "El número de documento debe tener entre 4 y 20 caracteres válidos.";
      }
      return null;
    default:
      return "Tipo de documento no reconocido.";
  }
}

export function validateRequiredText(value, label, maxLength = 255) {
  const clean = typeof value === "string" ? value.trim() : "";
  if (!clean) {
    return `El campo "${label}" es obligatorio.`;
  }
  if (clean.length > maxLength) {
    return `El campo "${label}" no debe exceder ${maxLength} caracteres.`;
  }
  return null;
}

export function validateUsername(usernameRaw) {
  const clean = typeof usernameRaw === "string" ? usernameRaw.trim() : "";
  if (!clean) {
    return "El nombre de usuario es obligatorio.";
  }
  if (clean.length < 3 || clean.length > 50) {
    return "El nombre de usuario debe tener entre 3 y 50 caracteres.";
  }
  if (!/^[A-Za-z0-9._-]+$/.test(clean)) {
    return "El usuario solo puede contener letras, números, puntos (.), guiones (-) y guiones bajos (_).";
  }
  return null;
}

export function validateEmailFormat(emailRaw, required = false) {
  const clean = typeof emailRaw === "string" ? emailRaw.trim() : "";
  if (!clean) {
    return required ? "El correo electrónico es obligatorio." : null;
  }
  if (clean.length > 150) {
    return "El correo electrónico no debe superar los 150 caracteres.";
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(clean)) {
    return "Ingrese un correo electrónico con formato válido (ejemplo@dominio.gob.pe).";
  }
  return null;
}

export function validatePhoneFormat(phoneRaw, required = false) {
  const clean = typeof phoneRaw === "string" ? phoneRaw.trim() : "";
  if (!clean) {
    return required ? "El teléfono es obligatorio." : null;
  }
  if (clean.length > 20 || !/^[0-9+\-\s()]{6,20}$/.test(clean)) {
    return "Ingrese un teléfono válido (entre 6 y 20 dígitos o signos permitidos).";
  }
  return null;
}

export function validatePasswordPolicy(password, confirmPassword) {
  if (!password) {
    return { field: "password", message: "La contraseña inicial es obligatoria." };
  }
  if (password.length < 12 || password.length > 72) {
    return {
      field: "password",
      message: "Por política de seguridad municipal, la contraseña debe tener entre 12 y 72 caracteres."
    };
  }
  if (!confirmPassword) {
    return { field: "confirmPassword", message: "Debe confirmar la contraseña inicial." };
  }
  if (password !== confirmPassword) {
    return { field: "confirmPassword", message: "Las contraseñas ingresadas no coinciden." };
  }
  return null;
}

export function validatePositiveNumber(rawValue, label, { integer = false, min = 0.01 } = {}) {
  const str = String(rawValue ?? "").trim();
  if (!str) {
    return `El campo "${label}" es obligatorio.`;
  }
  const num = Number(str);
  if (!Number.isFinite(num) || num < min) {
    return `El campo "${label}" debe ser un número mayor o igual a ${min}.`;
  }
  if (integer && !Number.isInteger(num)) {
    return `El campo "${label}" debe ser un número entero.`;
  }
  return null;
}

/**
 * Muestra un mensaje de error accesible vinculado al control del formulario.
 */
export function setFieldError(inputEl, message) {
  if (!inputEl) return;
  inputEl.setAttribute("aria-invalid", "true");
  const group = inputEl.closest(".form-group");
  if (!group) return;

  let errEl = group.querySelector(".field-error-msg");
  if (!errEl) {
    errEl = document.createElement("span");
    errEl.className = "field-error-msg";
    errEl.setAttribute("role", "alert");
    if (inputEl.id) {
      errEl.id = `${inputEl.id}-error`;
      inputEl.setAttribute("aria-describedby", errEl.id);
    }
    group.appendChild(errEl);
  }
  errEl.textContent = message;
  errEl.hidden = false;
}

export function clearFieldError(inputEl) {
  if (!inputEl) return;
  inputEl.removeAttribute("aria-invalid");
  const group = inputEl.closest(".form-group");
  if (!group) return;
  const errEl = group.querySelector(".field-error-msg");
  if (errEl) {
    errEl.textContent = "";
    errEl.hidden = true;
  }
}

export function clearFormErrors(formEl) {
  if (!formEl) return;
  formEl.querySelectorAll("[aria-invalid]").forEach((el) => el.removeAttribute("aria-invalid"));
  formEl.querySelectorAll(".field-error-msg").forEach((el) => {
    el.textContent = "";
    el.hidden = true;
  });
}

/**
 * Crea un grupo de campo de formulario accesible con etiqueta <label>, asterisco obligatorio y ayuda.
 */
export function createFormField({
  id,
  label,
  type = "text",
  required = false,
  maxLength = null,
  placeholder = "",
  hint = "",
  value = "",
  options = [],
  disabled = false,
  step = null,
  min = null,
  fullWidth = false,
  autocomplete = "off"
}) {
  const group = document.createElement("div");
  group.className = `form-group${fullWidth ? " form-col-full" : ""}`;

  const labelEl = document.createElement("label");
  labelEl.className = "form-label";
  labelEl.setAttribute("for", id);
  labelEl.textContent = label;

  if (required) {
    const reqSpan = document.createElement("span");
    reqSpan.className = "required-mark";
    reqSpan.setAttribute("aria-hidden", "true");
    reqSpan.textContent = "*";
    labelEl.appendChild(reqSpan);
  }

  let control;
  if (type === "select") {
    control = document.createElement("select");
    control.className = "form-select";
    for (const opt of options) {
      const optEl = document.createElement("option");
      optEl.value = opt.value ?? "";
      optEl.textContent = opt.label ?? String(opt.value ?? "");
      if (String(opt.value) === String(value)) {
        optEl.selected = true;
      }
      control.appendChild(optEl);
    }
  } else if (type === "textarea") {
    control = document.createElement("textarea");
    control.className = "form-textarea";
    control.value = value ?? "";
    if (placeholder) control.placeholder = placeholder;
  } else {
    control = document.createElement("input");
    control.type = type;
    control.className = "form-control";
    control.value = value ?? "";
    if (placeholder) control.placeholder = placeholder;
    if (autocomplete) control.autocomplete = autocomplete;
    if (step !== null) control.step = String(step);
    if (min !== null) control.min = String(min);
  }

  control.id = id;
  control.required = Boolean(required);
  control.disabled = Boolean(disabled);
  if (maxLength && type !== "select" && type !== "number") {
    control.maxLength = Number(maxLength);
  }

  control.addEventListener("input", () => clearFieldError(control));
  control.addEventListener("change", () => clearFieldError(control));

  group.append(labelEl, control);

  if (hint) {
    const hintEl = document.createElement("span");
    hintEl.className = "form-hint";
    hintEl.textContent = hint;
    group.appendChild(hintEl);
  }

  return { group, control, labelEl };
}

/**
 * Crea un selector dinámico con búsqueda remota paginada (Sección 15).
 * Evita descargar miles de registros en memoria y permite buscar por documento o nombre.
 */
export function createRemoteSearchSelect({
  id,
  label,
  required = false,
  placeholder = "Buscar por documento, nombres o apellidos...",
  hint = "",
  initialSelected = null, // { id, label }
  disabled = false,
  fullWidth = false,
  searchFn // async (query) => Array<{ id, primaryText, secondaryText }>
}) {
  const group = document.createElement("div");
  group.className = `form-group${fullWidth ? " form-col-full" : ""}`;

  const labelEl = document.createElement("label");
  labelEl.className = "form-label";
  labelEl.setAttribute("for", `${id}-search-input`);
  labelEl.textContent = label;
  if (required) {
    const reqSpan = document.createElement("span");
    reqSpan.className = "required-mark";
    reqSpan.setAttribute("aria-hidden", "true");
    reqSpan.textContent = "*";
    labelEl.appendChild(reqSpan);
  }

  const hiddenInput = document.createElement("input");
  hiddenInput.type = "hidden";
  hiddenInput.id = id;
  hiddenInput.value = initialSelected && initialSelected.id ? String(initialSelected.id) : "";

  const box = document.createElement("div");
  box.className = "remote-select-box";

  const selectedArea = document.createElement("div");
  const searchRow = document.createElement("div");
  searchRow.className = "remote-select-search-row";

  const searchInput = document.createElement("input");
  searchInput.type = "search";
  searchInput.id = `${id}-search-input`;
  searchInput.className = "form-control";
  searchInput.placeholder = placeholder;
  searchInput.disabled = disabled;

  const searchBtn = document.createElement("button");
  searchBtn.type = "button";
  searchBtn.className = "btn btn-secondary btn-sm";
  searchBtn.textContent = "Buscar";
  searchBtn.disabled = disabled;

  searchRow.append(searchInput, searchBtn);

  const resultsBox = document.createElement("div");
  resultsBox.className = "remote-select-results";
  resultsBox.hidden = true;

  let currentSelection = initialSelected && initialSelected.id ? { ...initialSelected } : null;

  const renderSelectionState = () => {
    clearChildren(selectedArea);
    if (currentSelection && currentSelection.id) {
      hiddenInput.value = String(currentSelection.id);
      const pill = document.createElement("div");
      pill.className = "remote-select-selected-pill";

      const textSpan = document.createElement("span");
      textSpan.textContent = `Seleccionado: ${currentSelection.label}`;
      pill.appendChild(textSpan);

      if (!disabled) {
        const clearBtn = document.createElement("button");
        clearBtn.type = "button";
        clearBtn.className = "btn-table-action is-danger";
        clearBtn.textContent = "Cambiar / Quitar";
        clearBtn.addEventListener("click", () => {
          currentSelection = null;
          hiddenInput.value = "";
          renderSelectionState();
          searchInput.focus();
          executeSearch(searchInput.value);
        });
        pill.appendChild(clearBtn);
      }

      selectedArea.appendChild(pill);
      searchRow.hidden = true;
      resultsBox.hidden = true;
    } else {
      hiddenInput.value = "";
      searchRow.hidden = Boolean(disabled);
    }
  };

  const executeSearch = async (queryText) => {
    if (disabled) return;
    clearFieldError(searchInput);
    resultsBox.hidden = false;
    clearChildren(resultsBox);

    const loadingDiv = document.createElement("div");
    loadingDiv.className = "remote-select-empty";
    loadingDiv.textContent = "Consultando registros en el servidor...";
    resultsBox.appendChild(loadingDiv);

    try {
      const items = await searchFn((queryText || "").trim());
      clearChildren(resultsBox);
      if (!Array.isArray(items) || items.length === 0) {
        const emptyDiv = document.createElement("div");
        emptyDiv.className = "remote-select-empty";
        emptyDiv.textContent = "No se encontraron registros activos con ese criterio.";
        resultsBox.appendChild(emptyDiv);
        return;
      }

      for (const item of items) {
        const itemBtn = document.createElement("button");
        itemBtn.type = "button";
        itemBtn.className = "remote-select-item";

        const primarySpan = document.createElement("strong");
        primarySpan.textContent = item.primaryText;

        const secondarySpan = document.createElement("span");
        secondarySpan.className = "text-muted";
        secondarySpan.textContent = item.secondaryText || `#${item.id}`;

        itemBtn.append(primarySpan, secondarySpan);
        itemBtn.addEventListener("click", () => {
          currentSelection = {
            id: item.id,
            label: `${item.primaryText}${item.secondaryText ? ` (${item.secondaryText})` : ""}`
          };
          clearFieldError(searchInput);
          renderSelectionState();
        });
        resultsBox.appendChild(itemBtn);
      }
    } catch (err) {
      clearChildren(resultsBox);
      const errDiv = document.createElement("div");
      errDiv.className = "remote-select-empty";
      errDiv.textContent = err.message || "Error al consultar opciones.";
      resultsBox.appendChild(errDiv);
    }
  };

  searchBtn.addEventListener("click", () => executeSearch(searchInput.value));
  searchInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      executeSearch(searchInput.value);
    }
  });
  searchInput.addEventListener("focus", () => {
    if (!currentSelection && resultsBox.hidden) {
      executeSearch(searchInput.value);
    }
  });

  renderSelectionState();

  box.append(hiddenInput, selectedArea, searchRow, resultsBox);
  group.append(labelEl, box);

  if (hint) {
    const hintEl = document.createElement("span");
    hintEl.className = "form-hint";
    hintEl.textContent = hint;
    group.appendChild(hintEl);
  }

  return {
    group,
    control: searchInput,
    getValue() {
      return currentSelection && currentSelection.id ? Number(currentSelection.id) : null;
    },
    setValue(selected) {
      currentSelection = selected && selected.id ? { ...selected } : null;
      renderSelectionState();
    },
    setError(message) {
      setFieldError(searchInput, message);
    }
  };
}
