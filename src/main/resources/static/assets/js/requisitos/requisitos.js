/**
 * Módulo Visual de Gestión del Catálogo de Requisitos (PROMPT 12 - Sección 11)
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
import {
  actualizarRequisito,
  cambiarEstadoRequisito,
  crearRequisito,
  listarRequisitos,
  obtenerRequisito
} from "./requisitos-api.js";

/**
 * Renderiza la vista de Gestión de Requisitos Documentales.
 */
export async function renderRequisitosView(currentUser, container) {
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
  toolbarCard.setAttribute("aria-label", "Filtros y acciones de requisitos");

  const topRow = document.createElement("div");
  topRow.className = "module-toolbar-top";

  const titleGroup = document.createElement("div");
  titleGroup.className = "module-toolbar-title-group";

  const heading = document.createElement("h2");
  heading.className = "module-toolbar-heading";
  heading.textContent = "Catálogo Institucional de Requisitos Documentales";

  const caption = document.createElement("p");
  caption.className = "module-toolbar-caption";
  caption.textContent =
    "Requisitos que pueden asociarse a los tipos de trámite municipal. Solo el Administrador puede modificar el catálogo.";

  titleGroup.append(heading, caption);

  const topActions = document.createElement("div");
  topActions.className = "module-toolbar-actions";

  if (isAdmin) {
    const newBtn = document.createElement("button");
    newBtn.type = "button";
    newBtn.className = "btn btn-primary";
    newBtn.textContent = "+ Nuevo requisito";
    newBtn.addEventListener("click", () => openRequisitoFormModal(null));
    topActions.appendChild(newBtn);
  }

  topRow.append(titleGroup, topActions);

  const filterForm = document.createElement("form");
  filterForm.className = "filter-form-grid";
  filterForm.noValidate = true;

  const codigoFilter = createFormField({
    id: "filtro-req-codigo",
    label: "Código",
    placeholder: "Ej. REQ-DJ"
  });

  const nombreFilter = createFormField({
    id: "filtro-req-nombre",
    label: "Nombre del requisito",
    placeholder: "Ej. Declaración Jurada"
  });

  const estadoFilter = createFormField({
    id: "filtro-req-estado",
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
      label: "ID",
      render(row) {
        const s = document.createElement("span");
        s.className = "cell-mono";
        s.textContent = `#${row.id}`;
        return s;
      }
    },
    {
      label: "Código",
      render(row) {
        const s = document.createElement("span");
        s.className = "cell-mono";
        s.textContent = row.codigo || "—";
        return s;
      }
    },
    {
      label: "Nombre del requisito",
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
        return row.descripcion || "Sin descripción";
      }
    },
    {
      label: "Estado",
      render(row) {
        return createStatusBadge(Boolean(row.activo));
      }
    },
    {
      label: "Acciones",
      render(row) {
        const actions = [
          {
            label: "Consultar",
            title: `Consultar detalle de ${row.nombre}`,
            onClick: () => openRequisitoDetailModal(row.id)
          }
        ];

        if (isAdmin) {
          actions.push(
            {
              label: "Editar",
              variant: "primary",
              onClick: () => openRequisitoFormModal(row)
            },
            row.activo
              ? {
                  label: "Desactivar",
                  variant: "danger",
                  onClick: () => handleToggleRequisitoStatus(row, false)
                }
              : {
                  label: "Activar",
                  variant: "success",
                  onClick: () => handleToggleRequisitoStatus(row, true)
                }
          );
        }

        return createTableActions(actions);
      }
    }
  ];

  const loadRequisitosPage = async () => {
    if (state.loading) return;
    state.loading = true;
    searchBtn.disabled = true;

    renderDataTable(tableContainer, {
      caption: "Tabla de requisitos documentales",
      columns,
      loading: true
    });

    try {
      const response = await listarRequisitos({
        codigo: state.filters.codigo,
        nombre: state.filters.nombre,
        activo: state.filters.activo,
        page: state.page,
        size: state.size
      });

      const rows = Array.isArray(response?.contenido) ? response.contenido : [];
      renderDataTable(tableContainer, {
        caption: "Tabla de requisitos documentales",
        columns,
        rows,
        emptyTitle: "No se encontraron requisitos en el catálogo",
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
          loadRequisitosPage();
        },
        onSizeChange(nextSize) {
          state.size = nextSize;
          state.page = 0;
          loadRequisitosPage();
        }
      });
    } catch (err) {
      renderDataTable(tableContainer, {
        caption: "Tabla de requisitos documentales",
        columns,
        error: err.message || "Error al consultar requisitos.",
        onRetry: () => loadRequisitosPage()
      });
      clearChildren(paginationContainer);
    } finally {
      state.loading = false;
      searchBtn.disabled = false;
    }
  };

  const openRequisitoDetailModal = async (reqId) => {
    try {
      const r = await obtenerRequisito(reqId);
      openModal({
        title: `Detalle de Requisito #${r.id}`,
        subtitle: `${r.codigo} — ${r.nombre}`,
        size: "md",
        renderBody(bodyEl) {
          const grid = document.createElement("div");
          grid.className = "detail-grid";

          const fields = [
            ["ID", `#${r.id}`],
            ["Código", r.codigo || "—"],
            ["Nombre", r.nombre || "—", true],
            ["Descripción", r.descripcion || "Sin descripción", true],
            ["Estado", r.activo ? "ACTIVO" : "INACTIVO"]
          ];

          for (const [label, val, full] of fields) {
            const item = document.createElement("div");
            item.className = `detail-item${full ? " is-full" : ""}`;
            const l = document.createElement("span");
            l.className = "detail-label";
            l.textContent = label;
            const v = document.createElement("span");
            v.className = "detail-value";
            v.textContent = val;
            item.append(l, v);
            grid.appendChild(item);
          }

          bodyEl.appendChild(grid);
        }
      });
    } catch (err) {
      showToast({
        type: "error",
        title: "Error",
        message: err.message || "No se pudo consultar el requisito."
      });
    }
  };

  const openRequisitoFormModal = (existingRow = null) => {
    const isEdit = Boolean(existingRow && existingRow.id);
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: isEdit ? `Editar Requisito #${existingRow.id}` : "Registrar Nuevo Requisito",
      subtitle: isEdit ? existingRow.nombre : "Complete la definición oficial del requisito documental.",
      size: "md",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const form = document.createElement("form");
        form.noValidate = true;
        formRef = form;

        const codigoField = createFormField({
          id: "req-form-codigo",
          label: "Código del requisito",
          required: true,
          maxLength: 50,
          value: existingRow?.codigo || "",
          placeholder: "Ej. REQ-VIGENCIA-PODER",
          hint: isEdit
            ? "Si el requisito ya tiene documentos vinculados, el backend impedirá cambiar su código."
            : "Código único institucional."
        });

        const nombreField = createFormField({
          id: "req-form-nombre",
          label: "Nombre del requisito",
          required: true,
          maxLength: 200,
          value: existingRow?.nombre || "",
          placeholder: "Ej. Vigencia de poder del representante legal"
        });

        const descField = createFormField({
          id: "req-form-desc",
          label: "Descripción e instrucciones para el administrado",
          type: "textarea",
          maxLength: 2000,
          value: existingRow?.descripcion || "",
          placeholder: "Detalle las características que debe cumplir el documento adjunto."
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
          const errCod = validateRequiredText(codigo, "Código del requisito", 50);
          if (errCod) {
            setFieldError(codigoField.control, errCod);
            hasErr = true;
          }
          const errNom = validateRequiredText(nombre, "Nombre del requisito", 200);
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
              descripcion: descripcion || null
            };
            if (isEdit) {
              await actualizarRequisito(existingRow.id, payload);
            } else {
              await crearRequisito(payload);
            }
            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: isEdit ? "Requisito actualizado" : "Requisito registrado",
              message: `El requisito ${codigo} fue guardado correctamente.`
            });
            loadRequisitosPage();
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(alertBox, err.message || "Error al guardar el requisito.", "error");
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
        saveBtn.textContent = isEdit ? "Guardar cambios" : "Registrar requisito";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  const handleToggleRequisitoStatus = async (row, targetActivo) => {
    await openConfirmDialog({
      title: targetActivo ? "Activar requisito" : "Desactivar requisito",
      message: targetActivo
        ? `¿Desea reactivar el requisito "${row.nombre}" (${row.codigo})?`
        : `¿Desea desactivar el requisito "${row.nombre}" (${row.codigo})?`,
      detail:
        "El requisito no se elimina físicamente para conservar la trazabilidad de documentos adjuntos en expedientes históricos.",
      confirmText: targetActivo ? "Sí, activar" : "Sí, desactivar",
      variant: targetActivo ? "primary" : "danger",
      async onConfirm() {
        await cambiarEstadoRequisito(row.id, targetActivo);
        showToast({
          type: "success",
          title: targetActivo ? "Requisito activado" : "Requisito desactivado",
          message: `Estado actualizado para ${row.codigo}.`
        });
        loadRequisitosPage();
      }
    });
  };

  filterForm.addEventListener("submit", (e) => {
    e.preventDefault();
    state.filters.codigo = codigoFilter.control.value.trim();
    state.filters.nombre = nombreFilter.control.value.trim();
    state.filters.activo = estadoFilter.control.value;
    state.page = 0;
    loadRequisitosPage();
  });

  clearBtn.addEventListener("click", () => {
    filterForm.reset();
    state.filters = { codigo: "", nombre: "", activo: "" };
    state.page = 0;
    loadRequisitosPage();
  });

  await loadRequisitosPage();
}
