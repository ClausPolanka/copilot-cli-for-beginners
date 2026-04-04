# Book Collection App

*(This README is intentionally rough so you can improve it with GitHub Copilot CLI)*

A Kotlin console app for managing books you have or want to read.
It can add, remove, and list books. Also mark them as read.

---

## Current Features

* Reads books from a JSON file (our database)
* Input checking is weak in some areas
* Some tests exist but probably not enough

---

## Files

* `src/main/kotlin/bookapp/Main.kt` - Main CLI entry point
* `src/main/kotlin/bookapp/models/Book.kt` - Book data class
* `src/main/kotlin/bookapp/services/BookCollection.kt` - BookCollection class with data logic
* `data.json` - Sample book data
* `src/test/kotlin/bookapp/BookCollectionTest.kt` - JUnit tests

---

## Running the App

```bash
./gradlew run --args="list"
./gradlew run --args="add"
./gradlew run --args="find"
./gradlew run --args="remove"
./gradlew run --args="help"
```

## Running Tests

```bash
./gradlew test
```

---

## Notes

* Not production-ready (obviously)
* Some code could be improved
* Could add more commands later
