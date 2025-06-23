package models;

import java.time.LocalDateTime;

public class ChatMessage {
    private int id;
    private int senderId;
    private String senderUsername;
    private String senderFullName;
    private String content;
    private LocalDateTime timestamp;
    private boolean isEdited;
    private LocalDateTime editedAt;
    private String messageType; // TEXT, IMAGE, FILE
    private String attachmentPath;

    // Constructors
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.messageType = "TEXT";
        this.isEdited = false;
    }

    public ChatMessage(int senderId, String senderUsername, String content) {
        this();
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderFullName() {
        return senderFullName;
    }

    public void setSenderFullName(String senderFullName) {
        this.senderFullName = senderFullName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    @Override
    public String toString() {
        return String.format("ChatMessage{id=%d, sender='%s', content='%s', timestamp='%s'}",
                id, senderUsername, content, timestamp);
    }
}