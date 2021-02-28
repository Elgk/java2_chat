package server;

import commands.Commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private  Server server;
    private  Socket socket;
    private  DataInputStream in;
    private  DataOutputStream out;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream((socket.getOutputStream()));

            new Thread(() -> {
                try {
                    // set timeout
                    socket.setSoTimeout(120000);

                    // цикл аутетификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith(Commands.END)) {
                            out.writeUTF(Commands.END);
                            throw new RuntimeException(" Client disconnected");
                        }
                        if (str.startsWith(Commands.AUTH)) {
                            String[] token = str.split("\\s");   // "\\s" - пробел (whitespace)
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService().getNicknameByLoginPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthrnticated(token[1])) {
                                    socket.setSoTimeout(0);
                                    nickname = newNick;
                                    sendMsg(Commands.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    System.out.println("Client: " + socket.getRemoteSocketAddress() + " connected with nick " + nickname);
                                    break;
                                } else {
                                    sendMsg("User is alredy login");
                                }
                            } else {
                                sendMsg("Entered login or password is not correct");
                            }
                        }
                        if (str.startsWith(Commands.REG)) {
                            String[] token = str.split("\\s", 4);   // "\\s" - пробел (whitespace)
                            if (token.length < 4) {
                                continue;
                            }
                            boolean regSuccess = server.getAuthService().registration(token[1], token[2], token[3]);
                            if (regSuccess) {
                                sendMsg(Commands.REG_OK);
                            } else {
                                sendMsg(Commands.REG_NO);
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
                            if (str.startsWith(Commands.PERSONAL_MSG)) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                } else {
                                    server.personalMsg(this, token[1], token[2]);
                                }
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                }catch (SocketTimeoutException e){
                    sendMsg(Commands.END);
                 //   e.printStackTrace();
                    System.out.println(e.getMessage());
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

    public String getLogin() {
        return login;
    }

    public String getNickname() {
        return nickname;
    }
}
