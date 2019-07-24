package sample;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

/**
 * 开始截屏时截取整个屏幕，然后在这个全屏截图上操作（可以理解为裁剪）。
 */
public class ScreenShot {

    private Callback callback;
    private int startX, startY, currX, currY;
    private int left, top, right, bottom;
    private Dimension screenDimen; // 屏幕信息

    private Stage stage;
    private Scene scene;
    private AnchorPane pane;
    private Canvas canvas;
    private Label indicator;
    private HBox toolBar;

    public static ScreenShot newInstance(Callback callback) {
        return new ScreenShot(callback);
    }

    private ScreenShot(Callback callback) {
        this.callback = callback;
        initScreen(); // 初始化屏幕信息
        initShade(); // 初始化截屏遮罩
        initSizeIndicator(); // 初始化截屏分辨率指示器
        initToolBar(); // 初始化截屏工具条
        observeDragEvent(pane, canvas); // 监听拖拽事件
    }

    private void initScreen() {
        screenDimen = Toolkit.getDefaultToolkit().getScreenSize();
        left = 0;
        top = 0;
        right = screenDimen.width;
        bottom = screenDimen.height;
        startX = 0;
        startY = 0;
    }

    private void initShade() {
        pane = new AnchorPane();
        pane.setPrefWidth(screenDimen.width);
        pane.setPrefHeight(screenDimen.height);
        canvas = new Canvas(screenDimen.width, screenDimen.height);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setStroke(Color.valueOf("#FF4500FF"));
        graphics.setFill(Color.valueOf("#00000088"));
        graphics.setLineWidth(2);
        graphics.fillRect(0, 0, screenDimen.width, screenDimen.height);
        pane.getChildren().add(canvas);
        scene = new Scene(pane, screenDimen.width, screenDimen.height);
        stage = new Stage();
        stage.setFullScreen(true);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("");
        stage.setScene(scene);
    }

    // 创建分辨率指示器
    private void initSizeIndicator() {
        indicator = new Label();
        indicator.setStyle("-fx-background-radius: 2; -fx-background-color: #00000099; -fx-text-fill: #FFF");
        indicator.setPadding(new Insets(3));
        indicator.setVisible(false);
        pane.getChildren().add(indicator);
    }

    // 创建工具栏
    private void initToolBar() {
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER_RIGHT);
        toolBar.setPrefWidth(50);
        Button completeBtn = new Button("完成");
        completeBtn.setOnAction(event -> {
            completeCapture();
        });
        toolBar.getChildren().add(completeBtn);
        toolBar.setVisible(false);
        pane.getChildren().add(toolBar);
    }

    public void capture() {
        reset();
        Image screenCapture = getCapture(new Rectangle(screenDimen));
        if (screenCapture != null) {
            pane.setBackground(new Background(new BackgroundImage(screenCapture, null, null, null, null)));
            stage.show();
        }
    }

    private void observeDragEvent(AnchorPane pane, Canvas canvas) {
        scene.setOnKeyPressed(event -> {
            System.out.println(event);
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelCapture();
            } else if (event.getCode() == KeyCode.ENTER) {
                completeCapture();
            }
        });

        pane.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() > 1) {
                completeCapture();
            } else if (event.getButton().equals(MouseButton.SECONDARY)) {
                cancelCapture();
            }
        });

        pane.setOnDragDetected(event -> {
            if (event.isPrimaryButtonDown() && event.getClickCount() == 1) {
                pane.startFullDrag();
                reset();
                startX = (int) event.getScreenX();
                startY = (int) event.getScreenY();
                AnchorPane.setLeftAnchor(indicator, (double) startX);
                AnchorPane.setTopAnchor(indicator, (double) startY - 26);
                System.out.println("entered: " + startX + ", " + startY);
            }
        });

        pane.setOnMouseDragOver(event -> {
            currX = (int) event.getScreenX();
            currY = (int) event.getScreenY();
            System.out.println("dragged:" + currX + ", " + currY);
            left = Math.min(startX, currX);
            top = Math.min(startY, currY);
            right = Math.max(startX, currX);
            bottom = Math.max(startY, currY);
            GraphicsContext graphics = canvas.getGraphicsContext2D();
            graphics.clearRect(0, 0, screenDimen.width, screenDimen.height);
            graphics.fillRect(0, 0, screenDimen.width, screenDimen.height);
            graphics.clearRect(left, top, right - left, bottom - top);
            graphics.strokeRect(left - 2, top - 2, right - left + 4, bottom - top + 4);
            indicator.setVisible(true);
            indicator.setText(bottom - top + " X " + (right - left));
        });

        pane.setOnMouseDragReleased(event -> {
            System.out.println("result rect is [" + left + ", " + top + ", " + right + ", " + bottom + "]");
            AnchorPane.setTopAnchor(toolBar, (double) bottom + 6);
            AnchorPane.setLeftAnchor(toolBar, right - toolBar.getWidth() + 2);
            toolBar.setVisible(true);
        });
    }

    private void reset() {
        indicator.setVisible(false);
        toolBar.setVisible(false);
        AnchorPane.clearConstraints(indicator);
        AnchorPane.clearConstraints(toolBar);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, screenDimen.width, screenDimen.height);
        graphics.fillRect(0, 0, screenDimen.width, screenDimen.height);
    }

    private void completeCapture() {
        Image image = getCapture(new Rectangle(left, top, right - left, bottom - top));
        callback.onCompleted(image);
        stage.close();
    }

    private void cancelCapture() {
        callback.onCancel();
        stage.close();
    }

    private Image getCapture(Rectangle rectangle) {
        try {
            Robot robot = new Robot();
            BufferedImage screenCapture = robot.createScreenCapture(rectangle);
            return SwingFXUtils.toFXImage(screenCapture, null);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        return null;
    }

    interface Callback {
        void onCancel();

        void onCompleted(Image image);
    }
}
