# MANUAL DE USUARIO
## Sistema Web de Gestión de Licencias Municipales
### Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray — Ayacucho, Perú

---

## 1. INTRODUCCIÓN

El **Sistema Web de Gestión de Licencias Municipales** es la plataforma digital oficial de la Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray, diseñada para automatizar, transparentar y agilizar el ciclo integral de tramitación, evaluación técnica, inspección, recaudación y emisión de licencias de funcionamiento de acuerdo con la Ley Marco de Licencias de Funcionamiento (Ley N.º 28976 y sus modificatorias).

El sistema opera bajo una arquitectura web responsiva accesible desde computadoras de escritorio, laptops y dispositivos móviles institucionales autorizados.

---

## 2. ROLES DE USUARIO Y NIVELES DE ACCESO

El sistema implementa control de acceso basado en roles (RBAC):

| Rol | Denominación Institucional | Atribuciones Principales |
|---|---|---|
| `ADMINISTRADOR` | Administrador del Sistema | Configuración global, administración de usuarios, anulación de pagos, revocación formal de licencias y consulta de auditoría inmutable. |
| `FUNCIONARIO` | Funcionario / Evaluador Municipal | Revisión de solicitudes, evaluación documental, emisión de observaciones, aprobación de trámites, verificación de pagos y emisión de licencias. |
| `SUPERVISOR` | Supervisor Municipal | Asignación de expedientes, reprogramación de inspecciones, supervisión de licencias activas y consulta de bitácoras de auditoría. |
| `CIUDADANO` | Administrado / Titular | Registro de solicitudes, carga de requisitos documentales, seguimiento de expedientes, subsanación de observaciones y declaración de pagos. |

---

## 3. INICIO DE SESIÓN Y SEGURIDAD

### 3.1 Acceso al Sistema
1. Abra el navegador web e ingrese a la dirección institucional: `http://localhost:8080` (o dominio municipal asignado).
2. Si no cuenta con una sesión activa, el sistema lo dirigirá a la pantalla de **Login Institucional** (`/login.html`).
3. Ingrese su **Nombre de Usuario** y su **Contraseña**.
4. Puede hacer clic en el icono del ojo para visualizar o enmascarar su contraseña.
5. Presione el botón **Iniciar Sesión**.

### 3.2 Seguridad de la Sesión
* **Token de Protección CSRF:** Cada petición mutante está protegida contra falsificación de peticiones en sitios cruzados.
* **Tiempo de Inactividad:** Por motivos de seguridad, la sesión caduca automáticamente tras 30 minutos de inactividad, solicitando un nuevo inicio de sesión.
* **Cierre de Sesión Seguro:** Para finalizar la jornada, haga clic en el botón superior derecho con sus iniciales y seleccione **Cerrar Sesión**.

---

## 4. DASHBOARD ADMINISTRATIVO

Al ingresar, el usuario accede al **Panel Principal** (`/dashboard.html`):

* **Tarjetas Métricas (KPIs):**
  * Solicitudes en trámite, aprobadas y observadas.
  * Licencias vigentes emitidas en el distrito.
  * Inspecciones técnicas ITSE pendientes o ejecutadas.
  * Recaudación verificada acumulada en Soles (PEN).
* **Gráficos Estadísticos:**
  * Evolución mensual de solicitudes (Enero a Diciembre).
  * Distribución porcentual de trámites por estado mediante gráfico tipo dona interactivo.
* **Actividad Reciente:** Registro cronológico de los últimos eventos administrativos con indicación de fecha, hora y usuario responsable.
* **Accesos Directos:** Botones de acción rápida para registrar una nueva solicitud, verificar pagos o programar inspecciones.

---

## 5. GESTIÓN DE PADRÓN MUNICIPAL

### 5.1 Ciudadanos y Administrados (`/pages/ciudadanos.html`)
* **Consulta:** Búsqueda por DNI, RUC, Carnet de Extranjería o nombres completos.
* **Registro:** Formulario con validación de tipo de documento (DNI de 8 dígitos numéricos, RUC de 11 dígitos, etc.), teléfono, correo electrónico y dirección fiscal.
* **Edición y Estado:** Actualización de datos de contacto y conmutación de estado activo/inactivo por personal autorizado.

### 5.2 Representantes Legales (`/pages/representantes.html`)
* Registro de apoderados acreditados mediante partida registral o poder notarial vinculado al establecimiento comercial.

### 5.3 Establecimientos Comerciales (`/pages/establecimientos.html`)
* **Datos del Local:** Nombre comercial, razón social, RUC titular, giro o actividad económica principal.
* **Condiciones Técnicas:** Área total ocupada en metros cuadrados ($m^2$), capacidad de aforo y nivel de riesgo de edificación conforme al Manual ITSE.

---

## 6. GESTIÓN DE SOLICITUDES Y EXPEDIENTES

### 6.1 Registro de Solicitud (`/pages/nueva-solicitud.html`)
1. Ingrese al módulo **Solicitudes** y pulse **Nueva Solicitud**.
2. Seleccione el Ciudadano solicitante y el Establecimiento comercial.
3. Elija el **Tipo de Trámite** (ej. *Licencia de Funcionamiento para Edificaciones de Riesgo Bajo o Medio*).
4. El sistema generará el número de expediente oficial con formato `EXP-YYYY-XXXXX`.
5. Adjunte los documentos obligatorios requeridos por el TUPA municipal.
6. Pulse **Presentar Solicitud** para remitir el expediente al área evaluadora.

