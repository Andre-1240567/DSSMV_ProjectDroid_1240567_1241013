# US5: [Epic: Mapas] Localizar Salas de Cinema Próximas (GPS + Foursquare + OSMDroid)

## 1. Descrição do Caso de Uso / User Story
Como um utilizador do CineTrack, pretendo localizar salas de cinema nas minhas imediações geográficas com base nas coordenadas de GPS do dispositivo e num raio configurável, visualizando os cinemas simultaneamente num mapa interativo (OpenStreetMap) e numa lista descritiva ordenada por distância, com indicação visual da área de pesquisa (circunferência translúcida azulada), marcadores clicáveis com pop-ups informativos e atalhos para abrir direções no Google Maps via *Implicit Intent*.

---

## 2. Critérios de Aceitação (Acceptance Criteria)
* **AC01 — Gestão de Permissões em Tempo de Execução (*Runtime Permissions*):** O sistema deve verificar a permissão `ACCESS_FINE_LOCATION`. Caso não esteja concedida, deve apresentar o diálogo nativo do Android solicitando autorização com explicação clara do motivo. Se negada, deve informar o utilizador sem bloquear a aplicação (RNF06).
* **AC02 — Obtenção de Coordenadas GPS Nativas:** A localização do utilizador deve ser recolhida através do hardware nativo (`LocationManager` com `GPS_PROVIDER` ou `NETWORK_PROVIDER`).
* **AC03 — Integração REST com a Foursquare Places API:** O sistema deve consultar assincronamente a modern API da Foursquare (`https://places-api.foursquare.com/places/search`) com categoria de salas de cinema (`10024`), passando as coordenadas atuais (`ll`), raio configurado no `SharedPrefManager`, ordenação por distância (`sort=DISTANCE`) e limite de 20 resultados.
* **AC04 — Segurança e Autenticação por Bearer Token:** A autenticação na Places API recorre ao cabeçalho `Authorization: Bearer <key>` injetado via `BuildConfig.FOURSQUARE_API_KEY` acompanhado do cabeçalho de versão `X-Places-Api-Version: 2025-06-17`.
* **AC05 — Mapeamento para o Domínio:** Cada resultado é instanciado na entidade pura [`Cinema.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/model/Cinema.java), preenchendo identificador Foursquare, nome, latitude, longitude, morada e distância em metros.
* **AC06 — Visualização de Mapa Interativo (OSMDroid):** O sistema apresenta um mapa interativo nativo OpenStreetMap (sem dependência de chaves pagas do Google Cloud):
  * **Auto-Centragem:** Centra automaticamente a câmara do mapa nas coordenadas GPS do utilizador com zoom adaptativo.
  * **Marcador de Utilizador:** Exibe um marcador azul (`ic_my_location`) na posição do utilizador.
  * **Circunferência Azulada de Raio:** Traça uma sobreposição poligonal circular translúcida azulada (`#262196F3`) com contorno sólido (`#2196F3`) correspondente ao raio de pesquisa selecionado nas preferências.
  * **Marcadores de Cinema com Pop-ups:** Plota pins vermelhos (`ic_cinema_pin`) para cada cinema encontrado. O clique no marcador abre uma bolha informativa (*InfoWindow*) com nome, distância e morada.
* **AC07 — Split View e Seletor de Modo de Visualização:** O ecrã dispõe de um seletor com três opções:
  * **Both:** Ecrã dividido (50% Mapa no topo, 50% Lista no fundo).
  * **Map:** Visualização expandida a 100% do mapa.
  * **List:** Visualização expandida a 100% da lista de cinemas.
* **AC08 — Sincronização Lista ⇄ Mapa:** Ao clicar num cartão de cinema na lista, o mapa anima e centra automaticamente sobre o marcador do cinema correspondente, abrindo a respetiva bolha informativa.
* **AC09 — Navegação Externa via Implicit Intent:** Ao clicar no botão "Directions" ou no pop-up do mapa, o sistema dispara um *Implicit Intent* com ação `Intent.ACTION_VIEW` e esquema `geo:latitude,longitude?q=Nome+do+Cinema` com fallback seguro para Google Maps no navegador.

---

## 3. Pré-condições e Pós-condições
* **Pré-condições:** O dispositivo deve ter o serviço de localização/GPS ativado, conectividade à Internet e autorização de permissão concedida.
* **Pós-condições:** As salas de cinema encontradas são apresentadas visualmente no mapa e listadas no `RecyclerView` ordenadas pela distância calculada ao utilizador.

---

## 4. Fluxo Principal de Eventos
1. O utilizador acede ao ecrã de cinemas próximos (`CinemasActivity`).
2. O sistema verifica as permissões de localização. Estando autorizadas, obtém a última posição conhecida ou solicita atualização ao GPS.
3. O sistema lê o raio de pesquisa configurado nas preferências locais via `SharedPrefManager`.
4. O sistema centra o mapa nas coordenadas adquiridas, desenha a circunferência azulada do raio e posiciona o marcador do utilizador.
5. O sistema exibe um indicador de carregamento (`ProgressBar`) e submete o pedido assíncrono à Foursquare Places API via Retrofit (`.enqueue()`).
6. A Foursquare API devolve os locais correspondentes em JSON.
7. O sistema valida e instancia os objetos de domínio `Cinema`.
8. O sistema plota os marcadores de cinema no mapa e preenche o `RecyclerView` através do `CinemaAdapter`.
9. O utilizador seleciona um cinema na lista (centrando o mapa) ou clica no marcador para obter detalhes.
10. O utilizador aciona o pedido de direções.
11. O sistema constrói e dispara um *Implicit Intent* com o URI geográfico (`geo:...`).
12. O Android abre o seletor ou a aplicação de mapas externa apresentando a rota.

---

## 5. Fluxos Alternativos e Exceções
* **FA1 — Permissão de Localização Recusada:**
  * No passo 2, se o utilizador recusar a permissão `ACCESS_FINE_LOCATION`, o sistema exibe um aviso explicativo e oferece um botão para tentar novamente ou abrir as definições da aplicação, não bloqueando a aplicação.
* **FA2 — GPS Desativado no Dispositivo:**
  * No passo 2, caso o provedor de localização esteja desligado, o sistema alerta o utilizador sugerindo a ativação dos serviços de localização.
* **FA3 — Alteração do Modo de Visualização:**
  * A qualquer momento, o utilizador pode alternar entre "Both", "Map" e "List", reorganizando a visibilidade do mapa e do RecyclerView.
* **E1 — Nenhum Cinema Encontrado no Raio:**
  * No passo 6, caso a lista de resultados esteja vazia, o sistema oculta a lista e apresenta o estado vazio (*Empty State*), sugerindo aumentar o raio de pesquisa nas preferências.
* **E2 — Falha de Conexão à Internet:**
  * No passo 5, se a chamada de rede falhar, o sistema apresenta um aviso de erro e desativa o indicador de progresso.

---

## 6. Diagrama de Sequência de Sistema (SSD)

Ficheiro PlantUML fonte: [`puml/ssd_us5_nearby_cinemas.puml`](puml/ssd_us5_nearby_cinemas.puml)

![Diagrama de Sequência de Sistema - US5](svg/ssd_us5_nearby_cinemas.svg)

---

## 7. Mapeamento Arquitetural (MVC)
* **Model:** [`Cinema.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/model/Cinema.java) e [`InvalidDataException.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/exceptions/InvalidDataException.java).
* **Network & Services:** `FoursquareApiService.java`, DTOs (`FsqPlacesResponse`, `FsqPlaceDto`), `RetrofitClient.java` e `LocationHelper.java`.
* **Views & Adapters:** `activity_cinemas.xml`, `item_cinema.xml`, `CinemaAdapter.java`, drawables `ic_my_location.xml` e `ic_cinema_pin.xml`, e `org.osmdroid.views.MapView`.
* **Controller:** `CinemasActivity.java` no pacote `pt.isep.dssmv.projectdroid.controllers`.
