# Plano de Divisão e Gestão de Trabalho em Equipa — CineTrack

**Unidade Curricular:** Desenvolvimento de Software e Sistemas Móveis (DSSMV)  
**Curso:** Licenciatura em Engenharia de Telecomunicações e Informática (LETI) — ISEP  
**Equipa:**  
* **André Silva (1240567)** — Ramo de Trabalho Git: `andre-1240567`  
* **Afonso Ferreira (1241013)** — Ramo de Trabalho Git: `afonso-1241013`  

---

## 1. Princípios e Critérios de Gestão Pedagógica

Em conformidade com as normas de avaliação da unidade curricular e as diretrizes do docente responsável (**Prof. Paulo Baltarejo Sousa**):

1. **Equilíbrio Técnico Estrito:**  
   A componente prática é sujeita à avaliação individual através da fórmula:
   $$\text{APP} = (\text{Checkpoint} \times 0.20 + \text{IMPL} \times 0.80) \times \textbf{Factor}$$
   O **Factor** individual (avaliado de 0% a 100% na defesa oral) exige que ambos os membros dominem as dimensões fundamentais do desenvolvimento móvel nativo: ciclo de vida de *Activities*, layouts XML responsivos (`dp`/`sp`), concorrência e respeito pela *UI Thread*, serviços externos (Firebase / Retrofit), sensores de hardware e navegação por *Intents*.

2. **Segregação de Ramos Git (Feature Branch Workflow Simplificado):**  
   * É expressamente interdito efetuar *push* direto para o ramo `main`.
   * Cada membro desenvolve exclusivamente na sua respetiva branch (`andre-1240567` e `afonso-1241013`).
   * A integração na `main` é realizada após validação de compilação sem erros (`./gradlew test`) e atualização prévia da documentação.

3. **Metodologia *Documentation-First*:**  
   Nenhuma User Story é codificada sem que a sua respetiva especificação esteja formalizada na pasta `docs/Use Cases/USx/` (com critérios de aceitação e Diagrama de Sequência de Sistema em PlantUML/SVG).

---

## 2. Matriz Geral de Atribuição das 11 User Stories

| ID | User Story | Responsável | Módulos e Componentes Arquiteturais |
| :--- | :--- | :--- | :--- |
| **US1** | [Auth] Registar e Autenticar Utilizador | **André** | Firebase Auth, `LoginActivity`, `RegisterActivity`, layouts XML. |
| **US2** | [CRUD] Criar e Gerir Listas de Filmes | **André** | Cloud Firestore CRUD, `CustomListAdapter`, diálogos de criação/remoção. |
| **US3** | [CRUD] Adicionar Filmes e Avaliações | **André** | Firestore, `RatingDialogFragment`, cálculo de médias, validações $[1.0, 5.0]$. |
| **US4** | [API] Pesquisa TMDB e Streaming Providers | **Afonso** | Retrofit 2, Gson, TMDB API (`/search/movie`, `/watch/providers`), `MovieAdapter`. |
| **US5** | [Mapas] Cinemas Próximos (GPS + Mapas) | **Afonso** | Permissões em runtime, GPS `LocationManager`, Foursquare Places API, MapView. |
| **US6** | [Hardware] Sugestão ao Agitar (Acelerómetro) | **Afonso** | `SensorManager`, Acelerómetro, limiar $>2.7g$, gestão no `onResume`/`onPause`. |
| **US7** | [Auth] Gestão de Perfil e Logout | **André** | `ProfileActivity`, `FirebaseAuth.signOut()`, gestão da pilha de navegação. |
| **US8** | [Persistence] Preferências Locais | **Afonso** | `SharedPrefManager`, gravação assíncrona mandatória com `.apply()`. |
| **US9** | [CRUD] Filtragem e Ordenação Local | **André** | `Comparator<SavedMovie>` no Model puro, UI de filtros (Spinner/RadioGroup). |
| **US10** | [API] Visualização de Trailers Oficiais | **Afonso** | Endpoint `/videos` do TMDB, *Implicit Intent* (`ACTION_VIEW`) com fallback web. |
| **US11** | [Social] Partilha Externa de Fichas | **André** | *Implicit Intent* (`ACTION_SEND`), `Intent.createChooser()`, texto formatado. |

---

## 3. Detalhe Operacional por Membro da Equipa

### 3.1. André Silva (`1240567`) — Branch `andre-1240567`
**Foco:** Camada de Identidade, Persistência Cloud NoSQL (Firestore), Gestão de Listas, Avaliações e Partilha Social.