### 6.2 Evaluación Técnica (`/pages/detalle-solicitud.html`)
* **Iniciar Revisión:** Cambia el estado del expediente a `EN_REVISION`.
* **Emitir Observaciones:** En caso de requisitos incompletos o inconsistentes, se redactan observaciones específicas otorgando un plazo legal al administrado (`OBSERVADA`).
* **Subsanación:** El administrado carga los documentos corregidos y pulsa **Subsanar**, transitando a `SUBSANADA`.
* **Aprobación o Rechazo:** Una vez cumplidos todos los requisitos técnicos, el funcionario emite la aprobación (`APROBADA`) o el rechazo motivado (`RECHAZADA`).

---

## 7. INSPECCIONES MUNICIPALES (ITSE)

### 7.1 Programación (`/pages/inspecciones.html`)
* Desde el módulo o directamente desde la solicitud aprobada, se selecciona **Programar Inspección**.
* Se asigna el inspector técnico, la fecha y hora programada, y las indicaciones previas.

### 7.2 Diligencia in situ y Dictamen (`/pages/detalle-inspeccion.html`)
1. El inspector asignado pulsa **Iniciar Inspección** al apersonarse al local (`EN_CURSO`).
2. Tras la evaluación de seguridad (señalética, extintores, rutas de evacuación, cableado eléctrico), pulsa **Registrar Dictamen Técnico**:
   * `FAVORABLE`: Cumple las condiciones mínimas de seguridad.
   * `CON_OBSERVACIONES`: Se otorga plazo perentorio para levantar observaciones menores.
   * `NO_FAVORABLE`: Representa riesgo inminente; deniega la continuidad del trámite.
3. Se adjuntan las observaciones técnicas y recomendaciones vinculantes.

---

## 8. GESTIÓN DE PAGOS Y CAJA MUNICIPAL

### 8.1 Registro de Declaración de Pago (`/pages/pagos.html`)
1. Administrados o cajeros ingresan a **Pagos ➔ Registrar Pago**.
2. Indican el número de expediente, concepto de cobro (Tasa de Licencia TUPA, Inspección ITSE, etc.), monto en Soles (PEN), medio de pago (`BANCO_NACION`, `CAJA_MUNICIPAL`, `TRANSFERENCIA`, `POS`, `EFECTIVO`) y número de operación del voucher.
3. El pago queda registrado en estado `PENDIENTE`.

### 8.2 Verificación Administrativa (`/pages/detalle-pago.html`)
* El cajero o funcionario coteja el comprobante con los extractos bancarios.
* Al pulsar **Verificar Pago**, el estado transita a `VERIFICADO`, habilitando de inmediato la emisión de la licencia.
* **Constancia de Pago:** Se puede descargar en cualquier momento la **Constancia Oficial de Pago en formato PDF**.

---

## 9. EMISIÓN Y VERIFICACIÓN DE LICENCIAS

### 9.1 Emisión Formal (`/pages/emitir-licencia.html`)
* Restringido a expedientes con solicitud en estado `APROBADA` y requisitos cumplidos.
* Se selecciona el tipo de vigencia: `DEFINITIVA` (indeterminada por Ley N.º 28976) o `TEMPORAL` (indicando fecha límite de caducidad).
* Al confirmar, se genera el código oficial `LF-YYYY-XXXXX` y un token criptográfico único.

### 9.2 Certificado Digital PDF (`/pages/detalle-licencia.html`)
* Vista gráfica del certificado con borde institucional y sello oficial.
* Descarga del documento PDF generado por el motor de reportes del servidor.
* Código QR impreso que enlaza directamente a la verificación pública municipal.

### 9.3 Portal de Verificación Pública (`/pages/verificar-licencia.html`)
* Accesible para cualquier ciudadano o fiscalizador policial/municipal escaneando el código QR sin necesidad de contraseña.
* Muestra el estado oficial (`VIGENTE`, `SUSPENDIDA`, `REVOCADA`), nombre comercial, giro, zonificación y nivel de riesgo, protegiendo los datos personales del administrado.

---

## 10. REPORTES MUNICIPALES (`/pages/reportes.html`)

1. Seleccione la pestaña temática: Solicitudes, Licencias, Inspecciones, Pagos, Establecimientos o Ciudadanos.
2. Defina los filtros deseados (rango de fechas desde/hasta, estado, giro, etc.).
3. Pulse **Generar Reporte** para actualizar los indicadores KPI, el gráfico de distribución y la tabla de resultados.
4. Para exportar la información oficial:
   * Pulse **Exportar PDF** para obtener el informe formal timbrado.
   * Pulse **Exportar CSV** para descargar los registros tabulados y abrirlos en Microsoft Excel.

---

## 11. CENTRO DE NOTIFICACIONES (`/pages/notificaciones.html`)

* La campana en el encabezado superior muestra una insignia roja con el número de avisos no leídos.
* Al hacer clic, se despliega una vista previa de los últimos avisos con la opción de abrir el **Centro de Notificaciones**.
* Puede filtrar por avisos no leídos o por tipo de trámite.
* Cada notificación permite navegar directamente al expediente o pago asociado.
* La opción **Marcar todas como leídas** actualiza instantáneamente el contador en todo el sistema.

---

## 12. BITÁCORA DE AUDITORÍA (`/pages/auditoria.html`)

* Módulo de solo lectura exclusivo para `ADMINISTRADOR` y `SUPERVISOR`.
* Permite buscar por fecha, usuario, módulo o tipo de operación (`INICIO_SESION`, `CREACION`, `APROBACION`, `EMISION`, etc.).
* Al hacer clic en **Ver Detalle**, se abre la ficha inmutable con la dirección IP de origen, usuario ejecutor y resumen técnico del cambio.
