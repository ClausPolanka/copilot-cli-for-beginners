package bookapp.services

import bookapp.models.Book
import bookapp.models.BookStats
import bookapp.models.SearchCriteria
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class BookCollection(dataFile: String? = null) {

    private val dataFile: String = dataFile ?: defaultDataFilePath()

    companion object {
        // Resolves the data file location:
        // - JAR execution: next to the JAR file
        // - Development (gradlew run / tests): project root (user.dir)
        internal fun defaultDataFilePath(): String {
            val appDir = runCatching {
                File(BookCollection::class.java.protectionDomain.codeSource.location.toURI()).let {
                    if (it.isFile) it.parentFile else File(System.getProperty("user.dir"))
                }
            }.getOrElse { File(System.getProperty("user.dir")) }
            return File(appDir, "bookapp_data.json").absolutePath
        }
    }

    private var books: MutableList<Book> = mutableListOf()

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    init {
        loadBooks()
    }

    val allBooks: List<Book>
        get() = books.toList()

    private fun loadBooks() {
        try {
            val json = File(dataFile).readText()
            val type = object : TypeToken<MutableList<Book>>() {}.type
            books = gson.fromJson(json, type) ?: mutableListOf()
        } catch (_: FileNotFoundException) {
            books = mutableListOf()
        } catch (_: Exception) {
            println("Warning: data.json is corrupted. Starting with empty collection.")
            books = mutableListOf()
        }
    }

    private fun saveBooks() {
        val json = gson.toJson(books)
        val targetFile = File(dataFile)
        val tmpFile = File(dataFile + ".tmp")
        // Write to a temp file first, then atomically rename it to the target.
        // This ensures data.json is never left in a partial or empty state if
        // the process crashes or the disk fills up mid-write.
        try {
            tmpFile.writeText(json)
            try {
                Files.move(
                    tmpFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
                // Filesystem doesn't support atomic moves; fall back to a regular replace
                Files.move(tmpFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            tmpFile.delete() // Remove partial temp file to avoid a corrupt rename on the next write
            throw e
        }
    }

    fun addBook(title: String, author: String, year: Int): Book {
        val book = Book(title = title, author = author, year = year)
        books.add(book)
        try {
            saveBooks()
        } catch (e: Exception) {
            books.remove(book) // rollback: keep memory and disk in sync
            throw e
        }
        return book
    }

    fun findBookByTitle(title: String): Book? {
        return books.find { it.title.equals(title, ignoreCase = true) }
    }

    fun markAsRead(title: String): Boolean {
        val book = findBookByTitle(title) ?: return false
        book.read = true
        try {
            saveBooks()
        } catch (e: Exception) {
            book.read = false // rollback
            throw e
        }
        return true
    }

    fun removeBook(title: String): Boolean {
        val book = findBookByTitle(title) ?: return false
        val originalIndex = books.indexOf(book)
        books.remove(book)
        try {
            saveBooks()
        } catch (e: Exception) {
            books.add(originalIndex, book) // rollback: restore at original position
            throw e
        }
        return true
    }

    fun findByAuthor(author: String): List<Book> {
        return books.filter { it.author.equals(author, ignoreCase = true) }
    }

    fun searchBooks(criteria: SearchCriteria): List<Book> {
        return books.filter { book ->
            val matchesText = criteria.searchText?.let {
                book.title.contains(it, ignoreCase = true) || 
                book.author.contains(it, ignoreCase = true)
            } ?: true
            
            val matchesYearFrom = criteria.yearFrom?.let { book.year >= it } ?: true
            val matchesYearTo = criteria.yearTo?.let { book.year <= it } ?: true
            val matchesReadStatus = criteria.readStatus?.let { book.read == it } ?: true
            
            matchesText && matchesYearFrom && matchesYearTo && matchesReadStatus
        }
    }

    fun getStatistics(bookList: List<Book> = books): BookStats {
        return BookStats(
            totalCount = bookList.size,
            readCount = bookList.count { it.read },
            unreadCount = bookList.count { !it.read },
            oldestBook = bookList.minByOrNull { it.year },
            newestBook = bookList.maxByOrNull { it.year }
        )
    }

    fun search(
        titleQuery: String? = null,
        authorQuery: String? = null,
        yearFrom: Int? = null,
        yearTo: Int? = null,
        read: Boolean? = null
    ): List<Book> {
        return books.filter { book ->
            val matchesTitle = titleQuery == null || 
                book.title.contains(titleQuery, ignoreCase = true)
            val matchesAuthor = authorQuery == null || 
                book.author.contains(authorQuery, ignoreCase = true)
            val matchesYearFrom = yearFrom == null || book.year >= yearFrom
            val matchesYearTo = yearTo == null || book.year <= yearTo
            val matchesRead = read == null || book.read == read

            matchesTitle && matchesAuthor && matchesYearFrom && matchesYearTo && matchesRead
        }
    }
}
