package gui;

import static util.ExceptionHandling.attempt;

import config.Config;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import proxy.auth.AuthDetails;
import proxy.auth.AuthDetailsManager;
import proxy.auth.AuthenticationMethod;
import proxy.auth.MicrosoftAuthHandler;

public class AuthTabController {
    public Button checkButton;
    public Button microsoftAuthButton;
    public VBox radioContainer;
    public Pane msAuthPane;
    public Pane manualAuthPane;
    public TextField accessToken;
    public TextField username;
    public Label statusText;
    public Hyperlink infoLink;

    private AuthDetails manualDetails;

    @FXML
    public void initialize() {
        infoLink.setOnAction(actionEvent -> GuiManager.openWebLink("https://github.com/mircokroon/minecraft-world-downloader/wiki/Authentication"));

        accessToken.textProperty().addListener((ov, oldV, newV) -> {
            // trim invalid characters, remove accessToken at front in case they copied the entire line
            String newToken = newV.trim()
                .replaceAll("[^A-Za-z0-9\\-_.]*", "")
                .replaceFirst("accessToken", "");
            accessToken.setText(newToken);

            manualDetails.setAccessToken(newToken);
        });

        ToggleGroup group = new ToggleGroup();
        for (AuthenticationMethod method : AuthenticationMethod.values()) {
            RadioButton btn = new RadioButton(method.getLabel());
            btn.setToggleGroup(group);
            btn.selectedProperty().addListener((v, o, n) -> {
                if (n) {
                    Config.setAuthMethod(method);
                    showAuthPane();
                }
            });
            if (Config.getAuthMethod() == method) {
                btn.setSelected(true);
            }
            btn.setPrefHeight(30);
            radioContainer.getChildren().add(btn);
        }
    }

    public void checkButtonPressed(ActionEvent actionEvent) {
        statusText.setText(validateAndGetLabel());
    }

    private String validateAndGetLabel() {
        try {
            AuthenticationMethod method = Config.getAuthMethod();
            AuthDetails details = AuthDetailsManager.getAuthDetails();

            boolean isValid = details.isValid();
            if (!isValid) {
                return method.getErrorMessage();
            }
            return "Valid session found! \n\nUsername: " + details.getUsername();

        } catch (IOException e) {
            return "Exception occurred: " + e.getMessage();
        }
    }

    public void msAuthPressed(ActionEvent actionEvent) {
        MsAuthController controller = GuiManager.loadMicrosoftLogin();
        controller.setHandlers(
            this::showAuthPane,
            ex -> statusText.setText("An error occured during Microsoft authentication.\n\n" + ex.getMessage()));
    }

    public void opened(GuiSettings guiSettings) {
        showAuthPane();
    }
    
    private void showAuthPane() {
        AuthenticationMethod method = Config.getAuthMethod();
        msAuthPane.setVisible(false);
        manualAuthPane.setVisible(false);
        statusText.setText("");

        switch (method) {
            case MANUAL -> {
                manualAuthPane.setVisible(true);

                if (manualDetails == null) {
                    manualDetails = new AuthDetails("");
                    Config.setManualAuthDetails(manualDetails);
                }
                accessToken.setText(manualDetails.getAccessToken());
            }
            case MICROSOFT -> {
                msAuthPane.setVisible(true);

                MicrosoftAuthHandler handler = Config.getMicrosoftAuth();

                if (handler != null && handler.hasLoggedIn()) {
                    statusText.setText("Microsoft login session present.");
                } else {
                    statusText.setText(method.getErrorMessage());
                }
            }
        }
    }
}