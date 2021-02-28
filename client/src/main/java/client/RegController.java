package client;

import commands.Commands;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class RegController {
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField nickNameField;
    @FXML
    private TextArea textArea;
    private  Controller controller;


    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void tryRegResult(String command){
        textArea.clear();
        if (command.equals(Commands.REG_OK)){
            textArea.appendText("Registration is successfull\n");
        }
        if (command.equals(Commands.REG_NO)){
            textArea.appendText("Registration is failed\n login or nickname is already used\n");
        }

    }

    public void tryToReg(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nickNameField.getText().trim();
        if (login.length()*password.length()*nickname.length() != 0){
            controller.registration(login, password, nickname);
        }

    }
}
