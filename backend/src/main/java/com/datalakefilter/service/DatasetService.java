package com.datalakefilter.service;

import com.datalakefilter.dto.CsvQualityResult;
import com.datalakefilter.dto.UploadDatasetResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DatasetService {

    private final MinioService minioService;
    private final HashService hashService;
    private final CsvQualityService csvQualityService;

    public DatasetService(
            MinioService minioService,
            HashService hashService,
            CsvQualityService csvQualityService
    ) {
        this.minioService = minioService;
        this.hashService = hashService;
        this.csvQualityService = csvQualityService;
    }

    public UploadDatasetResponse processDataset(MultipartFile file) {

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            return new UploadDatasetResponse(
                    null,
                    null,
                    "REJECTED",
                    "El archivo no tiene nombre.",
                    null,
                    null,
                    null,
                    null
            );
        }

        if (!fileName.toLowerCase().endsWith(".csv")) {
            return new UploadDatasetResponse(
                    fileName,
                    null,
                    "REJECTED",
                    "Solo se permiten archivos CSV.",
                    null,
                    null,
                    null,
                    null
            );
        }

        CsvQualityResult quality = csvQualityService.analyze(file);

        if (!quality.hasDataRows()) {
            String rejectedPath = "rejected/" + fileName;

            minioService.uploadFile(rejectedPath, file);

            return new UploadDatasetResponse(
                    fileName,
                    rejectedPath,
                    "REJECTED",
                    "El CSV no contiene filas de datos.",
                    null,
                    null,
                    null,
                    quality
            );
        }

        if (quality.hasDuplicateColumns()) {
            String reviewPath = "review/" + fileName;

            minioService.uploadFile(reviewPath, file);

            return new UploadDatasetResponse(
                    fileName,
                    reviewPath,
                    "REVIEW",
                    "El CSV contiene columnas duplicadas.",
                    null,
                    null,
                    null,
                    quality
            );
        }

        if (quality.nullPercentage() >= 40.0) {
            String reviewPath = "review/" + fileName;

            minioService.uploadFile(reviewPath, file);

            return new UploadDatasetResponse(
                    fileName,
                    reviewPath,
                    "REVIEW",
                    "El CSV contiene un porcentaje alto de valores nulos.",
                    null,
                    null,
                    null,
                    quality
            );
        }

        String newFileHash = hashService.calculateFileHash(file);
        Set<String> newRowHashes = hashService.calculateRowHashes(file);

        List<String> rawFiles = minioService.listFiles("raw/");

        double highestPartialMatch = 0.0;
        String mostSimilarFile = null;

        for (String rawFile : rawFiles) {

            try (
                    InputStream existingFileForHash = minioService.getFile(rawFile);
                    InputStream existingFileForRows = minioService.getFile(rawFile)
            ) {

                String existingHash = hashService.calculateHash(existingFileForHash);

                if (newFileHash.equals(existingHash)) {

                    String rejectedPath = "rejected/" + fileName;

                    minioService.uploadFile(rejectedPath, file);

                    return new UploadDatasetResponse(
                            fileName,
                            rejectedPath,
                            "REJECTED",
                            "Archivo duplicado exacto. Coincide con: " + rawFile,
                            newFileHash,
                            100.0,
                            rawFile,
                            quality
                    );
                }

                Set<String> existingRowHashes =
                        hashService.calculateRowHashes(existingFileForRows);

                double matchPercentage =
                        calculateMatchPercentage(newRowHashes, existingRowHashes);

                if (matchPercentage > highestPartialMatch) {
                    highestPartialMatch = matchPercentage;
                    mostSimilarFile = rawFile;
                }

            } catch (Exception e) {
                throw new RuntimeException(
                        "Error al comparar archivo con: " + rawFile,
                        e
                );
            }
        }

        if (highestPartialMatch >= 60.0) {

            String reviewPath = "review/" + fileName;

            minioService.uploadFile(reviewPath, file);

            return new UploadDatasetResponse(
                    fileName,
                    reviewPath,
                    "REVIEW",
                    "Posible duplicado parcial. Coincidencia de "
                            + highestPartialMatch
                            + "% con: "
                            + mostSimilarFile,
                    newFileHash,
                    highestPartialMatch,
                    mostSimilarFile,
                    quality
            );
        }

        String rawPath = "raw/" + fileName;

        minioService.uploadFile(rawPath, file);

        return new UploadDatasetResponse(
                fileName,
                rawPath,
                "ACCEPTED",
                "Archivo nuevo. Guardado en raw.",
                newFileHash,
                highestPartialMatch,
                mostSimilarFile,
                quality
        );
    }

    private double calculateMatchPercentage(
            Set<String> newRows,
            Set<String> existingRows
    ) {

        if (newRows == null || newRows.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(newRows);

        intersection.retainAll(existingRows);

        return Math.round(
                ((intersection.size() * 100.0) / newRows.size()) * 100.0
        ) / 100.0;
    }
}