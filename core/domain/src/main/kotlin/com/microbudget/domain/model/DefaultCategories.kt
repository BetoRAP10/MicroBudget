package com.microbudget.domain.model

/**
 * Categorías iniciales. Usan ids fijos para que reloj y celular las compartan
 * sin necesidad de sincronizarlas (ambos las crean idénticas al instalar).
 */
object DefaultCategories {
    const val FOOD = "food"
    const val TRANSPORT = "transport"
    const val ENTERTAINMENT = "entertainment"
    const val SHOPPING = "shopping"
    const val SERVICES = "services"
    const val OTHER = "other"

    val all: List<Category> = listOf(
        Category(FOOD, "Comida", "🍔", 0xFFF97316, isDefault = true, sortOrder = 0),
        Category(TRANSPORT, "Transporte", "🚌", 0xFF38BDF8, isDefault = true, sortOrder = 1),
        Category(ENTERTAINMENT, "Entretenimiento", "🎬", 0xFFE879F9, isDefault = true, sortOrder = 2),
        Category(SHOPPING, "Compras", "🛍", 0xFFFACC15, isDefault = true, sortOrder = 3),
        Category(SERVICES, "Servicios", "💡", 0xFF2DD4BF, isDefault = true, sortOrder = 4),
        Category(OTHER, "Otros", "📦", 0xFF94A3B8, isDefault = true, sortOrder = 5),
    )

    /** Categoría usada cuando un gasto apunta a una categoría eliminada o desconocida. */
    val fallback: Category = all.last()

    /** Iconos y colores ofrecidos al crear categorías nuevas. */
    val iconChoices = listOf("🍔", "☕", "🚌", "🚕", "⛽", "🎬", "🎮", "🛍", "👕", "💡", "📱", "🏠", "💊", "📚", "🎁", "🐶", "✈", "📦")
    val colorChoices = listOf(
        0xFFF97316, 0xFF38BDF8, 0xFFE879F9, 0xFFFACC15, 0xFF2DD4BF,
        0xFF94A3B8, 0xFFA78BFA, 0xFFF472B6, 0xFF4ADE80, 0xFFFB7185,
    )
}
