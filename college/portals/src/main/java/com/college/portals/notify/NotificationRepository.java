package com.college.portals.notify;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** SQL of the notification service. Rows are created by the Oracle trigger; this service only delivers them. */
@Repository
@Profile("notify")
public class NotificationRepository {

    private final JdbcTemplate jdbc;

    public NotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Channel.Message> pending(int limit) {
        return jdbc.query("""
                SELECT notif_id, recipient_type, channel, destination, title, message
                  FROM notification WHERE status = 'PENDING' ORDER BY notif_id FETCH FIRST ? ROWS ONLY
                """, (rs, i) -> new Channel.Message(rs.getLong("notif_id"), rs.getString("recipient_type"), rs.getString("channel"),
                rs.getString("destination"), rs.getString("title"), rs.getString("message")), limit);
    }

    public void markSent(long id) {
        jdbc.update("UPDATE notification SET status = 'SENT', sent_at = SYSTIMESTAMP, error_text = NULL WHERE notif_id = ?", id);
    }

    public void markFailed(long id, String error) {
        String e = error == null ? "error" : error.length() > 190 ? error.substring(0, 190) : error;
        jdbc.update("UPDATE notification SET status = 'FAILED', error_text = ? WHERE notif_id = ?", e, id);
    }

    public int retryFailed() {
        return jdbc.update("UPDATE notification SET status = 'PENDING', error_text = NULL WHERE status = 'FAILED'");
    }

    public List<Map<String, Object>> stats() {
        return jdbc.queryForList("""
                SELECT recipient_type "recipient", status "status", COUNT(*) "count"
                  FROM notification GROUP BY recipient_type, status ORDER BY recipient_type, status
                """);
    }

    public List<Map<String, Object>> recent(String status, int limit) {
        return jdbc.queryForList("""
                SELECT n.notif_id "id", n.recipient_type "recipient", n.destination "destination", n.channel "channel",
                       n.status "status", n.title "title", n.message "message", s.student_name "studentName",
                       TO_CHAR(n.created_at, 'YYYY-MM-DD HH24:MI:SS') "createdAt", TO_CHAR(n.sent_at, 'YYYY-MM-DD HH24:MI:SS') "sentAt",
                       n.error_text "error"
                  FROM notification n LEFT JOIN student s ON s.student_id = n.student_id
                 WHERE (? = 'ALL' OR n.status = ?)
                 ORDER BY n.notif_id DESC FETCH FIRST ? ROWS ONLY
                """, status, status, limit);
    }
}
