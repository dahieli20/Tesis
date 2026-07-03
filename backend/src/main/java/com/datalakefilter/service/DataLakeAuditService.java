package com.datalakefilter.service;

import com.datalakefilter.dto.AuditFactorResponse;
import com.datalakefilter.dto.DataLakeAuditResponse;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class DataLakeAuditService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public DataLakeAuditService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public DataLakeAuditResponse auditDataLake() {
        List<String> rawFiles = listCsvFiles("raw/");
        List<String> reviewFiles = listCsvFiles("review/");
        List<String> rejectedFiles = listCsvFiles("rejected/");

        int rawCount = rawFiles.size();
        int reviewCount = reviewFiles.size();
        int rejectedCount = rejectedFiles.size();
        int totalFiles = rawCount + reviewCount + rejectedCount;

        if (totalFiles == 0) {
        return new DataLakeAuditResponse(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                "SIN_DATOS",
                "El Data Lake todavía no contiene archivos para auditar.",
                List.of()
        );
        }

        double stateRisk = calculateStateRisk(totalFiles, reviewCount, rejectedCount);
        double rawQualityRisk = calculateRawQualityRisk(rawFiles);
        double rawDuplicationRisk = calculateRawDuplicationRisk(rawFiles);

        double globalRisk = round(
                (stateRisk * 0.30)
                        + (rawQualityRisk * 0.30)
                        + (rawDuplicationRisk * 0.40)
        );

        String classification = classify(globalRisk);
        String message = buildMessage(classification, globalRisk);

        List<AuditFactorResponse> factors = List.of(
                new AuditFactorResponse(
                        "Riesgo por distribución de estados",
                        stateRisk,
                        0.30,
                        round(stateRisk * 0.30),
                        "Mide la proporción de archivos en revisión y rechazados dentro del Data Lake."
                ),
                new AuditFactorResponse(
                        "Riesgo real de calidad en raw",
                        rawQualityRisk,
                        0.30,
                        round(rawQualityRisk * 0.30),
                        "Audita los archivos aceptados en raw/ para detectar problemas de calidad interna."
                ),
                new AuditFactorResponse(
                        "Riesgo por duplicidad en raw",
                        rawDuplicationRisk,
                        0.40,
                        round(rawDuplicationRisk * 0.40),
                        "Detecta archivos con el mismo contenido dentro de raw/, aunque tengan nombres diferentes."
                )
        );

        return new DataLakeAuditResponse(
                totalFiles,
                rawCount,
                reviewCount,
                rejectedCount,
                stateRisk,
                rawQualityRisk,
                rawDuplicationRisk,
                globalRisk,
                classification,
                message,
                factors
        );
    }

    public DataLakeAuditResponse auditRepository(String prefix) {
        String normalizedPrefix = normalizePrefix(prefix);

        List<String> repositoryFiles = listCsvFiles(normalizedPrefix);

        int totalFiles = repositoryFiles.size();

        if (totalFiles == 0) {
            return new DataLakeAuditResponse(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    "SIN_DATOS",
                    "No se encontraron archivos CSV para auditar en la ruta indicada.",
                    List.of()
            );
        }

        double qualityRisk = calculateRawQualityRisk(repositoryFiles);
        double redundancyRisk = calculateRawDuplicationRisk(repositoryFiles);

        double globalRisk = round(
                (qualityRisk * 0.45)
                        + (redundancyRisk * 0.55)
        );

        String classification = classify(globalRisk);

        String message = switch (classification) {
            case "DATA_LAKE_LIMPIO" ->
                    "El repositorio auditado se mantiene limpio. Riesgo global: " + globalRisk + "%.";
            case "FRONTERA" ->
                    "El repositorio auditado se encuentra en zona de frontera entre Data Lake y Data Swamp. Riesgo global: " + globalRisk + "%.";
            case "DATA_SWAMP" ->
                    "El repositorio auditado presenta alto riesgo de Data Swamp. Riesgo global: " + globalRisk + "%.";
            default ->
                    "No se pudo determinar la clasificación del repositorio auditado.";
        };

        List<AuditFactorResponse> factors = List.of(
                new AuditFactorResponse(
                        "Riesgo real de calidad del repositorio",
                        qualityRisk,
                        0.45,
                        round(qualityRisk * 0.45),
                        "Audita los archivos existentes para detectar nulos, columnas duplicadas, filas duplicadas y archivos sin datos útiles."
                ),
                new AuditFactorResponse(
                        "Riesgo por redundancia del repositorio",
                        redundancyRisk,
                        0.55,
                        round(redundancyRisk * 0.55),
                        "Detecta archivos duplicados o muy similares dentro de la ruta auditada."
                )
        );

        return new DataLakeAuditResponse(
                totalFiles,
                totalFiles,
                0,
                0,
                0,
                qualityRisk,
                redundancyRisk,
                globalRisk,
                classification,
                message,
                factors
        );
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return "";
        }

        String normalized = prefix.trim();

        if (normalized.equals("/")) {
            return "";
        }

        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }

        return normalized;
    }

    private List<String> listCsvFiles(String prefix) {
        List<String> files = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();

                if (!item.isDir() && item.objectName().toLowerCase().endsWith(".csv")) {
                    files.add(item.objectName());
                }
            }

            return files;
        } catch (Exception e) {
            throw new RuntimeException("Error al listar archivos en MinIO: " + prefix, e);
        }
    }

    private double calculateStateRisk(int totalFiles, int reviewFiles, int rejectedFiles) {
        double score = ((reviewFiles * 50.0) + (rejectedFiles * 100.0)) / totalFiles;
        return round(score);
    }

    private double calculateRawQualityRisk(List<String> rawFiles) {
        if (rawFiles.isEmpty()) {
            return 0;
        }

        double totalRisk = 0;

        for (String rawFile : rawFiles) {
            totalRisk += auditRawCsvFile(rawFile);
        }

        return round(totalRisk / rawFiles.size());
    }

    private double auditRawCsvFile(String objectName) {
        try (
                InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                )
        ) {
            List<String[]> rows = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                rows.add(parseCsvLine(line));
            }

            if (rows.isEmpty()) {
                return 100;
            }

            String[] headers = rows.get(0);
            List<String[]> dataRows = rows.size() > 1
                    ? rows.subList(1, rows.size())
                    : List.of();

            boolean hasNoDataRows = dataRows.isEmpty();
            boolean hasDuplicateColumns = hasDuplicateColumns(headers);
            double nullPercentage = calculateNullPercentage(dataRows);
            double duplicateRowsPercentage = calculateDuplicateRowsPercentage(dataRows);

            double risk = 0;

            if (hasNoDataRows) {
                risk += 40;
            }

            if (hasDuplicateColumns) {
                risk += 20;
            }

            if (nullPercentage > 0) {
                risk += Math.min(25, nullPercentage * 0.35);
            }

            if (duplicateRowsPercentage > 0) {
                risk += Math.min(15, duplicateRowsPercentage * 0.25);
            }

            return round(Math.min(risk, 100));
        } catch (Exception e) {
            throw new RuntimeException("Error al auditar archivo raw: " + objectName, e);
        }
    }

    private String[] parseCsvLine(String line) {
        return line.split(",", -1);
    }

    private boolean hasDuplicateColumns(String[] headers) {
        Set<String> seen = new HashSet<>();

        for (String header : headers) {
            String normalized = header.trim().toLowerCase();

            if (seen.contains(normalized)) {
                return true;
            }

            seen.add(normalized);
        }

        return false;
    }

    private double calculateNullPercentage(List<String[]> dataRows) {
        if (dataRows.isEmpty()) {
            return 100;
        }

        int totalCells = 0;
        int nullCells = 0;

        for (String[] row : dataRows) {
            for (String cell : row) {
                totalCells++;

                if (cell == null || cell.trim().isEmpty()) {
                    nullCells++;
                }
            }
        }

        if (totalCells == 0) {
            return 100;
        }

        return round((nullCells * 100.0) / totalCells);
    }

    private double calculateDuplicateRowsPercentage(List<String[]> dataRows) {
        if (dataRows.isEmpty()) {
            return 0;
        }

        Set<String> uniqueRows = new HashSet<>();
        int duplicatedRows = 0;

        for (String[] row : dataRows) {
            String normalizedRow = String.join("|", row).trim().toLowerCase();

            if (uniqueRows.contains(normalizedRow)) {
                duplicatedRows++;
            } else {
                uniqueRows.add(normalizedRow);
            }
        }

        return round((duplicatedRows * 100.0) / dataRows.size());
    }

    private double calculateRawDuplicationRisk(List<String> rawFiles) {
        if (rawFiles.size() <= 1) {
            return 0;
        }

        List<RepositoryFileFingerprint> fingerprints = new ArrayList<>();

        for (String rawFile : rawFiles) {
            fingerprints.add(new RepositoryFileFingerprint(
                    rawFile,
                    calculateSha256(rawFile),
                    readNormalizedRows(rawFile)
            ));
        }

        Map<String, Double> riskByFile = new HashMap<>();

        for (RepositoryFileFingerprint fingerprint : fingerprints) {
            riskByFile.put(fingerprint.objectName(), 0.0);
        }

        for (int i = 0; i < fingerprints.size(); i++) {
            for (int j = i + 1; j < fingerprints.size(); j++) {
                RepositoryFileFingerprint first = fingerprints.get(i);
                RepositoryFileFingerprint second = fingerprints.get(j);

                double pairRisk = calculatePairRedundancyRisk(first, second);

                if (pairRisk > 0) {
                    riskByFile.put(
                            first.objectName(),
                            Math.max(riskByFile.get(first.objectName()), pairRisk)
                    );

                    riskByFile.put(
                            second.objectName(),
                            Math.max(riskByFile.get(second.objectName()), pairRisk)
                    );
                }
            }
        }

        double totalRisk = 0;

        for (double risk : riskByFile.values()) {
            totalRisk += risk;
        }

        return round(totalRisk / rawFiles.size());
    }

    private double calculatePairRedundancyRisk(
            RepositoryFileFingerprint first,
            RepositoryFileFingerprint second
    ) {
        if (first.sha256().equals(second.sha256())) {
            return 100;
        }

        double similarity = calculateRowSimilarityPercentage(
                first.normalizedRows(),
                second.normalizedRows()
        );

        if (similarity >= 95) {
            return 90;
        }

        if (similarity >= 80) {
            return 70;
        }

        if (similarity >= 65) {
            return 45;
        }

        return 0;
    }

    private double calculateRowSimilarityPercentage(Set<String> firstRows, Set<String> secondRows) {
        if (firstRows.isEmpty() && secondRows.isEmpty()) {
            return 100;
        }

        if (firstRows.isEmpty() || secondRows.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(firstRows);
        intersection.retainAll(secondRows);

        int maxRows = Math.max(firstRows.size(), secondRows.size());

        return round((intersection.size() * 100.0) / maxRows);
    }

    private Set<String> readNormalizedRows(String objectName) {
        try (
                InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                )
        ) {
            Set<String> rows = new HashSet<>();
            String line;

            while ((line = reader.readLine()) != null) {
                String normalized = normalizeCsvLine(line);

                if (!normalized.isBlank()) {
                    rows.add(normalized);
                }
            }

            return rows;
        } catch (Exception e) {
            throw new RuntimeException("Error al leer filas normalizadas del archivo: " + objectName, e);
        }
    }

    private String normalizeCsvLine(String line) {
        if (line == null) {
            return "";
        }

        return line
                .replace("\uFEFF", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    private String calculateSha256(String objectName) {
        try (
                InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                )
        ) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular hash del archivo: " + objectName, e);
        }
    }

    private String classify(double globalRisk) {
        if (globalRisk <= 30) {
            return "DATA_LAKE_LIMPIO";
        }

        if (globalRisk <= 60) {
            return "FRONTERA";
        }

        return "DATA_SWAMP";
    }

    private String buildMessage(String classification, double globalRisk) {
        return switch (classification) {
            case "DATA_LAKE_LIMPIO" ->
                    "El Data Lake se mantiene limpio. Riesgo global: " + globalRisk + "%.";
            case "FRONTERA" ->
                    "El Data Lake se encuentra en una zona de frontera. Riesgo global: " + globalRisk + "%.";
            case "DATA_SWAMP" ->
                    "El Data Lake presenta alto riesgo de convertirse en Data Swamp. Riesgo global: " + globalRisk + "%.";
            default ->
                    "No se pudo determinar la clasificación del Data Lake.";
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record RepositoryFileFingerprint(
        String objectName,
        String sha256,
        Set<String> normalizedRows
    ) {
    }
}