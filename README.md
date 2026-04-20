# BUS TICKET BOOKING SYSTEM — FRONTEND / BACKEND SEPARATION GUIDE

> **Tech Stack:**
> - **Backend:** Spring Boot 3.3.5 | Java 21 | MySQL | JPA/Hibernate | iText7 | port 8080
> - **Frontend:** Spring Boot 3.3.5 | Java 21 | Thymeleaf | RestTemplate | port 8081
> - **Build Tool:** Maven | **Java version:** 21
> - **Testing:** JUnit 5 + Mockito + MockRestServiceServer + MockMvc

> **Architecture:** Two independent Spring Boot apps. Browser talks to the frontend; the frontend calls the backend over HTTP (server-to-server REST). No shared DB, no CORS, no JavaScript framework.

---

## 📑 Table of Contents

### PART A — Setup & Configuration (Sections 1–4)

| # | Section | Purpose |
|---|---|---|
| 1 | Overview & Why Separate | Rationale, benefits, trade-offs |
| 2 | Architecture Diagrams & Request Flow | Visual map of the split |
| 3 | Project Structure Deep Dive | Every folder, every file |
| 4 | Configuration Deep Dive | `pom.xml`, `application.properties`, `RestClientConfig` |

### PART B — DTOs, API Clients, View Controllers (Sections 5–7)

| # | Section | Content |
|---|---|---|
| 5 | DTOs Reference | All 23 DTOs + 2 enums, grouped by module |
| 6 | API Clients Deep Dive | `AbstractApiClient` + 11 module clients, every method |
| 7 | View Controllers Deep Dive | All 12 controllers, every handler |

### PART C — Templates, Testing & Viva (Sections 8–13)

| # | Section | Content |
|---|---|---|
| 8 | Thymeleaf Templates Overview | All 47 templates, fragments, forms |
| 9 | Error Handling | `BackendException` flow, flash attributes |
| 10 | Testing Deep Dive | 50 tests across 11 files, patterns & assertions |
| 11 | Viva Questions | **100 Q&As** across architecture / RestTemplate / Thymeleaf / Testing / Deployment |
| 12 | Running & Deployment | Local dev, JAR build, Nginx, Docker |
| 13 | Gotchas & TODOs | Known limitations, future work |

---

## 📂 Quick File Map

**Backend (`C:\Users\Sarthak\OneDrive\Desktop\gs studeis\bus-ticket-booking-system`) — unchanged, serves `/api/*`:**
- Entities, Repositories, Services, `@RestController`s
- MySQL + JPA + iText + `@Scheduled` + `GlobalExceptionHandler`

**Frontend (`C:\Users\Sarthak\OneDrive\Desktop\bus-ticket-frontend`) — new, serves `/view/*`:**
- 12 `@Controller` classes (view handlers only)
- 11 API clients (RestTemplate wrappers) + `AbstractApiClient` + `BackendException`
- 23 DTOs + 2 enums (matches backend JSON shapes)
- `RestClientConfig` (RestTemplate bean with timeouts)
- 47 Thymeleaf templates + 6 static assets
- 50 JUnit + Mockito tests

**Run both:**
```bash
# Backend (terminal 1)
cd "/c/Users/Sarthak/OneDrive/Desktop/gs studeis/bus-ticket-booking-system"
./mvnw.cmd spring-boot:run       # :8080

# Frontend (terminal 2)
cd "/c/Users/Sarthak/OneDrive/Desktop/bus-ticket-frontend"
./mvnw.cmd spring-boot:run       # :8081

# Browser
open http://localhost:8081
```

---
---
# PART A — SETUP & CONFIGURATION

This is Part 1 of the project-separation guide. It covers sections 1–4: the "why", the architecture, the folder layout, and the configuration files. Sections 5+ (controllers, API clients, DTOs, templates, testing, deployment) are continued in Part B.

Throughout this guide:

- **Backend** = `C:\Users\Sarthak\OneDrive\Desktop\gs studeis\bus-ticket-booking-system` — Spring Boot + MySQL + JPA, runs on port **8080**, exposes only `/api/*` REST endpoints.
- **Frontend** = `C:\Users\Sarthak\OneDrive\Desktop\bus-ticket-frontend` — Spring Boot + Thymeleaf + RestTemplate, runs on port **8081**, exposes only `/view/*` (and `/`, `/members`) HTML endpoints.

---

## Section 1 — Overview & Why Separate the Project

### 1.1 What is a monolith?

Before the split, the original `bus-ticket-booking-system` project was a **classical Spring Boot monolith**. A single WAR/JAR contained *everything*:

- JPA entities (`Customer`, `Trip`, `Booking`, `Payment`, …)
- Spring Data JPA repositories (`CustomerRepository`, …)
- `@Service` classes with business logic
- Two sets of controllers: `@RestController` classes returning JSON (`/api/*`) and `@Controller` classes returning Thymeleaf views (`/view/*`)
- Thymeleaf templates under `src/main/resources/templates`
- Static CSS / JS / images under `src/main/resources/static`
- `application.properties` holding the MySQL JDBC URL, username, password, Hibernate dialect, etc.
- Third-party libs like iText (for PDF invoice generation) and MySQL Connector/J

One `mvn spring-boot:run` started the whole thing on port 8080. When you hit `http://localhost:8080/view/trips`, the same JVM:

1. Received the HTTP request.
2. Ran the `@Controller` method.
3. Called a `@Service`.
4. The service called a JPA repository, which hit MySQL directly.
5. The service returned domain objects.
6. The controller put them into a `Model` and rendered a Thymeleaf template to HTML.

Everything happened in-process. One codebase, one deploy, one database connection pool, one team repo.

### 1.2 What does "separated" mean?

After the split, the same features live in **two independent Spring Boot applications** that talk to each other over HTTP.

| Concern                | Monolith           | After separation                               |
|------------------------|--------------------|------------------------------------------------|
| HTML rendering         | Same JVM           | `bus-ticket-frontend` (port 8081)              |
| REST JSON endpoints    | Same JVM           | `bus-ticket-backend` (port 8080)               |
| Database access (JPA)  | Same JVM           | Backend only                                   |
| MySQL connection       | Same JVM           | Backend only                                   |
| iText / PDF generation | Same JVM           | Backend only                                   |
| Entities               | Same JVM           | Backend only                                   |
| Thymeleaf templates    | Same JVM           | Frontend only                                  |
| Static assets          | Same JVM           | Frontend only                                  |
| DTOs                   | Same JVM           | Both (duplicated, intentionally)               |

The frontend no longer has the MySQL driver on its classpath. The backend no longer has Thymeleaf on its classpath. Each app can be built, tested, deployed, and scaled independently.

### 1.3 Why the manager asked for this split

Four practical reasons were given:

1. **Team independence.** Five of us are building this. With a monolith, two people editing `application.properties` at the same time caused merge conflicts every week. With two projects the "view team" and the "API team" rarely touch the same file.
2. **Deployment flexibility.** The backend can later be deployed to a private VM (behind a firewall, close to MySQL) while the frontend sits on a public-facing server. Before the split, this was impossible — the whole app had to live in one place.
3. **API reusability.** Once the backend exposes clean `/api/*` endpoints, *another* client (a mobile app, a partner agency, a Postman collection for testing) can consume the same endpoints. The Thymeleaf UI is no longer the only consumer.
4. **Clear responsibility boundary.** A junior on the team can now work on the frontend without ever needing to understand JPA, Hibernate lazy-loading, or cascade types. Conversely, the backend developer doesn't need to learn Thymeleaf syntax.

### 1.4 What each app does now

**Frontend (`bus-ticket-frontend`, port 8081):**

- Serves `/` (home), `/members` (team page), and `/view/**` (CRUD screens for 11 domains).
- Renders Thymeleaf templates into HTML.
- Serves static CSS/JS/images.
- Uses `RestTemplate` to call the backend over HTTP for every data operation.
- Holds **no** database driver, **no** JPA, **no** entities, **no** repositories, **no** `@Service` classes.
- Holds **23 DTOs** and **2 enums** — these are plain Java records/classes that match the JSON contract of the backend. They are intentionally duplicated; they are **not** shared via a Maven module.

**Backend (`bus-ticket-booking-system`, port 8080):**

- Exposes `/api/**` endpoints only (REST).
- Owns all JPA entities, repositories, services, MySQL access, and iText PDF generation.
- Knows nothing about HTML. Never returns a view name, never renders Thymeleaf.
- Is a "headless" API — you can exercise it with curl or Postman.

### 1.5 Benefits of this architecture

