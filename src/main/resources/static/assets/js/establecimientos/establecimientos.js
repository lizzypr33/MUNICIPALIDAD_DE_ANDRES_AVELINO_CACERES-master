/**
 * Módulo Visual de Gestión de Establecimientos Comerciales (PROMPT 11 - Secciones 13, 14 y 15)
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
  createRemoteSearchSelect,
  setFieldError,
  validateEmailFormat,
  validatePhoneFormat,
  validatePositiveNumber,
  validateRequiredText,
  validateRucPeruano
} from "../components/form-validation.js";
import {
  buscarCiudadanosActivosSelector,
  listarSolicitudesDeCiudadano
} from "../ciudadanos/ciudadanos-api.js";
import { openCiudadanoDetailModal } from "../ciudadanos/ciudadanos.js";
import { buscarRepresentantesActivosSelector } from "../representantes/representantes-api.js";
import {
  actualizarEstablecimiento,
  cambiarEstadoEstablecimiento,
  crearEstablecimiento,
  listarEstablecimientos,
  listarEstablecimientosPorCiudadano,
  listarSolicitudesDeEstablecimiento,
  obtenerEstablecimiento
} from "./establecimientos-api.js";

/**
 * Renderiza el módulo completo de Gestión de Establecimientos Comerciales.
 */
