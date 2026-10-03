package com.efm.filemanager.data.cloud

import com.efm.filemanager.data.local.CloudUploadEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudUploadRepositoryTest {
    @Test
    fun `done uploads are left out of the display list`() {
        val entries =
            listOf(
                entity(id = 1, status = CloudUploadStatus.QUEUED),
                entity(id = 2, status = CloudUploadStatus.UPLOADING),
                entity(id = 3, status = CloudUploadStatus.DONE),
                entity(id = 4, status = CloudUploadStatus.FAILED),
            )
        val shown = uploadsToDisplay(entries)
        assertEquals(listOf(1L, 2L, 4L), shown.map { it.id })
    }

    @Test
    fun `an empty list stays empty`() {
        assertEquals(emptyList<CloudUploadEntity>(), uploadsToDisplay(emptyList()))
    }

    private fun entity(
        id: Long,
        status: CloudUploadStatus,
    ): CloudUploadEntity =
        CloudUploadEntity(
            id = id,
            sourceUri = "content://source/$id",
            sourceName = "file-$id.txt",
            destinationParentUri = "content://dest",
            status = status.name,
            createdAt = id,
            updatedAt = id,
        )
}
