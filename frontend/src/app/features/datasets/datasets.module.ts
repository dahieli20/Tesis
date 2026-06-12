import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DatasetUploadComponent } from './pages/dataset-upload/dataset-upload.component';

@NgModule({
  declarations: [
    DatasetUploadComponent
  ],
  imports: [
    CommonModule
  ],
  exports: [
    DatasetUploadComponent
  ]
})
export class DatasetsModule { }