* **US1 — Registar e Autenticar Utilizador:**
  * Documentação: [`docs/Use Cases/US1/US1.md`](../Use%20Cases/US1/US1.md) e [`ssd_us1_auth.svg`](../Use%20Cases/US1/svg/ssd_us1_auth.svg).
  * Views: `activity_login.xml` e `activity_register.xml` com campos de texto com validação e dimensões estritamente em `dp`/`sp`.
  * Controllers: `LoginActivity.java` e `RegisterActivity.java` orquestrando `FirebaseAuth` assíncrono e limpeza da pilha de navegação (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`).
* **US2 — Criar e Gerir Listas de Filmes:**
  * Documentação: `docs/Use Cases/US2/US2.md` e SSD em PlantUML/SVG.
  * Backend/BaaS: `FirestoreManager.java` com métodos assíncronos (`createList`, `getUserLists`, `deleteList`).
  * Views/Adapters: `CustomListAdapter.java` para RecyclerView e diálogo modal para criação de nova lista.
* **US3 — Adicionar Filmes às Listas e Submeter Avaliações:**
  * Documentação: `docs/Use Cases/US3/US3.md` e SSD.
  * Controllers: Diálogo para seleção de lista de destino e submissão de nota ($[1.0, 5.0]$) e comentário.
  * Views: Componente visual de estrelas (`RatingBar`) e exibição de média na `MovieDetailActivity`.
* **US7 — Gestão de Perfil e Terminar Sessão:**
  * Documentação: `docs/Use Cases/US7/US7.md`.
  * Controllers/Views: `ProfileActivity.java` e `activity_profile.xml` com apresentação de metadados da conta e botão de logout seguro (`FirebaseAuth.signOut()`).
* **US9 — Filtragem e Ordenação da Coleção Local:**
  * Documentação: `docs/Use Cases/US9/US9.md`.
  * Model Puro: Implementação de comparadores (`Comparator<SavedMovie>`) em Java 21 puro para ordenação por título, nota e filtragem por `WatchStatus`.
  * Adapters: Invocação de `notifyDataSetChanged()` sem bloquear a UI Thread.
* **US11 — Partilha Externa de Fichas de Filmes:**
  * Documentação: `docs/Use Cases/US11/US11.md`.
  * Controllers: Construção de texto formatado e disparo de *Implicit Intent* com `Intent.ACTION_SEND` encapsulado em `Intent.createChooser()`.

---

### 3.2. Afonso Ferreira (`1241013`) — Branch `afonso-1241013`
**Foco:** Consumo de APIs REST Externas (Retrofit 2), Hardware e Sensores Nativos (GPS e Acelerómetro) e Armazenamento Local Leve.

* **US8 — Guardar Preferências Locais:**
  * Documentação: `docs/Use Cases/US8/US8.md`.
  * Utils/Service: Implementação de `SharedPrefManager.java` encapsulando as chaves de configuração, com gravação assíncrona estrita via `.apply()` (proibição formal de `.commit()`).
* **US4 — Pesquisa de Filmes e Provedores de Streaming (TMDB):**
  * Documentação: `docs/Use Cases/US4/US4.md` e SSD em PlantUML/SVG.
  * Rede: Configuração de `RetrofitClient.java`, interface `TmdbApiService.java` (`/search/movie`, `/watch/providers`) e classes DTO com Gson.
  * Views/Adapters: Ecrã de pesquisa, `MovieAdapter` com RecyclerView e carregamento assíncrono de posters com `.enqueue()`.
* **US5 — Localizar Salas de Cinema Próximas (Foursquare + GPS):**
  * Documentação: `docs/Use Cases/US5/US5.md` e SSD.
  * Hardware/Localização: Pedido de permissão `ACCESS_FINE_LOCATION` em tempo de execução e leitura de latitude/longitude via GPS.
  * Rede & Mapas: `FoursquareApiService.java` para recolha de POIs de cinemas num raio configurável e apresentação de marcadores no mapa com disparo de *Implicit Intent* para rotas no Google Maps.
* **US6 — Sugestão Aleatória ao Agitar o Dispositivo (Acelerómetro):**
  * Documentação: `docs/Use Cases/US6/US6.md`.
  * Hardware/Sensores: Implementação de `ShakeDetector.java` com `SensorEventListener` (`Sensor.TYPE_ACCELEROMETER`), cálculo de aceleração brusca ($>2.7g$) e gestão do ciclo de vida em `onResume()` e `onPause()`.
  * Controllers: Feedback háptico (vibração suave) e apresentação de diálogo com o filme sugerido aleatoriamente.
* **US10 — Visualização de Trailers Oficiais:**
  * Documentação: `docs/Use Cases/US10/US10.md`.
  * Rede & Intents: Consulta de chave de vídeo no endpoint `/movie/{id}/videos` do TMDB e lançamento de *Implicit Intent* defensivo (`ACTION_VIEW` com URI `vnd.youtube:...` e fallback para navegador web).

---

## 4. Roteiro de Execução em 3 Fases Paralelas

```
Fase 1 (Fundação de Sessão & Descoberta):
   André: US1 (Firebase Auth) + US7 (Perfil/Logout)
   Afonso: US8 (SharedPreferences) + US4 (Retrofit TMDB)
         ⬇ [Sincronização & Merge para main]

Fase 2 (Catálogo Pessoal & Hardware/Sensores):
   André: US2 (Listas Firestore) + US3 (Filmes & Avaliações Firestore)
   Afonso: US5 (GPS + Cinemas Foursquare) + US6 (Acelerómetro Shake)
         ⬇ [Sincronização & Merge para main]

Fase 3 (Enriquecimento, Filtros & Integrações Externas):
   André: US9 (Ordenação/Filtros) + US11 (Partilha Social)
   Afonso: US10 (Trailers YouTube) + Polimento de APIs
         ⬇ [Validação Final, Testes & Fecho do Relatório LNCS]
```

---

## 5. Protocolo de Revisão e Preparação para a Defesa Oral

1. **Revisão Cruzada (Peer Review):**  
   Nenhum código entra no ramo `main` sem que o par tenha executado os testes unitários (`./gradlew test`) e compreendido a lógica de implementação.
2. **Perguntas de Ensaio Mútuo:**  
   Antes de cada *merge*, cada membro deve explicar ao colega de equipa:
   * O porquê das opções arquiteturais adotadas;
   * Onde reside a responsabilidade do Model, da View e do Controller na funcionalidade;
   * Como é garantido que a *UI Thread* nunca é bloqueada.
