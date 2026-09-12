package com.william.emmaboard;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public final class MainActivity extends Activity implements CaptureServer.Listener {
    private EmmaDb db;
    private CaptureServer server;

    private TextView status;
    private EditText tokenField;
    private Spinner notebookSpinner;
    private CaptureAdapter captureAdapter;
    private ListView list;

    private final android.os.Handler handler = new android.os.Handler();

    public void onCreate(Bundle b) {
        super.onCreate(b);

        setTitle("EmmaBoard");
        db = new EmmaDb(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(8, 8, 8, 8);

        status = new TextView(this);
        status.setTextSize(15f);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout tokenRow = new LinearLayout(this);
        tokenRow.setOrientation(LinearLayout.HORIZONTAL);

        tokenField = new EditText(this);
        tokenField.setSingleLine(true);
        tokenField.setText(
            getSharedPreferences("emma", MODE_PRIVATE)
                .getString("token", "emma1234")
        );

        Button apply = new Button(this);
        apply.setText("Aplicar");

        tokenRow.addView(tokenField, new LinearLayout.LayoutParams(0, -2, 1f));
        tokenRow.addView(apply, new LinearLayout.LayoutParams(-2, -2));

        root.addView(tokenRow);

        LinearLayout notebooks = new LinearLayout(this);
        notebooks.setOrientation(LinearLayout.HORIZONTAL);

        notebookSpinner = new Spinner(this);

        Button newNotebook = new Button(this);
        newNotebook.setText("+ Cuaderno");

        notebooks.addView(
            notebookSpinner,
            new LinearLayout.LayoutParams(0, -2, 1f)
        );
        notebooks.addView(newNotebook, new LinearLayout.LayoutParams(-2, -2));

        root.addView(notebooks);

        list = new ListView(this);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView help = new TextView(this);
        help.setText(
            "Toca una miniatura para abrir/anotar. Mantener pulsado: mover de cuaderno."
        );
        root.addView(help, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);

        apply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String token = tokenField.getText().toString();

                getSharedPreferences("emma", MODE_PRIVATE)
                    .edit()
                    .putString("token", token)
                    .commit();

                restartServer();
            }
        });

        newNotebook.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createNotebookDialog();
            }
        });

        notebookSpinner.setOnItemSelectedListener(
            new android.widget.AdapterView.OnItemSelectedListener() {
                public void onItemSelected(
                    android.widget.AdapterView parent,
                    View view,
                    int position,
                    long id
                ) {
                    refreshCaptures();
                }

                public void onNothingSelected(android.widget.AdapterView parent) {}
            }
        );

        list.setOnItemClickListener(
            new android.widget.AdapterView.OnItemClickListener() {
                public void onItemClick(
                    android.widget.AdapterView parent,
                    View view,
                    int position,
                    long id
                ) {
                    CaptureItem item = captureAdapter.itemAt(position);

                    Intent i = new Intent(
                        MainActivity.this,
                        AnnotationActivity.class
                    );

                    i.putExtra("id", item.id);
                    startActivity(i);
                }
            }
        );

        list.setOnItemLongClickListener(
            new android.widget.AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(
                    android.widget.AdapterView parent,
                    View view,
                    int position,
                    long id
                ) {
                    CaptureItem item = captureAdapter.itemAt(position);
                    moveDialog(item);
                    return true;
                }
            }
        );

        refreshNotebooks();
        refreshCaptures();
        restartServer();
    }

    private synchronized void restartServer() {
        if (server != null) {
            server.stopAndWait();
            server = null;
        }

        String token = tokenField.getText().toString();

        server = new CaptureServer(
            this,
            db,
            8765,
            token,
            this
        );

        server.start();

        status.setText(
            "Escuchando: http://" + NetUtil.localIp() +
            ":8765   Token: " + token
        );
    }

    private void refreshNotebooks() {
        try {
            ArrayList<String> names = db.listNotebooks();

            ArrayAdapter<String> a = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                names
            );

            a.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            );

            notebookSpinner.setAdapter(a);

        } catch (Throwable e) {
            status.setText("Error mostrando cuadernos: " + String.valueOf(e));
        }
    }

    private void refreshCaptures() {
        try {
            Object selectedItem = notebookSpinner == null
                ? null
                : notebookSpinner.getSelectedItem();

            String selected = selectedItem == null
                ? "Todos"
                : selectedItem.toString();

            ArrayList<CaptureItem> items = db.listCaptures(selected);

            captureAdapter = new CaptureAdapter(this, items);
            list.setAdapter(captureAdapter);

        } catch (Throwable e) {
            status.setText("Error mostrando capturas: " + String.valueOf(e));
        }
    }

    private void createNotebookDialog() {
        final EditText input = new EditText(this);

        new AlertDialog.Builder(this)
            .setTitle("Nuevo cuaderno")
            .setView(input)
            .setPositiveButton(
                "Crear",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String n = input.getText().toString().trim();

                        if (n.length() > 0) {
                            db.ensureNotebook(n);
                            refreshNotebooks();
                        }
                    }
                }
            )
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void moveDialog(final CaptureItem item) {
        final ArrayList<String> all = db.listNotebooks();

        if (all.size() > 0 && "Todos".equals(all.get(0))) {
            all.remove(0);
        }

        final String[] names = all.toArray(new String[all.size()]);

        new AlertDialog.Builder(this)
            .setTitle("Mover captura")
            .setItems(
                names,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        db.moveCapture(item.id, names[which]);
                        refreshCaptures();
                    }
                }
            )
            .show();
    }

    public void onCaptureReceived(final String notebook, final String title) {
        handler.post(new Runnable() {
            public void run() {
                try {
                    refreshCaptures();

                    status.setText(
                        "Recibida: " + title +
                        " [" + notebook + "]  |  http://" +
                        NetUtil.localIp() + ":8765"
                    );

                    Toast.makeText(
                        MainActivity.this,
                        "Captura recibida",
                        Toast.LENGTH_SHORT
                    ).show();

                } catch (Throwable e) {
                    status.setText(
                        "Recibida, pero fallo al refrescar: " + String.valueOf(e)
                    );
                }
            }
        });
    }

    public void onServerError(final String message) {
        handler.post(new Runnable() {
            public void run() {
                status.setText("Servidor detenido: " + message);
            }
        });
    }

    protected void onResume() {
        super.onResume();

        if (db != null && list != null) {
            refreshCaptures();
        }
    }

    protected void onDestroy() {
        if (server != null) server.stopAndWait();
        if (db != null) db.close();
        super.onDestroy();
    }
}
