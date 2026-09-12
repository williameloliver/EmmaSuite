# Emma Suite v0.3 🐾

Versión pen-friendly para Master-G / MID con Android 2.1.

## EmmaBoard v0.3

Incluye:

- Zoom `+` / `-`
- Botón **Mover** para arrastrar la imagen con stylus
- Flechas `< > ^ v` para desplazar la imagen
- Botón **Ajustar** para volver a encajar la captura
- Grosor **Fino / Medio / Grueso**
- Lápiz rojo y negro
- Rectángulos
- Círculos
- Flechas
- **Texto sobre la captura**
  - pulsa `Texto`
  - escribe el contenido y tamaño
  - luego toca la imagen donde quieres colocarlo
- Goma
- Undo / Redo
- **Restablecer** borra todas las anotaciones de una vez
- **Nueva captura** vuelve al cuaderno
- Miniaturas JPG livianas (~180x110)
- `Img <` / `Img >` para recorrer imágenes del mismo cuaderno
- Renombrar imágenes
- Exportar PNG anotado
- Enviar PNG anotado de vuelta al PC
- Guardado vectorial de anotaciones en `.anno`
- Compatibilidad de lectura con `.anno` anteriores

## EmmaBridge v0.3

Además de capturar y enviar, escucha el puerto `8766` para recibir imágenes anotadas desde la tablet.

Las imágenes devueltas se guardan en:

`~/EmmaBoard_Recibidas`

En Windows normalmente:

`C:\Users\TU_USUARIO\EmmaBoard_Recibidas`

La primera vez Windows puede pedir permiso de Firewall para Java. Permite acceso en la red privada para que funcione **Enviar PC**.

## Puertos

- PC → tablet: `8765`
- tablet → PC: `8766`

Token inicial:

`emma1234`

## Compilación

El APK sigue usando:

`javac → dx → aapt → zipalign → jarsigner`

Sin AndroidX, sin Google Play Services y con `minSdkVersion=7`.

Sube el ZIP a la raíz de un repo y ejecuta:

```bash
git add .
git commit -m "Emma Suite v0.3 pen plus"
git push
```

Artifacts esperados:

- `EmmaBridge-v0.3`
- `EmmaBoard-v0.3-legacy`

El segundo contiene `app-debug.apk`.
