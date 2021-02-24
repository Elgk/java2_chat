package server;

import commands.Commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private  Server server;
    private  Socket socket;
    private  DataInputStream in;
    private  DataOutputStream out;
    private String nickname;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream((socket.getOutputStream()));

            new Thread(() -> {
                try {
                    // цикл аутетификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith(Commands.END)) {
                            out.writeUTF(Commands.END);
                            throw new RuntimeException(" Client disconnected");
                        }                             // "\\s" - пробел
                        if (str.startsWith(Commands.AUTH)) {
                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService().getNicknameByLoginPassword(token[1], token[2]);
                            if (newNick != null) {
                                nickname = newNick;
                                sendMsg(Commands.AUTH_OK + " " + nickname);
                                server.subscribe(this);
                                System.out.println("Client: " + socket.getRemoteSocketAddress() + " connected with nick " + nickname);
                                break;
                            } else {
                                sendMsg("Wrong login or password!");
                            }
                        }

                    }
                    // цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Commands.END)) {
                                out.writeUTF(Commands.END);
                                break;
                            }
                            if (str.startsWith(Commands.PERSONAL_MSG)){
                                String[] token = str.split("\\s",3);
                                if (token.length < 3){
                                    continue;
                                }else {
                                    server.personalMsg(this, token[1], token[2]);
                                }
                             }
                        }else {
                            server.broadcastMsg(this, str);
                        }
                    }
                }catch (RuntimeException e){
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Client disconnected: " + nickname);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg){
        try {
            out.writeUTF( msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }
}
