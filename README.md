# Planeta Atlântida 2027 — Backend Java (Spring Boot 3)

Backend RESTful desenvolvido em **Java 17** e **Spring Boot 3.3.4** para atendimento corporativo do **Portal de Patrocinadores do Planeta Atlântida** (Grupo RBS).

---

## 🏛️ Arquitetura (Cenário A)

O backend opera desacoplado do frontend React (`src/`), servindo como API e orquestrador de regras de negócio:

```
[ Frontend React (src/) ]
           │
           │ HTTP REST / JWT Bearer
           ▼
[ Backend Java (Spring Boot) ] ── (Apache POI) ──> Validação de Planilhas de Mídia
           │
           │ JDBC / PostgreSQL SSL
           ▼
[ Banco de Dados Supabase (PostgreSQL) ]
```

### Principais Funcionalidades:
- **Gestão de Patrocinadores:** CRUD de marcas parceiras, atribuição de cotas (`Master`, `Subcota G`, `Subcota`) e contatos.
- **Processamento de Mídia & Excel:** Leitura e importação de planilhas de veiculações (`.xlsx`) via **Apache POI** no servidor.
- **Gestão de Arquivos:** Metadados e geração de URLs assinadas e seguras para download.
- **Trilha de Auditoria:** Histórico em tempo real de todas as movimentações e ações de usuários.
- **Segurança (Spring Security):** Validação de tokens JWT do Supabase Auth e perfis de acesso (`ROLE_ADMIN_RBS` e `ROLE_SPONSOR`).
- **Documentação OpenAPI/Swagger:** Catálogo interativo de endpoints acessível via navegador.

---

## 🚀 Como Executar

### Pré-requisitos
- **Java 17** ou superior (JDK)
- **Maven 3.8+** (ou utilizar o wrapper)
- *(Opcional)* Docker e Docker Compose

---

### Executando com Maven Local

1. Configure as variáveis de ambiente necessárias (ou edite `src/main/resources/application.yml`):
   ```bash
   export SPRING_DATASOURCE_PASSWORD="sua-senha-do-banco-supabase"
   export SUPABASE_JWT_SECRET="seu-jwt-secret-do-supabase"
   ```

2. Compile e inicie a aplicação:
   ```bash
   mvn clean spring-boot:run
   ```

3. A aplicação estará ativa em:
   - **API Base:** `http://localhost:8080/api`
   - **Swagger UI:** `http://localhost:8080/swagger-ui.html`
   - **Healthcheck:** `http://localhost:8080/api/health`

---

### Executando com Docker

```bash
docker compose up --build -d
```

---

## 📡 Principais Endpoints

| Método | Endpoint | Descrição | Permissão |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Status da API | Público |
| `GET` | `/api/sponsors` | Listar marcas parceiras | Autenticado |
| `GET` | `/api/sponsors/{id}` | Detalhes da marca | Autenticado |
| `POST` | `/api/sponsors` | Cadastrar novo patrocinador | Admin RBS |
| `PUT` | `/api/sponsors/{id}` | Atualizar dados cadastrais | Admin RBS |
| `GET` | `/api/sponsors/{id}/media` | Listar veiculações de mídia | Autenticado |
| `POST` | `/api/media/import-excel` | Importar planilha Excel (.xlsx) | Admin RBS |
| `GET` | `/api/sponsors/{id}/files` | Listar arquivos por aba/categoria | Autenticado |
| `GET` | `/api/files/{id}/download-url` | Obter URL assinada de download | Autenticado |
| `GET` | `/api/audit/recent` | Últimas movimentações do portal | Admin RBS |
| `GET` | `/api/sponsors/{id}/audit` | Histórico de uma marca | Autenticado |
