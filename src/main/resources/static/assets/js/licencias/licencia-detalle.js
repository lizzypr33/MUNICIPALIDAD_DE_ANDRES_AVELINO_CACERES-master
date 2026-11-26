/**
 * Módulo de Detalle Integral de Licencia de Funcionamiento (PROMPT 13 - Secciones 9, 10, 12 y 13)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { openModal } from "../components/modal.js";
import { confirmAction } from "../components/confirm-dialog.js";
import {
  obtenerLicencia,
  descargarCertificadoLicencia,
  suspenderLicencia,
  reactivarLicencia,
  revocarLicencia,
  listarHistorialLicencia
} from "./licencias-api.js";

function formatDateTime(isoString) {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return String(isoString);
  return date.toLocaleString("es-PE", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function formatDate(isoDate) {
  if (!isoDate) return "—";
  const parts = String(isoDate).split("-");
  if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`;
  return String(isoDate);
}

function createLicenciaEstadoBadge(estado) {
  const clean = String(estado || "VIGENTE").toUpperCase();
  const cls =
    clean === "VIGENTE"
      ? "badge--success"
      : clean === "SUSPENDIDA"
        ? "badge--warning"
        : "badge--danger";
  return createElement("span", {
    className: `badge ${cls}`,
    textContent: clean
  });
}

export async function renderLicenciaDetalle({
  container,
  licenciaId,
  currentUser = null,
  onBack = null,
  onNavigateToSolicitud = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  container.appendChild(
    createElement("div", {
      className: "loading-state",
      textContent: "Consultando datos de la licencia de funcionamiento..."
    })
  );

  let licencia = null;

  async function reloadAll() {
    try {
      licencia = await obtenerLicencia(licenciaId);
      renderView();
    } catch (err) {
      clearChildren(container);
      container.appendChild(
        createElement("div", {
          className: "alert alert--danger",
          textContent: `Error al cargar la licencia: ${err.message || "No encontrada"}`
        })
      );
    }
  }

  function renderView() {
    clearChildren(container);

    const isSupervisorOrAdmin =
      hasRole(currentUser, ROLES.ADMINISTRADOR) || hasRole(currentUser, ROLES.SUPERVISOR);

    // 1. Barra de navegación superior
    const topBar = createElement("div", {
      className: "table-actions",
      attrs: { style: "justify-content: space-between; margin-bottom: 1.25rem;" }
    });

    const backBtn = createElement("button", {
      type: "button",
      className: "btn btn-secondary",
      textContent: "← Volver a Licencias"
    });
    if (typeof onBack === "function") {
      backBtn.addEventListener("click", onBack);
    }
    topBar.appendChild(backBtn);

    const rightTopBtns = createElement("div", { className: "table-actions", attrs: { style: "gap: 0.5rem;" } });
    const refreshBtn = createElement("button", {
      type: "button",
      className: "btn btn-secondary",
      textContent: "Actualizar"
    });
    refreshBtn.addEventListener("click", reloadAll);
    rightTopBtns.appendChild(refreshBtn);

    if (licencia.solicitudId && typeof onNavigateToSolicitud === "function") {
      const expBtn = createElement("button", {
        type: "button",
        className: "btn btn-secondary",
        textContent: "Ver Expediente"
      });
      expBtn.addEventListener("click", () => onNavigateToSolicitud(licencia.solicitudId));
      rightTopBtns.appendChild(expBtn);
    }

    topBar.appendChild(rightTopBtns);
    container.appendChild(topBar);

    // 2. Encabezado principal
    const headerBox = createElement("div", {
      className: "expediente-header",
      attrs: { style: "margin-bottom: 1.5rem;" }
    });

    const titleRow = createElement("div", {
      className: "table-actions",
      attrs: { style: "gap: 0.75rem; align-items: center;" }
    });
    titleRow.appendChild(
      createElement("h2", {
        className: "expediente-header__number",
        textContent: `LICENCIA N.º ${licencia.codigoLicencia}`
      })
    );
    titleRow.appendChild(createLicenciaEstadoBadge(licencia.estado));
    headerBox.appendChild(titleRow);

    const subMeta = createElement("div", {
      className: "expediente-header__meta",
      textContent: `Expediente: ${licencia.numeroExpediente || `Solicitud #${licencia.solicitudId}`} • Emitida: ${formatDate(licencia.fechaEmision)} • Vigencia: ${licencia.fechaVencimiento ? formatDate(licencia.fechaVencimiento) : "Indeterminada"}`
    });
    headerBox.appendChild(subMeta);
    container.appendChild(headerBox);

    // 3. Barra de acciones operativas
    const actionsBar = createElement("div", { className: "expediente-actions-bar" });

    // Descargar Certificado PDF
    const pdfBtn = createElement("button", {
      type: "button",
      className: "btn btn-primary",
      textContent: "📄 Descargar Certificado PDF"
    });
    pdfBtn.addEventListener("click", async () => {
      try {
        pdfBtn.disabled = true;
        await descargarCertificadoLicencia(licencia.id, licencia.codigoLicencia);
        showToast("Certificado PDF descargado correctamente.", "success");
      } catch (err) {
        showToast(err.message || "Error al descargar certificado.", "error");
      } finally {
        pdfBtn.disabled = false;
      }
    });
    actionsBar.appendChild(pdfBtn);

    // Verificar QR público
    const qrBtn = createElement("button", {
      type: "button",
      className: "btn btn-secondary",
      textContent: "🔍 Verificar Autenticidad (QR)"
    });
    qrBtn.addEventListener("click", () => {
      if (licencia.tokenVerificacion) {
        window.open(`/pages/verificar-licencia.html?token=${encodeURIComponent(licencia.tokenVerificacion)}`, "_blank");
      } else {
        showToast("No se registra token de verificación QR para esta licencia.", "warning");
      }
    });
    actionsBar.appendChild(qrBtn);

    // Suspender Licencia (Solo ADMINISTRADOR o SUPERVISOR)
    if (isSupervisorOrAdmin && licencia.estado === "VIGENTE") {
      const suspBtn = createElement("button", {
        type: "button",
        className: "btn btn--warning",
        textContent: "Suspender Licencia"
      });
      suspBtn.addEventListener("click", () => openCambiarEstadoModal("suspender"));
      actionsBar.appendChild(suspBtn);
    }

    // Reactivar Licencia (Solo ADMINISTRADOR o SUPERVISOR cuando está SUSPENDIDA)
    if (isSupervisorOrAdmin && licencia.estado === "SUSPENDIDA") {
      const reactBtn = createElement("button", {
        type: "button",
        className: "btn btn--success",
        textContent: "Reactivar Licencia"
      });
      reactBtn.addEventListener("click", () => openCambiarEstadoModal("reactivar"));
      actionsBar.appendChild(reactBtn);
    }

    // Revocar Licencia (Solo ADMINISTRADOR o SUPERVISOR)
    if (isSupervisorOrAdmin && (licencia.estado === "VIGENTE" || licencia.estado === "SUSPENDIDA")) {
      const revBtn = createElement("button", {
        type: "button",
        className: "btn btn--danger",
        textContent: "Revocar Licencia"
      });
      revBtn.addEventListener("click", () => openCambiarEstadoModal("revocar"));
      actionsBar.appendChild(revBtn);
    }

    container.appendChild(actionsBar);

    // 4. Tarjeta Oficial de Certificado (Vista Previa Visual del Título)
    const certCard = createElement("div", { className: "licencia-cert-preview" });

    const certHeader = createElement("div", { className: "licencia-cert-header" });
    certHeader.appendChild(
      createElement("div", {
        attrs: { style: "font-size: 0.8rem; font-weight: 700; color: var(--color-primary); text-transform: uppercase;" },
        textContent: "Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray"
      })
    );
    certHeader.appendChild(
      createElement("h3", { className: "licencia-cert-title", textContent: "Certificado de Licencia de Funcionamiento" })
    );
    certHeader.appendChild(
      createElement("div", { className: "licencia-cert-code", textContent: licencia.codigoLicencia })
    );
    certCard.appendChild(certHeader);

    const certGrid = createElement("div", { className: "licencia-cert-grid" });

    const certFields = [
      ["Titular / Razón Social", licencia.nombreTitular || licencia.razonSocial || "—"],
      ["Documento / RUC", licencia.ruc || licencia.numeroDocumentoTitular || "—"],
      ["Nombre Comercial", licencia.nombreComercial || "—"],
      ["Giro del Negocio", licencia.giroNegocio || "—"],
      ["Dirección del Local", licencia.direccionEstablecimiento || "—"],
      ["Área Autorizada", licencia.areaMetrosCuadrados ? `${licencia.areaMetrosCuadrados} m²` : "—"],
      ["Tipo de Trámite", licencia.nombreTipoTramite ? `${licencia.codigoTipoTramite} - ${licencia.nombreTipoTramite}` : "—"],
      ["Fecha de Emisión", formatDate(licencia.fechaEmision)],
      ["Fecha de Vencimiento", licencia.fechaVencimiento ? formatDate(licencia.fechaVencimiento) : "Indeterminada (Definitiva)"],
      ["Funcionario Emisor", licencia.nombreUsuarioEmisor || "Municipalidad Distrital"]
    ];

    for (const [k, v] of certFields) {
      const it = createElement("div", { className: "licencia-cert-item" });
      it.appendChild(createElement("div", { className: "licencia-cert-item__label", textContent: k }));
      it.appendChild(createElement("div", { className: "licencia-cert-item__val", textContent: v }));
      certGrid.appendChild(it);
    }
    certCard.appendChild(certGrid);

    // Sección inferior del certificado: QR y Token
    const qrSection = createElement("div", { className: "licencia-qr-section" });
    const qrInfo = createElement("div");
    qrInfo.appendChild(
      createElement("strong", { textContent: "Verificación Digital de Autenticidad" })
    );
    qrInfo.appendChild(
      createElement("p", {
        attrs: { style: "font-size: 0.8rem; color: var(--color-text-secondary); margin: 0.25rem 0 0 0;" },
        textContent: `Token QR: ${licencia.tokenVerificacion || "No emitido"} • URL de verificación pública: /pages/verificar-licencia.html?token=${licencia.tokenVerificacion || ""}`
      })
    );
    qrSection.appendChild(qrInfo);

    const qrBox = createElement("div", { className: "licencia-qr-box", textContent: "CÓDIGO QR OFICIAL" });
    qrSection.appendChild(qrBox);

    certCard.appendChild(qrSection);
    container.appendChild(certCard);

    // 5. Historial de Estados de la Licencia
    const histCard = createElement("div", { className: "card" });
    const histHeader = createElement("div", { className: "card-header" });
    histHeader.appendChild(
      createElement("h3", { className: "card-title", textContent: "Historial de Cambios de Estado de la Licencia" })
    );
    histCard.appendChild(histHeader);

    const histBody = createElement("div", { className: "card-body" });
    renderHistorialTimeline(histBody, licencia.historial || []);
    histCard.appendChild(histBody);
    container.appendChild(histCard);
  }

  function renderHistorialTimeline(mount, historialList = []) {
    clearChildren(mount);
    if (!Array.isArray(historialList) || historialList.length === 0) {
      mount.appendChild(
        createElement("div", { className: "empty-state", textContent: "No se registran transiciones de estado posteriores a la emisión." })
      );
      return;
    }

    const list = createElement("div", { className: "timeline-list" });

    for (const h of historialList) {
      const item = createElement("div", { className: "timeline-item" });
      const marker = createElement("div", { className: "timeline-marker", textContent: "🏛️" });
      const content = createElement("div", { className: "timeline-content" });

      const header = createElement("div", { className: "timeline-header" });
      const title = createElement("div", { className: "timeline-title" });
      title.textContent = `${h.estadoAnterior || "INICIAL"} → ${h.estadoNuevo}`;
      const date = createElement("time", { className: "timeline-date", textContent: formatDateTime(h.fechaCambio) });
      header.append(title, date);

      const actor = createElement("div", {
        className: "timeline-actor",
        textContent: `Usuario Responsable: ${h.nombreUsuarioResponsable || "Sistema Municipal"}`
      });
      content.append(header, actor);

      if (h.motivo) {
        content.appendChild(
          createElement("div", { className: "timeline-observations", textContent: `Motivo: ${h.motivo}` })
        );
      }

      item.append(marker, content);
      list.appendChild(item);
    }

    mount.appendChild(list);
  }

  // Modal para suspender, reactivar o revocar
  function openCambiarEstadoModal(actionType) {
    const isSusp = actionType === "suspender";
    const isReact = actionType === "reactivar";
    const isRev = actionType === "revocar";

    const titleText = isSusp
      ? "Suspender Licencia de Funcionamiento"
      : isReact
        ? "Reactivar Licencia de Funcionamiento"
        : "Revocar Licencia de Funcionamiento";

    const form = createElement("form", { className: "modal-form" });
    const motGroup = createElement("div", { className: "form-group" });
    motGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Motivo administrativo obligatorio *" })
    );
    const motText = createElement("textarea", {
      className: "form-control",
      attrs: { rows: "3", placeholder: "Fundamento legal, acta o resolución..." }
    });
    motGroup.appendChild(motText);

    const obsGroup = createElement("div", { className: "form-group" });
    obsGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Observaciones adicionales (opcional)" })
    );
    const obsText = createElement("textarea", { className: "form-control", attrs: { rows: "2" } });
    obsGroup.appendChild(obsText);

    form.append(motGroup, obsGroup);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", {
      type: "button",
      className: `btn ${isRev ? "btn--danger" : isSusp ? "btn--warning" : "btn--success"}`,
      textContent: titleText
    });

    const modal = openModal({
      title: titleText,
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const motivo = motText.value.trim();
      if (!motivo) {
        showToast("El motivo administrativo es estrictamente obligatorio.", "warning");
        return;
      }

      try {
        submitBtn.disabled = true;
        const payload = { motivo, observaciones: obsText.value.trim() || null };

        if (isSusp) {
          await suspenderLicencia(licencia.id, payload);
          showToast("Licencia suspendida exitosamente.", "success");
        } else if (isReact) {
          await reactivarLicencia(licencia.id, payload);
          showToast("Licencia reactivada exitosamente.", "success");
        } else if (isRev) {
          await revocarLicencia(licencia.id, payload);
          showToast("Licencia revocada definitivamente.", "success");
        }

        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al actualizar estado de la licencia.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  await reloadAll();
}
