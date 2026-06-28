# Cambios realizados

## Problema

Al eliminar una entrada, los archivos de fotos y videos no se borraban del disco.
Las rutas se guardaban como content URIs (`content://com.example...`) o file URIs
(`file:///...`), pero `borrarArchivos()` en `DiarioViewModel` intentaba borrar con
`File(uriString)`, lo que nunca resolvía a un archivo real.

El audio sí se borraba porque su ruta ya se guardaba como path absoluto del
sistema de archivos.

---

## Solución

Guardar la **ruta real del archivo** (`absolutePath`) en la base de datos en
lugar del content URI o file URI. Glide y VideoView aceptan rutas de archivo
sin necesidad de parsearlas como URI.

---

## Archivos modificados

### 1. `app/src/main/java/.../ui/NuevaEntradaActivity.kt`

- Se agregaron las variables `rutaFoto: String?` y `rutaVideo: String?`
  para almacenar la ruta absoluta del archivo físico.

- **`onFotoClick()`**: ahora crea el `File` directamente, guarda
  `archivo.absolutePath` en `rutaFoto`, genera el content URI con
  `FileProvider` solo para el intent de la cámara.

- **`onVideoClick()`**: mismo cambio que `onFotoClick()`.

- **`galeriaLauncher`**: al importar una foto, guarda `copia.absolutePath` en
  `rutaFoto` en vez de `Uri.fromFile(copia).toString()`.

- **`onGuardarClick()`**: guarda `rutaFoto` y `rutaVideo` en la
  entidad en lugar de `fotoUri?.toString()` y `videoUri?.toString()`.

- Se eliminó el método `crearUri()` (ya no se utiliza).

### 2. `app/src/main/java/.../ui/DetalleActivity.kt`

- **Carga de foto**: `Glide.with(this).load(entrada.rutaFoto)` en vez de
  `.load(Uri.parse(entrada.rutaFoto))`.

- **Carga de video**: `videoView.setVideoPath(entrada.rutaVideo)` en vez de
  `.setVideoURI(Uri.parse(entrada.rutaVideo))`.

- Se eliminó el import `android.net.Uri` (ya no se necesita).

### 3. `app/src/main/java/.../viemodels/DiarioViewModel.kt`

- Sin cambios. El método `borrarArchivos()` ya funcionaba correctamente;
  el problema era que recibía URIs en vez de paths reales.

---

## Efecto

| Antes | Después |
|---|---|
| `rutaFoto` = `"content://com.example.../foto_123.jpg"` | `rutaFoto` = `"/storage/.../Pictures/foto_123.jpg"` |
| `rutaVideo` = `"content://com.example.../video_123.mp4"` | `rutaVideo` = `"/storage/.../Movies/video_123.mp4"` |
| `File(ruta).exists()` → `false` (no borra) | `File(ruta).exists()` → `true` (borra correctamente) |
