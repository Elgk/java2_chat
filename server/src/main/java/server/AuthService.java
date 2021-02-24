package server;

public interface AuthService {
    String getNicknameByLoginPassword(String login, String password);
}
