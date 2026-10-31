# Glossário de Termos e Conceitos — CineTrack

Este glossário estabelece a terminologia oficial e os conceitos concetuais e técnicos adotados no projeto **CineTrack** (*ProjectDroid*), no âmbito da unidade curricular de Desenvolvimento de Software e Sistemas Móveis (DSSMV) da Licenciatura em Engenharia de Telecomunicações e Informática (LETI) do ISEP.

---

## 1. Conceitos do Modelo de Domínio (Domain Concepts)

| Termo (Inglês) | Designação em Português | Definição e Papel no Sistema |
| :--- | :--- | :--- |
| **User** | Utilizador | Entidade que representa a conta autenticada no sistema. Possui identificador único (UID gerado pelo Firebase), nome, email e data de registo. |
| **MovieList** | Lista de Filmes | Coleção personalizada de filmes criada pelo utilizador para organização pessoal (ex.: "Favoritos", "Para Ver Mais Tarde", "Clássicos"). |
| **SavedMovie** | Filme Guardado | Instância local de um filme integrada numa lista do utilizador, sincronizada com o identificador numérico externo do TMDB, com estado de visualização e notas associadas. |
| **WatchStatus** | Estado de Visualização | Enumeração representativa do progresso de consumo audiovisual do filme pelo utilizador: `PLAN_TO_WATCH`, `WATCHING`, `WATCHED`, `FAVORITE`. |
| **Rating** | Avaliação / Crítica | Classificação quantitativa de um filme com nota estritamente compreendida entre 1.0 e 5.0 estrelas e comentário qualitativo textual opcional. |
| **SavedMovieSortOption** | Opção de Ordenação | Critério de ordenação da coleção local: `TITLE_ASC` (alfabética), `RATING_DESC` (melhor avaliados), `YEAR_DESC` (mais recentes) ou `DATE_ADDED_DESC` (adicionados recentemente). |
| **SavedMovieFilterSorter**| Utilitário de Filtragem/Ordenação | Componente de domínio em Java 21 puro encarregue de aplicar comparadores (`Comparator<SavedMovie>`) e predicados de filtragem sem dependências do Android SDK. |
| **WatchProvider** | Fornecedor de Streaming | Plataforma digital licenciada de distribuição de streaming disponível na região portuguesa (ex.: Netflix, HBO Max, Disney+, Prime Video, Apple TV+), obtida via TMDB API. |
| **Cinema** | Sala de Cinema / POI | Ponto de interesse geográfico nas imediações do utilizador, caracterizado por identificador Foursquare, nome, latitude, longitude, endereço formatado e distância em metros. |
| **UserPreferences** | Preferências do Utilizador | Conjunto de definições leves locais (raio de pesquisa de cinemas em km, região padrão, sensibilidade do acelerómetro e subscrições ativas) persistidas em `SharedPreferences`. |

---

## 2. Padrões de Arquitetura e Engenharia Móvel

