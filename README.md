# khoaluantotnghiep

## AI Chatbot (Gemini Free) da duoc tich hop

He thong da co chatbot AI voi endpoint backend:

- `POST /api/chatbot/ask`
- `GET /api/chatbot/history` (can JWT)
- `DELETE /api/chatbot/history` (can JWT)

Lich su duoc luu theo `user/patient` neu request co JWT hop le.

### 1) Cau hinh Gemini API key

Dat bien moi truong khi chay backend:

- `GEMINI_API_KEY=<your_api_key>`

Tu chon:

- `AI_GEMINI_ENABLED=true`
- `GEMINI_MODEL=gemini-2.5-flash-lite`
- `GEMINI_ENDPOINT=https://generativelanguage.googleapis.com/v1beta/models`
- `GEMINI_TIMEOUT_SECONDS=20`

### 2) Cau hinh frontend goi backend

Tao file `Frontend/.env.local` tu mau `Frontend/.env.local.example` va dat:

- `NEXT_PUBLIC_API_BASE_URL=http://localhost:8081`

### 3) Chay he thong local

Truoc khi chay backend lan dau cho tinh nang lich su chatbot, chay SQL tao bang:

- Script: `Backend/scripts/add-chatbot-history-postgres.sql`

Tu thu muc goc project:

- `run-local.ps1`

Hoac chay rieng le:

1. Backend: `Backend\mvnw.cmd spring-boot:run`
2. Frontend: `cd Frontend && npm run dev`

Mo chatbot tai `http://localhost:3000`.

## Deploy Backend len Render + Frontend len Vercel

### A) Backend (Spring Boot) tren Render

Repo da co san file `render.yaml` o thu muc goc de Render doc cau hinh.

1. Day source len GitHub.
2. Vao Render -> New + -> Blueprint.
3. Chon repo nay, Render se tu doc `render.yaml`.
4. Sau khi tao service, vao `Environment` va dat cac bien bat buoc:
   - `DB_URL` (PostgreSQL JDBC URL)
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `REDIS_HOST`
   - `REDIS_PORT`
   - `REDIS_PASSWORD` (neu co)
   - `JWT_ACCESS_TOKEN_SECRET`
   - `JWT_REFRESH_TOKEN_SECRET`
   - `MAIL_USERNAME`
   - `MAIL_PASSWORD`
   - `GEMINI_API_KEY`
   - `APP_CORS_ALLOWED_ORIGIN_PATTERNS` (vi du: `https://ten-du-an.vercel.app`)

Luu y:

- Backend da duoc sua de doc cong tu bien `PORT` (Render cap tu dong).
- URL backend public sau deploy se co dang: `https://<render-service>.onrender.com`.

### B) Frontend (Next.js) tren Vercel

1. Vao Vercel -> Add New Project.
2. Import cung repo GitHub.
3. Dat `Root Directory` la `Frontend`.
4. Framework Preset: `Next.js`.
5. Dat Environment Variable:
   - `NEXT_PUBLIC_API_BASE_URL=https://<render-service>.onrender.com`
6. Deploy.

### C) Ket noi CORS sau khi co domain Vercel

Sau khi Vercel cap domain chinh thuc, cap nhat bien tren Render:

- `APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://<ten-du-an>.vercel.app`

Neu can cho phep nhieu domain (preview + production), dung dau phay:

- `APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://<ten-du-an>.vercel.app,https://<preview-domain>.vercel.app`
