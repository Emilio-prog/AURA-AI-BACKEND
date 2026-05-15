# AURA IA Backend

Backend Spring Boot para el panel de usuario de AURA IA. Gestiona la autenticacion, la persistencia en PostgreSQL, los datos de usuario, la facturacion y la integracion con Gemini.

## Requisitos

- JDK 21
- Docker, solo para tests de integracion con Testcontainers
- Cadena de conexion a Supabase PostgreSQL para ejecucion en desarrollo/produccion

Este repositorio incluye Maven Wrapper. El wrapper descarga su propia version de Maven la primera vez que se ejecuta. La maquina debe exponer JDK 21 mediante `JAVA_HOME` o `PATH`.

## Instalacion local limpia para evaluador

AURA IA se entrega en dos repositorios Git independientes. Para levantar la
aplicacion completa con los scripts versionados, clona backend y frontend como
carpetas hermanas dentro de una misma carpeta de trabajo:

```text
AURA-IA/
|-- AURA-AI-BACKEND/
`-- AURA-AI-FRONTEND/
```

Clonado recomendado:

```bash
mkdir AURA-IA
cd AURA-IA
git clone https://github.com/Emilio-prog/AURA-AI-BACKEND.git
git clone https://github.com/Emilio-prog/AURA-AI-FRONTEND.git
```

Requisitos minimos:

- JDK 21. Esta version esta fijada en `pom.xml` mediante `<java.version>21</java.version>`.
- Node.js 20 o superior para el frontend.

El arranque por defecto del script del frontend esta pensado para el
tutor/evaluador: no requiere `.env`, credenciales externas, PostgreSQL local,
H2 ni usuarios demo. Ese modo no arranca este backend en local; arranca Vite en
`http://localhost:5173` y usa un proxy de desarrollo hacia el backend real
desplegado en `https://api.aura-ia.es`.

Para arrancar este backend en local si hace falta PostgreSQL/Supabase real y
`AURA-AI-BACKEND/.env` con las variables `SPRING_DATASOURCE_*`, `JWT_SECRET` y
el resto de integraciones que se quieran probar.

Arranque completo desde la carpeta `AURA-IA`:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1
```

```bash
chmod +x AURA-AI-FRONTEND/scripts/start-dev.sh
./AURA-AI-FRONTEND/scripts/start-dev.sh
```

Verificacion rapida:

- El navegador abre `http://localhost:5173`.
- En modo tutor, las llamadas a `http://localhost:5173/api/v1/*` llegan al
  backend real `https://api.aura-ia.es/api/v1/*` mediante el proxy de Vite.
- En modo backend local, Swagger queda disponible en `http://localhost:8080/swagger-ui.html`.

## Configuracion

Copia `.env.example` a `.env` en el gestor de entorno local o exporta las variables antes de ejecutar la aplicacion. No commitees nunca credenciales reales.

Variables importantes:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` como secreto base64 de al menos 64 bytes decodificados
- `FRONTEND_BASE_URL`, usado para los enlaces de verificacion de email
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`
- `EMAIL_ENABLED=true` y `EMAIL_AUTO_VERIFY_WHEN_DISABLED=false` para verificacion real por email
- `AI_SERVICE_ENABLED`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `AI_MAX_HISTORY_MESSAGES`
- `ADMIN_EMAILS`, emails separados por comas que se promueven a administrador

El proyecto Supabase usado actualmente para desarrollo es `AURA-AI` (`aexcwfxhbiifvcxdgcxm`). El desarrollo local usa el pooler de sesiones de Supabase porque el host directo de base de datos es solo IPv6 en este proyecto:

```text
jdbc:postgresql://aws-1-eu-west-2.pooler.supabase.com:5432/postgres?sslmode=require
username: postgres.aexcwfxhbiifvcxdgcxm
```

El esquema publico del backend se aplico en Supabase mediante MCP como migracion `init_aura_backend_schema`. Los perfiles de Flyway usan `baseline-on-migrate=true`, por lo que un esquema de Supabase ya migrado no se recrea en tiempo de ejecucion.

### Resend SMTP

El proveedor de email recomendado es Resend mediante Spring Mail SMTP. Resend requiere un dominio de envio verificado antes de que destinatarios externos puedan recibir emails de verificacion.

```text
SMTP_HOST=smtp.resend.com
SMTP_PORT=587
SMTP_USERNAME=resend
SMTP_PASSWORD=<resend-api-key>
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_FROM=AURA IA <no-reply@aura-ia.es>
```

