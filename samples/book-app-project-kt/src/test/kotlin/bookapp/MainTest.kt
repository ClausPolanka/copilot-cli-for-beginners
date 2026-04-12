package bookapp

import bookapp.models.SearchCriteria
import bookapp.services.BookCollection
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.*

class MainTest {

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

    // --- parseSearchCriteria ---

    @Test
    fun `parseSearchCriteria with no args returns empty criteria`() {
        val criteria = parseSearchCriteria(arrayOf("list"))

        assertNull(criteria.searchText)
        assertNull(criteria.yearFrom)
        assertNull(criteria.yearTo)
        assertNull(criteria.readStatus)
    }

    @Test
    fun `parseSearchCriteria with --text sets searchText`() {
        val criteria = parseSearchCriteria(arrayOf("list", "--text", "Tolkien"))

        assertEquals("Tolkien", criteria.searchText)
    }

    @Test
    fun `parseSearchCriteria with --year-from sets yearFrom`() {
        val criteria = parseSearchCriteria(arrayOf("list", "--year-from", "2000"))

        assertEquals(2000, criteria.yearFrom)
    }

    @Test
    fun `parseSearchCriteria with --year-to sets yearTo`() {
        val criteria = parseSearchCriteria(arrayOf("list", "--year-to", "2020"))

        assertEquals(2020, criteria.yearTo)
    }

    @Test
    fun `parseSearchCriteria with --read sets readStatus to true`() {
        val criteria = parseSearchCriteria(arrayOf("list", "--read"))

        assertEquals(true, criteria.readStatus)
    }

    @Test
    fun `parseSearchCriteria with --unread sets readStatus to false`() {
        val criteria = parseSearchCriteria(arrayOf("list", "--unread"))

        assertEquals(false, criteria.readStatus)
    }

    @Test
    fun `parseSearchCriteria with combined args sets all fields`() {
        val criteria = parseSearchCriteria(arrayOf("list", "--text", "Dune", "--year-from", "1960", "--year-to", "1980", "--read"))

        assertEquals("Dune", criteria.searchText)
        assertEquals(1960, criteria.yearFrom)
        assertEquals(1980, criteria.yearTo)
        assertEquals(true, criteria.readStatus)
    }

    @Test
    fun `parseSearchCriteria with invalid year value sets yearFrom to null`() {
        val criteria = parseSearchCriteria(arrayOf("list", "--year-from", "abc"))

        assertNull(criteria.yearFrom)
    }

    // --- handleList ---

    private fun captureOutput(block: () -> Unit): String {
        val baos = ByteArrayOutputStream()
        val original = System.out
        System.setOut(PrintStream(baos))
        try {
            block()
        } finally {
            System.setOut(original)
        }
        return baos.toString()
    }

    private fun withInput(input: String, block: () -> Unit): String {
        val inStream = ByteArrayInputStream(input.toByteArray())
        val originalIn = System.`in`
        System.setIn(inStream)
        return try {
            captureOutput(block)
        } finally {
            System.setIn(originalIn)
        }
    }

    @Test
    fun `handleList with empty collection prints no books found`() {
        val output = captureOutput { handleList(collection, arrayOf("list")) }

        assertTrue(output.contains("No books found."))
    }

    @Test
    fun `handleList without filters shows all books`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)

        val output = captureOutput { handleList(collection, arrayOf("list")) }

        assertTrue(output.contains("1984"))
        assertTrue(output.contains("Dune"))
    }

    @Test
    fun `handleList with --text filter shows only matching books`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Dune Messiah", "Frank Herbert", 1969)

        val output = captureOutput { handleList(collection, arrayOf("list", "--text", "Dune")) }

        assertFalse(output.contains("1984"))
        assertTrue(output.contains("Dune"))
        assertTrue(output.contains("Dune Messiah"))
    }

    @Test
    fun `handleList with --year-from filter shows only books from that year onwards`() {
        collection.addBook("The Hobbit", "J.R.R. Tolkien", 1937)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.addBook("Neuromancer", "William Gibson", 1984)

        val output = captureOutput { handleList(collection, arrayOf("list", "--year-from", "1960")) }

        assertFalse(output.contains("The Hobbit"))
        assertTrue(output.contains("Dune"))
        assertTrue(output.contains("Neuromancer"))
    }

    @Test
    fun `handleList with --read filter shows only read books`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)
        collection.markAsRead("Dune")

        val output = captureOutput { handleList(collection, arrayOf("list", "--read")) }

        assertFalse(output.contains("1984"))
        assertTrue(output.contains("Dune"))
    }

    @Test
    fun `handleList with no matching results prints no books found`() {
        collection.addBook("1984", "George Orwell", 1949)
        collection.addBook("Dune", "Frank Herbert", 1965)

        val output = captureOutput { handleList(collection, arrayOf("list", "--text", "XYZ")) }

        assertTrue(output.contains("No books found."))
    }

    // --- handleAdd ---

    @Test
    fun `handleAdd with valid input adds book and confirms success`() {
        val output = withInput("The Hobbit\nJ.R.R. Tolkien\n1937\n") {
            handleAdd(collection)
        }

        assertTrue(output.contains("Book added successfully."))
        val book = collection.findBookByTitle("The Hobbit")
        assertNotNull(book)
        assertEquals("J.R.R. Tolkien", book.author)
        assertEquals(1937, book.year)
    }

    @Test
    fun `handleAdd with invalid year prints error message`() {
        val output = withInput("1984\nGeorge Orwell\nnotAYear\n") {
            handleAdd(collection)
        }

        assertTrue(output.contains("Error: 'notAYear' is not a valid year."))
        assertNull(collection.findBookByTitle("1984"))
    }

    @Test
    fun `handleAdd with empty title adds book with empty title`() {
        val output = withInput("\nSome Author\n2000\n") {
            handleAdd(collection)
        }

        assertTrue(output.contains("Book added successfully."))
        assertEquals(1, collection.allBooks.size)
        assertEquals("", collection.allBooks[0].title)
    }
}
