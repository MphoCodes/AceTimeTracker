package com.mphocodes.acetimetracker.data.repository

import android.app.Application
import android.content.Context
import com.mphocodes.acetimetracker.data.model.Category
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class FavoriteCategoriesTest {
    private lateinit var context: Context
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("favorite_categories", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun favoritesPersistAcrossStoreInstancesAndRemainAccountScoped() {
        FavoriteCategories(context).toggle("account-a", "category-1")
        val restored = FavoriteCategories(context)
        assertEquals(setOf("category-1"), restored.getIds("account-a"))
        assertTrue(restored.getIds("account-b").isEmpty())
        restored.toggle("account-b", "category-2")
        assertEquals(setOf("category-1"), restored.getIds("account-a"))
    }

    @Test fun sortingMovesFavoritesFirstAndRenamePreservesFavorite() {
        val store = FavoriteCategories(context)
        store.toggle("account", "second")
        val categories = listOf(Category("first", "Alpha"), Category("second", "Renamed"), Category("third", "Zulu"))
        assertEquals(listOf("second", "first", "third"), store.sort("account", categories).map { it.documentId })
        assertEquals(categories, store.sort("different-account", categories))
    }

    @Test fun togglingAgainRemovesFavoriteWithoutMutatingReturnedSnapshot() {
        val store = FavoriteCategories(context)
        val snapshot = store.toggle("account", "category")
        assertTrue(store.toggle("account", "category").isEmpty())
        assertEquals(setOf("category"), snapshot)
    }

    @Test fun missingAccountOrUnsavedCategoryCannotBeFavorited() {
        val store = FavoriteCategories(context)
        assertTrue(store.toggle("", "category").isEmpty())
        assertTrue(store.toggle("account", "").isEmpty())
        assertTrue(store.getIds("account").isEmpty())
    }
}
