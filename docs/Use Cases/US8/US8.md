# US8: [Epic: Persistence] Guardar Preferências Locais (SharedPreferences)

## 1. Descrição do Caso de Uso / User Story
Como um utilizador do CineTrack, pretendo configurar e persistir localmente as minhas preferências de aplicação (nomeadamente o raio de pesquisa de salas de cinema em quilómetros e o país/região padrão para consulta de catálogos e streaming), de modo a que a aplicação preserve estas definições de forma duradoura entre sessões e sem efetuar operações síncronas que afetem a fluidez da interface gráfica (*UI Thread*).

---

## 2. Critérios de Aceitação (Acceptance Criteria)
* **AC01 — Validação na Camada de Domínio Puro:** Os dados são validados estritamente pela entidade pura [`UserPreferences.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/model/UserPreferences.java). O raio de busca deve situar-se obrigatoriamente entre $1$ e $100\text{ km}$, e a região padrão não pode ser nula nem vazia (ex.: `"PT"`). Violações disparam a exceção checada [`InvalidDataException.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/exceptions/InvalidDataException.java).
* **AC02 — Valores Padrão (Fallbacks):** Caso o utilizador nunca tenha gravado preferências, o sistema deve assumir por omissão $10\text{ km}$ para o raio de cinemas e `"PT"` para a região padrão.
* **AC03 — Persistência Não-Bloqueante (RNF05):** A escrita de preferências em `SharedPreferences` deve ser realizada imperativa e exclusivamente através de **`.apply()`** (execução assíncrona em background). É **estritamente proibido** o recurso a `.commit()` (síncrono/bloqueante).
* **AC04 — Encapsulamento em Gestor Centralizado:** O acesso a `SharedPreferences` deve ser encapsulado na classe utilitária `SharedPrefManager.java` (padrão Singleton ou Factory com `Context.getSharedPreferences`), protegendo a lógica das *Activities*.
* **AC05 — Design Responsivo de Interface (RNF03):** O ecrã ou diálogo de preferências é desenhado em XML usando estritamente unidades **`dp`** e **`sp`**, sem píxeis absolutos (`px`).

---

## 3. Pré-condições e Pós-condições
* **Pré-condições:** A aplicação deve estar em execução no dispositivo Android.
* **Pós-condições:** As preferências são gravadas em ficheiro XML privado de `SharedPreferences` no armazenamento interno do dispositivo (`/data/data/pt.isep.dssmv.projectdroid/shared_prefs/`).

---

## 4. Fluxo Principal de Eventos
1. O utilizador acede ao ecrã/diálogo de Definições/Preferências.
2. O sistema consulta `SharedPrefManager` e obtém os valores correntes de raio e região.
3. O sistema apresenta o formulário preenchido com os valores ativos.
4. O utilizador ajusta o raio de pesquisa e/ou seleciona a região e clica em "Guardar".
5. O sistema instancia a entidade de domínio `UserPreferences` com os novos valores, validando as regras de negócio.
6. O sistema invoca `SharedPrefManager.saveUserPreferences(...)`, persistindo assincronamente com `.apply()`.
7. O sistema apresenta feedback de sucesso ao utilizador (*Toast* ou *Snackbar*) e encerra o ecrã/diálogo.

---

## 5. Fluxos Alternativos e Exceções
* **E1 — Valor de Raio Inválido:**
  * No passo 5, se o raio for inferior a $1$ ou superior a $100\text{ km}$, o domínio lança `InvalidDataException`.
  * O sistema captura a exceção, não grava em disco, e exibe mensagem de erro no respetivo campo (`TextInputLayout.setError`).
* **E2 — Código de Região Inválido:**
  * No passo 5, se a região estiver em branco, o domínio lança `InvalidDataException`.
  * O sistema exibe o erro e aguarda correção.

---

## 6. Diagrama de Sequência de Sistema (SSD)

Ficheiro PlantUML fonte: [`puml/ssd_us8_preferences.puml`](puml/ssd_us8_preferences.puml)

![Diagrama de Sequência de Sistema - US8](svg/ssd_us8_preferences.svg)

---

## 7. Mapeamento Arquitetural (MVC)
* **Model:** [`UserPreferences.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/model/UserPreferences.java) e [`InvalidDataException.java`](../../../src/main/java/pt/isep/dssmv/projectdroid/exceptions/InvalidDataException.java).
* **View:** `res/layout/activity_preferences.xml` ou diálogo correspondente usando `dp`/`sp`.
* **Persistence / Manager:** `SharedPrefManager.java` no pacote `pt.isep.dssmv.projectdroid.utils`.
* **Controller:** `PreferencesActivity.java` (ou gestão em diálogo) no pacote `pt.isep.dssmv.projectdroid.controllers`.
