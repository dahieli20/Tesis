import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DatasetUploadComponent } from './features/datasets/pages/dataset-upload/dataset-upload.component';
import { DatasetStatusComponent } from './features/datasets/pages/dataset-status/dataset-status.component';
import { DataLakeStatusComponent } from './features/datasets/pages/data-lake-status/data-lake-status.component';

const routes: Routes = [
  {
    path: 'datasets/upload',
    component: DatasetUploadComponent
  },
  {
    path: 'datasets/lake-status',
    component: DataLakeStatusComponent
  },
  {
    path: 'datasets/status/:status',
    component: DatasetStatusComponent
  },
  {
    path: '',
    redirectTo: 'datasets/upload',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'datasets/upload'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