Resend MCP esta autenticado mediante API key. El dominio de envio configurado es `aura-ia.es`; debe estar `verified` en Resend antes de usar `EMAIL_ENABLED=true` en local o en produccion.

Los emails de verificacion se envian como HTML con alternativa de texto plano. La plantilla HTML incluye el bloque de marca AURA IA, un boton principal de verificacion y una breve nota de seguridad.

## Ejecucion

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Swagger UI esta disponible en:

```text
http://localhost:8080/swagger-ui.html
```

Comprobacion de salud:

```text
GET /actuator/health
```

### Stack local completo de desarrollo

Desde la raiz del workspace `AURA-IA`, el stack completo de desarrollo se puede arrancar con un solo comando.

Windows:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1
```

macOS/Linux:

```bash
chmod +x AURA-AI-FRONTEND/scripts/start-dev.sh
./AURA-AI-FRONTEND/scripts/start-dev.sh
```

Los scripts arrancan el backend en `http://localhost:8080`, el frontend en `http://localhost:5173`, esperan a que ambos servicios respondan y abren el navegador en `http://localhost:5173`.

Si `AURA-AI-BACKEND/.env` define `SERVER_PORT`, los scripts usan ese puerto real del backend y muestran un aviso. Mantener alineados el entorno local del frontend y el callback OAuth:

```text
VITE_API_BASE_URL=http://localhost:<SERVER_PORT>/api/v1
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:<SERVER_PORT>/api/v1/auth/oauth/google/callback
```

Para parar el stack local:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1 -Stop
```

```bash
./AURA-AI-FRONTEND/scripts/start-dev.sh stop
```

Para ejecutar contra una base de datos real PostgreSQL/Supabase, usa el modo avanzado `-RealEnv`
en Windows o `real-env` en macOS/Linux. En ese caso se requiere
`AURA-AI-BACKEND/.env` con credenciales:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1 -RealEnv
```

```bash
./AURA-AI-FRONTEND/scripts/start-dev.sh real-env
```

### Verificacion tras los cambios

- URL del navegador: `http://localhost:5173`.
- Health del backend: `GET http://localhost:8080/actuator/health` devuelve `{"status":"UP"}`. Si `SERVER_PORT` esta definido en `.env`, usa ese puerto.
- Servidor de desarrollo frontend: Vite muestra `Local: http://localhost:5173/`.
- Las llamadas API del navegador apuntan a `http://localhost:<SERVER_PORT>/api/v1`.
- Si las llamadas API siguen apuntando a `127.0.0.1`, revisar `AURA-AI-FRONTEND/.env.local` y configurar `VITE_API_BASE_URL=http://localhost:<SERVER_PORT>/api/v1`.
- En Windows, revisar `.dev-logs/backend-dev.err.log`, `.dev-logs/backend-dev.out.log`, `.dev-logs/frontend-dev.err.log` y `.dev-logs/frontend-dev.out.log`.
- En macOS/Linux, revisar `.dev-logs/backend-dev.log` y `.dev-logs/frontend-dev.log`.
- Si Google OAuth esta habilitado localmente, `GOOGLE_OAUTH_REDIRECT_URI` debe coincidir con la URL del backend, por ejemplo `http://localhost:8080/api/v1/auth/oauth/google/callback`.

## Control de versiones

Los cambios del backend siguen GitFlow: el trabajo empieza en `feature`, se integra en `develop`,
se estabiliza en `release` y llega a `main` solo mediante un release etiquetado.
Las correcciones criticas de produccion usan `hotfix` y se devuelven a `develop`.

### Webhooks de facturacion Stripe

