/**
 * Módulo de Detalle Integral del Expediente y Solicitud Municipal (PROMPT 12 - Secciones 12, 13, 14, 15, 19 y 22)
 */
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { openModal } from "../components/modal.js";
import { confirmAction } from "../components/confirm-dialog.js";
import {
  obtenerSolicitud,
  presentarSolicitud,
  asignarFuncionario,
  iniciarRevisionSolicitud,
  observarSolicitud,
  subsanarSolicitud,
  aprobarSolicitud,
  rechazarSolicitud,
  archivarSolicitud
} from "./solicitudes-api.js";
import { listarRequisitosPorTipoTramite } from "../tramites/tramites-api.js";
import { listarDocumentosPorSolicitud } from "../documentos/documentos-api.js";
import {
  createSolicitudEstadoBadge,
  getSolicitudEstadoLabel,
  getAvailableTransitions
} from "./solicitud-estados.js";
import { renderTimelineItems } from "./solicitud-historial.js";
import {
  renderRequirementsChecklist,
  renderAttachedDocumentsList,
  openUploadDocumentModal,
  evaluateRequirementsCompliance
} from "../documentos/documentos.js";
import { listarUsuarios } from "../usuarios/usuarios-api.js";

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

export async function renderSolicitudDetalle({
  container,
  solicitudId,
  currentUser = null,
  onBack = null,
  onNavigateToEditDraft = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  container.appendChild(
    createElement("div", {
      className: "loading-state",
      textContent: "Cargando datos completos del expediente municipal..."
    })
  );

  let solicitud = null;
  let requisitos = [];
  let documentos = [];

  async function reloadAll() {
    try {
      solicitud = await obtenerSolicitud(solicitudId);
      documentos = Array.isArray(solicitud.documentos) ? solicitud.documentos : [];

      if (solicitud.tipoTramite?.id) {
        requisitos = await listarRequisitosPorTipoTramite(solicitud.tipoTramite.id);
      } else {
        requisitos = [];
      }
      renderView();
    } catch (err) {
      clearChildren(container);
      container.appendChild(
        createElement("div", {
          className: "field-error",
          textContent: `Error al cargar expediente: ${err.message || "No encontrado"}`
        })
      );
    }
  }

  function renderView() {
    clearChildren(container);

    const userRoles = currentUser?.roles || [];
    const transitions = getAvailableTransitions(solicitud.estado, userRoles);

    // 1. Barra superior de navegación y título
    const topBar = createElement("div", {
      className: "table-actions",
      attrs: { style: "justify-content: space-between; margin-bottom: 1rem;" }
    });

    const backBtn = createElement("button", {
      type: "button",
      className: "btn btn--secondary",
      textContent: "← Volver a Solicitudes"
    });
    if (typeof onBack === "function") {
      backBtn.addEventListener("click", onBack);
    }
    topBar.appendChild(backBtn);

    const refreshBtn = createElement("button", {
      type: "button",
      className: "btn btn--secondary",
      textContent: "Actualizar Expediente"
    });
    refreshBtn.addEventListener("click", reloadAll);
    topBar.appendChild(refreshBtn);
    container.appendChild(topBar);

    // 2. Encabezado principal del expediente
    const expedienteHeader = createElement("div", {
      className: "expediente-header",
      attrs: { style: "margin-bottom: 1.5rem;" }
    });

    const expCode = solicitud.numeroExpediente || `Borrador #${solicitud.id}`;
    const titleRow = createElement("div", {
      className: "table-actions",
      attrs: { style: "gap: 0.75rem; align-items: center;" }
    });

    titleRow.appendChild(
      createElement("h2", {
        className: "expediente-header__number",
        textContent: expCode
      })
    );
    titleRow.appendChild(createSolicitudEstadoBadge(solicitud.estado));
    expedienteHeader.appendChild(titleRow);

    const subMeta = createElement("div", {
      className: "expediente-header__meta",
      textContent: `Presentado: ${formatDateTime(solicitud.fechaPresentacion)} • Última Actualización: ${formatDateTime(solicitud.fechaActualizacion)}`
    });
    expedienteHeader.appendChild(subMeta);
    container.appendChild(expedienteHeader);

    // 3. Barra de acciones operativas de transición (Sección 12)
    const actionsBar = createElement("div", { className: "expediente-actions-bar" });
    let hasActions = false;

    // Editar Borrador
    if (transitions.canEditDraft && typeof onNavigateToEditDraft === "function") {
      hasActions = true;
      const editDraftBtn = createElement("button", {
        type: "button",
        className: "btn btn--secondary",
        textContent: "Editar Borrador"
      });
      editDraftBtn.addEventListener("click", () => onNavigateToEditDraft(solicitud.id));
      actionsBar.appendChild(editDraftBtn);
    }

    // Presentar formalmente
    if (transitions.canPresentar) {
      hasActions = true;
      const presBtn = createElement("button", {
        type: "button",
        className: "btn btn--success",
        textContent: "Presentar Formalmente"
      });
      presBtn.addEventListener("click", async () => {
        const compliance = evaluateRequirementsCompliance(requisitos, documentos);
        if (!compliance.allMandatoryMet) {
          showToast(
            `No puede presentar el expediente sin cumplir los ${compliance.pendientesObligatorios} requisito(s) obligatorio(s) pendiente(s).`,
            "error"
          );
          return;
        }

        const confirmed = await confirmAction({
          title: "Presentar Solicitud Municipal",
          message: "¿Confirma la presentación formal de este expediente? Se asignará un N° Oficial de Expediente y se iniciará el trámite regular.",
          confirmText: "Sí, presentar expediente",
          variant: "primary"
        });
        if (!confirmed) return;

        try {
          presBtn.disabled = true;
          await presentarSolicitud(solicitud.id);
          showToast("Solicitud presentada con éxito.", "success");
          await reloadAll();
        } catch (err) {
          showToast(err.message || "Error al presentar la solicitud.", "error");
        } finally {
          presBtn.disabled = false;
        }
      });
      actionsBar.appendChild(presBtn);
    }

    // Asignar Funcionario
    if (transitions.canAsignar) {
      hasActions = true;
      const assignBtn = createElement("button", {
        type: "button",
        className: "btn btn--primary",
        textContent: "Asignar Funcionario"
      });
      assignBtn.addEventListener("click", () => openAsignarModal());
      actionsBar.appendChild(assignBtn);
    }

    // Iniciar Revisión
    if (transitions.canIniciarRevision) {
      hasActions = true;
      const startRevBtn = createElement("button", {
        type: "button",
        className: "btn btn--info",
        textContent: "Iniciar Revisión Técnica"
      });
      startRevBtn.addEventListener("click", async () => {
        const confirmed = await confirmAction({
          title: "Iniciar Revisión Técnica",
          message: "¿Desea iniciar la revisión técnica y legal del expediente pasando a estado EN REVISIÓN?",
          confirmText: "Iniciar Revisión",
          variant: "primary"
        });
        if (!confirmed) return;

        try {
          startRevBtn.disabled = true;
          await iniciarRevisionSolicitud(solicitud.id);
          showToast("Expediente en estado EN REVISIÓN.", "success");
          await reloadAll();
        } catch (err) {
          showToast(err.message || "Error al iniciar revisión.", "error");
        } finally {
          startRevBtn.disabled = false;
        }
      });
      actionsBar.appendChild(startRevBtn);
    }

    // Observar
    if (transitions.canObservar) {
      hasActions = true;
      const obsBtn = createElement("button", {
        type: "button",
        className: "btn btn--warning",
        textContent: "Registrar Observación"
      });
      obsBtn.addEventListener("click", () => openObservarModal());
      actionsBar.appendChild(obsBtn);
    }

    // Subsanar
    if (transitions.canSubsanar) {
      hasActions = true;
      const subBtn = createElement("button", {
        type: "button",
        className: "btn btn--info",
        textContent: "Subsanar Observaciones"
      });
      subBtn.addEventListener("click", () => openSubsanarModal());
      actionsBar.appendChild(subBtn);
    }

    // Aprobar
    if (transitions.canAprobar) {
      hasActions = true;
      const approveBtn = createElement("button", {
        type: "button",
        className: "btn btn--success",
        textContent: "Aprobar Solicitud"
      });
      approveBtn.addEventListener("click", async () => {
        const confirmed = await confirmAction({
          title: "Aprobar Solicitud de Licencia",
          message: "¿Confirma que la solicitud y sus requisitos cumplen plenamente con la normativa municipal para ser APROBADA?",
          confirmText: "Aprobar Expediente",
          variant: "primary"
        });
        if (!confirmed) return;

        try {
          approveBtn.disabled = true;
          await aprobarSolicitud(solicitud.id);
          showToast("Solicitud aprobada favorablemente.", "success");
          await reloadAll();
        } catch (err) {
          showToast(err.message || "Error al aprobar la solicitud.", "error");
        } finally {
          approveBtn.disabled = false;
        }
      });
      actionsBar.appendChild(approveBtn);
    }

    // Rechazar
    if (transitions.canRechazar) {
      hasActions = true;
      const rejectBtn = createElement("button", {
        type: "button",
        className: "btn btn--danger",
        textContent: "Rechazar Solicitud"
      });
      rejectBtn.addEventListener("click", () => openRechazarModal());
      actionsBar.appendChild(rejectBtn);
    }

    // Archivar
    if (transitions.canArchivar) {
      hasActions = true;
      const archiveBtn = createElement("button", {
        type: "button",
        className: "btn btn--secondary",
        textContent: "Archivar Expediente"
      });
      archiveBtn.addEventListener("click", () => openArchivarModal());
      actionsBar.appendChild(archiveBtn);
    }

    // Adjuntar Documento General
    if (transitions.canUploadDocs) {
      hasActions = true;
      const addDocBtn = createElement("button", {
        type: "button",
        className: "btn btn--secondary",
        textContent: "+ Adjuntar Documento"
      });
      addDocBtn.addEventListener("click", () => {
        openUploadDocumentModal({
          solicitudId: solicitud.id,
          requisitos,
          onUploaded: async () => {
            await reloadAll();
          }
        });
      });
      actionsBar.appendChild(addDocBtn);
    }

    // Botones de integración entre módulos (PROMPT 13 - Sección 29)
    if (solicitud.estado !== "BORRADOR") {
      hasActions = true;
      const inspLinkBtn = createElement("button", {
        type: "button",
        className: "btn btn-secondary",
        textContent: "🔍 Ver Inspecciones ITSE"
      });
      inspLinkBtn.addEventListener("click", () => {
        window.location.hash = `#inspecciones?solicitudId=${encodeURIComponent(solicitud.id)}`;
      });
      actionsBar.appendChild(inspLinkBtn);

      const pagoLinkBtn = createElement("button", {
        type: "button",
        className: "btn btn-secondary",
        textContent: "💳 Ver Pagos del Expediente"
      });
      pagoLinkBtn.addEventListener("click", () => {
        window.location.hash = `#pagos?solicitudId=${encodeURIComponent(solicitud.id)}`;
      });
      actionsBar.appendChild(pagoLinkBtn);

      if (solicitud.estado === "APROBADA") {
        const licLinkBtn = createElement("button", {
          type: "button",
          className: "btn btn--primary",
          textContent: "📜 Emitir / Ver Licencia"
        });
        licLinkBtn.addEventListener("click", () => {
          window.location.hash = `#licencias?subview=emitir&solicitudId=${encodeURIComponent(solicitud.id)}`;
        });
        actionsBar.appendChild(licLinkBtn);
      }
    }

    if (hasActions) {
      container.appendChild(actionsBar);
    }

    // Alertas contextuales si la solicitud está OBSERVADA o RECHAZADA
    if (solicitud.estado === "OBSERVADA" && solicitud.observaciones) {
      const alertObs = createElement("div", {
        className: "alert alert--warning",
        attrs: { style: "margin-bottom: 1.5rem;" },
        textContent: `Expediente Observado: ${solicitud.observaciones}`
      });
      container.appendChild(alertObs);
    } else if (solicitud.estado === "RECHAZADA" && solicitud.motivoRechazo) {
      const alertRech = createElement("div", {
        className: "alert alert--danger",
        attrs: { style: "margin-bottom: 1.5rem;" },
        textContent: `Expediente Rechazado: ${solicitud.motivoRechazo}`
      });
      container.appendChild(alertRech);
    } else if (solicitud.estado === "ARCHIVADA" && solicitud.motivoRechazo) {
      const alertArch = createElement("div", {
        className: "alert alert--neutral",
        attrs: { style: "margin-bottom: 1.5rem;" },
        textContent: `Expediente Archivado: ${solicitud.motivoRechazo}`
      });
      container.appendChild(alertArch);
    }

    // 4. Layout en rejilla de dos columnas
    const gridLayout = createElement("div", { className: "expediente-grid-layout" });

    // --- COLUMNA 1: DATOS ADMINISTRATIVOS, ESTABLECIMIENTO, REQUISITOS Y DOCUMENTOS ---
    const colMain = createElement("div", { className: "expediente-main-column" });

    // Tarjeta: Datos del Trámite y Expediente
    const tramCard = createCard("Datos del Trámite y Expediente");
    const tramGrid = createElement("div", { className: "expediente-data-grid" });

    const tramiteFields = [
      ["Tipo de Trámite", solicitud.tipoTramite ? `${solicitud.tipoTramite.codigo} — ${solicitud.tipoTramite.nombre}` : "—"],
      ["Vigencia de Licencia", solicitud.tipoTramite?.vigenciaAnios ? `${solicitud.tipoTramite.vigenciaAnios} año(s)` : "Indeterminada"],
      ["Inspección Previa", solicitud.tipoTramite?.requiereInspeccionPrevia ? "Sí (ITSE Previa)" : "No (ITSE Posterior)"],
      ["Funcionario Responsable", solicitud.usuarioResponsableNombreCompleto || "Pendiente de Asignación"],
      ["Fecha de Ingreso", formatDateTime(solicitud.fechaPresentacion)],
      ["Observaciones Generales", solicitud.observaciones || "Sin observaciones adicionales"]
    ];

    for (const [label, val] of tramiteFields) {
      const item = createElement("div", { className: "expediente-data-item" });
      item.appendChild(createElement("div", { className: "expediente-data-item__label", textContent: label }));
      item.appendChild(createElement("div", { className: "expediente-data-item__value", textContent: val }));
      tramGrid.appendChild(item);
    }
    tramCard.body.appendChild(tramGrid);
    colMain.appendChild(tramCard.card);

    // Tarjeta: Administrado y Representante
    const admCard = createCard("Datos del Administrado y Representante Legal");
    const admGrid = createElement("div", { className: "expediente-data-grid" });

    const c = solicitud.ciudadano;
    const r = solicitud.representante;
    const admFields = [
      ["Administrado", c ? `${c.nombres} ${c.apellidos}` : "—"],
      ["Documento de Identidad", c ? `${c.tipoDocumento} ${c.numeroDocumento}` : "—"],
      ["Teléfono de Contacto", c?.telefono || "—"],
      ["Correo Electrónico", c?.email || "—"],
      ["Representante Legal", r ? `${r.nombres} ${r.apellidos} (${r.tipoDocumento} ${r.numeroDocumento})` : "Directo (sin representante)"],
      ["Contacto Representante", r ? `Tel: ${r.telefono || "—"} | Email: ${r.email || "—"}` : "—"]
    ];

    for (const [label, val] of admFields) {
      const item = createElement("div", { className: "expediente-data-item" });
      item.appendChild(createElement("div", { className: "expediente-data-item__label", textContent: label }));
      item.appendChild(createElement("div", { className: "expediente-data-item__value", textContent: val }));
      admGrid.appendChild(item);
    }
    admCard.body.appendChild(admGrid);
    colMain.appendChild(admCard.card);

    // Tarjeta: Establecimiento Comercial
    const estCard = createCard("Establecimiento Comercial Objeto de Licencia");
    const estGrid = createElement("div", { className: "expediente-data-grid" });

    const e = solicitud.establecimiento;
    const estFields = [
      ["Nombre Comercial", e?.nombreComercial || "—"],
      ["Razón Social / Titular", e?.razonSocial || "—"],
      ["RUC del Local", e?.ruc || "Sin RUC"],
      ["Dirección del Establecimiento", e?.direccion || "—"],
      ["Giro Comercial", e?.giroComercial || "—"],
      ["Zonificación Distrital", e?.zonificacion || "—"],
      ["Área Total", e ? `${e.areaTotalM2} m²` : "—"],
      ["Nivel de Riesgo ITSE", e?.nivelRiesgo || "—"]
    ];

    for (const [label, val] of estFields) {
      const item = createElement("div", { className: "expediente-data-item" });
      item.appendChild(createElement("div", { className: "expediente-data-item__label", textContent: label }));
      item.appendChild(createElement("div", { className: "expediente-data-item__value", textContent: val }));
      estGrid.appendChild(item);
    }
    estCard.body.appendChild(estGrid);
    colMain.appendChild(estCard.card);

    // Tarjeta: Checklist de Requisitos Exigidos
    const reqCard = createCard("Requisitos Exigidos para la Licencia");
    const reqMount = createElement("div");
    reqCard.body.appendChild(reqMount);

    renderRequirementsChecklist({
      container: reqMount,
      solicitudId: solicitud.id,
      requisitos,
      documentos,
      canUpload: transitions.canUploadDocs,
      onDocumentsChanged: reloadAll
    });
    colMain.appendChild(reqCard.card);

    // Tarjeta: Todos los Documentos Adjuntos
    const docsCard = createCard("Todos los Documentos Adjuntos del Expediente");
    const docsMount = createElement("div");
    docsCard.body.appendChild(docsMount);

    renderAttachedDocumentsList({
      container: docsMount,
      documentos,
      canModify: transitions.canUploadDocs,
      onDocumentsChanged: reloadAll
    });
    colMain.appendChild(docsCard.card);

    gridLayout.appendChild(colMain);

    // --- COLUMNA 2: RESUMEN DE CUMPLIMIENTO Y LÍNEA DE TIEMPO ---
    const colSide = createElement("div", { className: "expediente-side-column" });

    // Tarjeta: Resumen de Requisitos
    const compCard = createCard("Resumen de Cumplimiento");
    const compliance = evaluateRequirementsCompliance(requisitos, documentos);

    const compInfo = createElement("div");
    compInfo.appendChild(
      createElement("p", {
        attrs: { style: "margin-bottom: 0.75rem; font-size: 0.95rem;" },
        textContent: `Requisitos obligatorios cumplidos: ${compliance.cumplidos} de ${compliance.obligatorios}`
      })
    );

    const compBadge = createElement("span", {
      className: `badge ${compliance.allMandatoryMet ? "badge--success" : "badge--warning"}`,
      textContent: compliance.allMandatoryMet ? "TODOS LOS REQUISITOS CUMPLIDOS" : `${compliance.pendientesObligatorios} REQUISITO(S) PENDIENTE(S)`
    });
    compInfo.appendChild(compBadge);
    compCard.body.appendChild(compInfo);
    colSide.appendChild(compCard.card);

    // Tarjeta: Línea de Tiempo del Expediente
    const timelineCard = createCard("Línea de Tiempo del Expediente");
    const timelineMount = createElement("div");
    timelineCard.body.appendChild(timelineMount);

    renderTimelineItems(timelineMount, solicitud.historial || []);
    colSide.appendChild(timelineCard.card);

    gridLayout.appendChild(colSide);
    container.appendChild(gridLayout);
  }

  function createCard(titleText) {
    const card = createElement("div", { className: "card expediente-card", attrs: { style: "margin-bottom: 1.5rem;" } });
    const header = createElement("div", { className: "card-header" });
    header.appendChild(createElement("h3", { className: "card-title", textContent: titleText }));
    const body = createElement("div", { className: "card-body" });
    card.append(header, body);
    return { card, header, body };
  }

  // --- MODALES DE ACCIÓN ---
  async function openAsignarModal() {
    const form = createElement("form", { className: "modal-form" });
    const group = createElement("div", { className: "form-group" });
    group.appendChild(createElement("label", { className: "form-label", textContent: "Seleccione el Funcionario Responsable *" }));

    const select = createElement("select", { className: "form-select" });
    select.appendChild(createElement("option", { value: "", textContent: "— Cargando funcionarios... —" }));
    group.appendChild(select);

    const obsGroup = createElement("div", { className: "form-group" });
    obsGroup.appendChild(createElement("label", { className: "form-label", textContent: "Observaciones de la asignación" }));
    const obsText = createElement("textarea", { className: "form-input", attrs: { rows: "2" } });
    obsGroup.appendChild(obsText);

    form.append(group, obsGroup);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn--secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn--primary", textContent: "Asignar Responsable" });

    const modal = openModal({
      title: "Asignar Funcionario al Expediente",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    try {
      const page = await listarUsuarios({ activo: true, size: 50 });
      clearChildren(select);
      select.appendChild(createElement("option", { value: "", textContent: "— Seleccione funcionario —" }));
      const users = Array.isArray(page?.contenido) ? page.contenido : [];
      for (const u of users) {
        select.appendChild(
          createElement("option", {
            value: String(u.id),
            textContent: `${u.nombreCompleto} (${u.username})`
          })
        );
      }
    } catch (_e) {
      clearChildren(select);
      select.appendChild(createElement("option", { value: "", textContent: "Error al cargar funcionarios" }));
    }

    submitBtn.addEventListener("click", async () => {
      if (!select.value) {
        showToast("Seleccione un funcionario responsable.", "warning");
        return;
      }
      try {
        submitBtn.disabled = true;
        await asignarFuncionario(solicitud.id, {
          usuarioResponsableId: select.value,
          observaciones: obsText.value.trim()
        });
        showToast("Funcionario asignado correctamente.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al asignar funcionario.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  function openObservarModal() {
    const form = createElement("form", { className: "modal-form" });
    const group = createElement("div", { className: "form-group" });
    group.appendChild(createElement("label", { className: "form-label", textContent: "Detalle motivado de las observaciones *" }));
    const text = createElement("textarea", {
      className: "form-input",
      attrs: { rows: "4", placeholder: "Indique las observaciones técnicas o documentales a subsanar..." }
    });
    group.appendChild(text);
    form.appendChild(group);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn--secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn--warning", textContent: "Registrar Observación" });

    const modal = openModal({
      title: "Observar Solicitud",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const obs = text.value.trim();
      if (!obs) {
        showToast("Debe ingresar el detalle de la observación.", "warning");
        return;
      }
      try {
        submitBtn.disabled = true;
        await observarSolicitud(solicitud.id, obs);
        showToast("Solicitud observada correctamente.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al registrar observación.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  function openSubsanarModal() {
    const form = createElement("form", { className: "modal-form" });
    const group = createElement("div", { className: "form-group" });
    group.appendChild(createElement("label", { className: "form-label", textContent: "Detalle de la subsanación realizada *" }));
    const text = createElement("textarea", {
      className: "form-input",
      attrs: { rows: "4", placeholder: "Describa las subsanaciones y documentos aportados..." }
    });
    group.appendChild(text);
    form.appendChild(group);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn--secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn--info", textContent: "Presentar Subsanación" });

    const modal = openModal({
      title: "Subsanar Observaciones",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const detail = text.value.trim();
      if (!detail) {
        showToast("Debe indicar el detalle de la subsanación.", "warning");
        return;
      }
      try {
        submitBtn.disabled = true;
        await subsanarSolicitud(solicitud.id, detail);
        showToast("Subsanación registrada con éxito.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al presentar subsanación.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  function openRechazarModal() {
    const form = createElement("form", { className: "modal-form" });
    const group = createElement("div", { className: "form-group" });
    group.appendChild(createElement("label", { className: "form-label", textContent: "Motivo formal del rechazo *" }));
    const text = createElement("textarea", {
      className: "form-input",
      attrs: { rows: "4", placeholder: "Fundamento legal y técnico de la denegatoria..." }
    });
    group.appendChild(text);
    form.appendChild(group);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn--secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn--danger", textContent: "Confirmar Rechazo" });

    const modal = openModal({
      title: "Rechazar Solicitud de Licencia",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const motivo = text.value.trim();
      if (!motivo) {
        showToast("Debe ingresar el motivo de rechazo.", "warning");
        return;
      }
      try {
        submitBtn.disabled = true;
        await rechazarSolicitud(solicitud.id, { motivoRechazo: motivo });
        showToast("Solicitud rechazada formalmente.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al rechazar solicitud.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  function openArchivarModal() {
    const form = createElement("form", { className: "modal-form" });
    const group = createElement("div", { className: "form-group" });
    group.appendChild(createElement("label", { className: "form-label", textContent: "Motivo del archivamiento definitivo *" }));
    const text = createElement("textarea", {
      className: "form-input",
      attrs: { rows: "4", placeholder: "Indique la causa del archivamiento..." }
    });
    group.appendChild(text);
    form.appendChild(group);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn--secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn--secondary", textContent: "Confirmar Archivo" });

    const modal = openModal({
      title: "Archivar Expediente",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const motivo = text.value.trim();
      if (!motivo) {
        showToast("Debe indicar el motivo del archivamiento.", "warning");
        return;
      }
      try {
        submitBtn.disabled = true;
        await archivarSolicitud(solicitud.id, { motivoRechazo: motivo });
        showToast("Expediente archivado.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al archivar expediente.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  // Carga inicial
  await reloadAll();
}
