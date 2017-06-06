/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package musicplayer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;

/**
 *
 * @author Marek
 */
public class MainWindowController implements Initializable {

    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem closeMenuItem;
    @FXML
    private MenuItem aboutMenuItem;
    @FXML
    private Slider slider;
    @FXML
    private Label fileName;
    @FXML
    private Button playButton;
    @FXML
    private BarChart<String, Number> visualiser;

    private static final double INTERVAL = 0.005;
    private static final int BANDS = 32;
    private static final double DROPDOWN = 0.25;
    private XYChart.Data[] seriesData;
    private File selectedFile;
    private String audioPath;
    private MediaPlayer mediaPlayer;
    private Media media;

    @FXML
    private void handlePlayButtonAction(ActionEvent event) {
        if (mediaPlayer != null) {
            slider.setMax(mediaPlayer.getTotalDuration().toSeconds());

            if (mediaPlayer.getStatus().equals(MediaPlayer.Status.DISPOSED)) {
                loadMedia();
            }

            if (!mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                mediaPlayer.play();
                playButton.setText("Pause");
            } else {
                mediaPlayer.pause();
                playButton.setText("Play");
            }
        }
    }

    @FXML
    private void handleOpenMenuItemActtion(ActionEvent event) {
        if (mediaPlayer != null && mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
            mediaPlayer.stop();
            playButton.setText("Play");
            slider.setValue(0);
        }
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("MP3 files", "*.mp3");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Open MP3 File");
        selectedFile = fileChooser.showOpenDialog(MusicPlayer.getStage());
        try {
            media = new Media(selectedFile.toURI().toURL().toString());
            loadMedia();
        } catch (MalformedURLException ex) {
            System.out.println("Exception while reading file");
        }
    }

    @FXML
    private void handleCloseMenuItemAction(ActionEvent event) {
        MusicPlayer.getStage().close();
    }

    @FXML
    private void handleAboutMenuItemAction(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Cześć! Jestem odtwarzaczem z wizualizacją ;)");
        alert.setContentText("Zostałem napisany po to, by mój stwórca mógł zaliczyć ciekawy przedmiot. Szkoda tylko, że pozostało tak mało czasu na poznanie tego tematu po zrobieniu na przykład takich algorytmów...");
        alert.show();
    }

    private void loadMedia() {
        if (selectedFile != null) {
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.currentTimeProperty().addListener((Observable observable) -> {
                slider.setValue(mediaPlayer.getCurrentTime().toSeconds());
            });
            mediaPlayer.setAudioSpectrumListener(new SpektrumListener());
            mediaPlayer.setAudioSpectrumNumBands(BANDS);
            mediaPlayer.setAudioSpectrumInterval(INTERVAL);
            mediaPlayer.setOnEndOfMedia(() -> {
                slider.setValue(slider.getMax());
                loadMedia();
                playButton.setText("Play");
            });
            fileName.setText(selectedFile.getName().replace(".mp3", ""));
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        slider.setDisable(true);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        seriesData = new XYChart.Data[BANDS + 2];
        for (int i = 0; i < seriesData.length; i++) {
            seriesData[i] = new XYChart.Data<>(Integer.toString(i + 1), 0);
            //noinspection unchecked
            series.getData().add(seriesData[i]);
        }
        visualiser.getData().add(series);
        visualiser.setBarGap(0);
        visualiser.setCategoryGap(0);
    }

    private float[] createFilledBuffer(int size, float fillValue) {
        float[] floats = new float[size];
        Arrays.fill(floats, fillValue);
        return floats;
    }

    private class SpektrumListener implements AudioSpectrumListener {

        float[] buffer = createFilledBuffer(BANDS, mediaPlayer.getAudioSpectrumThreshold());

        @Override
        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
            Platform.runLater(() -> {
                //noinspection unchecked
                seriesData[0].setYValue(0);
                //noinspection unchecked
                seriesData[BANDS + 1].setYValue(0);
                for (int i = 0; i < magnitudes.length; i++) {
                    if (magnitudes[i] >= buffer[i]) {
                        buffer[i] = magnitudes[i];
                        //noinspection unchecked
                        seriesData[i + 1].setYValue(magnitudes[i] - mediaPlayer.getAudioSpectrumThreshold());
                    } else {
                        //noinspection unchecked
                        seriesData[i + 1].setYValue(buffer[i] - mediaPlayer.getAudioSpectrumThreshold());
                        buffer[i] -= DROPDOWN;
                    }
                }
            });
        }
    }
}
