import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

public class SpeedTest {

    private static final int BUFFER_SIZE = 8192;
    private static final String URL_100MB = "https://nbg1-speed.hetzner.com/100MB.bin";
    private static final String URL_1GB = "https://nbg1-speed.hetzner.com/1GB.bin";
    private static final String URL_10GB = "https://nbg1-speed.hetzner.com/10GB.bin";

    public static void main(String[] args) {
        String selectedUrl = null;

        for (String arg : args) {
            switch (arg) {
                case "--light":
                    selectedUrl = URL_100MB;
                    break;
                case "--standard":
                    selectedUrl = URL_1GB;
                    break;
                case "--heavy":
                    selectedUrl = URL_10GB;
                    break;
            }
        }

        if (selectedUrl == null) {
            System.out.println("Usage: java -jar speedtest.jar [--light | --standard | --heavy]");
            System.out.println("  --light     = 100MB test");
            System.out.println("  --standard  = 1GB test");
            System.out.println("  --heavy     = 10GB test");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        String fileName = "test_download.bin";

        String finalSelectedUrl = selectedUrl;
        Future<Boolean> future = executor.submit(() -> runSpeedTest(finalSelectedUrl, fileName));

        try {
            if (!future.get()) {
                System.err.println("Speed test failed.");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    public static boolean runSpeedTest(String fileURL, String savePath) {
        System.out.println("Starting network speed test...");

        long startTime = System.nanoTime();

        try {
            URL url = new URL(fileURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int contentLength = connection.getContentLength();

            if (contentLength == -1) {
                System.err.println("Unable to determine file size.");
                return false;
            }

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fileOut = new FileOutputStream(savePath);
                 BufferedOutputStream out = new BufferedOutputStream(fileOut, BUFFER_SIZE)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytes = 0;
                long lastReportTime = System.nanoTime();
                double peakSpeed = 0;

                long checkpoint50 = 0;
                long checkpoint100 = 0;

                boolean reached50 = false;
                boolean reached100 = false;

                while ((bytesRead = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;

                    long currentTime = System.nanoTime();
                    double elapsedSeconds = (currentTime - startTime) / 1_000_000_000.0;
                    double currentSpeed = (totalBytes / 1024.0 / 1024.0) / elapsedSeconds;

                    if (currentSpeed > peakSpeed) {
                        peakSpeed = currentSpeed;
                    }

                    int percent = (int) ((totalBytes * 100L) / contentLength);

                    if (!reached50 && percent >= 50) {
                        checkpoint50 = currentTime;
                        reached50 = true;
                    }
                    if (!reached100 && percent >= 100) {
                        checkpoint100 = currentTime;
                        reached100 = true;
                    }

                    if ((currentTime - lastReportTime) / 1_000_000_000.0 >= 0.5) {
                        System.out.printf("Testing: %d%% complete, Current speed: %.2f MB/s\r", percent, currentSpeed);
                        lastReportTime = currentTime;
                    }
                }

                long endTime = System.nanoTime();
                double totalTimeSec = (endTime - startTime) / 1_000_000_000.0;
                double avgSpeed = (totalBytes / 1024.0 / 1024.0) / totalTimeSec;
                double totalMB = totalBytes / 1024.0 / 1024.0;

                System.out.printf("%n%n----- Network Speed Test Report -----%n");
                System.out.printf("Tested URL             : %s%n", fileURL);
                System.out.printf("Data Transferred       : %.2f MB%n", totalMB);
                System.out.printf("Total Time             : %.2f seconds%n", totalTimeSec);
                System.out.printf("Average Speed          : %.2f MB/s%n", avgSpeed);
                System.out.printf("Peak Speed             : %.2f MB/s%n", peakSpeed);

                if (reached50) {
                    System.out.printf("Time to 50%%            : %.2f s%n", (checkpoint50 - startTime) / 1_000_000_000.0);
                }
                if (reached100) {
                    System.out.printf("Time to 100%%           : %.2f s%n", (checkpoint100 - startTime) / 1_000_000_000.0);
                }

                File file = new File(savePath);
                if (file.exists() && file.delete()) {
                    System.out.println("File Cleanup           : File deleted after test");
                } else {
                    System.out.println("File Cleanup           : File not deleted");
                }

                System.out.println("---------------------------------------");

                return true;
            }
        } catch (IOException e) {
            System.err.println("Error during download: " + e.getMessage());
            return false;
        }
    }
}
