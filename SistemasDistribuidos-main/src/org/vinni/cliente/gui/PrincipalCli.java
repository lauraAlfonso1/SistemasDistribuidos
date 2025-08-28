package org.vinni.cliente.gui;

import org.vinni.dto.MiDatagrama;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
  LAURA ALFONSO
*/
public class PrincipalCli extends JFrame {

    private final int PORT = 12345;

    // ==Archivo ===
    private static final int TamanioDatagrama = 1400;
    private String ipServidor = "127.0.0.1";
    private final JFileChooser selectorArchivo = new JFileChooser();

    // === Interfaz ===
    private JButton btEnviar;
    private JButton btEnviarArchivo;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JScrollPane jScrollPane1;
    private JTextPane logPane;
    private JTextField mensajeTxt;
    private JProgressBar progresoCli;

    // === Nuevo estilo===
    private StyledDocument doc;
    private Style stTime, stInfo, stOk, stWarn, stErr, stServer, stClient, stSection, stMono;
    private final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss");

    // — Progreso barra—
    private int progressStartOffset = -1;
    private String progressTitle = null;

    // — Barra de progreso  —
    private javax.swing.Timer timerProgress;
    private int smoothCurrent = 0;
    private int smoothTarget = 0;
    private boolean progressActive = false;

    private int ultimoPctMostrado = -1;

    public PrincipalCli() {
        initComponents();
        this.btEnviar.setEnabled(true);
        initLogStyles();
        logSection("📡 Cliente listo");

        // ===== tiempo de la barra de progreso =====
        timerProgress = new javax.swing.Timer(30, e -> {
            if (!progressActive) return;
            if (smoothCurrent < smoothTarget) {
                int gap = smoothTarget - smoothCurrent;
                int step = Math.max(1, gap / 8);
                step = Math.min(step, 3); // dar tiempo
                smoothCurrent += step;
                progresoCli.setValue(smoothCurrent);
                setProgress(smoothCurrent);
            }
        });
        timerProgress.start();
    }

