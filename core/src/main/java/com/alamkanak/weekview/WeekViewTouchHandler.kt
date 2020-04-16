package com.alamkanak.weekview

import android.graphics.RectF
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

internal class WeekViewTouchHandler<T : Any>(
    private val viewState: WeekViewViewState<T>,
    private val chipCache: EventChipCache<T>
) {

    var onEventClickListener: OnEventClickListener<T>? = null
    var onEventLongClickListener: OnEventLongClickListener<T>? = null

    var onEmptyViewClickListener: OnEmptyViewClickListener? = null
    var onEmptyViewLongClickListener: OnEmptyViewLongClickListener? = null

    fun handleLongClick(x: Float, y: Float) {
        val handled = onEventLongClickListener?.handleLongClick(x, y) ?: false
        if (!handled) {
            onEmptyViewLongClickListener?.handleLongClick(x, y)
        }
    }

    fun handleClick(x: Float, y: Float) {
        val handled = onEventClickListener?.handleClick(x, y) ?: false
        if (!handled) {
            onEmptyViewClickListener?.handleClick(x, y)
        }
    }

    /**
     * Returns the date and time that the user clicked on.
     *
     * @param x The x coordinate of the touch event.
     * @param y The y coordinate of the touch event.
     * @return The [Calendar] of the clicked position, or null if none was found.
     */
    fun calculateTimeFromPoint(x: Float, y: Float): Calendar? {
        val widthPerDay = viewState.drawableWidthPerDay
        val totalDayWidth = widthPerDay + viewState.columnGap
        val originX = viewState.currentOrigin.x
        val timeColumnWidth = viewState.timeColumnWidth

        val daysFromOrigin = (ceil((originX / totalDayWidth).toDouble()) * -1).toInt()
        var startPixel = originX + daysFromOrigin * totalDayWidth + timeColumnWidth

        val firstDay = daysFromOrigin + 1
        val lastDay = firstDay + viewState.numberOfVisibleDays

        for (dayNumber in firstDay..lastDay) {
            val start = max(startPixel, timeColumnWidth)
            val end = startPixel + totalDayWidth
            val width = end - start

            val isVisibleHorizontally = width > 0
            val isWithinDay = x.roundToInt() in start..end

            if (isVisibleHorizontally && isWithinDay) {
                val day = now() + Days(dayNumber - 1)

                val hourHeight = viewState.hourHeight
                val pixelsFromMidnight = y - viewState.currentOrigin.y - viewState.headerBounds.height
                val hour = (pixelsFromMidnight / hourHeight).toInt()

                val pixelsFromFullHour = pixelsFromMidnight - hour * hourHeight
                val minutes = ((pixelsFromFullHour / hourHeight) * 60).toInt()

                return day.withTime(viewState.minHour + hour, minutes)
            }

            startPixel += totalDayWidth
        }

        return null
    }

    private fun findHitEvent(x: Float, y: Float): EventChip<T>? {
        val candidates = chipCache.allEventChips.filter { it.isHit(x, y) }
        return when {
            candidates.isEmpty() -> null
            // Two events hit. This is most likely because an all-day event was clicked, but a
            // single event is rendered underneath it. We return the all-day event.
            candidates.size == 2 -> candidates.first { it.event.isAllDay }
            else -> candidates.first()
        }
    }

    private fun OnEventClickListener<T>.handleClick(x: Float, y: Float): Boolean {
        val eventChip = findHitEvent(x, y) ?: return false
        val isInHeader = y.roundToInt() in viewState.x..viewState.headerBounds.bottom

        if (eventChip.event.isNotAllDay && isInHeader) {
            // The user tapped in the header area and a single event that is rendered below it
            // has recognized the tap. We ignore this.
            return false
        }

        val data = checkNotNull(eventChip.originalEvent.data) {
            "Did you pass the original object into the constructor of WeekViewEvent?"
        }

        val rect = checkNotNull(eventChip.bounds)
        onEventClick(data, RectF(rect))
        return true
    }

    private fun OnEmptyViewClickListener.handleClick(x: Float, y: Float) {
        // If the tap was on in an empty space, then trigger the callback.
        if (viewState.calendarAreaBounds.contains(x.roundToInt(), y.roundToInt())) {
            calculateTimeFromPoint(x, y)?.let { time ->
                onEmptyViewClicked(time)
            }
        }
    }

    private fun OnEventLongClickListener<T>.handleLongClick(x: Float, y: Float): Boolean {
        val isInHeader = y.roundToInt() in viewState.x..viewState.headerBounds.bottom
        val eventChip = findHitEvent(x, y) ?: return false

        if (eventChip.event.isNotAllDay && isInHeader) {
            // The user tapped in the header area and a single event that is rendered below it
            // has recognized the tap. We ignore this.
            return false
        }

        val data = eventChip.originalEvent.data ?: throw NullPointerException(
            "Did you pass the original object into the constructor of WeekViewEvent?")

        val rect = checkNotNull(eventChip.bounds)
        onEventLongClick(data, RectF(rect))

        return true
    }

    private fun OnEmptyViewLongClickListener.handleLongClick(x: Float, y: Float) {
        val isInCalendarArea = viewState.calendarAreaBounds.contains(x.roundToInt(), y.roundToInt())
        if (isInCalendarArea) {
            calculateTimeFromPoint(x, y)?.let { time ->
                onEmptyViewLongClick(time)
            }
        }
    }
}
