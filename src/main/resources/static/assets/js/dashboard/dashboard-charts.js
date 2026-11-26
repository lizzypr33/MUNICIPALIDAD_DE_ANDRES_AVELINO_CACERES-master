/**
 * Gráficos estadísticos SVG en JavaScript puro para el Dashboard Municipal (PROMPT 10 - Secciones 9 y 10)
 * 1. Gráfico de líneas: Solicitudes por mes (Enero a Diciembre)
 * 2. Gráfico circular tipo dona: Solicitudes por estado (con cálculo seguro de porcentajes y tabla accesible)
 */
import { clearChildren } from "../ui.js";
import { formatCount } from "./dashboard-cards.js";

const SVG_NS = "http://www.w3.org/2000/svg";

const MESES_NOMBRES = Object.freeze([
  "Enero",
  "Febrero",
  "Marzo",
  "Abril",
  "Mayo",
  "Junio",
  "Julio",
  "Agosto",
  "Septiembre",
  "Octubre",
  "Noviembre",
  "Diciembre"
]);

const MESES_CORTOS = Object.freeze([
  "Ene", "Feb", "Mar", "Abr", "May", "Jun",
  "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
]);

const CONFIG_ESTADOS_DONA = Object.freeze([
  { key: "PRESENTADA", label: "Pendientes (Presentadas)", color: "#F59E0B" },
  { key: "EN_REVISION", label: "En revisión", color: "#2563EB" },
  { key: "OBSERVADA", label: "Observadas", color: "#EA580C" },
  { key: "SUBSANADA", label: "Subsanadas", color: "#0891B2" },
  { key: "APROBADA", label: "Aprobadas", color: "#16A34A" },
  { key: "RECHAZADA", label: "Rechazadas", color: "#DC2626" },
  { key: "ARCHIVADA", label: "Archivadas", color: "#64748B" },
  { key: "BORRADOR", label: "En borrador", color: "#8B5CF6" }
]);

function createSvgElement(tag, attrs = {}) {
  const el = document.createElementNS(SVG_NS, tag);
  for (const [k, v] of Object.entries(attrs)) {
    el.setAttribute(k, String(v));
  }
  return el;
}

/**
 * Determina el año de referencia para los 12 meses del gráfico lineal.
 */
function resolveTargetYear(estadisticas) {
  if (estadisticas?.fechaHasta && /^\d{4}/.test(estadisticas.fechaHasta)) {
    return Number(estadisticas.fechaHasta.substring(0, 4));
  }
  const porMes = Array.isArray(estadisticas?.porMes) ? estadisticas.porMes : [];
  if (porMes.length > 0) {
    const ultimoPeriodo = porMes[porMes.length - 1]?.periodo || "";
    if (/^\d{4}-\d{2}$/.test(ultimoPeriodo)) {
      return Number(ultimoPeriodo.substring(0, 4));
    }
  }
  return new Date().getFullYear();
}

/**
 * Construye el arreglo de 12 meses (Enero a Diciembre) con sus cantidades reales.
 */
export function buildMonthlySeries(estadisticas) {
  const year = resolveTargetYear(estadisticas);
  const map = new Map();
  const porMes = Array.isArray(estadisticas?.porMes) ? estadisticas.porMes : [];

  for (const item of porMes) {
    if (item && typeof item.periodo === "string") {
      map.set(item.periodo, Number(item.cantidad) || 0);
    }
  }

  return MESES_NOMBRES.map((nombreMes, idx) => {
    const monthNum = String(idx + 1).padStart(2, "0");
    const key = `${year}-${monthNum}`;
    return {
      index: idx,
      periodo: key,
      mesNombre: nombreMes,
      mesCorto: MESES_CORTOS[idx],
      cantidad: map.get(key) ?? 0
    };
  });
}

/**
 * Renderiza el gráfico de líneas "Solicitudes por mes" (Sección 9).
 */
