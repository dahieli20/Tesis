import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import {
  AuditFactorResponse,
  DataLakeAuditResponse,
  DatasetService
} from '../../services/dataset.service';

@Component({
  selector: 'app-data-lake-status',
  standalone: false,
  templateUrl: './data-lake-status.component.html',
  styleUrls: ['./data-lake-status.component.css']
})
export class DataLakeStatusComponent implements OnInit {
  globalAudit: DataLakeAuditResponse | null = null;
  loading = false;
  errorMessage = '';

  constructor(
    private datasetService: DatasetService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadGlobalAudit();
  }

  loadGlobalAudit(): void {
    this.loading = true;
    this.errorMessage = '';

    this.datasetService.getGlobalAudit().subscribe({
      next: response => {
        this.globalAudit = response;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'No se pudo obtener el estado global del Data Lake.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/datasets/upload']);
  }

  getAuditClassificationLabel(classification: string): string {
    switch (classification) {
      case 'DATA_LAKE_SALUDABLE':
      case 'DATA_LAKE_LIMPIO':
        return 'Data Lake saludable';
      case 'ZONA_FRONTERA':
      case 'FRONTERA':
        return 'Zona frontera';
      case 'DATA_SWAMP':
        return 'Data Swamp';
      case 'SIN_DATOS':
        return 'Sin datos';
      default:
        return classification;
    }
  }

  getAuditClassificationDescription(classification: string): string {
    switch (classification) {
      case 'DATA_LAKE_SALUDABLE':
      case 'DATA_LAKE_LIMPIO':
        return 'Bajo nivel de contaminación en la zona activa del Data Lake.';
      case 'ZONA_FRONTERA':
      case 'FRONTERA':
        return 'El Data Lake presenta señales iniciales de degradación.';
      case 'DATA_SWAMP':
        return 'Alto riesgo de pérdida de utilidad, confiabilidad y reutilización.';
      case 'SIN_DATOS':
        return 'Todavía no hay datos suficientes para calcular el indicador.';
      default:
        return 'Clasificación no reconocida.';
    }
  }

  getIriLevel(value: number): string {
    if (value <= 30) {
      return 'Ingreso estable';
    }

    if (value <= 60) {
      return 'Ingreso con alertas';
    }

    return 'Ingreso riesgoso';
  }

  getIriDescription(value: number): string {
    if (value <= 30) {
      return 'La mayoría de los archivos procesados fue aceptada o generó pocas alertas.';
    }

    if (value <= 60) {
      return 'Una parte importante de los archivos necesitó revisión o fue rechazada.';
    }

    return 'Muchos archivos intentaron ingresar con problemas y fueron filtrados por el sistema.';
  }

  getTopFactor(): AuditFactorResponse | null {
    if (!this.globalAudit || this.globalAudit.factors.length === 0) {
      return null;
    }

    return this.globalAudit.factors.reduce((highest, current) =>
      current.value > highest.value ? current : highest
    );
  }

  getFactorFriendlyName(factor: AuditFactorResponse): string {
    if (factor.name.startsWith('Q')) {
      return 'Calidad de los datos';
    }

    if (factor.name.startsWith('D')) {
      return 'Archivos repetidos';
    }

    if (factor.name.startsWith('R')) {
      return 'Archivos muy parecidos';
    }

    return factor.name;
  }

  getFactorFriendlyDescription(factor: AuditFactorResponse): string {
    if (factor.name.startsWith('Q')) {
      return 'Mira si los CSV aceptados tienen celdas vacías, filas repetidas dentro del mismo archivo o columnas duplicadas.';
    }

    if (factor.name.startsWith('D')) {
      return 'Mira si en raw/ hay archivos exactamente iguales, aunque tengan nombres distintos.';
    }

    if (factor.name.startsWith('R')) {
      return 'Mira si en raw/ hay datasets que no son idénticos, pero comparten muchas filas.';
    }

    return factor.description;
  }

  getFactorShortCode(factor: AuditFactorResponse): string {
    if (factor.name.startsWith('Q')) {
      return 'Q';
    }

    if (factor.name.startsWith('D')) {
      return 'D';
    }

    if (factor.name.startsWith('R')) {
      return 'R';
    }

    return '';
  }

  getRiskLevel(value: number): string {
    if (value <= 30) {
      return 'Bajo';
    }

    if (value <= 60) {
      return 'Medio';
    }

    return 'Alto';
  }

  getFactorMeasuredExplanation(factor: AuditFactorResponse): string {
    const level = this.getRiskLevel(factor.value).toLowerCase();

    if (factor.name.startsWith('Q')) {
      return `Nivel ${level}: mide cuántos problemas de calidad aparecen en los CSV aceptados.`;
    }

    if (factor.name.startsWith('D')) {
      return `Nivel ${level}: mide cuánto contenido repetido exacto existe en raw/.`;
    }

    if (factor.name.startsWith('R')) {
      return `Nivel ${level}: mide cuánto se parecen entre sí los datasets aceptados.`;
    }

    return `Nivel ${level}: mide la presencia de este problema en el Data Lake.`;
  }

  getFactorWeightExplanation(factor: AuditFactorResponse): string {
    const weight = Math.round(factor.weight * 100);

    if (factor.name.startsWith('Q')) {
      return `El modelo le da ${weight}% de importancia porque la calidad define si los datos se pueden usar.`;
    }

    if (factor.name.startsWith('D')) {
      return `El modelo le da ${weight}% de importancia porque los duplicados ocupan espacio y confunden el análisis.`;
    }

    if (factor.name.startsWith('R')) {
      return `El modelo le da ${weight}% de importancia porque datasets muy parecidos reducen la utilidad del lago.`;
    }

    return `El modelo le da ${weight}% de importancia dentro del índice final.`;
  }

  getFactorContributionExplanation(factor: AuditFactorResponse): string {
    return `Este factor agrega ${factor.contribution} puntos al DSI-v1 final.`;
  }

  getFactorFormula(factor: AuditFactorResponse): string {
    if (factor.name.startsWith('Q')) {
      return 'Qi = 0,50Ni + 0,30Fi + 0,20Ci';
    }

    if (factor.name.startsWith('D')) {
      return 'D = (N - H) / N';
    }

    if (factor.name.startsWith('R')) {
      return 'Ri = max Riesgo(Similitud(i,j))';
    }

    return '';
  }
}