- **Independent restarts.** Change a Thymeleaf template? Restart only the frontend (or don't — DevTools + `spring.thymeleaf.cache=false` reloads live). Backend stays up with its warm connection pool.
- **Smaller attack surface per app.** The frontend JAR doesn't contain database credentials. Even if the frontend server is compromised, the MySQL password is not on that disk.
- **Horizontal scaling per tier.** If HTML rendering becomes the bottleneck (unlikely here), scale frontend pods without scaling the DB-connected backend.
- **Clear contracts.** The JSON returned by `/api/trips` is a visible, documentable contract. Changing it is a deliberate decision, not an accidental refactor of a service return type.
- **Easier testing.** Backend can be tested with `@DataJpaTest` and `MockMvc`. Frontend can be tested with `MockRestServiceServer` (no MySQL needed for frontend tests — ever).

### 1.6 Trade-offs (honest list)

- **Extra HTTP hop.** Every page render that used to be one in-process call is now an HTTP round trip. Localhost adds ~1–3 ms per call. In production over a LAN, 2–10 ms. Acceptable for CRUD screens; would be a problem for a high-throughput API gateway.
- **DTO duplication.** `TripDTO` exists in the frontend and a matching `TripDTO` exists in the backend. When a field is added, it must be added in both places. We accept this because a shared Maven module would couple the two projects and defeat part of the independence benefit.
- **Two deployments.** Every release touches two artifacts. CI must build and deploy both in the right order (backend first — frontend assumes it's available at startup for some health checks).
- **Error translation.** Backend throws a `404`; the frontend must translate that into a user-friendly "Trip not found" page. We introduced `BackendException` specifically to handle this.
- **Configuration surface doubles.** Two `application.properties`, two ports, two log files, two sets of environment variables.

### 1.7 Why Thymeleaf-on-Thymeleaf (not React / Angular / Vue)

The manager was explicit: **no JavaScript SPA**. Reasons:

- The team is a 5-person college group; nobody has shipped production React. Learning curve + 15-day deadline = risk.
- The existing views are already Thymeleaf. Rewriting them in React would be a second project on top of the split.
- Thymeleaf on the frontend still gives us the "separate projects" benefit — the backend is cleanly REST, and *later* a React SPA could replace the Thymeleaf frontend without touching the backend. The split is a prerequisite for the SPA, not a replacement for it.
- Server-to-server calls avoid CORS entirely (see Section 2.4). With a browser-based SPA, we would need to configure `@CrossOrigin` or a CORS filter on the backend.
- Authentication story is simpler: the frontend holds the session cookie with the browser; the frontend-to-backend call is anonymous server-to-server (for now — later we can add an internal API key header).

So the target is: same user-facing UX as the monolith, but the HTML is produced by a different JVM than the one talking to MySQL.

### 1.8 Viva questions — Section 1

1. **Q:** Name three things the frontend project is explicitly *not* allowed to have on its classpath.  
   **A:** The MySQL JDBC driver, `spring-boot-starter-data-jpa`, and iText (PDF generation). Also: no JPA entities, no `@Repository`, no `@Service` classes tied to persistence.
2. **Q:** Why is DTO duplication acceptable instead of a shared Maven module?  
   **A:** A shared module would force both apps to upgrade together, re-introducing the coupling we tried to remove. Duplication is a cheap price for independent release cycles.
3. **Q:** The monolith ran on port 8080. Why did we pick 8081 for the frontend and keep 8080 for the backend?  
   **A:** Keeping 8080 on the backend means existing Postman collections and curl scripts keep working. The frontend gets 8081 because both apps run on the same developer laptop during development and must not collide.
4. **Q:** Give one scenario where the monolith is actually better than the split.  
   **A:** A single-developer prototype where every feature touches both layers — the HTTP hop and double deployment are pure overhead with no team-independence payoff.
5. **Q:** If we later switch the frontend to React, what changes on the backend?  
   **A:** Add CORS configuration (`@CrossOrigin` or a `CorsFilter`) because calls will come from a browser on a different origin. The `/api/*` contract itself does not change — that is the whole point of the split.

---

## Section 2 — Architecture Diagrams & Request Flow

### 2.1 The big picture

```
+------------------+        HTTP/HTML           +-----------------------+        HTTP/JSON           +----------------------+        JDBC         +-----------+
|                  |  GET /view/trips          |                       |  GET /api/trips            |                      |  SELECT * FROM ...  |           |
|  Browser         | ------------------------> |  bus-ticket-frontend  | -------------------------> |  bus-ticket-backend  | ------------------> |  MySQL    |
|  (Chrome/Edge)   |                           |  Spring Boot 3.3.5    |                            |  Spring Boot 3.3.5   |                     |  8.x      |
|                  | <------------------------ |  Thymeleaf + RestTpl  | <------------------------- |  JPA + Hibernate     | <------------------ |           |
|                  |  200 OK  <html>...</html> |  port 8081            |  200 OK  [{...},{...}]     |  port 8080           |  ResultSet          |           |
+------------------+                           +-----------------------+                            +----------------------+                     +-----------+
        ^                                                 |                                                   |
        |                                                 |                                                   |
        |  loads CSS/JS/IMG from 8081                     | RestTemplate                                      | Hibernate
        |  (static resources served by frontend)          | (server-to-server, no browser, no CORS)           | (EntityManager, Dialect)
```

The chain has **three hops**:

1. Browser → frontend (HTML over HTTP).
2. Frontend → backend (JSON over HTTP, server-to-server).
3. Backend → MySQL (JDBC, binary protocol).

The browser only ever sees hop 1. It does not know the backend exists. It does not open a socket to port 8080. Every resource the browser requests — HTML, CSS, images — comes from port 8081.

### 2.2 Component diagram (inside each app)

```
---------------- bus-ticket-frontend (8081) ----------------
                                                            
  Browser request                                           
       |                                                    
       v                                                    
  +------------------+                                      
  | ViewController   |   e.g. TripViewController            
  | (@Controller)    |   method: listTrips(Model model)     
  +------------------+                                      
       |                                                    
       v                                                    
  +------------------+                                      
  | *ApiClient       |   e.g. TripApiClient                 
  | (extends         |   method: findAll() -> List<TripDTO> 
  |  AbstractApi     |                                      
  |  Client)         |                                      
  +------------------+                                      
       |                                                    
       v                                                    
  +------------------+                                      
  | RestTemplate     |   (single bean, from RestClientConfig)
  +------------------+                                      
       |                                                    
       v   HTTP GET http://localhost:8080/api/trips         
       v                                                    
-----------------------------------------------------------
                                                            
---------------- bus-ticket-backend (8080) ----------------
                                                            
       |                                                    
       v                                                    
  +------------------+                                      
  | RestController   |   TripRestController                 
  | (@RestController)|   @GetMapping("/api/trips")          
  +------------------+                                      
       |                                                    
       v                                                    
  +------------------+                                      
  | Service          |   TripService (business rules)       
  +------------------+                                      
       |                                                    
       v                                                    
  +------------------+                                      
  | Repository       |   TripRepository extends JpaRepository
  +------------------+                                      
       |                                                    
       v   SELECT ... FROM trip ...                         
       v                                                    
  +------------------+                                      
  | MySQL            |                                      
  +------------------+                                      
```

The frontend's layers stop at the API client. The backend's layers stop at the repository. Neither app knows about the other's internals.

### 2.3 Request lifecycle — user clicks "Search trips"

Let's walk through a concrete user action. The user is on the trip-list page and the URL they hit is `http://localhost:8081/view/trips`.

**Step 1 — Browser sends HTTP request.**

```
GET /view/trips HTTP/1.1
Host: localhost:8081
Accept: text/html
Cookie: JSESSIONID=...
```

The browser knows nothing about port 8080. It just talks to 8081.

**Step 2 — Tomcat (inside frontend) dispatches to `TripViewController`.**

Spring's `DispatcherServlet` matches `/view/trips` to a method annotated `@GetMapping("/view/trips")` in `TripViewController`. The method signature looks like:

```java
@GetMapping
public String listTrips(Model model) {
    List<TripDTO> trips = tripApiClient.findAll();
    model.addAttribute("trips", trips);
    return "trip/list";
}
```

**Step 3 — `TripApiClient.findAll()` is called.**

`TripApiClient` extends `AbstractApiClient`. It builds the URL `http://localhost:8080/api/trips` (base URL from `backend.base-url` property + path) and calls `restTemplate.exchange(...)`.

**Step 4 — `RestTemplate` opens a TCP connection to port 8080.**

Because `backend.connect-timeout-ms=3000`, if the backend is down and the TCP SYN does not get a SYN-ACK within 3 seconds, the client throws. Because `backend.read-timeout-ms=15000`, if the connection is open but the backend takes longer than 15 seconds to send a full response, the client also throws. Both throws are caught by `AbstractApiClient` and wrapped in `BackendException`.

**Step 5 — Backend receives the HTTP call.**

Backend's Tomcat dispatches `GET /api/trips` to `TripRestController.list()`. That method calls `TripService.findAll()`, which calls `TripRepository.findAll()`, which issues a `SELECT * FROM trip` to MySQL. Hibernate maps rows to `Trip` entities. The service converts entities to `TripDTO` (same class name, different package — `com.busticketbookingsystem.dto.TripDTO` on the backend side). The controller returns `List<TripDTO>` and Spring Boot's Jackson auto-configuration serializes it to JSON.

**Step 6 — Backend sends JSON response.**

```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 742

[{"id":1,"source":"Pune","destination":"Mumbai","departureTime":"2026-04-20T09:00:00",...},...]
```

**Step 7 — `RestTemplate` deserializes JSON into `List<TripDTO>` on the frontend.**

Spring uses Jackson + the `ParameterizedTypeReference<List<TripDTO>>` passed in `AbstractApiClient` to reconstruct the list. These are frontend-side DTOs (`com.busfrontend.dto.TripDTO`). The field names must match the JSON exactly.

**Step 8 — Controller puts DTOs into the `Model`.**

`model.addAttribute("trips", trips)` hands the list to Thymeleaf.

**Step 9 — Thymeleaf renders HTML.**

The template `trip/list.html` does `<tr th:each="t : ${trips}">` and produces a full HTML page.

**Step 10 — Tomcat (inside frontend) sends HTML back to the browser.**

```
HTTP/1.1 200 OK
Content-Type: text/html;charset=UTF-8

<!DOCTYPE html>
<html>
<head>...</head>
<body>
  <table>
    <tr><td>1</td><td>Pune</td><td>Mumbai</td>...</tr>
    ...
  </table>
</body>
</html>
```

**Step 11 — Browser renders the page and fires subsequent requests for CSS/JS/images.**

These are also served by port 8081 (from `src/main/resources/static`). None of them touch port 8080.

### 2.4 Why no CORS is needed

CORS (Cross-Origin Resource Sharing) is a **browser** security mechanism. The browser compares the origin of the page (e.g. `http://localhost:8081`) to the origin of a `fetch()` or XHR call. If origins differ and the server did not opt-in with `Access-Control-Allow-Origin`, the browser blocks the response.

In this architecture, **the browser never calls port 8080**. The only HTTP traffic the browser initiates is to port 8081 for HTML, CSS, JS, images. All traffic to 8080 is initiated by `RestTemplate` inside the frontend JVM. `RestTemplate` is not a browser; it doesn't enforce the same-origin policy. It just opens a TCP socket and sends bytes.

Therefore:

- No `@CrossOrigin` annotations are needed on backend controllers.
- No `CorsFilter` or `CorsConfigurationSource` bean is needed.
- The backend can (and should) bind to `127.0.0.1` only in production, with a firewall rule blocking external access — only the frontend machine needs to reach it.

If we later replace Thymeleaf with React, CORS becomes a requirement because the browser will then call `/api/*` directly.

### 2.5 Data shape at each boundary

- **Browser <-> Frontend:** HTML (UTF-8), plus `text/css`, `application/javascript`, `image/png`. Cookies flow here for session state.
- **Frontend <-> Backend:** JSON (UTF-8) over HTTP. Request bodies use `application/json`; response bodies are JSON. No cookies, no sessions — each call is stateless.
- **Backend <-> MySQL:** MySQL binary protocol over TCP (port 3306). Credentials from `spring.datasource.username/password`.

The DTOs on the frontend are **JSON-shape mirrors** of the backend DTOs. They are not entities. They have no `@Entity`, no `@Id`, no JPA annotations.

### 2.6 Monolith vs separated — side-by-side flow

**Monolith flow (before):**

```
Browser  --HTTP-->  [Controller -> Service -> Repository -> MySQL]  --HTTP-->  Browser
                    \_________________ same JVM, port 8080 _________________/
```

One JVM, one port, three layers in-process. Total latency = HTTP + in-process method calls + JDBC.

**Separated flow (after):**

```
Browser --HTTP--> [ViewCtrl -> ApiClient -> RestTemplate] --HTTP--> [RestCtrl -> Service -> Repo -> MySQL] --HTTP--> frontend --HTTP--> Browser
                   \_______ frontend JVM, port 8081 ______/         \______________ backend JVM, port 8080 ______________/
```

Two JVMs, two ports. The extra HTTP hop between frontend and backend is the "cost". The benefit is everything in Section 1.5.

### 2.7 What happens when the backend is down?

A realistic failure mode worth understanding:

1. Browser hits `GET /view/trips` on port 8081.
2. `TripApiClient.findAll()` is called.
3. `RestTemplate` tries to open a TCP connection to `localhost:8080`. Backend is not running, so the OS immediately returns "connection refused" — usually within milliseconds, well before the 3-second connect timeout.
4. `RestTemplate` throws `ResourceAccessException`.
5. `AbstractApiClient` catches it and wraps it as `BackendException("Backend unavailable", cause)`.
6. `TripViewController` does not catch it; Spring's default error handler takes over.
7. Because `server.error.whitelabel.enabled=false`, the request is routed to a template under `templates/error/` (e.g. `error/5xx.html`), which renders a friendly "Service unavailable — please try again" page.

The frontend never crashes. It just shows an error page. This is a genuine benefit of the split: backend outages become graceful degradation, not frontend crashes.

### 2.8 Viva questions — Section 2

1. **Q:** The browser requests `/view/trips`. How many HTTP requests happen to fulfil that single page? Count only the HTML request, not CSS/JS.  
   **A:** Two. Browser → frontend (HTML) and frontend → backend (JSON). The browser only "sees" one.
2. **Q:** Why don't we need to configure CORS?  
   **A:** CORS is enforced by browsers. The frontend-to-backend call is made by `RestTemplate` inside a JVM, not by a browser, so same-origin policy does not apply.
3. **Q:** The backend is down. What does the user see, and why doesn't the frontend crash?  
   **A:** `RestTemplate` throws `ResourceAccessException`, `AbstractApiClient` wraps it as `BackendException`, and Spring Boot's error handling renders the `error/5xx.html` template. The frontend JVM stays alive and keeps serving other pages (e.g. `/members`).
4. **Q:** What is the difference in data format between the three boundaries (browser↔frontend, frontend↔backend, backend↔MySQL)?  
   **A:** HTML, JSON, and MySQL's binary protocol respectively.
5. **Q:** Where does the 3000 ms connect timeout come from and what does it protect us against?  
   **A:** `backend.connect-timeout-ms=3000` in `application.properties`, wired into `RestClientConfig`. It protects the frontend from hanging forever if the backend host is routable but not accepting connections (e.g. network partition, firewall dropping packets silently).

---

## Section 3 — Project Structure Deep Dive

### 3.1 Frontend folder tree (`bus-ticket-frontend`)

This is the actual, complete layout of the frontend project as it exists today:

```
bus-ticket-frontend/
|
+-- pom.xml                                  <-- Maven build file (Section 4)
+-- mvnw                                     <-- Unix Maven wrapper
+-- mvnw.cmd                                 <-- Windows Maven wrapper
|
+-- src/
|   +-- main/
|   |   +-- java/com/busfrontend/
|   |   |   +-- BusFrontendApplication.java          <-- @SpringBootApplication main class
|   |   |   |
|   |   |   +-- config/
|   |   |   |   +-- RestClientConfig.java            <-- Declares the RestTemplate bean
|   |   |   |
|   |   |   +-- controller/                          <-- 12 @Controller classes
|   |   |   |   +-- HomePageController.java          <-- "/" and "/members"
|   |   |   |   +-- AddressViewController.java       <-- /view/addresses/**
|   |   |   |   +-- AgencyViewController.java        <-- /view/agencies/**
|   |   |   |   +-- BookingViewController.java       <-- /view/bookings/**
|   |   |   |   +-- BusViewController.java           <-- /view/buses/**
|   |   |   |   +-- CustomerViewController.java      <-- /view/customers/**
|   |   |   |   +-- DriverViewController.java        <-- /view/drivers/**
|   |   |   |   +-- OfficeViewController.java        <-- /view/offices/**
|   |   |   |   +-- PaymentViewController.java       <-- /view/payments/**
|   |   |   |   +-- ReviewViewController.java        <-- /view/reviews/**
|   |   |   |   +-- RouteViewController.java         <-- /view/routes/**
|   |   |   |   +-- TripViewController.java          <-- /view/trips/**
|   |   |   |
|   |   |   +-- client/                              <-- 11 API clients + 2 shared
|   |   |   |   +-- AbstractApiClient.java           <-- Base class, exchange + error translation
|   |   |   |   +-- BackendException.java            <-- Wraps RestClientException
|   |   |   |   +-- AddressApiClient.java
|   |   |   |   +-- AgencyApiClient.java
|   |   |   |   +-- AgencyOfficeApiClient.java
|   |   |   |   +-- BookingApiClient.java
|   |   |   |   +-- BusApiClient.java
|   |   |   |   +-- CustomerApiClient.java
|   |   |   |   +-- DriverApiClient.java
|   |   |   |   +-- PaymentApiClient.java
|   |   |   |   +-- ReviewApiClient.java
|   |   |   |   +-- RouteApiClient.java
|   |   |   |   +-- TripApiClient.java
|   |   |   |
|   |   |   +-- dto/                                 <-- 23 DTOs + 2 enums
|   |   |       +-- AddressDTO.java
|   |   |       +-- AgencyDTO.java
|   |   |       +-- AgencyOfficeDTO.java
|   |   |       +-- AgencyRequestDTO.java
|   |   |       +-- AgencyResponseDTO.java
|   |   |       +-- BookingRequestDTO.java
|   |   |       +-- BookingResponseDTO.java
|   |   |       +-- BookingStatus.java               <-- enum
|   |   |       +-- BusDTO.java
|   |   |       +-- BusRequestDTO.java
|   |   |       +-- BusResponseDTO.java
|   |   |       +-- CustomerRequestDTO.java
|   |   |       +-- CustomerResponseDTO.java
|   |   |       +-- DriverDTO.java
|   |   |       +-- DriverRequestDTO.java
|   |   |       +-- DriverResponseDTO.java
|   |   |       +-- OfficeRequestDTO.java
|   |   |       +-- OfficeResponseDTO.java
|   |   |       +-- PaymentGroup.java                <-- validation group marker
|   |   |       +-- PaymentRequestDTO.java
|   |   |       +-- PaymentResponseDTO.java
|   |   |       +-- PaymentStatus.java               <-- enum
|   |   |       +-- ReviewDTO.java
|   |   |       +-- RouteDTO.java
|   |   |       +-- TripDTO.java
|   |   |
|   |   +-- resources/
|   |       +-- application.properties               <-- port 8081, backend URL, timeouts
|   |       +-- static/
|   |       |   +-- css/style.css
|   |       |   +-- js/                              <-- empty today; reserved
|   |       |   +-- images/
|   |       |       +-- Anushka.png
|   |       |       +-- Atharv.png
|   |       |       +-- Atharva.jpeg
|   |       |       +-- Kedar.jpg
|   |       |       +-- Sarthak.png
|   |       +-- templates/
|   |           +-- home/               (index, landing page)
|   |           +-- members/            (team page)
|   |           +-- team/               (legacy, kept for redirect compatibility)
|   |           +-- fragments/          (shared header/footer/navbar)
|   |           +-- error/              (4xx.html, 5xx.html, generic error.html)
|   |           +-- address/            (list, form, view)
|   |           +-- agency/
|   |           +-- booking/
|   |           +-- bus/
|   |           +-- customer/
|   |           +-- driver/
|   |           +-- office/
|   |           +-- payment/
|   |           +-- review/
|   |           +-- route/
|   |           +-- trip/
|   |
|   +-- test/
|       +-- java/com/busfrontend/
|       |   +-- client/
|       |   |   +-- AbstractApiClientErrorTranslationTest.java
|       |   |   +-- BookingApiClientTest.java
|       |   |   +-- CustomerApiClientTest.java
|       |   |   +-- PaymentApiClientTest.java
|       |   |   +-- ReviewApiClientTest.java
|       |   |   +-- TripApiClientTest.java
|       |   +-- controller/
|       |       +-- BookingViewControllerTest.java
|       |       +-- HomePageControllerTest.java
|       |       +-- PaymentViewControllerTest.java
|       |       +-- ReviewViewControllerTest.java
|       |       +-- TripViewControllerTest.java
|       +-- resources/
|           +-- application.properties           <-- test overrides
```

**File counts (authoritative for viva):**

- **52 Java files** in `src/main/java`: 1 main + 1 config + 12 controllers + 13 client classes (11 API clients + `AbstractApiClient` + `BackendException`) + 25 DTO files (23 DTO classes + 2 enums).
- **47 Thymeleaf templates** under `src/main/resources/templates` (across domain subfolders + home/members/team/fragments/error).
- **6 static assets** under `src/main/resources/static` (1 CSS + 5 images; `js/` is empty).
- **~50 test files** across `client` and `controller` packages (the on-disk count is 11 today; the full plan including controller + client coverage targets ~50).

### 3.2 Purpose of each frontend directory

- **`com.busfrontend`** (root package): chosen deliberately to differ from `com.busticketbookingsystem` (backend). Prevents any accidental dependency leak — if you import a backend class, the compile will fail because the package isn't on the classpath.
- **`BusFrontendApplication`**: the `main` method. Starts Spring Boot.
- **`config/`**: infrastructure beans only. Today it holds `RestClientConfig`. Later: `MessageSource`, `LocaleResolver`, `WebMvcConfigurer` for static-resource handling, etc.
- **`controller/`**: all `@Controller` (not `@RestController`) classes. Each maps a URL prefix under `/view/*` (except `HomePageController` which handles `/` and `/members`). Every method returns a view name (a `String`) or a `RedirectView`.
- **`client/`**: the HTTP layer. One class per backend resource. Every client extends `AbstractApiClient` which owns the `RestTemplate` + error translation. `BackendException` is a `RuntimeException` thrown on any backend failure.
- **`dto/`**: plain data carriers. No JPA, no `@Entity`. Field types and names mirror the backend JSON response contract exactly. Many resources have separate `RequestDTO` / `ResponseDTO` pairs (different fields — e.g. `password` is present on `CustomerRequestDTO` but not on `CustomerResponseDTO`).
- **`resources/application.properties`**: runtime config (port, backend URL, timeouts, Thymeleaf cache off).
- **`resources/static/`**: served verbatim by Spring Boot at `/css/**`, `/js/**`, `/images/**`.
- **`resources/templates/`**: Thymeleaf HTML files. One subfolder per domain. `fragments/` holds reusable pieces (navbar, footer) included via `th:replace`.
- **`test/java/com/busfrontend/client/`**: tests that use `MockRestServiceServer` to simulate backend JSON responses without spinning up a second Spring Boot.
- **`test/java/com/busfrontend/controller/`**: `@WebMvcTest` tests that mock the relevant `*ApiClient` and assert rendered view names / model attributes.

### 3.3 Backend folder tree (`bus-ticket-booking-system`)

The backend keeps most of what the monolith had — with three deletions and one narrowing:

```
bus-ticket-booking-system/
|
+-- pom.xml                                  (unchanged: JPA, MySQL, iText, validation, web)
+-- src/main/java/com/busticketbookingsystem/
|   +-- BusTicketBookingSystemApplication.java
|   +-- entity/        (Customer, Trip, Booking, Payment, Route, ...)
|   +-- repository/    (extends JpaRepository<..., Long>)
|   +-- service/       (business logic, @Service)
|   +-- controller/
|   |   +-- rest/      <-- KEEP: @RestController, mapped to /api/**
|   |   +-- view/      <-- REMOVE LATER: @Controller, mapped to /view/** (now lives in frontend)
|   +-- dto/           (request/response DTOs — still used by REST controllers)
|   +-- config/        (DataSourceConfig, possibly security)
|   +-- exception/     (GlobalExceptionHandler for REST error responses)
|   +-- util/          (PdfInvoiceGenerator — iText)
|
+-- src/main/resources/
|   +-- application.properties       (MySQL URL, dialect, JPA ddl-auto)
|   +-- templates/                   <-- REMOVE LATER (moved to frontend)
|   +-- static/                      <-- REMOVE LATER (moved to frontend)
|
+-- src/test/java/com/busticketbookingsystem/
    +-- @DataJpaTest / @WebMvcTest / @SpringBootTest
```

**What needs to be removed from the backend (cleanup task, tracked separately):**

1. All `@Controller` (view) classes under `controller/view/` — they are now `@Controller` classes inside the frontend under `com.busfrontend.controller`.
2. Everything under `src/main/resources/templates/` — moved to the frontend.
3. Everything under `src/main/resources/static/` — moved to the frontend.
4. The `spring-boot-starter-thymeleaf` dependency in `pom.xml` — no longer needed once the templates are gone.

**What stays on the backend:**

- Entities, repositories, services.
- `@RestController` classes under `controller/rest/`.
- DTOs used in request/response bodies of the REST endpoints.
- `config/` (datasource, etc.).
- `exception/GlobalExceptionHandler` — critical: it produces the JSON error bodies that the frontend's `AbstractApiClient` consumes.
- `util/PdfInvoiceGenerator` (iText) — invoice PDFs are generated on the backend and streamed through the frontend as a pass-through binary response.

### 3.4 Package naming: why `com.busfrontend.*` vs `com.busticketbookingsystem.*`

Three reasons:

1. **Compile-time isolation.** If a developer accidentally writes `import com.busticketbookingsystem.entity.Trip;` in a frontend class, Maven fails the build because that package isn't on the frontend classpath. Different package roots turn a runtime class-not-found into a compile-time error.
2. **Log clarity.** Stack traces show package names. When a bug report shows `at com.busfrontend.controller.TripViewController.listTrips` we know immediately which JVM produced the log.
3. **Artifact identity.** Maven `groupId:artifactId` are `com.busfrontend:bus-ticket-frontend` and `com.busticketbookingsystem:bus-ticket-booking-system`. The two JARs cannot collide in a local `.m2` cache or in Nexus.

### 3.5 Why DTOs live in `com.busfrontend.dto` and not in a shared module

Considered and rejected. A `bus-ticket-shared-dto` Maven module would:

- Force both apps to rebuild when a shared DTO changes.
- Re-introduce tight coupling (bump the backend's DTO = bump the frontend's dependency = deploy both).
- Complicate the Maven build for a 5-person college project.

Duplication is the simpler trade. When a field is added, the developer updates the DTO on both sides. Any mismatch manifests as a Jackson deserialization warning (unknown property) which we have configured to be lenient — extra fields are ignored, missing fields become `null`.

### 3.6 Viva questions — Section 3

1. **Q:** How many Java source files in `src/main/java` of the frontend, and what's the breakdown?  
   **A:** 52 total. 1 main class + 1 config + 12 controllers + 13 client classes + 25 DTO-package files (23 DTOs + 2 enums).
2. **Q:** Why is the package `com.busfrontend` rather than `com.busticketbookingsystem.frontend`?  
   **A:** To make it impossible to accidentally import a backend class — the backend's root package is not on the frontend classpath, so such an import would be a compile error.
3. **Q:** Name three things still on the backend that need to be removed as part of the cleanup.  
   **A:** View controllers under `controller/view/`, Thymeleaf templates, static resources, and the `spring-boot-starter-thymeleaf` dependency in `pom.xml`.
4. **Q:** Why are there separate `RequestDTO` and `ResponseDTO` classes for some resources (e.g. `CustomerRequestDTO` and `CustomerResponseDTO`)?  
   **A:** They have different field sets. The request version may carry a password or raw inputs; the response version omits sensitive fields and adds server-generated ones like `id` or `createdAt`.
5. **Q:** Why not a shared DTO Maven module?  
   **A:** It re-couples the two projects' release cycles and adds build complexity. Duplicated DTOs are cheaper for a small team.

---

## Section 4 — Configuration Deep Dive

### 4.1 The full `pom.xml`

Here is the actual `pom.xml` of the frontend, verbatim:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.busfrontend</groupId>
    <artifactId>bus-ticket-frontend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>bus-ticket-frontend</name>
    <description>Thymeleaf frontend for Bus Ticket Booking System (calls REST backend)</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 4.2 Every dependency explained

**`spring-boot-starter-parent` 3.3.5** — the parent POM. Manages versions of all Spring Boot starters, Jackson, Tomcat, Hibernate Validator, Lombok, JUnit 5, etc. We do not specify versions on individual starters because the parent does it for us (BOM effect).

**`<java.version>21</java.version>`** — compiles with Java 21. Virtual threads available, records available, pattern matching for switch available. The backend uses the same version for consistency.

**`spring-boot-starter-web`** — brings in:
- Spring MVC (`DispatcherServlet`, `@Controller`, `@GetMapping`, `Model`).
- Embedded Tomcat (so `java -jar` works without an external server).
- Jackson (`ObjectMapper`, used by `RestTemplate` to serialize/deserialize JSON).
- `RestTemplateBuilder` and `RestTemplate` classes.

This starter is the reason we can both receive HTTP requests (as an MVC app) and send HTTP requests (as a REST client).

**`spring-boot-starter-thymeleaf`** — brings in:
- `thymeleaf` and `thymeleaf-spring6` libraries.
- Auto-configures `SpringTemplateEngine`, `ThymeleafViewResolver`.
- Default template location: `classpath:/templates/`, default suffix: `.html`.

Because this starter is present, returning the string `"trip/list"` from a `@Controller` method resolves to `src/main/resources/templates/trip/list.html`.

**`spring-boot-starter-validation`** — brings in Hibernate Validator (the JSR-380 reference implementation). Required because our `RequestDTO` classes use `@NotNull`, `@Email`, `@Size`, `@Min`, `@Max`, `@Pattern`, `@Valid`. Without this starter, those annotations are silently ignored.

We validate on the frontend for UX reasons (show the user their error immediately instead of round-tripping to the backend). The backend also validates — defense in depth — but the frontend's first-line validation means most bad requests never leave the frontend JVM.

**`org.projectlombok:lombok` (optional=true)** — brings in Lombok annotations at compile time. Generates getters/setters/constructors/`@Data`/`@Builder`/`@Slf4j`. Marked `optional` so Lombok is not transitively pulled in by downstream consumers (not that we have any — we don't publish this JAR — but it's idiomatic).

Important: merely adding Lombok as a dependency isn't enough. The compiler must also see it as an *annotation processor*. That's what the `maven-compiler-plugin` block at the bottom does (see Section 4.3).

**`spring-boot-devtools` (runtime, optional)** — adds:
- LiveReload server (notifies the browser when classes change).
- Automatic restart when a classpath file changes.
- Helpful default property overrides (e.g. template caching off).

`scope=runtime` means it's not on the compile classpath — your production code cannot accidentally reference a DevTools class. `optional=true` prevents it leaking to downstream builds. When you build a production JAR via `mvn package -DskipTests`, DevTools is packaged but auto-disables itself outside IDE-like launch contexts.

**`spring-boot-starter-test` (test)** — brings in:
- JUnit Jupiter (JUnit 5).
- Mockito + Mockito-JUnit extension.
- AssertJ (fluent assertions).
- Spring's `MockMvc`, `MockRestServiceServer`, `@WebMvcTest`, `@SpringBootTest`.

`scope=test` means it's not on the main classpath. The production JAR does not contain JUnit.

### 4.3 The Lombok annotation processor block — why it exists

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
        <source>21</source>
        <target>21</target>
    </configuration>
</plugin>
```

With Java 21 and the newer Maven compiler plugin, annotation processors are *not* picked up automatically from the main classpath. They must be listed explicitly in `annotationProcessorPaths`. If you skip this block:

- The code compiles (because Lombok is on the classpath).
- But `@Data`, `@Getter`, `@Builder` generate no methods.
- Then calls like `trip.getSource()` fail compilation because `getSource()` doesn't exist.

Including the block tells `javac` to run Lombok during compilation so the synthetic methods are produced. `<source>21</source>` and `<target>21</target>` are also set here to be explicit (the parent POM's `java.version` property usually covers it, but being explicit makes IDE behaviour predictable).

The `spring-boot-maven-plugin` handles `mvn spring-boot:run` and `mvn package` (repackaging into an executable JAR with a `BOOT-INF` layout).

### 4.4 The full `application.properties`

```properties
spring.application.name=bus-ticket-frontend
server.port=8081

# Backend REST API base URL — change for prod
backend.base-url=http://localhost:8080

# RestTemplate timeouts (ms)
backend.connect-timeout-ms=3000
backend.read-timeout-ms=15000

# Thymeleaf: no cache in dev (so template edits reload without restart)
spring.thymeleaf.cache=false

# Disable whitelabel — we have our own error templates
server.error.whitelabel.enabled=false
```

### 4.5 Every property explained

**`spring.application.name=bus-ticket-frontend`** — the logical name of this app. Shows up in log output, actuator endpoints, and (if we later add Spring Cloud) in service discovery. Distinguishes our logs from the backend's in any centralized log aggregator.

**`server.port=8081`** — the TCP port the embedded Tomcat binds to. Chosen because 8080 is taken by the backend on the same dev laptop. In production these will almost always be on different hosts, so the port collision doesn't apply — but we keep the asymmetry to prevent confusion during development.

**`backend.base-url=http://localhost:8080`** — the base URL every `*ApiClient` prepends to its paths. `AbstractApiClient` reads this via `@Value("${backend.base-url}")`. Changing this value in prod (e.g. `http://backend.internal:8080` or `https://api.busbooking.com`) redirects all frontend-to-backend traffic without a code change.

**`backend.connect-timeout-ms=3000`** — how long `RestTemplate` waits for the TCP handshake. Short because if the backend is unreachable we want to fail fast and show an error page rather than hang the user's browser tab. 3 seconds is enough for any realistic LAN hop including DNS.

**`backend.read-timeout-ms=15000`** — how long to wait for a complete response *after* the connection is established. Set generously (15 s) because some backend operations (PDF generation for invoices via iText, reports, heavy joins) can take several seconds. If a request legitimately takes >15 s, we'd rather error than hold the user.

These two timeouts are deliberately different. See Section 4.7 for the reasoning.

**`spring.thymeleaf.cache=false`** — disables template caching. Without this, Thymeleaf reads each template once and caches the compiled output forever, so edits to `.html` files don't show up until JVM restart. With `false`, each request re-parses the template — slower, but exactly what you want in development.

In production, flip this to `true` (via a prod-profile properties file or env var). A cached template is an order of magnitude faster to render.

**`server.error.whitelabel.enabled=false`** — turns off Spring Boot's built-in generic error page ("Whitelabel Error Page — There was an unexpected error"). With it off, Spring falls back to looking for custom templates under `templates/error/` — matching `4xx.html`, `5xx.html`, or specific codes like `404.html`. We have those. Keeping whitelabel on would sometimes win over our templates and show users a framework-default page that reveals the Spring Boot version. Bad for UX and for security (information disclosure).

### 4.6 `RestClientConfig.java` — full code

```java
package com.busfrontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${backend.connect-timeout-ms:3000}")
    private long connectTimeoutMs;

    @Value("${backend.read-timeout-ms:15000}")
    private long readTimeoutMs;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
```

### 4.7 `RestClientConfig` — line by line

**`@Configuration`** — marks this class as a source of bean definitions. Spring scans it at startup, looks for `@Bean` methods, and registers their return values in the application context. Combined with the root `@SpringBootApplication` (which enables component scanning for `com.busfrontend`), this class is auto-discovered.

Why `@Configuration` and not `@Component`? Both are picked up by component scanning, but `@Configuration` has a critical extra behaviour: it wraps the class in a CGLIB proxy so that calling `@Bean`-annotated methods from within the class returns the same singleton instance instead of executing the method afresh. In this file we only have one `@Bean` method so the distinction is moot — but `@Configuration` is the idiomatic choice for bean-producing classes.

**`@Value("${backend.connect-timeout-ms:3000}")`** — injects the property. The `:3000` after the colon is a *default*; if `backend.connect-timeout-ms` is missing from `application.properties`, Spring uses 3000. That default matches our explicit property value, belt-and-suspenders style. This means the file can be run in a scratch environment without `application.properties` and still boot.

Same pattern for `readTimeoutMs` with default 15000.

**`@Bean public RestTemplate restTemplate(RestTemplateBuilder builder)`** — declares a bean. The method name `restTemplate` becomes the bean name. Spring automatically injects a `RestTemplateBuilder` instance as the method argument because Spring Boot auto-configures one.

Why take a builder instead of `new RestTemplate()` directly? Because `RestTemplateBuilder` has default customizers applied by Spring Boot — e.g. any `RestTemplateCustomizer` bean in the context (for adding interceptors, message converters, SSL config). Using the builder respects those customizations; `new RestTemplate()` bypasses them.

**`.setConnectTimeout(Duration.ofMillis(connectTimeoutMs))`** — sets the TCP handshake timeout. Underneath, on the default `SimpleClientHttpRequestFactory`, this becomes `HttpURLConnection.setConnectTimeout(int ms)`. Beyond this duration the client throws `ConnectException` / `SocketTimeoutException`.

**`.setReadTimeout(Duration.ofMillis(readTimeoutMs))`** — sets how long to wait for each chunk of response data. If the backend accepts the connection but then stalls (e.g. blocked on a slow SQL query), the client throws after this duration.

### 4.8 Why separate timeouts for connect vs read

Connect and read failures represent *different* problems and deserve different tolerances:

- **Connect timeout** is about host reachability. If the backend host is unreachable (wrong hostname, network partition, firewall drop), no response is ever coming. We want to fail *fast* so the user gets an error page quickly. 3 seconds is plenty.
- **Read timeout** is about backend processing speed. The connection is up, the backend received the request, and we are now waiting for it to finish. That can legitimately take several seconds for operations like "generate monthly booking report PDF" or "recompute seat availability". 15 seconds gives headroom.

If we used a single short timeout for both, legitimate slow operations would time out. If we used a single long timeout for both, connection problems would take 15+ seconds to surface, feeling like the app is hung.

### 4.9 Why `@Bean` not `@Component` for `RestTemplate`

`RestTemplate` is a class from Spring — we don't own its source. Putting `@Component` on it is impossible. The two patterns for registering third-party classes as beans are:

1. Declare a `@Bean` method in a `@Configuration` class (what we do).
2. Register a `BeanDefinition` programmatically (advanced, rare).

Option 1 is the normal, readable choice. It also gives us a clear place to configure timeouts, interceptors, and message converters.

### 4.10 `BusFrontendApplication.java` — full code and explanation

```java
package com.busfrontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BusFrontendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusFrontendApplication.class, args);
    }
}
```

Eleven lines; each one matters.

**`package com.busfrontend;`** — the root package. `@SpringBootApplication` triggers component scanning starting from *this* package, so every `@Controller`, `@Service`, `@Component`, `@Configuration`, and `@RestController` under `com.busfrontend.*` is auto-registered. If you moved this class to `com.acme`, none of our sub-packages would be scanned.

**`@SpringBootApplication`** — a composite of three annotations:
- `@SpringBootConfiguration` — marks this as the primary configuration class.
- `@EnableAutoConfiguration` — triggers Spring Boot's auto-configuration based on classpath presence. Because `spring-boot-starter-web` is on the classpath, it auto-configures `DispatcherServlet`, Tomcat, Jackson, etc. Because `spring-boot-starter-thymeleaf` is on the classpath, it auto-configures `SpringTemplateEngine` and `ThymeleafViewResolver`.
- `@ComponentScan` — scans the current package and subpackages for Spring-managed components.

Without `@EnableAutoConfiguration`, we'd have to write all of those beans by hand.

**`SpringApplication.run(...)`** — boots the context. Steps it performs (simplified):
1. Load `application.properties` (and profile-specific overrides).
2. Create an `ApplicationContext`.
3. Register all scanned beans.
4. Apply auto-configuration.
5. Start the embedded Tomcat on `server.port`.
6. Log "Started BusFrontendApplication in x.xxx seconds".

Once this call returns (actually, it blocks while Tomcat serves requests), the app is live on port 8081.

### 4.11 Environment-specific configuration (how to override in prod)

`application.properties` is committed to git and holds *development* defaults. In production we override values without editing the file. Several mechanisms, in precedence order (later overrides earlier):

**1. Environment variables (highest precedence, the recommended production approach).**

Spring Boot maps `BACKEND_BASE_URL` → `backend.base-url` automatically. On Linux:

```bash
export BACKEND_BASE_URL=http://backend.internal.prod:8080
export SERVER_PORT=80
export SPRING_THYMELEAF_CACHE=true
java -jar bus-ticket-frontend.jar
```

**2. Command-line arguments.**

```bash
java -jar bus-ticket-frontend.jar --backend.base-url=http://backend.internal.prod:8080 --server.port=80
```

**3. JVM system properties (`-D`).**

```bash
java -Dbackend.base-url=http://backend.internal.prod:8080 -Dserver.port=80 -jar bus-ticket-frontend.jar
```

**4. Profile-specific properties files.**

Create `src/main/resources/application-prod.properties` with prod values:

```properties
spring.thymeleaf.cache=true
backend.base-url=http://backend.internal.prod:8080
server.port=80
```

Activate the profile:

```bash
java -jar bus-ticket-frontend.jar --spring.profiles.active=prod
```

Or via env var: `SPRING_PROFILES_ACTIVE=prod`.

**5. External `application.properties` file next to the JAR.**

If a file named `application.properties` sits in the working directory when the JAR is launched, Spring Boot reads it and its values override the one embedded in the JAR. This is useful for ops teams who want to tweak config without rebuilding.

**Typical production settings (summary):**

```properties
spring.thymeleaf.cache=true
backend.base-url=http://10.0.1.50:8080
backend.connect-timeout-ms=2000
backend.read-timeout-ms=20000
server.port=80
logging.level.root=INFO
logging.level.com.busfrontend=INFO
```

**Never** commit real production values to git. Use env vars or a secrets manager (Vault, AWS Parameter Store).

### 4.12 Viva questions — Section 4

1. **Q:** Why does the frontend need `spring-boot-starter-validation` if the backend also validates?  
   **A:** To validate on the server-side of the frontend *before* making the HTTP call to the backend. The user sees field errors immediately, the backend is not contacted with known-invalid data, and the backend still re-validates (defense in depth) so a misbehaving client cannot bypass the rules.
2. **Q:** Why is Lombok listed *twice* in the POM — once as a dependency and once in `annotationProcessorPaths`?  
   **A:** Dependencies put the annotations on the classpath at compile and runtime. The annotation processor path tells `javac` to actually run Lombok's code generator. In Java 9+/21, processors are no longer auto-discovered from the main classpath, so both entries are required.
3. **Q:** Why are connect and read timeouts different values?  
   **A:** They cover different failure modes. Connect measures host reachability (fail fast, 3 s); read measures backend processing time (allow legitimate slow operations, 15 s).
4. **Q:** What does `spring.thymeleaf.cache=false` buy us, and why must it be `true` in prod?  
   **A:** In dev: live reload of template edits without restart. In prod: cached, pre-compiled templates render significantly faster, and templates never change between deploys anyway.
5. **Q:** If you wanted to point the frontend at a staging backend without rebuilding the JAR, how would you do it?  
   **A:** Set an environment variable (`BACKEND_BASE_URL=http://staging-backend:8080`) or pass a command-line argument (`--backend.base-url=...`). Spring Boot's property resolution picks up env vars and CLI args automatically at startup.

---

## End of Part A

This part covered the "why" and the "wiring up": the motivation for splitting, the request flow, the folder layout, and every line of configuration. Part B continues with the code itself — the API client layer (`AbstractApiClient`, `BackendException`, the 11 concrete clients), the view controllers, the DTO contracts, the Thymeleaf templates, the test strategy, and finally packaging / deployment.
# PART B — DTOs, CLIENTS, CONTROLLERS

This is Part 2 of the bus-ticket frontend deep dive. Part 1 covered the project shell, `pom.xml`, `application.properties`, `BusTicketFrontendApplication`, the `RestTemplate` bean, and templates overview. Part 2 drills into the three layers that actually move data around:

1. **DTO layer** — plain Java objects that mirror request/response JSON shapes.
2. **API Client layer** — thin wrappers over `RestTemplate` that call the Spring Boot backend.
3. **View Controller layer** — Thymeleaf-facing controllers that glue forms to API clients.

Read this top-to-bottom once, then keep it open as a reference while you wire up templates.

---

## Section 5 — DTOs Reference

DTOs (Data Transfer Objects) live under:

```
src/main/java/com/busfrontend/dto/
```

There are 23 DTO classes + 2 enums. Every single one uses Lombok to eliminate boilerplate:

| Annotation | What it generates |
|------------|-------------------|
| `@Data` | Getters, setters, `equals`, `hashCode`, `toString` |
| `@NoArgsConstructor` | Empty constructor (required by Jackson for JSON deserialisation) |
| `@AllArgsConstructor` | Full-field constructor |
| `@Builder` | Fluent builder (`SomeDTO.builder().foo(x).build()`) |

Jakarta Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Pattern`, `@Min`, `@Max`, `@Email`, `@Positive`, `@DecimalMin`) only fire when the backend controller uses `@Valid`. The frontend forwards DTOs as-is and lets the backend validate — but we still carry the annotations to stay symmetric with the backend.

### 5.1 Trip & Route DTOs

#### TripDTO.java

**Path:** `src/main/java/com/busfrontend/dto/TripDTO.java`

**Role:** Dual-purpose — used both as request (POST/PUT to `/api/trips`) and response (GET from `/api/trips`).

**Fields:**

| Field | Type | Purpose |
|-------|------|---------|
| `tripId` | `Integer` | Primary key, null on create |
| `routeId` | `Integer` | FK to Route |
| `busId` | `Integer` | FK to Bus |
| `boardingAddressId` | `Integer` | FK to Address (pickup point) |
| `droppingAddressId` | `Integer` | FK to Address (drop point) |
| `departureTime` | `LocalDateTime` | Scheduled departure |
| `arrivalTime` | `LocalDateTime` | Scheduled arrival |
| `driver1Id` | `Integer` | Primary driver FK |
| `driver2Id` | `Integer` | Optional co-driver FK |
| `availableSeats` | `Integer` | Decremented on each booking |
| `fare` | `BigDecimal` | Per-seat fare (money → always BigDecimal) |
| `tripDate` | `LocalDateTime` | Service date |
| `fromCity` | `String` | Display-only (enriched by backend JOIN) |
| `toCity` | `String` | Display-only |
| `busType` | `String` | Display-only |
| `registrationNumber` | `String` | Display-only |
| `boardingAddress` | `String` | Display-only |
| `droppingAddress` | `String` | Display-only |

**Why the display fields?** The backend returns them pre-joined so Thymeleaf doesn't need extra round-trips to show "Pune → Mumbai" instead of "Route #7". On the way in (create/update), these are usually null.

**Code:**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDTO {

    private Integer tripId;
    private Integer routeId;
    private Integer busId;
    private Integer boardingAddressId;
    private Integer droppingAddressId;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer driver1Id;
    private Integer driver2Id;
    private Integer availableSeats;
    private BigDecimal fare;
    private LocalDateTime tripDate;

    // Display fields
    private String fromCity;
    private String toCity;
    private String busType;
    private String registrationNumber;
    private String boardingAddress;
    private String droppingAddress;
}
```

**Example response JSON** from `GET /api/trips/42`:

```json
{
  "tripId": 42,
  "routeId": 7,
  "busId": 3,
  "boardingAddressId": 11,
  "droppingAddressId": 12,
  "departureTime": "2026-05-01T08:30:00",
  "arrivalTime":   "2026-05-01T14:00:00",
  "driver1Id": 5,
  "driver2Id": null,
  "availableSeats": 38,
  "fare": 650.00,
  "tripDate": "2026-05-01T00:00:00",
  "fromCity": "Pune",
  "toCity": "Mumbai",
  "busType": "AC Sleeper",
  "registrationNumber": "MH-12-AB-1234",
  "boardingAddress": "Shivajinagar Bus Stand",
  "droppingAddress": "Dadar East Terminus"
}
```

#### RouteDTO.java

**Path:** `src/main/java/com/busfrontend/dto/RouteDTO.java`

**Role:** Dual-purpose (request + response).

| Field | Type | Validation |
|-------|------|------------|
| `routeId` | `Integer` | — |
| `fromCity` | `String` | `@NotBlank "From city is required"` |
| `toCity` | `String` | `@NotBlank "To city is required"` |
| `breakPoints` | `Integer` | — |
| `duration` | `Integer` | `@Min(1) "Duration must be at least 1 minute"` |

Used by `RouteApiClient` — endpoints `/api/routes`.

**Example JSON:**

```json
{ "routeId": 7, "fromCity": "Pune", "toCity": "Mumbai", "breakPoints": 2, "duration": 210 }
```

---

### 5.2 Booking DTOs

#### BookingRequestDTO.java

**Role:** Client → server shape for POST `/api/bookings`.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {

    @NotNull(message = "Trip ID cannot be null")
    private Integer tripId;

    @NotEmpty(message = "Seat numbers cannot be empty")
    private List<Integer> seatNumbers;

    @NotNull(message = "Customer ID cannot be null")
    private Integer customerId;
}
```

**Why `List<Integer> seatNumbers`?** One HTTP call can reserve multiple seats — the backend fans out into N Booking rows atomically, and returns N booking IDs plus the summed fare.

**Example JSON:**

```json
{ "tripId": 42, "seatNumbers": [5, 6, 7], "customerId": 18 }
```

#### BookingResponseDTO.java

**Role:** Server → client shape returned by POST `/api/bookings`.

| Field | Type | Meaning |
|-------|------|---------|
| `message` | `String` | Human text, e.g. `"3 seats booked successfully"` |
| `bookingIds` | `List<Integer>` | One ID per seat booked |
| `totalFare` | `BigDecimal` | seatCount × perSeatFare |
| `customerId` | `Integer` | Echoed back for convenience |

**Example JSON:**

```json
{
  "message": "3 seats booked successfully",
  "bookingIds": [101, 102, 103],
  "totalFare": 1950.00,
  "customerId": 18
}
```

#### BookingStatus.java (enum)

```java
@SuppressWarnings("java:S115")
public enum BookingStatus {
    Available,
    Booked
}
```

The `@SuppressWarnings("java:S115")` silences SonarLint's "enum constants should be UPPER_CASE". They're mixed-case here because the backend serialises them that way in JSON (`"status": "Booked"`). If you changed them to `AVAILABLE`/`BOOKED`, Jackson would fail to match string values and everything would NPE.

#### Backend endpoints that use these DTOs

| Endpoint | Method | Accepts | Returns |
|----------|--------|---------|---------|
| `/api/bookings` | POST | `BookingRequestDTO` | `BookingResponseDTO` |
| `/api/bookings/{id}` | GET | — | `Map<String,Object>` (raw) |
| `/api/bookings/trip/{tripId}` | GET | — | `List<Map<String,Object>>` |
| `/api/bookings/{id}/ticket` | GET | — | `application/pdf` |
| `/api/bookings/group-ticket?bookingIds=1,2,3` | GET | — | `application/pdf` |

**Why `Map` for single-booking GET?** The backend returns a flat projection (`bookingId`, `tripId`, `seatNumber`, `status`) that doesn't perfectly align with any one DTO — we treat it as a generic `Map` to avoid a throwaway `BookingViewDTO`.

---

### 5.3 Payment DTOs

#### PaymentRequestDTO.java

**Role:** Client → server for POST `/api/payments`.

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

    @NotNull(message = "Booking ID is required")
    @Positive(message = "Booking ID must be positive")
    private Integer bookingId;

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    private Integer customerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
}
```

Notice this is NOT `@Builder` — we construct it by `new PaymentRequestDTO(bid, cid, amount)` inside `PaymentViewController`. That's a style choice only; either works.

**Example JSON:**

```json
{ "bookingId": 101, "customerId": 18, "amount": 650.00 }
```

#### PaymentResponseDTO.java

**Role:** Server → client.

| Field | Type |
|-------|------|
| `paymentId` | `Integer` |
| `bookingId` | `Integer` |
| `customerId` | `Integer` |
| `amount` | `BigDecimal` |
| `paymentStatus` | `PaymentStatus` |
| `paymentDate` | `LocalDateTime` |
| `message` | `String` |
| `hasValidBooking` | `boolean` |

`hasValidBooking` is used by templates to grey-out the "Download Ticket" button when the underlying booking got cancelled.

**Example JSON:**

```json
{
  "paymentId": 501,
  "bookingId": 101,
  "customerId": 18,
  "amount": 650.00,
  "paymentStatus": "Success",
  "paymentDate": "2026-04-19T12:03:45",
  "message": "Payment successful",
  "hasValidBooking": true
}
```

#### PaymentStatus.java (enum)

```java
public enum PaymentStatus {
    Success,
    Failed
}
```

Same mixed-case rationale as `BookingStatus`.

#### PaymentGroup.java

**Path:** `src/main/java/com/busfrontend/dto/PaymentGroup.java`

**Role:** Pure **view model** — never touches the wire. The Payments list page calls `paymentApiClient.getAll()`, gets back N individual `PaymentResponseDTO` rows, then `PaymentViewController.groupRelatedPayments(...)` collapses rows that share `customerId + paymentDate(second precision) + status` into one `PaymentGroup`. That way a 3-seat booking shows as **one** row with a single "Download Group Ticket" button instead of three separate ticket buttons.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGroup {
    private List<Integer> paymentIds;
    private List<Integer> bookingIds;
    private Integer customerId;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private int seatCount;

    public Integer getFirstPaymentId() {
        return paymentIds == null || paymentIds.isEmpty() ? null : paymentIds.get(0);
    }

    public boolean isGroup() {
        return paymentIds != null && paymentIds.size() > 1;
    }
}
```

The convenience getters `getFirstPaymentId()` and `isGroup()` are what Thymeleaf calls — `th:if="${group.isGroup()}"`.

---

### 5.4 Review DTO

#### ReviewDTO.java

**Role:** Request shape for POST `/api/reviews`; also used as display model (mapped from the backend's `Map<String,Object>`).

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {

    private Integer reviewId;

    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotNull(message = "Trip ID is required")
    private Integer tripId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    private String comment;

    private LocalDateTime reviewDate;
}
```

The `@Min(1)` / `@Max(5)` enforce a 1–5 star rating on the backend when `@Valid` runs. The frontend's `<select>` already constrains the UI to 1–5, so these are belt-and-braces.

**Example JSON:**

```json
{ "customerId": 18, "tripId": 42, "rating": 5, "comment": "Smooth ride, on-time arrival." }
```

---

### 5.5 Customer + Address DTOs

#### CustomerRequestDTO.java

```java
@Data
public class CustomerRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Invalid Email")
    private String email;

    @Pattern(regexp ="^\\d{10}$", message = "Phone must be 10 digit")
    private String phone;

    @NotNull
    private Integer addressId;
}
```

**Key detail — `addressId`, not an embedded Address.** Customers point at an existing Address row (created separately). `CustomerViewController.saveCustomer` does this in two steps: it first POSTs an address, gets `addressId` back, then POSTs the customer referencing that id. This keeps the Customer endpoint clean and avoids partial writes if the address is invalid.

#### CustomerResponseDTO.java

| Field | Type |
|-------|------|
| `id` | `Integer` |
| `name` | `String` |
| `email` | `String` |
| `phone` | `String` |
| `city` | `String` |

`city` is display-only, flattened from the linked Address. This is why the backend returns `city` but accepts `addressId` on input — different shapes for different needs.

#### AddressDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {

    private Integer addressId;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Zip code must be 6 digits")
    private String zipCode;
}
```

Single shape used for both request and response. Backend endpoints: `/api/addresses`, `/api/addresses/{id}`.

**Example JSON:**

```json
{ "addressId": 11, "address": "221B Baker Street", "city": "Pune", "state": "MH", "zipCode": "411001" }
```

---

### 5.6 Agency DTOs

There are **three** agency shapes — a legacy `AgencyDTO` (used by form binding in some templates) plus the clean Request/Response pair.

#### AgencyDTO.java

Catch-all combined shape (`agencyId` + all fields + all validations). Exists historically; prefer Request/Response in new code.

#### AgencyRequestDTO.java

```java
@Data
public class AgencyRequestDTO {

    @NotBlank(message = "Agency name is required")
    private String name;

    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone must be exactly 10 digits")
    private String phone;
}
```

No `agencyId` — the path variable carries it on update (`PUT /api/agencies/{id}`).

#### AgencyResponseDTO.java

Same fields plus `agencyId`. No validation annotations (responses don't need them).

#### AgencyOfficeDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyOfficeDTO {

    private Integer officeId;

    @NotNull(message = "Agency ID is required")
    private Integer agencyId;

    @Email(message = "Invalid email format")
    private String officeMail;

    @NotBlank(message = "Office contact person name is required")
    private String officeContactPersonName;

    @Pattern(regexp = "^\\d{10}$", message = "Phone must be 10 digits")
    private String officeContactNumber;

    @NotNull(message = "Office address ID is required")
    private Integer officeAddressId;

    // Display fields
    private String agencyName;
    private String officeCity;
}
```

Display fields (`agencyName`, `officeCity`) are backend-joined for the offices list page. This DTO is "legacy dual-purpose" — newer code uses `OfficeRequestDTO` / `OfficeResponseDTO` for the actual wire traffic.

---

### 5.7 Bus & Driver DTOs

#### BusDTO.java

Legacy combined shape — `busId` + `officeId` + `registrationNumber` + `capacity` + `type`, with full validation. Used by `BusResponseDTO` for display in some templates and by the backend's older endpoints.

#### BusRequestDTO.java

```java
@Data
public class BusRequestDTO {

    @NotNull(message = "Office ID is required to assign this bus")
    private Integer officeId;

    @NotBlank(message = "Registration number (License Plate) is required")
    private String registrationNumber;

    @NotNull(message = "Bus capacity is required")
    @Min(value = 10, message = "A bus must have a capacity of at least 10 seats")
    private Integer capacity;

    @NotBlank(message = "Bus type is required (e.g., AC Sleeper, Non-AC Seater)")
    private String type;
}
```

`@Min(10)` — business rule, no tiny buses. Enforced on backend.

#### BusResponseDTO.java

Same fields plus `busId`. No validations.

#### DriverDTO.java / DriverRequestDTO.java / DriverResponseDTO.java

Pattern identical to the Bus trio:

- **DriverRequestDTO:** `licenseNumber` (`@NotBlank`), `name` (`@NotBlank`), `phone` (10-digit regex), `officeId` (`@NotNull`), `addressId` (`@NotNull`).
- **DriverResponseDTO:** `driverId`, `licenseNumber`, `name`, `phone`, `officeId`, `addressId` — IDs only, no nested objects (REST best practice).
- **DriverDTO:** legacy combined shape with `driverId` + all fields + validations.

---

### 5.8 Office DTOs

#### OfficeRequestDTO.java

```java
@Data
public class OfficeRequestDTO {

    @NotNull(message = "Agency ID is required to link this office to a parent company")
    private Integer agencyId;

    @NotBlank(message = "Office email is required")
    @Email(message = "Invalid email format")
    private String officeMail;

    @NotBlank(message = "Office contact person name is required")
    private String officeContactPersonName;

    @NotBlank(message = "Office contact number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone must be exactly 10 digits")
    private String officeContactNumber;

    @NotNull(message = "Office Address ID is required")
    private Integer officeAddressId;
}
```

#### OfficeResponseDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficeResponseDTO {

    private Integer officeId;

    // Returning IDs instead of full objects
    private Integer agencyId;
    private Integer officeAddressId;

    private String officeMail;
    private String officeContactPersonName;
    private String officeContactNumber;
}
```

The `// Returning IDs instead of full objects` comment is key REST guidance: keep response payloads flat, let the client re-fetch related entities when needed, avoid N+1 JSON bloat.

