/**
 * Controlador principal del Centro de Notificaciones Municipales (PROMPT 14 - Secciones 17 a 24)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { clearChildren, showToast } from "../ui.js";
import { formatDateTimePE } from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import { refreshUnreadNotificationsBadge } from "../layout.js";
import {
  getNotificaciones,
  getNotificacionesNoLeidas,
  getNotificacionesContador,
  marcarNotificacionLeida,
  marcarTodasNotificacionesLeidas
} from "./notificaciones-api.js";
import { openNotificacionDetalleModal } from "./notificaciones-detalle.js";

const FILTER_TABS = [
  { key: "todas", label: "Todas" },
  { key: "no-leidas", label: "No leídas" },
  { key: "SOLICITUD", label: "Solicitudes" },
  { key: "LICENCIA", label: "Licencias" },
  { key: "INSPECCION", label: "Inspecciones" },
  { key: "PAGO", label: "Pagos" },
  { key: "ALERTA", label: "Alertas" }
];

export async function renderNotificacionesModule(user, container, { initialId = null } = {}) {
  if (!container) return;
  clearChildren(container);

  const root = document.createElement("div");
  root.className = "notificaciones-module";

  let activeTab = "todas";
  let currentPage = 0;
  let pageSize = 15;
  let unreadCount = 0;

  // 1. Barra de control superior
  const toolbarCard = document.createElement("div");
  toolbarCard.className = "notif-toolbar-card";

  const summaryLeft = document.createElement("div");
  summaryLeft.className = "notif-status-summary";

  const unreadPill = document.createElement("div");
  unreadPill.className = "notif-unread-count-pill";
  unreadPill.textContent = "Cargando notificaciones...";

  summaryLeft.appendChild(unreadPill);

  // Botón marcar todas como leídas
  const markAllBtn = document.createElement("button");
  markAllBtn.type = "button";
  markAllBtn.className = "btn btn-secondary";
  markAllBtn.textContent = "Marcar todas como leídas";
  markAllBtn.style.fontSize = "0.8125rem";

  markAllBtn.addEventListener("click", async () => {
    try {
      markAllBtn.disabled = true;
      const res = await marcarTodasNotificacionesLeidas();
      showToast({
        type: "success",
        title: "Actualizado",
        message: `Se marcaron ${res?.actualizadas || "todas"} las notificaciones como leídas.`
      });
      await refreshUnreadNotificationsBadge();
      await loadNotifications();
    } catch (err) {
      showToast({
        type: "error",
        title: "Error",
        message: err.message || "No se pudieron marcar todas las notificaciones."
      });
    } finally {
      markAllBtn.disabled = false;
    }
  });

  toolbarCard.append(summaryLeft, markAllBtn);
  root.appendChild(toolbarCard);

  // 2. Pestañas de filtrado
  const filterTabsContainer = document.createElement("div");
  filterTabsContainer.className = "notif-filter-tabs";

  FILTER_TABS.forEach(tab => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = `notif-filter-tab ${tab.key === activeTab ? "is-active" : ""}`;
    btn.textContent = tab.label;

    btn.addEventListener("click", () => {
      if (activeTab === tab.key) return;
      activeTab = tab.key;
      currentPage = 0;
      updateFilterTabVisuals();
      loadNotifications();
    });

    filterTabsContainer.appendChild(btn);
  });

  root.appendChild(filterTabsContainer);

  function updateFilterTabVisuals() {
    const buttons = filterTabsContainer.querySelectorAll(".notif-filter-tab");
    buttons.forEach((btn, idx) => {
      const match = FILTER_TABS[idx]?.key === activeTab;
      btn.classList.toggle("is-active", match);
    });
  }

  // 3. Contenedor de la lista de tarjetas
  const listContainer = document.createElement("div");
  listContainer.className = "notif-list-container";
  listContainer.setAttribute("role", "feed");
  listContainer.setAttribute("aria-busy", "true");
  root.appendChild(listContainer);

  // 4. Contenedor de paginación
  const paginationMount = document.createElement("div");
  root.appendChild(paginationMount);

  container.appendChild(root);

  async function updateUnreadPill() {
    try {
      const res = await getNotificacionesContador();
      unreadCount = typeof res?.noLeidas === "number" ? res.noLeidas : 0;
      if (unreadCount > 0) {
        unreadPill.className = "notif-unread-count-pill has-unread";
        unreadPill.textContent = `${unreadCount} ${unreadCount === 1 ? "notificación no leída" : "notificaciones no leídas"}`;
        markAllBtn.style.display = "inline-flex";
      } else {
        unreadPill.className = "notif-unread-count-pill";
        unreadPill.textContent = "No tiene notificaciones pendientes";
        markAllBtn.style.display = "none";
      }
    } catch (_e) {
      unreadPill.textContent = "Centro de notificaciones activo";
    }
  }

  async function loadNotifications() {
    clearChildren(listContainer);
    listContainer.setAttribute("aria-busy", "true");

    const loadingBox = document.createElement("div");
    loadingBox.className = "table-state-box";
    loadingBox.textContent = "Consultando avisos y notificaciones...";
    listContainer.appendChild(loadingBox);

    await updateUnreadPill();

    try {
      const queryParams = {
        page: currentPage,
        size: pageSize
      };

      let pageResponse;
      if (activeTab === "no-leidas") {
        pageResponse = await getNotificacionesNoLeidas(queryParams);
      } else {
        pageResponse = await getNotificaciones(queryParams);
      }

      clearChildren(listContainer);
      listContainer.setAttribute("aria-busy", "false");

      let items = Array.isArray(pageResponse?.contenido) ? pageResponse.contenido : [];

      // Si se filtró por tipo específico
      if (activeTab !== "todas" && activeTab !== "no-leidas") {
        items = items.filter(n => n.tipo === activeTab);
      }

      if (items.length === 0) {
        const emptyBox = document.createElement("div");
        emptyBox.className = "table-state-box";
        emptyBox.innerHTML = `
          <strong style="font-size: 1rem; color: var(--color-text);">No hay notificaciones disponibles</strong>
          <p class="text-muted" style="margin-top: 0.25rem;">No se encontraron avisos con el filtro seleccionado.</p>
        `;
        listContainer.appendChild(emptyBox);
        clearChildren(paginationMount);
        return;
      }

      items.forEach(notif => {
        const card = createNotificationCard(notif);
        listContainer.appendChild(card);
      });

      // Renderizar paginación
      renderPagination(paginationMount, {
        pagina: pageResponse.pagina,
        tamano: pageResponse.tamanio,
        totalElementos: pageResponse.totalElementos,
        totalPaginas: pageResponse.totalPaginas,
        primera: pageResponse.primera,
        ultima: pageResponse.ultima,
        onPageChange: (newPage) => {
          currentPage = newPage;
          loadNotifications();
        },
        onSizeChange: (newSize) => {
          pageSize = newSize;
          currentPage = 0;
          loadNotifications();
        }
      });
    } catch (err) {
      clearChildren(listContainer);
      listContainer.setAttribute("aria-busy", "false");

      const errorBox = document.createElement("div");
      errorBox.className = "table-state-box";
      errorBox.innerHTML = `
        <strong style="color: var(--color-danger);">Error al cargar notificaciones</strong>
        <p class="text-muted">${err.message || "No se pudo sincronizar la bandeja de avisos."}</p>
      `;
      listContainer.appendChild(errorBox);
    }
  }

  function createNotificationCard(notif) {
    const card = document.createElement("article");
    card.className = `notif-card ${notif.leida ? "is-read" : "is-unread"}`;
    card.setAttribute("aria-labelledby", `notif-title-${notif.id}`);

    // Icono según tipo
    const iconWrap = document.createElement("div");
    iconWrap.className = "notif-card-icon-wrap";
    iconWrap.innerHTML = getIconSvgForType(notif.tipo);

    const content = document.createElement("div");
    content.className = "notif-card-content";

    const header = document.createElement("div");
    header.className = "notif-card-header";

    const title = document.createElement("h4");
    title.id = `notif-title-${notif.id}`;
    title.className = "notif-card-title";
    title.textContent = notif.titulo || "Notificación municipal";

    const dateSpan = document.createElement("time");
    dateSpan.className = "notif-card-date";
    dateSpan.textContent = formatDateTimePE(notif.fechaCreacion);

    header.append(title, dateSpan);

    const body = document.createElement("p");
    body.className = "notif-card-body";
    body.textContent = notif.mensaje || "";

    const footer = document.createElement("div");
    footer.className = "notif-card-footer";

    const badgeGroup = document.createElement("div");
    badgeGroup.className = "notif-card-badge-group";

    const typeBadge = document.createElement("span");
    typeBadge.className = "badge badge-info";
    typeBadge.textContent = notif.tipo || "INFORMATIVA";

    const statusBadge = document.createElement("span");
    statusBadge.className = notif.leida ? "badge badge-neutral" : "badge badge-primary";
    statusBadge.textContent = notif.leida ? "Leída" : "No leída";

    badgeGroup.append(typeBadge, statusBadge);

    const actions = document.createElement("div");
    actions.className = "notif-card-actions";

    if (!notif.leida) {
      const markBtn = document.createElement("button");
      markBtn.type = "button";
      markBtn.className = "btn-notif-action btn-mark-read";
      markBtn.textContent = "Marcar leída";

      markBtn.addEventListener("click", async () => {
        try {
          markBtn.disabled = true;
          await marcarNotificacionLeida(notif.id);
          notif.leida = true;
          card.classList.remove("is-unread");
          card.classList.add("is-read");
          statusBadge.className = "badge badge-neutral";
          statusBadge.textContent = "Leída";
          markBtn.remove();
          await refreshUnreadNotificationsBadge();
          await updateUnreadPill();
        } catch (err) {
          showToast({
            type: "error",
            title: "Error",
            message: err.message || "No se pudo marcar la notificación."
          });
        }
      });
      actions.appendChild(markBtn);
    }

    const detailBtn = document.createElement("button");
    detailBtn.type = "button";
    detailBtn.className = "btn-notif-action";
    detailBtn.textContent = "Ver detalle";

    detailBtn.addEventListener("click", () => {
      openNotificacionDetalleModal(notif.id, {
        onReadUpdated: async () => {
          notif.leida = true;
          card.classList.remove("is-unread");
          card.classList.add("is-read");
          statusBadge.className = "badge badge-neutral";
          statusBadge.textContent = "Leída";
          await refreshUnreadNotificationsBadge();
          await updateUnreadPill();
        }
      });
    });

    actions.appendChild(detailBtn);
    footer.append(badgeGroup, actions);
    content.append(header, body, footer);
    card.append(iconWrap, content);

    return card;
  }

  function getIconSvgForType(tipo) {
    const t = String(tipo || "").toUpperCase();
    if (t === "SOLICITUD") {
      return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>`;
    }
    if (t === "LICENCIA") {
      return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="6"></circle><path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11"></path></svg>`;
    }
    if (t === "INSPECCION") {
      return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"></path><rect x="8" y="2" width="8" height="4" rx="1" ry="1"></rect></svg>`;
    }
    if (t === "PAGO") {
      return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>`;
    }
    if (t === "ALERTA") {
      return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
    }
    return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
  }

  // Carga inicial
  await loadNotifications();

  // Si se solicitó abrir una notificación por ID específico
  if (initialId) {
    openNotificacionDetalleModal(initialId, {
      onReadUpdated: async () => {
        await refreshUnreadNotificationsBadge();
        await loadNotifications();
      }
    });
  }
}
