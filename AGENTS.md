# AGENTS.md — konwencje projektu

Ten plik opisuje dobre praktyki, zasady Clean Code i zalecaną strukturę katalogów w tym repozytorium. Przy dodawaniu lub zmianie funkcji **trzymaj się tych reguł**.

## Stack technologiczny

| Obszar | Technologia |
|--------|-------------|
| Język | Java 25 |
| Framework | Spring Boot 4.x |
| Build | Gradle |
| Persystencja | Spring Data JPA, PostgreSQL (H2 w testach) |
| Bezpieczeństwo | Spring Security, JWT (jjwt) |
| Walidacja | Jakarta Validation |
| Dokumentacja API | springdoc-openapi |
| Testy | JUnit 5, MockMvc, AssertJ |
| Pokrycie | JaCoCo |

---

## Ogólna zasada architektury

Używamy **pakietowania po funkcji (feature)**, a wewnątrz każdej funkcji — **warstw technicznych**. Nie tworzymy globalnych pakietów `controller`, `service`, `repository` obejmujących całą aplikację.

`Application.java` pozostaje w korzeniu pakietu `com.example.springboot`, żeby Spring Boot skanował wszystkie podpakiety.

---

## Struktura repozytorium

```
admin-panel-java/
├── .github/workflows/ci.yml     # CI — ./gradlew test na każdy push/PR
├── build.gradle                 # zależności, Java toolchain, JaCoCo
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── AGENTS.md                    # ten plik
│
├── src/main/java/com/example/springboot/
│   ├── Application.java         # punkt wejścia Spring Boot
│   │
│   ├── common/                  # elementy współdzielone między modułami
│   │   └── web/
│   │       ├── ApiError.java
│   │       └── GlobalExceptionHandler.java
│   │
│   ├── auth/                    # moduł autentykacji
│   │   ├── web/                 # AuthController, body requestów
│   │   ├── service/             # AuthService — logika logowania, tokenów
│   │   ├── domain/              # AppUser, RefreshToken (@Entity)
│   │   ├── repository/          # UserRepository, RefreshTokenRepository
│   │   ├── security/            # SecurityConfig, JwtService, filtry
│   │   └── exception/           # AuthException
│   │
│   └── product/                 # moduł produktów (wzór dla nowych feature'ów)
│       ├── web/                 # ProductController
│       ├── service/             # ProductService
│       ├── domain/              # Product (@Entity)
│       ├── repository/            # ProductRepository
│       └── dto/                   # ProductDto
│
├── src/main/resources/
│   └── application.yaml         # konfiguracja domyślna (bez sekretów produkcyjnych)
│
└── src/test/java/com/example/springboot/
    ├── auth/web/                # AuthControllerTest
    ├── product/web/             # ProductControllerTest
    └── common/web/              # GlobalExceptionHandlerTest
```

Testy w `src/test/java` **odzwierciedlają** strukturę modułów produkcyjnych (np. `auth/web/AuthControllerTest.java`).

---

## Warstwy wewnątrz modułu

| Pakiet | Zawartość | Zasady |
|--------|-----------|--------|
| `web` | `@RestController`, body requestów (rekordy z walidacją) | Cienkie kontrolery — deleguj logikę do `service` |
| `service` | `@Service`, logika biznesowa, `@Transactional` | Nie zwracaj encji JPA na zewnątrz modułu — mapuj na DTO |
| `domain` | encje JPA (`@Entity`) | Tylko model persystencji danego modułu |
| `repository` | interfejsy `JpaRepository` | Bez logiki biznesowej — tylko zapytania |
| `dto` | rekordy odpowiedzi API | Oddzielone od encji; metody mapowania `from(Entity)` |
| `security` | JWT, filtry, `SecurityConfig`, `@ConfigurationProperties` | Tylko w module `auth` |
| `exception` | wyjątki specyficzne dla modułu | Np. `AuthException` w `auth.exception` |

---

## Clean Code — zasady obowiązujące w projekcie

### Nazewnictwo

