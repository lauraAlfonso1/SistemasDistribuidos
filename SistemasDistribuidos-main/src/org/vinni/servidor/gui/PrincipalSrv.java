package org.vinni.servidor.gui;

import org.vinni.dto.MiDatagrama;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
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
public class PrincipalSrv extends JFrame {

    private final int PORT = 12345;

    // === Archivos ===
    private File carpetaDestino = new File("C:\\Users\\natal\\Downloads\\");
    private boolean recibiendoArchivo = false;
    private String nombreArchivo = null;
    private long tamanioArchivo = 0L;
    private long tamanioRecibido = 0L;
    private FileOutputStream flujoArchivo = null;

    private static final int TamanioDatagrama = 1400;

    // === Interfaz ===
    private JButton bIniciar;
    private JLabel jLabel1;
    private JTextPane logPane;
    private JScrollPane jScrollPane1;
    private JProgressBar progresoSrv;
    private int ultimoPctSrv = -1;

    // =formato y estilo===
    private StyledDocument doc;
    private Style stTime, stInfo, stOk, stWarn, stErr, stServer, stClient, stSection, stMono;
    private final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss");

    // — Progreso barra —
    private int progressStartOffset = -1;
    private String progressTitle = null;

    public PrincipalSrv() {
        initComponents();
        initLogStyles();
        logSection("🖥 Servidor listo");
    }

    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new JButton();
        jLabel1 = new JLabel();
        logPane = new JTextPane();
        jScrollPane1 = new JScrollPane();
        progresoSrv = new JProgressBar(0, 100);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(this::bIniciarActionPerformed);
        getContentPane().add(bIniciar);
        bIniciar.setBounds(150, 50, 250, 40);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14));
        jLabel1.setForeground(new java.awt.Color(30, 136, 229));
        jLabel1.setText("SERVIDOR UDP : FERINK");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 220, 17);

        jScrollPane1.setViewportView(logPane);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 150, 500, 120);



        setSize(new java.awt.Dimension(570, 320));
        setLocationRelativeTo(null);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalSrv().setVisible(true));
    }

    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) { iniciar(); }

    // ====estilos =========
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
    private void logOk(String t)      { logLine("✔", t, stOk); }
    private void logWarn(String t)    { logLine("⚠", t, stWarn); }
    private void logErr(String t)     { logLine("✖", t, stErr); }
    private void logServer(String t)  { logLine("🖥", t, stServer); }


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


    private void actualizarProgresoServidor(int pct, boolean forzarLog) {
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;

        if (forzarLog || pct == 100 || ultimoPctSrv < 0 || pct - ultimoPctSrv >= 5) {
            ultimoPctSrv = pct;
            setProgress(pct); // una sola línea en el log
        }
    }

    public void iniciar() {
        if (!carpetaDestino.exists()) carpetaDestino.mkdirs();
        logInfo("Servidor UDP iniciado en el puerto " + PORT);

        new Thread(() -> {
            DatagramPacket dp;
            try (DatagramSocket socketudp = new DatagramSocket(PORT)) {
                try { socketudp.setReceiveBufferSize(512 * 1024); } catch (SocketException ignored) {}
                this.bIniciar.setEnabled(false);

                byte[] buf = new byte[Math.max(8192, TamanioDatagrama + 32)];

                while (true) {
                    dp = new DatagramPacket(buf, buf.length);
                    socketudp.receive(dp);

                    // datos binario
                    if (dp.getLength() >= 5
                            && buf[0] == 'D' && buf[1] == 'A' && buf[2] == 'T'
                            && buf[3] == 'A' && buf[4] == ':') {

                        if (recibiendoArchivo && flujoArchivo != null) {
                            int offset = 5;
                            int len = dp.getLength() - offset;
                            flujoArchivo.write(buf, offset, len);
                            tamanioRecibido += len;

                            int pct = (tamanioArchivo > 0)
                                    ? (int) Math.min(100, (tamanioRecibido * 100) / tamanioArchivo)
                                    : 0;
                            actualizarProgresoServidor(pct, false);

                            if (tamanioRecibido > tamanioArchivo) {
                                logWarn("Advertencia: recibido más de lo anunciado.");
                            }
                        } else {
                            logWarn("Llegó DATA pero no hay recepción activa.");
                        }
                        continue;
                    }

                    // Texto
                    String texto = new String(dp.getData(), 0, dp.getLength(), StandardCharsets.UTF_8).trim();

                    if (texto.startsWith("FILE|")) {
                        String[] partes = texto.split("\\|", 3);
                        if (partes.length == 3) {
                            nombreArchivo = partes[1];
                            try { tamanioArchivo = Long.parseLong(partes[2]); } catch (NumberFormatException e) { tamanioArchivo = 0L; }

                            try {
                                File destino = new File(carpetaDestino, nombreArchivo);
                                flujoArchivo = new FileOutputStream(destino);
                                recibiendoArchivo = true;
                                tamanioRecibido = 0L;
                                ultimoPctSrv = -1;

                                logSection("📥 Recepción de archivo");
                                logInfo("Nombre: " + nombreArchivo);
                                logInfo("Tamaño: " + tamanioArchivo + " bytes");
                                beginProgress("Recibiendo " + nombreArchivo);

                                DatagramPacket ok = MiDatagrama.crearDataG(dp.getAddress().getHostAddress(), dp.getPort(),
                                        "OK FILE " + nombreArchivo);
                                socketudp.send(ok);
                            } catch (IOException ex) {
                                logErr("Error al preparar archivo: " + ex.getMessage());
                                recibiendoArchivo = false;
                                if (flujoArchivo != null) try { flujoArchivo.close(); } catch (IOException ignored) {}
                                flujoArchivo = null;
                                actualizarProgresoServidor(0, true);
                            }
                        } else {
                            logErr("HEADER FILE mal formado.");
                        }

                    } else if (texto.startsWith("END|")) {
                        String[] partes = texto.split("\\|", 2);
                        String nombreFin = (partes.length == 2) ? partes[1] : null;

                        if (flujoArchivo != null) {
                            try { flujoArchivo.flush(); flujoArchivo.close(); } catch (IOException ignored) {}
                        }
                        flujoArchivo = null;

                        boolean okSize = (tamanioRecibido == tamanioArchivo);
                        actualizarProgresoServidor(100, true);

                        endProgress(okSize, okSize ? "OK" : "Tamaño no coincide");
                        if (okSize) {
                            logOk("Guardado en: " + new File(carpetaDestino, nombreFin).getAbsolutePath());
                        }


                        recibiendoArchivo = false;
                        nombreArchivo = null;
                        tamanioArchivo = 0L;
                        tamanioRecibido = 0L;

                        progresoSrv.setValue(0);
                        ultimoPctSrv = -1;

                        String resp = okSize ? "OK END " + nombreFin : "ERROR END " + nombreFin;
                        DatagramPacket ack = MiDatagrama.crearDataG(dp.getAddress().getHostAddress(), dp.getPort(), resp);
                        socketudp.send(ack);

                    } else {
                        logServer("Mensaje: " + texto);
                        DatagramPacket mensajeServ = MiDatagrama.crearDataG(
                                dp.getAddress().getHostAddress(),
                                dp.getPort(),
                                "Mensaje recibido en el servidor");
                        socketudp.send(mensajeServ);
                    }
                }

            } catch (SocketException ex) {
                Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
                logErr("Socket: " + ex.getMessage());
            } catch (IOException ex) {
                Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
                logErr("E/S: " + ex.getMessage());
            }
        }).start();
    }
}
