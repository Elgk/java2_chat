package server;

import commands.Commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int PORT = 8189;
    private ServerSocket server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        clients = new CopyOnWriteArrayList<>();// потокобезопасный класс
        if (!SQLHandler.connect()){
            throw new RuntimeException("Attempt to connect to DB is failed");
        }
        //authService = new SimpleAuthService();
        authService = new DBAuthService();

        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");
            while (true){
                socket = server.accept();
                System.out.println("Client connected");
                System.out.println("Client: "+ socket.getRemoteSocketAddress());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                SQLHandler.disconnect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public void broadcastMsg(ClientHandler sender, String msg){
        String message = String.format("[%s]: %s", sender.getNickname(), msg);
        for (ClientHandler client : clients) {
            client.sendMsg(message);

        }
    }

    public void personalMsg(ClientHandler sender, String recipient, String msg){
        String message = String.format("From[%s] to[%s]: %s", sender.getNickname(), recipient, msg);
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(recipient)){
                client.sendMsg(message);  //  это получатель
                if (!client.equals(sender)){
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg(String.format("User %s not found", recipient));
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientList();
    }
    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthrnticated(String login){
        for (ClientHandler c : clients ) {
            if (c.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }
    public void broadcastClientList(){
        StringBuilder sb = new StringBuilder(Commands.CLIENT_LIST);
        for (ClientHandler c: clients) {
            sb.append(" ").append(c.getNickname());
        }
        String msg = sb.toString();
        for (ClientHandler c: clients) {
            c.sendMsg(msg);
        }
    }
}
