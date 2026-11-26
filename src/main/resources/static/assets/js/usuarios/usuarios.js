/**
 * Módulo Visual de Gestión de Usuarios Municipales (PROMPT 11 - Secciones 5, 6 y 7)
 * Acceso exclusivo para el rol ADMINISTRADOR.
 */
import { ROLES, ROLE_LABELS } from "../config.js";
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
  validateEmailFormat,
  validatePasswordPolicy,
  validatePhoneFormat,
  validateRequiredText,
  validateUsername
} from "../components/form-validation.js";
import {
  actualizarUsuario,
  asignarRolesUsuario,
  cambiarEstadoUsuario,
  crearUsuario,
  listarRolesSistema,
  listarUsuarios,
  obtenerUsuario
} from "./usuarios-api.js";

/**
 * Renderiza el módulo completo de Gestión de Usuarios en el contenedor principal.
 */
export async function renderUsuariosModule(currentUser, container) {
  if (!container) return;
  clearChildren(container);

  if (!hasRole(currentUser, ROLES.ADMINISTRADOR)) {
    const alert = document.createElement("div");
    alert.className = "alert alert-error";
    alert.setAttribute("role", "alert");
    alert.textContent = "Acceso restringido: Solo el rol ADMINISTRADOR está autorizado para gestionar cuentas de usuario.";
    container.appendChild(alert);
    return;
  }

  const state = {
    filters: {
      nombre: "",
      username: "",
      rol: "",
      activo: ""
    },
    page: 0,
    size: 10,
    loading: false,
    rolesCatalog: []
  };

  const wrapper = document.createElement("div");
  wrapper.className = "management-module";

  // Tarjeta de barra de herramientas y filtros
  const toolbarCard = document.createElement("section");
  toolbarCard.className = "module-toolbar-card";
  toolbarCard.setAttribute("aria-label", "Filtros y acciones de usuarios");

  const topRow = document.createElement("div");
  topRow.className = "module-toolbar-top";

  const titleGroup = document.createElement("div");
  titleGroup.className = "module-toolbar-title-group";

  const heading = document.createElement("h2");
  heading.className = "module-toolbar-heading";
  heading.textContent = "Padrón de Cuentas y Permisos del Sistema";

  const caption = document.createElement("p");
  caption.className = "module-toolbar-caption";
  caption.textContent = "Consulte, registre, actualice estados y asigne roles institucionales con auditoría activa.";

  titleGroup.append(heading, caption);

  const topActions = document.createElement("div");
  topActions.className = "module-toolbar-actions";

  const newBtn = document.createElement("button");
  newBtn.type = "button";
  newBtn.id = "btn-nuevo-usuario";
  newBtn.className = "btn btn-primary";
  newBtn.textContent = "+ Nuevo usuario";

  topActions.appendChild(newBtn);
  topRow.append(titleGroup, topActions);

  // Formulario de búsqueda
  const filterForm = document.createElement("form");
  filterForm.className = "filter-form-grid";
  filterForm.noValidate = true;

  const nombreFilter = createFormField({
    id: "filtro-usuario-nombre",
    label: "Nombre o apellidos",
    placeholder: "Ej. Carlos Ayala"
  });

  const usernameFilter = createFormField({
    id: "filtro-usuario-username",
    label: "Usuario",
    placeholder: "Ej. admin.municipal"
  });

  const rolFilter = createFormField({
    id: "filtro-usuario-rol",
    label: "Rol institucional",
    type: "select",
    options: [
      { value: "", label: "Todos los roles" },
      { value: "ADMINISTRADOR", label: "ADMINISTRADOR" },
      { value: "SUPERVISOR", label: "SUPERVISOR" },
      { value: "FUNCIONARIO", label: "FUNCIONARIO" },
      { value: "CIUDADANO", label: "CIUDADANO" }
    ]
  });

  const estadoFilter = createFormField({
    id: "filtro-usuario-estado",
    label: "Estado de cuenta",
    type: "select",
    options: [
      { value: "", label: "Todos los estados" },
      { value: "true", label: "Activos" },
      { value: "false", label: "Inactivos" }
    ]
  });

  const filterButtonsWrap = document.createElement("div");
  filterButtonsWrap.className = "form-group filter-form-actions";

  const searchBtn = document.createElement("button");
  searchBtn.type = "submit";
  searchBtn.className = "btn btn-primary";
  searchBtn.textContent = "Buscar";

  const clearBtn = document.createElement("button");
  clearBtn.type = "button";
  clearBtn.className = "btn btn-secondary";
  clearBtn.textContent = "Limpiar filtros";

  filterButtonsWrap.append(searchBtn, clearBtn);
  filterForm.append(
    nombreFilter.group,
    usernameFilter.group,
    rolFilter.group,
    estadoFilter.group,
    filterButtonsWrap
  );

  toolbarCard.append(topRow, filterForm);

  // Contenedor de tabla y paginación
  const tableSection = document.createElement("section");
  tableSection.setAttribute("aria-label", "Listado paginado de usuarios");
  const tableContainer = document.createElement("div");
  const paginationContainer = document.createElement("div");
  tableSection.append(tableContainer, paginationContainer);

  wrapper.append(toolbarCard, tableSection);
  container.appendChild(wrapper);

  // Cargar catálogo real de roles desde /api/roles
  const ensureRolesCatalog = async () => {
    if (state.rolesCatalog.length > 0) {
      return state.rolesCatalog;
    }
    try {
      const roles = await listarRolesSistema();
      if (Array.isArray(roles) && roles.length > 0) {
        state.rolesCatalog = roles.filter((r) => r.activo !== false);
        clearChildren(rolFilter.control);
        const allOpt = document.createElement("option");
        allOpt.value = "";
        allOpt.textContent = "Todos los roles";
        rolFilter.control.appendChild(allOpt);
        for (const r of state.rolesCatalog) {
          const opt = document.createElement("option");
          opt.value = r.nombre;
          opt.textContent = `${r.nombre}${ROLE_LABELS[r.nombre] ? ` — ${ROLE_LABELS[r.nombre]}` : ""}`;
          rolFilter.control.appendChild(opt);
        }
      }
    } catch (_err) {
      // Mantiene las opciones estáticas por defecto si ocurre fallo temporal
    }
    return state.rolesCatalog;
  };

  const columns = [
    {
      label: "ID",
      render(row) {
        const span = document.createElement("span");
        span.className = "cell-mono";
        span.textContent = `#${row.id}`;
        return span;
      }
    },
    {
      label: "Nombre completo",
      render(row) {
        const div = document.createElement("div");
        const primary = document.createElement("span");
        primary.className = "cell-primary";
        primary.textContent = `${row.nombres || ""} ${row.apellidos || ""}`.trim() || "—";
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
      label: "Usuario",
      render(row) {
        const span = document.createElement("span");
        span.className = "cell-mono";
        span.textContent = row.username || "—";
        return span;
      }
    },
    {
      label: "Correo electrónico",
      render(row) {
        return row.email || "—";
      }
    },
    {
      label: "Rol o roles",
      render(row) {
        const list = document.createElement("div");
        list.className = "badge-list-inline";
        const roles = Array.isArray(row.roles) ? row.roles : [];
        if (roles.length === 0) {
          list.textContent = "Sin rol";
          return list;
        }
        for (const r of roles) {
          const b = document.createElement("span");
          b.className = `badge ${r === "ADMINISTRADOR" ? "badge-info" : "badge-neutral"}`;
          b.textContent = r;
          list.appendChild(b);
        }
        return list;
      }
    },
    {
      label: "Estado",
      render(row) {
        return createStatusBadge(Boolean(row.activo));
      }
    },
    {
      label: "Fecha de registro",
      render(row) {
        return formatDateTimePE(row.fechaCreacion);
      }
    },
    {
      label: "Acciones",
      render(row) {
        const isSelf = Number(row.id) === Number(currentUser.id);
        return createTableActions([
          {
            label: "Ver",
            title: `Ver detalle del usuario ${row.username}`,
            onClick: () => openViewUserModal(row.id)
          },
          {
            label: "Editar",
            title: `Editar datos de ${row.username}`,
            variant: "primary",
            onClick: () => openEditUserModal(row)
          },
          {
            label: "Roles",
            title: `Administrar roles de ${row.username}`,
            variant: "warning",
            onClick: () => openManageRolesModal(row)
          },
          row.activo
            ? {
                label: "Desactivar",
                title: isSelf
                  ? "No puede desactivar su propia cuenta activa en sesión"
                  : `Desactivar cuenta de ${row.username}`,
                variant: "danger",
                disabled: isSelf,
                onClick: () => handleToggleUserStatus(row, false)
              }
            : {
                label: "Activar",
                title: `Reactivar cuenta de ${row.username}`,
                variant: "success",
                onClick: () => handleToggleUserStatus(row, true)
              }
        ]);
      }
    }
  ];

  const loadUsersPage = async () => {
    if (state.loading) return;
    state.loading = true;
    searchBtn.disabled = true;

    renderDataTable(tableContainer, {
      caption: "Tabla de usuarios del sistema municipal",
      columns,
      loading: true
    });

    try {
      const response = await listarUsuarios({
        nombre: state.filters.nombre,
        username: state.filters.username,
        rol: state.filters.rol,
        activo: state.filters.activo,
        page: state.page,
        size: state.size
      });

      const rows = Array.isArray(response?.contenido) ? response.contenido : [];
      renderDataTable(tableContainer, {
        caption: "Tabla de usuarios del sistema municipal",
        columns,
        rows,
        emptyTitle: "No se encontraron usuarios con los filtros aplicados",
        emptySubtitle: "Puede limpiar los filtros de búsqueda o registrar una nueva cuenta municipal."
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
          loadUsersPage();
        },
        onSizeChange(nextSize) {
          state.size = nextSize;
          state.page = 0;
          loadUsersPage();
        }
      });
    } catch (err) {
      renderDataTable(tableContainer, {
        caption: "Tabla de usuarios del sistema municipal",
        columns,
        error: err.message || "Error al consultar usuarios del sistema.",
        onRetry: () => loadUsersPage()
      });
      clearChildren(paginationContainer);
    } finally {
      state.loading = false;
      searchBtn.disabled = false;
    }
  };

  // Vista de detalle de usuario (GET /api/usuarios/{id})
  const openViewUserModal = async (userId) => {
    try {
      const u = await obtenerUsuario(userId);
      openModal({
        title: `Detalle de Usuario #${u.id}`,
        subtitle: `Cuenta institucional: @${u.username}`,
        size: "md",
        renderBody(bodyEl) {
          const grid = document.createElement("div");
          grid.className = "detail-grid";

          const fields = [
            ["ID de registro", `#${u.id}`],
            ["Nombre de usuario", u.username],
            ["Nombres", u.nombres || "—"],
            ["Apellidos", u.apellidos || "—"],
            ["Correo electrónico", u.email || "—"],
            ["Teléfono", u.telefono || "No registrado"],
            ["Estado de cuenta", u.activo ? "ACTIVO" : "INACTIVO"],
            ["Roles asignados", Array.isArray(u.roles) && u.roles.length > 0 ? u.roles.join(", ") : "Ninguno"],
            ["Fecha de registro", formatDateTimePE(u.fechaCreacion)],
            ["Último acceso", formatDateTimePE(u.ultimoAcceso)]
          ];

          for (const [label, val] of fields) {
            const item = document.createElement("div");
            item.className = "detail-item";
            const lbl = document.createElement("span");
            lbl.className = "detail-label";
            lbl.textContent = label;
            const v = document.createElement("span");
            v.className = "detail-value";
            v.textContent = val;
            item.append(lbl, v);
            grid.appendChild(item);
          }

          bodyEl.appendChild(grid);
        }
      });
    } catch (err) {
      showToast({
        type: "error",
        title: "Error al consultar usuario",
        message: err.message || "No se pudo obtener el detalle del usuario."
      });
    }
  };

  // Modal de registro de nuevo usuario (POST /api/usuarios)
  const openCreateUserModal = async () => {
    const roles = await ensureRolesCatalog();
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: "Registrar Nuevo Usuario Municipal",
      subtitle: "Complete las credenciales iniciales y seleccione los roles autorizados.",
      size: "lg",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const form = document.createElement("form");
        form.className = "form-grid-2";
        form.noValidate = true;
        formRef = form;

        const nombresField = createFormField({
          id: "usr-new-nombres",
          label: "Nombres",
          required: true,
          maxLength: 100,
          placeholder: "Ej. María Elena"
        });

        const apellidosField = createFormField({
          id: "usr-new-apellidos",
          label: "Apellidos",
          required: true,
          maxLength: 100,
          placeholder: "Ej. Quispe Mendoza"
        });

        const usernameField = createFormField({
          id: "usr-new-username",
          label: "Nombre de usuario",
          required: true,
          maxLength: 50,
          placeholder: "Ej. mquispe.func",
          hint: "De 3 a 50 caracteres (letras, números, puntos, guiones)."
        });

        const emailField = createFormField({
          id: "usr-new-email",
          label: "Correo electrónico institucional",
          type: "email",
          required: true,
          maxLength: 150,
          placeholder: "usuario@municaceres.gob.pe"
        });

        const telefonoField = createFormField({
          id: "usr-new-telefono",
          label: "Teléfono de contacto",
          maxLength: 20,
          placeholder: "Ej. 966123456"
        });

        const estadoInfoField = createFormField({
          id: "usr-new-estado-info",
          label: "Estado inicial",
          value: "ACTIVO (por defecto al crear cuenta)",
          disabled: true
        });

        const passwordField = createFormField({
          id: "usr-new-password",
          label: "Contraseña inicial",
          type: "password",
          required: true,
          maxLength: 72,
          autocomplete: "new-password",
          hint: "Mínimo 12 caracteres por política de seguridad municipal."
        });

        const confirmPasswordField = createFormField({
          id: "usr-new-password-confirm",
          label: "Confirmar contraseña inicial",
          type: "password",
          required: true,
          maxLength: 72,
          autocomplete: "new-password",
          hint: "Debe coincidir exactamente con la contraseña inicial."
        });

        // Selector de roles (Checkboxes)
        const rolesGroup = document.createElement("div");
        rolesGroup.className = "form-group form-col-full";

        const rolesLabel = document.createElement("span");
        rolesLabel.className = "form-label";
        rolesLabel.textContent = "Roles autorizados ";
        const reqStar = document.createElement("span");
        reqStar.className = "required-mark";
        reqStar.textContent = "*";
        rolesLabel.appendChild(reqStar);

        const cardsGrid = document.createElement("div");
        cardsGrid.className = "checkbox-cards-grid";

        const roleCheckboxes = [];
        for (const r of roles) {
          const card = document.createElement("label");
          card.className = "checkbox-card";

          const cb = document.createElement("input");
          cb.type = "checkbox";
          cb.value = String(r.id);
          cb.dataset.roleName = r.nombre;

          const info = document.createElement("div");
          info.className = "checkbox-card-info";

          const rTitle = document.createElement("span");
          rTitle.className = "checkbox-card-title";
          rTitle.textContent = r.nombre;

          const rDesc = document.createElement("span");
          rDesc.className = "checkbox-card-desc";
          rDesc.textContent = r.descripcion || ROLE_LABELS[r.nombre] || "Rol del sistema municipal";

          info.append(rTitle, rDesc);
          card.append(cb, info);
          cardsGrid.appendChild(card);
          roleCheckboxes.push(cb);
        }

        const rolesErrSpan = document.createElement("span");
        rolesErrSpan.className = "field-error-msg";
        rolesErrSpan.hidden = true;

        rolesGroup.append(rolesLabel, cardsGrid, rolesErrSpan);

        form.append(
          nombresField.group,
          apellidosField.group,
          usernameField.group,
          emailField.group,
          telefonoField.group,
          estadoInfoField.group,
          passwordField.group,
          confirmPasswordField.group,
          rolesGroup
        );

        form.addEventListener("submit", async (e) => {
          e.preventDefault();
          hideInlineAlert(alertBox);
          clearFormErrors(form);
          rolesErrSpan.hidden = true;

          const nombres = nombresField.control.value.trim();
          const apellidos = apellidosField.control.value.trim();
          const username = usernameField.control.value.trim();
          const email = emailField.control.value.trim();
          const telefono = telefonoField.control.value.trim();
          const password = passwordField.control.value;
          const confirmPwd = confirmPasswordField.control.value;
          const selectedRoleIds = roleCheckboxes
            .filter((cb) => cb.checked)
            .map((cb) => Number(cb.value));

          let hasErr = false;
          const errNombres = validateRequiredText(nombres, "Nombres", 100);
          if (errNombres) {
            setFieldError(nombresField.control, errNombres);
            hasErr = true;
          }
          const errApellidos = validateRequiredText(apellidos, "Apellidos", 100);
          if (errApellidos) {
            setFieldError(apellidosField.control, errApellidos);
            hasErr = true;
          }
          const errUsername = validateUsername(username);
          if (errUsername) {
            setFieldError(usernameField.control, errUsername);
            hasErr = true;
          }
          const errEmail = validateEmailFormat(email, true);
          if (errEmail) {
            setFieldError(emailField.control, errEmail);
            hasErr = true;
          }
          const errPhone = validatePhoneFormat(telefono, false);
          if (errPhone) {
            setFieldError(telefonoField.control, errPhone);
            hasErr = true;
          }
          const errPwd = validatePasswordPolicy(password, confirmPwd);
          if (errPwd) {
            setFieldError(
              errPwd.field === "password" ? passwordField.control : confirmPasswordField.control,
              errPwd.message
            );
            hasErr = true;
          }
          if (selectedRoleIds.length === 0) {
            rolesErrSpan.textContent = "Seleccione al menos un rol institucional para el usuario.";
            rolesErrSpan.hidden = false;
            hasErr = true;
          }

          if (hasErr) return;

          modalApi.setBusy(true);
          if (submitBtnRef) setButtonLoading(submitBtnRef, true, "Guardando usuario...");

          try {
            await crearUsuario({
              username,
              password,
              nombres,
              apellidos,
              email,
              telefono: telefono || null,
              rolIds: selectedRoleIds
            });
            // Limpiar contraseñas inmediatamente de memoria del DOM
            passwordField.control.value = "";
            confirmPasswordField.control.value = "";

            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: "Usuario registrado",
              message: `La cuenta @${username} fue creada correctamente.`
            });
            state.page = 0;
            loadUsersPage();
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(alertBox, err.message || "No se pudo registrar el usuario.", "error");
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
        saveBtn.textContent = "Guardar usuario";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  // Modal de edición de datos de usuario (PUT /api/usuarios/{id})
  const openEditUserModal = (userRow) => {
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: `Editar Usuario #${userRow.id}`,
      subtitle: `Modificando datos de contacto de @${userRow.username}`,
      size: "md",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const form = document.createElement("form");
        form.className = "form-grid-2";
        form.noValidate = true;
        formRef = form;

        const usernameField = createFormField({
          id: "usr-edit-username",
          label: "Nombre de usuario (Inmutable)",
          value: userRow.username,
          disabled: true,
          fullWidth: true
        });

        const nombresField = createFormField({
          id: "usr-edit-nombres",
          label: "Nombres",
          required: true,
          maxLength: 100,
          value: userRow.nombres || ""
        });

        const apellidosField = createFormField({
          id: "usr-edit-apellidos",
          label: "Apellidos",
          required: true,
          maxLength: 100,
          value: userRow.apellidos || ""
        });

        const emailField = createFormField({
          id: "usr-edit-email",
          label: "Correo electrónico",
          type: "email",
          required: true,
          maxLength: 150,
          value: userRow.email || "",
          fullWidth: true
        });

        const telefonoField = createFormField({
          id: "usr-edit-telefono",
          label: "Teléfono",
          maxLength: 20,
          value: userRow.telefono || "",
          fullWidth: true
        });

        form.append(
          usernameField.group,
          nombresField.group,
          apellidosField.group,
          emailField.group,
          telefonoField.group
        );

        form.addEventListener("submit", async (e) => {
          e.preventDefault();
          hideInlineAlert(alertBox);
          clearFormErrors(form);

          const nombres = nombresField.control.value.trim();
          const apellidos = apellidosField.control.value.trim();
          const email = emailField.control.value.trim();
          const telefono = telefonoField.control.value.trim();

          let hasErr = false;
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
          const errMail = validateEmailFormat(email, true);
          if (errMail) {
            setFieldError(emailField.control, errMail);
            hasErr = true;
          }
          const errTel = validatePhoneFormat(telefono, false);
          if (errTel) {
            setFieldError(telefonoField.control, errTel);
            hasErr = true;
          }
          if (hasErr) return;

          modalApi.setBusy(true);
          if (submitBtnRef) setButtonLoading(submitBtnRef, true, "Actualizando...");

          try {
            await actualizarUsuario(userRow.id, {
              nombres,
              apellidos,
              email,
              telefono: telefono || null
            });
            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: "Registro actualizado",
              message: `Los datos del usuario @${userRow.username} fueron actualizados.`
            });
            loadUsersPage();
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(alertBox, err.message || "Error al actualizar el usuario.", "error");
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
        saveBtn.textContent = "Guardar cambios";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  // Modal de administración de roles (PUT /api/usuarios/{id}/roles)
  const openManageRolesModal = async (userRow) => {
    const roles = await ensureRolesCatalog();
    const isSelf = Number(userRow.id) === Number(currentUser.id);
    const currentRoleNames = new Set(Array.isArray(userRow.roles) ? userRow.roles : []);
    let submitBtnRef = null;
    let formRef = null;

    openModal({
      title: `Administrar Roles — @${userRow.username}`,
      subtitle: `${userRow.nombres || ""} ${userRow.apellidos || ""}`.trim(),
      size: "md",
      renderBody(bodyEl, modalApi) {
        const alertBox = document.createElement("div");
        alertBox.hidden = true;
        alertBox.style.marginBottom = "1rem";

        const notice = document.createElement("div");
        notice.className = "legal-disclaimer-box";
        notice.style.marginBottom = "1rem";
        notice.textContent = isSelf
          ? "Por seguridad, no se permite retirar el rol ADMINISTRADOR de su propia sesión activa."
          : "Al modificar los roles de un usuario, el servidor cerrará sus sesiones activas para aplicar los nuevos privilegios.";

        const form = document.createElement("form");
        form.noValidate = true;
        formRef = form;

        const cardsGrid = document.createElement("div");
        cardsGrid.className = "checkbox-cards-grid";

        const roleCheckboxes = [];
        for (const r of roles) {
          const card = document.createElement("label");
          card.className = "checkbox-card";

          const cb = document.createElement("input");
          cb.type = "checkbox";
          cb.value = String(r.id);
          cb.checked = currentRoleNames.has(r.nombre);
          if (isSelf && r.nombre === "ADMINISTRADOR") {
            cb.disabled = true;
          }

          const info = document.createElement("div");
          info.className = "checkbox-card-info";

          const rTitle = document.createElement("span");
          rTitle.className = "checkbox-card-title";
          rTitle.textContent = r.nombre;

          const rDesc = document.createElement("span");
          rDesc.className = "checkbox-card-desc";
          rDesc.textContent = r.descripcion || ROLE_LABELS[r.nombre] || "";

          info.append(rTitle, rDesc);
          card.append(cb, info);
          cardsGrid.appendChild(card);
          roleCheckboxes.push(cb);
        }

        form.appendChild(cardsGrid);

        form.addEventListener("submit", async (e) => {
          e.preventDefault();
          hideInlineAlert(alertBox);

          const selectedRoleIds = roleCheckboxes
            .filter((cb) => cb.checked)
            .map((cb) => Number(cb.value));

          if (selectedRoleIds.length === 0) {
            showInlineAlert(alertBox, "El usuario debe conservar al menos un rol asignado.", "error");
            return;
          }

          modalApi.setBusy(true);
          if (submitBtnRef) setButtonLoading(submitBtnRef, true, "Actualizando roles...");

          try {
            await asignarRolesUsuario(userRow.id, selectedRoleIds);
            modalApi.setBusy(false);
            modalApi.close();
            showToast({
              type: "success",
              title: "Roles actualizados",
              message: `Se actualizaron los privilegios de @${userRow.username}.`
            });
            loadUsersPage();
          } catch (err) {
            modalApi.setBusy(false);
            if (submitBtnRef) setButtonLoading(submitBtnRef, false);
            showInlineAlert(alertBox, err.message || "No fue posible asignar los roles.", "error");
          }
        });

        bodyEl.append(alertBox, notice, form);
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
        saveBtn.textContent = "Guardar roles";
        submitBtnRef = saveBtn;
        saveBtn.addEventListener("click", () => {
          if (formRef) formRef.requestSubmit();
        });

        footerEl.append(cancelBtn, saveBtn);
      }
    });
  };

  // Confirmación para activar o desactivar usuario (PATCH /api/usuarios/{id}/estado)
  const handleToggleUserStatus = async (userRow, targetActivo) => {
    await openConfirmDialog({
      title: targetActivo ? "Activar cuenta de usuario" : "Desactivar cuenta de usuario",
      message: targetActivo
        ? `¿Confirma que desea reactivar el acceso al sistema para el usuario @${userRow.username}?`
        : `¿Confirma que desea desactivar la cuenta del usuario @${userRow.username}?`,
      detail: targetActivo
        ? "El usuario podrá volver a iniciar sesión con sus roles asignados."
        : "Al desactivar la cuenta, todas sus sesiones activas se cerrarán de inmediato. El registro histórico se conserva para auditoría.",
      confirmText: targetActivo ? "Sí, activar usuario" : "Sí, desactivar usuario",
      variant: targetActivo ? "primary" : "danger",
      async onConfirm() {
        try {
          await cambiarEstadoUsuario(userRow.id, targetActivo);
          showToast({
            type: "success",
            title: targetActivo ? "Usuario activado" : "Usuario desactivado",
            message: `El estado de @${userRow.username} se actualizó correctamente.`
          });
          loadUsersPage();
        } catch (err) {
          showToast({
            type: "error",
            title: "Operación rechazada",
            message: err.message || "No se pudo cambiar el estado del usuario."
          });
          throw err;
        }
      }
    });
  };

  // Eventos de barra de búsqueda
  filterForm.addEventListener("submit", (e) => {
    e.preventDefault();
    state.filters.nombre = nombreFilter.control.value.trim();
    state.filters.username = usernameFilter.control.value.trim();
    state.filters.rol = rolFilter.control.value;
    state.filters.activo = estadoFilter.control.value;
    state.page = 0;
    loadUsersPage();
  });

  clearBtn.addEventListener("click", () => {
    filterForm.reset();
    state.filters = { nombre: "", username: "", rol: "", activo: "" };
    state.page = 0;
    loadUsersPage();
  });

  newBtn.addEventListener("click", () => openCreateUserModal());

  ensureRolesCatalog();
  await loadUsersPage();
}
