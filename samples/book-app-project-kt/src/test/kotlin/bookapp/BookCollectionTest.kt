package bookapp

import bookapp.models.SearchCriteria
import bookapp.services.BookCollection
import java.io.File
import java.io.IOException
import kotlin.test.*

class BookCollectionTest {

    private lateinit var tempFile: File
    private lateinit var collection: BookCollection

    @BeforeTest
    fun setUp() {
        tempFile = File.createTempFile("books", ".json")
        tempFile.writeText("[]")
        collection = BookCollection(tempFile.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        if (tempFile.exists()) tempFile.delete()
    }

    @Test
    fun `addBook should add and persist`() {
        val initialCount = collection.allBooks.size
        collection.addBook("1984", "George Orwell", 1949)

        assertEquals(initialCount + 1, collection.allBooks.size)

        val book = collection.findBookByTitle("1984")
        assertNotNull(book)
        assertEquals("George Orwell", book.author)
        assertEquals(1949, book.year)
        assertFalse(book.read)
    }

    @Test
    fun `markAsRead should set read to true`() {
        collection.addBook("Dune", "Frank Herbert", 1965)
        val result = collection.markAsRead("Dune")

        assertTrue(result)
        assertTrue(collection.findBookByTitle("Dune")!!.read)
    }

    @Test
    fun `markAsRead nonexistent book should return false`() {
        val result = collection.markAsRead("Nonexistent Book")
        assertFalse(result)
    }

    @Test
    fun `removeBook should remove existing book`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        val result = collection.removeBook("The Hobbit")

        assertTrue(result)
        assertNull(collection.findBookByTitle("The Hobbit"))
    }

    @Test
    fun `removeBook nonexistent book should return false`() {
        val result = collection.removeBook("Nonexistent Book")
        assertFalse(result)
    }

