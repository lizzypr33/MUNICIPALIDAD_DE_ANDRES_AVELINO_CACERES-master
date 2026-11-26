/**
 * Módulo de Detalle Integral de Inspección Técnica (PROMPT 13 - Secciones 19, 20 y 21)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { openModal } from "../components/modal.js";
import { confirmAction } from "../components/confirm-dialog.js";
import {
  obtenerInspeccion,
  iniciarInspeccion,
  registrarResultadoInspeccion,
  cancelarInspeccion
} from "./inspecciones-api.js";
import { openReprogramarInspeccionModal } from "./inspeccion-form.js";

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

function createInspeccionEstadoBadge(estado) {
  const clean = String(estado || "PROGRAMADA").toUpperCase();
  let cls = "badge-neutral";
  if (clean === "PROGRAMADA" || clean === "REPROGRAMADA") cls = "badge-info";
  else if (clean === "EN_PROCESO") cls = "badge-warning";
  else if (clean === "FINALIZADA" || clean === "REALIZADA") cls = "badge-success";
  else if (clean === "CANCELADA") cls = "badge-danger";

  return createElement("span", { className: `badge ${cls}`, textContent: clean });
}

function createInspeccionResultadoBadge(resultado) {
  if (!resultado) {
    return createElement("span", { className: "badge badge-neutral", textContent: "PENDIENTE" });
  }
  const clean = String(resultado).toUpperCase();
  let cls = "badge--resultado-favorable";
  if (clean.includes("OBSERVACIONES")) cls = "badge--resultado-observaciones";
  else if (clean.includes("DESFAVORABLE")) cls = "badge--resultado-desfavorable";

  return createElement("span", { className: `badge ${cls}`, textContent: clean });
}

export async function renderInspeccionDetalle({
  container,
  inspeccionId,
  currentUser = null,
  onBack = null,
  onNavigateToSolicitud = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  container.appendChild(
    createElement("div", {
      className: "loading-state",
      textContent: "Consultando datos de la inspección técnica..."
    })
  );

  let inspeccion = null;

  async function reloadAll() {
    try {
      inspeccion = await obtenerInspeccion(inspeccionId);
      renderView();
    } catch (err) {
      clearChildren(container);
      container.appendChild(
        createElement("div", {
          className: "alert alert--danger",
          textContent: `Error al cargar la inspección: ${err.message || "No encontrada"}`
        })
      );
    }
  }

  function renderView() {
    clearChildren(container);

    const isOfficialOrAdmin =
      hasRole(currentUser, ROLES.ADMINISTRADOR) ||
      hasRole(currentUser, ROLES.FUNCIONARIO) ||
      hasRole(currentUser, ROLES.SUPERVISOR);

    // 1. Barra superior
    const topBar = createElement("div", {
      className: "table-actions",
      attrs: { style: "justify-content: space-between; margin-bottom: 1.25rem;" }
    });

    const backBtn = createElement("button", {
      type: "button",
      className: "btn btn-secondary",
      textContent: "← Volver a Inspecciones"
    });
    if (typeof onBack === "function") {
      backBtn.addEventListener("click", onBack);
    }
    topBar.appendChild(backBtn);

    const rightTop = createElement("div", { className: "table-actions", attrs: { style: "gap: 0.5rem;" } });
    const refreshBtn = createElement("button", {
      type: "button",
      className: "btn btn-secondary",
      textContent: "Actualizar"
    });
    refreshBtn.addEventListener("click", reloadAll);
    rightTop.appendChild(refreshBtn);

    if (inspeccion.solicitudId && typeof onNavigateToSolicitud === "function") {
      const expBtn = createElement("button", {
        type: "button",
        className: "btn btn-secondary",
        textContent: "Ver Expediente"
      });
      expBtn.addEventListener("click", () => onNavigateToSolicitud(inspeccion.solicitudId));
      rightTop.appendChild(expBtn);
    }
    topBar.appendChild(rightTop);
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
        textContent: `INSPECCIÓN TÉCNICA N.º ${inspeccion.id}`
      })
    );
    titleRow.appendChild(createInspeccionEstadoBadge(inspeccion.estado));
    titleRow.appendChild(createInspeccionResultadoBadge(inspeccion.resultado));
    headerBox.appendChild(titleRow);

    const subMeta = createElement("div", {
      className: "expediente-header__meta",
      textContent: `Expediente: ${inspeccion.numeroExpediente || `ID #${inspeccion.solicitudId}`} • Programada: ${formatDateTime(inspeccion.fechaProgramada)} • Inspector: ${inspeccion.nombreInspector || "Pendiente"}`
    });
    headerBox.appendChild(subMeta);
    container.appendChild(headerBox);

    // 3. Barra de acciones operativas
    const actionsBar = createElement("div", { className: "expediente-actions-bar" });
    let hasActions = false;

    // Iniciar Inspección
    if (
      isOfficialOrAdmin &&
      (inspeccion.estado === "PROGRAMADA" || inspeccion.estado === "REPROGRAMADA")
    ) {
      hasActions = true;
      const startBtn = createElement("button", {
        type: "button",
        className: "btn btn-primary",
        textContent: "Iniciar Inspección In Situ"
      });
      startBtn.addEventListener("click", async () => {
        const confirmed = await confirmAction({
          title: "Iniciar Inspección Técnica",
          message: "¿Confirma el inicio de la diligencia de inspección in situ pasando a estado EN PROCESO?",
          confirmText: "Iniciar Diligencia",
          variant: "primary"
        });
        if (!confirmed) return;

        try {
          startBtn.disabled = true;
          await iniciarInspeccion(inspeccion.id);
          showToast("Inspección iniciada (EN PROCESO).", "success");
          await reloadAll();
        } catch (err) {
          showToast(err.message || "Error al iniciar inspección.", "error");
        } finally {
          startBtn.disabled = false;
        }
      });
      actionsBar.appendChild(startBtn);
    }

    // Registrar Resultado
    if (isOfficialOrAdmin && inspeccion.estado === "EN_PROCESO") {
      hasActions = true;
      const resBtn = createElement("button", {
        type: "button",
        className: "btn btn--success",
        textContent: "Registrar Resultado Técnico"
      });
      resBtn.addEventListener("click", () => openRegistrarResultadoModal());
      actionsBar.appendChild(resBtn);
    }

    // Reprogramar Inspección
    if (
      isOfficialOrAdmin &&
      (inspeccion.estado === "PROGRAMADA" || inspeccion.estado === "REPROGRAMADA")
    ) {
      hasActions = true;
      const repBtn = createElement("button", {
        type: "button",
        className: "btn btn-secondary",
        textContent: "Reprogramar Inspección"
      });
      repBtn.addEventListener("click", () => {
        openReprogramarInspeccionModal({
          inspeccionId: inspeccion.id,
          currentInspectorId: inspeccion.inspectorId,
          onReprogramada: reloadAll
        });
      });
      actionsBar.appendChild(repBtn);
    }

    // Cancelar Inspección
    if (
      isOfficialOrAdmin &&
      (inspeccion.estado === "PROGRAMADA" ||
        inspeccion.estado === "REPROGRAMADA" ||
        inspeccion.estado === "EN_PROCESO")
    ) {
      hasActions = true;
      const canBtn = createElement("button", {
        type: "button",
        className: "btn btn--danger",
        textContent: "Cancelar Inspección"
      });
      canBtn.addEventListener("click", () => openCancelarModal());
      actionsBar.appendChild(canBtn);
    }

    if (hasActions) {
      container.appendChild(actionsBar);
    }

    // Aviso si fue reprogramada
    if (inspeccion.motivoReprogramacion) {
      const repBox = createElement("div", {
        className: "reprogramacion-notice",
        textContent: `Aviso de Reprogramación: ${inspeccion.motivoReprogramacion}`
      });
      container.appendChild(repBox);
    }

    // 4. Tarjeta de Datos del Establecimiento e Inspección
    const dataCard = createElement("div", { className: "card", attrs: { style: "margin-bottom: 1.5rem;" } });
    const dataHeader = createElement("div", { className: "card-header" });
    dataHeader.appendChild(
      createElement("h3", { className: "card-title", textContent: "Datos de la Diligencia y Establecimiento" })
    );
    dataCard.appendChild(dataHeader);

    const dataBody = createElement("div", { className: "card-body" });
    const grid = createElement("div", { className: "expediente-data-grid" });

    const fields = [
      ["N° de Expediente", inspeccion.numeroExpediente || `Solicitud #${inspeccion.solicitudId}`],
      ["Establecimiento Comercial", inspeccion.nombreComercialEstablecimiento || "—"],
      ["Dirección a Inspeccionar", inspeccion.direccionEstablecimiento || "—"],
      ["Administrado Titular", inspeccion.nombreCiudadano || "—"],
      ["Inspector Asignado", inspeccion.nombreInspector || "Pendiente de Asignación"],
      ["Fecha y Hora Programada", formatDateTime(inspeccion.fechaProgramada)],
      ["Fecha y Hora Realizada", formatDateTime(inspeccion.fechaRealizada)],
      ["Estado de la Inspección", inspeccion.estado],
      ["Resultado / Calificación", inspeccion.resultado || "Aún no calificada"]
    ];

    for (const [k, v] of fields) {
      const item = createElement("div", { className: "expediente-data-item" });
      item.appendChild(createElement("div", { className: "expediente-data-item__label", textContent: k }));
      item.appendChild(createElement("div", { className: "expediente-data-item__value", textContent: v }));
      grid.appendChild(item);
    }
    dataBody.appendChild(grid);
    dataCard.appendChild(dataBody);
    container.appendChild(dataCard);

    // 5. Dictamen Técnico y Recomendaciones (si ya fue realizada o tiene observaciones)
    if (inspeccion.observaciones || inspeccion.recomendaciones) {
      const isFav = inspeccion.resultado === "FAVORABLE";
      const isDesfav = inspeccion.resultado === "DESFAVORABLE";
      const isObs = inspeccion.resultado && inspeccion.resultado.includes("OBSERVACIONES");

      const dictamenCard = createElement("div", { className: "card", attrs: { style: "margin-bottom: 1.5rem;" } });
      const dictamenHeader = createElement("div", { className: "card-header" });
      dictamenHeader.appendChild(
        createElement("h3", { className: "card-title", textContent: "Dictamen Técnico y Acta de Inspección" })
      );
      dictamenCard.appendChild(dictamenHeader);

      const dictamenBody = createElement("div", { className: "card-body" });

      const boxClass = isFav
        ? "dictamen-tecnico-box is-favorable"
        : isDesfav
          ? "dictamen-tecnico-box is-desfavorable"
          : isObs
            ? "dictamen-tecnico-box is-observaciones"
            : "dictamen-tecnico-box";

      const box = createElement("div", { className: boxClass });
      box.appendChild(
        createElement("div", {
          className: "dictamen-tecnico-heading",
          textContent: `Calificación Técnica: ${inspeccion.resultado || "Diligencia Técnica"}`
        })
      );

      if (inspeccion.observaciones) {
        box.appendChild(
          createElement("div", {
            attrs: { style: "font-weight: 700; margin-top: 0.5rem; font-size: 0.85rem;" },
            textContent: "Observaciones de la Inspección:"
          })
        );
        box.appendChild(
          createElement("p", { className: "dictamen-tecnico-content", textContent: inspeccion.observaciones })
        );
      }

      if (inspeccion.recomendaciones) {
        box.appendChild(
          createElement("div", {
            attrs: { style: "font-weight: 700; margin-top: 0.75rem; font-size: 0.85rem;" },
            textContent: "Recomendaciones de Seguridad en Edificaciones:"
          })
        );
        box.appendChild(
          createElement("p", { className: "dictamen-tecnico-content", textContent: inspeccion.recomendaciones })
        );
      }

      dictamenBody.appendChild(box);
      dictamenCard.appendChild(dictamenBody);
      container.appendChild(dictamenCard);
    }
  }

  // Modal para registrar resultado
  function openRegistrarResultadoModal() {
    const form = createElement("form", { className: "modal-form" });

    const resGroup = createElement("div", { className: "form-group" });
    resGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Calificación del Resultado Técnico *" })
    );
    const resSelect = createElement("select", { className: "form-control" });
    resSelect.appendChild(new Option("FAVORABLE — Cumple condiciones de seguridad", "FAVORABLE"));
    resSelect.appendChild(
      new Option("CON_OBSERVACIONES — Observaciones subsanables", "CON_OBSERVACIONES")
    );
    resSelect.appendChild(
      new Option("FAVORABLE_CON_OBSERVACIONES — Cumplimiento sustancial con medidas menores", "FAVORABLE_CON_OBSERVACIONES")
    );
    resSelect.appendChild(
      new Option("DESFAVORABLE — Alto riesgo o no cumple normativa ITSE", "DESFAVORABLE")
    );
    resGroup.appendChild(resSelect);

    const dateGroup = createElement("div", { className: "form-group", attrs: { style: "max-width: 320px;" } });
    dateGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Fecha y Hora Realizada de la Inspección" })
    );
    const dateIn = createElement("input", { type: "datetime-local", className: "form-control" });
    const now = new Date();
    dateIn.value = now.toISOString().slice(0, 16);
    dateGroup.appendChild(dateIn);

    const obsGroup = createElement("div", { className: "form-group" });
    obsGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Observaciones Técnicas de la Inspección *" })
    );
    const obsText = createElement("textarea", {
      className: "form-control",
      attrs: { rows: "4", placeholder: "Detalle de aforo, extintores, señalización, instalaciones eléctricas..." }
    });
    obsGroup.appendChild(obsText);

    const recGroup = createElement("div", { className: "form-group" });
    recGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Recomendaciones de Seguridad (opcional)" })
    );
    const recText = createElement("textarea", {
      className: "form-control",
      attrs: { rows: "3", placeholder: "Plazo de subsanación o medidas preventivas..." }
    });
    recGroup.appendChild(recText);

    form.append(resGroup, dateGroup, obsGroup, recGroup);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn-primary", textContent: "Guardar Dictamen" });

    const modal = openModal({
      title: "Registrar Resultado Técnico de la Inspección",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const obs = obsText.value.trim();
      if (!obs) {
        showToast("Las observaciones de la inspección son estrictamente obligatorias.", "warning");
        return;
      }

      try {
        submitBtn.disabled = true;
        const formattedDate = dateIn.value
          ? (dateIn.value.length === 16 ? `${dateIn.value}:00` : dateIn.value)
          : null;

        const payload = {
          resultado: resSelect.value,
          fechaRealizada: formattedDate,
          observaciones: obs,
          recomendaciones: recText.value.trim() || null
        };

        await registrarResultadoInspeccion(inspeccion.id, payload);
        showToast("Resultado de inspección registrado exitosamente.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al registrar resultado.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  // Modal para cancelar
  function openCancelarModal() {
    const form = createElement("form", { className: "modal-form" });
    const motGroup = createElement("div", { className: "form-group" });
    motGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Motivo de Cancelación de la Inspección *" })
    );
    const motText = createElement("textarea", {
      className: "form-control",
      attrs: { rows: "3", placeholder: "Indique la causa justificada de la cancelación..." }
    });
    motGroup.appendChild(motText);
    form.appendChild(motGroup);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Volver" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn--danger", textContent: "Confirmar Cancelación" });

    const modal = openModal({
      title: "Cancelar Inspección Programada",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const motivo = motText.value.trim();
      if (!motivo) {
        showToast("El motivo de cancelación es obligatorio.", "warning");
        return;
      }

      try {
        submitBtn.disabled = true;
        await cancelarInspeccion(inspeccion.id, { motivo });
        showToast("Inspección cancelada.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al cancelar inspección.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  await reloadAll();
}
