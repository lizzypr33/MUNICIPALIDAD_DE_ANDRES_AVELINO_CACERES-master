/**
 * Módulo de descarga y exportación oficial de reportes en PDF y CSV (PROMPT 14 - Secciones 14, 15 y 16)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { showToast } from "../ui.js";
import { downloadReportePdf, downloadReporteCsv } from "./reportes-api.js";

function resolveFilename(disposition, fallbackName) {
  if (disposition) {
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    const plainMatch = disposition.match(/filename="?([^";]+)"?/i);
    if (utf8Match && utf8Match[1]) {
      try {
        return decodeURIComponent(utf8Match[1]);
      } catch (_e) {
        return utf8Match[1];
      }
    } else if (plainMatch && plainMatch[1]) {
      return plainMatch[1];
    }
  }
  return fallbackName;
}

function triggerBlobDownload(blob, filename) {
  const objectUrl = URL.createObjectURL(blob);
  try {
    const anchor = document.createElement("a");
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.rel = "noopener";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  } finally {
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1500);
  }
}

/**
 * Ejecuta la exportación a PDF consultando el endpoint binario real del backend.
 */
export async function exportarReportePdf(tipo, filtros = {}, triggerBtn = null) {
  if (triggerBtn) {
    triggerBtn.disabled = true;
    triggerBtn.setAttribute("data-loading", "true");
  }

  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const fallback = `Reporte_${tipo}_${dateStr}.pdf`;

  try {
    showToast({
      type: "info",
      title: "Generando reporte PDF",
      message: "Procesando documento oficial en el servidor..."
    });

    const { blob, disposition, contentType } = await downloadReportePdf(tipo, filtros);

    if (contentType && !contentType.includes("pdf") && !contentType.includes("octet-stream")) {
      throw new Error("El archivo recibido no tiene un formato PDF válido.");
    }

    const filename = resolveFilename(disposition, fallback);
    triggerBlobDownload(blob, filename);

    showToast({
      type: "success",
      title: "Descarga completada",
      message: `El archivo ${filename} se descargó correctamente.`
    });
  } catch (err) {
    showToast({
      type: "error",
      title: "Error al exportar PDF",
      message: err.message || "No se pudo generar el archivo PDF en este momento."
    });
  } finally {
    if (triggerBtn) {
      triggerBtn.disabled = false;
      triggerBtn.removeAttribute("data-loading");
    }
  }
}

/**
 * Ejecuta la exportación a CSV consultando el endpoint binario real del backend.
 */
export async function exportarReporteCsv(tipo, filtros = {}, triggerBtn = null) {
  if (triggerBtn) {
    triggerBtn.disabled = true;
    triggerBtn.setAttribute("data-loading", "true");
  }

  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const fallback = `Reporte_${tipo}_${dateStr}.csv`;

  try {
    showToast({
      type: "info",
      title: "Generando reporte CSV",
      message: "Exportando registros en formato compatible..."
    });

    const { blob, disposition } = await downloadReporteCsv(tipo, filtros);
    const filename = resolveFilename(disposition, fallback);
    triggerBlobDownload(blob, filename);

    showToast({
      type: "success",
      title: "Descarga completada",
      message: `El archivo ${filename} se exportó correctamente.`
    });
  } catch (err) {
    showToast({
      type: "error",
      title: "Error al exportar CSV",
      message: err.message || "No se pudo exportar el archivo CSV en este momento."
    });
  } finally {
    if (triggerBtn) {
      triggerBtn.disabled = false;
      triggerBtn.removeAttribute("data-loading");
    }
  }
}
