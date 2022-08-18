operator fun <E : Enum<E>> List<E>.get(name: String): E? {
    return this.firstOrNull { it.name.equals(name, true) }
}