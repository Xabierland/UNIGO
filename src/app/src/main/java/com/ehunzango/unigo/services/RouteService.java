package com.ehunzango.unigo.services;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ehunzango.unigo.router.RouteFinder;
import com.ehunzango.unigo.router.adapters.IDataAdapter;
import com.ehunzango.unigo.router.adapters.NETEXAdapter;
import com.ehunzango.unigo.router.adapters.SHPAdapter;
import com.ehunzango.unigo.router.entities.Line;
import com.ehunzango.unigo.router.utils.FetchUtil;
import com.ehunzango.unigo.router.utils.ZipUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar la descarga y descompresión de datos de rutas
 */
public class RouteService {

    private static final String TAG = "RouteService";
    private static final String ZIP_DIR = "data";
    private static final String TUVISA_UNZIP_DIR = "data/tuvisa";
    private static final String BIDEGORRI_UNZIP_DIR = "data/viasciclistas23";
    private static final String TUVISA_ZIP_FILENAME = "netex_tuvisa.zip";
    private static final String TUVISA_URL = "https://02-pro-e3525cfb1b3d99109c5220a2b24bcb30-inet.s3.itbatera.euskadi.eus/transport/moveuskadi/tuvisa/netex_tuvisa.zip";
    private static final String BIDEGORRI_ZIP_FILENAME = "viasciclistas23.zip";
    private static final String BIDEGORRI_URL = "https://www.vitoria-gasteiz.org/docs/j34/catalogo/00/80/viasciclistas23.zip";

    private static RouteService instance;
    private final List<Line> lines = new ArrayList<>();
    private final List<Line> linesBus = new ArrayList<>();
    private boolean dataLoaded = false;

    // Interfaz para callbacks
    public interface RouteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    // Constructor privado para Singleton
    private RouteService() {
    }

    // Método para obtener la instancia única
    public static RouteService getInstance() {
        if (instance == null) {
            instance = new RouteService();
        }
        return instance;
    }

