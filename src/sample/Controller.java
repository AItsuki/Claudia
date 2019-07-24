package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Controller {
    public Button captureBtn;
    public TextField redText;
    public TextField greenText;
    public TextField blueText;
    public TextField offsetText;
    public Button searchBtn;
    public CheckBox checkBox;
    public Canvas canvas;
    private Timer timer = new Timer();
    private Image image;

    @FXML
    private void initialize() {
        setColorInputType(redText, greenText, blueText);
        setOffsetInputType(offsetText);
        ScreenShot screenShot = ScreenShot.newInstance(new ScreenShot.Callback() {
            @Override
            public void onCancel() {
                if (checkBox.isSelected()) {
                    Stage stage = (Stage) captureBtn.getScene().getWindow();
                    stage.setIconified(false);
                }
            }

            @Override
            public void onCompleted(Image image) {
                Controller.this.image = image;
                GraphicsContext graph = canvas.getGraphicsContext2D();
                graph.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                graph.drawImage(image, 0, 0);
                if (checkBox.isSelected()) {
                    Stage stage = (Stage) captureBtn.getScene().getWindow();
                    stage.setIconified(false);
                }
            }
        });

        captureBtn.setOnAction(event -> {
            if (checkBox.isSelected()) {
                Stage stage = (Stage) captureBtn.getScene().getWindow();
                stage.setIconified(true);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(screenShot::capture);
                    }
                }, 500);
            } else {
                screenShot.capture();
            }
        });

        searchBtn.setOnAction(event -> {
            GraphicsContext graph = canvas.getGraphicsContext2D();
            graph.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            graph.drawImage(image, 0, 0);
            int red = getInputColor(redText);
            int green = getInputColor(greenText);
            int blue = getInputColor(blueText);
            int offset = getInputColor(offsetText);
            List<ColorInfo> result = new ArrayList<>();
            if (image != null) {
                PixelReader pixelReader = image.getPixelReader();
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = pixelReader.getArgb(x, y);
                        int tr = (argb >> 16) & 0xff;
                        int tg = (argb >> 8) & 0xff;
                        int tb = argb & 0xff;
                        if (red > tr - 3 && red < tr + offset && green > tg - offset && green < tg + offset && blue > tb - offset && blue < tb + offset) {
                            result.add(new ColorInfo(x, y, tr, tg, tb));
                        }
                    }
                }
            }
            System.out.println(result);
            graph.setStroke(Color.RED);
            graph.setLineWidth(1);
            for (ColorInfo colorInfo : result) {
                graph.strokeOval(colorInfo.x - 5, colorInfo.y - 5, 10, 10);
            }
        });
    }

    private void setColorInputType(TextField... textFields) {
        for (TextField textField : textFields) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.isEmpty() && !newValue.matches("\\d{0,3}")) {
                    textField.setText(oldValue);
                } else if (newValue.length() == 3) {
                    int n = Integer.valueOf(newValue);
                    textField.setText(n > 255 ? "255" : newValue);
                } else {
                    textField.setText(newValue);
                }
            });
        }
    }

    private void setOffsetInputType(TextField... textFields) {
        for (TextField textField : textFields) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.isEmpty() && !newValue.matches("\\d{0,2}")) {
                    textField.setText(oldValue.isEmpty()? "3" : oldValue);
                } else if (newValue.length() > 0){
                    int n = Integer.valueOf(newValue);
                    if (n > 30) {
                        textField.setText("30");
                    } else if (n < 3) {
                        textField.setText("3");
                    } else {
                        textField.setText(newValue);
                    }
                } else {
                    textField.setText(newValue);
                }
            });
        }
    }


    private int getInputColor(TextField textField) {
        String colorStr = textField.getText();
        if (colorStr.isEmpty()) {
            colorStr = "0";
        }
        return Integer.valueOf(colorStr);
    }

    class ColorInfo {
        int x;
        int y;
        int r;
        int g;
        int b;

        ColorInfo(int x, int y, int r, int g, int b) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        public String toString() {
            return "ColorInfo{" +
                    "x=" + x +
                    ", y=" + y +
                    ", r=" + r +
                    ", g=" + g +
                    ", b=" + b +
                    '}';
        }
    }
}
