/**
 * Formulario de Registro de Declaración de Pago Municipal (PROMPT 13 - Sección 23)
 */
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { listarSolicitudes } from "../solicitudes/solicitudes-api.js";
import { registrarPago } from "./pagos-api.js";

export async function renderRegistrarPagoView({
  container,
  preselectedSolicitudId = null,
  onPagoRegistrado = null,
  onCancel = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  const wrapper = createElement("div", { className: "card" });
  const cardHeader = createElement("div", { className: "card-header" });
  cardHeader.appendChild(
    createElement("h2", { className: "card-title", textContent: "Registrar Declaración de Pago Municipal" })
  );
  wrapper.appendChild(cardHeader);

  const cardBody = createElement("div", { className: "card-body" });
  wrapper.appendChild(cardBody);
  container.appendChild(wrapper);

  cardBody.appendChild(
    createElement("div", { className: "loading-state", textContent: "Cargando expedientes disponibles..." })
  );

  let solicitudesList = [];

  try {
    const solPage = await listarSolicitudes({ size: 50 });
    solicitudesList = Array.isArray(solPage?.contenido) ? solPage.contenido : [];
  } catch (_e) {
    solicitudesList = [];
  }

  clearChildren(cardBody);

  const form = createElement("form", { className: "modal-form", attrs: { novalidate: "novalidate" } });

  // Selección de Solicitud / Expediente
  const solGroup = createElement("div", { className: "form-group" });
  solGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "pago-solicitud-select" }, textContent: "Expediente / Solicitud Vinculada *" })
  );
  const solSelect = createElement("select", { id: "pago-solicitud-select", className: "form-control" });
  solSelect.appendChild(createElement("option", { value: "", textContent: "— Seleccione expediente —" }));

  for (const s of solicitudesList) {
    const opt = createElement("option", {
      value: String(s.id),
      textContent: `${s.numeroExpediente || `Borrador #${s.id}`} — ${s.ciudadano?.nombres || s.ciudadanoNombreCompleto || "Titular"} (${s.tipoTramite?.nombre || "Trámite"})`
    });
    if (preselectedSolicitudId && Number(preselectedSolicitudId) === Number(s.id)) {
      opt.selected = true;
    }
    solSelect.appendChild(opt);
  }
  solGroup.appendChild(solSelect);

  // Código de Operación
  const codGroup = createElement("div", { className: "form-group" });
  codGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "pago-codigo-op" }, textContent: "Código de Operación o N° de Recibo / Depósito *" })
  );
  const codIn = createElement("input", {
    type: "text",
    id: "pago-codigo-op",
    className: "form-control",
    attrs: { maxlength: "50", placeholder: "Ej.: BCP-89218273 o BN-0038291..." }
  });
  codGroup.appendChild(codIn);

  // Concepto de Pago
  const concGroup = createElement("div", { className: "form-group" });
  concGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "pago-concepto" }, textContent: "Concepto del Pago *" })
  );
  const concIn = createElement("input", {
    type: "text",
    id: "pago-concepto",
    className: "form-control",
    attrs: { maxlength: "200", placeholder: "Ej.: Tasa por Tramitación de Licencia de Funcionamiento..." }
  });
  concIn.value = "Tasa Administrativa de Licencia de Funcionamiento";
  concGroup.appendChild(concIn);

  // Monto y Moneda
  const rowMonto = createElement("div", { className: "table-actions", attrs: { style: "gap: 1rem;" } });

  const montoGroup = createElement("div", { className: "form-group", attrs: { style: "flex: 1;" } });
  montoGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "pago-monto" }, textContent: "Monto Pagado (S/) *" })
  );
  const montoIn = createElement("input", {
    type: "number",
    id: "pago-monto",
    className: "form-control",
    attrs: { min: "0.01", step: "0.01", placeholder: "0.00" }
  });
  montoGroup.appendChild(montoIn);

  const monGroup = createElement("div", { className: "form-group", attrs: { style: "width: 120px;" } });
  monGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "pago-moneda" }, textContent: "Moneda" })
  );
  const monSelect = createElement("select", { id: "pago-moneda", className: "form-control" });
  monSelect.appendChild(new Option("PEN (S/)", "PEN"));
  monGroup.appendChild(monSelect);
  rowMonto.append(montoGroup, monGroup);

  // Fecha Declarada y Medio de Pago
  const rowDateMedio = createElement("div", { className: "table-actions", attrs: { style: "gap: 1rem;" } });

  const dateGroup = createElement("div", { className: "form-group", attrs: { style: "flex: 1;" } });
  dateGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "pago-fecha" }, textContent: "Fecha y Hora del Depósito / Pago *" })
  );
  const dateIn = createElement("input", {
    type: "datetime-local",
    id: "pago-fecha",
    className: "form-control"
  });
  const now = new Date();
  dateIn.value = now.toISOString().slice(0, 16);
  dateGroup.appendChild(dateIn);

  const medioGroup = createElement("div", { className: "form-group", attrs: { style: "flex: 1;" } });
  medioGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "pago-medio" }, textContent: "Medio de Pago *" })
  );
  const medioSelect = createElement("select", { id: "pago-medio", className: "form-control" });
  medioSelect.appendChild(new Option("Depósito Bancario (Ventanilla / Agente)", "DEPOSITO_BANCARIO"));
  medioSelect.appendChild(new Option("Transferencia Interbancaria (CCI)", "TRANSFERENCIA"));
  medioSelect.appendChild(new Option("Caja / Ventanilla Municipal", "VENTANILLA_MUNICIPAL"));
  medioSelect.appendChild(new Option("Billetera Digital (Yape / Plin)", "BILLETERA_DIGITAL"));
  medioSelect.appendChild(new Option("Pago Electrónico / Pasarela", "PAGO_ELECTRONICO"));
  medioGroup.appendChild(medioSelect);
  rowDateMedio.append(dateGroup, medioGroup);

  // Observaciones
  const obsGroup = createElement("div", { className: "form-group" });
  obsGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "pago-observaciones" }, textContent: "Observaciones de la Declaración" })
  );
  const obsIn = createElement("textarea", {
    id: "pago-observaciones",
    className: "form-control",
    attrs: { rows: "2", maxlength: "2000", placeholder: "Número de cuenta de origen, voucher fedateado, etc." }
  });
  obsGroup.appendChild(obsIn);

  form.append(solGroup, codGroup, concGroup, rowMonto, rowDateMedio, obsGroup);

  // Acciones
  const actionsRow = createElement("div", { className: "table-actions", attrs: { style: "justify-content: flex-end; gap: 0.75rem;" } });
  const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
  const submitBtn = createElement("button", { type: "submit", className: "btn btn-primary", textContent: "Registrar Declaración de Pago" });

  actionsRow.append(cancelBtn, submitBtn);
  form.appendChild(actionsRow);
  cardBody.appendChild(form);

  cancelBtn.addEventListener("click", () => {
    if (typeof onCancel === "function") onCancel();
  });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!solSelect.value) {
      showToast("Seleccione el expediente al que corresponde el pago.", "warning");
      return;
    }
    const codOp = codIn.value.trim();
    if (!codOp) {
      showToast("Ingrese el código de operación o comprobante de pago.", "warning");
      return;
    }
    const concepto = concIn.value.trim();
    if (!concepto) {
      showToast("Ingrese el concepto del pago.", "warning");
      return;
    }
    const monto = parseFloat(montoIn.value);
    if (!monto || monto <= 0) {
      showToast("El monto debe ser un valor positivo mayor a 0.", "warning");
      return;
    }
    if (!dateIn.value) {
      showToast("Indique la fecha y hora de la operación bancaria.", "warning");
      return;
    }

    try {
      submitBtn.disabled = true;
      submitBtn.textContent = "Registrando pago...";

      const formattedDate = dateIn.value.length === 16 ? `${dateIn.value}:00` : dateIn.value;

      const payload = {
        solicitudId: Number(solSelect.value),
        codigoOperacion: codOp,
        concepto,
        monto,
        moneda: monSelect.value || "PEN",
        fechaPago: formattedDate,
        medioPago: medioSelect.value,
        observaciones: obsIn.value.trim() || null
      };

      const result = await registrarPago(payload);
      showToast(`Pago registrado exitosamente. Estado: PENDIENTE DE VERIFICACIÓN.`, "success");

      if (typeof onPagoRegistrado === "function") {
        onPagoRegistrado(result);
      }
    } catch (err) {
      showToast(err.message || "Error al registrar pago.", "error");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Registrar Declaración de Pago";
    }
  });
}
