/**
 * Módulo Visual de Tipos de Trámite Municipal (PROMPT 12 - Sección 10)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import {
  clearChildren,
  hideInlineAlert,
  setButtonLoading,
  showInlineAlert,
  showToast
} from "../ui.js";
import {
  createStatusBadge,
  createTableActions,
  formatDateTimePE,
  renderDataTable
} from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import { openModal } from "../components/modal.js";
import { openConfirmDialog } from "../components/confirm-dialog.js";
import {
  clearFormErrors,
  createFormField,
  setFieldError,
  validateRequiredText
} from "../components/form-validation.js";
import { listarRequisitos } from "../requisitos/requisitos-api.js";
import {
  actualizarTipoTramite,
  cambiarEstadoTipoTramite,
  configurarRequisitosDeTipoTramite,
  crearTipoTramite,
  listarRequisitosPorTipoTramite,
  listarTiposTramite
} from "./tramites-api.js";

/**
 * Renderiza la vista completa de Tipos de Trámite Municipal.
 */
export async function renderTiposTramiteView(currentUser, container) {
  if (!container) return;
  clearChildren(container);

  const isAdmin = hasRole(currentUser, ROLES.ADMINISTRADOR);

  const state = {
    filters: {
      codigo: "",
      nombre: "",
      activo: ""
    },
    page: 0,
    size: 10,
    loading: false
  };

  const wrapper = document.createElement("div");
  wrapper.className = "management-module";

  const toolbarCard = document.createElement("section");
  toolbarCard.className = "module-toolbar-card";
  toolbarCard.setAttribute("aria-label", "Filtros y acciones de tipos de trámite");

  const topRow = document.createElement("div");
  topRow.className = "module-toolbar-top";

  const titleGroup = document.createElement("div");
  titleGroup.className = "module-toolbar-title-group";

  const heading = document.createElement("h2");
  heading.className = "module-toolbar-heading";
  heading.textContent = "Catálogo de Tipos de Trámite Municipal";

  const caption = document.createElement("p");
  caption.className = "module-toolbar-caption";
  caption.textContent =
    "Procedimientos administrativos vigentes y requisitos documentales aplicables. Los expedientes ya presentados conservan su snapshot histórico.";

  titleGroup.append(heading, caption);

  const topActions = document.createElement("div");
  topActions.className = "module-toolbar-actions";

  if (isAdmin) {
    const newBtn = document.createElement("button");
    newBtn.type = "button";
    newBtn.className = "btn btn-primary";
    newBtn.textContent = "+ Nuevo tipo de trámite";
    newBtn.addEventListener("click", () => openTipoTramiteFormModal(null));
    topActions.appendChild(newBtn);
  }

  topRow.append(titleGroup, topActions);

  const filterForm = document.createElement("form");
  filterForm.className = "filter-form-grid";
  filterForm.noValidate = true;

  const codigoFilter = createFormField({
    id: "filtro-tramite-codigo",
    label: "Código",
    placeholder: "Ej. LIC-FUNC"
  });

  const nombreFilter = createFormField({
    id: "filtro-tramite-nombre",
    label: "Nombre del procedimiento",
    placeholder: "Ej. Licencia de Funcionamiento"
  });

  const estadoFilter = createFormField({
    id: "filtro-tramite-estado",
    label: "Estado",
    type: "select",
    options: [
      { value: "", label: "Todos" },
      { value: "true", label: "Activos" },
      { value: "false", label: "Inactivos" }
    ]
  });

  const filterButtons = document.createElement("div");
  filterButtons.className = "form-group filter-form-actions";

  const searchBtn = document.createElement("button");
  searchBtn.type = "submit";
  searchBtn.className = "btn btn-primary";
  searchBtn.textContent = "Buscar";

  const clearBtn = document.createElement("button");
  clearBtn.type = "button";
  clearBtn.className = "btn btn-secondary";
  clearBtn.textContent = "Limpiar filtros";

  filterButtons.append(searchBtn, clearBtn);
  filterForm.append(
    codigoFilter.group,
    nombreFilter.group,
    estadoFilter.group,
    filterButtons
  );

  toolbarCard.append(topRow, filterForm);

  const tableSection = document.createElement("section");
  const tableContainer = document.createElement("div");
  const paginationContainer = document.createElement("div");
  tableSection.append(tableContainer, paginationContainer);

  wrapper.append(toolbarCard, tableSection);
  container.appendChild(wrapper);

  const columns = [
    {
      label: "Código",
      render(row) {
        const s = document.createElement("span");
        s.className = "cell-mono";
        s.textContent = row.codigo || `#${row.id}`;
        return s;
      }
    },
    {
      label: "Nombre del trámite",
      render(row) {
        const s = document.createElement("span");
        s.className = "cell-primary";
        s.textContent = row.nombre || "—";
        return s;
      }
    },
    {
      label: "Descripción",
      render(row) {
        return row.descripcion || "Sin descripción adicional";
      }
    },
    {
      label: "Estado",
      render(row) {
        return createStatusBadge(Boolean(row.activo));
      }
    },
    {
      label: "Registro",
      render(row) {
        return formatDateTimePE(row.fechaCreacion);
      }
    },
    {
      label: "Acciones",
      render(row) {
        const actions = [
          {
            label: "Ver requisitos",
            title: `Consultar requisitos de ${row.nombre}`,
            onClick: () => openVerRequisitosTramiteModal(row)
          }
        ];

        if (isAdmin) {
          actions.push(
            {
              label: "Configurar requisitos",
              title: `Asignar requisitos a ${row.nombre}`,
              variant: "warning",
              onClick: () => openConfigurarRequisitosModal(row)
            },
            {
              label: "Editar",
              title: `Editar trámite ${row.codigo}`,
              variant: "primary",
              onClick: () => openTipoTramiteFormModal(row)
            },
            row.activo
              ? {
                  label: "Desactivar",
                  variant: "danger",
                  onClick: () => handleToggleTramiteStatus(row, false)
                }
              : {
                  label: "Activar",
                  variant: "success",
                  onClick: () => handleToggleTramiteStatus(row, true)
                }
          );
        }

        return createTableActions(actions);
      }
    }
  ];

  const loadTramitesPage = async () => {
    if (state.loading) return;
    state.loading = true;
    searchBtn.disabled = true;

    renderDataTable(tableContainer, {
      caption: "Tabla de tipos de trámite municipal",
      columns,
      loading: true
    });

    try {
      const response = await listarTiposTramite({
        codigo: state.filters.codigo,
        nombre: state.filters.nombre,
        activo: state.filters.activo,
        page: state.page,
        size: state.size
      });

      const rows = Array.isArray(response?.contenido) ? response.contenido : [];
      renderDataTable(tableContainer, {
        caption: "Tabla de tipos de trámite municipal",
        columns,
        rows,
        emptyTitle: "No se encontraron tipos de trámite",
        emptySubtitle: "Ajuste los filtros de búsqueda."
      });

      renderPagination(paginationContainer, {
        pagina: response?.pagina ?? 0,
        tamano: response?.tamano ?? state.size,
        totalElementos: response?.totalElementos ?? 0,
        totalPaginas: response?.totalPaginas ?? 0,
        primera: response?.primera ?? true,
        ultima: response?.ultima ?? true,
        onPageChange(nextPage) {
          state.page = nextPage;
          loadTramitesPage();
        },
        onSizeChange(nextSize) {
          state.size = nextSize;
          state.page = 0;
          loadTramitesPage();
        }
      });
    } catch (err) {
      renderDataTable(tableContainer, {
        caption: "Tabla de tipos de trámite municipal",
        columns,
        error: err.message || "Error al consultar tipos de trámite.",
        onRetry: () => loadTramitesPage()
      });
      clearChildren(paginationContainer);
    } finally {
      state.loading = false;
      searchBtn.disabled = false;
    }
  };

  // Modal de consulta de requisitos asociados a un trámite
  const openVerRequisitosTramiteModal = async (tramiteRow) => {
    try {
      const requisitos = await listarRequisitosPorTipoTramite(tramiteRow.id);
      openModal({
        title: `Requisitos del Trámite: ${tramiteRow.codigo}`,
        subtitle: tramiteRow.nombre,
        size: "lg",
        renderBody(bodyEl) {
          if (tramiteRow.descripcion) {
            const descBox = document.createElement("div");
            descBox.className = "legal-disclaimer-box";
            descBox.style.marginBottom = "1rem";
            descBox.textContent = tramiteRow.descripcion;
            bodyEl.appendChild(descBox);
          }

          const tableBox = document.createElement("div");
          renderDataTable(tableBox, {
            caption: "Requisitos configurados para el tipo de trámite",
            columns: [
              { label: "Orden", render: (r) => String(r.ordenVisualizacion ?? 1) },
              { label: "Código", render: (r) => r.codigo || `#${r.id}` },
              { label: "Requisito", key: "nombre" },
              { label: "Descripción", render: (r) => r.descripcion || "—" },
              {
                label: "Carácter",
                render(r) {
                  const b = document.createElement("span");
                  b.className = `badge ${r.obligatorio !== false ? "badge-warning" : "badge-neutral"}`;
                  b.textContent = r.obligatorio !== false ? "OBLIGATORIO" : "OPCIONAL";
                  return b;
                }
              }
            ],
            rows: Array.isArray(requisitos) ? requisitos : [],
            emptyTitle: "Sin requisitos asociados actualmente",
            emptySubtitle: "Este tipo de trámite no tiene requisitos documentales configurados."
          });

          bodyEl.appendChild(tableBox);
        }
      });
    } catch (err) {
      showToast({
        type: "error",
        title: "Error al consultar requisitos",
        message: err.message || "No se pudieron cargar los requisitos del trámite."
      });
    }
  };

  // Modal de registro o edición de Tipo de Trámite (Solo ADMINISTRADOR)
  const openTipoTramiteFormModal = (existingRow = null) => {
    const isEdit = Boolean(existingRow && existingRow.id);
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: isEdit ? `Editar Tipo de Trámite #${existingRow.id}` : "Registrar Tipo de Trámite",
      subtitle: isEdit ? existingRow.nombre : "Defina el código y denominación oficial del procedimiento.",
      size: "md",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const form = document.createElement("form");
        form.noValidate = true;
        formRef = form;

        const codigoField = createFormField({
          id: "tram-form-codigo",
          label: "Código del trámite",
          required: true,
          maxLength: 50,
          value: existingRow?.codigo || "",
          placeholder: "Ej. LIC-INDETERMINADA",
          hint: isEdit
            ? "Si el trámite ya fue utilizado en solicitudes, el backend impedirá cambiar su código."
            : "Identificador único institucional."
        });

        const nombreField = createFormField({
          id: "tram-form-nombre",
          label: "Nombre oficial del trámite",
          required: true,
          maxLength: 150,
          value: existingRow?.nombre || "",
          placeholder: "Ej. Licencia de Funcionamiento Indeterminada"
        });

        const descField = createFormField({
          id: "tram-form-descripcion",
          label: "Descripción administrativa",
          type: "textarea",
          maxLength: 2000,
          value: existingRow?.descripcion || "",
          placeholder: "Alcance y condiciones del procedimiento según TUPA municipal."
        });

        form.append(codigoField.group, nombreField.group, descField.group);

        form.addEventListener("submit", async (e) => {
          e.preventDefault();
          hideInlineAlert(alertBox);
          clearFormErrors(form);

          const codigo = codigoField.control.value.trim().toUpperCase();
          const nombre = nombreField.control.value.trim();
          const descripcion = descField.control.value.trim();

          let hasErr = false;
          const errCod = validateRequiredText(codigo, "Código del trámite", 50);
          if (errCod) {
            setFieldError(codigoField.control, errCod);
            hasErr = true;
          }
          const errNom = validateRequiredText(nombre, "Nombre del trámite", 150);
          if (errNom) {
            setFieldError(nombreField.control, errNom);
            hasErr = true;
          }
          if (hasErr) return;

          modalApi.setBusy(true);
          if (submitBtnRef) setButtonLoading(submitBtnRef, true, "Guardando...");

          try {
            const payload = {
              codigo,
              nombre,
              descripcion: descripcion || null,
              requiereInspeccion: true
            };
            if (isEdit) {
              await actualizarTipoTramite(existingRow.id, payload);
            } else {
              await crearTipoTramite(payload);
            }
            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: isEdit ? "Trámite actualizado" : "Trámite registrado",
              message: `El procedimiento ${codigo} se guardó correctamente.`
            });
            loadTramitesPage();
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(alertBox, err.message || "Error al guardar el tipo de trámite.", "error");
          }
        });

        bodyEl.append(alertBox, form);
      },
      renderFooter(footerEl, modalApi) {
        clearChildren(footerEl);
        const cancelBtn = document.createElement("button");
        cancelBtn.type = "button";
        cancelBtn.className = "btn btn-secondary";
        cancelBtn.textContent = "Cancelar";
        cancelBtn.addEventListener("click", () => modalApi.close());

        const saveBtn = document.createElement("button");
        saveBtn.type = "button";
        saveBtn.className = "btn btn-primary";
        saveBtn.textContent = isEdit ? "Guardar cambios" : "Registrar trámite";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  // Modal de configuración de requisitos aplicables a un Tipo de Trámite (PUT /api/tipos-tramite/{id}/requisitos)
  const openConfigurarRequisitosModal = async (tramiteRow) => {
    try {
      const [actuales, catalogoPage] = await Promise.all([
        listarRequisitosPorTipoTramite(tramiteRow.id),
        listarRequisitos({ activo: "true", page: 0, size: 100 })
      ]);

      const catalogo = Array.isArray(catalogoPage?.contenido) ? catalogoPage.contenido : [];
      const actualesMap = new Map();
      if (Array.isArray(actuales)) {
        for (const r of actuales) {
          actualesMap.set(Number(r.id), {
            obligatorio: r.obligatorio !== false,
            ordenVisualizacion: r.ordenVisualizacion ?? 1
          });
        }
      }

      let submitBtnRef = null;
      let formRef = null;

      openModal({
        title: `Configurar Requisitos — ${tramiteRow.codigo}`,
        subtitle: tramiteRow.nombre,
        size: "lg",
        renderBody(bodyEl, modalApi) {
          const alertBox = document.createElement("div");
          alertBox.hidden = true;
          alertBox.style.marginBottom = "1rem";

          const snapshotNotice = document.createElement("div");
          snapshotNotice.className = "legal-disclaimer-box";
          snapshotNotice.style.marginBottom = "1rem";
          snapshotNotice.textContent =
            "Respeto de versión histórica (Snapshot): Los cambios de requisitos se aplicarán únicamente a nuevas solicitudes o borradores. Las solicitudes ya presentadas conservan intactos los requisitos vigentes en su fecha de presentación.";

          const form = document.createElement("form");
          form.noValidate = true;
          formRef = form;

          const listContainer = document.createElement("div");
          listContainer.className = "requisitos-checklist";

          const itemControls = [];

          if (catalogo.length === 0) {
            const emptyP = document.createElement("p");
            emptyP.className = "text-muted";
            emptyP.textContent =
              "No existen requisitos activos en el catálogo. Registre primero requisitos en la pestaña 'Catálogo de Requisitos'.";
            listContainer.appendChild(emptyP);
          } else {
            catalogo.forEach((req, idx) => {
              const existing = actualesMap.get(Number(req.id));
              const rowDiv = document.createElement("div");
              rowDiv.className = "requisito-check-item";

              const leftLabel = document.createElement("label");
              leftLabel.style.display = "flex";
              leftLabel.style.alignItems = "flex-start";
              leftLabel.style.gap = "0.6rem";
              leftLabel.style.cursor = "pointer";
              leftLabel.style.flex = "1";

              const cbInclude = document.createElement("input");
              cbInclude.type = "checkbox";
              cbInclude.checked = Boolean(existing);
              cbInclude.style.marginTop = "0.2rem";

              const infoDiv = document.createElement("div");
              infoDiv.className = "requisito-check-info";
              const titleSpan = document.createElement("span");
              titleSpan.className = "requisito-check-title";
              titleSpan.textContent = `${req.codigo} — ${req.nombre}`;
              const descSpan = document.createElement("span");
              descSpan.className = "requisito-check-desc";
              descSpan.textContent = req.descripcion || "";
              infoDiv.append(titleSpan, descSpan);

              leftLabel.append(cbInclude, infoDiv);

              const rightOpts = document.createElement("div");
              rightOpts.style.display = "flex";
              rightOpts.style.alignItems = "center";
              rightOpts.style.gap = "0.6rem";

              const obligLabel = document.createElement("label");
              obligLabel.style.fontSize = "0.78rem";
              obligLabel.style.display = "inline-flex";
              obligLabel.style.alignItems = "center";
              obligLabel.style.gap = "0.25rem";
              const cbOblig = document.createElement("input");
              cbOblig.type = "checkbox";
              cbOblig.checked = existing ? existing.obligatorio : true;
              obligLabel.append(cbOblig, document.createTextNode("Obligatorio"));

              const ordenInput = document.createElement("input");
              ordenInput.type = "number";
              ordenInput.className = "form-control";
              ordenInput.style.width = "68px";
              ordenInput.min = "1";
              ordenInput.value = String(existing ? existing.ordenVisualizacion : idx + 1);
              ordenInput.setAttribute("aria-label", `Orden de visualización para ${req.nombre}`);

              rightOpts.append(obligLabel, ordenInput);
              rowDiv.append(leftLabel, rightOpts);
              listContainer.appendChild(rowDiv);

              itemControls.push({
                requisitoId: req.id,
                cbInclude,
                cbOblig,
                ordenInput
              });
            });
          }

          form.appendChild(listContainer);

          form.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideInlineAlert(alertBox);

            const selected = itemControls
              .filter((item) => item.cbInclude.checked)
              .map((item, idx) => ({
                requisitoId: Number(item.requisitoId),
                obligatorio: Boolean(item.cbOblig.checked),
                ordenVisualizacion: Number(item.ordenInput.value) || idx + 1
              }));

            modalApi.setBusy(true);
            if (submitBtnRef) setButtonLoading(submitBtnRef, true, "Guardando configuración...");

            try {
              await configurarRequisitosDeTipoTramite(tramiteRow.id, selected);
              modalApi.setBusy(false);
              modalApi.close();
              showToast({
                type: "success",
                title: "Requisitos actualizados",
                message: `Se asignaron ${selected.length} requisitos al trámite ${tramiteRow.codigo}.`
              });
            } catch (err) {
              modalApi.setBusy(false);
              if (submitBtnRef) setButtonLoading(submitBtnRef, false);
              showInlineAlert(
                alertBox,
                err.message || "Error al configurar los requisitos del trámite.",
                "error"
              );
            }
          });

          bodyEl.append(alertBox, snapshotNotice, form);
        },
        renderFooter(footerEl, modalApi) {
          clearChildren(footerEl);
          const cancelBtn = document.createElement("button");
          cancelBtn.type = "button";
          cancelBtn.className = "btn btn-secondary";
          cancelBtn.textContent = "Cancelar";
          cancelBtn.addEventListener("click", () => modalApi.close());

          const saveBtn = document.createElement("button");
          saveBtn.type = "button";
          saveBtn.className = "btn btn-primary";
          saveBtn.textContent = "Guardar asignación";
          submitBtnRef = saveBtn;
          saveBtn.addEventListener("click", () => {
            if (formRef) formRef.requestSubmit();
          });

          footerEl.append(cancelBtn, saveBtn);
        }
      });
    } catch (err) {
      showToast({
        type: "error",
        title: "Error",
        message: err.message || "No se pudo cargar el catálogo de requisitos."
      });
    }
  };

  const handleToggleTramiteStatus = async (row, targetActivo) => {
    await openConfirmDialog({
      title: targetActivo ? "Activar tipo de trámite" : "Desactivar tipo de trámite",
      message: targetActivo
        ? `¿Desea reactivar el trámite "${row.nombre}" (${row.codigo})?`
        : `¿Desea desactivar el trámite "${row.nombre}" (${row.codigo})?`,
      detail:
        "Los expedientes en curso conservarán su validez histórica; un trámite inactivo no podrá seleccionarse en nuevas solicitudes.",
      confirmText: targetActivo ? "Sí, activar" : "Sí, desactivar",
      variant: targetActivo ? "primary" : "danger",
      async onConfirm() {
        await cambiarEstadoTipoTramite(row.id, targetActivo);
        showToast({
          type: "success",
          title: targetActivo ? "Trámite activado" : "Trámite desactivado",
          message: `Estado actualizado para ${row.codigo}.`
        });
        loadTramitesPage();
      }
    });
  };

  filterForm.addEventListener("submit", (e) => {
    e.preventDefault();
    state.filters.codigo = codigoFilter.control.value.trim();
    state.filters.nombre = nombreFilter.control.value.trim();
    state.filters.activo = estadoFilter.control.value;
    state.page = 0;
    loadTramitesPage();
  });

  clearBtn.addEventListener("click", () => {
    filterForm.reset();
    state.filters = { codigo: "", nombre: "", activo: "" };
    state.page = 0;
    loadTramitesPage();
  });

  await loadTramitesPage();
}
