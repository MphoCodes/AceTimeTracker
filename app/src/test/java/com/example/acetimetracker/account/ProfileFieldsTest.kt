package com.example.acetimetracker.account

import org.junit.Assert.*
import org.junit.Test

class ProfileFieldsTest {
    private val account = Account("user-a", "login@example.com", "Auth name", "auth-photo")

    @Test fun createsProfileFromAuthenticatedIdentity() {
        assertEquals(mapOf("displayName" to "Chosen name", "email" to "login@example.com", "profilePictureUrl" to "auth-photo"),
            ProfileFields.missing(emptyMap(), account, " Chosen name "))
    }

    @Test fun preservesExistingProfileAndHistory() {
        val existing = mapOf<String, Any?>("displayName" to "Existing", "email" to "legacy@example.com",
            "profilePictureUrl" to "old-photo", "dailyTimesheetEntries" to listOf(mapOf("totalHours" to 2.5)),
            "minGoal" to 4.0, "custom" to "keep")
        assertTrue(ProfileFields.missing(existing, account, "New name").isEmpty())
        assertEquals("legacy@example.com", existing["email"])
    }

    @Test fun fillsOnlyAbsentFieldsAndIsIdempotent() {
        val existing = mapOf<String, Any?>("displayName" to "", "profilePictureUrl" to null, "dailyTimesheetEntries" to listOf(1))
        val patch = ProfileFields.missing(existing, account)
        assertEquals(mapOf("email" to "login@example.com"), patch)
        assertTrue(ProfileFields.missing(existing + patch, account).isEmpty())
    }

    @Test fun textOnlyEditDoesNotRewriteEmailPhotoOrHistory() {
        assertEquals(mapOf("displayName" to "Edited"), ProfileFields.edit(" Edited ", null))
    }

    @Test fun photoChangesAreExplicit() {
        assertEquals(mapOf("displayName" to "Edited", "profilePictureUrl" to "new-photo"), ProfileFields.edit("Edited", "new-photo"))
    }
}
