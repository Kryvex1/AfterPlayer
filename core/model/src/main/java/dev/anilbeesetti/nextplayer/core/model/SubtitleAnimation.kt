package dev.anilbeesetti.nextplayer.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SubtitleAnimation {
    NONE,
    FADE,
    TYPEWRITER,
    SLIDE_UP,
    POP_IN,
    WORD_BY_WORD,
    SLIDE_HORIZONTAL,
    BOUNCE_DROP,
}
