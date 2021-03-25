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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private final int PORT = 8189;
    private ServerSocket server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        clients = new CopyOnWriteArrayList<>();
        if (!SQLHandler.connect()){
            RuntimeException e = new RuntimeException("Attempt to connect to DB is failed");
            logger.log(Level.SEVERE,"DB connection",e);
            throw e; //new RuntimeException("Attempt to connect to DB is failed");
        }

        authService = new DBAuthService();
        try {
            server = new ServerSocket(PORT);
            logger.info("Server started");
            while (true){
                socket = server.accept();
                logger.info("Client connected");
                logger.info("Client: "+ socket.getRemoteSocketAddress());
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
