package com.datalakefilter.service;

import com.datalakefilter.dto.CsvQualityResult;
import com.datalakefilter.dto.DatasetDecisionRequest;
import com.datalakefilter.dto.UploadDatasetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

    @Mock
    private MinioService minioService;

    @Mock
    private HashService hashService;

    @Mock
    private CsvQualityService csvQualityService;

    @InjectMocks
    private DatasetService datasetService;

    @Test
    void approveDatasetMovesReviewFileToRaw() {
        CsvQualityResult quality = new CsvQualityResult(
                10,
                4,
                5.0,
                0.0,
                false,
                true
        );
        DatasetDecisionRequest request = new DatasetDecisionRequest(
                "alumnos.csv",
                "review/alumnos.csv",
                "abc123",
                65.0,
                "raw/alumnos_base.csv",
                quality
        );

        UploadDatasetResponse response = datasetService.approveDataset(request);

        verify(minioService).moveFile("review/alumnos.csv", "raw/alumnos.csv");
        assertEquals("alumnos.csv", response.fileName());
        assertEquals("raw/alumnos.csv", response.path());
        assertEquals("ACCEPTED", response.status());
        assertEquals("abc123", response.fileHash());
        assertEquals(65.0, response.partialMatchPercentage());
        assertEquals("raw/alumnos_base.csv", response.matchedWith());
        assertEquals(quality, response.quality());
    }

    @Test
    void rejectDatasetMovesReviewFileToRejected() {
        DatasetDecisionRequest request = new DatasetDecisionRequest(
                "alumnos.csv",
                "review/alumnos.csv",
                null,
                null,
                null,
                null
        );

        UploadDatasetResponse response = datasetService.rejectDataset(request);

        verify(minioService).moveFile("review/alumnos.csv", "rejected/alumnos.csv");
        assertEquals("rejected/alumnos.csv", response.path());
        assertEquals("REJECTED", response.status());
    }

    @Test
    void decisionRequiresReviewSourcePath() {
        DatasetDecisionRequest request = new DatasetDecisionRequest(
                "alumnos.csv",
                "raw/alumnos.csv",
                null,
                null,
                null,
                null
        );

        assertThrows(
                ResponseStatusException.class,
                () -> datasetService.approveDataset(request)
        );
    }
}
