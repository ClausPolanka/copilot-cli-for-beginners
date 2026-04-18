# Code Review — book-app-project-kt

Reviewed files:
- `src/main/kotlin/bookapp/services/BookCollection.kt`
- `src/main/kotlin/bookapp/Main.kt`
- `src/main/kotlin/bookapp/models/Book.kt`
- `src/main/kotlin/bookapp/models/BookStats.kt`
- `src/main/kotlin/bookapp/models/SearchCriteria.kt`
- `src/test/kotlin/bookapp/BookCollectionTest.kt`
- `src/test/kotlin/bookapp/MainTest.kt`

---

## 🔴 High Severity

- [ ] **`saveBooks()` has no error handling** (`BookCollection.kt:42`)
  `File.writeText()` throws `IOException` on permission errors or disk-full. The in-memory list has already been mutated at this point, so a failed save silently leaves memory and disk out of sync with no error surfaced to the caller.

- [ ] **`addBook()` performs no input validation** (`BookCollection.kt:47`)
  Blank titles (`""`/`"   "`), blank authors, `year = 0`, negative years, and far-future years are all silently accepted and persisted. The `Book` model defaults `year = 0`, so a Gson deserialization failure on the year field also produces a silent invalid entry.

- [ ] **Empty title is accepted and confirmed as success** (`Main.kt:81`, `MainTest.kt:209`)
  `handleAdd` trims input but does not guard against blank strings before calling `addBook`. `MainTest` even asserts this as expected behaviour, encoding the bug as a feature.

---

## 🟡 Medium Severity

- [ ] **Duplicate search implementations** (`BookCollection.kt:76` and `BookCollection.kt:101`)
  `searchBooks(SearchCriteria)` and `search(titleQuery, authorQuery, ...)` perform identical filtering with different APIs. One should delegate to the other, or one should be removed. Having both creates a maintenance hazard and confusion for callers.

- [ ] **Reversed year range silently returns no results** (`BookCollection.kt:83`, `Main.kt:142`)
  Neither `searchBooks` nor `search` validates that `yearFrom <= yearTo`. Passing `yearFrom = 2024, yearTo = 1800` produces an empty list with no warning.

- [ ] **`handleRemove` never reports failure** (`Main.kt:103-105`)
  The message "Book removed if it existed." is printed regardless of whether the book was found. `removeBook()` returns `Boolean`, but `handleRemove` ignores it, giving the user no feedback when a title isn't found.

- [ ] **Corrupted JSON is swallowed silently** (`BookCollection.kt:36`)
  The catch-all `Exception` handler prints a console warning but the method returns normally. Callers cannot distinguish "file not found (expected)" from "data was corrupted and the entire collection was lost".

- [ ] **`getStatistics` accepts an arbitrary external list** (`BookCollection.kt:91`)
  The optional `bookList` parameter allows passing any `List<Book>` unrelated to the collection. The intent is unclear and the default parameter makes the API ambiguous. Consider a separate overload or removing the parameter.

- [ ] **`stats` command is not exposed in the CLI** (`Main.kt:204`)
  `getStatistics()` is fully implemented and well-tested but is not reachable from the command line. It is also absent from `showHelp()`.

---

## 🟢 Low Severity

- [ ] **`Book` fields are all `var` (mutable)** (`Book.kt:3`)
  All fields are mutable, allowing any code holding a `Book` reference to change its data. `title`, `author`, and `year` should be `val`; only `read` needs to be `var`. Returning books via `allBooks` or search results exposes this mutability.

- [ ] **`showHelp()` uses `trimEnd()` instead of `trimIndent()`** (`Main.kt:192`)
  The triple-quoted string uses `trimEnd()`, which only removes trailing whitespace. The leading indentation from the source code is preserved in the output, causing every help line to be printed with extra leading spaces.

- [ ] **Whitespace not trimmed in service-layer lookups** (`BookCollection.kt:55`)
  `findBookByTitle` uses `equals(ignoreCase = true)` but does not trim. A title with a trailing space (e.g. `"Dune "`) will not match `"Dune"`. The CLI trims in `Main.kt`, but direct API callers are not protected.

- [ ] **No test for `findByAuthor` in isolation** (`BookCollectionTest.kt`)
  `findByAuthor` is used in `MainTest` but has no dedicated unit test in `BookCollectionTest`. Edge cases (case-insensitivity, no matches, multiple matches) are not covered at the service level.

- [ ] **`parseSearchCriteria` silently discards invalid year values** (`Main.kt:48`)
  `toIntOrNull()` on a non-numeric `--year-from` or `--year-to` argument sets the field to `null` with no feedback. The user receives no indication their filter was ignored.
