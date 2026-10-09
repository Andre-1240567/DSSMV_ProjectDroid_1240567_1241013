# Glossário de Termos e Conceitos — CineTrack

Este glossário estabelece a terminologia oficial e os conceitos concetuais e técnicos adotados no projeto **CineTrack** (*ProjectDroid*), no âmbito da unidade curricular de Desenvolvimento de Software e Sistemas Móveis (DSSMV).

---

## 1. Conceitos do Modelo de Domínio (Domain Concepts)

| Termo (Inglês) | Designação em Português | Definição e Papel no Sistema |
| :--- | :--- | :--- |
| **User** | Utilizador | Entidade que representa a conta autenticada no sistema. Possui identificador único (UID gerado pelo Firebase), nome, email e data de registo. |
| **MovieList** | Lista de Filmes | Coleção personalizada de filmes criada pelo utilizador para organização pessoal (ex.: "Favoritos", "Para Ver Mais Tarde"). |
| **SavedMovie** | Filme Guardado | Instância local de um filme integrada numa lista do utilizador, sincronizada com o identificador numérico externo do TMDB. |
| **WatchStatus** | Estado de Visualização | Enumeração representativa do progresso de consumo audiovisual do filme pelo utilizador: `PLAN_TO_WATCH`, `WATCHING`, `WATCHED`, `FAVORITE`. |
| **Rating** | Avaliação / Crítica | Classificação quantitativa de um filme com nota estritamente compreendida entre 1.0 e 5.0 estrelas e comentário qualitativo textual opcional. |
| **WatchProvider** | Fornecedor de Streaming | Plataforma digital licenciada de distribuição de streaming disponível na região portuguesa (ex.: Netflix, HBO Max, Disney+), obtida via TMDB API. |
| **Cinema** | Sala de Cinema / POI | Ponto de interesse geográfico nas imediações do utilizador, caracterizado por latitude, longitude, endereço e distância linear, obtido via Foursquare API. |
| **UserPreferences** | Preferências do Utilizador | Conjunto de definições leves locais (raio de pesquisa de cinemas em km e região padrão de streaming) persistidas em `SharedPreferences`. |

---

## 2. Conceitos Arquiteturais e Padrões de Desenho

| Termo | Definição Técnica |
| :--- | :--- |
| **MVC (Model-View-Controller)** | Padrão arquitetural que segrega a aplicação em três camadas:<br>• **Model:** Entidades Java 21 puras e regras de negócio sem dependências do Android SDK.<br>• **View:** Ficheiros declarativos XML em `res/layout/` dimensionados em `dp`/`sp`.<br>• **Controller:** `Activities` que gerem o ciclo de vida e orquestram a comunicação assíncrona. |
| **UI Thread (Main Thread)** | Thread principal do Android responsável pela renderização da interface e pelo processamento de eventos do utilizador. **Regra de ouro:** nunca deve ser bloqueada com operações de rede, disco ou cálculos intensivos. |
| **Worker Thread** | Thread secundária executada em segundo plano para operações de entrada/saída (I/O) ou computação pesada, prevenindo travamentos na UI. |
| **ANR (*Application Not Responding*)** | Caixa de diálogo de erro acionada pelo sistema operativo Android quando a *UI Thread* permanece bloqueada por mais de 5 segundos. |
| **Explicit Intent** | Mensagem de navegação direcionada explicitamente para uma classe de atividade interna da própria aplicação (ex.: transição de `LoginActivity` para `MainActivity`). |
| **Implicit Intent** | Mensagem que solicita uma ação abstrata ao sistema Android, delegando a execução para qualquer aplicação instalada capaz de a resolver (ex.: abrir trailer no YouTube com `ACTION_VIEW` ou partilha com `ACTION_SEND`). |
| **Intent Chooser** | Janela de seleção nativa (`Intent.createChooser`) que apresenta ao utilizador todas as aplicações disponíveis para tratar um intent implícito. |

---

## 3. Serviços Remotos e Tecnologias

| Termo | Definição Técnica |
| :--- | :--- |
| **Firebase Authentication** | Serviço de Backend-as-a-Service (BaaS) da Google para gestão segura de credenciais, registo, login e controlo de tokens de sessão. |
| **Cloud Firestore** | Base de dados NoSQL flexível e em tempo real alojada na nuvem da Google, utilizada para persistência das listas e avaliações do utilizador. |
| **Retrofit 2** | Cliente HTTP tipado para Java e Android que abstrai pedidos REST, utilizando conversores Gson e processamento assíncrono via `.enqueue()`. |
| **The Movie Database (TMDB)** | Serviço REST externo que fornece metadados cinematográficos, cartazes (*posters*), sinopses, trailers e fornecedores de streaming por região. |
| **Foursquare Places API** | Serviço de pesquisa geoespacial de pontos de interesse que permite consultar salas de cinema próximas por coordenadas e categoria sem exigência de cartão de crédito. |
| **SharedPreferences** | Mecanismo de persistência local em ficheiro XML para pares chave-valor primitivos. Operações de gravação utilizam obrigatoriamente `.apply()` assíncrono em detrimento de `.commit()`. |

---

## 4. Hardware e Unidades de Medida

| Termo | Definição Técnica |
| :--- | :--- |
| **Acelerómetro (`SensorManager`)** | Sensor de hardware que mede a força de aceleração em três eixos (X, Y, Z). Utilizado para a funcionalidade *Shake to Suggest* através de limiar de aceleração brusca ($> 2.7g$). |
| **GPS / Location Services** | Sistema de posicionamento global do dispositivo móvel para leitura das coordenadas geográficas (latitude e longitude) em tempo real. |
| **dp (*Density-Independent Pixel*)** | Unidade de medida espacial abstrata adaptável à densidade física do ecrã ($1 \text{ dp} = 1 \text{ px}$ num ecrã de 160 dpi). Uso obrigatório em margens e tamanhos de componentes. |
| **sp (*Scale-Independent Pixel*)** | Unidade semelhante ao `dp`, mas redimensionável em função das preferências de tamanho de texto e acessibilidade definidas pelo utilizador no sistema operativo. |
