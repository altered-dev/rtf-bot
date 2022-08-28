import model.User
import org.jetbrains.exposed.sql.transactions.transaction

operator fun <E : Enum<E>> List<E>.get(name: String) =
    find { it.name.equals(name, true) }

fun getUser(id: ULong) = transaction { User.findById(id) ?: User.new(id) {} }