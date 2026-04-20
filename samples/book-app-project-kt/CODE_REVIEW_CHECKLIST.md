# Code Review Checklist — `book-app-project-kt`

**Files reviewed:** `BookCollection.kt`, `Main.kt`, `Book.kt`, `BookStats.kt`, `SearchCriteria.kt`, `BookCollectionTest.kt`, `MainTest.kt`, `build.gradle.kts`, `data.json`

---

## 🔴 Critical — Data Loss / Crash Risk

- [x] **Non-atomic writes in `saveBooks()`** (`BookCollection.kt:43`)
  `File.writeText()` truncates the file then writes. A crash mid-write permanently corrupts `data.json`. Fix: write to a temp file, then atomically rename (`Files.move(..., ATOMIC_MOVE)`).

- [x] **`saveBooks()` has no error handling** (`BookCollection.kt:42–44`)
  `IOException` on disk-full or permission errors is unhandled. The in-memory list is already mutated at this point, leaving memory and disk silently out of sync with no error surfaced to the user.

- [x] **Classpath data file used as write target** (`BookCollection.kt:14–16`)
  When packaged as a JAR, `javaClass.getResource("/data.json")` resolves to a read-only path inside the JAR. `saveBooks()` will crash with `IOException`. The app needs a separate writable data path (e.g. `~/.bookapp/data.json`).
  *(Mitigated: fallback now logs a clear warning. Full path migration out of scope.)*

---

## 🟠 High — Bugs / Incorrect Behavior

- [x] **`allBooks` leaks the live mutable backing list** (`BookCollection.kt:26–27`)
  The property is typed as `List<Book>` but returns the actual `MutableList`. Any caller can cast and mutate it, bypassing validation and `saveBooks()`. Fix: `get() = books.toList()`.

- [x] **No input validation in `addBook()`** (`BookCollection.kt:47–51`)
  Blank titles, blank authors, `year = 0`, negative years, and far-future years are silently persisted.

- [x] **Empty title accepted and confirmed as success** (`Main.kt:81`, `MainTest.kt:209`)
  `handleAdd` trims input but doesn't guard against blank strings. The test even encodes this as expected behaviour.

- [ ] **`handleRemove` ignores `removeBook()`'s return value** (`Main.kt:103–105`)
  Always prints `"Book removed if it existed."` — user gets no feedback when a title isn't found.

- [ ] **Duplicate books can be silently added** (`BookCollection.kt:47–51`)
  No duplicate check in `addBook()`. Calling it twice with the same title adds two identical entries.

- [ ] **`findByAuthor` uses exact match while `search(authorQuery)` uses substring match** (`BookCollection.kt:72`, `BookCollection.kt:112`)
  Inconsistent API — `findByAuthor("Herbert")` returns nothing, but `search(authorQuery = "Herbert")` finds results.

---

## 🟡 Medium — Performance / Maintainability

- [ ] **Two nearly-identical search implementations** (`BookCollection.kt:76` and `101`)
  `searchBooks(SearchCriteria)` and `search(...)` duplicate the same filtering logic with different APIs. Additionally, `searchBooks` combines `searchText` across title OR author, while `search` treats them as separate independent filters — an undocumented semantic difference.

- [ ] **Reversed year range silently returns empty results** (`BookCollection.kt:83`, `Main.kt:142`)
  Neither search method validates `yearFrom <= yearTo`. The user gets an empty list with no warning.

- [ ] **`getStatistics()` gives wrong results when `year = 0` entries exist** (`BookCollection.kt:96–97`)
  `minByOrNull { it.year }` always returns "Mysterious Book" (year=0 in seed data) as the "oldest book". Fix: filter out `year <= 0` before computing.

- [ ] **Corrupted JSON exception is swallowed** (`BookCollection.kt:36`)
  Catch-all prints a console warning but returns normally. Callers can't distinguish "file not found" from "entire collection was silently reset to empty".

- [ ] **`getStatistics()` not exposed in the CLI** (`Main.kt:204`)
  Fully implemented and tested, but not reachable from the command line and absent from `showHelp()`.

- [ ] **`getStatistics()` accepts arbitrary external list** (`BookCollection.kt:91`)
  The optional `bookList` parameter has ambiguous intent and makes the API confusing.

- [ ] **No `markAsUnread` operation**
  Users can mark a book as read but cannot reverse it.

- [ ] **`BookCollection` constructor falls back to relative path `"data.json"`** (`BookCollection.kt:16`)
  If the classpath resource isn't found, the fallback silently resolves relative to CWD, which is unpredictable in a packaged JAR. Prefer throwing a clear startup error.

---

## 🟢 Low — Style / Minor Improvements

- [ ] **`Book` fields are all `var`** (`Book.kt:3–8`)
  `title`, `author`, and `year` should be `val`. Only `read` needs to be mutable.

- [ ] **`showHelp()` uses `trimEnd()` instead of `trimIndent()`** (`Main.kt:192`)
  Preserves source-level indentation in output, causing every help line to print with extra leading spaces.

- [ ] **Service-layer lookups don't trim whitespace** (`BookCollection.kt:55`)
  `findBookByTitle("Dune ")` won't match `"Dune"`. CLI trims in `Main.kt`, but direct API callers are unprotected.

- [ ] **`parseSearchCriteria` silently discards invalid year values** (`Main.kt:48–58`)
  `toIntOrNull()` on non-numeric input silently ignores the filter — user gets no feedback.

- [ ] **Seed data contains an invalid entry** (`data.json:25–29`)
  `"Mysterious Book"` has an empty author and `"year": 0`.
  *(Intentionally kept — educational course material demonstrating edge cases.)*

- [ ] **`MainTest` stream-redirection helpers are not thread-safe** (`MainTest.kt:94–115`)
  `System.setOut`/`System.setIn` mutate global JVM state. Parallel test execution (JUnit 5 default) can cause tests to interfere with each other's captured output/input.

- [x] **`MainTest` encodes a bug as expected behaviour** (`MainTest.kt:209–217`)
  The `handleAdd with empty title` test asserts success, cementing the missing validation as intentional.

- [ ] **No dedicated `findByAuthor` unit tests** (`BookCollectionTest.kt`)
  Case-insensitivity, no-match, and multiple-match edge cases are untested at the service level.
