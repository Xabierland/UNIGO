package com.ehunzango.unigo.router.adapters;

import android.util.Log;

import com.ehunzango.unigo.router.entities.Line;
import com.ehunzango.unigo.router.entities.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** IDEA:
 *      A polyline is a set of parts that may or may not be connected, but each part is a set of
 *      interconnected points but the shape they form can be arbitrary. So we make the simple stupid
 *      play of:
 *          1. check if they connect inline (part A ends were part B starts).
 *          2. duplicating each part and rotating the dup one (they are bidirectional <:(B )~ )
 *          3. just press play in the RouteFinder and hope for the best (b^u^)b
 *  src: https://www.esri.com/content/dam/esrisites/sitecore-archive/Files/Pdfs/library/whitepapers/pdfs/shapefile.pdf
 */

public class SHPAdapter implements IDataAdapter {
    private static final String TAG = "SHPAdapter";
    private static final int SHP_FILE_CODE = 9994;
    private static final int SHP_SHAPE_TYPE_POLYLINE = 3;

    @Override
    public boolean load(String path, List<Line> lines) {
        try {
            File directory = new File(path);
            if (!directory.exists()) {
                Log.e(TAG, "Dir not found: " + path);
                return false;
            }
            File[] shpFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".shp"));

            if (shpFiles == null || shpFiles.length == 0) {
                Log.e(TAG, "No se encontraron archivos SHP en: " + path);
                return false;
            }

            for (File shpFile : shpFiles) {
                String baseName = shpFile.getName().substring(0, shpFile.getName().length() - 4);
                Log.d(TAG, "Procesando archivo SHP: " + shpFile.getName());

                // Opcionalmente leer archivo DBF para metadatos (nombres, etc.)
                File dbfFile = new File(shpFile.getParentFile(), baseName + ".dbf");
                Map<Integer, Map<String, Object>> attributes = new HashMap<>();
                if (dbfFile.exists()) {
                    try {
                        attributes = readDBF(dbfFile);
                    } catch (Exception e) {
                        Log.w(TAG, "Error al leer archivo DBF: " + e.getMessage());
                    }
                }

                // Leer geometrías del archivo SHP
                List<List<double[]>> polylines = readSHP(shpFile);
                Log.d(TAG, "Se encontraron " + polylines.size() + " polilíneas en " + shpFile.getName());

                // Crear líneas para cada polilínea
                for (int i = 0; i < polylines.size(); i++) {
                    List<double[]> points = polylines.get(i);
                    Map<String, Object> attrs = attributes.getOrDefault(i, new HashMap<>());

                    // Obtener nombre de la línea
                    String lineName = getLineNameFromAttributes(attrs, baseName, i);

                    // Crear nueva línea de tipo BIKE para bidegorri
                    Line line = new Line(lineName, Line.Type.BIKE, new ArrayList<>());

                    // Convertir puntos a nodos
                    List<Node> nodes = new ArrayList<>();
                    List<Float> deltas = new ArrayList<>();
                    
                    // Añadir nodos y calcular deltas (distancias entre nodos)
                    Node prevNode = null;
                    for (double[] point : points) {
                        Node node = new Node(point[1], point[0], line); // Lat, Lon
                        nodes.add(node);

                        if (prevNode != null) {
                            float delta = (float) prevNode.dist(node);
                            deltas.add(delta);
                        }
                        prevNode = node;
                    }

                    // Añadir delta final (distancia infinita para el último nodo)
                    if (!nodes.isEmpty()) {
                        deltas.add(Float.POSITIVE_INFINITY);
                        
                        // Añadir nodos y deltas a la línea
                        line.addNode(nodes, deltas);
                        
                        // Añadir línea a la lista resultado
                        lines.add(line);
                        Log.d(TAG, "Añadida línea: " + lineName + " con " + nodes.size() + " nodos");
                    }
                }
            }

