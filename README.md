# AlfaSchool (Java + React)

Base oficial do projeto em **Java/Spring Boot (backend)** e **React/Vite (frontend)**.

## Estrutura ativa

- `backend/` → API Spring Boot
- `frontend/` → Web app React

## Legado PHP

Todo o código Laravel foi isolado em:

- `legacy-laravel/`

Esse diretório está mantido apenas como referência histórica de migração e **não faz parte da execução principal**.

## Pré-requisitos

- Java 21 (JDK)
- Node.js 20+

## Como executar

### Backend

```bash
cd backend
mvnw.cmd spring-boot:run
```

API em `http://127.0.0.1:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

App em `http://127.0.0.1:5173`

## Testes e qualidade

### Backend

```bash
cd backend
mvnw.cmd test
```

### Frontend

```bash
cd frontend
npm run lint
npm run build
```

## Credencial inicial (ambiente local)

- Email: `superadmin@alfaschool.com`
- Senha: `SuperAdmin@2024!@#$`

> Para detalhes da transição técnica, consulte `MIGRACAO-JAVA-REACT.md`.
