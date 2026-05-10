package com.example.studentinfoapp;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Kiểm tra insert / select ({@link LegacyTaskDao#getAllTasks}) / update / delete.
 * Sau khi chạy app thật, mở App Inspection → Database Inspector để xem bảng {@code tasks}
 * trong {@link TaskDbHelper#DATABASE_NAME}.
 */
@RunWith(AndroidJUnit4.class)
public class TaskDaoInstrumentedTest {

    private LegacyTaskDao dao;

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ctx.deleteDatabase(TaskDbHelper.DATABASE_NAME);
        dao = new LegacyTaskDao(new TaskDbHelper(ctx));
    }

    @Test
    public void insertThenGetAll_returnsSameTask() {
        Task t = new Task("Lab 1", "Mô tả", "Homework", "2026-05-10", false, "High");
        assertNotEquals(-1, dao.insertTask(t));

        List<Task> all = dao.getAllTasks();
        assertEquals(1, all.size());
        assertEquals(t.getId(), all.get(0).getId());
        assertEquals("Lab 1", all.get(0).getTitle());
        assertEquals("Mô tả", all.get(0).getDescription());
        assertEquals("Homework", all.get(0).getCategory());
        assertFalse(all.get(0).isCompleted());
        assertEquals("High", all.get(0).getPriority());
    }

    @Test
    public void updateTask_changesFields() {
        Task t = new Task("Old", "d", "c", "2026-01-01", false, "Low");
        dao.insertTask(t);

        Task updated = new Task(t.getId(), "New title", "New desc", "Exam", "2026-12-31", true, "Medium");
        updated.setImagePath("/path/img.jpg");
        assertEquals(1, dao.updateTask(updated));

        List<Task> all = dao.getAllTasks();
        assertEquals(1, all.size());
        assertEquals("New title", all.get(0).getTitle());
        assertTrue(all.get(0).isCompleted());
        assertEquals("/path/img.jpg", all.get(0).getImagePath());
    }

    @Test
    public void deleteTask_removesRow() {
        Task a = new Task("A", "", "All", "2026-05-01", false, "Low");
        Task b = new Task("B", "", "All", "2026-05-02", false, "Low");
        dao.insertTask(a);
        dao.insertTask(b);
        assertEquals(2, dao.getAllTasks().size());

        assertEquals(1, dao.deleteTask(a.getId()));
        List<Task> rest = dao.getAllTasks();
        assertEquals(1, rest.size());
        assertEquals(b.getId(), rest.get(0).getId());
    }

    @Test
    public void getAllTasks_ordersByDeadline() {
        Task later = new Task("Later", "", "All", "2026-06-01", false, "Low");
        Task earlier = new Task("Earlier", "", "All", "2026-05-01", false, "Low");
        dao.insertTask(later);
        dao.insertTask(earlier);

        List<Task> all = dao.getAllTasks();
        assertEquals("Earlier", all.get(0).getTitle());
        assertEquals("Later", all.get(1).getTitle());
    }
}
