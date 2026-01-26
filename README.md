# Go-Tree API - Sistema SaaS de Gestão de Segurança do Trabalho (SST)

## 📌 Sobre o Projeto

O **Go-Tree API** é o backend de uma plataforma SaaS desenvolvida para gestão completa de Segurança e Saúde do Trabalho 
(SST). O sistema permite que técnicos e engenheiros de segurança gerenciem empresas, realizem visitas técnicas, 
checklists de riscos e análises ergonômicas (AEP), gerando automaticamente relatórios em PDF prontos para entrega.

O backend é projetado com uma arquitetura em camadas focada na integridade dos dados e na conformidade legal dos 
documentos gerados.

## ✨ Funcionalidades

O sistema possui um controle de acesso baseado em papéis (RBAC) definidos como **Técnico/Avaliador (USER)** e 
**Administrador do Sistema (ADMIN)**.

### 👷 Para Técnicos e Avaliadores (USER)

* **Autenticação e Perfil:**
    * Login seguro via **JWT (JSON Web Tokens)**.
    * Gestão de perfil próprio e alteração segura de senha.

* **Gestão de Visitas Técnicas:**
    * Agendamento e registro de visitas em empresas clientes.
    * Validação de conflitos de agenda (verifica se o técnico já tem visita no mesmo turno).
    * Registro de "Findings" (Não conformidades) com upload de fotos (Base64) e classificação de prioridade.
    * Coleta de assinatura do cliente (coordenadas geográficas + imagem da assinatura).

* **Análise Ergonômica (AEP):**
    * Criação de laudos ergonômicos preliminares.
    * Seleção de riscos ergonômicos baseada em um catálogo mestre padronizado.
    * Vínculo com Fisioterapeuta responsável.

* **Checklist de Riscos:**
    * Avaliação de riscos por função e setor.
    * Assinatura do técnico responsável direto no documento (opcional).

* **Motor de Documentos:**
    * Geração automática de PDFs para todos os módulos (Visitas, AEP, Riscos).
    * Download de documentos e prontos para entrega fiscal.
    * Envio direto para o e-mail do cliente.

### ⚙️ Para Administradores (ADMIN)

* **Gestão Global de Usuários:**
    * CRUD completo de usuários (Técnicos, Engenheiros, Backoffice).
    * **Inserção em Lote (Batch):** Endpoint otimizado para cadastrar múltiplos usuários de uma vez, com relatório de 
    * sucesso/falha.
    * Reset administrativo de senhas e controle de acesso.

* **Gestão de Clientes e Empresas:**
    * Cadastro de empresas clientes (Pessoas Jurídicas).
    * Gestão da estrutura organizacional (Unidades e Setores).
    * Vínculo N:N entre Clientes e Empresas (um cliente pode ter múltiplas filiais ou empresas vinculadas).

* **KPIs e Métricas:**
    * Consultas otimizadas para contagem de relatórios por técnico e por empresa (base para dashboards administrativos).

## 🛠️ Tecnologias Utilizadas

* **Linguagem e Frameworks:**
    * **Java 21** (LTS).
    * **Spring Boot 3.5.3**.
    * **Spring Security:** Controle de autenticação e autorização via anotações `@PreAuthorize`.
    * **Spring Data JPA:** Persistência de dados com Hibernate.

* **Documentos e PDF:**
    * **Thymeleaf:** Motor de templates para renderização do HTML dos relatórios.
    * **Flying Saucer (OpenPDF):** Conversão de HTML sanitizado para PDF de alta fidelidade.

* **Segurança:**
    * **JWT:** Autenticação stateless.
    * **BCrypt:** Hashing irreversível para senhas de acesso.

* **Bibliotecas e Ferramentas:**
    * **Lombok:** Redução de boilerplate.
    * **Caelum Stella:** Validação rigorosa de CPF e CNPJ.
    * **PostgreSQL:** Banco de dados relacional.
    * **Maven:** Gerenciamento de dependências.

## 🏛️ Arquitetura e Decisões de Design

* **Sanitização de Dados (XML Sanitizer):** Foi implementada uma camada de interceptação (`XmlSanitizer`) nos Services. 
* Como o motor de PDF (Flying Saucer) é sensível a caracteres XML inválidos, todos os inputs de texto livre (títulos, 
* descrições, observações) são limpos antes da persistência e da geração do documento, prevenindo erros de "Rascunho" 
* e falhas silenciosas.

* **Otimização de Consultas (Entity Graph):** Para evitar o problema de *N+1 queries* na geração de relatórios 
* complexos, utilizamos extensivamente a anotação `@EntityGraph` nos repositórios (`AepReportRepository`, 
* `RiskReportRepository`), carregando a árvore de dependências (Empresa -> Cliente -> Setor) em uma única consulta SQL.

* **Design Orientado a Documentos:** A arquitetura prioriza a imutabilidade dos documentos que requerem assinatura. Uma vez que um 
* relatório é finalizado e assinado, o sistema armazena o caminho físico do PDF (`pdfPath`), garantindo que alterações 
* futuras nos cadastros não alterem o histórico do documento emitido.

* **Tratamento de Exceções Granular:** O sistema diferencia erros de negócio (`IllegalStateException` para regras 
* como "agenda ocupada") de erros de integridade (`DataIntegrityViolation` para exclusão de registros vinculados), 
* retornando status HTTP adequados (409 Conflict, 404 Not Found) para o frontend.

## 🚀 Começando

### Pré-requisitos

* JDK 21
* Maven
* PostgreSQL (Local ou via Docker)

### Instalação e Execução

### Instalação e Execução

1. **Clone o repositório:**
2. **Configure seu aplication.properties**
3. Execute a aplicação


## 🔮 Futuro e Próximos Passos

- Assinatura Digital (ICP-Brasil): Upload seguro de certificado digital A1 (.pfx). O sistema armazenará a senha de forma 
criptografada e o técnico poderá optar por assinar para manter a integridade do documento, antes do envio para o cliente.

- Relatórios em Dashboards Analíticos: Exportação de documentos com visualizações gráficas dados consolidados para 
- análise estratégica de KPIs.

- Exportação da Agenda: Funcionalidade para exportar a programação de visitas técnicas em formatos Excel (.xlsx) e PDF 
para facilitar o planejamento offline.

- Notificações Push: Alerta para técnicos sobre visitas agendadas no dia seguinte.

## 👩🏻‍💻 Autora:

Desenvolvido por Brena Soares