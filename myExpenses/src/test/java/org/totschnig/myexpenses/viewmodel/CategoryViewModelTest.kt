package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.MatrixCursor
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.viewmodel.data.Category2

@RunWith(RobolectricTestRunner::class)
class CategoryViewModelTest {

    @Test
    fun buildCatTree() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val cursor =
            MatrixCursor(
                arrayOf(
                    KEY_ROWID,
                    KEY_PARENTID,
                    KEY_LABEL,
                    KEY_COLOR,
                    KEY_ICON,
                    KEY_PATH,
                    KEY_MATCHES_FILTER,
                    KEY_LEVEL
                ), 8
            )

        fun MatrixCursor.addCat(
            id: Int,
            parent: Int?,
            label: String,
            path: String = if (parent == null) label else ""
        ) =
            addRow(arrayOf(id, parent, label, null, null, path, 1, path.split(" -> ").size))
        cursor.addCat(1, null, "Car")
        cursor.addCat(2, 1, "Fuel", "Car -> Fuel")
        cursor.addCat(3, null, "Care")
        cursor.addCat(4, 3, "Clothing", "Care -> Clothing")
        cursor.addCat(5, 4, "Underwear", "Care -> Clothing -> Underwear")
        cursor.addCat(6, 3, "Hairdresser", "Care -> Hairdresser")
        cursor.addCat(7, null, "Food")
        cursor.addCat(8, null, "Leisure")
        cursor.moveToFirst()
        assertThat(CategoryViewModel.ingest(context, cursor, null, null, 1)).containsExactly(
            Category2(
                id = 1,
                level = 1,
                label = "Car",
                children = listOf(
                    Category2(
                        id = 2,
                        level = 2,
                        label = "Fuel",
                        path = "Car -> Fuel",
                        parentId = 1
                    )
                )
            ),
            Category2(
                id = 3,
                level = 1,
                label = "Care",
                children = listOf(
                    Category2(
                        id = 4,
                        level = 2,
                        parentId = 3,
                        label = "Clothing",
                        path = "Care -> Clothing",
                        children = listOf(
                            Category2(
                                id = 5,
                                level = 3,
                                parentId = 4,
                                label = "Underwear",
                                path = "Care -> Clothing -> Underwear"
                            )
                        )
                    ),
                    Category2(
                        id = 6,
                        level = 2,
                        parentId = 3,
                        label = "Hairdresser",
                        path = "Care -> Hairdresser"
                    )
                )
            ),
            Category2(
                id = 7,
                level = 1,
                label = "Food"
            ),
            Category2(
                id = 8,
                level = 1,
                label = "Leisure"
            ),
        ).inOrder()
    }
}