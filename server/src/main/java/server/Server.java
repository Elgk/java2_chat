package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class Server {
    private final int PORT = 8190;
    private ServerSocket server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        clients = new CopyOnWriteArrayList<>();// потокобезопасный класс
        authService = new SimpleAuthService();

        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");
           // System.out.println("Server connected");


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
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public void broadcastMsg(ClientHandler sender, String msg){
        String message = String.format("[%s] %s", sender.getNickname(), msg);
        for (ClientHandler client : clients) {
            client.sendMsg(message);

        }
    }

    public void personalMsg(ClientHandler sender, String recipient, String msg){
        String message = String.format("From[%s] to[%s] %s", sender.getNickname(), recipient, msg);
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(recipient)){
                client.sendMsg(message);  //  это получатель (recipient)
                if (!client.equals(sender)){
                    sender.sendMsg(message); // продублировать отправителю (sender)
                }
                return;
            }
        }
        sender.sendMsg(String.format("User %s not found", recipient));
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
    }
    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
    }

    public AuthService getAuthService() {
        return authService;
    }
}
