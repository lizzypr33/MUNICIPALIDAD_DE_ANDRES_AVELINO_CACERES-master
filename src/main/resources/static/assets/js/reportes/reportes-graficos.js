/**
 * Componente gráfico SVG accesible para Reportes Municipales (PROMPT 14 - Sección 13)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { clearChildren } from "../ui.js";

const SVG_NS = "http://www.w3.org/2000/svg";

const PALETTE = [
  "#2563EB", "#16A34A", "#F59E0B", "#EA580C",
  "#DC2626", "#8B5CF6", "#0891B2", "#64748B"
];

function createSvgElement(tag, attrs = {}) {
  const el = document.createElementNS(SVG_NS, tag);
  for (const [k, v] of Object.entries(attrs)) {
    el.setAttribute(k, String(v));
  }
  return el;
}

/**
 * Renderiza un gráfico tipo dona SVG accesible con leyenda y tabla accesible.
 *
 * @param {HTMLElement} container Contenedor donde se montará el gráfico
 * @param {Array<{ label: string, value: number, color?: string }>} data
 * @param {string} title Título del gráfico
 */
export function renderDonutChart(container, data = [], title = "Distribución del Reporte") {
  if (!container) return;
  clearChildren(container);

  const cleanData = (Array.isArray(data) ? data : [])
    .filter(item => item && Number(item.value) > 0)
    .map((item, idx) => ({
      label: item.label || "Sin etiqueta",
      value: Number(item.value),
      color: item.color || PALETTE[idx % PALETTE.length]
    }));

  const total = cleanData.reduce((acc, curr) => acc + curr.value, 0);

  const card = document.createElement("div");
  card.className = "reportes-chart-card";

  const header = document.createElement("div");
  header.className = "reportes-chart-header";

  const h3 = document.createElement("h3");
  h3.className = "reportes-chart-title";
  h3.textContent = title;

  const totalSpan = document.createElement("span");
  totalSpan.className = "reportes-kpi-sub";
  totalSpan.textContent = `Total analizado: ${total} registros`;

  header.append(h3, totalSpan);
  card.appendChild(header);

  if (cleanData.length === 0 || total === 0) {
    const emptyMsg = document.createElement("p");
    emptyMsg.className = "text-muted";
    emptyMsg.style.textAlign = "center";
    emptyMsg.style.padding = "2rem 0";
    emptyMsg.textContent = "No hay datos numéricos suficientes para proyectar el gráfico.";
    card.appendChild(emptyMsg);
    container.appendChild(card);
    return;
  }

  const canvasWrap = document.createElement("div");
  canvasWrap.className = "reportes-chart-canvas-wrap";

  // Dimensiones del SVG
  const size = 260;
  const strokeWidth = 36;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const center = size / 2;

  const svg = createSvgElement("svg", {
    viewBox: `0 0 ${size} ${size}`,
    className: "reportes-chart-svg",
    role: "img",
    "aria-label": `${title}. Total: ${total}.`
  });

  // Fondo del donut
  const bgCircle = createSvgElement("circle", {
    cx: center,
    cy: center,
    r: radius,
    fill: "none",
    stroke: "#F1F5F9",
    "stroke-width": strokeWidth
  });
  svg.appendChild(bgCircle);

  let accumulatedOffset = 0;

  cleanData.forEach(item => {
    const ratio = item.value / total;
    const dashLength = circumference * ratio;
    const strokeOffset = circumference - accumulatedOffset;

    const segment = createSvgElement("circle", {
      cx: center,
      cy: center,
      r: radius,
      fill: "none",
      stroke: item.color,
      "stroke-width": strokeWidth,
      "stroke-dasharray": `${dashLength} ${circumference}`,
      "stroke-dashoffset": String(strokeOffset),
      transform: `rotate(-90 ${center} ${center})`,
      style: "transition: stroke-width 0.2s ease;"
    });

    const titleEl = createSvgElement("title");
    titleEl.textContent = `${item.label}: ${item.value} (${((item.value / total) * 100).toFixed(1)}%)`;
    segment.appendChild(titleEl);

    svg.appendChild(segment);
    accumulatedOffset += dashLength;
  });

  // Texto central con el total
  const textTotal = createSvgElement("text", {
    x: center,
    y: center - 4,
    "text-anchor": "middle",
    "font-size": "24",
    "font-weight": "800",
    fill: "#172B4D"
  });
  textTotal.textContent = String(total);

  const textLabel = createSvgElement("text", {
    x: center,
    y: center + 16,
    "text-anchor": "middle",
    "font-size": "11",
    "font-weight": "600",
    fill: "#64748B"
  });
  textLabel.textContent = "TOTAL";

  svg.append(textTotal, textLabel);
  canvasWrap.appendChild(svg);
  card.appendChild(canvasWrap);

  // Leyenda accesible
  const legend = document.createElement("div");
  legend.className = "reportes-chart-legend";

  cleanData.forEach(item => {
    const itemEl = document.createElement("div");
    itemEl.className = "reportes-legend-item";

    const dot = document.createElement("span");
    dot.className = "reportes-legend-color";
    dot.style.backgroundColor = item.color;

    const pct = ((item.value / total) * 100).toFixed(1);
    const text = document.createElement("span");
    text.textContent = `${item.label}: ${item.value} (${pct}%)`;

    itemEl.append(dot, text);
    legend.appendChild(itemEl);
  });

  card.appendChild(legend);

  // Tabla accesible para lectores de pantalla
  const srTable = document.createElement("table");
  srTable.className = "sr-only";
  srTable.innerHTML = `
    <caption>${title} (Alternativa textual accesible)</caption>
    <thead><tr><th scope="col">Categoría</th><th scope="col">Cantidad</th><th scope="col">Porcentaje</th></tr></thead>
    <tbody>
      ${cleanData.map(d => `<tr><td>${d.label}</td><td>${d.value}</td><td>${((d.value / total) * 100).toFixed(1)}%</td></tr>`).join("")}
    </tbody>
  `;
  card.appendChild(srTable);

  container.appendChild(card);
}
