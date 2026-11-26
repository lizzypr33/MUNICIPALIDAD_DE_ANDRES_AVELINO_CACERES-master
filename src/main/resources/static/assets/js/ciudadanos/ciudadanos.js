/**
 * Módulo Visual de Gestión de Ciudadanos (PROMPT 11 - Secciones 8, 9, 10 y 25)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { navigateToSection } from "../router.js";
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
  maskDocumentNumber,
  renderDataTable
} from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import { openModal } from "../components/modal.js";
import { openConfirmDialog } from "../components/confirm-dialog.js";
import {
  TIPO_DOCUMENTO_OPTIONS,
  clearFormErrors,
  createFormField,
  setFieldError,
  validateDocumentoIdentidad,
  validateEmailFormat,
  validatePhoneFormat,
  validateRequiredText
} from "../components/form-validation.js";
import {
  actualizarCiudadano,
  cambiarEstadoCiudadano,
  crearCiudadano,
  listarCiudadanos,
  listarEstablecimientosDeCiudadano,
  listarSolicitudesDeCiudadano,
  obtenerCiudadano
} from "./ciudadanos-api.js";

/**
 * Renderiza el módulo de Gestión de Ciudadanos dentro del área principal.
 */
export async function renderCiudadanosModule(currentUser, container) {
  if (!container) return;
  clearChildren(container);

  const isStaff = hasRole(
    currentUser,
    ROLES.ADMINISTRADOR,
    ROLES.FUNCIONARIO,
    ROLES.SUPERVISOR
  );
  const canWrite = hasRole(currentUser, ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO);
  const isAdmin = hasRole(currentUser, ROLES.ADMINISTRADOR);

  if (!isStaff) {
    await renderCiudadanoSelfServiceView(currentUser, container);
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
    loading: false,
    maskDocuments: hasRole(currentUser, ROLES.SUPERVISOR) && !canWrite
  };

  const wrapper = document.createElement("div");
  wrapper.className = "management-module";

  // Barra superior de herramientas y filtros
  const toolbarCard = document.createElement("section");
  toolbarCard.className = "module-toolbar-card";
  toolbarCard.setAttribute("aria-label", "Filtros y acciones de ciudadanos");

  const topRow = document.createElement("div");
  topRow.className = "module-toolbar-top";

  const titleGroup = document.createElement("div");
  titleGroup.className = "module-toolbar-title-group";

  const heading = document.createElement("h2");
  heading.className = "module-toolbar-heading";
  heading.textContent = "Padrón Municipal de Ciudadanos y Administrados";

  const caption = document.createElement("p");
  caption.className = "module-toolbar-caption";
  caption.textContent =
    "Gestión de titulares vinculados a establecimientos comerciales y expedientes de licencias.";

  titleGroup.append(heading, caption);

  const topActions = document.createElement("div");
  topActions.className = "module-toolbar-actions";

  // Control de privacidad de documentos (Ley N.º 29733)
  const privacyLabel = document.createElement("label");
  privacyLabel.className = "privacy-toggle-bar";
  const privacyCheckbox = document.createElement("input");
  privacyCheckbox.type = "checkbox";
  privacyCheckbox.checked = state.maskDocuments;
  privacyCheckbox.addEventListener("change", () => {
    state.maskDocuments = privacyCheckbox.checked;
    loadCiudadanosPage();
  });
  const privacyText = document.createElement("span");
  privacyText.textContent = "Enmascarar documentos en tabla";
  privacyLabel.append(privacyCheckbox, privacyText);
  topActions.appendChild(privacyLabel);

  if (canWrite) {
    const newBtn = document.createElement("button");
    newBtn.type = "button";
    newBtn.id = "btn-nuevo-ciudadano";
    newBtn.className = "btn btn-primary";
    newBtn.textContent = "+ Nuevo ciudadano";
    newBtn.addEventListener("click", () => openCiudadanoFormModal(null));
    topActions.appendChild(newBtn);
  }

  topRow.append(titleGroup, topActions);

  // Formulario de filtros
  const filterForm = document.createElement("form");
  filterForm.className = "filter-form-grid";
  filterForm.noValidate = true;

  const tipoDocFilter = createFormField({
    id: "filtro-ciud-tipodoc",
    label: "Tipo de documento",
    type: "select",
    options: [{ value: "", label: "Todos" }, ...TIPO_DOCUMENTO_OPTIONS]
  });

  const numDocFilter = createFormField({
    id: "filtro-ciud-numdoc",
    label: "Número de documento",
    placeholder: "Ej. 45678912"
  });

  const nombresFilter = createFormField({
    id: "filtro-ciud-nombres",
    label: "Nombres",
    placeholder: "Ej. Juan Carlos"
  });

  const apellidosFilter = createFormField({
    id: "filtro-ciud-apellidos",
    label: "Apellidos",
    placeholder: "Ej. Huamán Flores"
  });

  const estadoFilter = createFormField({
    id: "filtro-ciud-estado",
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
  tableSection.setAttribute("aria-label", "Listado paginado de ciudadanos");
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
      label: "Tipo doc.",
      render(row) {
        const b = document.createElement("span");
        b.className = "badge badge-neutral";
        b.textContent = row.tipoDocumento || "—";
        return b;
      }
    },
    {
      label: "N.º documento",
      render(row) {
        const s = document.createElement("span");
        s.className = "cell-mono";
        s.textContent = maskDocumentNumber(row.numeroDocumento, state.maskDocuments);
        return s;
      }
    },
    {
      label: "Nombres",
      key: "nombres"
    },
    {
      label: "Apellidos",
      key: "apellidos"
    },
    {
      label: "Teléfono",
      render(row) {
        return row.telefono || "—";
      }
    },
    {
      label: "Correo",
      render(row) {
        return row.email || "—";
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
            label: "Detalles",
            title: `Ver información completa de ${row.nombres} ${row.apellidos}`,
            onClick: () => openCiudadanoDetailModal(row.id, canWrite)
          }
        ];

        if (canWrite) {
          actions.push({
            label: "Editar",
            title: `Editar ciudadano #${row.id}`,
            variant: "primary",
            onClick: () => openCiudadanoFormModal(row)
          });
        }

        actions.push(
          {
            label: "Establecimientos",
            title: `Consultar establecimientos de ${row.nombres} ${row.apellidos}`,
            onClick: () => openCiudadanoDetailModal(row.id, canWrite, "establecimientos")
          },
          {
            label: "Solicitudes",
            title: `Consultar solicitudes vinculadas a ${row.nombres} ${row.apellidos}`,
            onClick: () => openCiudadanoDetailModal(row.id, canWrite, "solicitudes")
          }
        );

        if (isAdmin) {
          actions.push(
            row.activo
              ? {
                  label: "Desactivar",
                  title: `Desactivar ciudadano #${row.id}`,
                  variant: "danger",
                  onClick: () => handleToggleCiudadanoStatus(row, false)
                }
              : {
                  label: "Activar",
                  title: `Reactivar ciudadano #${row.id}`,
                  variant: "success",
                  onClick: () => handleToggleCiudadanoStatus(row, true)
                }
          );
        }

        return createTableActions(actions);
      }
    }
  ];

  const loadCiudadanosPage = async () => {
    if (state.loading) return;
    state.loading = true;
    searchBtn.disabled = true;

    renderDataTable(tableContainer, {
      caption: "Tabla de ciudadanos registrados",
      columns,
      loading: true
    });

    try {
      const response = await listarCiudadanos({
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
        caption: "Tabla de ciudadanos registrados",
        columns,
        rows,
        emptyTitle: "No se encontraron ciudadanos registrados con esos criterios",
        emptySubtitle: "Verifique el número de documento o registre un nuevo ciudadano."
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
          loadCiudadanosPage();
        },
        onSizeChange(nextSize) {
          state.size = nextSize;
          state.page = 0;
          loadCiudadanosPage();
        }
      });
    } catch (err) {
      renderDataTable(tableContainer, {
        caption: "Tabla de ciudadanos registrados",
        columns,
        error: err.message || "Error al consultar ciudadanos.",
        onRetry: () => loadCiudadanosPage()
      });
      clearChildren(paginationContainer);
    } finally {
      state.loading = false;
      searchBtn.disabled = false;
    }
  };

  // Modal de Registro o Edición de Ciudadano (Sección 9)
  const openCiudadanoFormModal = (existingRow = null) => {
    const isEdit = Boolean(existingRow && existingRow.id);
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: isEdit ? `Editar Ciudadano #${existingRow.id}` : "Registrar Nuevo Ciudadano",
      subtitle: isEdit
        ? `Actualizando datos de ${existingRow.nombres} ${existingRow.apellidos}`
        : "Complete los datos de identificación y contacto del ciudadano.",
      size: "lg",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const disclaimer = document.createElement("div");
        disclaimer.className = "legal-disclaimer-box";
        disclaimer.style.marginBottom = "1rem";
        disclaimer.textContent =
          "Nota legal: La validación de longitud y formato del documento (ej. 8 dígitos para DNI) verifica la consistencia del dato ingresado, pero no constituye verificación biométrica ni consulta en línea con RENIEC. Los datos personales se tratan conforme a la Ley N.º 29733.";

        const form = document.createElement("form");
        form.className = "form-grid-2";
        form.noValidate = true;
        formRef = form;

        const tipoDocField = createFormField({
          id: "ciud-form-tipodoc",
          label: "Tipo de documento",
          type: "select",
          required: true,
          value: existingRow?.tipoDocumento || "DNI",
          options: TIPO_DOCUMENTO_OPTIONS
        });

        const numDocField = createFormField({
          id: "ciud-form-numdoc",
          label: "Número de documento",
          required: true,
          maxLength: 20,
          value: existingRow?.numeroDocumento || "",
          placeholder: "Ej. 45678912",
          hint: isEdit
            ? "Solo podrá modificarse si el ciudadano aún no tiene expedientes vinculados."
            : "DNI: 8 dígitos | RUC: 11 dígitos | CE: 9-12 caracteres."
        });

        const nombresField = createFormField({
          id: "ciud-form-nombres",
          label: "Nombres",
          required: true,
          maxLength: 100,
          value: existingRow?.nombres || "",
          placeholder: "Ej. Rosa María"
        });

        const apellidosField = createFormField({
          id: "ciud-form-apellidos",
          label: "Apellidos (paterno y materno)",
          required: true,
          maxLength: 100,
          value: existingRow?.apellidos || "",
          placeholder: "Ej. Cárdenas Quispe"
        });

        const telefonoField = createFormField({
          id: "ciud-form-telefono",
          label: "Teléfono / Celular",
          maxLength: 20,
          value: existingRow?.telefono || "",
          placeholder: "Ej. 966543210"
        });

        const emailField = createFormField({
          id: "ciud-form-email",
          label: "Correo electrónico",
          type: "email",
          maxLength: 150,
          value: existingRow?.email || "",
          placeholder: "ciudadano@correo.pe"
        });

        const direccionField = createFormField({
          id: "ciud-form-direccion",
          label: "Dirección domiciliaria",
          maxLength: 255,
          value: existingRow?.direccion || "",
          placeholder: "Ej. Av. Mariscal Cáceres N.º 450, Andrés Avelino Cáceres Dorregaray",
          fullWidth: true
        });

        form.append(
          tipoDocField.group,
          numDocField.group,
          nombresField.group,
          apellidosField.group,
          telefonoField.group,
          emailField.group,
          direccionField.group
        );

        form.addEventListener("submit", async (e) => {
          e.preventDefault();
          hideInlineAlert(alertBox);
          clearFormErrors(form);

          const tipoDocumento = tipoDocField.control.value;
          const numeroDocumento = numDocField.control.value.trim().toUpperCase();
          const nombres = nombresField.control.value.trim();
          const apellidos = apellidosField.control.value.trim();
          const telefono = telefonoField.control.value.trim();
          const email = emailField.control.value.trim();
          const direccion = direccionField.control.value.trim();

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
              telefono: telefono || null,
              email: email || null,
              direccion: direccion || null
            };

            if (isEdit) {
              await actualizarCiudadano(existingRow.id, payload);
            } else {
              await crearCiudadano(payload);
            }

            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: isEdit ? "Ciudadano actualizado" : "Ciudadano registrado",
              message: `${nombres} ${apellidos} (${tipoDocumento} ${numeroDocumento}) guardado correctamente.`
            });
            if (!isEdit) state.page = 0;
            loadCiudadanosPage();
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(alertBox, err.message || "Error al guardar el ciudadano.", "error");
          }
        });

        bodyEl.append(alertBox, disclaimer, form);
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
        saveBtn.textContent = isEdit ? "Guardar cambios" : "Registrar ciudadano";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  // Confirmación para activar o desactivar ciudadano (PATCH /api/ciudadanos/{id}/estado)
  const handleToggleCiudadanoStatus = async (row, targetActivo) => {
    await openConfirmDialog({
      title: targetActivo ? "Activar registro de ciudadano" : "Desactivar registro de ciudadano",
      message: targetActivo
        ? `¿Desea reactivar al ciudadano ${row.nombres} ${row.apellidos} (${row.tipoDocumento} ${row.numeroDocumento})?`
        : `¿Desea desactivar al ciudadano ${row.nombres} ${row.apellidos} (${row.tipoDocumento} ${row.numeroDocumento})?`,
      detail:
        "El registro no se elimina físicamente para preservar la trazabilidad histórica de expedientes y licencias municipales.",
      confirmText: targetActivo ? "Sí, activar ciudadano" : "Sí, desactivar ciudadano",
      variant: targetActivo ? "primary" : "danger",
      async onConfirm() {
        try {
          await cambiarEstadoCiudadano(row.id, targetActivo);
          showToast({
            type: "success",
            title: targetActivo ? "Ciudadano activado" : "Ciudadano desactivado",
            message: `Estado actualizado para ${row.nombres} ${row.apellidos}.`
          });
          loadCiudadanosPage();
        } catch (err) {
          showToast({
            type: "error",
            title: "Error",
            message: err.message || "No se pudo cambiar el estado del ciudadano."
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
    loadCiudadanosPage();
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
    loadCiudadanosPage();
  });

  await loadCiudadanosPage();
}

/**
 * Abre la vista modal de detalles completos de un ciudadano, incluyendo sus establecimientos
 * y solicitudes vinculadas (Sección 10).
 */
export async function openCiudadanoDetailModal(ciudadanoId, canNavigateModules = true, focusSection = "") {
  try {
    const [c, establecimientosRes, solicitudesRes] = await Promise.allSettled([
      obtenerCiudadano(ciudadanoId),
      listarEstablecimientosDeCiudadano(ciudadanoId),
      listarSolicitudesDeCiudadano(ciudadanoId, 8)
    ]);

    if (c.status !== "fulfilled" || !c.value) {
      throw new Error(c.reason?.message || "No fue posible obtener los datos del ciudadano.");
    }

    const ciudadano = c.value;
    const establecimientos =
      establecimientosRes.status === "fulfilled" && Array.isArray(establecimientosRes.value)
        ? establecimientosRes.value
        : [];
    const solicitudes =
      solicitudesRes.status === "fulfilled" && Array.isArray(solicitudesRes.value?.contenido)
        ? solicitudesRes.value.contenido
        : [];

    openModal({
      title: `Ficha de Ciudadano #${ciudadano.id}`,
      subtitle: `${ciudadano.nombres} ${ciudadano.apellidos} · ${ciudadano.tipoDocumento} ${ciudadano.numeroDocumento}`,
      size: "xl",
      renderBody(bodyEl, modalApi) {
        const grid = document.createElement("div");
        grid.className = "detail-grid";

        const items = [
          ["ID de ciudadano", `#${ciudadano.id}`],
          ["Estado", ciudadano.activo ? "ACTIVO" : "INACTIVO"],
          ["Tipo de documento", ciudadano.tipoDocumento || "—"],
          ["Número de documento", ciudadano.numeroDocumento || "—"],
          ["Nombres", ciudadano.nombres || "—"],
          ["Apellidos", ciudadano.apellidos || "—"],
          ["Teléfono", ciudadano.telefono || "No registrado"],
          ["Correo electrónico", ciudadano.email || "No registrado"],
          ["Dirección domiciliaria", ciudadano.direccion || "No registrada", true],
          ["Fecha de registro", formatDateTimePE(ciudadano.fechaRegistro)]
        ];

        for (const [label, val, full] of items) {
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

        // Bloque de establecimientos vinculados
        const estBlock = document.createElement("div");
        estBlock.className = "related-section-block";
        const estHeader = document.createElement("div");
        estHeader.className = "related-section-header";
        const estTitle = document.createElement("h3");
        estTitle.className = "related-section-title";
        estTitle.textContent = `Establecimientos Comerciales Vinculados (${establecimientos.length})`;
        estHeader.appendChild(estTitle);

        if (canNavigateModules) {
          const goEstBtn = document.createElement("button");
          goEstBtn.type = "button";
          goEstBtn.className = "btn-table-action is-primary";
          goEstBtn.textContent = "Ir al módulo de Establecimientos";
          goEstBtn.addEventListener("click", () => {
            sessionStorage.setItem("preload_establecimientos_ciudadano_id", String(ciudadano.id));
            modalApi.close();
            navigateToSection("establecimientos");
          });
          estHeader.appendChild(goEstBtn);
        }

        const estTableBox = document.createElement("div");
        renderDataTable(estTableBox, {
          caption: "Establecimientos vinculados al ciudadano",
          columns: [
            { label: "ID", render: (r) => `#${r.id}` },
            { label: "Nombre comercial", key: "nombreComercial" },
            { label: "RUC", render: (r) => r.ruc || "—" },
            { label: "Giro", key: "giroNegocio" },
            { label: "Dirección", key: "direccion" },
            { label: "Estado", render: (r) => createStatusBadge(Boolean(r.activo)) }
          ],
          rows: establecimientos,
          emptyTitle: "Sin establecimientos comerciales registrados",
          emptySubtitle: "Este ciudadano aún no figura como titular de establecimientos."
        });

        estBlock.append(estHeader, estTableBox);
        bodyEl.appendChild(estBlock);

        // Bloque de solicitudes vinculadas
        const solBlock = document.createElement("div");
        solBlock.className = "related-section-block";
        const solHeader = document.createElement("div");
        solHeader.className = "related-section-header";
        const solTitle = document.createElement("h3");
        solTitle.className = "related-section-title";
        solTitle.textContent = `Expedientes / Solicitudes Vinculadas (${solicitudes.length})`;
        solHeader.appendChild(solTitle);

        const solTableBox = document.createElement("div");
        renderDataTable(solTableBox, {
          caption: "Solicitudes vinculadas al ciudadano",
          columns: [
            { label: "Expediente", key: "numeroExpediente" },
            { label: "Trámite", key: "tipoTramiteNombre" },
            { label: "Establecimiento", key: "establecimientoNombreComercial" },
            { label: "Estado", key: "estado" },
            { label: "Presentación", render: (r) => formatDateTimePE(r.fechaPresentacion) }
          ],
          rows: solicitudes,
          emptyTitle: "Sin solicitudes registradas",
          emptySubtitle: "La gestión completa de expedientes y requisitos estará disponible en el módulo de Solicitudes."
        });

        solBlock.append(solHeader, solTableBox);
        bodyEl.appendChild(solBlock);

        if (focusSection === "establecimientos") {
          window.setTimeout(() => estBlock.scrollIntoView({ behavior: "smooth" }), 50);
        } else if (focusSection === "solicitudes") {
          window.setTimeout(() => solBlock.scrollIntoView({ behavior: "smooth" }), 50);
        }
      }
    });
  } catch (err) {
    showToast({
      type: "error",
      title: "Error al consultar detalles",
      message: err.message || "No se pudo obtener la ficha del ciudadano."
    });
  }
}

/**
 * Vista de autoservicio para usuarios con rol exclusivo CIUDADANO (Sección 22 y 25).
 * Permite consultar y actualizar únicamente su propio registro vinculado sin exponer datos de terceros.
 */
async function renderCiudadanoSelfServiceView(currentUser, container) {
  const card = document.createElement("section");
  card.className = "card";

  const header = document.createElement("div");
  header.className = "card-header";
  const title = document.createElement("h2");
  title.className = "card-title";
  title.textContent = "Mi Ficha de Ciudadano / Administrado";
  header.appendChild(title);

  const body = document.createElement("div");
  body.className = "card-body";

  const infoAlert = document.createElement("div");
  infoAlert.className = "legal-disclaimer-box";
  infoAlert.style.marginBottom = "1rem";
  infoAlert.textContent =
    "En cumplimiento de la Ley N.º 29733 (Ley de Protección de Datos Personales), su cuenta de ciudadano solo puede consultar y actualizar su propio registro vinculado.";
  body.appendChild(infoAlert);

  const contentBox = document.createElement("div");
  body.appendChild(contentBox);
  card.append(header, body);
  container.appendChild(card);

  // Intentar localizar el ciudadanoId vinculado desde sus solicitudes o consulta directa
  try {
    const solPage = await listarSolicitudesDeCiudadano("", 1);
    const firstSol = Array.isArray(solPage?.contenido) ? solPage.contenido[0] : null;
    if (firstSol && firstSol.ciudadanoId) {
      const c = await obtenerCiudadano(firstSol.ciudadanoId);
      renderOwnCiudadanoCard(c, contentBox);
      return;
    }
  } catch (_err) {
    // Continúa a consulta por ID propio
  }

  const promptP = document.createElement("p");
  promptP.textContent =
    "Ingrese el código de su ficha de ciudadano asignada en ventanilla municipal para consultar sus datos:";
  promptP.style.marginBottom = "0.75rem";

  const lookupForm = document.createElement("form");
  lookupForm.className = "filter-form-grid";
  const idField = createFormField({
    id: "self-ciudadano-id",
    label: "ID de Ficha de Ciudadano",
    type: "number",
    required: true,
    min: 1,
    placeholder: "Ej. 1"
  });
  const btnWrap = document.createElement("div");
  btnWrap.className = "form-group filter-form-actions";
  const lookupBtn = document.createElement("button");
  lookupBtn.type = "submit";
  lookupBtn.className = "btn btn-primary";
  lookupBtn.textContent = "Consultar mi ficha";
  btnWrap.appendChild(lookupBtn);
  lookupForm.append(idField.group, btnWrap);

  const resultBox = document.createElement("div");
  resultBox.style.marginTop = "1rem";

  lookupForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const idVal = Number(idField.control.value);
    if (!idVal || idVal < 1) {
      setFieldError(idField.control, "Ingrese un ID numérico válido.");
      return;
    }
    try {
      const c = await obtenerCiudadano(idVal);
      renderOwnCiudadanoCard(c, resultBox);
    } catch (err) {
      clearChildren(resultBox);
      const errAlert = document.createElement("div");
      errAlert.className = "alert alert-error";
      errAlert.textContent =
        err.message || "No tiene permisos para acceder a ese registro o no existe.";
      resultBox.appendChild(errAlert);
    }
  });

  contentBox.append(promptP, lookupForm, resultBox);
}

function renderOwnCiudadanoCard(c, targetEl) {
  clearChildren(targetEl);
  const btnRow = document.createElement("div");
  btnRow.style.marginTop = "0.85rem";
  const detailBtn = document.createElement("button");
  detailBtn.type = "button";
  detailBtn.className = "btn btn-primary btn-sm";
  detailBtn.textContent = "Ver mi ficha completa y establecimientos";
  detailBtn.addEventListener("click", () => openCiudadanoDetailModal(c.id, true));
  btnRow.appendChild(detailBtn);
  targetEl.appendChild(btnRow);
}
