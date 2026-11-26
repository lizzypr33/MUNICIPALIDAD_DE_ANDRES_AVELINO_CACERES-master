/**
 * Módulo de Verificación Pública de Licencias mediante Código QR (PROMPT 13 - Sección 11)
 * Funciona de manera autónoma sin requerir sesión autenticada.
 */
import { createElement, clearChildren } from "../dom.js";
import { verificarLicenciaPublica } from "./licencias-api.js";

function formatDateTime(isoString) {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return String(isoString);
  return date.toLocaleString("es-PE", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  });
}

function formatDate(isoString) {
  if (!isoString) return "—";
  const parts = String(isoString).split("-");
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return String(isoString);
}

/**
 * Inicializa la pantalla pública de verificación de licencias.
 */
export function initVerificacionPublicaPage() {
  const form = document.getElementById("public-verify-form");
  const tokenInput = document.getElementById("verify-token-input");
  const submitBtn = document.getElementById("verify-submit-btn");
  const resultMount = document.getElementById("verify-result-mount");

  if (!form || !tokenInput || !resultMount) return;

  async function performVerification(token) {
    const cleanToken = String(token || "").trim();
    if (!cleanToken) {
      renderError("Por favor ingrese el identificador o token de verificación.");
      return;
    }

    clearChildren(resultMount);
    resultMount.appendChild(
      createElement("div", {
        className: "loading-state",
        textContent: "Consultando autenticidad en la base de datos municipal..."
      })
    );

    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = "Verificando...";
    }

    try {
      const data = await verificarLicenciaPublica(cleanToken);
      renderResult(data);
    } catch (err) {
      renderError(err.message || "No se pudo verificar la licencia.");
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = "Verificar Autenticidad";
      }
    }
  }

  function renderResult(data) {
    clearChildren(resultMount);

    const isVigente = data.vigente && data.estado === "VIGENTE";
    const statusClass = isVigente ? "is-vigente" : "is-invalida";

    const card = createElement("div", {
      className: `card public-verify-result-card ${statusClass}`
    });

    const body = createElement("div", { className: "card-body" });

    // Encabezado del resultado
    const header = createElement("div", { className: "public-verify-result-header" });

    const titleGroup = createElement("div");
    titleGroup.appendChild(
      createElement("h3", {
        attrs: { style: "font-size: 1.15rem; font-weight: 800; color: var(--color-primary-dark); margin: 0 0 0.25rem 0;" },
        textContent: `LICENCIA N.º ${data.codigoLicencia || "NO DETERMINADO"}`
      })
    );
    titleGroup.appendChild(
      createElement("p", {
        attrs: { style: "font-size: 0.85rem; color: var(--color-text-secondary); margin: 0;" },
        textContent: `Fecha de consulta oficial: ${formatDateTime(data.fechaConsulta)}`
      })
    );
    header.appendChild(titleGroup);

    // Badge de estado
    const badge = createElement("span", {
      className: `public-verify-badge-status ${
        data.estado === "VIGENTE"
          ? "badge--success"
          : data.estado === "SUSPENDIDA"
            ? "badge--warning"
            : "badge--danger"
      }`,
      textContent: data.estado || "NO VIGENTE"
    });
    header.appendChild(badge);
    body.appendChild(header);

    // Mensaje de autenticidad institucional
    const alertDiv = createElement("div", {
      className: `alert ${isVigente ? "alert--success" : "alert--warning"}`,
      attrs: { style: "margin-bottom: 1.5rem;" },
      textContent: data.resultadoVerificacion || (isVigente ? "Documento municipal auténtico y vigente." : "Atención: La licencia no se encuentra vigente.")
    });
    body.appendChild(alertDiv);

    // Grilla de datos públicos del establecimiento
    const grid = createElement("div", { className: "licencia-cert-grid" });

    const fields = [
      ["Establecimiento Comercial", data.nombreComercial || "—"],
      ["Dirección Autorizada", data.direccionEstablecimiento || "—"],
      ["Giro de Negocio / Actividad", data.giroNegocio || "—"],
      ["Fecha de Emisión", formatDate(data.fechaEmision)],
      ["Vigencia de la Licencia", data.vigencia || (data.fechaVencimiento ? formatDate(data.fechaVencimiento) : "Indeterminada")]
    ];

    for (const [k, v] of fields) {
      const item = createElement("div", { className: "licencia-cert-item" });
      item.appendChild(createElement("div", { className: "licencia-cert-item__label", textContent: k }));
      item.appendChild(createElement("div", { className: "licencia-cert-item__val", textContent: v }));
      grid.appendChild(item);
    }

    body.appendChild(grid);
    card.appendChild(body);
    resultMount.appendChild(card);
  }

  function renderError(message) {
    clearChildren(resultMount);
    const card = createElement("div", { className: "card public-verify-result-card is-invalida" });
    const body = createElement("div", { className: "card-body" });
    body.appendChild(
      createElement("div", {
        className: "alert alert--danger",
        textContent: message
      })
    );
    card.appendChild(body);
    resultMount.appendChild(card);
  }

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    performVerification(tokenInput.value);
  });

  // Si viene con parámetro ?token= en la URL (ej. escaneo de QR)
  const params = new URLSearchParams(window.location.search);
  const urlToken = params.get("token");
  if (urlToken) {
    tokenInput.value = urlToken;
    performVerification(urlToken);
  }
}