    private void initComponents() {
        this.setTitle("Cliente UDP");

        jLabel1 = new JLabel("CLIENTE UDP : LUING");
        jScrollPane1 = new JScrollPane();
        logPane = new JTextPane();
        mensajeTxt = new JTextField();
        jLabel2 = new JLabel();
        btEnviar = new JButton();
        btEnviarArchivo = new JButton();
        progresoCli = new JProgressBar(0, 100);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 16));
        jLabel1.setForeground(new java.awt.Color(30, 136, 229));
        jLabel1.setBounds(110, 10, 250, 20);
        getContentPane().add(jLabel1);

        jScrollPane1.setViewportView(logPane);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(30, 210, 410, 110);

        mensajeTxt.setFont(new java.awt.Font("Segoe UI", 0, 14));
        getContentPane().add(mensajeTxt);
        mensajeTxt.setBounds(40, 120, 350, 30);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14));
        jLabel2.setText("Mensaje:");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(20, 90, 120, 30);

        btEnviar.setFont(new java.awt.Font("Segoe UI", 0, 14));
        btEnviar.setText("Enviar");
        btEnviar.addActionListener(this::btEnviarActionPerformed);
        getContentPane().add(btEnviar);
        btEnviar.setBounds(327, 160, 120, 27);

        btEnviarArchivo.setFont(new java.awt.Font("Segoe UI", 0, 14));
        btEnviarArchivo.setText("Enviar archivo");
        btEnviarArchivo.addActionListener(evt -> btEnviarArchivoActionPerformed());
        getContentPane().add(btEnviarArchivo);
        btEnviarArchivo.setBounds(170, 160, 140, 27);

        progresoCli.setStringPainted(true);
        getContentPane().add(progresoCli);
        progresoCli.setBounds(30, 190, 410, 20);

        setSize(new java.awt.Dimension(491, 375));
        setLocationRelativeTo(null);
    }

    private void btEnviarActionPerformed(java.awt.event.ActionEvent evt) {
        this.enviarMensaje();
    }

    private void btEnviarArchivoActionPerformed() {
        int res = selectorArchivo.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = selectorArchivo.getSelectedFile();
            if (f != null && f.exists() && f.isFile()) {
                enviarArchivo(f);
            } else {
                JOptionPane.showMessageDialog(this, "Archivo inválido.");
            }
        }
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalCli().setVisible(true));
    }

    // ========= estilos =========
    private void initLogStyles() {
        doc = logPane.getStyledDocument();
        logPane.setEditable(false);
        logPane.setBackground(new Color(250, 250, 250));
        logPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        stTime = doc.addStyle("time", def);
        StyleConstants.setForeground(stTime, new Color(120, 120, 120));

        stInfo = doc.addStyle("info", def);
        StyleConstants.setForeground(stInfo, new Color(60, 60, 60));

        stOk = doc.addStyle("ok", def);
        StyleConstants.setForeground(stOk, new Color(46, 125, 50));

        stWarn = doc.addStyle("warn", def);
        StyleConstants.setForeground(stWarn, new Color(230, 145, 56));

        stErr = doc.addStyle("err", def);
        StyleConstants.setForeground(stErr, new Color(198, 40, 40));

        stServer = doc.addStyle("server", def);
        StyleConstants.setForeground(stServer, new Color(25, 118, 210));

        stClient = doc.addStyle("client", def);
        StyleConstants.setForeground(stClient, new Color(94, 53, 177));

        stSection = doc.addStyle("section", def);
        StyleConstants.setBold(stSection, true);
        StyleConstants.setFontSize(stSection, 14);

        stMono = doc.addStyle("mono", def);
        StyleConstants.setFontFamily(stMono, "Consolas");
        StyleConstants.setForeground(stMono, new Color(80, 80, 80));
    }

    private void logLine(String icon, String text, Style style) {
        SwingUtilities.invokeLater(() -> {
            try {
                String ts = "[" + LocalTime.now().format(HHMMSS) + "] ";
                doc.insertString(doc.getLength(), ts, stTime);
                if (icon != null) doc.insertString(doc.getLength(), icon + " ", style);
                doc.insertString(doc.getLength(), text + "\n", style);
                logPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private void logSection(String t) { logLine("—", t, stSection); }
    private void logInfo(String t)    { logLine("•", t, stInfo); }
    private void logErr(String t)     { logLine("✖", t, stErr); }
    private void logServer(String t)  { logLine("🖥", t, stServer); }
    private void logClient(String t)  { logLine("📨", t, stClient); }

    // — Progreso barra —
    private void beginProgress(String title) {
        SwingUtilities.invokeLater(() -> {
            try {
                progressTitle = title;
                String ts = "[" + LocalTime.now().format(HHMMSS) + "] ";
                doc.insertString(doc.getLength(), ts, stTime);
                progressStartOffset = doc.getLength();
                doc.insertString(doc.getLength(), "⏳ " + title + " — 0%\n", stInfo);
                logPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }
    private void setProgress(int pct) {
        if (progressStartOffset < 0) return;
        SwingUtilities.invokeLater(() -> {
            try {
                int lineEnd = findLineEndFrom(progressStartOffset);
                doc.remove(progressStartOffset, lineEnd - progressStartOffset);
                doc.insertString(progressStartOffset,
                        "⏳ " + (progressTitle != null ? progressTitle : "Progreso") + " — " + pct + "%\n",
                        stInfo);
                logPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }
    private void endProgress(boolean ok, String suffix) {
        if (progressStartOffset < 0) return;
        SwingUtilities.invokeLater(() -> {
            try {
                int lineEnd = findLineEndFrom(progressStartOffset);
                doc.remove(progressStartOffset, lineEnd - progressStartOffset);
                Style st = ok ? stOk : stWarn;
                String icon = ok ? "✅" : "🟠";
                doc.insertString(progressStartOffset,
                        icon + " " + (progressTitle != null ? progressTitle : "Progreso") +
                                " — 100% " + (suffix != null ? ("· " + suffix) : "") + "\n",
                        st);
                logPane.setCaretPosition(doc.getLength());
                progressStartOffset = -1;
                progressTitle = null;
            } catch (BadLocationException ignored) {}
        });
    }
    private int findLineEndFrom(int start) throws BadLocationException {
        String text = doc.getText(0, doc.getLength());
        int idx = text.indexOf('\n', start);
        return (idx == -1) ? doc.getLength() : idx + 1;
    }

    // ========= logica para la barra de progreso=========
    private void actualizarProgresoCliente(int pct, boolean forzarLog) {
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;


        smoothTarget = pct;

        if (forzarLog || pct == 100 || ultimoPctMostrado < 0 || pct - ultimoPctMostrado >= 5) {
            ultimoPctMostrado = pct;
            setProgress(pct); // actualiza la línea del log
        }
    }

    private void enviarMensaje() {
        String mensaje = mensajeTxt.getText();
        if (mensaje.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay mensaje para enviar");
            return;
        }
        try (DatagramSocket canal = new DatagramSocket()) {
            try {
                canal.setSendBufferSize(256 * 1024);
                canal.setReceiveBufferSize(256 * 1024);
            } catch (SocketException ignored) {}

            DatagramPacket mensajeDG = MiDatagrama.crearDataG(ipServidor, PORT, mensaje);
            canal.send(mensajeDG);
            logClient("Mensaje enviado");

            mensajeTxt.setText("");
            mensajeTxt.requestFocusInWindow();

            canal.setSoTimeout(500);
            try {
                byte[] buf = new byte[2048];
                DatagramPacket resp = new DatagramPacket(buf, buf.length);
                canal.receive(resp);
                String texto = new String(resp.getData(), 0, resp.getLength(), StandardCharsets.UTF_8);
                logServer(texto);
            } catch (SocketTimeoutException ignored) {}

        } catch (SocketException ex) {
            Logger.getLogger(PrincipalCli.class.getName()).log(Level.SEVERE, null, ex);
            logErr("Socket: " + ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(PrincipalCli.class.getName()).log(Level.SEVERE, null, ex);
            logErr("E/S: " + ex.getMessage());
        }
    }

    private void enviarArchivo(File archivo) {
        long tamanio = archivo.length();
        String nombre = archivo.getName();

        // reeiniciar y  dar animacion de barra
        progressActive = true;
        smoothCurrent = 0;
        smoothTarget = 0;
        progresoCli.setValue(0);
        ultimoPctMostrado = -1;

        logSection("📤 Envío de archivo");
        logInfo("Nombre: " + nombre);
        logInfo("Tamaño: " + tamanio + " bytes");
        beginProgress("Enviando " + nombre);

        final int overhead = 5; // "DATA:"
        final int maxPayload = Math.max(1, TamanioDatagrama - overhead);

        try (DatagramSocket canal = new DatagramSocket();
             FileInputStream fis = new FileInputStream(archivo)) {

            try {
                canal.setSendBufferSize(512 * 1024);
                canal.setTrafficClass(0x10); // LowDelay
            } catch (SocketException ignored) {}

            //inicio
            String header = "FILE|" + nombre + "|" + tamanio;
            DatagramPacket dpHeader = MiDatagrama.crearDataG(ipServidor, PORT, header);
            canal.send(dpHeader);

            // mensaje archivo enviado
            try {
                byte[] tmp = new byte[2048];
                DatagramPacket r = new DatagramPacket(tmp, tmp.length);
                canal.receive(r);
                String ans = new String(r.getData(), 0, r.getLength(), StandardCharsets.UTF_8);

                if (ans.startsWith("OK END")) {
                    logServer("Servidor: Archivo recibido correctamente");
                } else if (ans.startsWith("ERROR END")) {
                    logServer("Servidor: Error al recibir el archivo");
                } else {
                    logServer(ans);
                }
            } catch (SocketTimeoutException ignored) {}

            // datos
            byte[] marco = new byte[TamanioDatagrama];
            marco[0] = 'D'; marco[1] = 'A'; marco[2] = 'T'; marco[3] = 'A'; marco[4] = ':'; // "DATA:"

            long enviados = 0L;
            int leidos;
            while ((leidos = fis.read(marco, overhead, maxPayload)) != -1) {
                DatagramPacket dpData = new DatagramPacket(marco, overhead + leidos, InetAddress.getByName(ipServidor), PORT);
                canal.send(dpData);
                enviados += leidos;

                int pct = (tamanio > 0) ? (int) Math.min(100, (enviados * 100) / tamanio) : 0;
                actualizarProgresoCliente(pct, false);
            }

            // fin
            String fin = "END|" + nombre;
            DatagramPacket dpFin = MiDatagrama.crearDataG(ipServidor, PORT, fin);
            canal.send(dpFin);


            try {
                byte[] tmp = new byte[2048];
                DatagramPacket r = new DatagramPacket(tmp, tmp.length);
                canal.receive(r);
                String ans = new String(r.getData(), 0, r.getLength(), StandardCharsets.UTF_8);

                if (ans.startsWith("OK END")) {
                    logServer("Archivo recibido correctamente en el servidor");
                } else if (ans.startsWith("ERROR END")) {
                    logServer("Error al recibir el archivo en el servidor");
                } else {
                    logServer(ans);
                }
            } catch (SocketTimeoutException ignored) {}



            actualizarProgresoCliente(100, true);
            endProgress(true, "Archivo enviado (" + tamanio + " bytes)");

            // Pausa y reinicio
            new javax.swing.Timer(700, ev -> {
                if (smoothCurrent >= 100) {
                    ((javax.swing.Timer) ev.getSource()).stop();
                    progressActive = false; // detener animación
                    // Desvanecer barra hacia 0
                    new javax.swing.Timer(16, t -> {
                        int v = progresoCli.getValue();
                        if (v <= 0) {
                            ((javax.swing.Timer) t.getSource()).stop();
                            progresoCli.setValue(0);
                            ultimoPctMostrado = -1;
                        } else {
                            progresoCli.setValue(Math.max(0, v - 5));
                        }
                    }).start();
                }
            }).start();

        } catch (SocketException ex) {
            Logger.getLogger(PrincipalCli.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Error de socket: " + ex.getMessage());
            endProgress(false, "Error de socket");


            progressActive = false;
            new javax.swing.Timer(300, ev -> {
                ((javax.swing.Timer) ev.getSource()).stop();
                progresoCli.setValue(0);
                ultimoPctMostrado = -1;
            }).start();

        } catch (IOException ex) {
            Logger.getLogger(PrincipalCli.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, "Error E/S: " + ex.getMessage());
            endProgress(false, "Error de E/S");


            progressActive = false;
            new javax.swing.Timer(300, ev -> {
                ((javax.swing.Timer) ev.getSource()).stop();
                progresoCli.setValue(0);
                ultimoPctMostrado = -1;
            }).start();
        }
    }
}
