package com.alamkanak.weekview

internal class EventChipsLoader<T>(
    viewState: WeekViewViewState<T>,
    private val chipCache: EventChipCache<T>
) {

    private val eventSplitter = WeekViewEventSplitter<T>(viewState)

    fun createAndCacheEventChips(events: List<WeekViewEvent<T>>) {
        chipCache += convertEventsToEventChips(events)
    }

    private fun convertEventsToEventChips(
        events: List<WeekViewEvent<T>>
    ): List<EventChip<T>> = events.sorted().map(this::convertEventToEventChips).flatten()

    private fun convertEventToEventChips(
        event: WeekViewEvent<T>
    ): List<EventChip<T>> = eventSplitter.split(event).map { EventChip(it, event) }
}