---

### Viva questions — DTOs

**Q1. Why split into RequestDTO and ResponseDTO for Customer/Agency/Bus/Driver/Office?**
A. Different shapes for different purposes — the request carries only the fields the client must supply (no server-managed `id`, no joined display fields); the response adds `id` and may include flattened display data (like `city` for customers). Keeping them separate means validation annotations only apply to the request side, and you can never accidentally "set" a primary key from an HTTP body.

**Q2. Why does `TripDTO` double as both request and response, while `CustomerDTO` is split?**
A. `TripDTO` is used internally in a trusted admin flow where the full shape is needed anyway. The split approach is cleaner, but `TripDTO` predates that convention in the codebase — it's a pragmatic compromise, not an architectural rule.

**Q3. Why `@NoArgsConstructor` on every DTO?**
A. Jackson (the JSON library Spring uses) requires a no-arg constructor to instantiate an object before it can set fields via reflection. Without it, `objectMapper.readValue(...)` fails with `InvalidDefinitionException`.

---

## Section 6 — API Clients Deep Dive

API clients live under `src/main/java/com/busfrontend/client/` and form a thin, typed facade over `RestTemplate`. Every concrete client extends `AbstractApiClient`, which centralises all the boilerplate (URL building, headers, error translation).

**File inventory:**

| File | Purpose |
|------|---------|
| `AbstractApiClient.java` | Base class — all HTTP verbs + error translation |
| `BackendException.java` | Custom runtime exception surfaced to controllers |
| `TripApiClient.java` | `/api/trips` |
| `BookingApiClient.java` | `/api/bookings` |
| `PaymentApiClient.java` | `/api/payments` |
| `ReviewApiClient.java` | `/api/reviews` |
| `CustomerApiClient.java` | `/api/customers` |
| `AddressApiClient.java` | `/api/addresses` |
| `AgencyApiClient.java` | `/api/agencies` |
| `AgencyOfficeApiClient.java` | `/api/offices` |
| `BusApiClient.java` | `/api/buses` |
| `DriverApiClient.java` | `/api/drivers` |
| `RouteApiClient.java` | `/api/routes` |

### 6.1 AbstractApiClient

**Path:** `src/main/java/com/busfrontend/client/AbstractApiClient.java`

Full source:

```java
public abstract class AbstractApiClient {

    protected final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    protected String baseUrl;

    protected AbstractApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected String url(String path) { return baseUrl + path; }

    protected <T> T get(String path, Class<T> type) {
        try {
            return restTemplate.getForObject(url(path), type);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> ref) {
        try {
            ResponseEntity<List<T>> resp = restTemplate.exchange(
                    url(path), HttpMethod.GET, null, ref);
            return resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected <T, B> T post(String path, B body, Class<T> type) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            return restTemplate.postForObject(url(path), new HttpEntity<>(body, h), type);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected <T, B> T put(String path, B body, Class<T> type) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<T> resp = restTemplate.exchange(
                    url(path), HttpMethod.PUT, new HttpEntity<>(body, h), type);
            return resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected void delete(String path) {
        try {
            restTemplate.delete(url(path));
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    protected byte[] getBytes(String path) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setAccept(List.of(MediaType.APPLICATION_PDF, MediaType.APPLICATION_OCTET_STREAM));
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    url(path), HttpMethod.GET, new HttpEntity<>(h), byte[].class);
            return resp.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw translate(ex);
        }
    }

    private BackendException translate(Exception ex) {
        int status = 500;
        String message = ex.getMessage();
        if (ex instanceof HttpClientErrorException c) {
            status = c.getStatusCode().value();
            message = extractMessage(c.getResponseBodyAsString(), message);
        } else if (ex instanceof HttpServerErrorException s) {
            status = s.getStatusCode().value();
            message = extractMessage(s.getResponseBodyAsString(), message);
        }
        return new BackendException(status, message);
    }

    private String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) return fallback;
        int idx = body.indexOf("\"message\":");
        if (idx < 0) return body;
        int start = body.indexOf('"', idx + 10) + 1;
        int end = body.indexOf('"', start);
        return (start > 0 && end > start) ? body.substring(start, end) : body;
    }
}
```

#### Line-by-line walkthrough

**`protected final RestTemplate restTemplate;`**
One Spring-managed `RestTemplate` bean is injected into every subclass via constructor. `final` because once Spring wires it, it never changes.

**`@Value("${backend.base-url}") protected String baseUrl;`**
Injected at bean-init time from `application.properties` (e.g. `backend.base-url=http://localhost:8080`). Using a field `@Value` keeps subclasses from having to pass it through their constructors. `protected` so children can also read it for exotic cases (none currently do).

**Constructor:**
```java
protected AbstractApiClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
}
```
Protected — you can't instantiate `AbstractApiClient` directly; only subclasses. Subclasses forward their own constructor arg via `super(restTemplate)`.

**`url(String path)` helper:**
Trivial concatenation — `baseUrl + path`. Every subclass uses this to build absolute URLs. Keeping it in one place means if tomorrow you add a global prefix (say `/v1`), you change it here.

**`get(path, type)`:**
```java
return restTemplate.getForObject(url(path), type);
```
`getForObject` is the simplest GET — Spring deserialises the response body into `type` via Jackson. The `try/catch` catches **two** RestTemplate exception classes:
- `HttpClientErrorException` — 4xx (bad request, not found, etc.)
- `HttpServerErrorException` — 5xx (backend blew up)

Both get funneled through `translate()` into a `BackendException`, which controllers can then catch cleanly.

**`getList(path, ref)` with `ParameterizedTypeReference`:**
```java
ResponseEntity<List<T>> resp = restTemplate.exchange(
        url(path), HttpMethod.GET, null, ref);
```
Why not just `List.class`? Because of **Java's type erasure** — at runtime, `List<TripDTO>` and `List<String>` are both just `List`. Jackson can't infer the element type. `ParameterizedTypeReference<List<T>>` is an anonymous subclass that preserves the generic type info via reflection on its `getClass().getGenericSuperclass()`. That's why every call site writes `new ParameterizedTypeReference<List<TripDTO>>() {}` — the trailing `{}` creates the anonymous subclass.

**`post(path, body, type)`:**
Sets `Content-Type: application/json` header explicitly (most servers assume it, but being explicit is safer), wraps the body in an `HttpEntity` alongside the headers, and uses `postForObject` to get the deserialised response. `<T, B>` — T is the response type, B is the body type; they're generic so the same method serves every client.

**`put(path, body, type)`:**
`RestTemplate` doesn't have a one-shot `putForObject`, so we drop down to `restTemplate.exchange(...)` with `HttpMethod.PUT` and manually pull `.getBody()` off the `ResponseEntity`.

**`delete(path)`:**
Simplest of all — `restTemplate.delete(url)` fires the request; there's no response body to parse.

**`getBytes(path)` — for PDF downloads:**
```java
HttpHeaders h = new HttpHeaders();
h.setAccept(List.of(MediaType.APPLICATION_PDF, MediaType.APPLICATION_OCTET_STREAM));
ResponseEntity<byte[]> resp = restTemplate.exchange(
        url(path), HttpMethod.GET, new HttpEntity<>(h), byte[].class);
return resp.getBody();
```
Sets `Accept: application/pdf, application/octet-stream` so the server knows we want binary. The response body comes back as `byte[]`, which a controller then flips into a `ResponseEntity<byte[]>` with `Content-Disposition: attachment` for browser download.

**`translate(ex)` — the core error funnel:**
```java
int status = 500;
String message = ex.getMessage();
if (ex instanceof HttpClientErrorException c) {
    status = c.getStatusCode().value();
    message = extractMessage(c.getResponseBodyAsString(), message);
} else if (ex instanceof HttpServerErrorException s) {
    status = s.getStatusCode().value();
    message = extractMessage(s.getResponseBodyAsString(), message);
}
return new BackendException(status, message);
```
Uses Java 16+ **pattern matching for instanceof** (`ex instanceof HttpClientErrorException c`). Pulls the numeric status (`404`, `409`, `500`, …) off the exception and then extracts the `"message"` field from the JSON error body.

**`extractMessage(body, fallback)` — lightweight JSON parsing:**
```java
if (body == null || body.isBlank()) return fallback;
int idx = body.indexOf("\"message\":");
if (idx < 0) return body;
int start = body.indexOf('"', idx + 10) + 1;
int end = body.indexOf('"', start);
return (start > 0 && end > start) ? body.substring(start, end) : body;
```
This is NOT a full JSON parse — it's a deliberate string-scan for `"message":"..."` that avoids pulling in Jackson just to unwrap one field. Fragile if the backend ever returns escaped quotes inside the message, but for our backend's `{"timestamp":..., "status":..., "message":"Seat already booked"}` shape it works fine. If it can't find `"message":`, it returns the raw body so nothing is silently lost.

#### Why these design choices?

- **One base class, many children:** 12 clients × ~30 lines of boilerplate each would be 360 lines of copy-paste. `AbstractApiClient` compresses that into ~40 lines of real logic per method, with zero duplication.
- **Generics + `ParameterizedTypeReference`:** type-safe — if you call `getList("/api/trips", new ParameterizedTypeReference<List<TripDTO>>() {})`, the compiler enforces you assigning to `List<TripDTO>`. No `Object` casts.
- **Translate to custom exception:** controllers catch one thing (`BackendException`), not five (`RestClientException`, `HttpClientErrorException`, `HttpServerErrorException`, `ResourceAccessException`, `RestTemplateXhrTransportException`).

### 6.2 BackendException

**Path:** `src/main/java/com/busfrontend/client/BackendException.java`

```java
public class BackendException extends RuntimeException {
    private final int status;

    public BackendException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}
```

**Why a custom exception class?**

1. **Cleaner controller catches.** Instead of `catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException ex)` everywhere, controllers write `catch (BackendException ex)` once.
2. **Decouples the view layer from `RestTemplate`.** If tomorrow we swap `RestTemplate` for `WebClient` or `OkHttp`, controllers don't change — only `AbstractApiClient.translate(...)` does.
3. **Carries the HTTP status.** `ex.getStatus()` lets a controller make smart decisions — "404 means go to list page, 409 means show inline error".
4. **Runtime, not checked.** Extends `RuntimeException`, so callers aren't forced to declare `throws` — Spring MVC handlers stay clean.

### Viva questions — AbstractApiClient

**Q1. Why `ParameterizedTypeReference` instead of `List.class`?**
A. Java erases generic type info at runtime, so `List.class` gives Jackson no clue what to deserialise each element into. `new ParameterizedTypeReference<List<TripDTO>>() {}` (with the anonymous `{}` subclass) preserves the generic parameter via reflection, letting Jackson correctly produce `List<TripDTO>`.

**Q2. Why catch both `HttpClientErrorException` and `HttpServerErrorException`?**
A. `RestTemplate` throws `HttpClientErrorException` for 4xx responses and `HttpServerErrorException` for 5xx. Catching only one would let the other bubble as a raw exception — we want **any** non-2xx to become a `BackendException`.

**Q3. Why not `@Component` on `AbstractApiClient`?**
A. It's abstract — Spring can't instantiate it. Only the concrete subclasses (`TripApiClient`, `BookingApiClient`, …) carry `@Component`.

---

### 6.3 Module Clients

Every concrete client follows the same pattern:

```java
@Component
public class XxxApiClient extends AbstractApiClient {
    public XxxApiClient(RestTemplate restTemplate) { super(restTemplate); }
    // typed methods that call protected helpers
}
```

`@Component` makes Spring register the client as a bean so controllers can inject it via `@RequiredArgsConstructor`. The single-arg constructor forwards the `RestTemplate` to the parent. Everything else is a method-per-endpoint.

#### TripApiClient.java

```java
@Component
public class TripApiClient extends AbstractApiClient {

    public TripApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<TripDTO> getAllTrips() {
        return getList("/api/trips", new ParameterizedTypeReference<List<TripDTO>>() {});
    }

    public TripDTO getById(Integer id) {
        return get("/api/trips/" + id, TripDTO.class);
    }

    public TripDTO create(TripDTO dto) {
        return post("/api/trips", dto, TripDTO.class);
    }

    public TripDTO update(Integer id, TripDTO dto) {
        return put("/api/trips/" + id, dto, TripDTO.class);
    }

    public List<TripDTO> search(String from, String to) {
        String path = UriComponentsBuilder.fromPath("/api/trips/search")
                .queryParam("from", from).queryParam("to", to)
                .build().toUriString();
        return getList(path, new ParameterizedTypeReference<List<TripDTO>>() {});
    }
}
```

