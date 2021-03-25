package server;

import commands.Commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
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
                            logger.info(" Client disconnected");
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
                                    logger.info("Client: " + socket.getRemoteSocketAddress() + " connected with nick " + nickname);
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
                        if (str.startsWith(Commands.CHG)){
                            String[] token = str.split("\\s");
                            boolean updSuccess = server.getAuthService().changeNickName(login, token[1]);
                            if (updSuccess){
                                nickname = token[1];
                                sendMsg(Commands.CHG_OK +" "+ nickname);
                                server.broadcastClientList();
                                // server.broadcastMsg(this, str);
                            }else {
                                sendMsg(Commands.CHG_NO);
                            }
                        }
                    }
                }catch (SocketTimeoutException e){
                    sendMsg(Commands.END);
                    logger.log(Level.ALL,"timeout", e);
                   // System.out.println(e.getMessage());
                }catch (RuntimeException e){
                    logger.log(Level.ALL,"runtime exception", e);
                  //  System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    logger.info("Client disconnected: " + nickname);
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
