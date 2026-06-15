# CLAUDE.md — Backend Biblioteca

## Visão geral
Sistema de reservas de salas e computadores para uma biblioteca.
Spring Boot + MySQL + JWT + Spring Security + Swagger.

**Stack:** Java 17, Spring Boot 4.0.5, JPA/Hibernate, Lombok, jjwt 0.12.6, Spring Mail, H2 (dev), MySQL (prod)

## Estrutura de pacotes
```
com.example.demo
├── annotations/     ← @Admin, @Public (NÃO MEXER)
├── config/          ← Toda a infraestrutura (NÃO MEXER — ver seção abaixo)
├── controller/      ← Endpoints REST
├── dto/             ← Objetos de entrada/saída das APIs
├── dtoEstatisticas/ ← DTOs específicos para o módulo de estatísticas
├── entity/          ← Entidades JPA
├── enums/           ← Enumerações do domínio
├── mapper/          ← ResponseMapper (NÃO MEXER)
├── repository/      ← Interfaces JPA
├── scheduler/       ← Tarefas agendadas (ex: expiração de reservas)
└── service/         ← Regras de negócio
```

## ⛔ NÃO MEXER — Feito pelo professor / Infraestrutura base

### Pacote `config/` — infraestrutura completa
- `SecurityConfig.java` — configuração do Spring Security e cadeia de filtros
- `JwtFilter.java` — filtro que valida o JWT em cada requisição
- `JwtUtil.java` — geração e validação de tokens JWT
- `CorsConfig.java` — política de CORS
- `AuthorizationInterceptor.java` — interpreta @Admin e @Public
- `WebMvcConfig.java` — registra o interceptor
- `GlobalExceptionHandler.java` — tratamento global de erros (respostas de erro padronizadas)
- `DataInitializer.java` — carga inicial de dados no banco
- `SwaggerConfig.java` / `SwaggerStartupListener.java` — configuração do Swagger/OpenAPI

### Demais arquivos base
- `annotations/Admin.java` e `annotations/Public.java` — anotações de controle de acesso
- `entity/BaseEntity.java` — superclasse com id, ativo, createdAt, updatedAt, deletedAt
- `repository/BaseRepository.java` — repositório base
- `service/BaseService.java` — service base
- `mapper/ResponseMapper.java` — mapeamento de entidades para DTOs de resposta
- `pom.xml` — não adicionar/remover dependências sem discutir
- `src/main/resources/application.properties` — não alterar; variáveis de ambiente via `.env` / `application-local.properties`

## ✅ Pode trabalhar livremente

- `controller/` — adicionar endpoints, ajustar rotas existentes
- `service/` — regras de negócio (exceto BaseService)
- `dto/` e `dtoEstatisticas/` — criar e editar DTOs
- `entity/` — criar e editar entidades (sempre estendendo BaseEntity)
- `repository/` — criar e editar repositórios (sempre estendendo BaseRepository)
- `enums/` — criar e editar enums
- `scheduler/ReservaScheduler.java` — tarefas agendadas

## Convenções importantes

### Entidades
- Toda entidade DEVE estender `BaseEntity` quando tem as mesmas caracteristicas de uso (fornece id, ativo, timestamps, deletedAt)
- Soft delete via campo `ativo = false` — nunca deletar fisicamente, salvo exceção explícita
- Lombok: usar `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@EqualsAndHashCode(callSuper = false)`

### Controllers
- Anotar com `@Admin` (só admin) ou `@Public` (sem autenticação); sem anotação = autenticado qualquer nível
- Estender `BaseController` quando disponível
- Retornar DTOs de resposta e idas, nunca entidades diretamente

### Segurança
- O token JWT é passado no header `Authorization: Bearer <token>`
- Roles: `NivelAcesso.ADMIN` e `NivelAcesso.PADRAO`
- TipoUsuario: outro enum separado (ex: ALUNO, PROFESSOR)

### Banco de dados
- JPA com `ddl-auto=update` (não recriar tabelas)
- Configuração via variáveis de ambiente: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- Arquivo local de config: `application-local.properties` (não commitar)

## Domínio — entidades principais
| Entidade | Descrição |
|---|---|
| Usuario | Usuário do sistema (ADMIN ou PADRAO) |
| Sala | Sala reservável |
| Computador | Computador reservável |
| PedidoReserva | Pedido de reserva (sala ou computador) — passa por aprovação ou nao |
| ReservaSala | Reserva efetivada de sala |
| ReservaComputador | Reserva efetivada de computador |
| AprovacaoReserva | Registro de aprovação/rejeição |

## Enums relevantes
- `NivelAcesso`: ADMIN, PADRAO
- `TipoUsuario`: (ver arquivo)
- `StatusReserva`: (ver arquivo)
- `StatusAprovacao`: (ver arquivo)
- `TipoPedido`: (ver arquivo)

## Como rodar localmente
```bash
./mvnw spring-boot:run
```
Swagger disponível em: `http://localhost:8080/swagger-ui.html`
