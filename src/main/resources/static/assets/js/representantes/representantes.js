/**
 * Módulo Visual de Gestión de Representantes (PROMPT 11 - Secciones 11 y 12)
 */
import { APP_CONFIG, ROLES } from "../config.js";
import { api } from "../api.js";
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
  TIPO_DOCUMENTO_OPTIONS,
  clearFormErrors,
  createFormField,
  createRemoteSearchSelect,
  setFieldError,
  validateDocumentoIdentidad,
  validateEmailFormat,
  validatePhoneFormat,
  validateRequiredText
} from "../components/form-validation.js";
import {
  actualizarRepresentante,
  cambiarEstadoRepresentante,
  crearRepresentante,
  listarRepresentantes,
  obtenerRepresentante,
  vincularRepresentanteAEstablecimiento
} from "./representantes-api.js";

/**
 * Busca establecimientos activos por nombre comercial o RUC para vincular representantes.
 */
async function buscarEstablecimientosActivosSelector(queryText = "") {
  const q = (queryText || "").trim();
  const isRuc = /^\d{8,11}$/.test(q);
  const qs = new URLSearchParams({
    activo: "true",
    page: "0",
    size: "10"
  });
  if (q) {
    if (isRuc) qs.set("ruc", q);
    else qs.set("nombreComercial", q);
  }
  const page = await api.get(`${APP_CONFIG.endpoints.establecimientos}?${qs.toString()}`);
  const items = Array.isArray(page?.contenido) ? page.contenido : [];
  return items.map((e) => ({
    id: e.id,
    primaryText: e.nombreComercial,
    secondaryText: `Titular: ${e.ciudadanoNombreCompleto || "—"}${e.ruc ? ` · RUC ${e.ruc}` : ""}`
  }));
}

/**
 * Renderiza el módulo de Gestión de Representantes dentro del contenedor principal.
 */
