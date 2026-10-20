package com.microbudget.domain.sync

enum class MergeDecision {
    /** No existe localmente o la versión remota es más nueva: se guarda la remota. */
    TAKE_REMOTE,

    /** La versión local es más nueva: se conserva y se reenvía al otro dispositivo. */
    KEEP_LOCAL,

    /** Ambas versiones son idénticas: solo se marca como sincronizada. */
    SAME,
}

/**
 * Resolución de conflictos "Last-Writer-Wins" (gana la última modificación):
 *
 * 1. Si el registro no existe localmente -> se acepta el remoto.
 * 2. Si son idénticos -> SAME.
 * 3. Gana el `updatedAt` mayor (la edición más reciente, venga del reloj o del celular).
 * 4. Empate exacto de `updatedAt` con contenido distinto:
 *    a) gana el borrado (más seguro: no "resucitar" un gasto que alguien eliminó);
 *    b) si no, desempate determinista comparando el contenido serializado, de modo que
 *       ambos dispositivos elijan SIEMPRE el mismo ganador y converjan.
 *
 * Nota: depende de que los relojes de los dispositivos estén razonablemente sincronizados
 * (en emuladores toman la hora de la PC).
 */
object ConflictResolver {

    fun <T : SyncRecord> decide(local: T?, remote: T): MergeDecision {
        if (local == null) return MergeDecision.TAKE_REMOTE
        if (local == remote) return MergeDecision.SAME
        return when {
            remote.updatedAt > local.updatedAt -> MergeDecision.TAKE_REMOTE
            remote.updatedAt < local.updatedAt -> MergeDecision.KEEP_LOCAL
            remote.deleted != local.deleted -> if (remote.deleted) MergeDecision.TAKE_REMOTE else MergeDecision.KEEP_LOCAL
            remote.toString() > local.toString() -> MergeDecision.TAKE_REMOTE
            else -> MergeDecision.KEEP_LOCAL
        }
    }
}
