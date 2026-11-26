/**
 * Componente reutilizable de Paginación sincronizado con PaginaResponse del Backend (PROMPT 11 - Sección 17)
 */
import { clearChildren } from "../ui.js";

const ALLOWED_PAGE_SIZES = [10, 20, 50];

/**
 * Renderiza los controles de paginación a partir de un objeto PaginaResponse del backend.
 *
 * @param {HTMLElement} container Elemento contenedor donde se montará la barra de paginación
 * @param {Object} options
 * @param {number} options.pagina Número de página actual (base 0 en Spring Data)
 * @param {number} options.tamano Tamaño de página actual
 * @param {number} options.totalElementos Total de registros encontrados
 * @param {number} options.totalPaginas Total de páginas
 * @param {boolean} options.primera Indica si es la primera página
 * @param {boolean} options.ultima Indica si es la última página
 * @param {(newPage: number) => void} options.onPageChange Callback al cambiar de página
 * @param {(newSize: number) => void} [options.onSizeChange] Callback al cambiar tamaño de página
 */
export function renderPagination(
  container,
  {
    pagina = 0,
    tamano = 20,
    totalElementos = 0,
    totalPaginas = 0,
    primera = true,
    ultima = true,
    onPageChange,
    onSizeChange
  } = {}
) {
  if (!container) return;
  clearChildren(container);

  const bar = document.createElement("nav");
  bar.className = "pagination-bar";
  bar.setAttribute("aria-label", "Navegación de páginas de la tabla");

  const info = document.createElement("div");
  info.className = "pagination-info";

  if (totalElementos === 0) {
    info.textContent = "Mostrando 0 registros";
  } else {
    const startRecord = pagina * tamano + 1;
    const endRecord = Math.min((pagina + 1) * tamano, totalElementos);
    const strongRange = document.createElement("strong");
    strongRange.textContent = `${startRecord}–${endRecord}`;
    const strongTotal = document.createElement("strong");
    strongTotal.textContent = String(totalElementos);

    info.append(
      document.createTextNode("Mostrando "),
      strongRange,
      document.createTextNode(" de "),
      strongTotal,
      document.createTextNode(
        ` registros (Página ${pagina + 1} de ${Math.max(1, totalPaginas)})`
      )
    );
  }

  const controls = document.createElement("div");
  controls.className = "pagination-controls";

  if (typeof onSizeChange === "function") {
    const sizeLabel = document.createElement("label");
    sizeLabel.className = "sr-only";
    sizeLabel.textContent = "Registros por página";

    const sizeSelect = document.createElement("select");
    sizeSelect.className = "pagination-size-select";
    sizeSelect.setAttribute("aria-label", "Cantidad de registros por página");

    for (const sizeOpt of ALLOWED_PAGE_SIZES) {
      const opt = document.createElement("option");
      opt.value = String(sizeOpt);
      opt.textContent = `${sizeOpt} por página`;
      if (Number(tamano) === sizeOpt) {
        opt.selected = true;
      }
      sizeSelect.appendChild(opt);
    }

    sizeSelect.addEventListener("change", () => {
      const nextSize = Number(sizeSelect.value) || 20;
      onSizeChange(nextSize);
    });

    controls.append(sizeLabel, sizeSelect);
  }

  const safeTotalPages = Math.max(1, totalPaginas);

  const prevBtn = document.createElement("button");
  prevBtn.type = "button";
  prevBtn.className = "page-btn";
  prevBtn.textContent = "← Anterior";
  prevBtn.disabled = primera || pagina <= 0;
  prevBtn.setAttribute("aria-label", "Ir a la página anterior");
  prevBtn.addEventListener("click", () => {
    if (pagina > 0 && typeof onPageChange === "function") {
      onPageChange(pagina - 1);
    }
  });
  controls.appendChild(prevBtn);

  // Botones numerados acotados alrededor de la página actual
  const windowRadius = 2;
  const startPage = Math.max(0, pagina - windowRadius);
  const endPage = Math.min(safeTotalPages - 1, pagina + windowRadius);

  for (let p = startPage; p <= endPage; p += 1) {
    const numBtn = document.createElement("button");
    numBtn.type = "button";
    numBtn.className = `page-btn${p === pagina ? " is-active" : ""}`;
    numBtn.textContent = String(p + 1);
    numBtn.setAttribute("aria-label", `Página ${p + 1}`);
    if (p === pagina) {
      numBtn.setAttribute("aria-current", "page");
    }
    numBtn.addEventListener("click", () => {
      if (p !== pagina && typeof onPageChange === "function") {
        onPageChange(p);
      }
    });
    controls.appendChild(numBtn);
  }

  const nextBtn = document.createElement("button");
  nextBtn.type = "button";
  nextBtn.className = "page-btn";
  nextBtn.textContent = "Siguiente →";
  nextBtn.disabled = ultima || pagina >= safeTotalPages - 1;
  nextBtn.setAttribute("aria-label", "Ir a la página siguiente");
  nextBtn.addEventListener("click", () => {
    if (!ultima && pagina < safeTotalPages - 1 && typeof onPageChange === "function") {
      onPageChange(pagina + 1);
    }
  });
  controls.appendChild(nextBtn);

  bar.append(info, controls);
  container.appendChild(bar);
}