export async function renderRepresentantesModule(currentUser, container) {
  if (!container) return;
  clearChildren(container);

  const canView = hasRole(
    currentUser,
    ROLES.ADMINISTRADOR,
    ROLES.FUNCIONARIO,
    ROLES.SUPERVISOR
  );
  const canWrite = hasRole(currentUser, ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO);
  const isAdmin = hasRole(currentUser, ROLES.ADMINISTRADOR);

  if (!canView) {
    const alert = document.createElement("div");
    alert.className = "alert alert-error";
    alert.setAttribute("role", "alert");
    alert.textContent = "No cuenta con permisos para acceder al módulo de representantes.";
    container.appendChild(alert);
    return;
  }

  const state = {
    filters: {
      tipoDocumento: "",
      numeroDocumento: "",
      nombres: "",
      apellidos: "",
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
  toolbarCard.setAttribute("aria-label", "Filtros y acciones de representantes");

  const topRow = document.createElement("div");
  topRow.className = "module-toolbar-top";

  const titleGroup = document.createElement("div");
  titleGroup.className = "module-toolbar-title-group";

  const heading = document.createElement("h2");
  heading.className = "module-toolbar-heading";
  heading.textContent = "Padrón de Representantes Vinculados a Trámites y Establecimientos";

  const caption = document.createElement("p");
  caption.className = "module-toolbar-caption";
  caption.textContent =
    "Registro, consulta, edición y vinculación de representantes autorizados ante la municipalidad.";

  titleGroup.append(heading, caption);

  const topActions = document.createElement("div");
  topActions.className = "module-toolbar-actions";

  if (canWrite) {
    const newBtn = document.createElement("button");
    newBtn.type = "button";
    newBtn.id = "btn-nuevo-representante";
    newBtn.className = "btn btn-primary";
    newBtn.textContent = "+ Nuevo representante";
    newBtn.addEventListener("click", () => openRepresentanteFormModal(null));
    topActions.appendChild(newBtn);
  }

  topRow.append(titleGroup, topActions);

  // Formulario de filtros
  const filterForm = document.createElement("form");
  filterForm.className = "filter-form-grid";
  filterForm.noValidate = true;

  const tipoDocFilter = createFormField({
    id: "filtro-rep-tipodoc",
    label: "Tipo de documento",
    type: "select",
    options: [{ value: "", label: "Todos" }, ...TIPO_DOCUMENTO_OPTIONS]
  });

  const numDocFilter = createFormField({
    id: "filtro-rep-numdoc",
    label: "N.º de documento",
    placeholder: "Ej. 41234567"
  });

  const nombresFilter = createFormField({
    id: "filtro-rep-nombres",
    label: "Nombres",
    placeholder: "Ej. Luis Alberto"
  });

  const apellidosFilter = createFormField({
    id: "filtro-rep-apellidos",
    label: "Apellidos",
    placeholder: "Ej. Paredes Soto"
  });

  const estadoFilter = createFormField({
    id: "filtro-rep-estado",
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
    tipoDocFilter.group,
    numDocFilter.group,
    nombresFilter.group,
    apellidosFilter.group,
    estadoFilter.group,
    filterButtons
  );

  toolbarCard.append(topRow, filterForm);

  const tableSection = document.createElement("section");
  tableSection.setAttribute("aria-label", "Listado paginado de representantes");
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
      label: "Nombre completo",
      render(row) {
        const s = document.createElement("span");
        s.className = "cell-primary";
        s.textContent = `${row.nombres || ""} ${row.apellidos || ""}`.trim() || "—";
        return s;
      }
    },
    {
      label: "Documento",
      render(row) {
        const s = document.createElement("span");
        s.className = "cell-mono";
        s.textContent = `${row.tipoDocumento || ""} ${row.numeroDocumento || ""}`.trim();
        return s;
      }
    },
    {
      label: "Tipo de representación / Cargo",
      render(row) {
        return row.cargo || "Representante acreditado";
      }
    },
    {
      label: "Datos de contacto",
      render(row) {
        const div = document.createElement("div");
        const primary = document.createElement("span");
        primary.textContent = row.email || "Sin correo";
        div.appendChild(primary);
        if (row.telefono) {
          const sec = document.createElement("span");
          sec.className = "cell-secondary";
          sec.textContent = `Tel: ${row.telefono}`;
          div.appendChild(sec);
        }
        return div;
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
            title: `Consultar detalle de ${row.nombres} ${row.apellidos}`,
            onClick: () => openRepresentanteDetailModal(row.id)
          }
        ];

        if (canWrite) {
          actions.push(
            {
              label: "Editar",
              title: `Editar representante #${row.id}`,
              variant: "primary",
              onClick: () => openRepresentanteFormModal(row)
            },
            {
              label: "Vincular",
              title: row.activo
                ? `Vincular a establecimiento comercial`
                : "Solo se pueden vincular representantes activos",
              variant: "warning",
              disabled: !row.activo,
              onClick: () => openVincularRepresentanteModal(row)
            }
          );
        }

        if (isAdmin) {
          actions.push(
            row.activo
              ? {
                  label: "Desactivar",
                  title: `Desactivar representante #${row.id}`,
                  variant: "danger",
                  onClick: () => handleToggleRepresentanteStatus(row, false)
                }
              : {
                  label: "Activar",
                  title: `Reactivar representante #${row.id}`,
                  variant: "success",
                  onClick: () => handleToggleRepresentanteStatus(row, true)
                }
          );
        }

        return createTableActions(actions);
      }
    }
  ];

  const loadRepresentantesPage = async () => {
    if (state.loading) return;
    state.loading = true;
    searchBtn.disabled = true;

    renderDataTable(tableContainer, {
      caption: "Tabla de representantes municipales",
      columns,
      loading: true
    });

    try {
      const response = await listarRepresentantes({
        tipoDocumento: state.filters.tipoDocumento,
        numeroDocumento: state.filters.numeroDocumento,
        nombres: state.filters.nombres,
        apellidos: state.filters.apellidos,
        activo: state.filters.activo,
        page: state.page,
        size: state.size
      });

      const rows = Array.isArray(response?.contenido) ? response.contenido : [];
      renderDataTable(tableContainer, {
        caption: "Tabla de representantes municipales",
        columns,
        rows,
        emptyTitle: "No se encontraron representantes con esos filtros",
        emptySubtitle: "Limpie los filtros o registre un nuevo representante."
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
          loadRepresentantesPage();
        },
        onSizeChange(nextSize) {
          state.size = nextSize;
          state.page = 0;
          loadRepresentantesPage();
        }
      });
    } catch (err) {
      renderDataTable(tableContainer, {
        caption: "Tabla de representantes municipales",
        columns,
        error: err.message || "Error al consultar representantes.",
        onRetry: () => loadRepresentantesPage()
      });
      clearChildren(paginationContainer);
    } finally {
      state.loading = false;
      searchBtn.disabled = false;
    }
  };

  // Modal de Consulta / Detalle de Representante (Sección 11)
  const openRepresentanteDetailModal = async (repId) => {
    try {
      const rep = await obtenerRepresentante(repId);
      // Consultar establecimientos vinculados donde este representante figura asignado
      let linkedEsts = [];
      try {
        const estPage = await api.get(`${APP_CONFIG.endpoints.establecimientos}?page=0&size=50`);
        const allItems = Array.isArray(estPage?.contenido) ? estPage.contenido : [];
        linkedEsts = allItems.filter((e) => Number(e.representanteId) === Number(rep.id));
      } catch (_e) {
        linkedEsts = [];
      }

      openModal({
        title: `Ficha de Representante #${rep.id}`,
        subtitle: `${rep.nombres} ${rep.apellidos} · ${rep.tipoDocumento} ${rep.numeroDocumento}`,
        size: "lg",
        renderBody(bodyEl) {
          const legalNotice = document.createElement("div");
          legalNotice.className = "legal-disclaimer-box";
          legalNotice.style.marginBottom = "1rem";
          legalNotice.textContent =
            "Nota legal: El registro administrativo de un representante no constituye por sí solo acreditación legal suficiente ni otorga acceso automático a datos privados del titular sin la documentación sustentatoria del expediente.";

          const grid = document.createElement("div");
          grid.className = "detail-grid";

          const fields = [
            ["ID de registro", `#${rep.id}`],
            ["Estado", rep.activo ? "ACTIVO" : "INACTIVO"],
            ["Tipo de documento", rep.tipoDocumento || "—"],
            ["Número de documento", rep.numeroDocumento || "—"],
            ["Nombres", rep.nombres || "—"],
            ["Apellidos", rep.apellidos || "—"],
            ["Cargo / Tipo de representación", rep.cargo || "Representante acreditado"],
            ["Teléfono", rep.telefono || "No registrado"],
            ["Correo electrónico", rep.email || "No registrado", true]
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

          const relBlock = document.createElement("div");
          relBlock.className = "related-section-block";
          const relTitle = document.createElement("h3");
          relTitle.className = "related-section-title";
          relTitle.style.marginBottom = "0.55rem";
          relTitle.textContent = `Establecimientos y Titulares Representados (${linkedEsts.length})`;

          const relTable = document.createElement("div");
          renderDataTable(relTable, {
            caption: "Establecimientos vinculados al representante",
            columns: [
              { label: "ID", render: (r) => `#${r.id}` },
              { label: "Establecimiento", key: "nombreComercial" },
              { label: "RUC", render: (r) => r.ruc || "—" },
              { label: "Titular representado", key: "ciudadanoNombreCompleto" },
              { label: "Estado", render: (r) => createStatusBadge(Boolean(r.activo)) }
            ],
            rows: linkedEsts,
            emptyTitle: "Sin establecimientos vinculados actualmente",
            emptySubtitle: "Puede utilizar la acción 'Vincular' para asociar este representante a un establecimiento."
          });

          relBlock.append(relTitle, relTable);
          bodyEl.append(legalNotice, grid, relBlock);
        }
      });
    } catch (err) {
      showToast({
        type: "error",
        title: "Error al consultar representante",
        message: err.message || "No se pudo obtener la información."
      });
    }
  };

  // Modal de Registro o Edición de Representante (Sección 12)
  const openRepresentanteFormModal = (existingRow = null) => {
    const isEdit = Boolean(existingRow && existingRow.id);
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: isEdit ? `Editar Representante #${existingRow.id}` : "Registrar Nuevo Representante",
      subtitle: isEdit
        ? `Actualizando datos de ${existingRow.nombres} ${existingRow.apellidos}`
        : "Complete los datos del representante y opcionalmente vincúlelo a un establecimiento.",
      size: "lg",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const legalBox = document.createElement("div");
        legalBox.className = "legal-disclaimer-box";
        legalBox.style.marginBottom = "1rem";
        legalBox.textContent =
          "Importante: Registrar un representante permite asociarlo a trámites y establecimientos comerciales, pero la acreditación legal (vigencia de poder o carta poder) se verifica documentalmente en el expediente municipal.";

        const form = document.createElement("form");
        form.className = "form-grid-2";
        form.noValidate = true;
        formRef = form;

        const tipoDocField = createFormField({
          id: "rep-form-tipodoc",
          label: "Tipo de documento",
          type: "select",
          required: true,
          value: existingRow?.tipoDocumento || "DNI",
          options: TIPO_DOCUMENTO_OPTIONS
        });

        const numDocField = createFormField({
          id: "rep-form-numdoc",
          label: "Número de documento",
          required: true,
          maxLength: 20,
          value: existingRow?.numeroDocumento || "",
          placeholder: "Ej. 41234567"
        });

        const nombresField = createFormField({
          id: "rep-form-nombres",
          label: "Nombres",
          required: true,
          maxLength: 100,
          value: existingRow?.nombres || "",
          placeholder: "Ej. Jorge Luis"
        });

        const apellidosField = createFormField({
          id: "rep-form-apellidos",
          label: "Apellidos",
          required: true,
          maxLength: 100,
          value: existingRow?.apellidos || "",
          placeholder: "Ej. Vargas Palomino"
        });

        const cargoField = createFormField({
          id: "rep-form-cargo",
          label: "Cargo / Tipo de representación",
          maxLength: 100,
          value: existingRow?.cargo || "",
          placeholder: "Ej. Gerente General / Apoderado Legal",
          hint: "Indique la calidad en la que representa al titular o entidad."
        });

        const telefonoField = createFormField({
          id: "rep-form-telefono",
          label: "Teléfono",
          maxLength: 20,
          value: existingRow?.telefono || "",
          placeholder: "Ej. 966987654"
        });

        const emailField = createFormField({
          id: "rep-form-email",
          label: "Correo electrónico",
          type: "email",
          maxLength: 150,
          value: existingRow?.email || "",
          placeholder: "representante@empresa.pe",
          fullWidth: true
        });

        // Selector dinámico opcional para vincular inmediatamente con un establecimiento (Sección 12 y 15)
        const estSelect = createRemoteSearchSelect({
          id: "rep-form-establecimiento-vincular",
          label: "Vincular a establecimiento comercial (Opcional)",
          placeholder: "Buscar establecimiento activo por nombre comercial o RUC...",
          hint: "Si selecciona un establecimiento, este representante quedará asignado a dicho local y su titular.",
          fullWidth: true,
          searchFn: buscarEstablecimientosActivosSelector
        });

        form.append(
          tipoDocField.group,
          numDocField.group,
          nombresField.group,
          apellidosField.group,
          cargoField.group,
          telefonoField.group,
          emailField.group,
          estSelect.group
        );

        form.addEventListener("submit", async (e) => {
          e.preventDefault();
          hideInlineAlert(alertBox);
          clearFormErrors(form);

          const tipoDocumento = tipoDocField.control.value;
          const numeroDocumento = numDocField.control.value.trim().toUpperCase();
          const nombres = nombresField.control.value.trim();
          const apellidos = apellidosField.control.value.trim();
          const cargo = cargoField.control.value.trim();
          const telefono = telefonoField.control.value.trim();
          const email = emailField.control.value.trim();
          const selectedEstId = estSelect.getValue();

          let hasErr = false;
          const errDoc = validateDocumentoIdentidad(tipoDocumento, numeroDocumento);
          if (errDoc) {
            setFieldError(numDocField.control, errDoc);
            hasErr = true;
          }
          const errNom = validateRequiredText(nombres, "Nombres", 100);
          if (errNom) {
            setFieldError(nombresField.control, errNom);
            hasErr = true;
          }
          const errApe = validateRequiredText(apellidos, "Apellidos", 100);
          if (errApe) {
            setFieldError(apellidosField.control, errApe);
            hasErr = true;
          }
          const errTel = validatePhoneFormat(telefono, false);
          if (errTel) {
            setFieldError(telefonoField.control, errTel);
            hasErr = true;
          }
          const errMail = validateEmailFormat(email, false);
          if (errMail) {
            setFieldError(emailField.control, errMail);
            hasErr = true;
          }

          if (hasErr) return;

          modalApi.setBusy(true);
          if (submitBtnRef) setButtonLoading(submitBtnRef, true, "Guardando...");

          try {
            const payload = {
              tipoDocumento,
              numeroDocumento,
              nombres,
              apellidos,
              cargo: cargo || null,
              telefono: telefono || null,
              email: email || null
            };

            const savedRep = isEdit
              ? await actualizarRepresentante(existingRow.id, payload)
              : await crearRepresentante(payload);

            if (selectedEstId && savedRep && savedRep.id) {
              await vincularRepresentanteAEstablecimiento(selectedEstId, savedRep.id);
            }

            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: isEdit ? "Representante actualizado" : "Representante registrado",
              message: `${nombres} ${apellidos} guardado correctamente${selectedEstId ? " y vinculado al establecimiento seleccionado" : ""}.`
            });
            if (!isEdit) state.page = 0;
            loadRepresentantesPage();
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(alertBox, err.message || "Error al guardar el representante.", "error");
          }
        });

        bodyEl.append(alertBox, legalBox, form);
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
        saveBtn.textContent = isEdit ? "Guardar cambios" : "Registrar representante";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  // Modal de vinculación de representante con un establecimiento comercial y su titular (Sección 11 y 12)
  const openVincularRepresentanteModal = (repRow) => {
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: `Vincular Representante #${repRow.id}`,
      subtitle: `${repRow.nombres} ${repRow.apellidos} (${repRow.tipoDocumento} ${repRow.numeroDocumento})`,
      size: "md",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const form = document.createElement("form");
        form.noValidate = true;
        formRef = form;

        const estSelect = createRemoteSearchSelect({
          id: "vincular-rep-est-id",
          label: "Seleccione el establecimiento comercial activo",
          required: true,
          placeholder: "Buscar establecimiento por nombre comercial o RUC...",
          hint: "El establecimiento seleccionado actualizará su representante legal asignado.",
          searchFn: buscarEstablecimientosActivosSelector
        });

        form.appendChild(estSelect.group);

        form.addEventListener("submit", async (e) => {
          e.preventDefault();
          hideInlineAlert(alertBox);

          const estId = estSelect.getValue();
          if (!estId) {
            estSelect.setError("Debe seleccionar un establecimiento activo para realizar la vinculación.");
            return;
          }

          modalApi.setBusy(true);
          if (submitBtnRef) setButtonLoading(submitBtnRef, true, "Vinculando...");

          try {
            const updatedEst = await vincularRepresentanteAEstablecimiento(estId, repRow.id);
            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: "Vinculación completada",
              message: `Se asignó a ${repRow.nombres} ${repRow.apellidos} como representante de "${updatedEst.nombreComercial}".`
            });
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(
              alertBox,
              err.message || "No fue posible vincular el representante con el establecimiento.",
              "error"
            );
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
        saveBtn.textContent = "Confirmar vinculación";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  // Confirmación para activar o desactivar representante
  const handleToggleRepresentanteStatus = async (row, targetActivo) => {
    await openConfirmDialog({
      title: targetActivo ? "Activar representante" : "Desactivar representante",
      message: targetActivo
        ? `¿Desea reactivar al representante ${row.nombres} ${row.apellidos}?`
        : `¿Desea desactivar al representante ${row.nombres} ${row.apellidos}?`,
      detail:
        "Un representante inactivo no podrá ser asignado a nuevos establecimientos ni expedientes, conservándose su historial administrativo.",
      confirmText: targetActivo ? "Sí, activar" : "Sí, desactivar",
      variant: targetActivo ? "primary" : "danger",
      async onConfirm() {
        try {
          await cambiarEstadoRepresentante(row.id, targetActivo);
          showToast({
            type: "success",
            title: targetActivo ? "Representante activado" : "Representante desactivado",
            message: `Estado actualizado para ${row.nombres} ${row.apellidos}.`
          });
          loadRepresentantesPage();
        } catch (err) {
          showToast({
            type: "error",
            title: "Error",
            message: err.message || "No se pudo cambiar el estado del representante."
          });
          throw err;
        }
      }
    });
  };

  filterForm.addEventListener("submit", (e) => {
    e.preventDefault();
    state.filters.tipoDocumento = tipoDocFilter.control.value;
    state.filters.numeroDocumento = numDocFilter.control.value.trim();
    state.filters.nombres = nombresFilter.control.value.trim();
    state.filters.apellidos = apellidosFilter.control.value.trim();
    state.filters.activo = estadoFilter.control.value;
    state.page = 0;
    loadRepresentantesPage();
  });

  clearBtn.addEventListener("click", () => {
    filterForm.reset();
    state.filters = {
      tipoDocumento: "",
      numeroDocumento: "",
      nombres: "",
      apellidos: "",
      activo: ""
    };
    state.page = 0;
    loadRepresentantesPage();
  });

  await loadRepresentantesPage();
}
