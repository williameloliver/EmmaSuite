package com.william.emmabridge;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class BridgeFrame extends JFrame implements ReturnServer.Listener {
    private final JTextField host = new JTextField();
    private final JTextField port = new JTextField();
    private final JTextField token = new JTextField();
    private final JTextField notebook = new JTextField();
    private final JTextField maxWidth = new JTextField();

    private final JLabel status = new JLabel("Listo.");
    private final JLabel returnStatus = new JLabel();

    private ReturnServer returnServer;

    public BridgeFrame() {
        super("EmmaBridge v0.3");

        Config c = Config.load();

        host.setText(c.host);
        port.setText(String.valueOf(c.port));
        token.setText(c.token);
        notebook.setText(c.notebook);
        maxWidth.setText(String.valueOf(c.maxWidth));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        form.add(new JLabel("IP tablet:"));
        form.add(host);

        form.add(new JLabel("Puerto tablet:"));
        form.add(port);

        form.add(new JLabel("Token:"));
        form.add(token);

        form.add(new JLabel("Cuaderno:"));
        form.add(notebook);

        form.add(new JLabel("Ancho maximo:"));
        form.add(maxWidth);

        JButton save = new JButton("Guardar");
        JButton test = new JButton("Probar conexion");
        JButton capture = new JButton("Capturar y enviar");

        save.addActionListener(e -> saveConfig());
        test.addActionListener(e -> testConnection());
        capture.addActionListener(e -> capture());

        JPanel buttons = new JPanel();
        buttons.add(save);
        buttons.add(test);
        buttons.add(capture);

        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        body.add(form, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new GridLayout(0, 1));
        bottom.add(status);
        bottom.add(returnStatus);

        add(body, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(520, 330);
        setLocationRelativeTo(null);

        startReturnServer();
    }

    private void startReturnServer() {
        Config c = Config.load();

        returnServer = new ReturnServer(
            c.returnPort,
            c.token,
            this
        );

        returnServer.start();

        returnStatus.setText(
            "Retorno tablet: puerto " + c.returnPort +
            "  |  " + returnServer.folder().getAbsolutePath()
        );
    }

    private Config current() throws Exception {
        Config c = new Config();

        c.host = host.getText().trim();
        c.port = Integer.parseInt(port.getText().trim());
        c.token = token.getText();
        c.notebook = notebook.getText().trim().length() == 0
            ? "Inbox"
            : notebook.getText().trim();

        c.maxWidth = Integer.parseInt(maxWidth.getText().trim());

        if (c.host.length() == 0) {
            throw new Exception("Falta la IP");
        }

        return c;
    }

    private void saveConfig() {
        try {
            Config c = current();
            c.save();

            if (returnServer != null) returnServer.stop();

            returnServer = new ReturnServer(
                c.returnPort,
                c.token,
                this
            );

            returnServer.start();

            status.setText("Configuracion guardada.");
            returnStatus.setText(
                "Retorno tablet: puerto " + c.returnPort +
                "  |  " + returnServer.folder().getAbsolutePath()
            );

        } catch (Exception e) {
            showError(e);
        }
    }

    private void testConnection() {
        try {
            final Config c = current();
            c.save();

            status.setText("Probando...");

            new Thread(() -> {
                try {
                    final String r = Uploader.ping(c);

                    SwingUtilities.invokeLater(
                        () -> status.setText("Tablet: " + r)
                    );

                } catch (final Exception ex) {
                    SwingUtilities.invokeLater(
                        () -> status.setText("Error: " + ex.getMessage())
                    );
                }
            }, "emma-ping").start();

        } catch (Exception e) {
            showError(e);
        }
    }

    private void capture() {
        try {
            final Config c = current();
            c.save();

            setState(Frame.ICONIFIED);

            ScreenSelector.start(new ScreenSelector.Listener() {
                public void selected(BufferedImage image) {
                    final BufferedImage small =
                        ImageTools.resizeToMaxWidth(
                            image,
                            c.maxWidth
                        );

                    final String title =
                        "Captura " +
                        new SimpleDateFormat(
                            "yyyy-MM-dd HH-mm-ss"
                        ).format(new Date());

                    new Thread(() -> {
                        try {
                            Uploader.send(
                                c,
                                small,
                                title
                            );

                            SwingUtilities.invokeLater(() -> {
                                setState(Frame.NORMAL);
                                toFront();
                                status.setText(
                                    "Enviado a " +
                                    c.notebook +
                                    " ✓"
                                );
                            });

                        } catch (final Exception ex) {
                            SwingUtilities.invokeLater(() -> {
                                setState(Frame.NORMAL);
                                toFront();
                                status.setText(
                                    "Error: " +
                                    ex.getMessage()
                                );
                            });
                        }
                    }, "emma-upload").start();
                }

                public void cancelled() {
                    SwingUtilities.invokeLater(
                        () -> setState(Frame.NORMAL)
                    );
                }
            });

        } catch (Exception e) {
            showError(e);
        }
    }

    public void saved(final File file) {
        SwingUtilities.invokeLater(() -> {
            returnStatus.setText(
                "Recibida desde tablet: " +
                file.getAbsolutePath()
            );
        });
    }

    public void error(final String message) {
        SwingUtilities.invokeLater(() -> {
            returnStatus.setText(
                "Error receptor PC: " + message
            );
        });
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(
            this,
            e.getMessage(),
            "EmmaBridge",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
