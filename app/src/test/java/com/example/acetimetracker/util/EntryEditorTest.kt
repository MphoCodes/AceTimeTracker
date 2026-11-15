package com.example.acetimetracker.util

import com.example.acetimetracker.data.model.TimesheetEntry
import org.junit.Assert.*
import org.junit.Test

class EntryEditorTest {
    private val original = TimesheetEntry("stable", "owner", "Work", "2026-11-15", "09:00", "2026-11-15", "10:00", 1200000, "photo", "old")
    @Test fun notesEditPreservesIdentityPhotoAndActiveDuration() {
        val result = EntryEditor.build(original, "owner", "Work", "2026-11-15", "09:00", "2026-11-15", "10:00", "new", "photo")
        assertEquals("stable", result.id)
        assertEquals(1200000L, result.timeSpent)
        assertEquals("photo", result.imageUrl)
        assertEquals("new", result.description)
    }
    @Test fun changingRangeRecalculatesDuration() {
        assertEquals(7200000L, EntryEditor.build(original, "owner", "Work", "2026-11-15", "09:00", "2026-11-15", "11:00", "", "photo").timeSpent)
    }
    @Test(expected = IllegalArgumentException::class) fun cannotEditAnotherAccount() {
        EntryEditor.build(original, "other", "Work", "2026-11-15", "09:00", "2026-11-15", "10:00", "", "")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsReversedRange() {
        EntryEditor.build(null, "owner", "Work", "2026-11-15", "11:00", "2026-11-15", "10:00", "", "")
    }
    @Test fun notesOnSubMinuteTimerRemainEditable() {
        val short = original.copy(endTime="09:00", timeSpent=15000)
        assertEquals(15000L, EntryEditor.build(short,"owner","Work","2026-11-15","09:00","2026-11-15","09:00","new", "photo").timeSpent)
    }
}
