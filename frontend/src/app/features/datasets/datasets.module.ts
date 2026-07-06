import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DatasetStatusComponent } from './pages/dataset-status/dataset-status.component';
import { DatasetUploadComponent } from './pages/dataset-upload/dataset-upload.component';
import { DataLakeStatusComponent } from './pages/data-lake-status/data-lake-status.component';

@NgModule({
  declarations: [
    DatasetUploadComponent,
    DatasetStatusComponent,
    DataLakeStatusComponent
  ],
  imports: [
    CommonModule
  ],
  exports: [
    DatasetUploadComponent,
    DatasetStatusComponent,
    DataLakeStatusComponent
  ]
})
export class DatasetsModule { }
