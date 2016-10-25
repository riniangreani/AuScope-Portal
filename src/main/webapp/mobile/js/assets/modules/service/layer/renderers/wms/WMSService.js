/**
 * WMSService handles rendering of all wms layers onto the map
 * @module map
 * @class WMSService
 * 
 */
allModules.service('WMSService',['GoogleMapService','LayerManagerService','Constants','GetWMSRelatedService','RenderStatusService','QuerierPanelService','WMS_1_1_0_Service','WMS_1_3_0_Service',
                                 function (GoogleMapService,LayerManagerService,Constants,GetWMSRelatedService,RenderStatusService,QuerierPanelService,WMS_1_1_0_Service,WMS_1_3_0_Service) {
    
    var addLayerToGoogleMap = function(mapLayer,layer,onlineResource){
        
        var map =  GoogleMapService.getMap();
        
        var registerTileLoadedEvent = function(mapLayer, layer,onlineResource,status){
            google.maps.event.addListener(mapLayer, 'tilesloaded', function(evt) {
                RenderStatusService.updateCompleteStatus(layer,onlineResource,status);
              });
        };
        
        // Register map click/touch events to allow creation of the query information panel
        var registerClickEvent = function(map, onlineResource, bbox){
            var mapEventListener = google.maps.event.addListener(map, 'mousedown', function(evt) {
                // Send a request to the WMS service if the click is within the resource's bounding box
                if (evt.latLng.lat() < bbox.northBoundLatitude && evt.latLng.lat() > bbox.southBoundLatitude &&
                    evt.latLng.lng() < bbox.eastBoundLongitude && evt.latLng.lng() > bbox.westBoundLongitude) {
                        
                    // Send request to WMS service
                    GetWMSRelatedService.getWMSMarkerInfo(evt.latLng, evt.pixel, map, onlineResource).then(function(response) 
                        { 
                            // Used to check for an empty response, which occurs when user clicks/touches on empty space
                            var empty_html_body = /<body>\s*<\/body>/g;
                            var empty_html_body2 = /<body>\s*<script .+<\/script>\s*<\/body>/g;
                            var empty_gml_body = /<wfs:FeatureCollection .+\/>$/g;

                            // Open if panel if there was a valid response (NB: Only status code 200 will return a complete response)
                            if (response.status==200 && empty_gml_body.test(response.data)==false) {
                                QuerierPanelService.setPanelXml(response.data);
                                QuerierPanelService.openPanel(false);
                            }
                        },
                        function(errorResponse) {
                            console.log("WMS Service Error: ", errorResponse);
                        }
                    );
                }
            });
            QuerierPanelService.registerLayer(onlineResource, mapEventListener);
        };
        
        /**
         * Hack to override the gettile to gain access to the underlying img for event handling
         */
        var overrideToRegisterFailureEvent = function(mapLayer,failureHandlerCB){
            
            mapLayer.imagelayerGetTile = mapLayer.getTile;

            mapLayer.getTile = function(tileCoord, zoom, ownerDocument){           
                var node = mapLayer.imagelayerGetTile(tileCoord, zoom, ownerDocument);
                
                node.innerHTML = '<img src="'+mapLayer.j(tileCoord,zoom)+'"/>';
                $("img", node).one("error", function() {               
                    node.innerHTML = "";
                    failureHandlerCB();
                });
                        
                return node;
            };
            return mapLayer;
        };
        
        registerTileLoadedEvent(mapLayer,layer,onlineResource,Constants.statusProgress.COMPLETED);
        
        // Get the bounding box for the 'onlineResource', then register for click events within that bounding box
        var cswRecords = LayerManagerService.getCSWRecords(layer);
        var done = false;
        for (var i=0; i<cswRecords.length && !done; i++) {
            var onlineResources = cswRecords[i].onlineResources;
            for (var j=0; j<onlineResources.length; j++) {
                if (onlineResource==onlineResources[j]) {
                    var bbox = cswRecords[i].geographicElements[0];
                    registerClickEvent(map, onlineResource, bbox);
                    done = true;
                    break;
                }
            }
        }
        
        mapLayer = overrideToRegisterFailureEvent(mapLayer,function(){
            RenderStatusService.updateCompleteStatus(layer,onlineResource,Constants.statusProgress.ERROR);
        });
        map.overlayMapTypes.push(mapLayer);
        GoogleMapService.addLayerToActive(layer,mapLayer);       
       
    };
   
 
    /**
     * Method to decide how the wms should be rendered and add the wms to the map 
     * @method renderLayer
     * @param layer - The layer containing the wms to be rendered
     * @param selectedFilters - OPTIONAL - parameter to be passed into retrieving the SLD.
     */
    this.renderLayer = function(layer,selectedFilters){   

        var me = this;
        var maxSldLength = 2000;

        GetWMSRelatedService.getWMSStyle(layer,selectedFilters).then(function(style){
            var onlineResources = LayerManagerService.getWMS(layer);            
            RenderStatusService.setMaxValue(layer,onlineResources.length);
            for(var index in onlineResources){  
                
                RenderStatusService.updateCompleteStatus(layer,onlineResources[index],Constants.statusProgress.RUNNING);
                if(onlineResources[index].version === Constants.WMSVersion['1.1.1'] || onlineResources[index].version === Constants.WMSVersion['1.1.0']){
                    var mapLayer = WMS_1_1_0_Service.generateLayer(onlineResources[index],(style!=null && style.length<maxSldLength?style:null));                        
                    addLayerToGoogleMap(mapLayer,layer,onlineResources[index]);                    
                }else if(onlineResources[index].version === Constants.WMSVersion['1.3.0']){
                    var mapLayer = WMS_1_3_0_Service.generateLayer(onlineResources[index],(style!=null && style.length<maxSldLength?style:null)); 
                    addLayerToGoogleMap(mapLayer,layer,onlineResources[index]); 
                    
                }        
            }
        });
  
    };
    
    
    
    
    
    /**
     * Method to decide how the wms csw record should be rendered and add the wms to the map 
     * @method renderCSWRecord
     * @param layer - The layer containing the wms to be rendered
     * @param cswRecord - The cswRecord containing the wms to be rendered
     */
    this.renderCSWRecord = function(layer,cswRecord){         
        var me = this;
        var maxSldLength = 2000;
        
        GetWMSRelatedService.getWMSStyle(layer).then(function(style){
            var onlineResources = LayerManagerService.getWMSFromCSW(cswRecord);            
            RenderStatusService.setMaxValue(layer,onlineResources.length);
            for(var index in onlineResources){  
                
                RenderStatusService.updateCompleteStatus(layer,onlineResources[index],Constants.statusProgress.RUNNING);
                
                if(onlineResources[index].version === Constants.WMSVersion['1.1.1'] || onlineResources[index].version === Constants.WMSVersion['1.1.0']){
                    var mapLayer = WMS_1_1_0_Service.generateLayer(onlineResources[index],(style!=null && style.length<maxSldLength?style:null));                        
                    addLayerToGoogleMap(mapLayer,layer,onlineResources[index]);                  
                }else if(onlineResources[index].version === Constants.WMSVersion['1.3.0']){
                    var mapLayer = WMS_1_3_0_Service.generateLayer(onlineResources[index],(style!=null && style.length<maxSldLength?style:null)); 
                    addLayerToGoogleMap(mapLayer,layer,onlineResources[index]);  
                }        
            }
        });
  
    };
    
     
}]);