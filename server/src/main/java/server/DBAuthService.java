package server;

public class DBAuthService implements AuthService{
    @Override
    public String getNicknameByLoginPassword(String login, String password) {
        return SQLHandler.getNicknameByLoginPassword(login, password);
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        return SQLHandler.registration(login, password, nickname);
    }

    @Override
    public boolean changeNickName(String login, String newNickName) {
        return SQLHandler.changeNickName(login, newNickName);
    }
}
