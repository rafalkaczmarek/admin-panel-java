# AGENTS.md — konwencje projektu

Ten plik opisuje dobre praktyki organizacji kodu w tym repozytorium. Przy dodawaniu lub zmianie funkcji **trzymaj się tej struktury**.

## Ogólna zasada

Używamy **pakietowania po funkcji (feature)**, a wewnątrz każdej funkcji — **warstw technicznych**. Nie tworzymy globalnych pakietów `controller`, `service`, `repository` obejmujących całą aplikację.

`Application.java` pozostaje w korzeniu pakietu `com.example.springboot`, żeby Spring Boot skanował wszystkie podpakiety.

## Struktura katalogów

```
src/main/java/com/example/springboot/
├── Application.java
├── common/                    # elementy współdzielone między modułami
│   └── web/
│       ├── ApiError.java
│       └── GlobalExceptionHandler.java
├── auth/                      # moduł autentykacji
│   ├── web/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   ├── security/
│   └── exception/
└── product/                   # moduł produktów (wzór dla nowych feature’ów)
    ├── web/
    ├── service/
    ├── domain/
    ├── repository/
    └── dto/
```

Testy w `src/test/java` **odzwierciedlają** tę samą strukturę (np. `auth/web/AuthControllerTest.java`).

## Warstwy wewnątrz modułu

| Pakiet | Zawartość | Zasady |
|--------|-----------|--------|
| `web` | `@RestController`, body requestów (rekordy z walidacją) | Cienkie kontrolery — deleguj logikę do `service` |
| `service` | `@Service`, logika biznesowa, transakcje | Nie zwracaj encji JPA na zewnątrz modułu — mapuj na DTO |
| `domain` | encje JPA (`@Entity`) | Tylko model persystencji danego modułu |
| `repository` | interfejsy `JpaRepository` | Bez logiki biznesowej |
| `dto` | rekordy odpowiedzi API | Oddzielone od encji; metody mapowania `from(Entity)` |
| `security` | JWT, filtry, `SecurityConfig`, `@ConfigurationProperties` | Tylko w module `auth` (lub przyszłym `security`, jeśli wyodrębnimy) |
| `exception` | wyjątki specyficzne dla modułu | Np. `AuthException` w `auth.exception` |

## Pakiet `common`

Trafiają tu wyłącznie elementy **używane przez wiele modułów**:

- format błędów API (`ApiError`)
- globalny handler wyjątków (`GlobalExceptionHandler`)

Nie umieszczaj w `common` logiki specyficznej dla jednego modułu (np. JWT, encje użytkownika).

## Nowy moduł (feature)

Przy dodawaniu np. `order`:

1. Utwórz `com.example.springboot.order` z podpakietami: `web`, `service`, `domain`, `repository`, `dto` (oraz inne tylko jeśli potrzebne).
2. Endpointy pod `/api/...` w dedykowanym kontrolerze w `order.web`.
3. Testy w `src/test/java/.../order/web/`.
4. Nie importuj klas z `domain` innego modułu — komunikacja między modułami przez ID, serwisy aplikacyjne lub zdarzenia, nie przez bezpośrednie `@Autowired` encji sąsiada.

## Zależności między modułami

- **Dozwolone:** moduł → `common`, moduł → własne podpakiety.
- **Unikaj:** `product` importuje `auth.domain.AppUser` (wyjątek: testy integracyjne mogą tworzyć dane pomocnicze — preferuj fabryki/test helpers).
- Współdzielona konfiguracja Spring (CORS, security) zostaje w module odpowiedzialnym za bezpieczeństwo (`auth.security`).

## Konwencje kodu

- **Kontrolery:** tylko mapowanie HTTP, walidacja wejścia, wywołanie serwisu.
- **DTO zamiast encji** w odpowiedziach REST.
- **Rekordy** (Java records) dla DTO i prostych body requestów.
- **Logowanie:** `INFO` na granicach API (wejście/wyjście), `DEBUG` w serwisach, `WARN` przy błędach auth.
- **Wyjątki biznesowe:** dedykowany typ + obsługa w `GlobalExceptionHandler` → spójny JSON `{ "code", "message" }`.
- **Sekrety:** tylko zmienne środowiskowe / profile (`application-local.yaml`), nigdy w repo.

## Czego nie robić

- Nie wracaj do płaskiej struktury (`auth/AuthService.java` obok `auth/AppUser.java` bez podpakietów).
- Nie twórz globalnego pakietu `entity` lub `model` dla całej aplikacji.
- Nie duplikuj `GlobalExceptionHandler` w każdym module.
- Nie umieszczaj logiki biznesowej w repozytoriach ani kontrolerach.

## Weryfikacja

Po większych zmianach struktury:

```bash
./gradlew.bat test
```

Wszystkie testy muszą przechodzić przed merge.