- **Klasy:** rzeczowniki w PascalCase — `ProductService`, `AuthController`, `JwtAuthFilter`.
- **Metody:** czasowniki opisujące intencję — `login`, `listProducts`, `from`.
- **Zmienne i parametry:** pełne, znaczące nazwy — `rawRefreshToken`, nie `t` ani `data`.
- **Stałe:** `UPPER_SNAKE_CASE` — np. `REFRESH_COOKIE`.
- **Pakiety:** małe litery, jeden segment = jeden moduł lub warstwa — `auth.service`, nie `services.auth`.

### Funkcje i klasy

- **Jedna odpowiedzialność (SRP):** kontroler mapuje HTTP, serwis realizuje reguły biznesowe, repozytorium persystuje dane.
- **Małe metody:** jeśli metoda robi więcej niż jedną rzecz logiczną, wydziel pomocniczą metodę prywatną (np. `refreshCookie()` w `AuthController`).
- **Mało argumentów:** preferuj rekordy request body zamiast długich list parametrów.
- **Brak efektów ubocznych w niespodziewanych miejscach:** zapis do bazy tylko w serwisie, nie w kontrolerze.
- **Wstrzykiwanie zależności przez konstruktor** — bez `@Autowired` na polach.

### Czytelność

- **Nie komentuj oczywistego** — kod powinien być samoopisujący. Komentarze tylko przy nietrywialnej logice biznesowej lub ograniczeniach technicznych.
- **Unikaj magicznych liczb i stringów** — wyciągaj do stałych lub konfiguracji (`AuthProperties`, `application.yaml`).
- **Preferuj `var` tam, gdzie typ jest oczywisty** z prawej strony przypisania.
- **Rekordy Java** dla niemutowalnych DTO i prostych body requestów z walidacją (`LoginBody`, `ProductDto`).

### Obsługa błędów

- **Wyjątki biznesowe** jako dedykowane typy (`AuthException`) z kodem i statusem HTTP.
- **Nie łap ogólnego `Exception`** w warstwie biznesowej — pozwól `GlobalExceptionHandler` obsłużyć błędy spójnie.
- **Spójny format odpowiedzi błędu:** `{ "code": "...", "message": "..." }` przez `ApiError`.

### DRY i YAGNI

- **Nie duplikuj** mapowania encja → DTO — jedna metoda `ProductDto.from(Product)`.
- **Nie buduj abstrakcji „na zapas"** — wspólny kod trafia do `common` dopiero gdy używają go co najmniej dwa moduły.
- **Nie over-engineeruj** — prosty serwis z jedną metodą jest lepszy niż zbędne interfejsy i fabryki.

---

## Konwencje REST i Spring

### Endpointy

- Prefiks API: `/api/...`
- Kontrolery w `{moduł}.web`, mapowanie przez `@RequestMapping` na poziomie klasy.
- Body requestów jako **rekordy wewnętrzne kontrolera** lub w tym samym pliku co kontroler (dla prostych przypadków).
- Odpowiedzi REST jako **DTO**, nigdy encje JPA.

### Walidacja

- `@Valid` na body requestu w kontrolerze.
- Adnotacje Jakarta Validation na polach rekordu (`@NotBlank`, `@Email`, itd.).
- Błędy walidacji obsługuje `GlobalExceptionHandler` → `400 BAD_REQUEST`.

### Logowanie

| Poziom | Kiedy |
|--------|-------|
| `INFO` | Granice API — wejście/wyjście (np. `GET /api/products -> 5 item(s)`) |
| `DEBUG` | Szczegóły w serwisach i kontrolerach auth (bez haseł i tokenów) |
| `WARN` | Błędy autentykacji, podejrzane żądania |

**Nigdy nie loguj:** haseł, surowych tokenów JWT/refresh, pełnych danych wrażliwych.

### Transakcje

- `@Transactional` na metodach serwisu modyfikujących dane.
- `spring.jpa.open-in-view: false` — lazy loading poza transakcją serwisu jest niedozwolony.

---

## Pakiet `common`

Trafiają tu wyłącznie elementy **używane przez wiele modułów**:

- format błędów API (`ApiError`)
- globalny handler wyjątków (`GlobalExceptionHandler`)

Nie umieszczaj w `common` logiki specyficznej dla jednego modułu (np. JWT, encje użytkownika, konfiguracja security).

---

## Bezpieczeństwo