Checkout puede abrirse desde desarrollo local, pero Stripe no puede llamar directamente a `localhost` despues del pago. Para sincronizar cambios de plan localmente en Windows, arranca el stack completo desde la raiz del workspace `AURA-IA` con:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1 -StripeWebhook
```

Ese comando lee `STRIPE_SECRET_KEY`, obtiene el secreto local de firma de webhook de Stripe CLI, lo inyecta en el proceso del backend como `STRIPE_WEBHOOK_SECRET` y reenvia eventos de Stripe a:

```text
http://localhost:8080/api/v1/webhooks/stripe
```

Si Stripe CLI no esta instalado, el script usa Docker Desktop como alternativa y ejecuta la imagen oficial `stripe/stripe-cli`. Para parar todo:

```powershell
.\AURA-AI-FRONTEND\scripts\start-dev.ps1 -Stop
```

Produccion debe usar una URL publica real del backend, actualmente esperada como:

```text
https://api.aura-ia.es/api/v1/webhooks/stripe
```

Usa el secreto de firma de webhook del Dashboard para produccion/Dokploy, y el secreto local del listener solo para ejecuciones locales.

Los detalles del despliegue en produccion estan documentados en [`docs/stripe-production.md`](docs/stripe-production.md).

## Flujo de autenticacion

1. `POST /api/v1/auth/register` crea el usuario y devuelve `202 Accepted`.
2. En produccion, se envia un email de verificacion a `${FRONTEND_BASE_URL}/#/verify-email?token=...` para el frontend actual con HashRouter.
3. En desarrollo, si `EMAIL_ENABLED=false` y `EMAIL_AUTO_VERIFY_WHEN_DISABLED=true`, la cuenta se verifica inmediatamente.
4. `POST /api/v1/auth/verify-email?token=...` verifica la cuenta cuando la verificacion por email esta activa.
5. `POST /api/v1/auth/login` devuelve `accessToken` y `refreshToken`.
6. `POST /api/v1/auth/refresh` rota el refresh token.
7. `POST /api/v1/auth/logout` revoca el refresh token actual.

Los usuarios no pueden iniciar sesion hasta que el email este verificado. Las contrasenas requieren 12 caracteres con mayuscula, minuscula, numero y simbolo.

## Contrato del servicio de IA

El backend integra Gemini directamente desde Spring Boot mediante `RestClient`; no hay servicio Python/FastAPI en la arquitectura actual. El comportamiento en tiempo de ejecucion se controla mediante:

```text
AI_SERVICE_ENABLED=true
GEMINI_API_KEY=<google-ai-studio-api-key>
GEMINI_MODEL=gemini-flash-latest
AI_MAX_HISTORY_MESSAGES=12
AI_CHAT_RATE_LIMIT_CAPACITY=20
AI_CHAT_RATE_LIMIT_REFILL_MINUTES=5
```

Cuando `AI_SERVICE_ENABLED=false`, falta `GEMINI_API_KEY`, Gemini no esta disponible o Gemini devuelve una respuesta invalida, el backend devuelve un fallback seguro y determinista en lugar de romper el chat.

Endpoints autenticados del chat:

```http
GET /api/v1/chatbot/sessions
POST /api/v1/chatbot/sessions
GET /api/v1/chatbot/sessions/{id}
POST /api/v1/chatbot/sessions/{id}/messages
DELETE /api/v1/chatbot/sessions/{id}
```

Peticion de mensaje:

```json
{ "message": "Estoy nervioso hoy" }
```

Respuesta de sesion:

```json
{
  "id": "e5c679e7-366c-404a-a4ff-2c92992fd464",
  "title": "Estoy nervioso hoy",
  "messages": [
    {
      "role": "user",
      "content": "Estoy nervioso hoy",
      "timestamp": "2026-05-12T09:06:24.790608500Z"
    },
    {
      "role": "assistant",
      "content": "Estoy contigo. Probemos una respiracion breve...",
      "timestamp": "2026-05-12T09:06:28.286856700Z",
      "riskLevel": "low",
      "emotions": ["anxiety"],
      "sentiment": "supportive"
    }
  ],
  "startedAt": "2026-05-12T09:06:24.548620Z",
  "updatedAt": "2026-05-12T09:06:28.286856700Z"
}
```

Solo se envian a Gemini el mensaje actual y los mensajes mas recientes de la misma sesion de chat. No se envian a Gemini entradas de diario, registros de mood, datos de onboarding, contactos, datos de facturacion ni detalles del perfil de usuario.

Las reglas de seguridad se aplican antes y despues de Gemini. Los mensajes que impliquen ideacion suicida, autolesion, dano a otras personas o peligro inmediato devuelven una respuesta de seguridad de alto riesgo que menciona en Espana el `112` y el `024`. Las solicitudes normales sobre ansiedad, panico, sueno o calma reciben apoyo de grounding y respiracion sin numeros de emergencia.

## Pruebas

```bash
./mvnw test
```

Los tests de integracion usan Testcontainers PostgreSQL y son opt-in para que las builds normales no fallen en maquinas donde Docker Desktop no este ejecutandose:

```bash
./mvnw test -Daura.integration-tests=true
```

## Notas de contrato con el frontend

El backend expone el contrato seguro consumido por el frontend actual:

- El registro devuelve verificacion pendiente, no tokens.
- La contrasena demo debe ser fuerte si se inicializa desde backend.
- Mood usa `beforeLevel` y `afterLevel` de 1 a 10.
- Los nombres de campos JSON de la API son estables en ingles; los mensajes se localizan mediante `Accept-Language`.