export async function renderEstablecimientosModule(currentUser, container) {
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
    await renderCiudadanoEstablecimientosView(currentUser, container);
    return;
  }

  const preloadedCiudadanoId = sessionStorage.getItem("preload_establecimientos_ciudadano_id") || "";
  if (preloadedCiudadanoId) {
    sessionStorage.removeItem("preload_establecimientos_ciudadano_id");
  }

  const state = {
    filters: {
      nombreComercial: "",
      ruc: "",
      ciudadanoId: preloadedCiudadanoId,
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
  toolbarCard.setAttribute("aria-label", "Filtros y acciones de establecimientos");

  const topRow = document.createElement("div");
  topRow.className = "module-toolbar-top";

  const titleGroup = document.createElement("div");
  titleGroup.className = "module-toolbar-title-group";

  const heading = document.createElement("h2");
  heading.className = "module-toolbar-heading";
  heading.textContent = "Padrón de Establecimientos Comerciales del Distrito";

  const caption = document.createElement("p");
  caption.className = "module-toolbar-caption";
  caption.textContent =
    "Administración de locales comerciales, giros de negocio, área, ubicación, titulares y representantes.";

  titleGroup.append(heading, caption);

  const topActions = document.createElement("div");
  topActions.className = "module-toolbar-actions";

  if (canWrite) {
    const newBtn = document.createElement("button");
    newBtn.type = "button";
    newBtn.id = "btn-nuevo-establecimiento";
    newBtn.className = "btn btn-primary";
    newBtn.textContent = "+ Nuevo establecimiento";
    newBtn.addEventListener("click", () => openEstablecimientoFormModal(null));
    topActions.appendChild(newBtn);
  }

  topRow.append(titleGroup, topActions);

  // Formulario de filtros
  const filterForm = document.createElement("form");
  filterForm.className = "filter-form-grid";
  filterForm.noValidate = true;

  const nombreFilter = createFormField({
    id: "filtro-est-nombre",
    label: "Nombre comercial",
    placeholder: "Ej. Botica Virgen del Carmen"
  });

  const rucFilter = createFormField({
    id: "filtro-est-ruc",
    label: "RUC (11 dígitos)",
    maxLength: 11,
    placeholder: "Ej. 20601234567"
  });

  const titularIdFilter = createFormField({
    id: "filtro-est-ciudadanoid",
    label: "ID Titular",
    type: "number",
    min: 1,
    value: preloadedCiudadanoId,
    placeholder: "Ej. 1"
  });

  const estadoFilter = createFormField({
    id: "filtro-est-estado",
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
    nombreFilter.group,
    rucFilter.group,
    titularIdFilter.group,
    estadoFilter.group,
    filterButtons
  );

  toolbarCard.append(topRow, filterForm);

  const tableSection = document.createElement("section");
  tableSection.setAttribute("aria-label", "Listado paginado de establecimientos");
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
      label: "Nombre comercial",
      render(row) {
        const div = document.createElement("div");
        const primary = document.createElement("span");
        primary.className = "cell-primary";
        primary.textContent = row.nombreComercial || "—";
        div.appendChild(primary);
        if (row.areaMetrosCuadrados) {
          const sec = document.createElement("span");
          sec.className = "cell-secondary";
          sec.textContent = `Área: ${row.areaMetrosCuadrados} m² · Pisos: ${row.numeroPisos ?? 1}`;
          div.appendChild(sec);
        }
        return div;
      }
    },
    {
      label: "Razón social",
      render(row) {
        return row.razonSocial || "—";
      }
    },
    {
      label: "RUC",
      render(row) {
        const s = document.createElement("span");
        s.className = "cell-mono";
        s.textContent = row.ruc || "—";
        return s;
      }
    },
    {
      label: "Dirección",
      render(row) {
        const div = document.createElement("div");
        const d = document.createElement("span");
        d.textContent = row.direccion || "—";
        div.appendChild(d);
        if (row.referencia) {
          const ref = document.createElement("span");
          ref.className = "cell-secondary";
          ref.textContent = `Ref: ${row.referencia}`;
          div.appendChild(ref);
        }
        return div;
      }
    },
    {
      label: "Giro comercial",
      key: "giroNegocio"
    },
    {
      label: "Titular",
      render(row) {
        const div = document.createElement("div");
        const t = document.createElement("span");
        t.className = "cell-primary";
        t.textContent = row.ciudadanoNombreCompleto || `#${row.ciudadanoId || "—"}`;
        div.appendChild(t);
        if (row.ciudadanoNumeroDocumento) {
          const doc = document.createElement("span");
          doc.className = "cell-secondary";
          doc.textContent = `Doc: ${row.ciudadanoNumeroDocumento}`;
          div.appendChild(doc);
        }
        if (row.representanteNombreCompleto) {
          const rep = document.createElement("span");
          rep.className = "cell-secondary";
          rep.textContent = `Rep: ${row.representanteNombreCompleto}`;
          div.appendChild(rep);
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
            label: "Ver",
            title: `Ver detalle de ${row.nombreComercial}`,
            onClick: () => openEstablecimientoDetailModal(row.id)
          }
        ];

        if (canWrite) {
          actions.push({
            label: "Editar",
            title: `Editar establecimiento #${row.id}`,
            variant: "primary",
            onClick: () => openEstablecimientoFormModal(row, false)
          });
        }

        if (row.ciudadanoId) {
          actions.push({
            label: "Titular",
            title: `Consultar ficha del titular ${row.ciudadanoNombreCompleto || ""}`,
            onClick: () => openCiudadanoDetailModal(row.ciudadanoId, true)
          });
        }

        actions.push({
          label: "Solicitudes",
          title: `Consultar solicitudes vinculadas al establecimiento`,
          onClick: () => openEstablecimientoDetailModal(row.id, "solicitudes")
        });

        if (isAdmin) {
          actions.push(
            row.activo
              ? {
                  label: "Desactivar",
                  title: `Desactivar establecimiento #${row.id}`,
                  variant: "danger",
                  onClick: () => handleToggleEstablecimientoStatus(row, false)
                }
              : {
                  label: "Activar",
                  title: `Reactivar establecimiento #${row.id}`,
                  variant: "success",
                  onClick: () => handleToggleEstablecimientoStatus(row, true)
                }
          );
        }

        return createTableActions(actions);
      }
    }
  ];

  const loadEstablecimientosPage = async () => {
    if (state.loading) return;
    state.loading = true;
    searchBtn.disabled = true;

    renderDataTable(tableContainer, {
      caption: "Tabla de establecimientos comerciales",
      columns,
      loading: true
    });

    try {
      const response = await listarEstablecimientos({
        nombreComercial: state.filters.nombreComercial,
        ruc: state.filters.ruc,
        ciudadanoId: state.filters.ciudadanoId,
        activo: state.filters.activo,
        page: state.page,
        size: state.size
      });

      const rows = Array.isArray(response?.contenido) ? response.contenido : [];
      renderDataTable(tableContainer, {
        caption: "Tabla de establecimientos comerciales",
        columns,
        rows,
        emptyTitle: "No se encontraron establecimientos comerciales",
        emptySubtitle: "Ajuste los filtros de búsqueda o registre un nuevo establecimiento."
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
          loadEstablecimientosPage();
        },
        onSizeChange(nextSize) {
          state.size = nextSize;
          state.page = 0;
          loadEstablecimientosPage();
        }
      });
    } catch (err) {
      renderDataTable(tableContainer, {
        caption: "Tabla de establecimientos comerciales",
        columns,
        error: err.message || "Error al consultar establecimientos.",
        onRetry: () => loadEstablecimientosPage()
      });
      clearChildren(paginationContainer);
    } finally {
      state.loading = false;
      searchBtn.disabled = false;
    }
  };

  // Modal de Registro o Edición de Establecimiento (Sección 14)
  const openEstablecimientoFormModal = (existingRow = null, isCiudadanoSelfEdit = false) => {
    const isEdit = Boolean(existingRow && existingRow.id);
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: isEdit ? `Editar Establecimiento #${existingRow.id}` : "Registrar Nuevo Establecimiento",
      subtitle: isEdit
        ? `Actualizando datos de "${existingRow.nombreComercial}"`
        : "Complete los datos comerciales, ubicación, área y titularidad del establecimiento.",
      size: "xl",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const legalDisclaimer = document.createElement("div");
        legalDisclaimer.className = "legal-disclaimer-box";
        legalDisclaimer.style.marginBottom = "1rem";
        legalDisclaimer.textContent =
          "Nota técnica y legal: La validación de 11 dígitos y dígito verificador del RUC comprueba la estructura tributaria peruana (Módulo 11), pero no constituye consulta en línea con SUNAT ni acredita estado activo o habido. El giro comercial se registra según la declaración del administrado sujeta a evaluación municipal.";

        const form = document.createElement("form");
        form.className = "form-grid-2";
        form.noValidate = true;
        formRef = form;

        const nombreField = createFormField({
          id: "est-form-nombre",
          label: "Nombre comercial",
          required: true,
          maxLength: 200,
          value: existingRow?.nombreComercial || "",
          placeholder: "Ej. Restaurante Tradiciones de Ayacucho"
        });

        const razonField = createFormField({
          id: "est-form-razon",
          label: "Razón social (si corresponde)",
          maxLength: 200,
          value: existingRow?.razonSocial || "",
          placeholder: "Ej. Inversiones Gastronómicas Huamanga S.A.C."
        });

        const rucField = createFormField({
          id: "est-form-ruc",
          label: "RUC (11 dígitos)",
          maxLength: 11,
          value: existingRow?.ruc || "",
          disabled: isCiudadanoSelfEdit,
          placeholder: "Ej. 20601234567",
          hint: "Prefijos válidos: 10, 15, 17 o 20 con dígito verificador Módulo 11."
        });

        const giroField = createFormField({
          id: "est-form-giro",
          label: "Giro comercial / Actividad económica",
          required: true,
          maxLength: 150,
          value: existingRow?.giroNegocio || "",
          placeholder: "Ej. Venta de abarrotes / Restaurante / Botica",
          hint: "Campo descriptivo de actividad comercial (el backend no restringe a un catálogo cerrado)."
        });

        const direccionField = createFormField({
          id: "est-form-direccion",
          label: "Dirección del establecimiento",
          required: true,
          maxLength: 255,
          value: existingRow?.direccion || "",
          placeholder: "Ej. Av. El Ejército N.º 320, Andrés Avelino Cáceres Dorregaray",
          fullWidth: true
        });

        const referenciaField = createFormField({
          id: "est-form-referencia",
          label: "Referencia de ubicación",
          maxLength: 255,
          value: existingRow?.referencia || "",
          placeholder: "Ej. A media cuadra del Parque Principal",
          fullWidth: true
        });

        const areaField = createFormField({
          id: "est-form-area",
          label: "Área del establecimiento (m²)",
          type: "number",
          required: true,
          step: "0.01",
          min: "0.01",
          value: existingRow?.areaMetrosCuadrados ?? "",
          placeholder: "Ej. 65.50"
        });

        const pisosField = createFormField({
          id: "est-form-pisos",
          label: "Número de pisos ocupados",
          type: "number",
          required: true,
          step: "1",
          min: "1",
          value: existingRow?.numeroPisos ?? "1",
          placeholder: "Ej. 1"
        });

        const telefonoField = createFormField({
          id: "est-form-telefono",
          label: "Teléfono del local",
          maxLength: 20,
          value: existingRow?.telefono || "",
          placeholder: "Ej. 966112233"
        });

        const emailField = createFormField({
          id: "est-form-email",
          label: "Correo electrónico del local",
          type: "email",
          maxLength: 150,
          value: existingRow?.email || "",
          placeholder: "contacto@comercio.pe"
        });

        // Selector dinámico remoto para el Ciudadano Titular (Sección 15)
        const titularSelect = createRemoteSearchSelect({
          id: "est-form-ciudadano-id",
          label: "Ciudadano titular del establecimiento",
          required: true,
          disabled: isCiudadanoSelfEdit,
          initialSelected:
            existingRow && existingRow.ciudadanoId
              ? {
                  id: existingRow.ciudadanoId,
                  label: `${existingRow.ciudadanoNombreCompleto || `Ciudadano #${existingRow.ciudadanoId}`}${existingRow.ciudadanoNumeroDocumento ? ` (${existingRow.ciudadanoNumeroDocumento})` : ""}`
                }
              : null,
          placeholder: "Buscar ciudadano titular por DNI, RUC o nombres...",
          hint: isEdit
            ? "Solo puede cambiarse la titularidad si el establecimiento no tiene expedientes vinculados."
            : "Busque y seleccione un ciudadano activo del padrón municipal.",
          fullWidth: true,
          searchFn: buscarCiudadanosActivosSelector
        });

        // Selector dinámico remoto para el Representante Legal (Sección 15)
        const repSelect = createRemoteSearchSelect({
          id: "est-form-representante-id",
          label: "Representante legal acreditado (Opcional)",
          required: false,
          disabled: isCiudadanoSelfEdit,
          initialSelected:
            existingRow && existingRow.representanteId
              ? {
                  id: existingRow.representanteId,
                  label:
                    existingRow.representanteNombreCompleto ||
                    `Representante #${existingRow.representanteId}`
                }
              : null,
          placeholder: "Buscar representante activo por documento o nombre...",
          hint: "Seleccione un representante si el titular actúa mediante apoderado o persona jurídica.",
          fullWidth: true,
          searchFn: buscarRepresentantesActivosSelector
        });

        form.append(
          nombreField.group,
          razonField.group,
          rucField.group,
          giroField.group,
          direccionField.group,
          referenciaField.group,
          areaField.group,
          pisosField.group,
          telefonoField.group,
          emailField.group,
          titularSelect.group,
          repSelect.group
        );

        form.addEventListener("submit", async (e) => {
          e.preventDefault();
          hideInlineAlert(alertBox);
          clearFormErrors(form);

          const nombreComercial = nombreField.control.value.trim();
          const razonSocial = razonField.control.value.trim();
          const ruc = rucField.control.value.trim();
          const giroNegocio = giroField.control.value.trim();
          const direccion = direccionField.control.value.trim();
          const referencia = referenciaField.control.value.trim();
          const areaRaw = areaField.control.value;
          const pisosRaw = pisosField.control.value;
          const telefono = telefonoField.control.value.trim();
          const email = emailField.control.value.trim();
          const ciudadanoId = titularSelect.getValue();
          const representanteId = repSelect.getValue();

          let hasErr = false;
          const errNom = validateRequiredText(nombreComercial, "Nombre comercial", 200);
          if (errNom) {
            setFieldError(nombreField.control, errNom);
            hasErr = true;
          }
          const errRuc = validateRucPeruano(ruc, false);
          if (errRuc) {
            setFieldError(rucField.control, errRuc);
            hasErr = true;
          }
          const errGiro = validateRequiredText(giroNegocio, "Giro comercial", 150);
          if (errGiro) {
            setFieldError(giroField.control, errGiro);
            hasErr = true;
          }
          const errDir = validateRequiredText(direccion, "Dirección", 255);
          if (errDir) {
            setFieldError(direccionField.control, errDir);
            hasErr = true;
          }
          const errArea = validatePositiveNumber(areaRaw, "Área en metros cuadrados", {
            integer: false,
            min: 0.01
          });
          if (errArea) {
            setFieldError(areaField.control, errArea);
            hasErr = true;
          }
          const errPisos = validatePositiveNumber(pisosRaw, "Número de pisos", {
            integer: true,
            min: 1
          });
          if (errPisos) {
            setFieldError(pisosField.control, errPisos);
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
          if (!ciudadanoId) {
            titularSelect.setError("El ciudadano titular es obligatorio.");
            hasErr = true;
          }

          if (hasErr) return;

          modalApi.setBusy(true);
          if (submitBtnRef) setButtonLoading(submitBtnRef, true, "Guardando...");

          try {
            const payload = {
              nombreComercial,
              razonSocial: razonSocial || null,
              ruc: ruc || null,
              direccion,
              referencia: referencia || null,
              giroNegocio,
              areaMetrosCuadrados: Number(areaRaw),
              numeroPisos: Number(pisosRaw),
              telefono: telefono || null,
              email: email || null,
              ciudadanoId: Number(ciudadanoId),
              representanteId: representanteId ? Number(representanteId) : null
            };

            if (isEdit) {
              await actualizarEstablecimiento(existingRow.id, payload);
            } else {
              await crearEstablecimiento(payload);
            }

            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: isEdit ? "Establecimiento actualizado" : "Establecimiento registrado",
              message: `El establecimiento "${nombreComercial}" fue guardado correctamente.`
            });
            if (!isEdit) state.page = 0;
            loadEstablecimientosPage();
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(
              alertBox,
              err.message || "Error al guardar el establecimiento comercial.",
              "error"
            );
          }
        });

        bodyEl.append(alertBox, legalDisclaimer, form);
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
        saveBtn.textContent = isEdit ? "Guardar cambios" : "Registrar establecimiento";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  // Confirmación para activar o desactivar establecimiento (PATCH /api/establecimientos/{id}/estado)
  const handleToggleEstablecimientoStatus = async (row, targetActivo) => {
    await openConfirmDialog({
      title: targetActivo
        ? "Activar establecimiento comercial"
        : "Desactivar establecimiento comercial",
      message: targetActivo
        ? `¿Desea reactivar el establecimiento "${row.nombreComercial}"?`
        : `¿Desea desactivar el establecimiento "${row.nombreComercial}"?`,
      detail:
        "El registro se conserva íntegramente para auditoría y trazabilidad de licencias e inspecciones municipales.",
      confirmText: targetActivo ? "Sí, activar" : "Sí, desactivar",
      variant: targetActivo ? "primary" : "danger",
      async onConfirm() {
        try {
          await cambiarEstadoEstablecimiento(row.id, targetActivo);
          showToast({
            type: "success",
            title: targetActivo ? "Establecimiento activado" : "Establecimiento desactivado",
            message: `Estado actualizado para "${row.nombreComercial}".`
          });
          loadEstablecimientosPage();
        } catch (err) {
          showToast({
            type: "error",
            title: "Error",
            message: err.message || "No se pudo cambiar el estado del establecimiento."
          });
          throw err;
        }
      }
    });
  };

  filterForm.addEventListener("submit", (e) => {
    e.preventDefault();
    state.filters.nombreComercial = nombreFilter.control.value.trim();
    state.filters.ruc = rucFilter.control.value.trim();
    state.filters.ciudadanoId = titularIdFilter.control.value.trim();
    state.filters.activo = estadoFilter.control.value;
    state.page = 0;
    loadEstablecimientosPage();
  });

  clearBtn.addEventListener("click", () => {
    filterForm.reset();
    titularIdFilter.control.value = "";
    state.filters = {
      nombreComercial: "",
      ruc: "",
      ciudadanoId: "",
      activo: ""
    };
    state.page = 0;
    loadEstablecimientosPage();
  });

  await loadEstablecimientosPage();
}

