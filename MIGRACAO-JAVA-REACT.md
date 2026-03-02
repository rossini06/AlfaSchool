# Migração para Java + React (Spring Boot)

## Nova arquitetura

- Backend: `backend/` (Spring Boot 3.5, Java 21, JPA, Security, JWT, H2)
- Frontend: `frontend/` (React + Vite)
- Endpoints principais:
  - `POST /api/auth/login`
  - `GET /api/health`
  - `GET /actuator/health`

## Credencial inicial

- Email: `superadmin@alfaschool.com`
- Senha: `SuperAdmin@2024!@#$`

A conta é criada automaticamente no startup do backend (`SuperAdminInitializer`).

## Como executar

### 1) Backend

```bash
cd backend
mvnw.cmd spring-boot:run
```

Backend em: `http://127.0.0.1:8080`

### 2) Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend em: `http://127.0.0.1:5173`

## Build e testes

### Backend

```bash
cd backend
mvnw.cmd test
```

### Frontend

```bash
cd frontend
npm run build
```

## Observações de migração

- A base Laravel legado permanece no repositório para referência e transição gradual.
- A autenticação principal já está funcional no backend Java com lockout por tentativas inválidas.
- O frontend React já consome o login da API Spring via proxy do Vite.
- Próximas migrações recomendadas:
  1. Usuários/roles/permissões completos no backend Java.
  2. Módulos acadêmicos (alunos, turmas, notas, frequência).
  3. Integração de equipamentos de controle de acesso (SDK/API do fabricante).