export function renderSolicitudesPorMesChart(container, estadisticas) {
  if (!container) return;
  clearChildren(container);

  const series = buildMonthlySeries(estadisticas);
  const totalAnual = series.reduce((acc, s) => acc + s.cantidad, 0);
  const year = resolveTargetYear(estadisticas);

  if (totalAnual === 0) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    const msg = document.createElement("p");
    msg.textContent = `No se registran solicitudes presentadas para los meses del año ${year} en el período consultado.`;
    empty.appendChild(msg);
    container.appendChild(empty);
    return;
  }

  const wrapper = document.createElement("div");
  wrapper.className = "chart-wrapper";

  const svgContainer = document.createElement("div");
  svgContainer.className = "chart-svg-container";

  const width = 680;
  const height = 250;
  const padLeft = 44;
  const padRight = 24;
  const padTop = 24;
  const padBottom = 42;
  const plotWidth = width - padLeft - padRight;
  const plotHeight = height - padTop - padBottom;

  const maxVal = Math.max(4, ...series.map((s) => s.cantidad));
  const yTicks = 4;
  const stepMax = Math.ceil(maxVal / yTicks) * yTicks;

  const svg = createSvgElement("svg", {
    viewBox: `0 0 ${width} ${height}`,
    class: "line-chart-svg",
    role: "img",
    "aria-label": `Gráfico de líneas de solicitudes por mes del año ${year}. Total: ${totalAnual} solicitudes.`
  });

  // Cuadrícula horizontal y etiquetas del eje Y
  for (let i = 0; i <= yTicks; i++) {
    const val = Math.round((stepMax / yTicks) * i);
    const y = padTop + plotHeight - (i / yTicks) * plotHeight;

    const line = createSvgElement("line", {
      x1: padLeft,
      y1: y,
      x2: width - padRight,
      y2: y,
      stroke: "#E2E8F0",
      "stroke-width": "1"
    });
    svg.appendChild(line);

    const label = createSvgElement("text", {
      x: padLeft - 8,
      y: y + 4,
      "text-anchor": "end",
      fill: "#64748B",
      "font-size": "11"
    });
    label.textContent = String(val);
    svg.appendChild(label);
  }

  // Coordenadas de los 12 meses
  const coords = series.map((item, idx) => {
    const x = padLeft + (idx / (series.length - 1)) * plotWidth;
    const y = padTop + plotHeight - (item.cantidad / stepMax) * plotHeight;
    return { ...item, x, y };
  });

  // Área suave bajo la curva
  const areaPoints = [
    `${coords[0].x},${padTop + plotHeight}`,
    ...coords.map((c) => `${c.x},${c.y}`),
    `${coords[coords.length - 1].x},${padTop + plotHeight}`
  ].join(" ");

  const polygon = createSvgElement("polygon", {
    points: areaPoints,
    fill: "rgba(37, 99, 235, 0.10)"
  });
  svg.appendChild(polygon);

  // Línea azul principal
  const polylinePoints = coords.map((c) => `${c.x},${c.y}`).join(" ");
  const polyline = createSvgElement("polyline", {
    points: polylinePoints,
    fill: "none",
    stroke: "#0B4A87",
    "stroke-width": "2.75",
    "stroke-linecap": "round",
    "stroke-linejoin": "round"
  });
  svg.appendChild(polyline);

  // Puntos visibles y etiquetas del eje X
  for (const pt of coords) {
    const circle = createSvgElement("circle", {
      cx: pt.x,
      cy: pt.y,
      r: "4.5",
      fill: "#FFFFFF",
      stroke: "#2563EB",
      "stroke-width": "2.5"
    });
    const title = createSvgElement("title");
    title.textContent = `${pt.mesNombre} ${year}: ${pt.cantidad} solicitud(es)`;
    circle.appendChild(title);
    svg.appendChild(circle);

    if (pt.cantidad > 0) {
      const valueTag = createSvgElement("text", {
        x: pt.x,
        y: pt.y - 9,
        "text-anchor": "middle",
        fill: "#0B4A87",
        "font-size": "10.5",
        "font-weight": "700"
      });
      valueTag.textContent = String(pt.cantidad);
      svg.appendChild(valueTag);
    }

    const xLabel = createSvgElement("text", {
      x: pt.x,
      y: height - 14,
      "text-anchor": "middle",
      fill: "#172B4D",
      "font-size": "11",
      "font-weight": "600"
    });
    xLabel.textContent = pt.mesCorto;
    svg.appendChild(xLabel);
  }

  svgContainer.appendChild(svg);

  // Resumen tabular accesible para lectores de pantalla
  const srTable = document.createElement("table");
  srTable.className = "sr-only";
  const caption = document.createElement("caption");
  caption.textContent = `Solicitudes por mes (${year})`;
  srTable.appendChild(caption);
  const tbody = document.createElement("tbody");
  for (const item of series) {
    const tr = document.createElement("tr");
    const th = document.createElement("th");
    th.textContent = item.mesNombre;
    const td = document.createElement("td");
    td.textContent = String(item.cantidad);
    tr.append(th, td);
    tbody.appendChild(tr);
  }
  srTable.appendChild(tbody);

  wrapper.append(svgContainer, srTable);
  container.appendChild(wrapper);
}