    /**
     * Inicializa el servicio de rutas, comprueba si existen los archivos,
     * los descarga si es necesario y los descomprime.
     *
     * @param context Contexto de la aplicación
     * @param callback Callback para notificar el resultado
     */
    public void initialize(Context context, RouteCallback callback) {
        // Ejecutar en un hilo separado para no bloquear el hilo principal
        new Thread(() -> {
            try {
                Log.d(TAG, "Inicializando servicio de rutas");
                
                // Definir directorio para los zips
                File zipDir = new File(context.getFilesDir(), ZIP_DIR);
                if (!zipDir.exists() && !zipDir.mkdirs()) {
                    throw new IOException("No se pudo crear el directorio: " + zipDir.getAbsolutePath());
                }
                
                // Procesar archivo Tuvisa
                File tuvisaZipFile = new File(zipDir, TUVISA_ZIP_FILENAME);
                File tuvisaUnzipDir = new File(context.getFilesDir(), TUVISA_UNZIP_DIR);
                
                // Comprobar si necesitamos descargar y descomprimir Tuvisa
                boolean needDownloadTuvisa = !tuvisaZipFile.exists();
                boolean needUnzipTuvisa = !tuvisaUnzipDir.exists() || tuvisaUnzipDir.list() == null || tuvisaUnzipDir.list().length == 0;
                
                // Descargar Tuvisa si es necesario
                if (needDownloadTuvisa) {
                    Log.d(TAG, "Descargando archivo Tuvisa: " + TUVISA_URL);
                    FetchUtil.downloadFile(TUVISA_URL, tuvisaZipFile);
                    Log.d(TAG, "Descarga completada. Tamaño del archivo: " + tuvisaZipFile.length() + " bytes");
                    needUnzipTuvisa = true; // Si descargamos, siempre descomprimimos
                }
                
                // Descomprimir Tuvisa si es necesario
                if (needUnzipTuvisa) {
                    Log.d(TAG, "Descomprimiendo archivo Tuvisa");
                    if (!tuvisaUnzipDir.exists() && !tuvisaUnzipDir.mkdirs()) {
                        throw new IOException("No se pudo crear el directorio: " + tuvisaUnzipDir.getAbsolutePath());
                    }
                    ZipUtils.unzip(new FileInputStream(tuvisaZipFile), tuvisaUnzipDir);
                }
                
                // Procesar archivo Bidegorri
                File bidegorriZipFile = new File(zipDir, BIDEGORRI_ZIP_FILENAME);
                File bidegorriUnzipDir = new File(context.getFilesDir(), BIDEGORRI_UNZIP_DIR);
                
                // Comprobar si necesitamos descargar y descomprimir Bidegorri
                boolean needDownloadBidegorri = !bidegorriZipFile.exists();
                boolean needUnzipBidegorri = !bidegorriUnzipDir.exists() || bidegorriUnzipDir.list() == null || bidegorriUnzipDir.list().length == 0;
                
                // Descargar Bidegorri si es necesario
                if (needDownloadBidegorri) {
                    Log.d(TAG, "Descargando archivo Bidegorri: " + BIDEGORRI_URL);
                    FetchUtil.downloadFile(BIDEGORRI_URL, bidegorriZipFile);
                    Log.d(TAG, "Descarga completada. Tamaño del archivo: " + bidegorriZipFile.length() + " bytes");
                    needUnzipBidegorri = true; // Si descargamos, siempre descomprimimos
                }
                
                // Descomprimir Bidegorri si es necesario
                if (needUnzipBidegorri) {
                    Log.d(TAG, "Descomprimiendo archivo Bidegorri");
                    if (!bidegorriUnzipDir.exists() && !bidegorriUnzipDir.mkdirs()) {
                        throw new IOException("No se pudo crear el directorio: " + bidegorriUnzipDir.getAbsolutePath());
                    }
                    ZipUtils.unzip(new FileInputStream(bidegorriZipFile), bidegorriUnzipDir);
                }

                IDataAdapter loader = new SHPAdapter();
                // Marcar como completado con éxito
                dataLoaded = false;
                String fullPath = new File(context.getFilesDir(), RouteService.BIDEGORRI_UNZIP_DIR).getAbsolutePath();
                dataLoaded = loader.load(fullPath, this.lines);

                Log.d(TAG, "=====================================");
                Log.d(TAG, "DATA LOADED!!!!!!!!!!!!!!!!!!!!!!!!!!");
                Log.d(TAG, String.format("size: %d", this.lines.size()));
                Log.d(TAG, "=====================================");


                NETEXAdapter adapter = new NETEXAdapter();
                // Marcar como completado con éxito
                dataLoaded = false;
                fullPath = new File(context.getFilesDir(), RouteService.TUVISA_UNZIP_DIR).getAbsolutePath();
                dataLoaded = adapter.load(fullPath, this.linesBus);

                Log.d(TAG, "=====================================");
                Log.d(TAG, "DATA LOADED!!!!!!!!!!!!!!!!!!!!!!!!!!");
                Log.d(TAG, String.format("size: %d", this.linesBus.size()));
                Log.d(TAG, "=====================================");


                // Notificar éxito en el hilo principal
                if (callback != null) {
                    Handler mainHandler = new Handler(context.getMainLooper());
                    mainHandler.post(callback::onSuccess);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error al inicializar servicio de rutas", e);
                
                // Notificar error en el hilo principal
                if (callback != null) {
                    final String errorMessage = e.getMessage();
                    Handler mainHandler = new Handler(context.getMainLooper());
                    mainHandler.post(() -> callback.onError(errorMessage));
                }
            }
        }).start();
    }
    
    /**
     * Obtiene la lista de líneas cargadas
     *
     * @return Lista de líneas
     */
    public List<Line> getLines() {
        return lines;
    }

    public List<Line> getBusLines()
    {
        return linesBus;
    }



    
    /**
     * Comprueba si los datos están cargados
     *
     * @return true si los datos están cargados, false en caso contrario
     */
    public boolean isDataLoaded() {
        return dataLoaded;
    }
}