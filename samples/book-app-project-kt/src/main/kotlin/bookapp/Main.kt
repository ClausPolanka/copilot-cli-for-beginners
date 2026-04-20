package bookapp

import bookapp.models.Book
import bookapp.models.SearchCriteria
import bookapp.services.BookCollection

fun showBooks(books: List<Book>) {
    if (books.isEmpty()) {
        println("No books found.")
        return
    }

    println("\nYour Book Collection:\n")

    books.forEachIndexed { index, book ->
        val status = if (book.read) "✓" else " "
        println("${index + 1}. [$status] ${book.title} by ${book.author} (${book.year})")
    }

    println()
}

fun handleList(collection: BookCollection, args: Array<String>) {
    val criteria = parseSearchCriteria(args)
    val books = collection.searchBooks(criteria)
    showBooks(books)
}

fun parseSearchCriteria(args: Array<String>): SearchCriteria {
    var searchText: String? = null
    var yearFrom: Int? = null
    var yearTo: Int? = null
    var readStatus: Boolean? = null
    
    var i = 1 // Skip the "list" command itself
    while (i < args.size) {
        when (args[i]) {
            "--text" -> {
                if (i + 1 < args.size) {
                    searchText = args[i + 1]
                    i += 2
                } else {
                    i++
                }
            }
            "--year-from" -> {
                if (i + 1 < args.size) {
                    yearFrom = args[i + 1].toIntOrNull()
                    i += 2
                } else {
                    i++
                }
            }
            "--year-to" -> {
                if (i + 1 < args.size) {
                    yearTo = args[i + 1].toIntOrNull()
                    i += 2
                } else {
                    i++
                }
            }
            "--read" -> {
                readStatus = true
                i++
            }
            "--unread" -> {
                readStatus = false
                i++
            }
            else -> i++
        }
    }
    
    return SearchCriteria(searchText, yearFrom, yearTo, readStatus)
}

fun handleAdd(collection: BookCollection) {
    println("\nAdd a New Book\n")

    print("Title: ")
    val title = readlnOrNull()?.trim() ?: ""

    if (title.isBlank()) {
        println("\nError: Title cannot be empty.\n")
        return
    }

    print("Author: ")
    val author = readlnOrNull()?.trim() ?: ""

    print("Year: ")
    val yearStr = readlnOrNull()?.trim() ?: ""

    val year = yearStr.toIntOrNull()
    if (year != null) {
        try {
            collection.addBook(title, author, year)
            println("\nBook added successfully.\n")
        } catch (e: Exception) {
            println("\nError: Could not save book. ${e.message}\n")
        }
    } else {
        println("\nError: '$yearStr' is not a valid year.\n")
    }
}

fun handleRemove(collection: BookCollection) {
    println("\nRemove a Book\n")

    print("Enter the title of the book to remove: ")
    val title = readlnOrNull()?.trim() ?: ""

    try {
        if (collection.removeBook(title)) {
            println("\nBook removed successfully.\n")
        } else {
            println("\nBook not found.\n")
        }
    } catch (e: Exception) {
        println("\nError: Could not save changes. ${e.message}\n")
    }
}

fun handleFind(collection: BookCollection) {
    println("\nFind Books by Author\n")

    print("Author name: ")
    val author = readlnOrNull()?.trim() ?: ""
    val books = collection.findByAuthor(author)

    showBooks(books)
}

fun handleRead(collection: BookCollection) {
    println("\nMark a Book as Read\n")

    print("Enter the title of the book: ")
    val title = readlnOrNull()?.trim() ?: ""

    try {
        if (collection.markAsRead(title)) {
            println("\nBook marked as read.\n")
        } else {
            println("\nBook not found.\n")
        }
    } catch (e: Exception) {
        println("\nError: Could not save changes. ${e.message}\n")
    }
}

fun handleSearch(collection: BookCollection) {
    println("\nSearch and Filter Books\n")

    print("Title (or press Enter to skip): ")
    val title = readlnOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    print("Author (or press Enter to skip): ")
    val author = readlnOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    print("Year from (or press Enter to skip): ")
    val yearFromStr = readlnOrNull()?.trim()
    val yearFrom = yearFromStr?.toIntOrNull()

    print("Year to (or press Enter to skip): ")
    val yearToStr = readlnOrNull()?.trim()
    val yearTo = yearToStr?.toIntOrNull()

    print("Read status (read/unread or press Enter to skip): ")
    val readStatus = readlnOrNull()?.trim()?.lowercase()
    val read = when (readStatus) {
        "read" -> true
        "unread" -> false
        else -> null
    }

    val results = collection.search(
        titleQuery = title,
        authorQuery = author,
        yearFrom = yearFrom,
        yearTo = yearTo,
        read = read
    )

    println()
    showBooks(results)
}

fun showHelp() {
    println(
        """
        
    Book Collection Helper

    Commands:
      list                           - Show all books
      list --text <query>            - Search books by title or author
      list --year-from <year>        - Filter books from year onwards
      list --year-to <year>          - Filter books up to year
      list --read                    - Show only read books
      list --unread                  - Show only unread books
      add                            - Add a new book
      read                           - Mark a book as read
      remove                         - Remove a book by title
      find                           - Find books by author
      search                         - Search and filter books interactively
      help                           - Show this help message
      
    Examples:
      list --text Tolkien                    - Find books with "Tolkien" in title or author
      list --year-from 2000 --unread         - Unread books from 2000 onwards
      list --text Harry --year-to 2010       - Books with "Harry" published up to 2010
        """.trimEnd()
    )
}

fun main(args: Array<String>) {
    val collection = BookCollection()

    if (args.isEmpty()) {
        showHelp()
        return
    }

    when (args[0].lowercase()) {
        "list"   -> handleList(collection, args)
        "add"    -> handleAdd(collection)
        "read"   -> handleRead(collection)
        "remove" -> handleRemove(collection)
        "find"   -> handleFind(collection)
        "search" -> handleSearch(collection)
        "help"   -> showHelp()
        else     -> {
            println("Unknown command.\n")
            showHelp()
        }
    }
}
