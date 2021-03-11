package server;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;

    private static PreparedStatement psInsert;
    private static PreparedStatement resultSet;
    private static PreparedStatement psUpdate;


    public static boolean connect()  {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
            prepareAllStatements();
            return  true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void disconnect() throws SQLException {
        resultSet.close();
        psInsert.close();
        psUpdate.close();
        connection.close();
    }

    private static void prepareAllStatements(){
        try {
            resultSet = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? and password = ?;");
            psUpdate = connection.prepareStatement("UPDATE users SET nickname = ? WHERE login = ?;");
            psInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?);");

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    public static String getNicknameByLoginPassword(String login, String password) {
        try {
         //   PreparedStatement resultSet = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? and password = ?;");
            resultSet.setString(1, login);
            resultSet.setString(2, password);
            ResultSet rs = resultSet.executeQuery();
            if (rs.next()){
                return rs.getString(1);
            }else {
                return null;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }
    public static boolean changeNickName(String login, String newNickName){
        try {
          //  PreparedStatement psUpdate = connection.prepareStatement("UPDATE users SET nickname = ? WHERE login = ?;");
            psUpdate.setString(1,newNickName);
            psUpdate.setString(2,login);
            if (psUpdate.executeUpdate() == 1){
                return true;
            }else {return false;}

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
    }
    public static boolean registration(String login, String password, String nickname) {
        try {
           // PreparedStatement psInsert = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?);");
            psInsert.setString(1, login);
            psInsert.setString(2, password);
            psInsert.setString(3, nickname);
            if (psInsert.executeUpdate() == 1){
                return true;
            }else {
                return  false;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
    }
}
