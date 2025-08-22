package com.bikecare.bikecomponent.history

enum class EventType {
    INSTALLED,     // nainstalováno
    UPDATED,       // změna/úprava metadat
    REMOVED,       // odstraněno z kola
    RESTORED,      // vráceno zpět (undo remove)
    REPLACED,      // nahrazeno jinou komponentou
    DELETED,       // soft delete (pokud používáš)
    HARD_DELETED   // hard delete komponenty
}