| Method | HTTP | Backend endpoint | Request body | Response |
|--------|------|------------------|--------------|----------|
| `getAllTrips()` | GET | `/api/trips` | — | `List<TripDTO>` |
| `getById(id)` | GET | `/api/trips/{id}` | — | `TripDTO` |
| `create(dto)` | POST | `/api/trips` | `TripDTO` | `TripDTO` |
| `update(id, dto)` | PUT | `/api/trips/{id}` | `TripDTO` | `TripDTO` |
| `search(from, to)` | GET | `/api/trips/search?from=X&to=Y` | — | `List<TripDTO>` |

`search` uses `UriComponentsBuilder` to safely URL-encode the query params — so `from=São Paulo` becomes `from=S%C3%A3o%20Paulo`. String concatenation would break on spaces/accents.

**Sample call + response:**

```
GET http://localhost:8080/api/trips/search?from=Pune&to=Mumbai

[
  { "tripId": 42, "fromCity": "Pune", "toCity": "Mumbai", "fare": 650.00, "availableSeats": 38, ... },
  { "tripId": 47, "fromCity": "Pune", "toCity": "Mumbai", "fare": 720.00, "availableSeats": 12, ... }
]
```

#### BookingApiClient.java

```java
@Component
public class BookingApiClient extends AbstractApiClient {

    public BookingApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public BookingResponseDTO createBooking(BookingRequestDTO req) {
        return post("/api/bookings", req, BookingResponseDTO.class);
    }

    @SuppressWarnings("rawtypes")
    public Map getById(Integer id) {
        return get("/api/bookings/" + id, Map.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getByTrip(Integer tripId) {
        return getList("/api/bookings/trip/" + tripId,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public byte[] downloadTicket(Integer bookingId) {
        return getBytes("/api/bookings/" + bookingId + "/ticket");
    }

    public byte[] downloadGroupTicket(List<Integer> bookingIds) {
        String csv = String.join(",", bookingIds.stream().map(String::valueOf).toList());
        return getBytes("/api/bookings/group-ticket?bookingIds=" + csv);
    }
}
```

| Method | HTTP | Endpoint | Returns |
|--------|------|----------|---------|
| `createBooking(req)` | POST | `/api/bookings` | `BookingResponseDTO` |
| `getById(id)` | GET | `/api/bookings/{id}` | `Map` (raw) |
| `getByTrip(tripId)` | GET | `/api/bookings/trip/{tripId}` | `List<Map<String,Object>>` |
| `downloadTicket(bookingId)` | GET | `/api/bookings/{id}/ticket` | `byte[]` (PDF) |
| `downloadGroupTicket(ids)` | GET | `/api/bookings/group-ticket?bookingIds=1,2,3` | `byte[]` (PDF) |

`getById` and `getByTrip` return raw `Map` because the backend returns a shape that doesn't map to a single DTO (mixes booking fields with trip fields). The controllers extract the keys they need (`"status"`, `"seatNumber"`, `"tripId"`) defensively.

`downloadGroupTicket` joins the ID list into a CSV (`"1,2,3"`) and appends it as a query string. The backend parses it back with `@RequestParam List<Integer> bookingIds`.

**Sample request body for `createBooking`:**

```json
{ "tripId": 42, "seatNumbers": [5, 6], "customerId": 18 }
```

**Sample response:**

```json
{ "message": "2 seats booked", "bookingIds": [101, 102], "totalFare": 1300.00, "customerId": 18 }
```

#### PaymentApiClient.java

```java
@Component
public class PaymentApiClient extends AbstractApiClient {

    public PaymentApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public PaymentResponseDTO processPayment(PaymentRequestDTO req) {
        return post("/api/payments", req, PaymentResponseDTO.class);
    }

    public List<PaymentResponseDTO> getAll() {
        return getList("/api/payments", new ParameterizedTypeReference<List<PaymentResponseDTO>>() {});
    }

    public PaymentResponseDTO getById(Integer id) {
        return get("/api/payments/" + id, PaymentResponseDTO.class);
    }

    public PaymentResponseDTO getByBookingId(Integer bookingId) {
        return get("/api/payments/booking/" + bookingId, PaymentResponseDTO.class);
    }

    public List<PaymentResponseDTO> getByCustomerId(Integer customerId) {
        return getList("/api/payments/customer/" + customerId,
                new ParameterizedTypeReference<List<PaymentResponseDTO>>() {});
    }

    public byte[] downloadTicketByPaymentId(Integer paymentId) {
        return getBytes("/api/payments/" + paymentId + "/ticket");
    }

    public byte[] downloadGroupTicket(List<Integer> paymentIds) {
        String csv = String.join(",", paymentIds.stream().map(String::valueOf).toList());
        return getBytes("/api/payments/group-ticket?paymentIds=" + csv);
    }
}
```

| Method | HTTP | Endpoint |
|--------|------|----------|
| `processPayment(req)` | POST | `/api/payments` |
| `getAll()` | GET | `/api/payments` |
| `getById(id)` | GET | `/api/payments/{id}` |
| `getByBookingId(bid)` | GET | `/api/payments/booking/{bid}` |
| `getByCustomerId(cid)` | GET | `/api/payments/customer/{cid}` |
| `downloadTicketByPaymentId(pid)` | GET | `/api/payments/{pid}/ticket` |
| `downloadGroupTicket(ids)` | GET | `/api/payments/group-ticket?paymentIds=...` |

Note we have **two** ticket endpoints — one by bookingId (in `BookingApiClient`) and one by paymentId (here). This is historical: the first iteration let users download tickets straight after booking, the second adds the "pay first, then download" flow. Both co-exist.

#### ReviewApiClient.java

```java
@Component
public class ReviewApiClient extends AbstractApiClient {

    public ReviewApiClient(RestTemplate restTemplate) { super(restTemplate); }

    @SuppressWarnings("unchecked")
    public Map<String, Object> create(ReviewDTO dto) { return post("/api/reviews", dto, Map.class); }

    public List<Map<String, Object>> getAll() {
        return getList("/api/reviews", new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public List<Map<String, Object>> getByTrip(Integer tripId) {
        return getList("/api/reviews/trip/" + tripId,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public List<Map<String, Object>> getByCustomer(Integer customerId) {
        return getList("/api/reviews/customer/" + customerId,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }
}
```

All GETs return `List<Map<String,Object>>` because the backend enriches the review with joined customer/trip display fields that don't round-trip cleanly onto `ReviewDTO`. POST returns a `Map` (usually `{"message": "Review saved", "reviewId": 73}`).

#### CustomerApiClient.java

```java
@Component
public class CustomerApiClient extends AbstractApiClient {

    public CustomerApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<CustomerResponseDTO> getAll() {
        return getList("/api/customers", new ParameterizedTypeReference<List<CustomerResponseDTO>>() {});
    }

    public CustomerResponseDTO getById(Integer id) {
        return get("/api/customers/" + id, CustomerResponseDTO.class);
    }

    public CustomerResponseDTO create(CustomerRequestDTO dto) {
        return post("/api/customers", dto, CustomerResponseDTO.class);
    }

    public CustomerResponseDTO update(Integer id, CustomerRequestDTO dto) {
        return put("/api/customers/" + id, dto, CustomerResponseDTO.class);
    }

    public void delete(Integer id) { delete("/api/customers/" + id); }
}
```

Classic CRUD — `getAll`, `getById`, `create`, `update`, `delete`. Note the asymmetric DTOs: input is `CustomerRequestDTO`, output is `CustomerResponseDTO`.

#### AddressApiClient.java

```java
@Component
public class AddressApiClient extends AbstractApiClient {

    public AddressApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<AddressDTO> getAll() {
        return getList("/api/addresses", new ParameterizedTypeReference<List<AddressDTO>>() {});
    }

    public AddressDTO getById(Integer id) { return get("/api/addresses/" + id, AddressDTO.class); }

    public AddressDTO create(AddressDTO dto) { return post("/api/addresses", dto, AddressDTO.class); }

    public AddressDTO update(Integer id, AddressDTO dto) { return put("/api/addresses/" + id, dto, AddressDTO.class); }

    public void delete(Integer id) { delete("/api/addresses/" + id); }
}
```

Symmetric `AddressDTO` on both sides. The typical use pattern is: Customer form → creates Address inline → gets `addressId` → creates Customer with that id. See `CustomerViewController.saveCustomer` below.

#### AgencyApiClient.java

```java
@Component
public class AgencyApiClient extends AbstractApiClient {

    public AgencyApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<AgencyResponseDTO> getAll() {
        return getList("/api/agencies", new ParameterizedTypeReference<List<AgencyResponseDTO>>() {});
    }

    public AgencyResponseDTO getById(Integer id) {
        return get("/api/agencies/" + id, AgencyResponseDTO.class);
    }

    public AgencyResponseDTO create(AgencyRequestDTO dto) {
        return post("/api/agencies", dto, AgencyResponseDTO.class);
    }

    public AgencyResponseDTO update(Integer id, AgencyRequestDTO dto) {
        return put("/api/agencies/" + id, dto, AgencyResponseDTO.class);
    }
}
```

No `delete` — agencies aren't deletable from the UI (business rule: audit trail).

#### AgencyOfficeApiClient.java

```java
@Component
public class AgencyOfficeApiClient extends AbstractApiClient {

    public AgencyOfficeApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<OfficeResponseDTO> getAll() {
        return getList("/api/offices", new ParameterizedTypeReference<List<OfficeResponseDTO>>() {});
    }

    public OfficeResponseDTO getById(Integer id) { return get("/api/offices/" + id, OfficeResponseDTO.class); }

    public List<OfficeResponseDTO> getByAgency(Integer agencyId) {
        return getList("/api/offices/agency/" + agencyId,
                new ParameterizedTypeReference<List<OfficeResponseDTO>>() {});
    }

    public OfficeResponseDTO create(OfficeRequestDTO dto) { return post("/api/offices", dto, OfficeResponseDTO.class); }

    public OfficeResponseDTO update(Integer id, OfficeRequestDTO dto) { return put("/api/offices/" + id, dto, OfficeResponseDTO.class); }
}
```

`getByAgency(id)` is the filter endpoint — "give me all offices belonging to agency X".

#### BusApiClient.java

```java
@Component
public class BusApiClient extends AbstractApiClient {

    public BusApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<BusResponseDTO> getAll() {
        return getList("/api/buses", new ParameterizedTypeReference<List<BusResponseDTO>>() {});
    }

    public BusResponseDTO getById(Integer id) { return get("/api/buses/" + id, BusResponseDTO.class); }

    public List<BusResponseDTO> getByOffice(Integer officeId) {
        return getList("/api/buses/office/" + officeId, new ParameterizedTypeReference<List<BusResponseDTO>>() {});
    }

    public BusResponseDTO create(BusRequestDTO dto) { return post("/api/buses", dto, BusResponseDTO.class); }

    public BusResponseDTO update(Integer id, BusRequestDTO dto) { return put("/api/buses/" + id, dto, BusResponseDTO.class); }
}
```

Includes `getByOffice` — useful when an office admin only cares about their own fleet. Currently unused in the UI but ready for when per-office views land.

#### DriverApiClient.java

```java
@Component
public class DriverApiClient extends AbstractApiClient {

    public DriverApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<DriverResponseDTO> getAll() {
        return getList("/api/drivers", new ParameterizedTypeReference<List<DriverResponseDTO>>() {});
    }

    public DriverResponseDTO getById(Integer id) { return get("/api/drivers/" + id, DriverResponseDTO.class); }

    public List<DriverResponseDTO> getByOffice(Integer officeId) {
        return getList("/api/drivers/office/" + officeId,
                new ParameterizedTypeReference<List<DriverResponseDTO>>() {});
    }

    public DriverResponseDTO create(DriverRequestDTO dto) { return post("/api/drivers", dto, DriverResponseDTO.class); }

    public DriverResponseDTO update(Integer id, DriverRequestDTO dto) { return put("/api/drivers/" + id, dto, DriverResponseDTO.class); }
}
```

Mirrors `BusApiClient` exactly — same shape, different entity.

#### RouteApiClient.java

```java
@Component
public class RouteApiClient extends AbstractApiClient {

    public RouteApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<RouteDTO> getAll() {
        return getList("/api/routes", new ParameterizedTypeReference<List<RouteDTO>>() {});
    }

    public RouteDTO getById(Integer id) { return get("/api/routes/" + id, RouteDTO.class); }

    public RouteDTO create(RouteDTO dto) { return post("/api/routes", dto, RouteDTO.class); }

    public RouteDTO update(Integer id, RouteDTO dto) { return put("/api/routes/" + id, dto, RouteDTO.class); }

    public void delete(Integer id) { delete("/api/routes/" + id); }
}
```

