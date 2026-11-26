/**
 * Enrutador de secciones internas del contenedor administrativo (PROMPT 09)
 */
import { MENU_ITEMS } from "./config.js";
import { hasPermission } from "./auth.js";

let activeRouteKey = "dashboard";
let routeChangeHandler = null;

export function getActiveRouteKey() {
  return activeRouteKey;
}

export function resolveRouteKeyFromHash() {
  const raw = window.location.hash ? window.location.hash.replace(/^#/, "").trim().toLowerCase() : "";
  if (!raw) {
    return "dashboard";
  }
  const cleanKey = raw.split(/[/?]/)[0];
  const found = MENU_ITEMS.find((item) => item.key === cleanKey);
  return found ? found.key : "dashboard";
}

export function navigateToSection(sectionKey) {
  const target = MENU_ITEMS.find((item) => item.key === sectionKey) ? sectionKey : "dashboard";
  if (window.location.hash !== `#${target}`) {
    window.location.hash = `#${target}`;
  } else if (typeof routeChangeHandler === "function") {
    routeChangeHandler(target);
  }
}

export function initRouter(user, onRouteChange) {
  routeChangeHandler = (requestedKey) => {
    const item = MENU_ITEMS.find((m) => m.key === requestedKey) || MENU_ITEMS[0];
    const allowed = hasPermission(user, item.key);
    activeRouteKey = item.key;
    onRouteChange(item, allowed);
  };

  window.addEventListener("hashchange", () => {
    routeChangeHandler(resolveRouteKeyFromHash());
  });

  routeChangeHandler(resolveRouteKeyFromHash());
}