    @Test
    fun `getStatistics should return correct counts`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)
        collection.markAsRead("Dune")

        val stats = collection.getStatistics()

        assertEquals(3, stats.totalCount)
        assertEquals(1, stats.readCount)
        assertEquals(2, stats.unreadCount)
    }

    @Test
    fun `getStatistics should find oldest and newest books`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)

        val stats = collection.getStatistics()

        assertEquals("1984", stats.oldestBook?.title)
        assertEquals("Neuromancer", stats.newestBook?.title)
    }

    @Test
    fun `getStatistics on empty list should return zeros and nulls`() {
        val stats = collection.getStatistics()

        assertEquals(0, stats.totalCount)
        assertEquals(0, stats.readCount)
        assertEquals(0, stats.unreadCount)
        assertNull(stats.oldestBook)
        assertNull(stats.newestBook)
    }

    @Test
    fun `getStatistics should accept custom book list`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)

        val subset = collection.findByAuthor("Frank Herbert")
        val stats = collection.getStatistics(subset)

        assertEquals(1, stats.totalCount)
        assertEquals("Dune", stats.oldestBook?.title)
        assertEquals("Dune", stats.newestBook?.title)
    }

    @Test
    fun `searchBooks with text should match title`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)

        val criteria = SearchCriteria(searchText = "Hobbit")
        val results = collection.searchBooks(criteria)

        assertEquals(1, results.size)
        assertEquals("The Hobbit", results[0].title)
    }

    @Test
    fun `searchBooks with text should match author`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("The Lord of the Rings", "J.R.R. Tolkien", 1954)
        collection.addBook("1984", "George Orwell", 1949)

        val criteria = SearchCriteria(searchText = "Tolkien")
        val results = collection.searchBooks(criteria)

        assertEquals(2, results.size)
        assertTrue(results.all { it.author == "J.R.R. Tolkien" })
    }

    @Test
    fun `searchBooks with text should be case-insensitive`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)

        val criteria = SearchCriteria(searchText = "tolkien")
        val results = collection.searchBooks(criteria)

        assertEquals(1, results.size)
    }

    @Test
    fun `searchBooks with yearFrom should filter correctly`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)

        val criteria = SearchCriteria(yearFrom = 1960)
        val results = collection.searchBooks(criteria)

        assertEquals(2, results.size)
        assertTrue(results.all { it.year >= 1960 })
    }

    @Test
    fun `searchBooks with yearTo should filter correctly`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)

        val criteria = SearchCriteria(yearTo = 1950)
        val results = collection.searchBooks(criteria)

        assertEquals(2, results.size)
        assertTrue(results.all { it.year <= 1950 })
    }

    @Test
    fun `searchBooks with year range should filter correctly`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)

        val criteria = SearchCriteria(yearFrom = 1940, yearTo = 1970)
        val results = collection.searchBooks(criteria)

        assertEquals(2, results.size)
        assertTrue(results.all { it.year in 1940..1970 })
    }

    @Test
    fun `searchBooks with readStatus true should return only read books`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.markAsRead("1984")
        collection.markAsRead("Dune")

        val criteria = SearchCriteria(readStatus = true)
        val results = collection.searchBooks(criteria)

        assertEquals(2, results.size)
        assertTrue(results.all { it.read })
    }

    @Test
    fun `searchBooks with readStatus false should return only unread books`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.markAsRead("1984")

        val criteria = SearchCriteria(readStatus = false)
        val results = collection.searchBooks(criteria)

        assertEquals(2, results.size)
        assertTrue(results.all { !it.read })
    }

    @Test
    fun `searchBooks with combined criteria should apply AND logic`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Foundation", "Isaac Asimov", 1951)
        collection.markAsRead("1984")
        collection.markAsRead("Foundation")

        val criteria = SearchCriteria(
            yearFrom = 1940,
            yearTo = 1960,
            readStatus = true
        )
        val results = collection.searchBooks(criteria)

        assertEquals(2, results.size)
        assertTrue(results.all { it.year in 1940..1960 && it.read })
    }

    @Test
    fun `searchBooks with no criteria should return all books`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)

        val criteria = SearchCriteria()
        val results = collection.searchBooks(criteria)

        assertEquals(3, results.size)
    }

    @Test
    fun `searchBooks with no matches should return empty list`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("1984", "George Orwell", 1949)

        val criteria = SearchCriteria(searchText = "NonexistentBook")
        val results = collection.searchBooks(criteria)

        assertEquals(0, results.size)
    }

    @Test
    fun `search with title query should return matching books`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("The Dune Messiah", "Frank Herbert", 1969)

        val results = collection.search(titleQuery = "dune")

        assertEquals(2, results.size)
        assertTrue(results.any { it.title == "Dune" })
        assertTrue(results.any { it.title == "The Dune Messiah" })
    }

    @Test
    fun `search with author query should return matching books`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Animal Farm", "George Orwell", 1945)
        collection.addBook("Dune", "Frank Herbert", 1965)

        val results = collection.search(authorQuery = "orwell")

        assertEquals(2, results.size)
        assertTrue(results.all { it.author == "George Orwell" })
    }

    @Test
    fun `search with year range should return books in range`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)

        val results = collection.search(yearFrom = 1950, yearTo = 1985)

        assertEquals(2, results.size)
        assertTrue(results.any { it.title == "Dune" })
        assertTrue(results.any { it.title == "Neuromancer" })
    }

    @Test
    fun `search with read status should filter correctly`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.markAsRead("Dune")

        val readBooks = collection.search(read = true)
        val unreadBooks = collection.search(read = false)

        assertEquals(1, readBooks.size)
        assertEquals("Dune", readBooks[0].title)
        assertEquals(1, unreadBooks.size)
        assertEquals("1984", unreadBooks[0].title)
    }

    @Test
    fun `search with combined criteria should apply all filters`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Dune Messiah", "Frank Herbert", 1969)
        collection.markAsRead("Dune")

        val results = collection.search(
            titleQuery = "dune",
            authorQuery = "herbert",
            yearFrom = 1960,
            yearTo = 1970,
            read = true
        )

        assertEquals(1, results.size)
        assertEquals("Dune", results[0].title)
    }

    @Test
    fun `search with no criteria should return all books`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)

        val results = collection.search()

        assertEquals(3, results.size)
    }

    @Test
    fun `addBook should throw IOException and not corrupt data file when temp write is blocked`() {
        collection.addBook("Dune", "Frank Herbert", 1965)
        val originalContent = tempFile.readText()

        // Prove that saveBooks() uses a .tmp file (atomic write pattern):
        // Place a directory at the exact path the code would use for the temp file.
        // The code must try to write data.json.tmp → creating a file where a dir exists
        // throws IOException → original data.json is never touched → content preserved.
        val tmpPath = File(tempFile.absolutePath + ".tmp")
        tmpPath.mkdir()

        try {
            assertFailsWith<IOException> {
                collection.addBook("1984", "George Orwell", 1949)
            }
            assertEquals(originalContent, tempFile.readText())
        } finally {
            tmpPath.delete()
        }
    }
}

