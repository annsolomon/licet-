package com.college.portals.notify;

/** SYLLABUS: INTERFACE. One way of delivering a notification (log file, web hook, later SMS / e-mail). */
public interface Channel {

    /** A message to deliver. */
    record Message(long id, String recipientType, String channel, String destination, String title, String body) { }

    String name();

    /** Throws when delivery failed; the dispatcher then marks the notification FAILED. */
    void send(Message message) throws Exception;
}
