import { Component } from '@angular/core';

import { DatasetService, UploadDatasetResponse } from '../../services/dataset.service';

@Component({
  selector: 'app-dataset-upload',
  standalone: false,
  templateUrl: './dataset-upload.component.html',
  styleUrls: ['./dataset-upload.component.css']
})
export class DatasetUploadComponent {

  selectedFile: File | null = null;
  result: UploadDatasetResponse | null = null;
  loading = false;
  errorMessage = '';

  constructor(private datasetService: DatasetService) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      this.selectedFile = null;
      return;
    }

    this.selectedFile = input.files[0];
    this.result = null;
    this.errorMessage = '';
  }

  uploadFile(): void {
    if (!this.selectedFile) {
      this.errorMessage = 'Seleccioná un archivo CSV.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.result = null;

    this.datasetService.uploadDataset(this.selectedFile).subscribe({
      next: (response) => {
        this.result = response;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Ocurrió un error al subir el archivo.';
        this.loading = false;
      }
    });
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACCEPTED':
        return 'Aceptado';
      case 'REVIEW':
        return 'En revisión';
      case 'REJECTED':
        return 'Rechazado';
      default:
        return status;
    }
  }

  getRecommendation(status: string): string {
    switch (status) {
        case 'ACCEPTED':
        return 'El dataset puede ser almacenado en la zona raw del Data Lake.';
        case 'REVIEW':
        return 'El dataset debe ser revisado antes de almacenarse definitivamente.';
        case 'REJECTED':
        return 'El dataset no debe ser incorporado al Data Lake.';
        default:
        return 'No se pudo determinar una recomendación.';
    }
    }
}