            // HACK: we take all the lines as bidirectional (b^u^)b
            lines.addAll(
                    lines.stream()
                            .map(Line::dup) // or line -> line.dup()
                            .collect(Collectors.toList())
            );

            Log.i(TAG, "Cargadas " + lines.size() + " líneas de archivos SHP");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar archivos SHP: " + e.getMessage(), e);
            return false;
        }
    }

    private String getLineNameFromAttributes(Map<String, Object> attrs, String baseName, int index) {
        // Buscar nombre en campos comunes
        for (String fieldName : new String[]{"NAME", "NOMBRE", "DESCRIP", "DESCRIPTION", "LABEL"}) {
            if (attrs.containsKey(fieldName) && attrs.get(fieldName) != null) {
                return attrs.get(fieldName).toString();
            }
        }
        // Nombre por defecto
        return "Bidegorri " + baseName + "-" + index;
    }

    private List<List<double[]>> readSHP(File shpFile) throws IOException {
        List<List<double[]>> polylines = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(shpFile)) {
            // Leer encabezado (100 bytes)
            byte[] headerBytes = new byte[100];
            if (fis.read(headerBytes) != 100) {
                throw new IOException("Encabezado SHP inválido");
            }

            ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
            headerBuffer.order(ByteOrder.BIG_ENDIAN);

            // Verificar código de archivo
            int fileCode = headerBuffer.getInt(0);
            if (fileCode != SHP_FILE_CODE) {
                throw new IOException("Código de archivo SHP inválido: " + fileCode);
            }

            // Leer longitud del archivo
            headerBuffer.order(ByteOrder.LITTLE_ENDIAN);
            int fileLength = headerBuffer.getInt(24) * 2;
            
            // Verificar tipo de forma
            int shapeType = headerBuffer.getInt(32);
            if (shapeType != SHP_SHAPE_TYPE_POLYLINE) {
                Log.w(TAG, "El archivo SHP contiene tipo " + shapeType + ", se esperaba PolyLine (3)");
            }

            // Leer registros
            int position = 100; // Posición después del encabezado
            byte[] recordHeaderBytes = new byte[8];
            
            while (position < fileLength) {
                // Leer encabezado de registro
                if (fis.read(recordHeaderBytes) != 8) {
                    break;
                }
                position += 8;
                
                ByteBuffer recordHeaderBuffer = ByteBuffer.wrap(recordHeaderBytes);
                recordHeaderBuffer.order(ByteOrder.BIG_ENDIAN);

                // NOTE: we don't check for Record Number (I hope it doesn't matter)
                int contentLength = recordHeaderBuffer.getInt(4) * 2;
                
                // Leer contenido del registro
                byte[] recordContentBytes = new byte[contentLength];
                if (fis.read(recordContentBytes) != contentLength) {
                    break;
                }
                position += contentLength;
                
                ByteBuffer recordContentBuffer = ByteBuffer.wrap(recordContentBytes);
                recordContentBuffer.order(ByteOrder.LITTLE_ENDIAN);
                
                // Verificar tipo de registro
                int recordShapeType = recordContentBuffer.getInt(0);
                if (recordShapeType == SHP_SHAPE_TYPE_POLYLINE) {
                    // Número de partes y puntos
                    int numParts = recordContentBuffer.getInt(36);
                    int numPoints = recordContentBuffer.getInt(40);
                    
                    // Leer índices de inicio de cada parte
                    int[] partIndices = new int[numParts + 1];
                    for (int i = 0; i < numParts; i++) {
                        partIndices[i] = recordContentBuffer.getInt(44 + i * 4);
                    }
                    partIndices[numParts] = numPoints; // Índice fina
                    
                    // Leer puntos
                    double[][] allPoints = new double[numPoints][2];
                    int pointsOffset = 44 + numParts * 4;
                    for (int i = 0; i < numPoints; i++) {
                        int pos = pointsOffset + i * 16;
                        allPoints[i][0] = recordContentBuffer.getDouble(pos); // X (longitud)
                        allPoints[i][1] = recordContentBuffer.getDouble(pos + 8); // Y (latitud)
                    }
                    
                    // Crear una lista para cada parte
                    for (int i = 0; i < numParts; i++) {
                        List<double[]> part = new ArrayList<>();
                        for (int j = partIndices[i]; j < partIndices[i + 1]; j++) {
                            part.add(allPoints[j]);
                        }
                        if (!part.isEmpty()) {
                            polylines.add(part);
                        }
                    }
                }
            }
        }

        return polylines;
    }

    private Map<Integer, Map<String, Object>> readDBF(File dbfFile) throws IOException {
        Map<Integer, Map<String, Object>> records = new HashMap<>();
        
        try (FileInputStream fis = new FileInputStream(dbfFile)) {
            // Leer encabezado DBF (32 bytes)
            byte[] headerBytes = new byte[32];
            if (fis.read(headerBytes) != 32) {
                throw new IOException("Encabezado DBF inválido");
            }
            
            ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
            headerBuffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // Número de registros y tamaños
            int numRecords = headerBuffer.getInt(4);
            int headerSize = (headerBuffer.getShort(8) & 0xFFFF);
            int recordSize = (headerBuffer.getShort(10) & 0xFFFF);
            
            // Calcular número de campos
            int numFields = (headerSize - 33) / 32;
            
            // Información de los campos
            String[] fieldNames = new String[numFields];
            char[] fieldTypes = new char[numFields];
            int[] fieldLengths = new int[numFields];
            
            // Leer descriptores de campos
            for (int i = 0; i < numFields; i++) {
                byte[] fieldDescBytes = new byte[32];
                if (fis.read(fieldDescBytes) != 32) {
                    throw new IOException("Descriptor de campo inválido");
                }
                
                // Extraer nombre del campo
                StringBuilder nameBuilder = new StringBuilder();
                for (int j = 0; j < 11 && fieldDescBytes[j] != 0; j++) {
                    nameBuilder.append((char)(fieldDescBytes[j] & 0xFF));
                }
                fieldNames[i] = nameBuilder.toString().trim();
                
                // Tipo y longitud del campo
                fieldTypes[i] = (char)(fieldDescBytes[11] & 0xFF);
                fieldLengths[i] = (fieldDescBytes[16] & 0xFF);
            }
            
            // Saltar byte terminador
            fis.skip(1);
            
            // Leer registros
            for (int i = 0; i < numRecords; i++) {
                Map<String, Object> record = new HashMap<>();
                
                // Verificar bandera de eliminación
                byte deletionFlag = (byte) fis.read();
                if (deletionFlag == '*') {
                    fis.skip(recordSize - 1);
                    continue;
                }
                
                // Leer valores de campos
                for (int j = 0; j < numFields; j++) {
                    byte[] fieldValueBytes = new byte[fieldLengths[j]];
                    if (fis.read(fieldValueBytes) != fieldLengths[j]) {
                        throw new IOException("Datos de campo incompletos");
                    }
                    
                    String fieldValue = new String(fieldValueBytes).trim();
                    Object value = parseFieldValue(fieldValue, fieldTypes[j]);
                    record.put(fieldNames[j], value);
                }
                
                records.put(i, record);
            }
        }
        
        return records;
    }
    
    private Object parseFieldValue(String value, char type) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            switch (type) {
                case 'C': // Character
                    return value;
                case 'N': // Numeric
                case 'F': // Float
                    if (value.contains(".")) {
                        return Double.parseDouble(value);
                    } else {
                        return Integer.parseInt(value);
                    }
                case 'L': // Logical
                    return "T".equalsIgnoreCase(value) || "Y".equalsIgnoreCase(value);
                default:
                    return value;
            }
        } catch (NumberFormatException e) {
            return value;
        }
    }
}