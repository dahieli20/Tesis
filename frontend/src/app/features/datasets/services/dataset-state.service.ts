import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { FileQueueItem } from '../../../models/dataset-queue.model';

@Injectable({
  providedIn: 'root'
})
export class DatasetStateService {
  private readonly fileQueueSubject = new BehaviorSubject<FileQueueItem[]>([]);

  fileQueue$ = this.fileQueueSubject.asObservable();

  setFileQueue(items: FileQueueItem[]): void {
    this.fileQueueSubject.next([...items]);
  }

  getFileQueueSnapshot(): FileQueueItem[] {
    return this.fileQueueSubject.value;
  }

  updateItemStatus(
    targetItem: FileQueueItem,
    status: 'ACCEPTED' | 'REJECTED',
    destinationFolder: 'raw' | 'rejected',
    message: string
  ): void {
    const updatedQueue = this.fileQueueSubject.value.map(item => {
      if (item !== targetItem || !item.result) {
        return item;
      }

      return {
        ...item,
        state: 'DONE' as const,
        result: {
          ...item.result,
          status,
          path: `${destinationFolder}/${item.result.fileName}`,
          message
        }
      };
    });

    this.fileQueueSubject.next(updatedQueue);
  }
}