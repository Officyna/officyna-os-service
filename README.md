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
* Java 21+
* Docker e Docker Compose (para banco local MongoDB)
* Maven 3.9+ (ou utilizar o wrapper `./mvnw.cmd` / `./mvnw`)

### 1. Subir MongoDB Localmente
```bash
docker-compose up -d mongodb
```

### 2. Compilar e Executar Testes Unitários
```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

### 3. Validação Completa do Ciclo de Build e Cobertura (JaCoCo)
```bash
# Windows
.\mvnw.cmd clean verify

# Linux / macOS
./mvnw clean verify
```

### 4. Executar a Aplicação Localmente
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`.
Acesse a documentação OpenAPI/Swagger em: `http://localhost:8080/swagger-ui.html`.

---

## 📊 Qualidade e Cobertura (JaCoCo)

* **Testes Automatizados:** 324 testes unitários e de integração cobrindo 100% dos fluxos de domínio e casos de uso de OS, Cliente, Veículo e Infraestrutura de Segurança.
* **Cobertura Mínima:** Regra configurada no Maven JaCoCo Plugin garantindo cobertura ≥ 80% nos pacotes de domínio e use cases.