/**
 * Calcula los porcentajes por estado evitando divisiones entre cero.
 */
export function buildStateDistribution(estadisticas) {
  const porEstado = estadisticas?.porEstado || {};
  const rawItems = CONFIG_ESTADOS_DONA.map((cfg) => {
    const count = Number(porEstado[cfg.key]) || 0;
    return { ...cfg, count };
  });

  const total = rawItems.reduce((acc, item) => acc + item.count, 0);

  return {
    total,
    items: rawItems.map((item) => {
      const percentage = total > 0 ? (item.count / total) * 100 : 0;
      return {
        ...item,
        percentage,
        percentageFormatted: `${percentage.toFixed(1)}%`
      };
    })
  };
}

/**
 * Renderiza el gráfico circular tipo dona "Solicitudes por estado" (Sección 10).
 */
export function renderSolicitudesPorEstadoChart(container, estadisticas) {
  if (!container) return;
  clearChildren(container);

  const { total, items } = buildStateDistribution(estadisticas);

  const layout = document.createElement("div");
  layout.className = "donut-layout";

  const size = 190;
  const cx = size / 2;
  const cy = size / 2;
  const radius = 66;
  const circumference = 2 * Math.PI * radius;

  const svg = createSvgElement("svg", {
    viewBox: `0 0 ${size} ${size}`,
    class: "donut-svg",
    role: "img",
    "aria-label": `Gráfico circular de solicitudes por estado. Total: ${total} solicitudes.`
  });

  // Anillo base gris claro
  const baseTrack = createSvgElement("circle", {
    cx,
    cy,
    r: radius,
    fill: "none",
    stroke: "#E2E8F0",
    "stroke-width": "24"
  });
  svg.appendChild(baseTrack);

  if (total > 0) {
    let accumulatedRatio = 0;
    for (const seg of items) {
      if (seg.count <= 0) continue;
      const ratio = seg.count / total;
      const dashLength = ratio * circumference;
      const dashOffset = -accumulatedRatio * circumference;
      accumulatedRatio += ratio;

      const arc = createSvgElement("circle", {
        cx,
        cy,
        r: radius,
        fill: "none",
        stroke: seg.color,
        "stroke-width": "24",
        "stroke-dasharray": `${dashLength} ${circumference}`,
        "stroke-dashoffset": String(dashOffset),
        transform: `rotate(-90 ${cx} ${cy})`
      });
      const title = createSvgElement("title");
      title.textContent = `${seg.label}: ${seg.count} (${seg.percentageFormatted})`;
      arc.appendChild(title);
      svg.appendChild(arc);
    }
  }

  // Etiqueta central con el total de solicitudes
  const centerTotal = createSvgElement("text", {
    x: cx,
    y: cy - 2,
    "text-anchor": "middle",
    fill: "#172B4D",
    "font-size": "22",
    "font-weight": "800"
  });
  centerTotal.textContent = formatCount(total);

  const centerSub = createSvgElement("text", {
    x: cx,
    y: cy + 16,
    "text-anchor": "middle",
    fill: "#64748B",
    "font-size": "10.5",
    "font-weight": "700"
  });
  centerSub.textContent = "SOLICITUDES";

  svg.append(centerTotal, centerSub);

  // Leyenda accesible con cantidades y porcentajes
  const legend = document.createElement("ul");
  legend.className = "donut-legend";
  legend.setAttribute("aria-label", "Distribución de solicitudes por estado");

  for (const seg of items) {
    const li = document.createElement("li");
    li.className = "donut-legend-item";

    const left = document.createElement("span");
    left.className = "donut-legend-left";

    const swatch = document.createElement("span");
    swatch.className = "donut-swatch";
    swatch.style.backgroundColor = seg.color;
    swatch.setAttribute("aria-hidden", "true");

    const nameSpan = document.createElement("span");
    nameSpan.textContent = seg.label;

    left.append(swatch, nameSpan);

    const right = document.createElement("span");
    right.className = "donut-legend-values";
    right.textContent = `${formatCount(seg.count)} (${seg.percentageFormatted})`;

    li.append(left, right);
    legend.appendChild(li);
  }

  layout.append(svg, legend);
  container.appendChild(layout);
}
