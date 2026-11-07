package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY isCompleted ASC, id DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE workspaceId = :workspaceId ORDER BY isCompleted ASC, id DESC")
    fun getCoursesByWorkspace(workspaceId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE isCompleted = 0 ORDER BY id DESC")
    fun getActiveCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE workspaceId = :workspaceId AND isCompleted = 0 ORDER BY id DESC")
    fun getActiveCoursesByWorkspace(workspaceId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses")
    suspend fun getAllCoursesList(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE workspaceId = :workspaceId")
    suspend fun deleteCoursesByWorkspace(workspaceId: String)

    @Query("DELETE FROM courses")
    suspend fun clearAllCourses()
}
