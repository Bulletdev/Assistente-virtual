import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.util.Properties;

public class ClippyApp {

    private static String API_KEY;

    static {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(fis);
            API_KEY = prop.getProperty("OPENAI_API_KEY");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Falha ao carregar a chave API. Verifique se o arquivo config.properties existe e contém a chave OPENAI_API_KEY.");
        }
    }

    private static final String CLIPPY_OCIOSO = "clippy_idle.gif";
    private static final String CLIPPY_PESQUISANDO = "clippy_searching.gif";
    private static final String CLIPPY_INTERAGINDO = "clippy_interacting.gif";

    private static JLabel imageLabel;
    private static ImageIcon clippyOciosoIcon;
    private static ImageIcon clippyPesquisandoIcon;
    private static ImageIcon clippyInteragindoIcon;
    private static JTextArea resultArea;
    private static JTextField searchField;
    private static JButton searchButton;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Assistente Virtual");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Carregando ícones
        clippyOciosoIcon = new ImageIcon(CLIPPY_OCIOSO);
        clippyPesquisandoIcon = new ImageIcon(CLIPPY_PESQUISANDO);
        clippyInteragindoIcon = new ImageIcon(CLIPPY_INTERAGINDO);

        // Painel principal
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 248, 255)); // Alice Blue
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Painel do assistente
        JPanel assistantPanel = createAssistantPanel();
        mainPanel.add(assistantPanel, BorderLayout.WEST);

        // Painel de chat
        JPanel chatPanel = createChatPanel();
        mainPanel.add(chatPanel, BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static JPanel createAssistantPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(230, 230, 250)); // Lavender
        panel.setBorder(BorderFactory.createLineBorder(new Color(147, 112, 219), 2)); 
        panel.setPreferredSize(new Dimension(200, 0));

        imageLabel = new JLabel(clippyOciosoIcon);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                trocarAnimacao(clippyInteragindoIcon);
                JOptionPane.showMessageDialog(null, "Olá! Como posso ajudar?", "Assistente Virtual", JOptionPane.INFORMATION_MESSAGE);
                trocarAnimacao(clippyOciosoIcon);
            }
        });

        JLabel nameLabel = new JLabel("Assistente Virtual");
        nameLabel.setHorizontalAlignment(JLabel.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(nameLabel, BorderLayout.SOUTH);

        return panel;
    }

    private static JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 14));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setBackground(new Color(248, 248, 255)); // Ghost White

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 250), 1)); 

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(147, 112, 219), 1), 
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        searchButton = new JButton("Enviar");
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        searchButton.setBackground(new Color(147, 112, 219)); // Medium Purple
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);

        inputPanel.add(searchField, BorderLayout.CENTER);
        inputPanel.add(searchButton, BorderLayout.EAST);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        ActionListener searchAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String query = searchField.getText();
                if (!query.trim().isEmpty()) {
                    trocarAnimacao(clippyPesquisandoIcon);
                    resultArea.setText("Consultando o assistente...\n");
                    
                    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                        @Override
                        protected String doInBackground() throws Exception {
                            return sendChatGPTRequest(query);
                        }

                        @Override
                        protected void done() {
                            try {
                                String response = get();
                                resultArea.setText("Você: " + query + "\n\nAssistente: " + response);
                            } catch (Exception ex) {
                                resultArea.setText("Erro: " + ex.getMessage());
                            } finally {
                                trocarAnimacao(clippyOciosoIcon);
                                searchField.setText("");
                            }
                        }
                    };
                    worker.execute();
                }
            }
        };

        searchButton.addActionListener(searchAction);
        searchField.addActionListener(searchAction);

        return panel;
    }

    private static void trocarAnimacao(ImageIcon novaAnimacao) {
        imageLabel.setIcon(novaAnimacao);
        imageLabel.revalidate();
        imageLabel.repaint();
    }

    private static String sendChatGPTRequest(String query) {
        String responseText = "";
        int maxRetries = 5; 
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                System.out.println("Tentativa " + (retryCount + 1) + " de " + maxRetries);
                
                URI uri = new URI("https://api.openai.com/v1/chat/completions");
                URL url = uri.toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000); // 30 segundos de timeout
                connection.setReadTimeout(60000); // 60 segundos de timeout

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "gpt-3.5-turbo");
                requestBody.put("messages", new JSONObject[]{
                    new JSONObject().put("role", "system").put("content", "Você é um assistente virtual amigável e prestativo."),
                    new JSONObject().put("role", "user").put("content", query)
                });

                String jsonInputString = requestBody.toString();
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                System.out.println("Código de resposta: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        responseText = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                        System.out.println("Resposta recebida com sucesso");
                        break;
                    }
                } else {
                    String errorMessage = "Erro: " + responseCode + " - " + connection.getResponseMessage();
                    System.err.println(errorMessage);
                    
                    if (responseCode == 429) {
                        retryCount++;
                        long waitTime = (long) Math.pow(2, retryCount) * 1000;
                        System.err.println("Limite de taxa atingido. Tentando novamente em " + waitTime + "ms");
                        Thread.sleep(waitTime);
                    } else if (responseCode == 401) {
                        responseText = "Erro: Chave API inválida. Por favor, verifique sua chave API e tente novamente.";
                        break;
                    } else {
                        responseText = errorMessage;
                        retryCount++;
                        if (retryCount < maxRetries) {
                            System.err.println("Tentando novamente em 5 segundos...");
                            Thread.sleep(5000);
                        }
                    }
                }
            } catch (Exception e) {
                String errorMessage = "Erro: " + e.getMessage();
                System.err.println(errorMessage);
                e.printStackTrace();
                responseText = errorMessage;
                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        System.err.println("Tentando novamente em 5 segundos...");
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (retryCount == maxRetries) {
            responseText = "Erro: Número máximo de tentativas excedido. Por favor, tente novamente mais tarde.";
        }

        return responseText;
    }
}