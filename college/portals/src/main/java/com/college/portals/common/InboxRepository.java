package com.college.portals.common;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** The notification inbox of a parent or of a class advisor (rows created by the Oracle trigger). */
@Repository
@Profile({"parent", "advisor"})
public class InboxRepository {

    private final JdbcTemplate jdbc;

    public InboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(String type, long recipientId, boolean unreadOnly, int limit) {
        return jdbc.queryForList("""
                SELECT n.notif_id "id", n.student_id "studentId", s.student_name "studentName", n.title "title",
                       n.message "message", n.channel "channel", n.destination "destination", n.category "category", n.status "status",
                       CASE WHEN n.read_flag = 'Y' THEN 1 ELSE 0 END "read",
                       TO_CHAR(n.created_at, 'YYYY-MM-DD HH24:MI') "createdAt",
                       TO_CHAR(n.sent_at, 'YYYY-MM-DD HH24:MI') "sentAt"
                  FROM notification n LEFT JOIN student s ON s.student_id = n.student_id
                 WHERE n.recipient_type = ? AND n.recipient_id = ? AND (? = 0 OR n.read_flag = 'N')
                 ORDER BY n.notif_id DESC FETCH FIRST ? ROWS ONLY
                """, type, recipientId, unreadOnly ? 1 : 0, limit);
    }

    public long unread(String type, long recipientId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE recipient_type = ? AND recipient_id = ? AND read_flag = 'N'",
                Long.class, type, recipientId);
        return n == null ? 0 : n;
    }

    public int markRead(String type, long recipientId, long notifId) {
        return jdbc.update("UPDATE notification SET read_flag = 'Y' WHERE notif_id = ? AND recipient_type = ? AND recipient_id = ?",
                notifId, type, recipientId);
    }

    public int markAllRead(String type, long recipientId) {
        return jdbc.update("UPDATE notification SET read_flag = 'Y' WHERE recipient_type = ? AND recipient_id = ? AND read_flag = 'N'",
                type, recipientId);
    }
}
