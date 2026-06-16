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
}
