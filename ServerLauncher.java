import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ServerLauncher extends JFrame {
    private JComboBox<String> serverToolComboBox;
    private JSlider ramSlider;
    private JTextArea consoleArea;
    private JTextField commandInput;
    private Process minecraftServerProcess;

    private static final String JAVA_DOWNLOAD_URL = "https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.zip";
    private static final String[] SERVER_TOOLS = {
            "Bukkit",
            "Spigot",
            "Paper",
            "Forge",
            "Sponge",
            "Mohist"
    };

    public ServerLauncher() {
        setTitle("自動伺服器啟動器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel createServerPanel = createCreateServerPanel();
        JPanel executeCommandPanel = createExecuteCommandPanel();

        add(createServerPanel, BorderLayout.NORTH);
        add(executeCommandPanel, BorderLayout.CENTER);
    }

    private JPanel createCreateServerPanel() {
        JPanel createServerPanel = new JPanel();
        createServerPanel.setLayout(new FlowLayout());

        serverToolComboBox = new JComboBox<>(SERVER_TOOLS);
        createServerPanel.add(new JLabel("選擇伺服器工具："));
        createServerPanel.add(serverToolComboBox);

        ramSlider = new JSlider(1, 16, 4);
        ramSlider.setMajorTickSpacing(1);
        ramSlider.setPaintTicks(true);
        ramSlider.setPaintLabels(true);
        createServerPanel.add(new JLabel("選擇RAM大小（GB）："));
        createServerPanel.add(ramSlider);

        JButton createServerButton = new JButton("創建伺服器");
        createServerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createMinecraftServer();
            }
        });
        createServerPanel.add(createServerButton);

        JButton openServerButton = new JButton("開啟伺服器");
        openServerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openMinecraftServer();
            }
        });
        createServerPanel.add(openServerButton);

        return createServerPanel;
    }

    private JPanel createExecuteCommandPanel() {
        JPanel executeCommandPanel = new JPanel();
        executeCommandPanel.setLayout(new BorderLayout());

        commandInput = new JTextField(30);
        commandInput.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                executeCommand();
            }
        });
        executeCommandPanel.add(commandInput, BorderLayout.NORTH);

        JButton executeButton = new JButton("執行指令");
        executeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                executeCommand();
            }
        });
        executeCommandPanel.add(executeButton, BorderLayout.CENTER);

        consoleArea = new JTextArea(15, 30);
        consoleArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(consoleArea);
        executeCommandPanel.add(scrollPane, BorderLayout.SOUTH);

        return executeCommandPanel;
    }

    private void createMinecraftServer() {
        String selectedTool = (String) serverToolComboBox.getSelectedItem();
        int ramInGB = ramSlider.getValue();

        consoleArea.setText("下載 Java 17...\n");

        try {
            downloadAndExtractJava17();

            consoleArea.append("下載 " + selectedTool + "...\n");

            String downloadURL = getDownloadURL(selectedTool);
            String toolFileName = downloadURL.substring(downloadURL.lastIndexOf('/') + 1);
            downloadFile(downloadURL, toolFileName);

            String renamedFileName = "server.jar";
            renameFile(toolFileName, renamedFileName);

            createBatFile(renamedFileName, ramInGB);

            acceptEULA(); // 自動同意 EULA

            consoleArea.append("伺服器創建完成。\n");
        } catch (IOException e) {
            consoleArea.append("錯誤發生：" + e.getMessage());
        }
    }

    private void acceptEULA() throws IOException {
        Files.write(Paths.get("eula.txt"), "eula=true".getBytes());
    }

    private void downloadAndExtractJava17() throws IOException {
        downloadFile(JAVA_DOWNLOAD_URL, "java17.zip");
        unzip("java17.zip", ".");
    }

    private String getDownloadURL(String serverTool) {
        switch (serverTool) {
            case "Bukkit":
                return "https://download.getbukkit.org/craftbukkit/craftbukkit-1.19.4.jar";
            case "Spigot":
                return "https://download.getbukkit.org/spigot/spigot-1.19.4.jar";
            case "Paper":
                return "https://api.papermc.io/v2/projects/paper/versions/1.19.4/builds/545/downloads/paper-1.19.4-545.jar";
            case "Forge":
                return "https://maven.minecraftforge.net/net/minecraftforge/forge/1.19.4-45.0.66/forge-1.19.4-45.0.66-installer.jar";
            case "Sponge":
                return "https://repo.spongepowered.org/repository/maven-releases/org/spongepowered/spongevanilla/1.19.4-10.0.0-RC1341/spongevanilla-1.19.4-10.0.0-RC1341-universal.jar";
            case "Mohist":
                return "https://mohistmc.com/builds/1.19.4/mohist-1.19.4-120-server.jar";
            default:
                throw new IllegalArgumentException("無效的伺服器工具：" + serverTool);
        }
    }

    private void downloadFile(String url, String fileName) throws IOException {
        URL downloadURL = new URL(url);
        try (InputStream in = downloadURL.openStream()) {
            Files.copy(in, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void renameFile(String oldName, String newName) throws IOException {
        Files.move(Paths.get(oldName), Paths.get(newName), StandardCopyOption.REPLACE_EXISTING);
    }

    private void createBatFile(String serverFileName, int ramInGB) throws IOException {
        String batFileName = "start_server.bat";
        String content = "@echo off\n";
        content += "java -Xmx" + ramInGB + "G -Xms" + ramInGB + "G -jar " + serverFileName;

        Files.write(Paths.get(batFileName), content.getBytes());
    }

    private void openMinecraftServer() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "start_server.bat");
            processBuilder.redirectErrorStream(true);
            minecraftServerProcess = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(minecraftServerProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                consoleArea.append(line + "\n");
            }
        } catch (IOException e) {
            consoleArea.append("錯誤發生：" + e.getMessage() + "\n");
        }
    }

    private void executeCommand() {
        String command = commandInput.getText();
        consoleArea.append("> " + command + "\n");
        commandInput.setText("");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                consoleArea.append(line + "\n");
            }
        } catch (IOException e) {
            consoleArea.append("錯誤發生：" + e.getMessage() + "\n");
        }
    }

    private void unzip(String zipFilePath, String destDir) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    new File(filePath).getParentFile().mkdirs();
                    try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ServerLauncher serverLauncher = new ServerLauncher();
                serverLauncher.setVisible(true);
            }
        });
    }
}
