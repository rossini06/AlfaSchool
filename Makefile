.PHONY: up down build build-no-cache logs logs-backend logs-frontend restart \
        backend-shell frontend-shell db-shell redis-shell ps clean setup help

up:
	docker-compose up -d

down:
	docker-compose down

build:
	docker-compose build

build-no-cache:
	docker-compose build --no-cache

logs:
	docker-compose logs -f

logs-backend:
	docker-compose logs -f backend

logs-frontend:
	docker-compose logs -f frontend

restart:
	docker-compose down && docker-compose up -d

backend-shell:
	docker-compose exec backend sh

frontend-shell:
	docker-compose exec frontend sh

db-shell:
	docker-compose exec mysql mysql -u root -p alfaschool

redis-shell:
	docker-compose exec redis redis-cli

ps:
	docker-compose ps

clean:
	docker-compose down -v --remove-orphans

setup:
	cp .env.example .env
	docker-compose up -d

help:
	@echo ""
	@echo "AlfaSchool — Comandos disponíveis:"
	@echo ""
	@echo "  make up              Inicia todos os serviços"
	@echo "  make down            Para todos os serviços"
	@echo "  make build           Constrói as imagens"
	@echo "  make build-no-cache  Reconstrói sem cache"
	@echo "  make logs            Exibe logs de todos os serviços"
	@echo "  make logs-backend    Logs apenas do backend"
	@echo "  make restart         Reinicia todos os serviços"
	@echo "  make backend-shell   Shell do container backend"
	@echo "  make db-shell        Acessa o MySQL"
	@echo "  make redis-shell     Acessa o Redis CLI"
	@echo "  make clean           Remove containers e volumes"
	@echo "  make setup           Configuração inicial (copia .env e sobe)"
	@echo ""
	@echo "Serviços:"
	@echo "  Frontend   → http://localhost"
	@echo "  Backend    → http://localhost:8081"
	@echo "  phpMyAdmin → http://localhost:8082"
	@echo ""
