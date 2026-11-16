package com.mphocodes.acetimetracker.ui.viewmodel

import com.mphocodes.acetimetracker.data.model.Category
import org.junit.Assert.*
import org.junit.Test

class CategoryEditValidationTest {
    @Test fun editsCanRetainTheirOwnNameButCannotUseAnotherCategoryName() {
        val original = Category(documentId = "one", name = "Reading")
        val other = Category(documentId = "two", name = "Exercise")
        assertNull(CategoryValidation.error(original.copy(description = "Updated"), listOf(original, other)))
        assertNotNull(CategoryValidation.error(original.copy(name = "  EXERCISE  "), listOf(original, other)))
    }

    @Test fun editRejectsInvalidGoalValuesAndRanges() {
        val original = Category(documentId = "one", name = "Reading")
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f).forEach { value ->
            assertNotNull(CategoryValidation.error(original.copy(goal = value), emptyList()))
        }
        assertNotNull(CategoryValidation.error(original.copy(minGoal = 4f, maxGoal = 2f), emptyList()))
        assertNotNull(CategoryValidation.error(original.copy(goal = 6f, minGoal = 2f, maxGoal = 4f), emptyList()))
        assertNull(CategoryValidation.error(original.copy(goal = 3f, minGoal = 2f, maxGoal = 4f), emptyList()))
    }

    @Test fun malformedNonemptyGoalTextCannotSilentlyClearAnExistingGoal() {
        listOf("abc", "-", "NaN", "Infinity", "-0.5").forEach {
            assertNotNull(CategoryValidation.goalInputError(it))
        }
        assertNull(CategoryValidation.goalInputError("", "  ", "0", "1.25"))
    }

    @Test fun whitespaceNameIsRejectedDuringEdit() {
        assertNotNull(CategoryValidation.error(Category(documentId = "one", name = "  "), emptyList()))
    }
}
