package client;

import commands.Commands;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea textArea;
    @FXML
    public TextField textField;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView<String> clintList;
    @FXML
    public MenuBar menuBar;


    private final String IP_ADDR = "localhost";
    private final int PORT = 8189;    
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickName;
    private Stage stage;
    private Stage regStage;
    private RegController regController;


    public void setAuthenticated(boolean authenticated){
        this.authenticated = authenticated;
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        menuBar.setVisible(authenticated);
        menuBar.setManaged(authenticated);
        clintList.setVisible(authenticated);
        clintList.setManaged(authenticated);
        if (!authenticated){
            nickName = "";
            loginField.clear();
        }
        textArea.clear();
        setTitle(nickName);
    }

    public String getNickName() {
        return nickName;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(()->{
            stage = (Stage)textArea.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (socket != null && !socket.isClosed()){
                    try {
                        out.writeUTF(Commands.END);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
    }
    private void connect(){
        try {
            socket = new Socket(IP_ADDR, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(()->{
                try {
                    // цикл аутетификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.startsWith(Commands.END)) {
                                throw new RuntimeException("Server closed your connection!");
                            }
                            if (str.startsWith(Commands.AUTH)) {
                                String[] token = str.split("\\s");
                                nickName = token[1];
                                setAuthenticated(true);
                                break;
                            }
                            if (str.startsWith(Commands.REG_OK)){
                                regController.tryRegResult(Commands.REG_OK);
                            }
                            if (str.startsWith(Commands.REG_NO)){
                                regController.tryRegResult(Commands.REG_NO);
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                    // цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")){
                            if (str.equals(Commands.END)) {
                                System.out.println("Your connection is closed");
                                break;
                            }
                            if (str.startsWith(Commands.CHG_OK)){
                                regController.tryRegResult(Commands.CHG_OK);
                                String[] token = str.split("\\s");
                                nickName = token[1];
                                setTitle(nickName);
                            }
                            if (str.startsWith(Commands.CHG_NO)){
                                regController.tryRegResult(Commands.CHG_NO);
                            }
                        if (str.startsWith(Commands.CLIENT_LIST)){
                            String[] token = str.split("\\s");
                            Platform.runLater(()->{
                                clintList.getItems().clear();
                                for (int i = 1; i < token.length; i++) {
                                    clintList.getItems().add(token[i]);
                                }
                            });
                        }
                        }else {
                            textArea.appendText(str + "\n");
                        }
                    }
                }catch (RuntimeException e){
                    System.out.println(e.getMessage());
                }catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    setAuthenticated(false);
                    try {
                        socket.close();
                        textArea.appendText("Your connection is closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(ActionEvent actionEvent) {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void tryToAuth(ActionEvent actionEvent) {
        authMethod();
    /*    if (socket == null || socket.isClosed()){
            connect();
        }
        try {
            out.writeUTF(String.format("%s: %s %s", Commands.AUTH, loginField.getText().trim(), passwordField.getText().trim() ));

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            passwordField.clear();
        }*/

    }

    public void setTitle(String nickname){
        Platform.runLater(()->{
            if (nickname.equals("")){
                stage.setTitle("Best Chat of World");
            }else
                stage.setTitle(String.format("Best Chat of World [%s]",nickname));
        });
    }

    public void clientListOnMouseReleased(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2){
            String msg = String.format("%s, %s ", Commands.PERSONAL_MSG, clintList.getSelectionModel().getSelectedItem());
            textField.setText(msg);
        }
    }

    public void tryToAuthByKey(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            authMethod();
        }
    }
    private void authMethod(){
            if (socket == null || socket.isClosed()){
                connect();
            }
            try {
                out.writeUTF(String.format("%s: %s %s", Commands.AUTH, loginField.getText().trim(), passwordField.getText().trim() ));

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                passwordField.clear();
            }
    }

    public void showRegWindow(ActionEvent actionEvent) {
        if (regStage == null){
            initRegWindow();
        }
        regStage.setTitle("Chat registration");
        regController.setRegMode(true);
        regStage.show();
    }
    public void changeNick(ActionEvent actionEvent) {
        if (regStage == null){
            initRegWindow();
        }
        regStage.setTitle("Chat nickname changing");
        regController.setRegMode(false);
        regStage.show();
    }

    private void initRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/regManager.fxml"));
            Parent root = fxmlLoader.load();
            regController = fxmlLoader.getController();
            regController.setController(this); /*// Controller установил ссылку на себя в контроллере RegController*/

            regStage = new Stage();
            regStage.setScene(new Scene(root, 450, 340));
            regStage.initStyle(StageStyle.UTILITY);
            regStage.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public void  registration(String login, String password, String nickname){
        if (socket == null || socket.isClosed()){
            connect();
        }
        try {
            out.writeUTF(String.format("%s %s %s %s", Commands.REG, login, password, nickname));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToChange(String nickName){
        try {
            out.writeUTF(String.format("%s %s", Commands.CHG,  nickName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
