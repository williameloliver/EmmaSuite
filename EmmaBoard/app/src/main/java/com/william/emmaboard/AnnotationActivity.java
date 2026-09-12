package com.william.emmaboard;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class AnnotationActivity extends Activity {
    private AnnotationView drawing;
    private EmmaDb db;
    private CaptureItem current;

    private File annoFile;
    private File mergedFile;

    private float currentWidth = 4f;

    public void onCreate(Bundle b) {
        super.onCreate(b);

        db = new EmmaDb(this);

        long id = getIntent().getLongExtra("id", -1L);
        current = db.getCapture(id);

        if (current == null) {
            Toast.makeText(this, "No encontre la captura.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(current.title);

        annoFile = new File(current.path + ".anno");
        mergedFile = new File(current.path + ".annotated.png");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap base = BitmapFactory.decodeFile(current.path, options);

        if (base == null) {
            Toast.makeText(this, "No pude abrir la captura.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        drawing = new AnnotationView(
            this,
            base,
            AnnotationStore.load(annoFile)
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        root.addView(toolRow(), new LinearLayout.LayoutParams(-1, -2));
        root.addView(navRow(), new LinearLayout.LayoutParams(-1, -2));
        root.addView(drawing, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private View toolRow() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);

        Button pen = button("Lapiz");
        Button red = button("Rojo");
        Button black = button("Negro");

        Button thin = button("Fino");
        Button medium = button("Medio");
        Button thick = button("Grueso");

        Button rect = button("Rect");
        Button circle = button("Circulo");
        Button arrow = button("Flecha");
        Button text = button("Texto");
        Button eraser = button("Goma");
        Button pan = button("Mover");

        Button undo = button("Undo");
        Button redo = button("Redo");
        Button reset = button("Restablecer");

        pen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.setTool(AnnotationView.TOOL_PEN);
            }
        });

        red.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.setPenColor(Color.RED);
                drawing.setWidth(currentWidth);
            }
        });

        black.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.setPenColor(Color.BLACK);
                drawing.setWidth(currentWidth);
            }
        });

        thin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentWidth = 2f;
                drawing.setWidth(currentWidth);
            }
        });

        medium.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentWidth = 5f;
                drawing.setWidth(currentWidth);
            }
        });

        thick.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentWidth = 10f;
                drawing.setWidth(currentWidth);
            }
        });

        rect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.setTool(AnnotationView.TOOL_RECT);
            }
        });

        circle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.setTool(AnnotationView.TOOL_CIRCLE);
            }
        });

        arrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.setTool(AnnotationView.TOOL_ARROW);
            }
        });

        text.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showTextDialog();
            }
        });

        eraser.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.setTool(AnnotationView.TOOL_ERASER);
            }
        });

        pan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.setTool(AnnotationView.TOOL_PAN);
            }
        });

        undo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.undo();
            }
        });

        redo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.redo();
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawing.clearAll();
                try { if (annoFile.exists()) annoFile.delete(); } catch (Throwable ignored) {}
                Toast.makeText(
                    AnnotationActivity.this,
                    "Imagen restablecida",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        bar.addView(pen);
        bar.addView(red);
        bar.addView(black);
        bar.addView(thin);
        bar.addView(medium);
        bar.addView(thick);
        bar.addView(rect);
        bar.addView(circle);
        bar.addView(arrow);
        bar.addView(text);
        bar.addView(eraser);
        bar.addView(pan);
        bar.addView(undo);
        bar.addView(redo);
        bar.addView(reset);

        scroll.addView(bar);
        return scroll;
    }

    private View navRow() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);

        Button zoomOut = button("Zoom -");
        Button zoomIn = button("Zoom +");
        Button fit = button("Ajustar");

        Button left = button("<");
        Button right = button(">");
        Button up = button("^");
        Button down = button("v");

        Button prev = button("Img <");
        Button next = button("Img >");

        Button rename = button("Renombrar");
        Button back = button("Nueva captura");
        Button export = button("Exportar PNG");
        Button sendPc = button("Enviar PC");
        Button save = button("Guardar");

        zoomOut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { drawing.zoomOut(); }
        });

        zoomIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { drawing.zoomIn(); }
        });

        fit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { drawing.fit(); }
        });

        left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { drawing.panBy(-45f, 0f); }
        });

        right.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { drawing.panBy(45f, 0f); }
        });

        up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { drawing.panBy(0f, -45f); }
        });

        down.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { drawing.panBy(0f, 45f); }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { moveImage(-1); }
        });

        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { moveImage(1); }
        });

        rename.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { renameDialog(); }
        });

        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveAnnotationsOnly();
                finish();
            }
        });

        export.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { exportPng(); }
        });

        sendPc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { sendBackToPc(); }
        });

        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveAll(); }
        });

        bar.addView(zoomOut);
        bar.addView(zoomIn);
        bar.addView(fit);

        bar.addView(left);
        bar.addView(right);
        bar.addView(up);
        bar.addView(down);

        bar.addView(prev);
        bar.addView(next);

        bar.addView(rename);
        bar.addView(back);
        bar.addView(export);
        bar.addView(sendPc);
        bar.addView(save);

        scroll.addView(bar);
        return scroll;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setMinHeight(48);
        return b;
    }

    private void showTextDialog() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 6, 10, 6);

        final EditText text = new EditText(this);
        text.setHint("Texto");

        final EditText size = new EditText(this);
        size.setHint("Tamano");
        size.setSingleLine(true);
        size.setText("26");
        size.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        box.addView(text);
        box.addView(size);

        new AlertDialog.Builder(this)
            .setTitle("Insertar texto")
            .setView(box)
            .setPositiveButton("Colocar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String value = text.getText().toString();
                    if (value.length() == 0) return;

                    float textSize = 26f;

                    try {
                        textSize = Float.parseFloat(size.getText().toString());
                    } catch (Throwable ignored) {}

                    drawing.setTextTool(value, textSize);

                    Toast.makeText(
                        AnnotationActivity.this,
                        "Toca la imagen donde quieres colocar el texto.",
                        Toast.LENGTH_LONG
                    ).show();
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void renameDialog() {
        final EditText input = new EditText(this);
        input.setText(current.title);
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
            .setTitle("Renombrar imagen")
            .setView(input)
            .setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String title = input.getText().toString().trim();

                    if (title.length() == 0) return;

                    db.renameCapture(current.id, title);
                    current.title = title;
                    setTitle(title);
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void moveImage(int delta) {
        saveAnnotationsOnly();

        ArrayList<CaptureItem> items = db.listCaptures(current.notebook);
        int index = -1;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == current.id) {
                index = i;
                break;
            }
        }

        if (index < 0) return;

        int target = index + delta;

        if (target < 0 || target >= items.size()) {
            Toast.makeText(this, "No hay otra imagen.", Toast.LENGTH_SHORT).show();
            return;
        }

        CaptureItem item = items.get(target);

        Intent intent = new Intent(this, AnnotationActivity.class);
        intent.putExtra("id", item.id);

        startActivity(intent);
        finish();
    }

    private void saveAnnotationsOnly() {
        if (drawing == null) return;

        try {
            AnnotationStore.save(annoFile, drawing.getMarks());
        } catch (Throwable ignored) {}
    }

    private void saveAll() {
        Bitmap merged = null;

        try {
            AnnotationStore.save(annoFile, drawing.getMarks());

            merged = drawing.renderMerged();

            FileOutputStream out = new FileOutputStream(mergedFile);
            merged.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();

        } catch (Throwable e) {
            Toast.makeText(
                this,
                "Error: " + String.valueOf(e),
                Toast.LENGTH_LONG
            ).show();
        } finally {
            if (merged != null) {
                try { merged.recycle(); } catch (Throwable ignored) {}
            }
        }
    }

    private String safeName(String s) {
        if (s == null || s.length() == 0) return "EmmaBoard";

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (
                (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') ||
                c == '-' ||
                c == '_' ||
                c == ' '
            ) {
                b.append(c);
            } else {
                b.append('_');
            }
        }

        return b.toString().trim();
    }

    private void exportPng() {
        Bitmap merged = null;

        try {
            File base = Environment.getExternalStorageDirectory();
            File dir = new File(base, "EmmaBoard/Exports");

            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("No pude crear " + dir.getAbsolutePath());
            }

            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            File file = new File(
                dir,
                safeName(current.title) + "_" + stamp + ".png"
            );

            merged = drawing.renderMerged();

            FileOutputStream out = new FileOutputStream(file);
            merged.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Toast.makeText(
                this,
                "PNG: " + file.getAbsolutePath(),
                Toast.LENGTH_LONG
            ).show();

        } catch (Throwable e) {
            Toast.makeText(
                this,
                "No pude exportar: " + String.valueOf(e),
                Toast.LENGTH_LONG
            ).show();
        } finally {
            if (merged != null) {
                try { merged.recycle(); } catch (Throwable ignored) {}
            }
        }
    }

    private void sendBackToPc() {
        final CaptureItem fresh = db.getCapture(current.id);

        if (
            fresh == null ||
            fresh.returnHost == null ||
            fresh.returnHost.trim().length() == 0
        ) {
            Toast.makeText(
                this,
                "Esta captura no conoce la IP del PC. Envia una captura nueva desde EmmaBridge v0.3.",
                Toast.LENGTH_LONG
            ).show();
            return;
        }

        final Bitmap merged;

        try {
            merged = drawing.renderMerged();
        } catch (Throwable e) {
            Toast.makeText(this, "No pude preparar la imagen.", Toast.LENGTH_LONG).show();
            return;
        }

        final String token = getSharedPreferences("emma", MODE_PRIVATE)
            .getString("token", "emma1234");

        Toast.makeText(this, "Enviando al PC...", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            public void run() {
                try {
                    ReturnClient.send(
                        fresh.returnHost,
                        fresh.returnPort,
                        token,
                        fresh.title,
                        merged
                    );

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(
                                AnnotationActivity.this,
                                "Enviada al PC",
                                Toast.LENGTH_SHORT
                            ).show();
                        }
                    });

                } catch (final Throwable e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(
                                AnnotationActivity.this,
                                "Error PC: " + String.valueOf(e),
                                Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                } finally {
                    try { merged.recycle(); } catch (Throwable ignored) {}
                }
            }
        }, "emma-return").start();
    }

    protected void onPause() {
        super.onPause();
        saveAnnotationsOnly();
    }

    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}
