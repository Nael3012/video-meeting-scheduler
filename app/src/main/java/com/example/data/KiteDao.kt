package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KiteDao {
    @Query("SELECT * FROM meetings ORDER BY meetingDateMillis ASC, createdAt DESC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>

    @Query("SELECT COUNT(*) FROM meetings")
    suspend fun getMeetingsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    @Query("SELECT * FROM meetings WHERE id = :id LIMIT 1")
    suspend fun getMeetingById(id: Long): MeetingEntity?

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)

    @Query("SELECT * FROM reminders WHERE isDismissed = 0 ORDER BY createdAt DESC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissReminder(id: Long)

    @Query("DELETE FROM reminders WHERE meetingId = :meetingId")
    suspend fun deleteRemindersForMeeting(meetingId: Long)

    @Query("SELECT * FROM user_accounts WHERE id = 'user_default' LIMIT 1")
    fun getUserAccount(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_accounts WHERE id = 'user_default' LIMIT 1")
    suspend fun getUserAccountOnce(): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserAccount(account: UserAccountEntity)

    @Update
    suspend fun updateUserAccount(account: UserAccountEntity)
}
