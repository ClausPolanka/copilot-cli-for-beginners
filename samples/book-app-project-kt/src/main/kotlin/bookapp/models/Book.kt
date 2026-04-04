package bookapp.models

data class Book(
    var title: String = "",
    var author: String = "",
    var year: Int = 0,
    var read: Boolean = false
)