/**
 * Abre la vista modal de detalle completo de un establecimiento comercial y sus solicitudes vinculadas.
 */
export async function openEstablecimientoDetailModal(establecimientoId, focusSection = "") {
  try {
    const [estRes, solRes] = await Promise.allSettled([
      obtenerEstablecimiento(establecimientoId),
      listarSolicitudesDeEstablecimiento(establecimientoId, 8)
    ]);

    if (estRes.status !== "fulfilled" || !estRes.value) {
      throw new Error(estRes.reason?.message || "No se pudo cargar el establecimiento.");
    }

    const e = estRes.value;
    const solicitudes =
      solRes.status === "fulfilled" && Array.isArray(solRes.value?.contenido)
        ? solRes.value.contenido
        : [];

    openModal({
      title: `Detalle de Establecimiento #${e.id}`,
      subtitle: `${e.nombreComercial}${e.ruc ? ` · RUC ${e.ruc}` : ""}`,
      size: "xl",
      renderBody(bodyEl) {
        const grid = document.createElement("div");
        grid.className = "detail-grid";

        const fields = [
          ["ID de establecimiento", `#${e.id}`],
          ["Estado", e.activo ? "ACTIVO" : "INACTIVO"],
          ["Nombre comercial", e.nombreComercial || "—"],
          ["Razón social", e.razonSocial || "No aplica / Persona natural"],
          ["RUC", e.ruc || "No registrado"],
          ["Giro de negocio", e.giroNegocio || "—"],
          ["Área declarada", `${e.areaMetrosCuadrados ?? 0} m²`],
          ["Número de pisos", String(e.numeroPisos ?? 1)],
          ["Dirección comercial", e.direccion || "—", true],
          ["Referencia de ubicación", e.referencia || "Sin referencia", true],
          [
            "Ciudadano titular",
            `${e.ciudadanoNombreCompleto || "—"}${e.ciudadanoNumeroDocumento ? ` (${e.ciudadanoNumeroDocumento})` : ""}`
          ],
          [
            "Representante legal",
            e.representanteNombreCompleto || "Sin representante asignado"
          ],
          ["Teléfono", e.telefono || "No registrado"],
          ["Correo electrónico", e.email || "No registrado"],
          ["Fecha de registro", formatDateTimePE(e.fechaRegistro)]
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

        const solBlock = document.createElement("div");
        solBlock.className = "related-section-block";
        const solTitle = document.createElement("h3");
        solTitle.className = "related-section-title";
        solTitle.style.marginBottom = "0.55rem";
        solTitle.textContent = `Solicitudes / Expedientes del Establecimiento (${solicitudes.length})`;

        const solTableBox = document.createElement("div");
        renderDataTable(solTableBox, {
          caption: "Solicitudes vinculadas al establecimiento comercial",
          columns: [
            { label: "Expediente", key: "numeroExpediente" },
            { label: "Tipo de trámite", key: "tipoTramiteNombre" },
            { label: "Titular", key: "ciudadanoNombreCompleto" },
            { label: "Estado", key: "estado" },
            { label: "Fecha presentación", render: (r) => formatDateTimePE(r.fechaPresentacion) }
          ],
          rows: solicitudes,
          emptyTitle: "Sin solicitudes registradas para este establecimiento",
          emptySubtitle: "Los expedientes vinculados a este local aparecerán aquí."
        });

        solBlock.append(solTitle, solTableBox);
        bodyEl.append(grid, solBlock);

        if (focusSection === "solicitudes") {
          window.setTimeout(() => solBlock.scrollIntoView({ behavior: "smooth" }), 50);
        }
      }
    });
  } catch (err) {
    showToast({
      type: "error",
      title: "Error al consultar establecimiento",
      message: err.message || "No se pudo obtener la información del establecimiento."
    });
  }
}

/**
 * Vista adaptada para usuarios con rol exclusivo CIUDADANO (Sección 22).
 */
async function renderCiudadanoEstablecimientosView(currentUser, container) {
  const card = document.createElement("section");
  card.className = "card";

  const header = document.createElement("div");
  header.className = "card-header";
  const title = document.createElement("h2");
  title.className = "card-title";
  title.textContent = "Mis Establecimientos Comerciales";
  header.appendChild(title);

  const body = document.createElement("div");
  body.className = "card-body";
  const tableBox = document.createElement("div");
  body.appendChild(tableBox);
  card.append(header, body);
  container.appendChild(card);

  try {
    const solPage = await listarSolicitudesDeCiudadano("", 5);
    const firstSol = Array.isArray(solPage?.contenido) ? solPage.contenido[0] : null;
    if (firstSol && firstSol.ciudadanoId) {
      const list = await listarEstablecimientosPorCiudadano(firstSol.ciudadanoId);
      renderDataTable(tableBox, {
        caption: "Mis establecimientos comerciales",
        columns: [
          { label: "ID", render: (r) => `#${r.id}` },
          { label: "Nombre comercial", key: "nombreComercial" },
          { label: "RUC", render: (r) => r.ruc || "—" },
          { label: "Dirección", key: "direccion" },
          { label: "Giro", key: "giroNegocio" },
          { label: "Estado", render: (r) => createStatusBadge(Boolean(r.activo)) },
          {
            label: "Acciones",
            render: (r) =>
              createTableActions([
                {
                  label: "Ver",
                  onClick: () => openEstablecimientoDetailModal(r.id)
                }
              ])
          }
        ],
        rows: list,
        emptyTitle: "No tiene establecimientos comerciales vinculados",
        emptySubtitle: "Acérquese a la ventanilla municipal para el registro inicial de su establecimiento."
      });
      return;
    }
  } catch (_err) {
    // Mostrar mensaje informativo
  }

  renderDataTable(tableBox, {
    caption: "Mis establecimientos comerciales",
    columns: [],
    rows: [],
    emptyTitle: "Sin establecimientos vinculados a su cuenta",
    emptySubtitle:
      "Cuando su ficha de ciudadano titular tenga establecimientos registrados, podrá consultarlos aquí."
  });
}
