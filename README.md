# Listful — Voice Command Shopping Assistant

A voice-driven shopping list: speak or type a command, a Spring Boot API
parses the intent and updates the list, and the page shows smart
suggestions (seasonal picks, frequently-bought nudges, substitutes).

This is now a **single fullstack app** — one server, one port, one URL.
Spring Boot serves both the REST API and the web page itself, so there's
no separate frontend process to run.

## Stack

- **Backend + Frontend server:** Spring Boot 4.1.0 (Java 25), Spring Data JPA, **MySQL**
- **Frontend:** vanilla HTML / CSS / JavaScript (served as static files by Spring Boot), Web Speech API for voice input
- **No external paid services** — NLP is rule-based (`CommandParser.java`)

## Project layout

```
backend/
  src/main/java/...          <- Spring Boot application + REST API
  src/main/resources/
    application.properties
    static/                  <- the website itself (Spring Boot serves these automatically)
      index.html
      style.css
      app.js
```

Anything placed in `src/main/resources/static/` is automatically served
by Spring Boot at the root URL — that's the whole trick. No separate
frontend folder, no second server, no CORS juggling.

## Running it

**Requires JDK 25, Maven 3.9+, and a running MySQL server (8.0+).**

### 1. Set your MySQL credentials

Open `backend/src/main/resources/application.properties` and update these
two lines to match your local MySQL login:

```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

You don't need to manually create the `shoppingdb` database — the
connection URL includes `createDatabaseIfNotExist=true`, so MySQL creates
it automatically on first run (as long as your user has permission to
create databases). The `items` table itself is created automatically by
Hibernate on startup.

### 2. Run it

```
cd backend
mvn spring-boot:run
```

Then open:

```
http://localhost:8080
```

### Don't have MySQL installed yet / just want to try it quickly?

Run with the H2 in-memory profile instead — no install required, but data
resets every restart:

```
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Running in IntelliJ IDEA

1. File → Open → select the `backend/` folder (the one with `pom.xml`)
2. Let Maven finish downloading dependencies
3. Edit `application.properties` with your MySQL credentials (step 1 above)
4. Right-click `VoiceShoppingAssistantApplication.java` → Run
5. Open `http://localhost:8080` in your browser

To run with the H2 fallback profile in IntelliJ instead: **Run → Edit
Configurations** → select the app's run configuration → under "Active
profiles" (or "Program arguments", depending on IntelliJ version) put `h2`.

> **Note on this build environment:** I wrote and reviewed all backend
> code by hand and compiled the Java sources against a real JDK 25 with
> `javac --release 25` to confirm there are no language-level issues. I
> could not run `mvn spring-boot:run` end-to-end in the sandbox I built
> this in, since it has no network access to Maven Central. Run it
> locally as your first real build — if anything doesn't compile, share
> the error and I'll fix it.

## Approach (≈200 words)

I split the system into two clean layers that now share one server.
Spring Boot serves the static frontend directly from
`src/main/resources/static/`, so the whole app is reachable from a
single URL and port — no separate dev server, no cross-origin requests
to manage.

The **frontend** only does two jobs: capture speech via the Web Speech
API (with a language dropdown for multilingual input) and render
whatever the API returns — it does no command interpretation itself, so
there's one source of truth for intent logic. A manual text input covers
browsers without speech support.

The **backend** does the real work. `CommandParser` is a small rule-based
NLP layer that recognizes varied phrasings ("add milk", "I need apples",
"I want to buy bananas") via trigger-word matching plus regex for
quantities, units, and price filters ("find toothpaste under $5").
`ShoppingListService` turns parsed commands into list mutations,
`CategoryClassifier` auto-tags items by aisle, and `SmartSuggestionService`
surfaces seasonal items and things bought often but not yet on today's list.

Given the 8-hour budget, I skipped a real ML/NLU service and cloud
deployment in favor of a fully working, dependency-light demo that runs
entirely on a laptop.

## API endpoints

| Method | Path                         | Purpose                                  |
|--------|------------------------------|-------------------------------------------|
| GET    | `/`                          | The web app itself                        |
| POST   | `/api/voice-command`         | Send `{ "transcript": "..." }`, get back the parsed intent + result |
| GET    | `/api/items`                 | Current (unpurchased) list                |
| POST   | `/api/items`                 | Manually add an item                      |
| PATCH  | `/api/items/{id}/purchased`  | Toggle purchased state                    |
| DELETE | `/api/items/{id}`            | Remove one item                           |
| DELETE | `/api/items`                 | Clear the whole list                      |
| GET    | `/api/suggestions`           | Seasonal + frequently-bought suggestions  |

## Example voice/text commands

- "Add milk" · "I need apples" · "I want to buy bananas"
- "Add 2 bottles of water" · "Buy 5 oranges"
- "Remove milk from my list"
- "Find toothpaste under $5"
- "Clear my list"

## UI highlights

- **Aisle-grouped list** — items are grouped and sorted the way a store is laid out (Produce → Dairy → Bakery → …), not just a flat list
- **Progress bar** — "X of Y picked up" tracks purchased vs. active items
- **Toast notifications** — command feedback appears as a stacked, auto-dismissing toast instead of a static banner, so multiple quick commands don't overwrite each other's messages
- **Swap suggestions** — when you add an item with a known substitute (e.g. milk → almond/oat/soy milk), a one-tap "swap for X" chip appears
- **Picked-up fold** — purchased items collapse into a foldable section instead of cluttering the active list
- **Loading skeleton** — the list shows a shimmer placeholder while the initial fetch is in flight
- **Waveform + ripple mic** — animated feedback while listening, with `prefers-reduced-motion` respected throughout

## Switching to a different database (optional)

MySQL is the default now. To go back to zero-install H2 for quick local
testing, use the `h2` Spring profile (see "Running it" above) — no code
or config changes needed, it's already wired up in
`application-h2.properties`.

## What's not included (out of scope for 8 hours)

- Cloud hosting/deployment (runs locally; any of AWS/Firebase/Render would work with no code changes — the static/ folder deploys as-is)
- A trained ML recommendation model (suggestions are rule/history-based instead)
- True translation of non-English speech to English item names (the browser transcribes in the selected language; item names are stored as spoken)
