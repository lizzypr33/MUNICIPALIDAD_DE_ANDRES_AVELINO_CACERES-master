/**
 * Módulo de Gestión Visual de Documentos Adjuntos y Cumplimiento de Requisitos (PROMPT 12 - Secciones 14, 15 y 23)
 */
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { openModal } from "../components/modal.js";
import { confirmAction } from "../components/confirm-dialog.js";
import {
  subirDocumentoSolicitud,
  descargarDocumento,
  retirarDocumento
} from "./documentos-api.js";
import {
  formatFileSize,
  validateMunicipalFile,
  createUploadDropzone
} from "./upload.js";

function formatDateTime(isoString) {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return String(isoString);
  return date.toLocaleString("es-PE", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function resolveMimeBadgeText(tipoMime = "", nombreOriginal = "") {
  const mime = String(tipoMime || "").toLowerCase();
  if (mime.includes("pdf") || String(nombreOriginal).toLowerCase().endsWith(".pdf")) {
    return "PDF";
  }
  if (mime.includes("png") || String(nombreOriginal).toLowerCase().endsWith(".png")) {
    return "PNG";
  }
  if (
    mime.includes("jpeg") ||
    mime.includes("jpg") ||
    String(nombreOriginal).toLowerCase().endsWith(".jpg") ||
    String(nombreOriginal).toLowerCase().endsWith(".jpeg")
  ) {
    return "JPG";
  }
  return "DOC";
}

function createEstadoDocumentoBadge(estado) {
  const clean = String(estado || "PENDIENTE").toUpperCase();
  const variant =
    clean === "VALIDADO"
      ? "badge--success"
      : clean === "RECHAZADO"
        ? "badge--danger"
        : "badge--warning";
  return createElement("span", {
    className: `badge ${variant}`,
    textContent: clean
  });
}

/**
 * Evalúa el cumplimiento de los requisitos obligatorios frente a los documentos activos de la solicitud.
 * Coincide con la regla de negocio del backend en SolicitudServiceImpl.presentar().
 */
export function evaluateRequirementsCompliance(requisitos = [], documentos = []) {
  const activeDocs = (Array.isArray(documentos) ? documentos : []).filter(
    (doc) => doc && doc.activo !== false && String(doc.estado || "").toUpperCase() !== "RECHAZADO"
  );

  const docsByRequisitoId = new Map();
  for (const doc of activeDocs) {
    if (doc.requisitoId !== undefined && doc.requisitoId !== null) {
      const key = Number(doc.requisitoId);
      if (!docsByRequisitoId.has(key)) {
        docsByRequisitoId.set(key, []);
      }
      docsByRequisitoId.get(key).push(doc);
    }
  }

  const normalizedReqs = (Array.isArray(requisitos) ? requisitos : []).map((req) => {
    const reqId = Number(req.requisitoId ?? req.id);
    const matchingDocs = docsByRequisitoId.get(reqId) || [];
    const cumplido = matchingDocs.length > 0;
    const obligatorio = Boolean(req.obligatorio);
    return {
      requisitoId: reqId,
      codigo: req.requisitoCodigo || req.codigo || `REQ-${reqId}`,
      nombre: req.requisitoNombre || req.nombre || "Requisito documental",
      descripcion: req.requisitoDescripcion || req.descripcion || "",
      obligatorio,
      ordenVisual: req.ordenVisual ?? 1,
      cumplido,
      documentos: matchingDocs
    };
  });

  const total = normalizedReqs.length;
  const obligatorios = normalizedReqs.filter((r) => r.obligatorio).length;
  const cumplidos = normalizedReqs.filter((r) => r.obligatorio && r.cumplido).length;
  const pendientesObligatorios = Math.max(0, obligatorios - cumplidos);
  const allMandatoryMet = pendientesObligatorios === 0;

  return {
    items: normalizedReqs,
    total,
    obligatorios,
    cumplidos,
    pendientesObligatorios,
    allMandatoryMet,
    activeDocs
  };
}

/**
 * Renderiza el checklist interactivo de requisitos exigidos por el trámite con carga directa por requisito.
 */
export function renderRequirementsChecklist({
  container,
  solicitudId = null,
  requisitos = [],
  documentos = [],
  canUpload = false,
  onEnsureSolicitudDraft = null,
  onDocumentsChanged = null
} = {}) {
  if (!container) return null;
  clearChildren(container);

  const compliance = evaluateRequirementsCompliance(requisitos, documentos);

  if (compliance.items.length === 0) {
    container.appendChild(
      createElement("div", {
        className: "empty-state",
        textContent: "Este tipo de trámite no registra requisitos documentales específicos configurados."
      })
    );
    return compliance;
  }

  for (const item of compliance.items) {
    const statusClass = item.cumplido
      ? "requisito-upload-item--cumplido"
      : item.obligatorio
        ? "requisito-upload-item--pendiente"
        : "";

    const card = createElement("div", {
      className: `requisito-upload-item ${statusClass}`.trim()
    });

    const header = createElement("div", { className: "requisito-upload-item__header" });

    const titleWrap = createElement("div");
    const titleEl = createElement("div", {
      className: "requisito-upload-item__title",
      textContent: `${item.codigo} — ${item.nombre}`
    });
    titleWrap.appendChild(titleEl);

    if (item.descripcion) {
      titleWrap.appendChild(
        createElement("div", {
          className: "requisito-upload-item__meta",
          textContent: item.descripcion
        })
      );
    }

    const badgesWrap = createElement("div", { className: "table-actions" });
    badgesWrap.appendChild(
      createElement("span", {
        className: `badge ${item.obligatorio ? "badge--warning" : "badge--neutral"}`,
        textContent: item.obligatorio ? "OBLIGATORIO" : "OPCIONAL"
      })
    );
    badgesWrap.appendChild(
      createElement("span", {
        className: `badge ${
          item.cumplido ? "badge--success" : item.obligatorio ? "badge--danger" : "badge--info"
        }`,
        textContent: item.cumplido
          ? "CUMPLIDO"
          : item.obligatorio
            ? "PENDIENTE"
            : "NO ADJUNTADO"
      })
    );

    header.append(titleWrap, badgesWrap);
    card.appendChild(header);

    // Documentos ya asociados a este requisito
    if (item.documentos.length > 0) {
      const attachedList = createElement("div", { className: "documentos-list" });
      for (const doc of item.documentos) {
        const docRow = createElement("div", { className: "documento-card" });

        const docInfo = createElement("div", { className: "documento-card__info" });
        docInfo.appendChild(
          createElement("span", {
            className: "documento-card__icon",
            textContent: resolveMimeBadgeText(doc.tipoMime, doc.nombreOriginal)
          })
        );

        const docText = createElement("div");
        docText.appendChild(
          createElement("div", {
            className: "documento-card__name",
            textContent: doc.nombreOriginal || `Documento #${doc.id}`
          })
        );
        docText.appendChild(
          createElement("div", {
            className: "documento-card__details",
            textContent: `${formatFileSize(doc.tamanioBytes)} • Subido: ${formatDateTime(
              doc.fechaSubida
            )}${doc.observaciones ? ` • Nota: ${doc.observaciones}` : ""}`
          })
        );
        docInfo.appendChild(docText);

        const docActions = createElement("div", { className: "documento-card__actions" });
        docActions.appendChild(createEstadoDocumentoBadge(doc.estado));

        const downloadBtn = createElement("button", {
          type: "button",
          className: "btn btn--secondary btn--sm",
          textContent: "Descargar"
        });
        downloadBtn.addEventListener("click", async () => {
          try {
            downloadBtn.disabled = true;
            await descargarDocumento(doc.id, doc.nombreOriginal);
          } catch (err) {
            showToast(err.message || "No se pudo descargar el documento.", "error");
          } finally {
            downloadBtn.disabled = false;
          }
        });
        docActions.appendChild(downloadBtn);

        if (canUpload) {
          const removeBtn = createElement("button", {
            type: "button",
            className: "btn btn--danger btn--sm",
            textContent: "Retirar"
          });
          removeBtn.addEventListener("click", async () => {
            const confirmed = await confirmAction({
              title: "Retirar documento adjunto",
              message: `¿Desea retirar el archivo "${doc.nombreOriginal}" asociado al requisito ${item.codigo}? Podrá adjuntar un archivo de reemplazo inmediatamente.`,
              confirmText: "Sí, retirar archivo",
              variant: "danger"
            });
            if (!confirmed) return;
            try {
              removeBtn.disabled = true;
              await retirarDocumento(doc.id);
              showToast("Documento retirado correctamente.", "success");
              if (typeof onDocumentsChanged === "function") {
                await onDocumentsChanged();
              }
            } catch (err) {
              showToast(err.message || "Error al retirar el documento.", "error");
            } finally {
              removeBtn.disabled = false;
            }
          });
          docActions.appendChild(removeBtn);
        }

        docRow.append(docInfo, docActions);
        attachedList.appendChild(docRow);
      }
      card.appendChild(attachedList);
    }

    // Controles de subida si la solicitud permite adjuntar archivos (BORRADOR u OBSERVADA)
    if (canUpload) {
      const uploadBar = createElement("div", { className: "table-actions" });

      const fileInput = createElement("input", {
        type: "file",
        className: "sr-only",
        accept: ".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
      });

      const selectBtn = createElement("button", {
        type: "button",
        className: `btn ${item.cumplido ? "btn--secondary" : "btn--primary"} btn--sm`,
        textContent: item.cumplido ? "Adjuntar otro / Reemplazar" : "Subir archivo (PDF / JPG / PNG)"
      });

      const statusSpan = createElement("span", {
        className: "requisito-upload-item__meta",
        textContent: "Máx. 10 MB (.pdf, .jpg, .jpeg, .png)"
      });

      selectBtn.addEventListener("click", () => fileInput.click());

      fileInput.addEventListener("change", async () => {
        const file = fileInput.files && fileInput.files[0] ? fileInput.files[0] : null;
        fileInput.value = "";
        if (!file) return;

        const validation = validateMunicipalFile(file);
        if (!validation.valid) {
          showToast(validation.error, "error");
          return;
        }

        try {
          selectBtn.disabled = true;
          selectBtn.textContent = "Subiendo archivo...";

          let targetSolicitudId = solicitudId;
          if (!targetSolicitudId && typeof onEnsureSolicitudDraft === "function") {
            targetSolicitudId = await onEnsureSolicitudDraft();
          }

          if (!targetSolicitudId) {
            throw new Error("Debe guardar primero el borrador de la solicitud antes de adjuntar documentos.");
          }

          await subirDocumentoSolicitud(targetSolicitudId, {
            archivo: file,
            requisitoId: item.requisitoId,
            observaciones: `Adjunto para requisito ${item.codigo}`
          });

          showToast(`Archivo "${file.name}" adjuntado al requisito ${item.codigo}.`, "success");
          if (typeof onDocumentsChanged === "function") {
            await onDocumentsChanged(targetSolicitudId);
          }
        } catch (err) {
          showToast(err.message || "Error al subir el documento adjunto.", "error");
        } finally {
          selectBtn.disabled = false;
          selectBtn.textContent = item.cumplido
            ? "Adjuntar otro / Reemplazar"
            : "Subir archivo (PDF / JPG / PNG)";
        }
      });

      uploadBar.append(fileInput, selectBtn, statusSpan);
      card.appendChild(uploadBar);
    }

    container.appendChild(card);
  }

  return compliance;
}

/**
 * Renderiza la lista completa de documentos adjuntos del expediente (incluyendo adjuntos generales o con requisito).
 */
export function renderAttachedDocumentsList({
  container,
  documentos = [],
  canModify = false,
  onDocumentsChanged = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  const activeList = (Array.isArray(documentos) ? documentos : []).filter(
    (doc) => doc && doc.activo !== false
  );

  if (activeList.length === 0) {
    container.appendChild(
      createElement("div", {
        className: "empty-state",
        textContent: "No se han adjuntado documentos activos a este expediente."
      })
    );
    return;
  }

  for (const doc of activeList) {
    const card = createElement("div", { className: "documento-card" });

    const info = createElement("div", { className: "documento-card__info" });
    info.appendChild(
      createElement("span", {
        className: "documento-card__icon",
        textContent: resolveMimeBadgeText(doc.tipoMime, doc.nombreOriginal)
      })
    );

    const textWrap = createElement("div");
    textWrap.appendChild(
      createElement("div", {
        className: "documento-card__name",
        textContent: doc.nombreOriginal || `Documento #${doc.id}`
      })
    );

    const reqLabel = doc.requisitoCodigo
      ? `${doc.requisitoCodigo} - ${doc.requisitoNombre || ""}`
      : "Adjunto general del expediente";

    textWrap.appendChild(
      createElement("div", {
        className: "documento-card__details",
        textContent: `${reqLabel} • ${formatFileSize(doc.tamanioBytes)} • ${formatDateTime(
          doc.fechaSubida
        )}${doc.observaciones ? ` • ${doc.observaciones}` : ""}`
      })
    );
    info.appendChild(textWrap);

    const actions = createElement("div", { className: "documento-card__actions" });
    actions.appendChild(createEstadoDocumentoBadge(doc.estado));

    const downloadBtn = createElement("button", {
      type: "button",
      className: "btn btn--secondary btn--sm",
      textContent: "Descargar"
    });
    downloadBtn.addEventListener("click", async () => {
      try {
        downloadBtn.disabled = true;
        await descargarDocumento(doc.id, doc.nombreOriginal);
      } catch (err) {
        showToast(err.message || "No fue posible descargar el archivo.", "error");
      } finally {
        downloadBtn.disabled = false;
      }
    });
    actions.appendChild(downloadBtn);

    if (canModify) {
      const withdrawBtn = createElement("button", {
        type: "button",
        className: "btn btn--danger btn--sm",
        textContent: "Retirar"
      });
      withdrawBtn.addEventListener("click", async () => {
        const confirmed = await confirmAction({
          title: "Retirar documento del expediente",
          message: `¿Confirma que desea retirar el archivo "${doc.nombreOriginal}" del expediente?`,
          confirmText: "Retirar documento",
          variant: "danger"
        });
        if (!confirmed) return;

        try {
          withdrawBtn.disabled = true;
          await retirarDocumento(doc.id);
          showToast("Documento retirado del expediente.", "success");
          if (typeof onDocumentsChanged === "function") {
            await onDocumentsChanged();
          }
        } catch (err) {
          showToast(err.message || "Error al retirar el documento.", "error");
        } finally {
          withdrawBtn.disabled = false;
        }
      });
      actions.appendChild(withdrawBtn);
    }

    card.append(info, actions);
    container.appendChild(card);
  }
}

/**
 * Abre un diálogo modal para subir un documento adjunto eligiendo el requisito aplicable y observaciones.
 */
export function openUploadDocumentModal({
  solicitudId,
  requisitos = [],
  preselectedRequisitoId = null,
  onUploaded = null
} = {}) {
  const form = createElement("form", {
    className: "modal-form",
    attrs: { novalidate: "novalidate" }
  });

  const reqGroup = createElement("div", { className: "form-group" });
  reqGroup.appendChild(
    createElement("label", {
      className: "form-label",
      attrs: { for: "modal-doc-requisito" },
      textContent: "Requisito documental asociado"
    })
  );

  const reqSelect = createElement("select", {
    id: "modal-doc-requisito",
    className: "form-select"
  });
  reqSelect.appendChild(
    createElement("option", {
      value: "",
      textContent: "— Documento sustentatorio general (sin requisito específico) —"
    })
  );

  for (const req of requisitos) {
    const reqId = req.requisitoId ?? req.id;
    const codigo = req.requisitoCodigo || req.codigo || "";
    const nombre = req.requisitoNombre || req.nombre || "";
    const opt = createElement("option", {
      value: String(reqId),
      textContent: `${codigo} — ${nombre}${req.obligatorio ? " (Obligatorio)" : " (Opcional)"}`
    });
    if (preselectedRequisitoId && Number(preselectedRequisitoId) === Number(reqId)) {
      opt.selected = true;
    }
    reqSelect.appendChild(opt);
  }
  reqGroup.appendChild(reqSelect);

  const dropzoneGroup = createElement("div", { className: "form-group" });
  dropzoneGroup.appendChild(
    createElement("label", {
      className: "form-label",
      textContent: "Archivo digital (.pdf, .jpg, .jpeg, .png — Máx. 10 MB) *"
    })
  );
  const dropzoneCtrl = createUploadDropzone({
    idPrefix: "modal-doc-file"
  });
  dropzoneGroup.appendChild(dropzoneCtrl.element);

  const obsGroup = createElement("div", { className: "form-group" });
  obsGroup.appendChild(
    createElement("label", {
      className: "form-label",
      attrs: { for: "modal-doc-obs" },
      textContent: "Observaciones o glosa del documento (opcional)"
    })
  );
  const obsInput = createElement("textarea", {
    id: "modal-doc-obs",
    className: "form-input",
    attrs: {
      rows: "2",
      maxlength: "500",
      placeholder: "Ej.: Copia fedateada vigente o plano firmado por arquitecto colegiado..."
    }
  });
  obsGroup.appendChild(obsInput);

  form.append(reqGroup, dropzoneGroup, obsGroup);

  const cancelBtn = createElement("button", {
    type: "button",
    className: "btn btn--secondary",
    textContent: "Cancelar"
  });
  const submitBtn = createElement("button", {
    type: "button",
    className: "btn btn--primary",
    textContent: "Subir Documento"
  });

  const modal = openModal({
    title: "Adjuntar Documento al Expediente",
    content: form,
    footerActions: [cancelBtn, submitBtn]
  });

  cancelBtn.addEventListener("click", () => modal.close());

  submitBtn.addEventListener("click", async () => {
    const file = dropzoneCtrl.getSelectedFile();
    const validation = validateMunicipalFile(file);
    if (!validation.valid) {
      dropzoneCtrl.setError(validation.error);
      return;
    }

    try {
      submitBtn.disabled = true;
      submitBtn.textContent = "Subiendo...";

      await subirDocumentoSolicitud(solicitudId, {
        archivo: file,
        requisitoId: reqSelect.value || null,
        observaciones: obsInput.value.trim()
      });

      showToast("Documento subido exitosamente al expediente.", "success");
      modal.close();
      if (typeof onUploaded === "function") {
        await onUploaded();
      }
    } catch (err) {
      dropzoneCtrl.setError(err.message || "Error al subir el archivo.");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Subir Documento";
    }
  });
}
