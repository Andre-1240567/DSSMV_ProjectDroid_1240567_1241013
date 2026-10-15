# US6: [Epic: Hardware] Sugestão Aleatória ao Agitar o Dispositivo (Acelerómetro)

## 1. Descrição do Caso de Uso / User Story
Como um utilizador do CineTrack indeciso sobre que filme assistir, pretendo agitar fisicamente o meu smartphone (*Shake to Suggest*) para que a aplicação selecione e me apresente instantaneamente um filme aleatório da minha coleção de filmes guardados, acompanhado por um ligeiro feedback tátil/vibração e um diálogo modal com o título, poster e sinopse do filme sorteado.

---

## 2. Critérios de Aceitação (Acceptance Criteria)
* **AC01 — Leitura de Aceleração com o Sensor Nativo:** A deteção do movimento físico deve recorrer à classe nativa `SensorManager` e ao acelerómetro do hardware (`Sensor.TYPE_ACCELEROMETER`), encapsulada na classe utilitária `ShakeDetector.java` (implementando `SensorEventListener`).
* **AC02 — Limiar de Aceleração e Filtro de Ruído:** O detetor deve calcular a aceleração vetorial instantânea:
  $$g = \frac{\sqrt{a_x^2 + a_y^2 + a_z^2}}{g_0}$$
  onde $g_0 \approx 9.81\text{ m/s}^2$. Apenas movimentos com força superior a $2.7g$ devem disparar o evento.
* **AC03 — Prevenção de Múltiplos Disparos (*Debounce*):** O sistema deve implementar um intervalo mínimo de arrefecimento (cooldown de pelo menos $1000\text{ ms}$) após um disparo válido para impedir deteções sucessivas acidentais no mesmo movimento de agitação.
* **AC04 — Gestão Rigorosa do Ciclo de Vida da Activity (Preservação de Bateria):** O listener do sensor de hardware deve ser **registado obrigatoriamente no método `onResume()`** e **desregistado no método `onPause()`** da `Activity`. Deixar o acelerómetro ativo em background é expressamente proibido para salvaguardar a bateria do dispositivo.
* **AC05 — Feedback Háptico Suave:** Ao detetar a agitação com sucesso, o sistema deve fornecer uma vibração suave (via `Vibrator` ou feedback tátil de sistema), indicando ao utilizador que o gesto foi reconhecido.
* **AC06 — Seleção Aleatória e Apresentação:** O filme sorteado deve provir da coleção de filmes associados ao utilizador. A sugestão é apresentada através de um diálogo (`dialog_shake_suggestion.xml`) contendo o poster, o título, o ano de lançamento e um botão para abrir os detalhes completos.
* **AC07 — Estado Vazio Defensivo:** Se o utilizador não possuir filmes guardados na sua coleção, o sistema deve exibir uma mensagem indicativa encorajando a guardar filmes em listas primeiro.

---

## 3. Pré-condições e Pós-condições
* **Pré-condições:** O dispositivo Android deve possuir sensor de acelerómetro e a aplicação deve estar no primeiro plano (*foreground*).
* **Pós-condições:** Um diálogo modal com a sugestão sorteada é apresentado no ecrã e o utilizador pode navegar para a ficha do filme.

---

## 4. Fluxo Principal de Eventos
1. O utilizador encontra-se no ecrã da aplicação com a `Activity` em estado ativo (`onResume`).
2. O sistema inicializa o `SensorManager` e regista o `ShakeDetector` para escutar eventos de aceleração.
3. O utilizador agita energicamente o dispositivo físico (ultrapassando o limiar de $2.7g$).
4. O `ShakeDetector` deteta o evento, verifica o término do período de arrefecimento e notifica o listener.
5. O sistema emite um feedback háptico (vibração curta).
6. O sistema obtém a lista de filmes guardados pelo utilizador nas suas listas.
7. O sistema sorteia pseudo-aleatoriamente um filme dessa coleção.
8. O sistema exibe o diálogo de sugestão com o poster, título e sinopse do filme sorteado.
9. O utilizador visualiza a recomendação e clica em "Ver Detalhes".
10. O sistema abre a `MovieDetailActivity` com a ficha completa do filme sugerido.

---

## 5. Fluxos Alternativos e Exceções
* **FA1 — Utilizador Fecha a Sugestão:**
  * No passo 9, o utilizador clica em "Fechar" ou fora do diálogo. O sistema encerra o diálogo e mantém o ecrã anterior ativo.
* **FA2 — Ausência de Sensor de Acelerómetro no Dispositivo:**
  * No passo 2, se o hardware não dispuser de acelerómetro (ex.: emulador desprovido de sensores virtuais), o sistema ignora silenciosamente o registo sem provocar crash e disponibiliza uma opção em menu/botão como fallback.
* **E1 — Coleção de Filmes Vazia:**
  * No passo 6, caso o utilizador ainda não tenha nenhum filme nas suas listas, o sistema apresenta um diálogo informativo: *"Ainda não tens filmes guardados para sortear. Explora o catálogo e adiciona filmes às tuas listas!"*.

---

## 6. Diagrama de Sequência de Sistema (SSD)

Ficheiro PlantUML fonte: [`puml/ssd_us6_shake_suggest.puml`](puml/ssd_us6_shake_suggest.puml)

![Diagrama de Sequência de Sistema - US6](svg/ssd_us6_shake_suggest.svg)

---

## 7. Mapeamento Arquitetural (MVC)
* **Model:** [`SavedMovie.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/model/SavedMovie.java) e [`MovieList.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/model/MovieList.java).
* **Hardware & Sensor Service:** `ShakeDetector.java` no pacote `pt.isep.dssmv.projectdroid.sensors`.
* **Views:** `dialog_shake_suggestion.xml` concebido em `dp`/`sp`.
* **Controller:** `MainActivity.java` gerindo `onResume()` e `onPause()`.