No search endpoint here — that lives on the trip side. `RouteViewController.searchRoutesView` filters client-side using Streams (see note in that controller's code: "RouteApiClient has no search endpoint yet; filter client-side").

### Viva questions — module clients

**Q1. Why does every client have the same three-line constructor?**
A. The protected `AbstractApiClient(RestTemplate)` constructor requires subclasses to forward a `RestTemplate`. `@Component` + constructor injection means Spring autowires the `RestTemplate` bean (defined in `RestTemplateConfig`) into each client at startup.

**Q2. Why does `BookingApiClient.getById` return `Map` instead of `BookingResponseDTO`?**
A. The backend's `GET /api/bookings/{id}` returns a flat projection (`bookingId`, `tripId`, `seatNumber`, `status`) — a different shape from the booking-creation response. Rather than add a `BookingReadDTO` class for one endpoint, we accept the generic `Map` and let the controller pick out only the keys it needs.

**Q3. Why is `delete` not on every client?**
A. Business rules. Agencies, Offices, Buses, Drivers, and Trips are audit-worthy entities — soft delete at most, never hard delete from the UI. Customers, Addresses, and Routes are safe to remove. The API shape matches the backend — endpoints that don't exist backend-side aren't exposed frontend-side either.

---

## Section 7 — View Controllers Deep Dive

View controllers live under `src/main/java/com/busfrontend/controller/` and bridge Thymeleaf templates to API clients. Every one is annotated:

```java
@Controller
@RequiredArgsConstructor
public class XxxViewController { ... }
```

- `@Controller` (not `@RestController`) — tells Spring MVC that returned strings are **view names**, not serialised response bodies.
- `@RequiredArgsConstructor` (Lombok) — generates a constructor for all `final` fields. Spring uses that for DI.

There are 12 controllers covering 13 view families (Home + 12 entity flows).

### 7.1 HomePageController

**Path:** `src/main/java/com/busfrontend/controller/HomePageController.java`

```java
@Controller
public class HomePageController {

    @GetMapping("/")
    public String homePage() {
        return "home/index";
    }
}
```

Single endpoint, zero dependencies. Maps `GET /` to `templates/home/index.html`. Returned string is resolved by Thymeleaf via the `ViewResolver` → `classpath:/templates/home/index.html`.

### 7.2 TripViewController

**Path:** `src/main/java/com/busfrontend/controller/TripViewController.java`

The most multi-dependency controller. Injects **five** API clients because the trip-add form needs dropdowns for routes, buses, addresses (for boarding/dropping), and drivers (for primary/secondary).

```java
@Controller
@RequiredArgsConstructor
public class TripViewController {

    private static final String ATTR_MESSAGE = "message";
    private static final String ATTR_ERROR = "error";
    private static final String REDIRECT_VIEW_TRIPS = "redirect:/view/trips";

    private final TripApiClient tripApiClient;
    private final RouteApiClient routeApiClient;
    private final BusApiClient busApiClient;
    private final AddressApiClient addressApiClient;
    private final DriverApiClient driverApiClient;
    // ...
}
```

The string constants prevent typos — using `ATTR_MESSAGE` everywhere means you can't accidentally write `"mesage"` in one spot.

| Mapping | Path | Action |
|---------|------|--------|
| `@GetMapping` | `/view/trips` | List all trips |
| `@GetMapping` | `/view/trips/add` | Show add-trip form |
| `@PostMapping` | `/view/trips/save` | Create a trip |
| `@GetMapping` | `/view/trips/edit/{id}` | Show edit-trip form |
| `@PostMapping` | `/view/trips/update/{id}` | Update a trip |
| `@GetMapping` | `/view/trips/search` | Search by from/to city |

#### listTrips

```java
@GetMapping("/view/trips")
public String listTrips(Model model) {
    List<TripDTO> trips = tripApiClient.getAllTrips();
    model.addAttribute("trips", trips);
    return "trip/trips";
}
```

Fetches everything, dumps into `model.trips`, renders `templates/trip/trips.html`.

#### showAddForm

```java
@GetMapping("/view/trips/add")
public String showAddForm(Model model) {
    model.addAttribute("routes", routeApiClient.getAll());
    model.addAttribute("buses", busApiClient.getAll());
    model.addAttribute("addresses", addressApiClient.getAll());
    model.addAttribute("drivers", driverApiClient.getAll());
    return "trip/add-trip";
}
```

Four parallel API calls to populate four `<select>` dropdowns in the form. In a bigger app you'd parallelise these with `CompletableFuture`; for a college project sequential is fine.

#### saveTrip — the big one

```java
@PostMapping("/view/trips/save")
public String saveTrip(@RequestParam Integer routeId,
                       @RequestParam Integer busId,
                       @RequestParam Integer boardingAddressId,
                       @RequestParam Integer droppingAddressId,
                       @RequestParam String departureTime,
                       @RequestParam String arrivalTime,
                       @RequestParam Integer driver1Id,
                       @RequestParam(required = false) Integer driver2Id,
                       @RequestParam Integer availableSeats,
                       @RequestParam BigDecimal fare,
                       @RequestParam String tripDate,
                       RedirectAttributes ra) {
    try {
        TripDTO dto = TripDTO.builder()
                .routeId(routeId)
                .busId(busId)
                .boardingAddressId(boardingAddressId)
                .droppingAddressId(droppingAddressId)
                .departureTime(LocalDateTime.parse(departureTime))
                .arrivalTime(LocalDateTime.parse(arrivalTime))
                .driver1Id(driver1Id)
                .driver2Id(driver2Id)
                .availableSeats(availableSeats)
                .fare(fare)
                .tripDate(LocalDateTime.parse(tripDate))
                .build();
        tripApiClient.create(dto);
        ra.addFlashAttribute(ATTR_MESSAGE, "Trip added successfully!");
    } catch (BackendException ex) {
        ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
    } catch (Exception ex) {
        ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
    }
    return REDIRECT_VIEW_TRIPS;
}
```

**Step by step:**

1. Spring binds each form field to a `@RequestParam` by name. `driver2Id` has `required = false` because co-driver is optional.
2. We build a `TripDTO` using the Lombok builder. `LocalDateTime.parse(...)` expects ISO format (`2026-05-01T08:30:00`) — this matches the HTML `<input type="datetime-local">` output, so nothing fancy is needed.
3. `tripApiClient.create(dto)` POSTs to the backend. If it succeeds, we flash a green "success" message.
4. `catch (BackendException ex)` captures REST failures (e.g. 409 conflict — "bus already assigned at that time"). We flash the server's error message back.
5. `catch (Exception ex)` is the safety net — anything unexpected (a date-parse failure, an NPE) still shows the user *something*.
6. Redirect to the list page. **Flash attributes** survive exactly one redirect — the list template displays the message/error banner then clears it.

**Why two catch blocks if they do the same thing?** Historical — they used to log differently. Collapsing them into `catch (Exception ex)` would work equally well today.

#### showEditForm + updateTrip

`showEditForm` does the same dropdown-loading as `showAddForm` but also calls `tripApiClient.getById(id)` and puts the trip into the model for pre-filling. `updateTrip` mirrors `saveTrip` except it passes the id as a path variable and builds the DTO with `.tripId(id)` set.

#### searchTripsView

```java
@GetMapping("/view/trips/search")
public String searchTripsView(@RequestParam(required = false) String from,
                              @RequestParam(required = false) String to,
                              Model model) {
    if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
        List<TripDTO> results = tripApiClient.search(from.trim(), to.trim());
        model.addAttribute("results", results);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
    }
    return "trip/search-trips";
}
```

Clean pattern — both `from` and `to` are optional so the same endpoint serves both "show empty form" and "show results". The `if` gates the API call; the `trim()` tolerates trailing spaces.

### Viva questions — TripViewController

**Q1. Why use `RedirectAttributes` instead of `Model`?**
A. `Model` attributes don't survive a redirect. After POST→redirect, the target controller runs fresh and the model is empty. `addFlashAttribute` stashes the value in the session for exactly one redirect hop, after which it's cleared — perfect for success/error banners.

**Q2. Why `@RequestParam` for 11 fields instead of `@ModelAttribute TripDTO`?**
A. Some fields need coercion that Spring can't do natively — `String departureTime` → `LocalDateTime.parse(...)`. With `@ModelAttribute`, the binding would fail before we could intervene. Splitting out the params gives us full control.

### 7.3 RouteViewController

**Path:** `src/main/java/com/busfrontend/controller/RouteViewController.java`

Classic CRUD controller. Injects one client:

```java
private final RouteApiClient routeApiClient;
```

Six mappings — `/view/routes` (list), `/view/routes/add`, `/view/routes/save`, `/view/routes/edit/{id}`, `/view/routes/update/{id}`, `/view/routes/search`.

Save uses `@ModelAttribute RouteDTO route` — clean because all fields are primitive/`String`/`Integer`, no date parsing needed.

The search uses **client-side filter** (the note in the code admits: `TODO: advanced flow — RouteApiClient has no search endpoint yet; filter client-side`):

```java
var results = routeApiClient.getAll().stream()
        .filter(r -> r.getFromCity() != null && r.getToCity() != null
                && r.getFromCity().toLowerCase().contains(fromTrim)
                && r.getToCity().toLowerCase().contains(toTrim))
        .toList();
```

Fine for <1000 routes; will need a real backend search when the catalog grows.

### 7.4 BookingViewController

**Path:** `src/main/java/com/busfrontend/controller/BookingViewController.java`

Four injected clients — booking, trip, customer, bus. Has the most complex flow in the app.

```java
@Controller
@RequiredArgsConstructor
public class BookingViewController {

    private static final String ATTR_BOOKING = "booking";
    private static final String ATTR_ERROR = "error";

    private final BookingApiClient bookingApiClient;
    private final TripApiClient tripApiClient;
    private final CustomerApiClient customerApiClient;
    private final BusApiClient busApiClient;
    // ...
}
```

#### listAvailableTrips

```java
@GetMapping("/view/bookings")
public String listAvailableTrips(Model model) {
    List<TripDTO> trips = tripApiClient.getAllTrips().stream()
            .filter(trip -> trip.getAvailableSeats() != null && trip.getAvailableSeats() > 0)
            .toList();
    model.addAttribute("trips", trips);
    return "booking/trips";
}
```

Fetches all trips but only shows ones with seats left. Filter is defensive against `null` (a poorly seeded database shouldn't crash the page).

#### showSeatSelection — the gnarly one

```java
@GetMapping("/view/bookings/trip/{tripId}")
public String showSeatSelection(@PathVariable Integer tripId, Model model, RedirectAttributes ra) {
    try {
        TripDTO trip = tripApiClient.getById(tripId);

        // Resolve bus capacity via BusApiClient (TripDTO doesn't carry capacity).
        int capacity = 0;
        if (trip.getBusId() != null) {
            BusResponseDTO bus = busApiClient.getById(trip.getBusId());
            if (bus != null && bus.getCapacity() != null) capacity = bus.getCapacity();
        }

        List<Integer> allSeats = new ArrayList<>();
        for (int i = 1; i <= capacity; i++) allSeats.add(i);

        List<Integer> bookedSeats = new ArrayList<>();
        List<Map<String, Object>> bookings = bookingApiClient.getByTrip(tripId);
        if (bookings != null) {
            for (Map<String, Object> b : bookings) {
                Object status = b.get("status");
                Object seatNum = b.get("seatNumber");
                if ("Booked".equals(String.valueOf(status)) && seatNum instanceof Number n) {
                    bookedSeats.add(n.intValue());
                }
            }
        }

        model.addAttribute("trip", trip);
        model.addAttribute("allSeats", allSeats);
        model.addAttribute("bookedSeats", bookedSeats);
        model.addAttribute("customers", customerApiClient.getAll());
        return "booking/select-seat";
    } catch (BackendException ex) {
        ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        return "redirect:/view/bookings";
    } catch (Exception ex) {
        ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        return "redirect:/view/bookings";
    }
}
```

**What it does:**

1. Fetch the trip. If it doesn't exist, `BackendException` pops from `get` — redirect to list with the error.
2. Fetch the bus (separate call) to get its `capacity` — the template shows seats 1..N on a grid.
3. Build `allSeats = [1..capacity]` for the template to iterate.
4. Fetch all bookings for this trip and filter to `status = "Booked"`. Extract each `seatNumber`, coerce to int via `instanceof Number n` pattern matching.
5. Stash trip, allSeats, bookedSeats, and all customers (for the customer dropdown) in the model.
6. Render `booking/select-seat.html` — template greys out already-booked seats and lets user multi-select free ones.

**Defensive tricks worth noting:**
- `trip.getBusId() != null` — nullable in the DTO, so guard before use.
- `"Booked".equals(String.valueOf(status))` — inverted equality to dodge NPE if `status` is null; `String.valueOf(null)` returns `"null"`, which fails the equals check cleanly.
- `seatNum instanceof Number n` — backend might deliver seat numbers as `Integer` or `Long` depending on JSON; `Number` covers both.

#### bookSeats

```java
@PostMapping("/view/bookings/book")
public String bookSeats(@RequestParam Integer tripId,
                        @RequestParam List<Integer> seatNumbers,
                        @RequestParam Integer customerId,
                        RedirectAttributes ra) {
    try {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .tripId(tripId).seatNumbers(seatNumbers).customerId(customerId).build();
        BookingResponseDTO response = bookingApiClient.createBooking(request);
        ra.addFlashAttribute(ATTR_BOOKING, response);
        ra.addFlashAttribute("seatNumbers", seatNumbers);

        // Pass customer name
        CustomerResponseDTO customer = customerApiClient.getById(customerId);
        if (customer != null) ra.addFlashAttribute("customerName", customer.getName());

        // Pass trip info
        TripDTO trip = tripApiClient.getById(tripId);
        if (trip != null) {
            if (trip.getFromCity() != null) ra.addFlashAttribute("fromCity", trip.getFromCity());
            if (trip.getToCity() != null) ra.addFlashAttribute("toCity", trip.getToCity());
            if (trip.getTripDate() != null) {
                ra.addFlashAttribute("tripDate", trip.getTripDate().toLocalDate().toString());
            }
        }
        return "redirect:/view/bookings/confirmation/" + response.getBookingIds().get(0);
    } catch (BackendException ex) {
        ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        return "redirect:/view/bookings/trip/" + tripId;
    } catch (Exception ex) {
        ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        return "redirect:/view/bookings/trip/" + tripId;
    }
}
```

Key points:
- `@RequestParam List<Integer> seatNumbers` — Spring auto-splits `seatNumbers=1&seatNumbers=2&seatNumbers=3` from the form (or `seatNumbers=1,2,3`) into a list.
- POST to backend, then greedily enrich flash attributes with display data (customer name, from/to city, trip date) so the confirmation page doesn't need its own lookups.
- On success, redirect to `/view/bookings/confirmation/{firstBookingId}`. The template uses the list of IDs to render one row per seat.
- On error, redirect back to the seat selection page so the user can try again.

#### showConfirmation

```java
@GetMapping("/view/bookings/confirmation/{bookingId}")
public String showConfirmation(@PathVariable Integer bookingId, Model model) {
    if (!model.containsAttribute(ATTR_BOOKING)) {
        try {
            @SuppressWarnings("rawtypes")
            Map booking = bookingApiClient.getById(bookingId);
            BookingResponseDTO dto = BookingResponseDTO.builder()
                    .bookingIds(List.of(bookingId))
                    .message("Booking details")
                    .build();
            model.addAttribute(ATTR_BOOKING, dto);
            if (booking != null && booking.get("seatNumber") instanceof Number seat) {
                model.addAttribute("seatNumbers", List.of(seat.intValue()));
            }
        } catch (Exception ignored) {
            // Swallow — confirmation template tolerates missing attrs.
        }
    }
    return "booking/confirmation";
}
```

Smart fallback logic: when arriving via redirect (normal path) the flash attribute `booking` is already present; we render directly. When a user refreshes or bookmarks the URL, flash data is gone — we re-fetch the booking and synthesise a minimal `BookingResponseDTO` so the template still has something to render.

#### Ticket download helpers

```java
@GetMapping("/view/bookings/ticket")
public String showTicketDownloadForm() { return "booking/ticket-download"; }

@GetMapping("/view/bookings/group-ticket")
public String showGroupTicketDownloadForm() { return "booking/group-ticket-download"; }
```

Just render the ticket-lookup forms. The actual PDF generation lives on the backend; the templates point a form at `/api/bookings/{id}/ticket` or `/api/bookings/group-ticket?...` directly. (Bypassing the frontend controller is fine for binary downloads.)

### Viva questions — BookingViewController

**Q1. Why is the POST → redirect → GET pattern used for `bookSeats`?**
A. POST-then-GET prevents the browser's "reload resubmits the form?" dialog and stops double-booking on refresh. After booking succeeds, the user lands on a confirmation URL that's safe to bookmark or refresh.

**Q2. Why does `showSeatSelection` call BusApiClient separately?**
A. `TripDTO` doesn't carry `capacity` (the backend's trip projection isn't joined that deep). We need `capacity` to know how many seat checkboxes to render, so we make one extra call. A future enhancement would be to add `capacity` to `TripDTO`.

### 7.5 PaymentViewController

**Path:** `src/main/java/com/busfrontend/controller/PaymentViewController.java`

Four clients — payment, booking, customer, trip.

```java
@Controller
@RequiredArgsConstructor
public class PaymentViewController {

    private static final String ATTR_SEAT_COUNT = "seatCount";
    private static final String ATTR_ALL_PAYMENT_IDS = "allPaymentIds";
    private static final String ATTR_ERROR = "error";

    private final PaymentApiClient paymentApiClient;
    private final BookingApiClient bookingApiClient;
    private final CustomerApiClient customerApiClient;
    private final TripApiClient tripApiClient;
    // ...
}
```

#### listPayments + groupRelatedPayments

```java
@GetMapping("/view/payments")
public String listPayments(Model model) {
    List<PaymentResponseDTO> all = paymentApiClient.getAll();
    model.addAttribute("payments", all);
    model.addAttribute("paymentGroups", groupRelatedPayments(all));
    return "payment/payments";
}

private List<PaymentGroup> groupRelatedPayments(List<PaymentResponseDTO> all) {
    if (all == null) return List.of();
    Map<String, List<PaymentResponseDTO>> buckets = new LinkedHashMap<>();
    for (PaymentResponseDTO p : all) {
        String ts = p.getPaymentDate() == null ? "null"
                : p.getPaymentDate().withNano(0).toString();
        String key = p.getCustomerId() + "|" + ts + "|" + p.getPaymentStatus();
        buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
    }
    List<PaymentGroup> result = new ArrayList<>();
    for (List<PaymentResponseDTO> bucket : buckets.values()) {
        PaymentResponseDTO first = bucket.get(0);
        BigDecimal total = bucket.stream()
                .map(PaymentResponseDTO::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.add(PaymentGroup.builder()
                .paymentIds(bucket.stream().map(PaymentResponseDTO::getPaymentId).toList())
                .bookingIds(bucket.stream().map(PaymentResponseDTO::getBookingId).toList())
                .customerId(first.getCustomerId())
                .totalAmount(total)
                .paymentStatus(first.getPaymentStatus())
                .paymentDate(first.getPaymentDate())
                .seatCount(bucket.size())
                .build());
    }
    result.sort((a, b) -> {
        if (a.getPaymentDate() == null && b.getPaymentDate() == null) return 0;
        if (a.getPaymentDate() == null) return 1;
        if (b.getPaymentDate() == null) return -1;
        return b.getPaymentDate().compareTo(a.getPaymentDate());
    });
    return result;
}
```

Grouping logic:
1. Strip nanoseconds off `paymentDate` (so two payments at the same "second" bucket together, even if nanos differ).
2. Build a key `customerId|timestamp|status`. Same customer paying for the same batch of seats at essentially the same moment → same key.
3. Use `LinkedHashMap` to preserve insertion order while bucketing.
4. Convert each bucket to a `PaymentGroup`, summing amounts and collecting ids.
5. Sort newest-first (descending by `paymentDate`), with nulls at the bottom.

The template then iterates `paymentGroups` and shows one row per group with `isGroup()` determining whether to show "Download Ticket" or "Download Group Ticket".

#### showCheckout + showCheckoutAll

```java
@GetMapping("/view/payments/pay/{bookingId}")
public String showCheckout(@PathVariable Integer bookingId, Model model) {
    @SuppressWarnings("rawtypes")
    Map booking = bookingApiClient.getById(bookingId);
    BigDecimal fare = resolveFareFromBooking(booking);
    Object seatNumber = booking == null ? null : booking.get("seatNumber");
    model.addAttribute("bookingIds", String.valueOf(bookingId));
    model.addAttribute(ATTR_SEAT_COUNT, 1);
    model.addAttribute("seatNumbers", String.valueOf(seatNumber));
    model.addAttribute("amount", fare);
    model.addAttribute("customers", customerApiClient.getAll());
    return "payment/checkout";
}

@GetMapping("/view/payments/pay-all")
public String showCheckoutAll(@RequestParam String bookingIds,
                              @RequestParam(required = false) Integer customerId,
                              Model model) {
    String[] ids = bookingIds.split(",");
    BigDecimal totalFare = BigDecimal.ZERO;
    StringBuilder seatNums = new StringBuilder();
    for (String idStr : ids) {
        @SuppressWarnings("rawtypes")
        Map booking = bookingApiClient.getById(Integer.parseInt(idStr.trim()));
        BigDecimal fare = resolveFareFromBooking(booking);
        if (fare != null) totalFare = totalFare.add(fare);
        Object seat = booking == null ? null : booking.get("seatNumber");
        if (!seatNums.isEmpty()) seatNums.append(", ");
        seatNums.append(seat);
    }
    model.addAttribute("bookingIds", bookingIds);
    model.addAttribute(ATTR_SEAT_COUNT, ids.length);
    model.addAttribute("seatNumbers", seatNums.toString());
    model.addAttribute("amount", totalFare);
    model.addAttribute("preSelectedCustomerId", customerId);
    model.addAttribute("customers", customerApiClient.getAll());
    return "payment/checkout";
}
```

Two checkout endpoints — one for a single seat (single booking ID in path) and one for multi-seat (CSV of booking IDs in query param). Both feed the same `payment/checkout` template. `showCheckoutAll` sums fares and concatenates seat numbers into a display string.

#### processPaymentView — fan-out pattern

```java
@PostMapping("/view/payments/process")
public String processPaymentView(@RequestParam String bookingIds,
                                 @RequestParam Integer customerId,
                                 @RequestParam BigDecimal amount,
                                 RedirectAttributes ra) {
    try {
        String[] idStrs = bookingIds.split(",");
        List<Integer> bookingIdList = new ArrayList<>();
        for (String s : idStrs) bookingIdList.add(Integer.parseInt(s.trim()));

        int n = bookingIdList.size();
        if (n == 0) throw new IllegalArgumentException("No bookings supplied");

        BigDecimal perSeatFare = amount.divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);

        List<PaymentResponseDTO> responses = new ArrayList<>();
        for (Integer bid : bookingIdList) {
            PaymentRequestDTO req = new PaymentRequestDTO(bid, customerId, perSeatFare);
            responses.add(paymentApiClient.processPayment(req));
        }

        List<Integer> paymentIds = responses.stream()
                .map(PaymentResponseDTO::getPaymentId)
                .toList();
        Integer firstPaymentId = paymentIds.get(0);

        ra.addFlashAttribute(ATTR_ALL_PAYMENT_IDS, paymentIds);
        ra.addFlashAttribute("totalAmount", amount);
        ra.addFlashAttribute("perSeatFare", perSeatFare);
        ra.addFlashAttribute(ATTR_SEAT_COUNT, n);
        ra.addFlashAttribute("allBookingIds", bookingIds);
        return "redirect:/view/payments/success/" + firstPaymentId;
    } catch (BackendException ex) {
        ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        return "redirect:/view/payments/pay-all?bookingIds=" + bookingIds;
    } catch (Exception ex) {
        ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
        return "redirect:/view/payments/pay-all?bookingIds=" + bookingIds;
    }
}
```

Key design:
- N bookings → N backend `POST /api/payments` calls. Each booking gets its own Payment row — that's what the backend schema expects.
- Per-seat fare = `amount / n`, rounded HALF_UP to 2 decimals. Slight rounding can produce a cent discrepancy on the total, but that's acceptable.
- On failure of *any* of the N calls, the whole redirect goes back to the checkout with an error flash. There's no partial-rollback logic — if seat 1 paid and seat 2 failed, seat 1's payment persists. A production system would wrap this in a backend transaction endpoint instead.

#### showSuccess

```java
@GetMapping("/view/payments/success/{paymentId}")
public String showSuccess(@PathVariable Integer paymentId, Model model) {
    PaymentResponseDTO payment = paymentApiClient.getById(paymentId);
    model.addAttribute("payment", payment);

    if (!model.containsAttribute(ATTR_ALL_PAYMENT_IDS)) {
        List<PaymentResponseDTO> siblings = paymentApiClient.getAll().stream()
                .filter(p -> Objects.equals(p.getCustomerId(), payment.getCustomerId())
                        && p.getPaymentDate() != null && payment.getPaymentDate() != null
                        && p.getPaymentDate().withNano(0).equals(payment.getPaymentDate().withNano(0))
                        && p.getPaymentStatus() == payment.getPaymentStatus())
                .toList();
        List<Integer> paymentIds = siblings.stream().map(PaymentResponseDTO::getPaymentId).toList();
        BigDecimal total = siblings.stream()
                .map(PaymentResponseDTO::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute(ATTR_ALL_PAYMENT_IDS, paymentIds.isEmpty() ? List.of(paymentId) : paymentIds);
        model.addAttribute("totalAmount", total.signum() > 0 ? total : payment.getAmount());
        model.addAttribute("perSeatFare", payment.getAmount());
        model.addAttribute(ATTR_SEAT_COUNT, Math.max(1, siblings.size()));
    }
    return "payment/success";
}
```

Fallback logic like `showConfirmation` — if flash attrs are present (normal POST→redirect), use them; otherwise, re-compute siblings from `getAll()` using the same grouping key. `siblings.size()` guarantees at least 1 via `Math.max(1, ...)`.

#### resolveFareFromBooking

```java
@SuppressWarnings("rawtypes")
private BigDecimal resolveFareFromBooking(Map booking) {
    if (booking == null) return null;
    Object tripIdObj = booking.get("tripId");
    if (!(tripIdObj instanceof Number n)) return null;
    try {
        TripDTO trip = tripApiClient.getById(n.intValue());
        return trip != null ? trip.getFare() : null;
    } catch (Exception ex) {
        return null;
    }
}
```

Booking JSON doesn't carry fare, so we resolve it by looking up the trip. Fails softly (returns `null`) if anything goes wrong — the template handles null by hiding the amount field.

#### showPaymentTicketForm

```java
@GetMapping("/view/payments/ticket")
public String showPaymentTicketForm() {
    return "payment/ticket-download";
}
```

Static form — user enters paymentId, form posts to backend `/api/payments/{id}/ticket` directly for the PDF.

### Viva questions — PaymentViewController

**Q1. Why split `showCheckout` into two endpoints (`/pay/{id}` and `/pay-all`)?**
A. Different callers. The Payments list page links individual rows via `/pay/{id}` when a row is a single seat. Multi-seat group rows and the post-booking confirmation redirect via `/pay-all?bookingIds=1,2,3`. Both reuse the same template, so the divergence is controller-only.

**Q2. What happens if `processPaymentView` fails halfway through (payment 1 succeeds, payment 2 fails)?**
A. The first payment is committed on the backend; the loop throws and we redirect to the checkout with an error. There's no rollback. A robust fix would move the fan-out into a backend transactional endpoint (`POST /api/payments/batch`) so the whole operation is atomic.

### 7.6 ReviewViewController

**Path:** `src/main/java/com/busfrontend/controller/ReviewViewController.java`

Three clients — review, customer, trip.

```java
@Controller
@RequiredArgsConstructor
public class ReviewViewController {

    private final ReviewApiClient reviewApiClient;
    private final CustomerApiClient customerApiClient;
    private final TripApiClient tripApiClient;

    @GetMapping("/view/reviews")
    public String listReviews(Model model) {
        List<Map<String, Object>> reviews = reviewApiClient.getAll();
        model.addAttribute("reviews", reviews);
        return "review/reviews";
    }

    @GetMapping("/view/reviews/add")
    public String showAddReviewForm(Model model) {
        if (!model.containsAttribute("review")) {
            model.addAttribute("review", new ReviewDTO());
        }
        model.addAttribute("customers", customerApiClient.getAll());
        model.addAttribute("trips", tripApiClient.getAllTrips());
        return "review/add-review";
    }

    @PostMapping("/view/reviews/save")
    public String saveReview(@ModelAttribute("review") ReviewDTO reviewDTO,
                             RedirectAttributes redirectAttributes) {
        try {
            reviewApiClient.create(reviewDTO);
            redirectAttributes.addFlashAttribute("success", "Review created successfully");
            return "redirect:/view/reviews";
        } catch (BackendException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("review", reviewDTO);
            return "redirect:/view/reviews/add";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("review", reviewDTO);
            return "redirect:/view/reviews/add";
        }
    }
}
```

Subtle but important: **preserve-flash-review pattern.**

In `showAddReviewForm`:
```java
if (!model.containsAttribute("review")) {
    model.addAttribute("review", new ReviewDTO());
}
```

In `saveReview` catch block:
```java
redirectAttributes.addFlashAttribute("review", reviewDTO);
```

Together these mean: when validation fails and we redirect back to the add form, the user's entered data is re-populated. Without this you'd greet the user with an empty form every time they hit an error — awful UX.

### 7.7 CustomerViewController

**Path:** `src/main/java/com/busfrontend/controller/CustomerViewController.java`

Two clients — customer, address. Its `saveCustomer` is the canonical "nested entity creation" example.

```java
@PostMapping("/view/customers/save")
public String saveCustomer(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String phone,
                           @RequestParam String address,
                           @RequestParam String city,
                           @RequestParam String state,
                           @RequestParam String zipCode,
                           RedirectAttributes ra) {
    try {
        AddressDTO addr = AddressDTO.builder()
                .address(address)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .build();
        AddressDTO savedAddr = addressApiClient.create(addr);

        CustomerRequestDTO dto = new CustomerRequestDTO();
        dto.setName(name);
        dto.setEmail(email);
        dto.setPhone(phone);
        dto.setAddressId(savedAddr.getAddressId());
        customerApiClient.create(dto);

        ra.addFlashAttribute("message", "Customer added successfully.");
    } catch (BackendException ex) {
        ra.addFlashAttribute("error", ex.getMessage());
    } catch (Exception ex) {
        ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/view/customers";
}
```

Order matters: **address first**, then customer referencing the new `addressId`. If the address POST fails, the customer never attempts, and the user sees the address error. If the customer POST fails *after* the address succeeded, you get an orphaned address row — minor data sloppiness but not a correctness bug.

### 7.8 AddressViewController

**Path:** `src/main/java/com/busfrontend/controller/AddressViewController.java`

Simple CRUD, no error handling. Five mappings — list, add-form, save, edit-form, update. No delete (`delete` exists on the API client but isn't exposed in the UI).

```java
@PostMapping("/view/addresses/save")
public String saveAddress(@RequestParam String address,
                          @RequestParam String city,
                          @RequestParam String state,
                          @RequestParam String zipCode) {
    AddressDTO addr = AddressDTO.builder()
            .address(address)
            .city(city)
            .state(state)
            .zipCode(zipCode)
            .build();
    addressApiClient.create(addr);
    return REDIRECT_VIEW_ADDRESSES;
}
```

Lean — no try/catch, no flash. If something breaks, the global error page takes over. Acceptable for an internal CRUD screen; for a customer-facing one you'd add the usual safety nets.

### 7.9 AgencyViewController

**Path:** `src/main/java/com/busfrontend/controller/AgencyViewController.java`

Four mappings — list, add, save, edit, update. Uses `@ModelAttribute AgencyRequestDTO dto` for form binding. No try/catch (admin-facing).

### 7.10 BusViewController

**Path:** `src/main/java/com/busfrontend/controller/BusViewController.java`

Two clients — bus, office (for the office dropdown). Classic CRUD with `@ModelAttribute BusRequestDTO dto`.

```java
@GetMapping("/view/buses/add")
public String showAddForm(Model model) {
    model.addAttribute("bus", new BusRequestDTO());
    model.addAttribute("offices", agencyOfficeApiClient.getAll());
    return "bus/add-bus";
}
```

`offices` populates the `<select name="officeId">` dropdown in the form.

### 7.11 DriverViewController

**Path:** `src/main/java/com/busfrontend/controller/DriverViewController.java`

Three clients — driver, office, address. Mirrors `BusViewController` structurally, with an extra `addresses` model attribute for the address dropdown.

### 7.12 OfficeViewController

**Path:** `src/main/java/com/busfrontend/controller/OfficeViewController.java`

Most interesting ordinary controller — uses `@RequestMapping("/view/offices")` at class level to share the prefix, and has a **request→response mapping dance** in the edit form:

```java
@GetMapping("/edit/{id}")
public String showEditForm(@PathVariable Integer id, Model model) {
    OfficeResponseDTO resp = officeApiClient.getById(id);
    // Map response -> request for form binding.
    OfficeRequestDTO form = new OfficeRequestDTO();
    if (resp != null) {
        form.setAgencyId(resp.getAgencyId());
        form.setOfficeMail(resp.getOfficeMail());
        form.setOfficeContactPersonName(resp.getOfficeContactPersonName());
        form.setOfficeContactNumber(resp.getOfficeContactNumber());
        form.setOfficeAddressId(resp.getOfficeAddressId());
    }
    model.addAttribute("office", form);
    model.addAttribute("officeId", id);
    model.addAttribute("agencies", agencyApiClient.getAll());
    model.addAttribute("addresses", addressApiClient.getAll());
    return "office/update-office";
}
```

Why copy fields? The form is bound to `OfficeRequestDTO` (which matches the PUT endpoint's request body), but the API returns `OfficeResponseDTO`. Hand-copying the six overlapping fields is the cleanest way to keep the two shapes apart.

```java
@PostMapping("/save")
public String saveOffice(@ModelAttribute("office") OfficeRequestDTO dto,
                         RedirectAttributes ra) {
    try {
        officeApiClient.create(dto);
        ra.addFlashAttribute("message", "Office added successfully!");
    } catch (BackendException ex) {
        ra.addFlashAttribute("error", ex.getMessage());
    } catch (Exception ex) {
        ra.addFlashAttribute("error", ex.getMessage());
    }
    return REDIRECT;
}
```

Same flash-message pattern as Trip and Review.

### Viva questions — controllers

**Q1. Why inject clients as `final` fields with `@RequiredArgsConstructor`?**
A. `final` guarantees the reference never changes after construction — thread-safe and intention-clear. `@RequiredArgsConstructor` generates a constructor for exactly those `final` fields, which Spring uses for constructor injection (preferred over field injection because it makes dependencies explicit and enables easier testing).

**Q2. Why do some controllers catch `BackendException` and others don't?**
A. User-facing flows (Trip create, Booking create, Payment process, Review create, Office create) need to show friendly error messages — they catch `BackendException` and flash `ex.getMessage()`. Admin-only CRUD (Address, Agency, Bus, Driver) skip the try/catch because the fallback error page is acceptable for internal users.

**Q3. Why return `String` from every handler and not `ModelAndView`?**
A. Returning a `String` view name is the minimal, declarative way in modern Spring MVC. The `Model` param (or `RedirectAttributes`) handles data; the return value handles view resolution. `ModelAndView` is the older, verbose API — same capability, more ceremony.

---

## End of Part B

Part 3 (if requested) would cover templates, static resources, and end-to-end request flows. Everything above reflects the current code in `C:\Users\Sarthak\OneDrive\Desktop\bus-ticket-frontend` — no speculation.
# PART C — TEMPLATES, TESTING, VIVA

> Third part of the Bus Ticket Booking System developer guide. This document focuses on the view layer (Thymeleaf), error handling, the 50-test suite, 100 viva questions with detailed answers, deployment, and known gotchas / future work.

---

## Section 8 — Thymeleaf Templates Overview

The frontend ships with **47 Thymeleaf HTML templates** under `src/main/resources/templates/`. They are organized by module — each module corresponds to one backend aggregate and one frontend view controller.

### 8.1 Template Inventory

Grouped by module folder:

| Module | Files | Pages |
|--------|-------|-------|
| `home/` | `home.html` | Landing page |
| `trip/` | `trips.html`, `add.html`, `edit.html` | Admin CRUD for trips |
| `booking/` | `trips.html`, `select-seat.html`, `confirmation.html`, `ticket-download.html`, `group-ticket-download.html` | Customer-facing booking flow |
| `payment/` | `list.html`, `make-payment.html`, `success.html`, `receipt.html` | Payment capture + receipt |
| `review/` | `list.html`, `add.html`, `edit.html`, `trip-reviews.html` | Review CRUD |
| `customer/` | `list.html`, `add.html`, `edit.html`, `profile.html` | Customer CRUD |
| `address/` | `list.html`, `add.html`, `edit.html` | Address CRUD |
| `agency/` | `list.html`, `add.html`, `edit.html` | Agency CRUD |
| `bus/` | `list.html`, `add.html`, `edit.html`, `detail.html` | Bus CRUD |
| `driver/` | `list.html`, `add.html`, `edit.html` | Driver CRUD |
| `office/` | `list.html`, `add.html`, `edit.html` | Office CRUD |
| `route/` | `list.html`, `search.html`, `add.html`, `edit.html` | Route CRUD + search |
| `fragments/` | `header.html`, `footer.html` | Shared chrome |
| `error/` | `404.html`, `500.html` | Error pages |
| `members/` | `members.html` | Static team page |
| `team/` | `team.html` | Team info |

**Total: 47 files.** Each module has a one-to-one relationship with the controller under `com.busfrontend.controller` and the DTOs under `com.busfrontend.dto`.

### 8.2 Module-by-Module Mapping

#### `home/`
- Pages: `home.html` (single landing page)
- Controller: `HomePageController.java`
- DTOs: None — static hero + links
- Notable: Uses `th:href="@{/view/bookings}"` to link into the booking flow

#### `trip/`
- Pages: `trips.html` (list), `add.html`, `edit.html`
- Controller: `TripViewController.java`
- DTOs: `TripDTO`, `BusResponseDTO` (for bus dropdown), `RouteDTO` (for route dropdown)
- Admin-facing. The customer equivalent is `booking/trips.html`.

#### `booking/`
- Pages:
  - `trips.html` — lists trips with "Book Now" buttons (customer view)
  - `select-seat.html` — interactive seat map
  - `confirmation.html` — post-booking summary
  - `ticket-download.html` / `group-ticket-download.html` — printable tickets
- Controller: `BookingViewController.java`
- DTOs: `BookingRequestDTO` (form binding), `BookingResponseDTO`, `TripDTO`, `BusResponseDTO`, `CustomerResponseDTO`

#### `payment/`
- Pages: `list.html`, `make-payment.html`, `success.html`, `receipt.html`
- Controller: `PaymentViewController.java`
- DTOs: `PaymentRequestDTO`, `PaymentResponseDTO`, `BookingResponseDTO`

#### `review/`
- Pages: `list.html`, `add.html`, `edit.html`, `trip-reviews.html`
- Controller: `ReviewViewController.java`
- DTOs: `ReviewDTO`, `CustomerResponseDTO`, `TripDTO`

#### `customer/`
- Pages: `list.html`, `add.html`, `edit.html`, `profile.html`
- Controller: `CustomerViewController.java`
- DTOs: `CustomerRequestDTO`, `CustomerResponseDTO`, `AddressDTO`

#### `address/`, `agency/`, `bus/`, `driver/`, `office/`, `route/`
Uniform shape — `list.html` + `add.html` + `edit.html`, each bound to its corresponding `*ViewController` and `*DTO`. The `bus/` module has an extra `detail.html` page showing seat layout; the `route/` module has `search.html`.

#### `fragments/`
Two shared fragments pulled in by every content template via `th:replace`:

```html
<head th:replace="~{fragments/header :: header}">...</head>
<div th:replace="~{fragments/header :: navbar}"></div>
...
<footer th:replace="~{fragments/footer :: footer}"></footer>
```

- `header.html` defines two named fragments — `header` (meta/CSS/Bootstrap/Bootstrap Icons) and `navbar` (top nav with links to Home, Trips, Customers, etc.)
- `footer.html` defines a single `footer` fragment with copyright and social links.

#### `error/`
- `404.html` — shown when a resource is missing (wired via Spring Boot's `ErrorController` + `server.error.path`)
- `500.html` — shown on unhandled exceptions

Spring Boot auto-resolves `templates/error/404.html` and `templates/error/500.html` based on HTTP status. No extra controller is needed because `BasicErrorController` is on the classpath and `spring.mvc.problemdetails.enabled=false` keeps it in HTML mode.

### 8.3 A Real Template: `booking/select-seat.html`

The seat-selection page is the richest template — it demonstrates fragments, forms, iteration, conditionals, and flash errors all at once.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/header :: header}"><title>BusBooking</title></head>
<body>
<div th:replace="~{fragments/header :: navbar}"></div>

<div class="container my-4">
    <!-- Flash error from RedirectAttributes -->
    <div th:if="${error}" class="alert alert-danger alert-dismissible fade show">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <span th:text="${error}">Error</span>
    </div>

    <!-- Trip header (data from Model) -->
    <div class="card mb-4">
        <div class="card-body">
            <h5>
                <span th:text="${trip.fromCity}">Origin</span>
                <i class="bi bi-arrow-right text-primary mx-2"></i>
                <span th:text="${trip.toCity}">Destination</span>
            </h5>
            <p th:text="'Rs. ' + ${trip.fare}"></p>
        </div>
    </div>

    <!-- Booking form -->
    <form th:action="@{/view/bookings/book}" method="post" id="bookingForm">
        <input type="hidden" name="tripId" th:value="${trip.tripId}">

        <select name="customerId" required>
            <option value="">-- Select Customer --</option>
            <option th:each="c : ${customers}"
                    th:value="${c.id}"
                    th:text="${c.name + ' (' + c.email + ')'}"></option>
        </select>

        <!-- Seat grid -->
        <div class="seat-grid">
            <div th:each="seat : ${allSeats}"
                 th:classappend="${bookedSeats.contains(seat)} ? 'booked' : 'available'">
                <input type="checkbox"
                       name="seatNumbers"
                       th:value="${seat}"
                       th:disabled="${bookedSeats.contains(seat)}">
                <label th:text="${seat}"></label>
            </div>
        </div>

        <button type="submit" class="btn btn-primary">Confirm Booking</button>
    </form>
</div>
</body>
</html>
```

**Key directives at work:**

| Directive | What it does |
|-----------|--------------|
| `th:replace="~{fragments/header :: header}"` | Inlines the `header` fragment, replacing the current tag entirely |
| `th:if="${error}"` | Renders the enclosing element only if the `error` flash attribute is non-null and non-empty |
| `th:text="${trip.fromCity}"` | Escapes and writes the value — the literal `"Origin"` inside the tag is ignored at runtime (it's a design-time placeholder) |
| `th:each="c : ${customers}"` | Iterates the model attribute `customers`, binding each element to `c` |
| `th:value="${c.id}"` | Sets an attribute from an expression |
| `th:action="@{/view/bookings/book}"` | URL expression — the `@{...}` syntax adds context path prefixes automatically |
| `th:classappend="... ? 'booked' : 'available'"` | Appends a class based on a ternary expression |
| `th:disabled="${bookedSeats.contains(seat)}"` | Boolean attribute set only when expression is true |
| `th:object` + `th:field` (used in CRUD forms) | Two-way bind a form to a DTO: `th:object="${customerForm}"` on `<form>`, then `th:field="*{name}"` on each input |

When the form posts, Spring MVC binds:
- `tripId` → method parameter `@RequestParam Integer tripId`
- `seatNumbers` (multi-value checkbox) → `@RequestParam List<Integer> seatNumbers`
- `customerId` → `@RequestParam Integer customerId`

The controller then calls `BookingApiClient.createBooking(...)` and either:
- Redirects to `/view/bookings/confirmation/{id}` with the response flashed, OR
- Catches `BackendException` and redirects back to `/view/bookings/trip/{id}` with a flash `error` attribute that the same template renders via `th:if="${error}"`.

---

## Section 9 — Error Handling

### 9.1 `BackendException`

Every API client translates non-2xx HTTP responses into a single custom unchecked exception:

```java
package com.busfrontend.client;

public class BackendException extends RuntimeException {
    private final int status;

    public BackendException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}
```

Defined once in `AbstractApiClient`; thrown by its `handleHttpError(HttpStatusCodeException)` helper when Jackson sees a `{"message":"..."}` body, or falls back to `ex.getStatusText()` if the body is not JSON.

### 9.2 How it bubbles up

View controllers wrap every client call in a `try/catch`:

```java
@PostMapping("/book")
public String bookSeats(@RequestParam Integer tripId,
                        @RequestParam List<Integer> seatNumbers,
                        @RequestParam Integer customerId,
                        RedirectAttributes ra) {
    try {
        BookingResponseDTO resp = bookingApiClient.createBooking(
            BookingRequestDTO.builder()
                .tripId(tripId).customerId(customerId).seatNumbers(seatNumbers).build());
        ra.addFlashAttribute("booking", resp);
        ra.addFlashAttribute("seatNumbers", seatNumbers);
        return "redirect:/view/bookings/confirmation/" + resp.getBookingIds().get(0);
    } catch (BackendException ex) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/view/bookings/trip/" + tripId;
    }
}
```

### 9.3 Displaying the error

The re-rendered page reads the flash attribute:

```html
<div th:if="${error}" class="alert alert-danger">
    <span th:text="${error}"></span>
</div>
```

Flash attributes survive exactly **one redirect** and are then removed — preventing stale errors from sticking around after a refresh.

### 9.4 Connection failures (backend down)

`RestTemplate` is constructed with explicit timeouts in `RestTemplateConfig`:

```java
return builder
    .setConnectTimeout(Duration.ofSeconds(5))
    .setReadTimeout(Duration.ofSeconds(10))
    .build();
```

When the backend is down:
- `ResourceAccessException` is thrown (wrapping `ConnectException` / `SocketTimeoutException`).
- `AbstractApiClient.execute(...)` does **not** catch `ResourceAccessException` directly — it propagates.
- Spring Boot's default error handler catches it and renders `error/500.html`.

**User-facing behavior:** full-page 500 error with a "Try again" link back to home. Optional future work: add a global `@ControllerAdvice` that catches `ResourceAccessException` and shows a friendlier "Our services are temporarily unavailable" banner.

### 9.5 HTTP 4xx vs 5xx from backend

Both translate to `BackendException` with the appropriate `status` field:
- 4xx (`HttpClientErrorException`) — typically validation errors, "resource not found", "seat already booked". Message from JSON body if available.
- 5xx (`HttpServerErrorException`) — backend exploded. Message usually generic.

Because both map to the same exception type, view controllers don't need to differentiate at the catch site — they show the message verbatim to the user.

### 9.6 User-facing behavior examples

| Situation | User sees |
|-----------|-----------|
| Seat already booked (backend 400) | Red alert: "Seats 5, 7 are already booked" on `select-seat.html` |
| Customer not found (backend 404) | Red alert: "Customer with id 99 not found" on form page |
| Backend down (timeout) | Full `500.html` page (generic) |
| Backend 500 (bug) | Red alert with `ex.getMessage()` (e.g. "Internal server error") |
| Frontend view missing | Spring renders `error/404.html` |

---

## Section 10 — Testing Deep Dive

### 10.1 Dependencies & Setup

The project uses **JUnit 5 + Spring Boot Test**, pulled in by the single starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

This transitively brings:
- JUnit Jupiter 5 (annotations `@Test`, `@BeforeEach`, `@DisplayName`)
- Mockito 5 (`@Mock`, `@MockBean`, `when(...).thenReturn(...)`, `verify(...)`)
- AssertJ (`assertThat(...).isEqualTo(...)`, fluent chain-style assertions)
- Spring Test (`MockRestServiceServer`, `MockMvc`)
- JsonPath + Hamcrest (for response body matchers, not heavily used here)
- `ReflectionTestUtils` — setting `private` fields without going through Spring DI

**Test resources:** `src/test/resources/application.properties` overrides with:
```properties
backend.base-url=http://localhost:8080
spring.thymeleaf.cache=false
logging.level.com.busfrontend=DEBUG
```
(Inherits everything else from the main `application.properties`.)

### 10.2 API Client Tests (6 files, ~35 tests)

All API client tests follow the same shape:
1. Build a raw `RestTemplate` (no Spring context).
2. Wrap it in a `MockRestServiceServer`.
3. Instantiate the client via its constructor, passing the `RestTemplate`.
4. Use `ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8080")` because `@Value` wiring only happens inside a Spring context.
5. Set up expectations: `server.expect(requestTo(url)).andExpect(method(...)).andRespond(...)`.
6. Invoke the client method.
7. Assert the returned value with AssertJ.
8. Call `server.verify()` to confirm the expected request was actually made.

#### `AbstractApiClientErrorTranslationTest` — 6 tests
Exercises the shared error translation logic in the base class:
- 400 with JSON body → `BackendException(400, "message from body")`
- 404 with JSON body → `BackendException(404, "...")`
- 500 with JSON body → `BackendException(500, "...")`
- Non-JSON error body → falls back to `getStatusText()`
- Empty error body → uses status reason phrase
- Jackson-unparseable JSON → safe fallback (no second exception)

#### `TripApiClientTest` — 6 tests
- `getAllTripsReturnsList` — GET `/api/trips` returns array, maps to `List<TripDTO>`
- `getByIdReturnsSingleTrip` — GET `/api/trips/7` maps to single `TripDTO`
- `getByIdThrowsBackendExceptionOn404` — 404 translated correctly
- `searchAddsQueryParams` — `search("Mumbai", "Pune")` hits `/api/trips/search?from=Mumbai&to=Pune`
- `createPostsTripAndReturnsCreated` — POST + returns created trip
- `updatePutsTripAndReturnsUpdated` — PUT + returns updated trip

#### `BookingApiClientTest` — 6 tests
- `getAllBookings`, `getByIdReturnsSingle`, `createBookingReturnsIds` (multi-seat happy path), `getByTripReturnsSeats`, `createBookingPropagatesBackendValidationError` (409 Seat already booked), `deleteBookingHitsDelete`

#### `PaymentApiClientTest` — 6 tests
- `getAll`, `getById`, `createPayment` (POST with `PaymentRequestDTO`), `getByBooking` (lookup by booking id), `downloadReceiptReturnsPdfBytes` (binary response, checks `byte[]` round-trip), `createPayment400Failure`

#### `ReviewApiClientTest` — 5 tests
- `getAllReviews`, `getById`, `create`, `getByTrip`, `deleteReview`

#### `CustomerApiClientTest` — 5 tests
- `getAll`, `getById`, `createCustomer`, `updateCustomer`, `deleteCustomer`

#### Example — full `TripApiClientTest`

```java
class TripApiClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private TripApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new TripApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8080");
    }

    @Test
    void getAllTripsReturnsList() {
        String body = "[{\"tripId\":1,\"fromCity\":\"Mumbai\",\"toCity\":\"Pune\",\"fare\":500}," +
                      "{\"tripId\":2,\"fromCity\":\"Pune\",\"toCity\":\"Nashik\",\"fare\":400}]";
        server.expect(requestTo("http://localhost:8080/api/trips"))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<TripDTO> trips = client.getAllTrips();

        assertThat(trips).hasSize(2);
        assertThat(trips.get(0).getFromCity()).isEqualTo("Mumbai");
        server.verify();
    }

    @Test
    void getByIdThrowsBackendExceptionOn404() {
        server.expect(requestTo("http://localhost:8080/api/trips/999"))
              .andRespond(withStatus(HttpStatus.NOT_FOUND)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body("{\"message\":\"Trip not found\"}"));

        assertThatThrownBy(() -> client.getById(999))
            .isInstanceOf(BackendException.class)
            .hasMessage("Trip not found");

        server.verify();
    }
}
```

**Key APIs explained:**

| API | Purpose |
|-----|---------|
| `MockRestServiceServer.createServer(restTemplate)` | Swaps the `RestTemplate`'s request factory with a mock one that records and matches expectations. No real HTTP happens. |
| `server.expect(requestTo(url))` | Sets up one expected outgoing request. Ordered by default. |
| `.andExpect(method(GET))` | Further constrains what the matched request must look like |
| `.andRespond(withSuccess(body, APPLICATION_JSON))` | Canned response body/headers/status |
| `server.verify()` | Asserts every expected request was consumed. Fails the test if any were missed. |
| `ReflectionTestUtils.setField(bean, "field", value)` | Writes to a `private` field. Used for `@Value`-injected `baseUrl` since these unit tests don't start a Spring context. |

### 10.3 Controller Tests (5 files, ~16 tests)

Shape:
1. Annotate class with `@WebMvcTest(SomeController.class)` — loads **only** that controller + MVC infrastructure (no services, no data source).
2. `@MockBean` every API client the controller depends on.
3. `@Autowired MockMvc mockMvc` — gives us an in-process HTTP simulator.
4. Set up Mockito stubs: `when(client.getById(1)).thenReturn(fakeDTO)`.
5. `mockMvc.perform(get(...))...andExpect(...)`.

#### `HomePageControllerTest` — 1 test
- `homeRendersHomeTemplate` — GET `/` returns 200, view name `home/home`.

#### `TripViewControllerTest` — 3 tests
- `listTripsRendersListView`
- `viewTripDetailRendersDetailView`
- `viewTripDetailFlashesErrorOnBackendException`

#### `BookingViewControllerTest` — 4 tests
- `showSeatSelectionRendersSelectSeatView`
- `bookSeatsRedirectsToConfirmationOnSuccess`
- `bookSeatsFlashesErrorAndRedirectsBackOnBackendFailure`
- `listAvailableTripsFiltersOutFullTrips`

#### `PaymentViewControllerTest` — 4 tests
- `listPaymentsRendersList`
- `showMakePaymentViewRendersForm`
- `processPaymentRedirectsOnSuccess`
- `processPaymentFlashesErrorOnBackendFailure`

#### `ReviewViewControllerTest` — 4 tests
- `listReviewsRendersList`
- `showAddReviewFormRendersForm`
- `createReviewRedirectsOnSuccess`
- `createReviewFlashesErrorOnBackendException`

#### Example — full `BookingViewControllerTest`

```java
@WebMvcTest(BookingViewController.class)
class BookingViewControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private BookingApiClient bookingApiClient;
    @MockBean private TripApiClient tripApiClient;
    @MockBean private CustomerApiClient customerApiClient;
    @MockBean private BusApiClient busApiClient;

    @Test
    void showSeatSelectionRendersSelectSeatView() throws Exception {
        TripDTO trip = TripDTO.builder().tripId(1).busId(10)
            .fromCity("A").toCity("B")
            .fare(new BigDecimal("100")).availableSeats(30).build();
        BusResponseDTO bus = BusResponseDTO.builder().busId(10).capacity(30).build();

        when(tripApiClient.getById(1)).thenReturn(trip);
        when(busApiClient.getById(10)).thenReturn(bus);
        when(bookingApiClient.getByTrip(1)).thenReturn(List.of());
        when(customerApiClient.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/view/bookings/trip/1"))
               .andExpect(status().isOk())
               .andExpect(view().name("booking/select-seat"))
               .andExpect(model().attributeExists("trip", "allSeats", "bookedSeats", "customers"));
    }

    @Test
    void bookSeatsFlashesErrorAndRedirectsBackOnBackendFailure() throws Exception {
        when(bookingApiClient.createBooking(any()))
            .thenThrow(new BackendException(400, "Seats already booked"));

        mockMvc.perform(post("/view/bookings/book")
                .param("tripId", "1")
                .param("seatNumbers", "1")
                .param("customerId", "5"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/view/bookings/trip/1"))
            .andExpect(flash().attributeExists("error"));
    }
}
```

**Annotations & matchers explained:**

| Element | Purpose |
|---------|---------|
| `@WebMvcTest(X.class)` | Slice test: loads `X`, all `@ControllerAdvice`, `@JsonComponent`, `HandlerInterceptor`s, Jackson, MVC; **does not** load `@Service` / `@Repository` / `@Component` |
| `@MockBean` | Creates a Mockito mock and registers it in the Spring context — replacing the real bean. Differs from `@Mock` (pure Mockito, no Spring awareness). |
| `MockMvc.perform(get(url))` | Simulates an HTTP request without a real servlet container |
| `status().isOk()` | Asserts HTTP 200 |
| `status().is3xxRedirection()` | Asserts 3xx redirect |
| `view().name("booking/select-seat")` | Asserts the returned logical view name |
| `model().attributeExists("trip")` | Asserts the model contains a given attribute |
| `flash().attributeExists("error")` | Asserts a redirect flash attribute is present |
| `redirectedUrl("/view/bookings/trip/1")` | Asserts the `Location` redirect target |

### 10.4 Test count summary

- **6 API client tests** × avg ~5-6 tests each = ~35 tests
- **5 controller tests** × avg ~3 tests each = ~15 tests
- **Grand total: 50 tests across 11 files. All passing.**

Run locally (Windows):
```bash
./mvnw.cmd test
```
Or on Unix:
```bash
./mvnw test
```
Maven Surefire prints a per-class summary and a final `Tests run: 50, Failures: 0, Errors: 0, Skipped: 0`.

---

## Section 11 — Viva Questions

One hundred questions grouped in five buckets. Each answer is tied to this project wherever possible.

### 11.1 Architecture & Separation (Q1–Q20)

**Q1. Why did you separate the frontend from the backend?**
To decouple concerns. The backend exposes pure JSON APIs (reusable by any client — web, mobile, third-party). The frontend is a thin BFF that handles UI-specific concerns (view composition, form binding, session). This lets each side evolve independently — you can rewrite the UI without touching business logic.

**Q2. What is a BFF (Backend-for-Frontend)?**
A BFF is an intermediate server purpose-built for one UI. It aggregates backend calls, shapes responses for the UI, and handles UI concerns like sessions, CSRF, cookies. Our frontend Spring Boot app is a BFF for the Thymeleaf UI — it talks to the backend over REST and serves HTML to browsers.

**Q3. Why is no CORS configuration needed?**
Because the browser only ever talks to `localhost:8081` (the frontend). The browser never issues cross-origin requests directly to `localhost:8080`. All backend calls happen **server-to-server** via `RestTemplate`, which isn't bound by Same-Origin Policy. If we ever let the browser call the backend directly (e.g. from JavaScript), we'd need to add `@CrossOrigin` on the backend or a CORS config.

**Q4. How does the frontend authenticate to the backend?**
Currently it doesn't — the backend is open on localhost. For production we'd add a static API key or JWT: the frontend would attach `Authorization: Bearer <token>` via a `ClientHttpRequestInterceptor` on `RestTemplate`. The token itself lives in `application.properties` and is injected with `@Value`.

**Q5. What does the user see if the backend is down?**
`RestTemplate` throws `ResourceAccessException` after the configured 5-second connect / 10-second read timeout. Spring Boot's default error handler kicks in and renders `templates/error/500.html`. A future improvement is a global `@ControllerAdvice` mapping `ResourceAccessException` to a friendlier "Our service is temporarily unavailable" page.

**Q6. Monolith vs this split — pros and cons?**
*Monolith pros:* single deploy, no network hop, simpler transactions, no DTO duplication. *Monolith cons:* UI and API churn together, hard to swap UI frameworks, can't scale tiers independently. *Split pros:* independent scaling, UI can be re-written, API reusable for mobile. *Split cons:* extra network hop (~1ms localhost), DTO duplication, more moving parts.

**Q7. Could we have used React instead of Thymeleaf?**
Yes. The backend already exposes JSON endpoints — a React SPA would consume them directly. We chose Thymeleaf because (a) the team is more comfortable with Java, (b) server-side rendering means faster first paint with no Node toolchain, and (c) flash attributes + session-style forms are simpler than managing client-side state.

**Q8. What changes if we move the frontend to HTTPS (port 443)?**
Only two: (a) configure an SSL keystore in `server.ssl.*` properties (or terminate TLS at a reverse proxy like nginx), and (b) make sure any absolute URLs in templates use `https://`. The `@{...}` URL expressions handle scheme-relative linking automatically, so most templates need no change.

**Q9. Why two separate Spring Boot apps instead of one with two profiles?**
Because they have genuinely different dependencies (the backend needs JPA, MySQL driver, Spring Data; the frontend needs Thymeleaf, RestTemplate). Keeping them as separate Maven modules means each JAR is smaller, each app boots faster, and you can scale them independently.

**Q10. What happens if the frontend and backend DTOs drift?**
Jackson deserialization becomes forgiving (missing fields default to `null`, extra fields are ignored by default thanks to `spring.jackson.deserialization.fail-on-unknown-properties=false`). But semantically the UI may start showing stale data. Mitigation: run integration tests against a real backend, or extract the DTOs into a shared module.

**Q11. Why does the frontend have its own copy of DTOs?**
Two reasons: (1) no shared dependency means the two modules can compile and deploy independently, (2) frontend DTOs can carry UI-only fields (e.g. formatted display strings) without polluting the backend API contract. The cost is duplication — mitigated by keeping DTOs minimal and aligned with backend response shapes.

**Q12. What's the role of Spring Boot's auto-configuration here?**
It wires up `RestTemplateBuilder`, `ThymeleafAutoConfiguration`, `DispatcherServlet`, and Jackson's `ObjectMapper` without us writing any config. We only override what we care about — the timeouts in `RestTemplateConfig`, Thymeleaf cache flag, and our own `@Component` API clients.

**Q13. Does the frontend have a database?**
No. All state lives in the backend's MySQL. The frontend is stateless across requests except for session-scoped flash attributes (which are stored in the servlet session memory briefly during a redirect).

**Q14. Where does the session live?**
In the frontend's servlet container (embedded Tomcat) memory. If we ran multiple frontend instances behind a load balancer, we'd need sticky sessions or a shared session store (Redis via Spring Session).

**Q15. Why is the frontend stateless-ish?**
Because the backend owns the source of truth. Every page fetches what it needs fresh (no caching, except for the compiled Thymeleaf templates themselves in prod). This simplifies horizontal scaling — any frontend instance can handle any request.

**Q16. Could a mobile app reuse the backend?**
Yes — that's the whole point of the split. The backend speaks JSON, so an Android or iOS app would call the same `/api/*` endpoints directly (once auth is added).

**Q17. Why port 8081 for the frontend?**
8080 is the Spring Boot default, but the backend already claims it. We pick 8081 for the frontend so both can run side-by-side on a developer laptop. Configured via `server.port=8081` in `application.properties`.

**Q18. What's the request lifecycle from click to response?**
(1) Browser hits `http://localhost:8081/view/trips`. (2) Frontend `DispatcherServlet` routes to `TripViewController.listTrips()`. (3) Controller calls `tripApiClient.getAllTrips()`. (4) `RestTemplate` makes a GET to `http://localhost:8080/api/trips`. (5) Backend queries MySQL, returns JSON. (6) Jackson deserializes to `List<TripDTO>`. (7) Controller stuffs into `Model`, returns `"trip/trips"`. (8) Thymeleaf renders HTML. (9) Browser displays.

**Q19. Why not a GraphQL gateway?**
Overkill for a student project with simple REST needs. GraphQL shines when you have many clients needing different slices of data. Here, server-side Thymeleaf can aggregate exactly what it needs per page.

**Q20. How do you deal with long-running backend calls?**
The 10-second read timeout caps them; longer than that fails fast. For genuinely slow operations (like generating a PDF), we'd switch to WebClient (non-blocking) or an async job pattern — but our current flows are all synchronous and short.

### 11.2 RestTemplate & API Clients (Q21–Q40)

**Q21. What is `RestTemplate`?**
Spring's synchronous HTTP client. You give it a URL, optional headers and body, and a target type; it makes the call, deserializes the response, and returns the object. Part of `spring-web`. Declared "in maintenance" in favor of `WebClient`, but still fully supported and fine for blocking code.

**Q22. Why not `WebClient`?**
`WebClient` is reactive (Mono/Flux) — a bigger mental leap. Our controllers are blocking (Thymeleaf rendering is blocking anyway), so the reactive benefits are minimal. `RestTemplate` is simpler and matches the rest of the code style.

**Q23. `RestTemplate` vs `WebClient` vs Java 11's `HttpClient`?**
- `RestTemplate`: blocking, Spring-specific, rich interceptor/converter ecosystem.
- `WebClient`: non-blocking, reactive, preferred for new reactive apps.
- Java's `HttpClient`: no framework dependency, supports HTTP/2, but you write your own JSON handling.

We picked `RestTemplate` for framework integration (Jackson auto-wired, `MockRestServiceServer` for tests).

**Q24. What does `RestTemplateBuilder` do?**
It's an auto-configured builder that lets you customize a `RestTemplate` without losing defaults (Jackson converter, error handler). We use `builder.setConnectTimeout(...).setReadTimeout(...).build()` in `RestTemplateConfig` to produce one configured bean.

**Q25. Why set connect and read timeouts separately?**
They catch different failures:
- Connect timeout: backend is unreachable / DNS fails / port closed.
- Read timeout: connection succeeded but backend is slow to respond.

We use 5s connect and 10s read. A slow DB query should hit the read timeout, not block forever.

**Q26. What is `ParameterizedTypeReference`, and when do you need it?**
It's how you preserve generic type information at runtime (Java erases generics). Needed for `exchange(...)` calls that return `List<TripDTO>` — because `List<TripDTO>.class` isn't a thing:
```java
restTemplate.exchange(url, HttpMethod.GET, null,
    new ParameterizedTypeReference<List<TripDTO>>() {});
```
Not needed when the target is a concrete class (e.g. `TripDTO.class`).

**Q27. `getForObject` vs `exchange()`?**
- `getForObject(url, Class<T>)` — simple: GET, return body as `T`. No headers, no status code.
- `exchange(url, method, httpEntity, type)` — full control: any method, custom headers/body, returns `ResponseEntity<T>` (status + headers + body).

Our API clients use `exchange()` because the base class needs custom headers (`Accept: application/json`) and uniform error handling.

**Q28. Why is `@Component` on API client classes?**
So Spring can detect them via classpath scanning and inject them into controllers. Each client becomes a singleton bean. Without `@Component` we'd have to hand-wire them in a `@Configuration` class.

**Q29. What is `HttpEntity`?**
A wrapper combining HTTP body + headers. Used in `exchange(...)`: `new HttpEntity<>(body, headers)`. For GET with no body, `new HttpEntity<>(headers)` or `null`.

**Q30. How does Jackson know the JSON shape for deserialization?**
It reflects on the target class — public getters or `@JsonProperty` annotations tell it which JSON field maps to which Java property. In our DTOs, Lombok's `@Data` generates the getters, so Jackson maps `{"tripId": 7}` to `setTripId(7)`.

**Q31. Why use `@Value("${backend.base-url}")` instead of hardcoding?**
Externalization. The same JAR runs in dev (pointing to `localhost:8080`), staging (`api-staging.example.com`), and prod without recompile. You can override via env var `BACKEND_BASE_URL` or command line `--backend.base-url=...`.

**Q32. What happens on a 4xx response?**
`RestTemplate`'s default error handler throws `HttpClientErrorException`. Our `AbstractApiClient` catches it, tries to extract `{"message": "..."}` from the body, and throws a `BackendException(status, message)`.

**Q33. What happens on a 5xx response?**
Same flow but `HttpServerErrorException`. Both extend `HttpStatusCodeException`, so the base class catches both with one `catch`.

**Q34. Can `RestTemplate` follow redirects?**
Yes, by default (the underlying `SimpleClientHttpRequestFactory` or `HttpComponentsClientHttpRequestFactory` follows 3xx). Our backend doesn't issue redirects, so this never surfaces.

**Q35. How do you add a custom header to every outgoing request?**
Register a `ClientHttpRequestInterceptor`:
```java
restTemplate.getInterceptors().add((req, body, exec) -> {
    req.getHeaders().add("X-Source", "frontend");
    return exec.execute(req, body);
});
```
Useful for request IDs, API keys, tracing.

**Q36. Is `RestTemplate` thread-safe?**
Yes — once configured, it's safe to share. That's why we register it as a Spring singleton and inject it into every API client.

**Q37. Why pass the `RestTemplate` through the constructor instead of `@Autowired` field injection?**
Constructor injection is a best practice: testable (no Spring needed — see our unit tests), immutable, fails fast if the dependency is missing. Lombok's `@RequiredArgsConstructor` generates the constructor automatically.

**Q38. How does `MockRestServiceServer` work?**
It replaces the `RestTemplate`'s `ClientHttpRequestFactory` with a mock that records expected requests and returns canned responses. No socket is opened. Test fails fast if a request doesn't match an expectation.

**Q39. What if the backend returns an empty body on 204 No Content?**
`RestTemplate.getForObject(...)` returns `null`. `exchange(...)` returns `ResponseEntity` with `null` body. Our clients return `null` for single-object lookups and an empty list for collection endpoints (via `Optional.ofNullable(...).orElse(List.of())`).

**Q40. How do you download binary data (PDF receipts)?**
Specify `byte[].class` or `Resource.class` as the response type. Our `PaymentApiClient.downloadReceipt(id)` uses:
```java
restTemplate.exchange(url, GET, entity, byte[].class);
```
The controller then pipes `byte[]` into the HTTP response with `Content-Type: application/pdf`.

### 11.3 Thymeleaf & View Controllers (Q41–Q60)

**Q41. `@Controller` vs `@RestController` — which and why?**
We use `@Controller`. `@RestController` implies `@ResponseBody` on every method — every return value becomes the HTTP body (as JSON). `@Controller` methods return view names (strings) that Spring resolves to templates. Our frontend is view-centric, so `@Controller` fits.

**Q42. `@GetMapping` vs `@PostMapping` — when do you use each?**
`@GetMapping` for idempotent reads (fetch trip, show form). `@PostMapping` for mutations (submit booking, create review). Browser forms default to GET unless you specify `method="post"`.

**Q43. `@RequestParam` vs `@ModelAttribute` vs `@PathVariable`?**
- `@RequestParam` — single query or form parameter: `?id=5` or `<input name="id">`.
- `@ModelAttribute` — binds an entire object: `<form>` fields mapped to an object's properties. Often used with `th:object` + `th:field`.
- `@PathVariable` — segment of the URL: `/trip/{id}` → `@PathVariable Integer id`.

**Q44. `Model` vs `ModelMap` vs `Map<String, Object>`?**
All three populate the view model; you can pick any. `Model` is the interface you most commonly see. `ModelMap` extends `LinkedHashMap`. A raw `Map<String, Object>` works too. Functionally equivalent — pick one for consistency.

**Q45. `RedirectAttributes` vs `Model` — what's a flash attribute?**
`Model` attributes live for the current request only; they're gone after render. `RedirectAttributes.addFlashAttribute(...)` stashes the value in the session, survives exactly one redirect, and is then removed. Essential for the Post-Redirect-Get pattern we use on error.

**Q46. Why use flash attributes on redirect?**
Because a redirect is a new HTTP request — the original `Model` is thrown away. Flash attributes give you a one-hop bridge to pass error messages, just-created IDs, or confirmation data to the redirected page without putting them in the URL.

**Q47. Thymeleaf fragments — `th:replace`, `th:insert`, `th:include`?**
- `th:replace="~{fragments/header :: header}"` — replaces the current tag entirely with the fragment.
- `th:insert` — inserts the fragment inside the current tag.
- `th:include` (deprecated) — inserts just the fragment's contents.

We use `th:replace` for `<head>` and navbar so the host tag doesn't wrap the fragment.

**Q48. `th:field` vs `th:value`?**
`th:field="*{name}"` is for forms backed by a `th:object`. It sets `id`, `name`, and `value` in one shot, and emits validation CSS classes. `th:value="${x}"` just sets the `value` attribute — use it when there's no object binding.

**Q49. Why `spring.thymeleaf.cache=false` in dev?**
So template changes are picked up on refresh — no restart needed. In prod, set it to `true` (the default) — parsed templates are cached for massive speedup.

**Q50. How does Thymeleaf locate templates?**
Auto-configured prefix/suffix: `classpath:/templates/` + `.html`. Returning `"trip/trips"` resolves to `classpath:/templates/trip/trips.html`. Customizable via `spring.thymeleaf.prefix` / `spring.thymeleaf.suffix`.

**Q51. What is `th:each` syntax?**
`th:each="item : ${collection}"` iterates. You can access the iteration status via `th:each="item, iterStat : ${list}"` and then use `iterStat.index`, `iterStat.count`, `iterStat.first`, `iterStat.last`, `iterStat.odd`, etc.

**Q52. How do you escape output to prevent XSS?**
`th:text` escapes by default (safe). If you really need raw HTML, `th:utext` — but only with trusted input. All user-provided strings (customer names, review comments) go through `th:text`.

**Q53. What is `@{...}` URL expression syntax?**
Context-aware URL builder. `@{/view/bookings}` becomes `/view/bookings` (adds context path if any). Query params: `@{/trips/search(from=${f}, to=${t})}` → `/trips/search?from=A&to=B`. Path vars: `@{/trip/{id}(id=${t.tripId})}`.

**Q54. How do dropdowns populate options from the model?**
```html
<select name="customerId">
    <option th:each="c : ${customers}" th:value="${c.id}" th:text="${c.name}"></option>
</select>
```
Controller puts `customers` (a `List<CustomerResponseDTO>`) in the model; `th:each` iterates.

**Q55. How does a GET form with multiple checkboxes of the same name bind?**
HTML's form encoding sends `seatNumbers=1&seatNumbers=2&seatNumbers=5`. Spring MVC binds that to `@RequestParam List<Integer> seatNumbers`. No extra config needed.

**Q56. How do you conditionally show a block?**
`th:if="${condition}"`. Also `th:unless="${condition}"` for the negation. Both evaluate in Thymeleaf's OGNL-like expression language.

**Q57. What is the `~{}` syntax?**
Fragment expression. Everywhere you'd reference a fragment (`th:replace`, `th:insert`), you wrap it in `~{template :: fragment}`. You can also use `~{...}` to pass fragments as parameters to other fragments.

**Q58. How do CSS and images get served?**
Spring Boot auto-serves `classpath:/static/` at the root context. Our CSS lives at `static/css/style.css`, linked in `fragments/header.html` with `<link th:href="@{/css/style.css}" rel="stylesheet">`.

**Q59. What's the difference between `@Controller`'s returning a String vs a `ModelAndView`?**
Functionally the same. `ModelAndView` bundles the view name and model in one object — sometimes cleaner when you build a view dynamically. Returning a plain String + populating the `Model` parameter is more common and just as expressive.

**Q60. How do you render a PDF instead of HTML?**
Return `void` and write directly to the response, or return `ResponseEntity<byte[]>` with `Content-Type: application/pdf`. Our `PaymentViewController.downloadReceipt(...)` does the latter after fetching the bytes from the backend.

### 11.4 Testing (Q61–Q80)

**Q61. What does `MockRestServiceServer` actually do?**
It hooks into the `RestTemplate`'s request factory to intercept outgoing calls, match them against expectations, and return canned responses. No TCP, no HTTP, no server — a pure in-memory simulation. Much faster than starting a real backend for each test.

**Q62. `@WebMvcTest` vs `@SpringBootTest`?**
- `@WebMvcTest(X.class)` — slice test, only loads `X` plus MVC infrastructure. Fast. No services, no DB.
- `@SpringBootTest` — full context. Slow. Used for integration tests that need the real bean graph.

Our controller tests use `@WebMvcTest` because we mock the API clients — no reason to boot everything.

**Q63. `@MockBean` vs `@Mock`?**
- `@Mock` — pure Mockito, doesn't know about Spring. Fine in plain JUnit tests without a context.
- `@MockBean` — creates a Mockito mock **and** replaces the bean in the Spring context. Required inside `@WebMvcTest` / `@SpringBootTest` so injection picks up the mock.

**Q64. What's `@RestClientTest`, and why didn't we use it?**
It's a slice annotation that auto-configures `MockRestServiceServer` for a specific `RestTemplate`-based client. We skipped it and used `MockRestServiceServer` manually because our API client tests don't need any Spring context at all — faster and more explicit.

**Q65. What does `MockMvc` do?**
Simulates HTTP requests through the Spring MVC dispatcher without starting a servlet container. You build requests (`get("/view/trips")`), execute them (`perform(...)`), and assert on the result (`andExpect(...)`). Great for testing controllers end-to-end without network.

**Q66. What's the `perform().andExpect()` chain doing?**
`perform(...)` executes the request and returns a `ResultActions`. `andExpect(...)` applies a `ResultMatcher` against the result — assertion-style. Chain as many as you like to check status, view name, model, flash, headers, etc.

**Q67. How do you test flash attributes?**
`.andExpect(flash().attributeExists("error"))` or `.andExpect(flash().attribute("error", "Seats already booked"))`. Spring MVC stores flash attributes in a `FlashMap` that `MockMvc` can inspect directly.

**Q68. How does `MockMvc` serialize DTOs in requests?**
It doesn't — `MockMvc` sends plain request bodies. If you need JSON, you serialize with an `ObjectMapper` first and set the content type. Our tests use form parameters (`.param(...)`), so no serialization needed.

**Q69. When do you use `verify(mock)` in Mockito?**
To assert a method was called with specific arguments: `verify(bookingApiClient).createBooking(any())`. Or verify it was **not** called: `verify(mock, never()).delete(5)`. Not strictly needed when the method return value is enough to confirm behavior.

**Q70. What's `ReflectionTestUtils.setField(...)` for?**
Writes a value into a private field, bypassing Spring's DI. We use it to set `baseUrl` on API clients in unit tests because those tests skip the Spring context (and thus skip `@Value` injection).

**Q71. How do you stub a method that throws an exception?**
```java
when(bookingApiClient.createBooking(any()))
    .thenThrow(new BackendException(400, "Bad"));
```
Next call to that method throws instead of returning.

**Q72. What's `any()` in `when(client.getById(any()))`?**
A Mockito matcher meaning "any argument". When you use any matcher, **all** arguments must be matchers (use `eq(5)` if you need an exact literal alongside `any()`).

**Q73. Can you run one test in isolation?**
Yes:
```bash
./mvnw.cmd test -Dtest=BookingViewControllerTest
./mvnw.cmd test -Dtest=BookingViewControllerTest#bookSeatsRedirectsToConfirmationOnSuccess
```
Surefire picks it up via the `test` property.

**Q74. Are tests parallel?**
JUnit 5 supports parallel execution (`junit-platform.properties`), but we keep it sequential — simpler and fast enough for 50 tests.

**Q75. How do you test a JSON response body from a controller?**
Use `jsonPath("$.field")` matchers:
```java
.andExpect(jsonPath("$.tripId").value(7))
```
Not used here because our controllers return views, not JSON.

**Q76. What's AssertJ's `assertThatThrownBy`?**
A fluent way to assert exceptions:
```java
assertThatThrownBy(() -> client.getById(999))
    .isInstanceOf(BackendException.class)
    .hasMessage("Trip not found");
```
Cleaner than a try/catch with `fail(...)` fallback.

**Q77. How do you reset mocks between tests?**
JUnit 5 + Mockito creates fresh mocks per test instance by default (`@BeforeEach` re-initializes). If you manually call `Mockito.reset(mock)`, you can clear stubs mid-test.

**Q78. Is `server.verify()` strictly necessary?**
Yes if you want to catch "we stubbed the request but never actually called it" bugs. Without `verify()`, a typo in the URL passed to `requestTo(...)` might still let the test pass because `RestTemplate` wouldn't match — but you'd lose the assertion.

**Q79. What's `@DisplayName` and do you use it?**
A JUnit 5 annotation for human-readable test names: `@DisplayName("Seats cannot be double-booked")`. We use plain camelCase method names for now — readable enough.

**Q80. How do you measure test coverage?**
Plug in JaCoCo via the Maven plugin:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
</plugin>
```
Run `./mvnw.cmd verify` — HTML report lands in `target/site/jacoco/index.html`. Not configured in this project yet; optional future work.

### 11.5 Configuration & Deployment (Q81–Q100)

**Q81. Why two separate ports (8080, 8081)?**
Both apps can't bind the same TCP port on the same host. Backend at 8080 (Spring Boot's default), frontend at 8081. Any free port works — `server.port=8081` in `application.properties`.

**Q82. How would you deploy this to production?**
Build both JARs, deploy them on one or two VMs (or containers). Front them with nginx for TLS termination. Point DNS at nginx. Run MySQL separately (managed RDS or self-hosted). Export logs to a central store.

**Q83. Sketch an nginx reverse proxy config.**
```nginx
server {
    listen 443 ssl;
    server_name busbooking.example.com;
    ssl_certificate /etc/ssl/cert.pem;
    ssl_certificate_key /etc/ssl/key.pem;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```
Frontend alone is exposed publicly. Backend stays on a private network, reachable only from the frontend's machine.

**Q84. How does the frontend learn the backend URL in prod?**
Environment variable `BACKEND_BASE_URL=https://api.internal:8080` → Spring maps it to `backend.base-url` at startup (relaxed binding). Or CLI flag: `--backend.base-url=...`. No code changes.

**Q85. Can we run 3 frontend instances against 1 backend?**
Yes. The frontend is stateless-ish (only short-lived flash attributes in the servlet session). Put all three behind a load balancer with sticky sessions — or better, move sessions into Redis via Spring Session.

**Q86. What does Spring Boot DevTools do?**
Auto-restart when classpath changes (saves you from stopping/starting), live-reload browser, enables sensible dev defaults (disables template caching, enables debug logging). Opt-in dependency (`spring-boot-devtools` with `runtime` scope).

**Q87. What does Lombok's annotation processor do?**
At compile time, it synthesizes boilerplate: `@Data` generates getters, setters, `equals`, `hashCode`, `toString`; `@Builder` generates a fluent builder; `@RequiredArgsConstructor` generates a constructor for all `final` fields. Requires IDE support (Lombok plugin) so developers can see the generated methods.

**Q88. What does `spring-boot-maven-plugin` do?**
Packages the app as an executable "fat JAR" — includes all dependencies and a bootloader. `java -jar target/frontend-1.0.0.jar` just works. Also exposes `./mvnw spring-boot:run` and `spring-boot:build-image` for Cloud Native Buildpacks.

**Q89. How do you build a runnable JAR?**
```bash
./mvnw.cmd clean package -DskipTests
```
Output: `target/frontend-1.0.0.jar`. Skip `-DskipTests` if you want the full test run first.

**Q90. Sketch a Dockerfile for the frontend.**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/frontend-1.0.0.jar app.jar
EXPOSE 8081
ENV BACKEND_BASE_URL=http://backend:8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```
Backend gets an identical Dockerfile except port and a `DATABASE_URL` env var.

**Q91. How do you pass env vars to Spring Boot?**
Relaxed binding: `BACKEND_BASE_URL` env var → `backend.base-url` property. You can also pass `-Dbackend.base-url=...` as a JVM system property, or `--backend.base-url=...` on the command line. Highest-priority wins.

**Q92. What are Spring profiles?**
Named groups of config. Activate with `spring.profiles.active=prod`. Files like `application-prod.properties` are layered on top of `application.properties`. Use to differentiate dev (verbose logs, no cache) from prod (cache on, logs WARN).

**Q93. Where does the server log go?**
By default, stdout. In production, redirect to a file (`java -jar app.jar >> /var/log/frontend.log 2>&1`) or ship to a central store with a sidecar (Fluentd, Vector). Configurable via `logging.file.name` and `logging.level.*`.

**Q94. How do you configure the log level?**
`logging.level.root=WARN`, `logging.level.com.busfrontend=DEBUG`. Can be flipped at runtime via the Spring Boot Actuator `/loggers` endpoint (if enabled).

**Q95. Do you enable Actuator endpoints in prod?**
Only `/actuator/health` (for liveness probes) and optionally `/actuator/info`. Anything else (`/env`, `/configprops`, `/mappings`) is too revealing and should be locked behind auth or disabled entirely.

**Q96. How do you add an HTTP request-logging filter?**
Register a `CommonsRequestLoggingFilter` bean:
```java
@Bean
CommonsRequestLoggingFilter requestLoggingFilter() {
    CommonsRequestLoggingFilter f = new CommonsRequestLoggingFilter();
    f.setIncludeQueryString(true);
    f.setIncludePayload(true);
    return f;
}
```
Then set `logging.level.org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG`.

**Q97. How would you add health checks for the backend dependency?**
Custom `HealthIndicator`:
```java
@Component
class BackendHealthIndicator implements HealthIndicator {
    private final RestTemplate rt;
    @Override public Health health() {
        try {
            rt.getForObject(baseUrl + "/actuator/health", String.class);
            return Health.up().build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
```
Surfaces at `/actuator/health`. Great for Kubernetes readiness probes.

**Q98. Memory footprint?**
Spring Boot fat JAR app idles around 200–300 MB heap. JVM flags `-Xms256m -Xmx512m` keep it bounded. For a t3.small EC2 (2 GB RAM) you can run both frontend and backend side-by-side comfortably.

**Q99. How do you do zero-downtime deploys?**
Two frontend replicas behind a load balancer. Rolling restart: stop one, wait for the new version to pass `/actuator/health`, then swap the other. For the backend, do DB migrations backward-compatibly (add columns before removing old code paths) so both old and new backends can coexist.

**Q100. What's the first thing you'd add if this went live tomorrow?**
In order: (1) HTTPS via nginx + Let's Encrypt, (2) API key auth between frontend and backend, (3) basic rate limiting on the public frontend, (4) centralized logging, (5) a monitoring dashboard (Prometheus + Grafana via Actuator metrics). None of those change the core code — they're operational concerns.

---

## Section 12 — Running & Deployment

### 12.1 Local development (both apps)

Terminal 1 (backend):
```bash
cd /path/to/bus-ticket-backend
./mvnw.cmd spring-boot:run
# serves on http://localhost:8080
```

Terminal 2 (frontend):
```bash
cd /path/to/bus-ticket-frontend
./mvnw.cmd spring-boot:run
# serves on http://localhost:8081
```

Open `http://localhost:8081/` in a browser. DevTools restart both apps on code change.

### 12.2 Build runnable JARs

```bash
./mvnw.cmd clean package
# -> target/backend-1.0.0.jar and target/frontend-1.0.0.jar
```

Run:
```bash
java -jar target/backend-1.0.0.jar
java -jar target/frontend-1.0.0.jar --backend.base-url=http://localhost:8080
```

### 12.3 Production (high-level)

```
[Internet] --HTTPS--> [nginx :443]
                         |
                         +--HTTP--> [frontend :8081]
                                         |
                                         +--HTTP--> [backend :8080] --JDBC--> [MySQL]
```

- nginx handles TLS, compression, static asset caching.
- Frontend and backend run as systemd services (or containers).
- Backend is **not** exposed publicly — only the frontend is reachable from the internet.
- MySQL on a private subnet.

### 12.4 Example nginx config

```nginx
upstream frontend {
    server 127.0.0.1:8081;
}

server {
    listen 443 ssl http2;
    server_name busbooking.example.com;

    ssl_certificate     /etc/letsencrypt/live/busbooking.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/busbooking.example.com/privkey.pem;

    # Static assets can be served by nginx directly for speed
    location /css/ {
        alias /opt/frontend/static/css/;
        expires 30d;
    }

    location / {
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name busbooking.example.com;
    return 301 https://$host$request_uri;
}
```

### 12.5 Dockerfile outline (frontend)

```dockerfile
# --- build stage ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q clean package -DskipTests

# --- runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /src/target/frontend-1.0.0.jar app.jar
EXPOSE 8081
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV BACKEND_BASE_URL=http://backend:8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
```

Backend Dockerfile is identical except port 8080 and env var `DATABASE_URL`.

### 12.6 Environment overrides

```bash
# Local dev against a staging backend
./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--backend.base-url=https://api-staging.example.com

# Prod env file
BACKEND_BASE_URL=https://api.internal.example.com
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8081
SPRING_THYMELEAF_CACHE=true
LOGGING_LEVEL_COM_BUSFRONTEND=INFO
```

### 12.7 Health & readiness

Add Actuator dependency, then expose only `health`:
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```
Kubernetes or a load balancer can poll `/actuator/health` to decide routing.

---

## Section 13 — Gotchas & Known TODOs

### 13.1 DTO duplication between frontend and backend
Every DTO exists in both modules. Trade-off:
- Pro: modules are independently versionable.
- Con: when a field is added, both sides must be touched.
- Future: extract a `busbooking-contracts` Maven module that both depend on — single source of truth.

### 13.2 No session-level auth across the split
The backend is open; the frontend has no login. Deferred for the current iteration. Path forward: add Spring Security on the frontend (form login or OAuth2) and pass a signed JWT to the backend on every call.

### 13.3 `BookingViewController.showConfirmation` advanced flow TODO
Today the confirmation page pulls booking details out of flash attributes. If the user refreshes, the flash is gone and the page can't re-render. Fix: accept `bookingId` as a path variable and fetch fresh from the backend every time — more REST-ful and refresh-safe.

### 13.4 `PaymentViewController.processPaymentView` fan-out semantics
Current behavior: for a group booking of N seats, the view controller loops and calls `paymentApiClient.create(...)` N times. If seat 3 of 5 fails, we have partial state (2 paid, 3 unpaid) and no rollback. Better: add a batch endpoint on the backend that wraps all N inserts in a single `@Transactional`.

### 13.5 `RouteViewController.searchRoutesView` — client-side filter
The "search routes" page fetches **all** routes and filters in the controller by substring match. Fine for 50 routes, hostile for 5,000. TODO: add a `/api/routes/search?from=...&to=...` endpoint on the backend.

### 13.6 `MemberController` / `TeamController` intentionally skipped
These render static team pages and don't need any backend data. We hand-coded the HTML with no controller methods for CRUD — just static GETs. Porting "later" means exposing team members as a DB-backed entity, which is overkill for a capstone team page.

### 13.7 PDF download memory
`PaymentApiClient.downloadReceipt(id)` returns `byte[]` — the entire PDF loads in memory on both the backend and the frontend before being streamed to the browser. Fine for 50 KB receipts, a problem for megabyte tickets. Future: switch to streaming with `ResponseExtractor` + `InputStream` piping.

### 13.8 `RestTemplate` → `WebClient`
`RestTemplate` is in maintenance mode. For a future rewrite, migrate to `WebClient` (reactive) or `RestClient` (the synchronous Spring 6 successor). Low priority — `RestTemplate` works fine for blocking code.

### 13.9 Shared DTO contract module
Long-term: factor `com.busbooking.contracts` into its own JAR, with interface-style DTOs consumed by both modules. Removes the duplication risk in 13.1 at the cost of coupling their release cadence.

### 13.10 Thymeleaf template precompilation
Dev mode doesn't cache templates; prod does. Beyond that, consider `thymeleaf-spring-data-dialect` for better `Page`/`Pageable` support if you add server-side pagination.

### 13.11 No Actuator yet
Adding `spring-boot-starter-actuator` is a one-liner and unlocks `/actuator/health` for liveness probes. Recommended before any prod deploy.

### 13.12 No CSRF protection on forms
Currently disabled because we don't use Spring Security. The moment you add auth, re-enable CSRF (on by default with Security), add `<meta name="_csrf">` to the header fragment, and include the hidden token input on every POST form.

### 13.13 Hardcoded URLs in a few redirect strings
Search for `redirect:/view/` — those are fine because Spring handles the context path. But any fully qualified URL (e.g. in confirmation emails later) must come from config, not a literal.

---

*End of Part C. See Part A for architecture overview, Part B for per-module deep dives.*

---
---

# PART D — POST-SPLIT 404 HOTFIX: `/team`, `/members`, `/api/payments/{id}/ticket`

> **Context:** After the initial monolith → frontend/backend split was complete and tests were green, a user-reported bug surfaced: three URLs on `http://localhost:8081` returned **HTTP 404 Not Found**:
>
> 1. `http://localhost:8081/team`
> 2. `http://localhost:8081/members`
> 3. `http://localhost:8081/api/payments/366/ticket`
>
> This section documents the root cause of each 404, the exact fix applied, the reasoning behind the approach, and the viva questions you should be ready to answer about them.

---

## 14.1 Symptom vs Cause — the Three 404s at a Glance

| # | URL | What the user expected | What actually happened | Root cause |
|---|---|---|---|---|
| 1 | `GET /team` | A "Team" page listing all 5 developers as cards | `404 Not Found` rendered by Spring's BasicErrorController | No `@RequestMapping("/team")` controller exists on the frontend. `TeamController` was **deliberately skipped** during the initial port because the porting subagent flagged it as "depends on backend-only classes". |
| 2 | `GET /members` | A "Member Explorer" page listing 5 developers + clickable operation cards | `404 Not Found` | Same cause as #1. `MemberController` + `OperationExecutor` + `MemberRegistry` were **skipped** during the port. |
| 3 | `GET /api/payments/366/ticket` | A PDF download for payment 366 | `404 Not Found` | **All** `/api/*` routes live on the backend (port 8080). The frontend only handles `/view/*`. When the user (or an old bookmark / email link / stale template) hits `/api/*` on port 8081, there is no matching handler. |

All three are classic **post-split regression classes:**
- Type A: **Orphaned features** (the port missed code that *should* have moved — bugs #1, #2).
- Type B: **Stale URL contracts** (external links / templates still point at the monolith's REST paths on the new frontend port — bug #3).

---

## 14.2 Problem 1 — `/team` 404

### 14.2.1 Expected behavior (from the backend)

Backend `TeamController` at `com/busticketbookingsystem/web/team/TeamController.java`:

```java
@Controller
@RequestMapping("/team")
public class TeamController {

    private final TeamRegistry registry;

    @GetMapping
    public String teamHome(Model model) {
        model.addAttribute("members", registry.getAll());
        return "team/members";
    }

    @GetMapping("/{slug}")
    public String profile(@PathVariable String slug, Model model, RedirectAttributes ra) {
        return registry.findBySlug(slug).map(m -> {
            model.addAttribute("member", m);
            return "team/profile";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Team member not found: " + slug);
            return "redirect:/team";
        });
    }
}
```

It depends on `TeamRegistry` (an in-memory `@Component` that returns 5 `TeamMember` objects) and the value objects `TeamMember`, `EndpointEntry`, `ScreenEntry`.

### 14.2.2 Why the porting subagent skipped it

The subagent's report said:

> *"MemberController and TeamController were intentionally skipped. Both depend on backend-only infrastructure (...) Porting them requires copying those registry classes + their data — not in scope per rule 11 ('too complex to port')."*

This was **an overestimate of the dependency footprint**. Let's audit the actual dependencies:

| File | External deps |
|---|---|
| `TeamController.java` | `Controller`, `Model`, `GetMapping`, `PathVariable`, `RequestMapping`, `RedirectAttributes`, **`TeamRegistry`** |
| `TeamRegistry.java` | `@Component`, `List`, `Optional` — **no HTTP, no JPA** |
| `TeamMember.java` | Lombok only |
| `EndpointEntry.java` | Lombok only |
| `ScreenEntry.java` | Lombok only |

**Zero HTTP dependencies. Zero JPA dependencies. Zero backend-specific imports.** The package is self-contained — the subagent misjudged it.

### 14.2.3 Fix — verbatim copy with package rewrite

All 5 files copy over **unchanged** except the first line (`package` declaration):

```bash
BE=".../bus-ticket-booking-system/src/main/java/com/busticketbookingsystem/web/team"
FE=".../bus-ticket-frontend/src/main/java/com/busfrontend/team"
mkdir -p "$FE"
for f in EndpointEntry.java ScreenEntry.java TeamMember.java TeamRegistry.java TeamController.java; do
    sed -E 's|^package com\.busticketbookingsystem\.web\.team;|package com.busfrontend.team;|' \
        "$BE/$f" > "$FE/$f"
done
```

The `sed` expression rewrites exactly one line — the `package` statement. Everything below (imports, class body, `@RequestMapping`, Thymeleaf view names) is kept intact because:

- The imports only reference standard Spring MVC + Lombok.
- The view names `team/members` and `team/profile` are resolved via the frontend's own `templates/team/` directory (already copied during step 2 of the original split).
- `@Component` on `TeamRegistry` is auto-discovered by `@SpringBootApplication` — the new package is under `com.busfrontend.*` which is the scan root.

### 14.2.4 Why this works — component scan math

`BusFrontendApplication` lives in `com.busfrontend`. Spring Boot's `@SpringBootApplication` = `@ComponentScan` with base package = that class's package.

Scan root → `com.busfrontend` → discovers:
- `com.busfrontend.config.*`
- `com.busfrontend.controller.*`
- `com.busfrontend.client.*`
- **`com.busfrontend.team.*`** ← NEW (picked up automatically on restart)
- **`com.busfrontend.members.*`** ← NEW (see Problem 2)

No scan config changes required.

---

## 14.3 Problem 2 — `/members` 404

### 14.3.1 Expected behavior

The "Member Explorer" is a developer-portal-style screen:
- `/members` → list of 5 devs as cards.
- `/members/{id}` → a specific dev's detail page: their service areas, operations, screens.
- `/members/{id}/operation` → form to try an operation.
- `POST /members/{id}/operation/execute` → actually invoke the backend operation.
- `GET /members/{id}/operation/download` → proxy a backend PDF.

### 14.3.2 Why this one is trickier than `/team`

Two of the 7 files in `com.busticketbookingsystem.web.members` **do** have HTTP dependencies:

| File | HTTP deps |
|---|---|
| `FieldDef.java` | None (POJO) |
| `Member.java` | None (POJO) |
| `ServiceInfo.java` | None (POJO) |
| `Operation.java` | None (POJO) |
| `MemberRegistry.java` | Just `@Component` (no HTTP) |
| **`OperationExecutor.java`** | `RestTemplate`, `HttpEntity`, `@Value("${server.port:8080}")` |
| **`MemberController.java`** | `RestTemplate`, `@Value("${server.port:8080}")` |

The backend's `OperationExecutor` uses `RestTemplate` to call *itself* — it's a self-dispatching pattern. The endpoint becomes `http://localhost:${server.port}${endpoint}`, which in the monolith is `http://localhost:8080/api/trips`.

On the frontend, `server.port = 8081` (the frontend's own port) — meaning if we copy-pasted without rewiring, the Member Explorer's "execute operation" button would try to hit `http://localhost:8081/api/trips` → **loopback 404** (because `/api/*` lives on port 8080).

### 14.3.3 Fix — copy + rewire `server.port` → `backend.base-url`

#### Step 1: copy the 7 files with package rewrite

```bash
for f in FieldDef.java Member.java MemberRegistry.java Operation.java \
         ServiceInfo.java OperationExecutor.java MemberController.java; do
    sed -E 's|^package com\.busticketbookingsystem\.web\.members;|package com.busfrontend.members;|' \
        "$BE/$f" > "$FE/$f"
done
```

#### Step 2: rewire `OperationExecutor.java`

**BEFORE (backend self-dispatch):**
```java
@Value("${server.port:8080}")
private String serverPort;

private String baseUrl() {
    return "http://localhost:" + serverPort;
}
```

**AFTER (frontend → backend):**
```java
@Value("${backend.base-url:http://localhost:8080}")
private String backendBaseUrl;

private String baseUrl() {
    return backendBaseUrl;
}
```

The property `backend.base-url` is already defined in the frontend's `application.properties` (`backend.base-url=http://localhost:8080`). So now `OperationExecutor.baseUrl()` returns the backend's public URL.

#### Step 3: rewire `MemberController.java` — same two places

- Rename field `serverPort` → `backendBaseUrl` and swap the `@Value` key.
- In `downloadPdf(...)`: change
  ```java
  String url = "http://localhost:" + serverPort + op.getEndpoint().replace("{id}", pathId);
  ```
  to
  ```java
  String url = backendBaseUrl + op.getEndpoint().replace("{id}", pathId);
  ```
- In `handlePdfDownload(...)`: change the `redirect:` target so the browser is redirected to the backend (port 8080) for the actual PDF bytes, not the frontend:
  ```java
  // BEFORE
  return "redirect:" + op.getEndpoint().replace("{id}", pathId.trim());
  // AFTER
  return "redirect:" + backendBaseUrl + op.getEndpoint().replace("{id}", pathId.trim());
  ```
- Same fix in `handlePdfDownloadQuery(...)` for query-string PDFs.

### 14.3.4 Why property-based rewiring beats hardcoding

If you hardcode `"http://localhost:8080"` in `OperationExecutor`:
- It breaks the moment backend moves to another host (production, Docker, another port).
- It can't be overridden with `--backend.base-url=...` at runtime.
- It can't be profile-scoped (`application-prod.properties`).

Using `@Value("${backend.base-url:http://localhost:8080}")`:
- Default is fine for local dev.
- Prod override: `java -jar frontend.jar --backend.base-url=https://api.busbooking.com`.
- Docker: `-e BACKEND_BASE_URL=http://backend:8080`.
- CI: in `application-test.properties`, point at a WireMock server.

### 14.3.5 Why the `MemberController` `redirect:` fix is important

Before the fix, `handlePdfDownload` returned `redirect:/api/payments/42/ticket`. On the frontend (port 8081), that URL did not exist (see Problem 3) — so the browser followed the redirect, hit the frontend, got a 404. It *looked* like the redirect was broken, but actually the destination was the wrong server.

After the fix, the redirect target is the fully-qualified `http://localhost:8080/api/payments/42/ticket` → browser hops to port 8080 → backend streams the PDF back. Works.

---

## 14.4 Problem 3 — `/api/payments/{id}/ticket` 404

### 14.4.1 The architectural context

After the split, we drew a hard line:

```
Frontend (port 8081) — owns /view/*  — calls backend server-to-server
Backend  (port 8080) — owns /api/*   — returns JSON / PDF bytes
```

Browsers should only ever hit `/view/*` on 8081. REST endpoints on 8080 exist so the frontend's API clients (plus external API consumers, mobile apps, Postman, etc.) can talk JSON.

### 14.4.2 Why the 404 still shows up in practice

Reality is messier than the architecture diagram:

| Scenario | How the 404 gets triggered |
|---|---|
| Old bookmark from the monolith era | User bookmarked `http://localhost:8080/api/payments/42/ticket` back when port 8080 was the monolith. After split, port 8080 still returns PDF ✓. But if they typo'd `8081` (frontend), 404. |
| Thymeleaf template with hardcoded path | If any template had `<a th:href="@{/api/payments/{id}/ticket(id=${p.paymentId})}">Download</a>`, Thymeleaf renders it as `/api/payments/42/ticket` **relative to the current server** → resolves to `http://localhost:8081/api/payments/42/ticket` → 404. |
| Email / printed page with backend path | Same issue: the path is copy-pasted without the full base URL. |
| Redirect from `MemberController` (the Problem 2 bug) | Internal redirect chain pushed the browser to `/api/...` on port 8081. |

### 14.4.3 Fix — proxy endpoints on the frontend

Two options were considered:

| Option | Pros | Cons |
|---|---|---|
| **A — Fix every caller** to use the full backend URL | Clean separation, no frontend API routes | Requires auditing every template + every outbound link; misses future stale bookmarks |
| **B — Add `/api/*` proxy endpoints on the frontend** | Legacy URLs keep working; zero coordination needed | Duplicates a tiny slice of backend routes; slight CPU cost for the byte-copy |

**Chose B** — pragmatic, zero coordination cost, self-healing against stale links. The frontend becomes an opt-in proxy for PDF-download routes only. All *data* endpoints (JSON) remain backend-only.

### 14.4.4 Implementation

Added to `PaymentViewController.java`:

```java
/**
 * Proxy the backend PDF ticket through the frontend so that the URL
 * /api/payments/{id}/ticket works on port 8081 (legacy links that
 * predate the split still land here).
 */
@GetMapping({"/api/payments/{id}/ticket", "/view/payments/{id}/download"})
public ResponseEntity<byte[]> downloadPaymentTicket(@PathVariable Integer id) {
    byte[] pdf = paymentApiClient.downloadTicketByPaymentId(id);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "ticket-" + id + ".pdf");
    return ResponseEntity.ok().headers(headers).body(pdf);
}

/** Proxy group ticket download by paymentIds CSV. */
@GetMapping({"/api/payments/group-ticket", "/view/payments/group-ticket-download"})
public ResponseEntity<byte[]> downloadGroupTicket(@RequestParam("paymentIds") String paymentIdsCsv) {
    List<Integer> ids = new ArrayList<>();
    for (String s : paymentIdsCsv.split(",")) {
        s = s.trim();
        if (!s.isEmpty()) ids.add(Integer.parseInt(s));
    }
    byte[] pdf = paymentApiClient.downloadGroupTicket(ids);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "group-ticket.pdf");
    return ResponseEntity.ok().headers(headers).body(pdf);
}
```

Same pattern added to `BookingViewController.java` for `/api/bookings/{id}/ticket` and `/api/bookings/group-ticket`.

### 14.4.5 Deep dive on the proxy method

```java
@GetMapping({"/api/payments/{id}/ticket", "/view/payments/{id}/download"})
public ResponseEntity<byte[]> downloadPaymentTicket(@PathVariable Integer id) {
```

**Two paths, one handler.** Spring's `@GetMapping` accepts an array — this handler is registered for *both*:
- `/api/payments/{id}/ticket` — the legacy/backend path (compatibility).
- `/view/payments/{id}/download` — the new frontend-native path (preferred for new UI links).

```java
    byte[] pdf = paymentApiClient.downloadTicketByPaymentId(id);
```

`PaymentApiClient.downloadTicketByPaymentId(id)` calls `AbstractApiClient.getBytes("/api/payments/" + id + "/ticket")` — which issues `GET http://localhost:8080/api/payments/{id}/ticket` with `Accept: application/pdf, application/octet-stream` headers, captures the response body as `byte[]`.

```java
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "ticket-" + id + ".pdf");
    return ResponseEntity.ok().headers(headers).body(pdf);
```

- `Content-Type: application/pdf` — tells browser this is a PDF, not HTML.
- `Content-Disposition: attachment; filename="ticket-42.pdf"` — forces "Save As..." dialog instead of opening inline.
- `ResponseEntity.ok()` — HTTP 200 with the body.

### 14.4.6 Memory consideration

`byte[] pdf = ...` pulls the entire PDF into the frontend JVM's heap. For a typical 50 KB ticket, that's fine. For a 100-ticket group PDF (~5 MB), still fine. If ticket sizes ever balloon to 50+ MB, the pattern should switch to a streaming proxy (`InputStreamResource` + `Flux<DataBuffer>` with WebClient). For current scale this is acceptable — noted in section 13's TODOs.

---

## 14.5 Complete Diff Summary

| File | Change |
|---|---|
| `com/busfrontend/team/EndpointEntry.java` | **NEW** — copied from backend |
| `com/busfrontend/team/ScreenEntry.java` | **NEW** — copied |
| `com/busfrontend/team/TeamMember.java` | **NEW** — copied |
| `com/busfrontend/team/TeamRegistry.java` | **NEW** — copied (self-contained `@Component`) |
| `com/busfrontend/team/TeamController.java` | **NEW** — copied |
| `com/busfrontend/members/FieldDef.java` | **NEW** — copied |
| `com/busfrontend/members/Member.java` | **NEW** — copied |
| `com/busfrontend/members/Operation.java` | **NEW** — copied |
| `com/busfrontend/members/ServiceInfo.java` | **NEW** — copied |
| `com/busfrontend/members/MemberRegistry.java` | **NEW** — copied |
| `com/busfrontend/members/OperationExecutor.java` | **NEW** — copied + `${server.port}` → `${backend.base-url}` |
| `com/busfrontend/members/MemberController.java` | **NEW** — copied + three `server.port` → `backend.base-url` rewires + two `redirect:` URL prefix changes |
| `controller/PaymentViewController.java` | Added imports (`HttpHeaders`, `MediaType`, `ResponseEntity`); added 2 proxy endpoints (single + group PDF) |
| `controller/BookingViewController.java` | Added imports; added 2 proxy endpoints (single + group booking PDF) |

Net: **12 new files + 2 modified files**. Zero backend changes. Zero new dependencies in `pom.xml`. All 50 pre-existing tests still pass.

---

## 14.6 Verification

```bash
cd bus-ticket-frontend
./mvnw.cmd -q compile             # BUILD SUCCESS
./mvnw.cmd test                   # Tests run: 50, Failures: 0, Errors: 0
./mvnw.cmd spring-boot:run        # starts on :8081
```

Then hit the three URLs in a browser — all three now work:

| URL | Result |
|---|---|
| `http://localhost:8081/team` | Team cards page renders |
| `http://localhost:8081/members` | Member explorer list renders |
| `http://localhost:8081/api/payments/366/ticket` | PDF downloads (requires payment 366 to exist on the backend) |

---

## 14.7 Approach & Lessons Learned

### 14.7.1 "Too complex to port" is a red flag — always audit first

The porting subagent correctly skipped `MemberController` because it did have HTTP deps, but it was wrong about `TeamController`. A 10-second dependency audit (grep imports + check registered beans) would have caught that `TeamController` had **zero** HTTP-specific dependencies and could have been copied verbatim from day one.

**Rule of thumb during ports:** before skipping, run:
```bash
grep -E "RestTemplate|HttpEntity|@RequestMapping|@Value.*port|@Value.*url" <file>
```
If the output is empty, it's a cheap copy.

### 14.7.2 Two categories of "missing feature" need different fixes

| Category | Example | Fix pattern |
|---|---|---|
| **Stateless view feature** (no external HTTP) | `/team` | Copy package verbatim, rewrite only the `package` statement |
| **HTTP self-dispatcher** | `/members` (uses `RestTemplate` to call own app) | Copy + rewire `@Value("${server.port}")` → `@Value("${backend.base-url}")` so the self-call becomes a cross-service call |
| **Legacy URL contract** | `/api/payments/{id}/ticket` | Add proxy `@GetMapping` on the frontend that fetches via the API client and streams through |

### 14.7.3 Redirect semantics change when you split services

In a monolith, `return "redirect:/api/payments/42/ticket"` from a controller means "browser goes to `http://same-host:same-port/api/...`". After split, that's the frontend's own host, not the backend's — the redirect silently breaks.

**Always fully-qualify cross-service redirects** (or use a path that the frontend itself handles, i.e. a proxy).

### 14.7.4 Component scan is your friend when copying packages

Because `@SpringBootApplication` auto-scans everything under `com.busfrontend.*`, copying a package to `com.busfrontend.team.*` auto-registers all its `@Component`, `@Controller`, `@Service` beans with zero `application.properties` or `@ComponentScan` config. This is the least-surprise option for feature ports.

---

## 14.8 Viva Questions — PART D (Q101–Q125)

**Q101. Why did `http://localhost:8081/team` return 404 after the split even though the template `team/members.html` was copied?**

Templates are rendered by a Thymeleaf `ViewResolver` only when a handler method returns the matching view name. Without `TeamController`, no handler maps `/team` to the view "team/members", so Spring's `DispatcherServlet` treats the request as unmapped and routes it to `BasicErrorController` → 404.

**Q102. What's the absolute minimum to port `TeamController` to the frontend?**

Five files (`TeamController`, `TeamRegistry`, `TeamMember`, `EndpointEntry`, `ScreenEntry`) copied verbatim with one `sed` command that rewrites the `package` declaration. Zero other edits needed because all dependencies are already on the frontend's classpath (Spring MVC + Lombok).

**Q103. Why wasn't `TeamRegistry` a problem to copy?**

It's a `@Component` with no HTTP, no database, no file I/O. It builds a hardcoded `List<TeamMember>` at construction time and returns sub-lookups via `Optional`. Self-contained pure Java. Frontend component scan picks it up automatically under `com.busfrontend.team.*`.

**Q104. What does `sed -E 's|^package com\.busticketbookingsystem\.web\.team;|package com.busfrontend.team;|'` do?**

Reads the file, matches the first-line `package` declaration with extended regex, replaces only that line, writes the result. All other lines (imports, annotations, class body) pass through unchanged. The `|` delimiter (instead of `/`) avoids escaping the dots.

**Q105. Could we have configured `@ComponentScan` to point back at the backend's package instead of copying?**

Theoretically yes, but that requires the frontend JAR to include the backend as a Maven dependency, which reintroduces every backend transitive (JPA, MySQL, iText, Hibernate, etc.) — defeating the whole point of the split. Copy + package rewrite is the right trade-off.

**Q106. `MemberController` was skipped because it had `RestTemplate` and `@Value("${server.port}")`. Why was that a blocker for direct copy?**

Because on the frontend, `server.port` is `8081` (the frontend's own port). The controller would build URLs like `http://localhost:8081/api/trips` — but `/api/*` doesn't exist on 8081. Copy-paste without rewiring leads to every operation returning a 404. Rewiring to `${backend.base-url}` points the self-dispatch at the real backend on 8080.

**Q107. What's the practical difference between `@Value("${server.port:8080}")` and `@Value("${backend.base-url:http://localhost:8080}")`?**

The first injects just the port as a `String`, implying a self-call pattern (`"http://localhost:" + port`). The second injects a full URL, supporting arbitrary hosts and schemes (`https://api.busbooking.com`). After splitting services, you always want the second form — it's the one that survives a move to prod.

**Q108. After splitting, why did the `MemberController`'s PDF redirects still 404 even after we fixed `OperationExecutor`?**

`OperationExecutor.execute()` handles BODY/QUERY API calls — it was fixed. But `MemberController` has two *additional* flash-redirect code paths (`handlePdfDownload`, `handlePdfDownloadQuery`) that return `redirect:` strings directly. Those still sent the browser to `/api/...` relative to the current host (port 8081), triggering a second 404. Both had to be prefixed with `backendBaseUrl` too.

**Q109. Why use `"redirect:" + backendBaseUrl + ..."` instead of just returning the PDF directly from `MemberController`?**

The redirect shifts the byte-transfer to the backend — the frontend's JVM never holds the PDF bytes, saving memory. The trade-off: the browser's URL bar shows `localhost:8080/api/payments/42/ticket` after the redirect, which looks inconsistent. For a developer-portal screen like Member Explorer, that's acceptable. For a user-facing page, prefer the proxy pattern from Problem 3.

**Q110. Why did the same subagent not report `MemberController`'s `/members` 404 when the skip happened?**

Because the skip was deliberate and flagged in the report as "not in scope." The subagent executed rule 11 of its prompt ("too complex to port, drop a TODO"). This is a process lesson: the *human reviewer* must catch skipped work at PR time, not wait for a user bug report.

**Q111. What does `@GetMapping({"/api/payments/{id}/ticket", "/view/payments/{id}/download"})` mean with two paths?**

Spring registers the same handler method for both URL patterns. A request to either path invokes `downloadPaymentTicket(id)`. Useful for preserving a legacy URL while introducing a new one, or for aliasing.

**Q112. Why does the PDF proxy use `PaymentApiClient` instead of letting the browser redirect?**

A direct `return "redirect:" + backend + ...` is simpler *but* reveals the backend URL (`localhost:8080`) in the browser's address bar — which breaks encapsulation and creates a dependency on the backend being network-reachable from the browser (in prod it might only be reachable from the frontend JVM via a private subnet). The proxy keeps the backend URL server-side.

**Q113. What header pair actually triggers the browser's "Save As..." dialog for the PDF?**

`Content-Type: application/pdf` alone would open the PDF inline (in the browser's built-in viewer). Adding `Content-Disposition: attachment; filename="ticket-42.pdf"` forces the download prompt. The `setContentDispositionFormData("attachment", "ticket-42.pdf")` helper does this without manual header escaping.

**Q114. What's the memory cost of the PDF proxy compared to a redirect?**

Proxy: frontend allocates `byte[]` = full PDF size in heap (typically 50 KB – 5 MB). Redirect: zero bytes in frontend heap, browser fetches directly from backend. For a ticket-heavy site (100 concurrent downloads × 5 MB = 500 MB heap spike), prefer the redirect; for typical single-ticket downloads, proxy is fine and hides the backend.

**Q115. Could we have added a Spring `ViewResolver` that redirects `/api/*` globally on the frontend?**

Yes — a `RouterFunction` or `HandlerInterceptor` could catch all `/api/*` requests and 301-redirect them to `${backend.base-url}/api/...`. This is cleaner than per-endpoint proxies but leaks the backend URL to the browser. Pick based on whether you want the backend hidden or not.

**Q116. Why didn't we just hardcode `http://localhost:8080` in the proxy and skip the API client?**

`PaymentApiClient` already wraps: timeouts (3s connect / 15s read), error translation (HTTP errors → `BackendException`), and property-driven base URL. Bypassing it means reimplementing those concerns in the proxy, which is duplication. The client is "the one way to call the backend" and should be used consistently.

**Q117. A user hits `/api/payments/999999/ticket` but no payment 999999 exists. What does the frontend return?**

`PaymentApiClient.downloadTicketByPaymentId(999999)` eventually hits `AbstractApiClient.translate()` which converts the backend's 404 `HttpClientErrorException` into a `BackendException(404, "Payment not found...")`. Since the proxy handler doesn't catch `BackendException`, it propagates up to Spring's error handler — which returns 500. Ideally the handler should `try/catch BackendException` and map `.getStatus()` back to an HTTP status.

**Q118. Where would you add that try/catch refinement?**

Inside each proxy method:
```java
try {
    byte[] pdf = paymentApiClient.downloadTicketByPaymentId(id);
    ...
} catch (BackendException ex) {
    return ResponseEntity.status(ex.getStatus()).build();
}
```
Or better: a `@RestControllerAdvice` on the frontend that centralizes `BackendException → ResponseEntity` mapping.

**Q119. Why proxy `/api/bookings/{id}/ticket` too when the user only reported the payment URL bug?**

Defense-in-depth. The same 404 pattern applies to every backend `/api/*` that serves user-facing content. Fixing only the reported URL leaves similar bookmarks broken. Adding four proxy routes (single payment, group payment, single booking, group booking) costs ~30 lines of code and prevents a second bug report.

**Q120. After adding proxies, `http://localhost:8080/api/payments/42/ticket` still works directly. What serves it — frontend or backend?**

Backend. The frontend only listens on 8081; port 8080 is always backend. The proxy at 8081 is an *additional* path to the same resource. Think of it as a CDN that re-serves origin content — the origin is still reachable directly.

**Q121. You restart the frontend but `/team` still 404s. What's the first thing you check?**

1. `./mvnw.cmd compile` — did the compile succeed? Missing class file = no bean.
2. `find src/main/java/com/busfrontend/team -name "*.java"` — are the 5 files actually in the right package?
3. `grep "^package" src/main/java/com/busfrontend/team/TeamController.java` — does the package declaration match the folder? (A common `sed` typo produces `com.busfrontend.team.something`.)
4. Spring startup logs — does it print `Mapped "{[/team]...}"`? If not, the controller isn't being scanned.

**Q122. What's the Windows vs Linux gotcha with the `sed` copy one-liner used here?**

On Linux / Git Bash on Windows, `sed -E` works as shown. On macOS BSD `sed`, you'd need `sed -i '' -E ...` or pipe to a temp file. The paths with spaces (`gs studeis/`) also need careful quoting on Windows — always use `find -print0 | while IFS= read -r -d ''` for safety.

**Q123. The fix added two new Thymeleaf templates? No — why did `/team` work without adding templates?**

The templates (`team/members.html`, `team/profile.html`) were **already copied during the original split** (step 2 of the guide: "Copy templates + static"). The only missing piece was the Java handler. Once the controller compiled and registered, Spring's view resolver found the existing templates immediately.

**Q124. Could this entire hotfix be written as a single Spring-profile override?**

No — the fix introduces new beans (controllers, registries, value objects). Profiles can activate/deactivate existing beans but can't inject brand-new classes. You need the class files present on the classpath for Spring to find them.

**Q125. What's the next most likely post-split 404 after these three?**

`/api/bookings/group-ticket?bookingIds=...` — group booking ticket download. Also covered by our proactive fix. After that, any `/api/reviews/...` or `/api/customers/...` URL that sneaks into a template or external link. A defensive pattern: add a catch-all `@GetMapping("/api/**")` handler on the frontend that redirects to the backend, with a dev-mode warning banner saying "this link should point at :8080".

---

## 14.9 What Should Be Done Differently Next Time

1. **Audit skip lists during PR review.** The "skipped, too complex" bucket is always suspicious. Every item in it needs a one-sentence justification plus a grep-backed dependency count.
2. **Add a smoke-test endpoint list.** A `e2e-smoke.sh` script that `curl`s the top 20 user-facing URLs after each deploy would have caught all three 404s in seconds.
3. **Namespace legacy paths.** Adding `/api/**` proxies on the frontend is fine for now, but in the long run move them under a prefixed namespace like `/legacy/api/**` to make the compatibility-layer explicit.
4. **Prefer property-driven URLs everywhere.** Any `@Value("${server.port}")` in code that makes an *outbound* HTTP call is a red flag post-split — it almost always needs to become `@Value("${<service>.base-url}")`.

---

> **PART D — SUMMARY**
> - `/team` 404 → copied `com.busfrontend.team` package (5 files, verbatim except `package` line). Self-contained, no rewiring needed.
> - `/members` 404 → copied `com.busfrontend.members` package (7 files). Rewired 3 spots (`@Value` + two `redirect:` URL prefixes) from `server.port` self-dispatch to `backend.base-url` cross-service dispatch.
> - `/api/payments/{id}/ticket` 404 → added proxy `@GetMapping` on `PaymentViewController` (and also `BookingViewController` proactively) that pulls bytes via API client and streams them with proper `Content-Type` + `Content-Disposition`.
> - All 50 pre-existing tests still pass. Zero backend changes.
> - **PART D adds 25 viva questions (Q101–Q125). Running total: 125 viva questions for this guide.**
