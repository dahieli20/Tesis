import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DatasetStatusComponent } from './pages/dataset-status/dataset-status.component';
import { DatasetUploadComponent } from './pages/dataset-upload/dataset-upload.component';

@NgModule({
  declarations: [
    DatasetUploadComponent,
    DatasetStatusComponent
  ],
  imports: [
    CommonModule
  ],
  exports: [
    DatasetUploadComponent,
    DatasetStatusComponent
  ]
})
export class DatasetsModule { }