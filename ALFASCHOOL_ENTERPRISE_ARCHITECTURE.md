# AlfaSchool - Diretrizes Arquiteturais Enterprise

Este documento define as regras estruturais, estratégicas e técnicas do AlfaSchool.
Toda decisão técnica deve respeitar estas diretrizes antes de qualquer implementação.

O sistema é um SaaS educacional multi-tenant, multi-unidade, escalável e preparado para integração com hardware (catracas, relógios de ponto, biometria).

------------------------------------------------------------
1. VISÃO DO PRODUTO
------------------------------------------------------------

- Sistema SaaS educacional
- Multi-clientes (cada escola é um tenant)
- Multi-unidades por cliente
- Controle acadêmico + financeiro + acesso físico
- Alta escalabilidade
- Arquitetura preparada para crescimento nacional

------------------------------------------------------------
2. STACK TECNOLÓGICO OFICIAL
------------------------------------------------------------

Backend:
- Java 21
- Spring Boot 3.x
- Spring Security
- JWT
- JPA (Hibernate)
- PostgreSQL
- Flyway (versionamento de banco)
- Actuator
- Logs estruturados (JSON)

Frontend:
- React (Vite)
- Estrutura modular por domínio
- Gerenciamento de estado padronizado (Zustand ou Context API)
- Axios com interceptors
- Controle centralizado de autenticação

Infraestrutura:
- Docker
- Docker Compose
- Preparado para Kubernetes
- CI/CD
- Variáveis de ambiente obrigatórias
- Sem credenciais hardcoded

------------------------------------------------------------
3. MODELO ARQUITETURAL
------------------------------------------------------------

Arquitetura: Monolito Modular Evolutivo

Estrutura backend:

- domain/
- application/
- infrastructure/
- security/
- config/
- integration/
- shared/

Separação obrigatória:
- Entity ≠ DTO
- Controller ≠ Service
- Service ≠ Repository
- Nunca expor Entity diretamente na API

Padrão de camadas:

Controller → Application Service → Domain → Repository

------------------------------------------------------------
4. MULTI-TENANCY (OBRIGATÓRIO)
------------------------------------------------------------

Modelo adotado: Tenant por schema ou Tenant por coluna (definir oficialmente).

Regras:

- Toda entidade deve possuir tenant_id
- Nenhuma consulta pode ignorar o tenant
- Filtro automático por tenant
- Usuário só enxerga dados do próprio tenant
- Estrutura preparada para isolamento futuro

Sem isso, nenhuma feature pode ser considerada válida.

------------------------------------------------------------
5. SEGURANÇA
------------------------------------------------------------

- JWT obrigatório
- Refresh token obrigatório
- Controle de roles e permissões granular
- Lockout por tentativas inválidas
- Auditoria de ações críticas
- Senhas com BCrypt
- Política de troca de senha
- Logs de autenticação
- Nunca salvar senha inicial fixa
- Senha gerada por variável de ambiente

------------------------------------------------------------
6. ESCALABILIDADE
------------------------------------------------------------

Regras:

- Código stateless
- Sessão nunca armazenada em memória
- Preparado para múltiplas instâncias
- Conexão com banco via pool
- Uso mínimo de estado no backend
- Cache planejado (Redis futuro)
- Pronto para fila de eventos (RabbitMQ/Kafka futuro)

------------------------------------------------------------
7. INTEGRAÇÃO COM HARDWARE
------------------------------------------------------------

Criar módulo isolado:

integration/
    accesscontrol/
    biometrics/
    turnstile/
    timeclock/

Regras:

- Nunca misturar regra acadêmica com integração física
- Comunicação assíncrona quando possível
- Tratamento de falhas de rede
- Retry strategy
- Logs de integração

------------------------------------------------------------
8. PADRÕES DE DESENVOLVIMENTO
------------------------------------------------------------

Obrigatório:

- DTOs para entrada e saída
- Validações com Bean Validation
- Tratamento global de exceções
- Response padrão da API:

{
  "timestamp": "",
  "status": 200,
  "message": "",
  "data": {}
}

- Versionamento de API (/api/v1/)
- Testes unitários para serviços críticos
- Testes de integração para autenticação

------------------------------------------------------------
9. BANCO DE DADOS
------------------------------------------------------------

Banco oficial: PostgreSQL

Obrigatório:

- Índices para:
    - tenant_id
    - foreign keys
    - campos de busca frequente
- Auditoria básica:
    - created_at
    - updated_at
    - created_by
    - updated_by
- Soft delete quando aplicável
- Versionamento com Flyway

------------------------------------------------------------
10. FRONTEND - PADRÕES
------------------------------------------------------------

- Estrutura por módulos:
    modules/
        auth/
        academic/
        financial/
        access/
        reports/

- Rota protegida via guard
- Interceptor para JWT
- Logout automático ao expirar token
- UI preparada para múltiplas unidades
- Controle de permissões no frontend

------------------------------------------------------------
11. OBSERVABILIDADE
------------------------------------------------------------

- Spring Actuator habilitado
- Endpoint /health funcional
- Logs estruturados
- Preparado para integração com:
    - Prometheus
    - Grafana
    - ELK

------------------------------------------------------------
12. BOAS PRÁTICAS OBRIGATÓRIAS
------------------------------------------------------------

- Nunca implementar funcionalidade quebrando arquitetura
- Nunca ignorar multi-tenant
- Nunca usar credenciais fixas
- Nunca retornar dados sensíveis
- Nunca misturar responsabilidades
- Toda feature deve ser pensada para escalar

------------------------------------------------------------
13. ROADMAP ESTRUTURAL

Ordem recomendada:

1. Autenticação + Multi-tenant sólido
2. Usuários / Roles / Permissões
3. Módulo Acadêmico
4. Módulo Financeiro
5. Módulo de Acesso Físico
6. Relatórios avançados
7. Cache
8. Fila de eventos
9. Escalonamento horizontal

------------------------------------------------------------

Qualquer IA que for gerar código deve:

1. Ler este documento.
2. Validar se a implementação respeita multi-tenant.
3. Garantir segurança.
4. Garantir escalabilidade.
5. Não quebrar padrões estruturais.
6. Gerar código pronto para produção.