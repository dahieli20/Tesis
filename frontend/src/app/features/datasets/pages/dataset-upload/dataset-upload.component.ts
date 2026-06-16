import { Component, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { DatasetService } from '../../services/dataset.service';
import { DatasetStateService } from '../../services/dataset-state.service';

import {
  FileQueueItem,
  QueueState,
  StatusView
} from '../../../../models/dataset-queue.model';



@Component({
  selector: 'app-dataset-upload',
  standalone: false,
  templateUrl: './dataset-upload.component.html',
  styleUrls: ['./dataset-upload.component.css']
})
export class DatasetUploadComponent {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  fileQueue: FileQueueItem[] = [];
  selectedIndex: number | null = null;
  activeStatusView: StatusView | null = null;

  loading = false;
  errorMessage = '';
  isDragging = false;

constructor(
  private datasetService: DatasetService,
  private datasetStateService: DatasetStateService,
  private router: Router
) {
  this.fileQueue = this.datasetStateService.getFileQueueSnapshot();

  if (this.fileQueue.length > 0) {
    this.selectedIndex = 0;
  }
}

  get selectedItem(): FileQueueItem | null {
    if (this.selectedIndex === null) {
      return null;
    }

    return this.fileQueue[this.selectedIndex] ?? null;
  }

  get waitingCount(): number {
    return this.fileQueue.filter(
      item => item.state === 'WAITING' || item.state === 'ANALYZING'
    ).length;
  }

  get acceptedCount(): number {
    return this.fileQueue.filter(item => item.result?.status === 'ACCEPTED').length;
  }

  get reviewCount(): number {
    return this.fileQueue.filter(item => item.result?.status === 'REVIEW').length;
  }

  get rejectedCount(): number {
    return this.fileQueue.filter(item => item.result?.status === 'REJECTED').length;
  }

  get reviewItems(): FileQueueItem[] {
    return this.fileQueue.filter(item => item.result?.status === 'REVIEW');
  }

  getFileIndex(item: FileQueueItem): number {
    return this.fileQueue.indexOf(item);
  }

  goToDashboard(): void {
    this.activeStatusView = null;
  }

  openFileSelector(): void {
    if (this.loading) {
      return;
    }

    this.resetFileInput();
    this.fileInput.nativeElement.click();
  }

  resetFileInput(): void {
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      this.resetFileInput();
      return;
    }

    this.addFilesToQueue(Array.from(input.files));
    this.resetFileInput();
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;

    if (!event.dataTransfer?.files || event.dataTransfer.files.length === 0) {
      return;
    }

    this.addFilesToQueue(Array.from(event.dataTransfer.files));
  }

  addFilesToQueue(files: File[]): void {
    const csvFiles = files.filter(file =>
      file.name.toLowerCase().endsWith('.csv')
    );

    if (csvFiles.length === 0) {
      this.errorMessage = 'Seleccioná al menos un archivo CSV.';
      return;
    }

    if (csvFiles.length !== files.length) {
      this.errorMessage = 'Algunos archivos fueron ignorados porque no son CSV.';
    } else {
      this.errorMessage = '';
    }

    const newItems: FileQueueItem[] = csvFiles.map(file => ({
      file,
      state: 'WAITING',
      result: null,
      errorMessage: ''
    }));

this.fileQueue = [...this.fileQueue, ...newItems];

this.syncQueueState();

if (this.selectedIndex === null && this.fileQueue.length > 0) {
  this.selectedIndex = 0;
}
  }

  selectFile(index: number): void {
    this.selectedIndex = index;
  }

  async uploadAllFiles(): Promise<void> {
    if (this.fileQueue.length === 0) {
      this.errorMessage = 'Seleccioná al menos un archivo CSV.';
      return;
    }

    const pendingFiles = this.fileQueue.filter(
      item => item.state === 'WAITING' || item.state === 'ERROR'
    );

    if (pendingFiles.length === 0) {
      this.errorMessage = 'No hay archivos nuevos pendientes para analizar.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    try {
      for (const item of pendingFiles) {
        item.state = 'ANALYZING';
        item.errorMessage = '';
        item.result = null;

        try {
          const response = await firstValueFrom(
            this.datasetService.uploadDataset(item.file)
          );

        item.result = response;
        item.state = 'DONE';

        this.syncQueueState();
        } catch (error) {
          item.state = 'ERROR';
          item.errorMessage = 'Ocurrió un error al subir o analizar este archivo.';

          this.syncQueueState();
        }
      }
    } finally {
      this.loading = false;
    }
  }

  removeFile(index: number): void {
    if (this.loading) {
      return;
    }

this.fileQueue.splice(index, 1);

this.syncQueueState();

    if (this.fileQueue.length === 0) {
      this.selectedIndex = null;
      this.errorMessage = '';
      return;
    }

    if (this.selectedIndex !== null) {
      if (index === this.selectedIndex) {
        this.selectedIndex = 0;
      } else if (index < this.selectedIndex) {
        this.selectedIndex--;
      }
    }
  }

clearQueue(): void {
  if (this.loading) {
    return;
  }

  this.fileQueue = [];
  this.selectedIndex = null;
  this.errorMessage = '';

  this.syncQueueState();
}

  getItemDisplayStatusClass(item: FileQueueItem): string {
    if (item.result) {
      return item.result.status.toLowerCase();
    }

    return item.state.toLowerCase();
  }

  getItemDisplayStatusLabel(item: FileQueueItem): string {
    if (item.result) {
      return this.getStatusLabel(item.result.status);
    }

    return this.getQueueStateLabel(item.state);
  }

  getQueueStateLabel(state: QueueState): string {
    switch (state) {
      case 'WAITING':
        return 'En espera';
      case 'ANALYZING':
        return 'Analizando';
      case 'DONE':
        return 'Finalizado';
      case 'ERROR':
        return 'Error';
      default:
        return state;
    }
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

  formatFileSize(size: number): string {
    if (size < 1024) {
      return `${size} B`;
    }

    if (size < 1024 * 1024) {
      return `${(size / 1024).toFixed(2)} KB`;
    }

    return `${(size / (1024 * 1024)).toFixed(2)} MB`;
  }
  selectStatusView(status: StatusView): void {
    this.activeStatusView = status;

    if (status === 'REVIEW') {
      this.selectFirstReviewItem();
    }
  }

private selectFirstReviewItem(): void {
  const firstReviewItem = this.reviewItems[0];

  if (!firstReviewItem) {
    this.selectedIndex = null;
    this.activeStatusView = null;
    return;
  }

  this.selectedIndex = this.fileQueue.indexOf(firstReviewItem);
}
approveSelectedReviewItem(): void {
  const item = this.selectedItem;

  if (!item || !item.result || item.result.status !== 'REVIEW') {
    return;
  }

  this.datasetService.approveDataset(item.result).subscribe({
    next: result => {
      item.result = result;
      item.state = 'DONE';

      this.syncQueueState();
      this.selectFirstReviewItem();
    },
    error: () => {
      item.errorMessage = 'No se pudo aprobar el documento en el backend.';
      this.syncQueueState();
    }
  });
}

rejectSelectedReviewItem(): void {
  const item = this.selectedItem;

  if (!item || !item.result || item.result.status !== 'REVIEW') {
    return;
  }

  this.datasetService.rejectDataset(item.result).subscribe({
    next: result => {
      item.result = result;
      item.state = 'DONE';

      this.syncQueueState();
      this.selectFirstReviewItem();
    },
    error: () => {
      item.errorMessage = 'No se pudo rechazar el documento en el backend.';
      this.syncQueueState();
    }
  });
}

goToReviewView(): void {
  this.activeStatusView = 'REVIEW';
  this.syncQueueState();
  this.router.navigate(['/datasets/en-revision']);
}

private syncQueueState(): void {
  this.datasetStateService.setFileQueue(this.fileQueue);
}

goToStatusView(status: StatusView): void {
  this.activeStatusView = status;
  this.syncQueueState();
  this.router.navigate(['/datasets/status', status]);
}
}
