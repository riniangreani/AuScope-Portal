import { LayerModel } from '../../portal-core-ag/modal/data/layer.model';
import { OlMapService } from '../../portal-core-ag/service/openlayermap/ol-map.service';
import { Component, Input} from '@angular/core';


@Component({
    selector: 'app-filter-panel',
    templateUrl: './filterpanel.component.html'
})


export class FilterPanelComponent {


  @Input() layer: LayerModel;

  constructor(public olMapService: OlMapService) {}

  public addLayer(layer): void {
    this.olMapService.addLayer(layer);
  }

}
