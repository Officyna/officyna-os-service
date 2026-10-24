# 🛠️ Officyna - Microsserviço de Ordem de Serviço (OS)

> **Tech Challenge - Fase 4: Desacoplamento e Arquitetura de Microsserviços**

O **`officyna-os-service`** é o microsserviço responsável pelo núcleo de atendimento da oficina e pelo ciclo de vida da **Ordem de Serviço (OS)**, gerenciando clientes, veículos e o fluxo operacional de ordens de serviço.

---

## 📋 Sumário
* [Objetivo do Microsserviço](#-objetivo-do-microsserviço)
* [Domínios e Funcionalidades](#-domínios-e-funcionalidades)
* [Arquitetura e Organização de Pacotes](#-arquitetura-e-organização-de-pacotes)
* [Persistência NoSQL](#-persistência-nosql)
* [Estrutura do Repositório](#-estrutura-do-repositório)
* [Instruções de Execução e Testes](#-instruções-de-execução-e-testes)
* [Qualidade e Cobertura (JaCoCo)](#-qualidade-e-cobertura-jacoco)

---

## 🎯 Objetivo do Microsserviço

Como parte da evolução para microsserviços (Fase 4):
* **Isolamento de Domínio:** O `officyna-os-service` gerencia exclusivamente as entidades e operações de **Ordem de Serviço**, **Cliente** e **Veículo**.
* **Desacoplamento Completo:** Módulos de Estoque (`supplies`), Mão de Obra/Serviços (`labors`), Usuários administrativos/mecânicos e faturamento foram desacoplados e externalizados para seus respectivos microsserviços e comunicação assíncrona.
* **Persistência NoSQL:** Utilização de MongoDB / Amazon DocumentDB com persistência isolada para as coleções do serviço.

---

## ✨ Domínios e Funcionalidades

### 1. Ordem de Serviço (`domain.serviceorder`)
* Abertura e acompanhamento do ciclo de vida da OS:
  - `RECEIVED` (Recebida)
  - `IN_DIAGNOSIS` (Em diagnóstico)
  - `WAITING_APPROVAL` (Aguardando aprovação)
  - `APPROVED` (Aprovada)
  - `IN_EXECUTION` (Em execução)
  - `FINISHED` (Finalizada)
  - `DELIVERED` (Entregue)
  - `CANCELED` (Cancelada)
* Registro de itens de serviços e peças requeridas na OS (como referências desacopladas).
* Histórico e rastreabilidade de transições de status.

### 2. Clientes (`domain.customer`)
* Cadastro, consulta, atualização e exclusão lógica de clientes (Pessoa Física e Pessoa Jurídica).
* Validação de CPF/CNPJ (Módulo 11).

### 3. Veículos (`domain.vehicle`)
* Cadastro e gestão de veículos vinculados a clientes.
* Validação de formato de placa (padrão nacional e padrão Mercosul).

---

## 🏗️ Arquitetura e Organização de Pacotes

O projeto adota os princípios de **Clean Architecture** organizados pelas camadas globais:

```
br.com.officyna/
├── api/                            # Adaptadores Primários (Entrada HTTP REST)
│   ├── customer/                   # Controllers, DTOs (Request/Response) e Mappers de Cliente
│   ├── vehicle/                    # Controllers, DTOs (Request/Response) e Mappers de Veículo
│   └── serviceorder/               # Controllers, DTOs (Request/Response) e Mappers de Ordem de Serviço
├── domain/                         # Núcleo de Domínio e Regras de Negócio (Puro)
│   ├── customer/                   # Entidades, Value Objects, Gateways e UseCases/Services de Cliente
│   ├── vehicle/                    # Entidades, Value Objects, Gateways e UseCases/Services de Veículo
│   └── serviceorder/               # Entidades, Value Objects, Gateways e UseCases/Services de OS
├── infrastructure/                 # Adaptadores Secundários e Frameworks
│   ├── auth/                       # Modelos e utilitários de autenticação
│   ├── config/                     # Configurações de Beans, Spring Security e Documentação
│   ├── converter/                  # Conversores customizados
│   ├── exception/                  # Tratamento global de erros e exceptions de infraestrutura
│   ├── logging/                    # Interceptors e logs estruturados em JSON
│   ├── persistence/                # Implementações MongoDB / DocumentDB (Documents, Repositories, Gateways)
│   └── security/                   # Filtros JWT, UserDetailsService e validação de tokens
└── seed/                           # Carga inicial de dados de desenvolvimento (Seeder)
```

---

## 🗄️ Persistência NoSQL

O serviço utiliza **MongoDB / Amazon DocumentDB** para armazenar as seguintes coleções exclusivas:
* `service_orders`: Documentos das Ordens de Serviço e histórico de status.
* `service_order_sequences`: Sequenciador atômico para numeração amigável das OS.
* `customers`: Documentos de clientes.
* `vehicles`: Documentos de veículos.

As antigas coleções e documentos relacionais ou de outros domínios (`users`, `labors`, `supplies`, `labor_monitorings`) foram completamente removidos da base deste microsserviço.

---

## 🚀 Instruções de Execução e Testes

### Pré-requisitos
* **Java JDK 21+** (configurado no `JAVA_HOME`)
* **Docker** e **Docker Compose** (com o **Docker Desktop** em execução no modo Linux containers)
* **Maven 3.9+** (opcional, pois o projeto já inclui o wrapper `./mvnw.cmd` / `./mvnw`)

---

### Opção A: Infraestrutura no Docker + Aplicação na IDE/Terminal (Recomendado para Desenvolvimento)

Nesse modo, você sobe o banco de dados (**MongoDB**), a mensageria (**LocalStack SQS**) e os painéis visuais (**Mongo Express** e **SQS Admin**) via Docker, e executa a aplicação Spring Boot diretamente na máquina local.

#### 1. Subir a infraestrutura base
Na raiz do projeto, execute:
```bash
docker compose up -d mongodb mongo-express localstack sqs-admin
```
* O container **`mongodb`** ficará disponível em `localhost:27017` (banco `officyna`).
* O container **`localstack`** ficará disponível em `localhost:4566` e executará automaticamente o script `localstack-init/init-sqs.sh`, criando as filas SQS (`eventForDiagnostics`, `eventDiagnosis`, `eventDiagnosisCompleted`, `eventGeneratePayment`, `eventBugetGenerated`, `topicAprovacao`).
* O **`mongo-express`** ficará acessível em `http://localhost:8081` (usuário: `admin` / senha: `admin`).
* O **`sqs-admin`** (painel visual das filas SQS) ficará acessível em `http://localhost:8088`.

Verifique se os containers estão saudáveis (`healthy`):
```bash
docker compose ps
```

#### 2. Executar a aplicação Spring Boot
```bash
# Windows (PowerShell / CMD)
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

> **Dica — Popular dados iniciais (Seed de Clientes e Veículos):**
> Para iniciar a aplicação populando clientes e veículos de teste automaticamente (`DatabaseSeeder`), habilite a propriedade `database.seed.enabled=true`:
> ```bash
> # Windows
> .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--database.seed.enabled=true"
>
> # Linux / macOS
> ./mvnw spring-boot:run -Dspring-boot.run.arguments="--database.seed.enabled=true"
> ```

---

### Opção B: Ambiente Completo via Docker Compose (Infraestrutura + App + Kong Gateway)

Para rodar toda a stack containerizada (incluindo o build da aplicação e o API Gateway **Kong**):
```bash
docker compose up -d --build
```

Para acompanhar os logs da aplicação:
```bash
docker compose logs -f app
```

Para encerrar todos os containers:
```bash
docker compose down
```

---

### 🌐 Acessos e Endpoints Locais

| Serviço | URL | Credenciais / Observações |
| :--- | :--- | :--- |
| **API (`officyna-os-service`)** | `http://localhost:8080` | Porta padrão configurada em `SERVER_PORT` |
| **Swagger UI (OpenAPI)** | `http://localhost:8080/swagger-ui.html` | Documentação interativa dos endpoints |
| **OpenAPI JSON** | `http://localhost:8080/api-docs` | Especificação OpenAPI bruta |
| **Healthcheck (Actuator)** | `http://localhost:8080/actuator/health` | Deve retornar `{"status":"UP"}` |
| **Mongo Express** | `http://localhost:8081` | Interface web do MongoDB (`admin` / `admin`) |
| **SQS Admin** | `http://localhost:8088` | Interface web para visualizar e gerenciar as filas SQS do LocalStack |
| **Kong Gateway** *(Opção B)* | `http://localhost:8000/officyna` | Proxy reverso configurado no `Kong.yml` |

#### Inspecionando filas SQS via Terminal (Alternativa ao SQS Admin)
```bash
# Listar todas as filas criadas no LocalStack
docker exec -it officyna-os-service-localstack awslocal sqs list-queues

# Ler mensagens de uma fila específica
docker exec -it officyna-os-service-localstack awslocal sqs receive-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/eventForDiagnostics
```

---

### 🔐 Autenticação Local (Como obter o Token JWT)

A maioria dos endpoints exige autenticação via **Bearer Token (JWT)**. Os usuários locais para desenvolvimento são:

| Perfil (`UserRole`) | E-mail | Senha |
| :--- | :--- | :--- |
| `ADMIN` | `admin@email.com` | `admin123` |
| `ATTENDANT` | `atendente@email.com` | `atendente123` |

1. Realize uma requisição `POST` para `/api/auth/login`:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email": "admin@email.com", "password": "admin123"}'
   ```
2. Copie o valor do campo `token` retornado no JSON.
3. No **Swagger UI** (`http://localhost:8080/swagger-ui.html`), clique em **Authorize** no topo da página, cole o token e confirme, ou envie no header das requisições:
   ```http
   Authorization: Bearer <SEU_TOKEN_JWT>
   ```

---

### ⚙️ Principais Variáveis de Ambiente

| Variável | Valor Padrão Local | Descrição |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação |
| `MONGODB_URI` | `mongodb://localhost:27017/officyna` | URI de conexão com o MongoDB |
| `AWS_ENDPOINT` | `http://localhost:4566` | Endpoint do LocalStack / AWS SQS |
| `AWS_REGION` | `us-east-1` | Região AWS padrão |
| `AWS_SQS_LISTENER_ENABLED` | `true` | Ativa/desativa o polling das filas SQS (`false` para rodar sem LocalStack) |
| `PRODUCTION_SERVICE_URL` | `http://localhost:8082` | URL do microsserviço de produção para consulta de mão de obra |

---

### 🧪 Executar Testes e Validação de Cobertura

#### 1. Compilar e Executar Testes Unitários e de Integração
```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

#### 2. Validação Completa do Ciclo de Build e Cobertura (JaCoCo)
```bash
# Windows
.\mvnw.cmd clean verify

# Linux / macOS
./mvnw clean verify
```
*(O relatório HTML de cobertura do JaCoCo é gerado em `target/site/jacoco/index.html`)*.

#### 3. Análise com SonarQube Local (Opcional)
```bash
docker compose --profile sonar up -d sonarqube
.\mvnw.cmd clean verify sonar:sonar -Dsonar.token=<SEU_TOKEN_SONAR>
```

---

## 📊 Qualidade e Cobertura (JaCoCo)

* **Testes Automatizados:** 324 testes unitários e de integração cobrindo 100% dos fluxos de domínio e casos de uso de OS, Cliente, Veículo e Infraestrutura de Segurança.
* **Cobertura Mínima:** Regra configurada no Maven JaCoCo Plugin garantindo cobertura ≥ 80% nos pacotes de domínio e use cases.
