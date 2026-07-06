import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

export interface CsvQualityResult {
  totalRows: number;
  totalColumns: number;
  nullPercentage: number;
  duplicateRowsPercentage: number;
  hasDuplicateColumns: boolean;
  hasDataRows: boolean;
}

export interface UploadDatasetResponse {
  fileName: string;
  path: string;
  status: string;
  message: string;
  fileHash: string;
  partialMatchPercentage: number;
  matchedWith: string;
  quality: CsvQualityResult;
}

export interface AuditFactorResponse {
  name: string;
  value: number;
  weight: number;
  contribution: number;
  description: string;
}

export interface DataLakeAuditResponse {
  totalFiles: number;
  rawFiles: number;
  reviewFiles: number;
  rejectedFiles: number;
  ingestionRiskPercentage: number;
  qualityRiskPercentage: number;
  exactDuplicationRiskPercentage: number;
  redundancyRiskPercentage: number;
  dataSwampIndexPercentage: number;
  globalRiskPercentage: number;
  classification: string;
  message: string;
  factors: AuditFactorResponse[];
}
@Injectable({
  providedIn: 'root'
})
export class DatasetService {

  private readonly apiUrl = `${environment.apiUrl}/datasets`;

  constructor(private http: HttpClient) {}

  uploadDataset(file: File): Observable<UploadDatasetResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<UploadDatasetResponse>(`${this.apiUrl}/upload`, formData);
  }

  approveDataset(dataset: UploadDatasetResponse): Observable<UploadDatasetResponse> {
    return this.http.post<UploadDatasetResponse>(
      `${this.apiUrl}/review/approve`,
      dataset
    );
  }

  rejectDataset(dataset: UploadDatasetResponse): Observable<UploadDatasetResponse> {
    return this.http.post<UploadDatasetResponse>(
      `${this.apiUrl}/review/reject`,
      dataset
    );
  }

  getGlobalAudit(): Observable<DataLakeAuditResponse> {
    const timestamp = new Date().getTime();

    return this.http.get<DataLakeAuditResponse>(
      `${this.apiUrl}/audit/global?t=${timestamp}`
    );
  }


}
