/**
 * Módulo de Formulario Guiado por Pasos (Wizard) para Nueva Solicitud Municipal (PROMPT 12 - Secciones 11, 14, 18 y 21)
 */
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { listarTiposTramite, listarRequisitosPorTipoTramite } from "../tramites/tramites-api.js";
import { listarCiudadanos, obtenerMiPerfilCiudadano } from "../ciudadanos/ciudadanos-api.js";
import { listarEstablecimientos, listarEstablecimientosPorCiudadano } from "../establecimientos/establecimientos-api.js";
import { listarRepresentantes } from "../representantes/representantes-api.js";
import { getCurrentUser, hasRole } from "../auth.js";
import { ROLES } from "../config.js";
import {
  crearSolicitud,
  actualizarSolicitud,
  presentarSolicitud,
  obtenerSolicitud
} from "./solicitudes-api.js";
import { listarDocumentosPorSolicitud } from "../documentos/documentos-api.js";
import { renderRequirementsChecklist, evaluateRequirementsCompliance } from "../documentos/documentos.js";

export function renderNuevaSolicitudWizard({
  container,
  initialDraftId = null,
  onFinished = null,
  onCancel = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  // Estado reactivo del Wizard
  let currentStep = 1;
  const totalSteps = 5;
  let draftSolicitudId = initialDraftId ? Number(initialDraftId) : null;

  // Catálogos cargados
  let tiposTramiteList = [];
  let ciudadanosList = [];
  let establecimientosList = [];
  let representantesList = [];
  let currentRequisitos = [];
  let currentDocumentos = [];

  // Datos seleccionados
  const formData = {
    tipoTramiteId: null,
    ciudadanoId: null,
    establecimientoId: null,
    representanteId: null,
    observaciones: ""
  };

  // Contenedor principal
  const wizardCard = createElement("div", { className: "card" });

  // 1. Barra de progreso visual (Stepper)
  const stepper = createElement("div", {
    className: "wizard-stepper",
    attrs: { role: "tablist", "aria-label": "Pasos de registro de la solicitud" }
  });

  const stepLabels = [
    { num: 1, title: "Tipo de Trámite" },
    { num: 2, title: "Administrado" },
    { num: 3, title: "Establecimiento" },
    { num: 4, title: "Requisitos" },
    { num: 5, title: "Presentación" }
  ];

  const stepElements = stepLabels.map((st) => {
    const el = createElement("div", {
      className: `wizard-step ${st.num === 1 ? "is-active" : ""}`,
      attrs: { id: `wizard-step-indicator-${st.num}` }
    });
    const circle = createElement("div", {
      className: "wizard-step__indicator",
      textContent: String(st.num)
    });
    const label = createElement("div", {
      className: "wizard-step__label",
      textContent: st.title
    });
    el.append(circle, label);
    stepper.appendChild(el);
    return el;
  });

  wizardCard.appendChild(stepper);

  // 2. Contenedor de paso activo
  const stepContentContainer = createElement("div", {
    className: "card-body",
    attrs: { "aria-live": "polite" }
  });
  wizardCard.appendChild(stepContentContainer);

  // 3. Barra de navegación del wizard (Footer)
  const footer = createElement("div", { className: "card-footer wizard-footer" });

  const prevBtn = createElement("button", {
    type: "button",
    className: "btn btn--secondary",
    textContent: "← Anterior"
  });
  prevBtn.disabled = true;

  const nextBtn = createElement("button", {
    type: "button",
    className: "btn btn--primary",
    textContent: "Siguiente →"
  });

  const draftStatusEl = createElement("span", {
    className: "text-muted",
    attrs: { style: "font-size: 0.85rem;" }
  });

  const footerLeft = createElement("div", { className: "table-actions" });
  footerLeft.append(prevBtn, draftStatusEl);

  const footerRight = createElement("div", { className: "table-actions" });
  const cancelBtn = createElement("button", {
    type: "button",
    className: "btn btn--secondary",
    textContent: "Cancelar"
  });
  footerRight.append(cancelBtn, nextBtn);

  footer.append(footerLeft, footerRight);
  wizardCard.appendChild(footer);
  container.appendChild(wizardCard);

  function updateStepperUI() {
    stepElements.forEach((el, idx) => {
      const stepNum = idx + 1;
      el.classList.toggle("is-active", stepNum === currentStep);
      el.classList.toggle("is-completed", stepNum < currentStep);
    });

    prevBtn.disabled = currentStep === 1;

    if (currentStep === totalSteps) {
      nextBtn.textContent = "Presentar Solicitud Formal";
      nextBtn.className = "btn btn--success";
    } else {
      nextBtn.textContent = "Siguiente →";
      nextBtn.className = "btn btn--primary";
    }

    if (draftSolicitudId) {
      draftStatusEl.textContent = `Borrador municipal N° ${draftSolicitudId} guardado.`;
    } else {
      draftStatusEl.textContent = "";
    }
  }

  // --- CARGA DE CATÁLOGOS INICIALES ---
  async function loadInitialCatalogs() {
    stepContentContainer.appendChild(
      createElement("div", {
        className: "loading-state",
        textContent: "Cargando catálogos municipales para el trámite..."
      })
    );

    try {
      const user = await getCurrentUser();
      const esSoloCiudadano =
        hasRole(user, ROLES.CIUDADANO) &&
        !hasRole(user, ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR);

      if (esSoloCiudadano) {
        // El ciudadano solo ve su propia ficha y sus establecimientos (el backend no le permite listar a otros)
        const [tramitesPage, miFicha] = await Promise.all([
          listarTiposTramite({ activo: true, size: 50 }),
          obtenerMiPerfilCiudadano()
        ]);
        const misEstablecimientos = await listarEstablecimientosPorCiudadano(miFicha.id);

        tiposTramiteList = Array.isArray(tramitesPage?.contenido) ? tramitesPage.contenido : [];
        ciudadanosList = [miFicha];
        formData.ciudadanoId = miFicha.id;
        establecimientosList = Array.isArray(misEstablecimientos) ? misEstablecimientos : [];
        representantesList = [];
      } else {
        const [tramitesPage, ciudPage, estabPage, repPage] = await Promise.all([
          listarTiposTramite({ activo: true, size: 50 }),
          listarCiudadanos({ activo: true, size: 100 }),
          listarEstablecimientos({ activo: true, size: 100 }),
          listarRepresentantes({ activo: true, size: 100 })
        ]);

        tiposTramiteList = Array.isArray(tramitesPage?.contenido) ? tramitesPage.contenido : [];
        ciudadanosList = Array.isArray(ciudPage?.contenido) ? ciudPage.contenido : [];
        establecimientosList = Array.isArray(estabPage?.contenido) ? estabPage.contenido : [];
        representantesList = Array.isArray(repPage?.contenido) ? repPage.contenido : [];
      }

      if (draftSolicitudId) {
        const sol = await obtenerSolicitud(draftSolicitudId);
        formData.tipoTramiteId = sol.tipoTramite?.id || null;
        formData.ciudadanoId = sol.ciudadano?.id || null;
        formData.establecimientoId = sol.establecimiento?.id || null;
        formData.representanteId = sol.representante?.id || null;
        formData.observaciones = sol.observaciones || "";
        currentDocumentos = Array.isArray(sol.documentos) ? sol.documentos : [];

        if (formData.tipoTramiteId) {
          currentRequisitos = await listarRequisitosPorTipoTramite(formData.tipoTramiteId);
        }
      }

      renderCurrentStep();
    } catch (err) {
      clearChildren(stepContentContainer);
      stepContentContainer.appendChild(
        createElement("div", {
          className: "field-error",
          textContent: `Error al cargar datos del sistema: ${err.message || "Verifique su conexión"}`
        })
      );
    }
  }

  // --- RENDERIZADO DE CADA PASO ---
  function renderCurrentStep() {
    clearChildren(stepContentContainer);
    updateStepperUI();

    switch (currentStep) {
      case 1:
        renderStep1();
        break;
      case 2:
        renderStep2();
        break;
      case 3:
        renderStep3();
        break;
      case 4:
        renderStep4();
        break;
      case 5:
        renderStep5();
        break;
    }
  }

  // PASO 1: Selección de Tipo de Trámite
  function renderStep1() {
    const wrap = createElement("div");

    wrap.appendChild(
      createElement("h2", {
        className: "page-subtitle",
        textContent: "Paso 1: Seleccione el Tipo de Trámite Municipal"
      })
    );

    const desc = createElement("p", {
      attrs: { style: "margin-bottom: 1.5rem; color: var(--color-text-secondary);" },
      textContent: "Elija la modalidad de trámite según las características de su establecimiento y nivel de riesgo."
    });
    wrap.appendChild(desc);

    const grid = createElement("div", { className: "wizard-choice-grid" });

    if (tiposTramiteList.length === 0) {
      grid.appendChild(
        createElement("div", {
          className: "empty-state",
          textContent: "No hay tipos de trámite habilitados en este momento."
        })
      );
    }

    for (const t of tiposTramiteList) {
      const isSelected = formData.tipoTramiteId === t.id;
      const card = createElement("div", {
        className: `wizard-choice-card ${isSelected ? "is-selected" : ""}`,
        attrs: {
          role: "button",
          tabindex: "0",
          "aria-pressed": isSelected ? "true" : "false"
        }
      });

      const header = createElement("div", { className: "wizard-choice-card__header" });
      const codeSpan = createElement("span", {
        className: "badge badge--info",
        textContent: t.codigo
      });
      const titleSpan = createElement("strong", { textContent: t.nombre });
      header.append(codeSpan, titleSpan);

      const body = createElement("p", {
        className: "wizard-choice-card__desc",
        textContent: t.descripcion || "Trámite de Licencia Municipal"
      });

      const meta = createElement("div", {
        className: "wizard-choice-card__meta",
        textContent: `Vigencia: ${t.vigenciaAnios ? `${t.vigenciaAnios} año(s)` : "Indeterminada"} • Inspección Previa: ${t.requiereInspeccionPrevia ? "Sí (ITSE previa)" : "No (ITSE posterior)"}`
      });

      card.append(header, body, meta);

      card.addEventListener("click", async () => {
        formData.tipoTramiteId = t.id;
        wrap.querySelectorAll(".wizard-choice-card").forEach((c) => c.classList.remove("is-selected"));
        card.classList.add("is-selected");
        try {
          currentRequisitos = await listarRequisitosPorTipoTramite(t.id);
        } catch (_e) {
          currentRequisitos = [];
        }
      });

      grid.appendChild(card);
    }

    wrap.appendChild(grid);
    stepContentContainer.appendChild(wrap);
  }

  // PASO 2: Selección de Administrado / Ciudadano
  function renderStep2() {
    const wrap = createElement("div");

    wrap.appendChild(
      createElement("h2", {
        className: "page-subtitle",
        textContent: "Paso 2: Datos del Administrado (Ciudadano Solicitante)"
      })
    );

    const filterGroup = createElement("div", {
      className: "form-group",
      attrs: { style: "max-width: 480px; margin-bottom: 1.5rem;" }
    });
    filterGroup.appendChild(
      createElement("label", {
        className: "form-label",
        attrs: { for: "wizard-ciudadano-search" },
        textContent: "Buscar ciudadano por DNI / Carnet o Nombres"
      })
    );

    const searchInput = createElement("input", {
      type: "search",
      id: "wizard-ciudadano-search",
      className: "form-input",
      attrs: { placeholder: "Ingrese DNI o apellidos..." }
    });
    filterGroup.appendChild(searchInput);
    wrap.appendChild(filterGroup);

    const listWrap = createElement("div", { className: "wizard-choice-grid" });

    function renderCiudadanosOptions(filterText = "") {
      clearChildren(listWrap);
      const clean = filterText.trim().toLowerCase();
      const filtered = clean
        ? ciudadanosList.filter(
            (c) =>
              c.numeroDocumento.toLowerCase().includes(clean) ||
              c.nombres.toLowerCase().includes(clean) ||
              c.apellidos.toLowerCase().includes(clean)
          )
        : ciudadanosList;

      if (filtered.length === 0) {
        listWrap.appendChild(
          createElement("div", {
            className: "empty-state",
            textContent: "No se encontraron ciudadanos que coincidan con la búsqueda."
          })
        );
        return;
      }

      for (const c of filtered) {
        const isSelected = formData.ciudadanoId === c.id;
        const card = createElement("div", {
          className: `wizard-choice-card ${isSelected ? "is-selected" : ""}`,
          attrs: { role: "button", tabindex: "0" }
        });

        const header = createElement("div", { className: "wizard-choice-card__header" });
        const docBadge = createElement("span", {
          className: "badge badge--neutral",
          textContent: `${c.tipoDocumento}: ${c.numeroDocumento}`
        });
        const nameStrong = createElement("strong", {
          textContent: `${c.nombres} ${c.apellidos}`
        });
        header.append(docBadge, nameStrong);

        const meta = createElement("div", {
          className: "wizard-choice-card__meta",
          textContent: `Teléfono: ${c.telefono || "—"} • Correo: ${c.email || "—"}`
        });

        card.append(header, meta);

        card.addEventListener("click", () => {
          formData.ciudadanoId = c.id;
          listWrap.querySelectorAll(".wizard-choice-card").forEach((x) => x.classList.remove("is-selected"));
          card.classList.add("is-selected");
        });

        listWrap.appendChild(card);
      }
    }

    searchInput.addEventListener("input", () => {
      renderCiudadanosOptions(searchInput.value);
    });

    renderCiudadanosOptions();
    wrap.appendChild(listWrap);
    stepContentContainer.appendChild(wrap);
  }

  // PASO 3: Selección de Establecimiento Comercial y Representante Legal
  function renderStep3() {
    const wrap = createElement("div");

    wrap.appendChild(
      createElement("h2", {
        className: "page-subtitle",
        textContent: "Paso 3: Establecimiento Comercial y Representante"
      })
    );

    // Selección de Establecimiento
    const estabHeader = createElement("div", {
      className: "form-label",
      attrs: { style: "font-weight: 700; margin-top: 1rem;" },
      textContent: "1. Seleccione el Establecimiento Comercial Objeto de la Licencia *"
    });
    wrap.appendChild(estabHeader);

    // Filtrar preferentemente los establecimientos del ciudadano seleccionado si existen
    const userEstabs = formData.ciudadanoId
      ? establecimientosList.filter((e) => e.ciudadanoId === formData.ciudadanoId)
      : establecimientosList;
    const availableEstabs = userEstabs.length > 0 ? userEstabs : establecimientosList;

    const estabGrid = createElement("div", {
      className: "wizard-choice-grid",
      attrs: { style: "margin-bottom: 2rem;" }
    });

    if (availableEstabs.length === 0) {
      estabGrid.appendChild(
        createElement("div", {
          className: "empty-state",
          textContent: "No se registran establecimientos comerciales disponibles para vincular."
        })
      );
    }

    for (const e of availableEstabs) {
      const isSelected = formData.establecimientoId === e.id;
      const card = createElement("div", {
        className: `wizard-choice-card ${isSelected ? "is-selected" : ""}`,
        attrs: { role: "button", tabindex: "0" }
      });

      const header = createElement("div", { className: "wizard-choice-card__header" });
      const rucBadge = createElement("span", {
        className: "badge badge--info",
        textContent: `RUC: ${e.ruc || "Sin RUC"}`
      });
      const nameStrong = createElement("strong", { textContent: e.nombreComercial });
      header.append(rucBadge, nameStrong);

      const desc = createElement("p", {
        className: "wizard-choice-card__desc",
        textContent: `Giro: ${e.giroComercial || "General"} — Área: ${e.areaTotalM2 || 0} m²`
      });

      const meta = createElement("div", {
        className: "wizard-choice-card__meta",
        textContent: `Dirección: ${e.direccion || "—"}`
      });

      card.append(header, desc, meta);

      card.addEventListener("click", () => {
        formData.establecimientoId = e.id;
        estabGrid.querySelectorAll(".wizard-choice-card").forEach((x) => x.classList.remove("is-selected"));
        card.classList.add("is-selected");
      });

      estabGrid.appendChild(card);
    }

    wrap.appendChild(estabGrid);

    // Selección de Representante Legal (Opcional)
    const repHeader = createElement("div", {
      className: "form-label",
      attrs: { style: "font-weight: 700;" },
      textContent: "2. Representante Legal del Trámite (Opcional)"
    });
    wrap.appendChild(repHeader);

    const repSelectGroup = createElement("div", {
      className: "form-group",
      attrs: { style: "max-width: 500px; margin-bottom: 1.5rem;" }
    });

    const repSelect = createElement("select", {
      id: "wizard-rep-select",
      className: "form-select"
    });
    repSelect.appendChild(
      createElement("option", {
        value: "",
        textContent: "— Trámite directo sin representante legal apoderado —"
      })
    );

    for (const r of representantesList) {
      const opt = createElement("option", {
        value: String(r.id),
        textContent: `${r.tipoDocumento} ${r.numeroDocumento} — ${r.nombres} ${r.apellidos}`
      });
      if (formData.representanteId === r.id) {
        opt.selected = true;
      }
      repSelect.appendChild(opt);
    }

    repSelect.addEventListener("change", () => {
      formData.representanteId = repSelect.value ? Number(repSelect.value) : null;
    });

    repSelectGroup.appendChild(repSelect);
    wrap.appendChild(repSelectGroup);

    // Observaciones iniciales
    const obsGroup = createElement("div", { className: "form-group" });
    obsGroup.appendChild(
      createElement("label", {
        className: "form-label",
        attrs: { for: "wizard-obs-input" },
        textContent: "Observaciones preliminares del administrado (opcional)"
      })
    );
    const obsInput = createElement("textarea", {
      id: "wizard-obs-input",
      className: "form-input",
      attrs: { rows: "2", maxlength: "500" }
    });
    obsInput.value = formData.observaciones || "";
    obsInput.addEventListener("input", () => {
      formData.observaciones = obsInput.value.trim();
    });
    obsGroup.appendChild(obsInput);
    wrap.appendChild(obsGroup);

    stepContentContainer.appendChild(wrap);
  }

  // Asegura la existencia del borrador en el backend antes de adjuntar documentos
  async function ensureDraftSaved() {
    if (!formData.tipoTramiteId || !formData.ciudadanoId || !formData.establecimientoId) {
      throw new Error("Complete primero los pasos 1, 2 y 3 para registrar el borrador.");
    }

    const payload = {
      tipoTramiteId: formData.tipoTramiteId,
      ciudadanoId: formData.ciudadanoId,
      establecimientoId: formData.establecimientoId,
      representanteId: formData.representanteId,
      observaciones: formData.observaciones || null
    };

    if (draftSolicitudId) {
      await actualizarSolicitud(draftSolicitudId, {
        tipoTramiteId: payload.tipoTramiteId,
        establecimientoId: payload.establecimientoId,
        representanteId: payload.representanteId,
        observaciones: payload.observaciones
      });
      return draftSolicitudId;
    }

    const created = await crearSolicitud(payload);
    draftSolicitudId = created.id;
    updateStepperUI();
    return draftSolicitudId;
  }

  // PASO 4: Carga y Verificación de Requisitos
  async function renderStep4() {
    const wrap = createElement("div");

    wrap.appendChild(
      createElement("h2", {
        className: "page-subtitle",
        textContent: "Paso 4: Verificación y Carga de Requisitos Documentales"
      })
    );

    const note = createElement("p", {
      attrs: { style: "color: var(--color-text-secondary); margin-bottom: 1.5rem;" },
      textContent: "Suba los archivos correspondientes a cada requisito obligatorio (.pdf, .jpg, .jpeg, .png — Máx. 10 MB). Todos los requisitos obligatorios deben estar cumplidos antes de presentar el expediente."
    });
    wrap.appendChild(note);

    const checklistContainer = createElement("div", { className: "requisitos-checklist-mount" });
    wrap.appendChild(checklistContainer);
    stepContentContainer.appendChild(wrap);

    // Cargar documentos actualizados del borrador
    if (draftSolicitudId) {
      try {
        currentDocumentos = await listarDocumentosPorSolicitud(draftSolicitudId);
      } catch (_e) {
        currentDocumentos = [];
      }
    }

    renderRequirementsChecklist({
      container: checklistContainer,
      solicitudId: draftSolicitudId,
      requisitos: currentRequisitos,
      documentos: currentDocumentos,
      canUpload: true,
      onEnsureSolicitudDraft: ensureDraftSaved,
      onDocumentsChanged: async (targetId) => {
        if (targetId) draftSolicitudId = targetId;
        currentDocumentos = await listarDocumentosPorSolicitud(draftSolicitudId);
        renderRequirementsChecklist({
          container: checklistContainer,
          solicitudId: draftSolicitudId,
          requisitos: currentRequisitos,
          documentos: currentDocumentos,
          canUpload: true,
          onEnsureSolicitudDraft: ensureDraftSaved,
          onDocumentsChanged: async () => {
            currentDocumentos = await listarDocumentosPorSolicitud(draftSolicitudId);
          }
        });
      }
    });
  }

  // PASO 5: Confirmación y Presentación
  function renderStep5() {
    const wrap = createElement("div");

    wrap.appendChild(
      createElement("h2", {
        className: "page-subtitle",
        textContent: "Paso 5: Revisión General y Presentación Formal del Expediente"
      })
    );

    const selectedTramite = tiposTramiteList.find((t) => t.id === formData.tipoTramiteId);
    const selectedCiudadano = ciudadanosList.find((c) => c.id === formData.ciudadanoId);
    const selectedEstab = establecimientosList.find((e) => e.id === formData.establecimientoId);
    const selectedRep = representantesList.find((r) => r.id === formData.representanteId);

    const compliance = evaluateRequirementsCompliance(currentRequisitos, currentDocumentos);

    const summaryCard = createElement("div", {
      className: "expediente-card",
      attrs: { style: "margin-bottom: 1.5rem;" }
    });

    const summaryGrid = createElement("div", { className: "expediente-data-grid" });

    const fields = [
      ["Trámite Municipal", selectedTramite ? `${selectedTramite.codigo} — ${selectedTramite.nombre}` : "—"],
      ["Administrado", selectedCiudadano ? `${selectedCiudadano.nombres} ${selectedCiudadano.apellidos} (${selectedCiudadano.tipoDocumento} ${selectedCiudadano.numeroDocumento})` : "—"],
      ["Establecimiento", selectedEstab ? `${selectedEstab.nombreComercial} (RUC: ${selectedEstab.ruc || "Sin RUC"})` : "—"],
      ["Dirección", selectedEstab ? selectedEstab.direccion : "—"],
      ["Representante Legal", selectedRep ? `${selectedRep.nombres} ${selectedRep.apellidos} (${selectedRep.numeroDocumento})` : "Directo (sin apoderado)"],
      ["Requisitos Obligatorios", `${compliance.cumplidos} de ${compliance.obligatorios} cumplidos (${compliance.allMandatoryMet ? "Completo" : "Incompleto"})`]
    ];

    for (const [k, v] of fields) {
      const item = createElement("div", { className: "expediente-data-item" });
      item.appendChild(createElement("div", { className: "expediente-data-item__label", textContent: k }));
      item.appendChild(createElement("div", { className: "expediente-data-item__value", textContent: v }));
      summaryGrid.appendChild(item);
    }

    summaryCard.appendChild(summaryGrid);
    wrap.appendChild(summaryCard);

    if (!compliance.allMandatoryMet) {
      wrap.appendChild(
        createElement("div", {
          className: "alert alert--danger",
          textContent: `Existen ${compliance.pendientesObligatorios} requisito(s) obligatorio(s) pendiente(s) de adjuntar. Debe adjuntar todos los documentos exigidos antes de presentar el expediente.`
        })
      );
    } else {
      wrap.appendChild(
        createElement("div", {
          className: "alert alert--success",
          textContent: "Todos los requisitos obligatorios han sido satisfechos. Al presionar 'Presentar Solicitud Formal', el sistema congelará la lista de requisitos y generará el Número Oficial de Expediente."
        })
      );
    }

    stepContentContainer.appendChild(wrap);
  }

  // --- BOTONES DE ACCIÓN DEL WIZARD ---
  cancelBtn.addEventListener("click", () => {
    if (typeof onCancel === "function") {
      onCancel();
    }
  });

  prevBtn.addEventListener("click", () => {
    if (currentStep > 1) {
      currentStep--;
      renderCurrentStep();
    }
  });

  nextBtn.addEventListener("click", async () => {
    try {
      if (currentStep === 1) {
        if (!formData.tipoTramiteId) {
          showToast("Seleccione un tipo de trámite para continuar.", "warning");
          return;
        }
        currentStep = 2;
        renderCurrentStep();
        return;
      }

      if (currentStep === 2) {
        if (!formData.ciudadanoId) {
          showToast("Seleccione el ciudadano administrado solicitante.", "warning");
          return;
        }
        currentStep = 3;
        renderCurrentStep();
        return;
      }

      if (currentStep === 3) {
        if (!formData.establecimientoId) {
          showToast("Seleccione el establecimiento comercial objeto de la licencia.", "warning");
          return;
        }
        // Auto-guardar borrador al pasar al paso 4
        nextBtn.disabled = true;
        nextBtn.textContent = "Guardando borrador...";
        await ensureDraftSaved();
        currentStep = 4;
        renderCurrentStep();
        return;
      }

      if (currentStep === 4) {
        currentStep = 5;
        renderCurrentStep();
        return;
      }

      if (currentStep === 5) {
        const compliance = evaluateRequirementsCompliance(currentRequisitos, currentDocumentos);
        if (!compliance.allMandatoryMet) {
          showToast("No puede presentar la solicitud sin cumplir todos los requisitos obligatorios.", "error");
          return;
        }

        nextBtn.disabled = true;
        nextBtn.textContent = "Presentando formalmente...";

        const result = await presentarSolicitud(draftSolicitudId, formData.observaciones);
        showToast(`Solicitud presentada con éxito. N° de Expediente: ${result.numeroExpediente}`, "success");

        if (typeof onFinished === "function") {
          onFinished(result);
        }
      }
    } catch (err) {
      showToast(err.message || "Error al procesar la solicitud.", "error");
    } finally {
      nextBtn.disabled = false;
      updateStepperUI();
    }
  });

  // Inicio de carga
  loadInitialCatalogs();
}
