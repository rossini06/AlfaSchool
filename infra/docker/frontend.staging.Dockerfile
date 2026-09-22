# Imagem de produção do frontend: builda a SPA e serve numa porta só, com o
# nginx fazendo proxy de /api, /actuator e dos streams SSE para o backend.
# O contexto de build e' a RAIZ do repo (COPY frontend/ e infra/nginx/).

# ── Stage 1: build da SPA ──────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci || npm install
COPY frontend/ .
RUN npm run build

# ── Stage 2: nginx servindo o dist ─────────────────────────────────────
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY infra/nginx/default.staging.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
