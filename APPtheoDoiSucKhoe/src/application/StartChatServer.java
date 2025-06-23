package application;

import server.ChatServer;

public class StartChatServer {
    public static void main(String[] args) {
        try {
            ChatServer.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}