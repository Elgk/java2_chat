package server;

public interface AuthService {
    String getNicknameByLoginPassword(String login, String password);
    boolean registration(String login, String password, String nickname);
    boolean changeNickName(String login, String newNickName);

}