| Termo | Definição Técnica |
| :--- | :--- |
| **MVC (Model-View-Controller)** | Padrão arquitetural que segrega a aplicação em três camadas fundamentais:<br>• **Model:** Entidades Java 21 puras e regras de negócio sem dependências do Android SDK.<br>• **View:** Ficheiros declarativos XML em `res/layout/` dimensionados estritamente em `dp`/`sp`.<br>• **Controller:** `Activities` que orquestram o ciclo de vida, eventos de UI e a comunicação assíncrona. |
| **UI Thread (Main Thread)** | Thread principal do Android responsável pela renderização da interface e pelo processamento de eventos de toque. **Regra de ouro:** nunca pode ser bloqueada com operações de I/O, rede, disco ou cálculos intensivos. |
| **Worker Thread (Background)** | Thread secundária executada em segundo plano para operações de entrada/saída (I/O) ou computação pesada, prevenindo travamentos na UI Thread. |
| **ANR (*Application Not Responding*)** | Caixa de diálogo de erro acionada pelo sistema operativo Android quando a *UI Thread* permanece bloqueada por mais de 5 segundos. |
| **Explicit Intent** | Mensagem de navegação direcionada explicitamente para uma classe de atividade interna da própria aplicação (ex.: transição de `LoginActivity` para `MainActivity`). |
| **Implicit Intent** | Mensagem que solicita uma ação abstrata ao sistema Android, delegando a execução para qualquer aplicação externa capaz de a resolver (ex.: abrir trailer no YouTube com `ACTION_VIEW` ou navegação GPS no Google Maps). |
| **Intent Chooser** | Diálogo nativo do sistema (`Intent.createChooser`) que apresenta ao utilizador todas as aplicações disponíveis para tratar um intent implícito. |
| **Activity Lifecycle** | Ciclo de estados gerenciado pelo Android (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`). Crucial para registrar/desregistrar listeners de sensores e prevenir memory leaks. |
| **Adapter Pattern** | Padrão estrutural que faz a ponte entre a fonte de dados (listas de modelos de domínio) e os componentes visuais de renderização em grelha ou lista (`RecyclerView.Adapter`). |
| **ViewHolder Pattern** | Padrão de otimização de performance no Android que armazena em cache as referências das views filhas de cada item, evitando chamadas repetitivas e lentas a `findViewById`. |
| **Realtime Snapshot Listener**| Mecanismo reativo de escuta de dados remotos no Cloud Firestore (`addSnapshotListener`) que notifica a aplicação instantaneamente perante alterações no servidor sem necessidade de polling. |

---

## 3. Serviços Remotos e Tecnologias de Rede

| Termo | Definição Técnica |
| :--- | :--- |
| **Firebase Authentication** | Serviço de Backend-as-a-Service (BaaS) da Google para gestão segura de credenciais, registo, login e controlo de tokens de sessão. |
| **Cloud Firestore** | Base de dados NoSQL documental, em nuvem e em tempo real, utilizada para a persistência das listas personalizadas e filmes dos utilizadores. |
| **Retrofit 2** | Cliente HTTP type-safe para Java e Android que abstrai pedidos REST, utilizando conversores Gson e processamento assíncrono via `.enqueue(Callback<T>)`. |
| **The Movie Database (TMDB)** | Serviço REST externo que fornece metadados cinematográficos, cartazes (*posters*), sinopses, trailers oficiais e fornecedores de streaming por região. |
| **TMDB Guest Session** | Mecanismo de autenticação efémera fornecido pela API do TMDB que permite submeter notas diretamente no ecossistema global do TMDB via POST/DELETE sem exigir login web complexo. |
| **Foursquare Places API** | Serviço de pesquisa geoespacial de pontos de interesse (modern Places API v2025-06-17) autenticado via Bearer Token para consulta de salas de cinema (`categories=10024`). |
| **SharedPreferences** | Mecanismo de persistência local em ficheiro XML privado para pares chave-valor primitivos. Operações de gravação utilizam obrigatoriamente `.apply()` assíncrono em detrimento de `.commit()`. |
| **OkHttp Logging Interceptor**| Intercetor de rede configurado no cliente HTTP para logging de pedidos e respostas durante o desenvolvimento e depuração. |

---

## 4. Bibliotecas de Terceiros e Frameworks

| Termo | Definição Técnica |
| :--- | :--- |
| **OSMDroid (OpenStreetMap)** | Biblioteca de mapeamento open-source para Android que permite renderizar mosaicos de mapas (*tiles*), traçar circunferências poligonais (`Polygon`) e plotar marcadores interativos (`Marker`) sem necessidade de chaves de API pagas ou consolas Google Cloud. |
| **Glide** | Biblioteca de carregamento, descodificação e cacheamento eficiente de imagens (como posters e logótipos de streaming) com placeholders e suporte ao ciclo de vida da Activity. |
| **Gson** | Biblioteca de serialização e desserialização entre objetos Java e representações JSON através de anotações como `@SerializedName`. |
| **Material Design Components**| Biblioteca oficial da Google (`com.google.android.material`) que fornece componentes visuais avançados (`MaterialButtonToggleGroup`, `FloatingActionButton`, `BottomNavigationView`). |

---

## 5. Hardware, Sensores e Unidades de Medida

| Termo | Definição Técnica |
| :--- | :--- |
| **Acelerómetro (`SensorManager`)** | Sensor de hardware que mede a força de aceleração em três eixos ortogonais (X, Y, Z). Utilizado na funcionalidade *Shake to Suggest* com limiar ajustável ($> 2.7g$). |
| **Multi-Peak Shake Filter** | Algoritmo implementado em `ShakeDetector` que exige inversões de aceleração deliberadas (múltiplos picos) e tempo de cooldown para evitar falsos positivos provocados pelo simples caminhar. |
| **GPS / Location Services** | Sistema de posicionamento global do dispositivo móvel (`LocationManager` com `GPS_PROVIDER` ou `NETWORK_PROVIDER`) para leitura de latitude e longitude em tempo real. |
| **Runtime Permissions** | Modelo de segurança introduzido no Android 6.0 (API 23) em que permissões perigosas (`ACCESS_FINE_LOCATION`) têm de ser explicitamente autorizadas pelo utilizador em tempo de execução via diálogo nativo. |
| **Haptic Feedback (`Vibrator`)**| Resposta táctil ao utilizador produzida através do motor de vibração do dispositivo aquando da deteção do gesto de agitar (*Shake*). |
| **dp (*Density-Independent Pixel*)** | Unidade de medida espacial abstrata adaptável à densidade física do ecrã ($1 \text{ dp} = 1 \text{ px}$ num ecrã de 160 dpi). Uso obrigatório em margens, paddings e dimensões estruturais. |
| **sp (*Scale-Independent Pixel*)** | Unidade semelhante ao `dp`, mas redimensionável em função das preferências de acessibilidade e tamanho de fonte configuradas pelo utilizador no sistema operativo. |

---

## 6. Qualidade, Testabilidade e Segurança

| Termo | Definição Técnica |
| :--- | :--- |
| **JUnit 5 (Jupiter BOM)** | Framework de testes unitários utilizado para validação exaustiva do modelo de domínio, comparadores de ordenação e deserialização de DTOs JSON. |
| **Test-Driven Development (TDD)** | Prática de desenvolvimento onde os testes unitários são concebidos em paralelo com a especificação das entidades de domínio, garantindo pureza arquitetural sem dependências do Android SDK. |
| **BuildConfig & `.env`** | Mecanismo de injeção segura de segredos em tempo de compilação via Gradle, prevenindo a inclusão acidental de chaves de API (`TMDB_API_KEY`, `FOURSQUARE_API_KEY`) no controlo de versões Git. |
| **Git Feature Branch Workflow**| Estrutura de ramificação que proíbe commits diretos na branch `main`, exigindo branches individuais dedicadas (`afonso-1241013`, `andre-1240567`) com integração via Pull Requests validados por testes. |