- Konfiguracja security w `auth.security` (`SecurityConfig`, `JwtAuthFilter`, `JwtService`).
- Sekrety JWT i hasła DB **tylko** przez zmienne środowiskowe lub profile lokalne — nigdy w repo produkcyjnym.
- Refresh token w **HttpOnly cookie** (`path=/api/auth`), access token w body odpowiedzi.
- Hasła użytkowników hashowane przez `PasswordEncoder` (BCrypt).
- Tokeny refresh przechowywane jako hash w bazie (`TokenHasher`).

---

## Konfiguracja

```
src/main/resources/application.yaml   # domyślna konfiguracja
src/test/resources/application.yaml   # konfiguracja testowa (H2, krótsze TTL)
```

- Wartości wrażliwe przez `${ENV_VAR:default}` — domyślne tylko do dev/test.
- Właściwości aplikacji pod prefiksem `app.*` (np. `app.auth.jwt.access-secret`).
- Mapowanie na rekordy przez `@ConfigurationProperties` w odpowiednim module (`AuthProperties`).

---

## Testy

### Organizacja

- Testy kontrolerów: `@SpringBootTest` + `@AutoConfigureMockMvc` w `{moduł}/web/`.
- Testy handlera wyjątków: jednostkowo w `common/web/`.
- Struktura katalogów testowych = struktura produkcyjna.

### Dobre praktyki testowe

- **Arrange–Act–Assert** — czytelny podział w każdym teście.
- **Jeden scenariusz na test** — osobne metody dla login sukces, login błędne hasło, refresh wygasły token.
- **Aserty AssertJ** — `assertThat(...).isEqualTo(...)`, nie JUnit `assertEquals` tam gdzie możliwe.
- **Czyszczenie danych** w `@BeforeEach` — `deleteAll()` repozytoriów przed każdym testem integracyjnym.
- **JSON w testach** — text blocks (`""" ... """`) dla czytelności body requestów.
- **Nie testuj frameworka** — testuj zachowanie aplikacji (status HTTP, cookie, struktura odpowiedzi).

### Uruchamianie

```bash
./gradlew.bat test          # Windows
./gradlew test              # Linux/macOS
```

Raport JaCoCo: `build/reports/jacoco/test/html/index.html`

---

## Nowy moduł (feature) — checklist

Przy dodawaniu np. `order`:

1. Utwórz `com.example.springboot.order` z podpakietami: `web`, `service`, `domain`, `repository`, `dto`.
2. Endpointy pod `/api/orders` w dedykowanym kontrolerze w `order.web`.
3. Serwis w `order.service` — logika biznesowa, mapowanie na DTO.
4. Encja w `order.domain`, repozytorium w `order.repository`.
5. Wyjątki modułowe w `order.exception` + handler w `GlobalExceptionHandler` (jeśli potrzebny).
6. Testy w `src/test/java/.../order/web/OrderControllerTest.java`.
7. Nie importuj klas z `domain` innego modułu — komunikacja między modułami przez ID, serwisy aplikacyjne lub zdarzenia.

---

## Zależności między modułami

```
✅ Dozwolone:     moduł → common, moduł → własne podpakiety
❌ Unikaj:        product → auth.domain.AppUser
                  (wyjątek: testy integracyjne — preferuj fabryki/test helpers)
```

Współdzielona konfiguracja Spring (CORS, security) zostaje w module odpowiedzialnym za bezpieczeństwo (`auth.security`).

---

## Czego nie robić

- Nie wracaj do płaskiej struktury (`auth/AuthService.java` obok `auth/AppUser.java` bez podpakietów).
- Nie twórz globalnego pakietu `entity`, `model` ani `controller` dla całej aplikacji.
- Nie duplikuj `GlobalExceptionHandler` w każdym module.
- Nie umieszczaj logiki biznesowej w repozytoriach ani kontrolerach.
- Nie zwracaj encji JPA bezpośrednio z endpointów REST.
- Nie commituj plików z sekretami (`.env`, klucze produkcyjne).
- Nie wyłączaj testów ani hooków CI bez uzasadnienia.

---

## Weryfikacja przed merge

```bash
./gradlew.bat test
```

Wszystkie testy muszą przechodzić. CI uruchamia `./gradlew test` na każdy push i pull request.
