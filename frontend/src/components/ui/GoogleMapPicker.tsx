'use client';

import { useState, useEffect, useRef } from 'react';
import { X, MapPin, Search, Check } from 'lucide-react';

interface GoogleMapPickerProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (location: LocationData) => void;
  initialLocation?: LocationData;
}

export interface LocationData {
  address: string;
  latitude: number;
  longitude: number;
  city?: string;
  country?: string;
}

declare global {
  interface Window {
    google: any;
    initMap: () => void;
  }
}

export default function GoogleMapPicker({ isOpen, onClose, onSelect, initialLocation }: GoogleMapPickerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedLocation, setSelectedLocation] = useState<LocationData | null>(initialLocation || null);
  const [isLoading, setIsLoading] = useState(false);
  const [mapLoaded, setMapLoaded] = useState(false);
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const markerRef = useRef<any>(null);

  // Prevent background scrolling when modal is open
  useEffect(() => {
    if (isOpen) {
      // Save current scroll position
      const scrollY = window.scrollY;
      
      // Lock body scroll
      document.body.style.position = 'fixed';
      document.body.style.top = `-${scrollY}px`;
      document.body.style.width = '100%';
      document.body.style.overflow = 'hidden';
      
      return () => {
        // Restore scroll when modal closes
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.width = '';
        document.body.style.overflow = '';
        window.scrollTo(0, scrollY);
      };
    }
  }, [isOpen]);

  // Load Google Maps API
  useEffect(() => {
    if (isOpen && !window.google) {
      const script = document.createElement('script');
      script.src = `https://maps.googleapis.com/maps/api/js?key=${process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY}&libraries=places&callback=initMap`;
      script.async = true;
      script.defer = true;
      
      window.initMap = () => {
        setMapLoaded(true);
        initializeMap();
      };
      
      document.head.appendChild(script);
      
      return () => {
        document.head.removeChild(script);
        delete (window as any).initMap;
      };
    } else if (isOpen && window.google && !mapLoaded) {
      setMapLoaded(true);
      initializeMap();
    }
  }, [isOpen]);

  const initializeMap = () => {
    if (!mapRef.current || !window.google) {return;}

    const defaultCenter = selectedLocation 
      ? { lat: selectedLocation.latitude, lng: selectedLocation.longitude }
      : { lat: 39.9042, lng: 116.4074 }; // Default to Beijing

    mapInstanceRef.current = new window.google.maps.Map(mapRef.current, {
      center: defaultCenter,
      zoom: 10,
      mapTypeId: 'roadmap',
      styles: [
        {
          featureType: 'poi',
          elementType: 'labels',
          stylers: [{ visibility: 'off' }]
        }
      ]
    });

    // Add click listener
    mapInstanceRef.current.addListener('click', (event: any) => {
      const lat = event.latLng.lat();
      const lng = event.latLng.lng();
      
      // Reverse geocoding to get address
      const geocoder = new window.google.maps.Geocoder();
      geocoder.geocode({ location: { lat, lng } }, (results: any[], status: string) => {
        if (status === 'OK' && results[0]) {
          const result = results[0];
          const location: LocationData = {
            address: result.formatted_address,
            latitude: lat,
            longitude: lng,
            city: result.address_components.find((comp: any) => 
              comp.types.includes('locality')
            )?.long_name,
            country: result.address_components.find((comp: any) => 
              comp.types.includes('country')
            )?.long_name
          };
          
          setSelectedLocation(location);
          updateMarker(lat, lng);
        }
      });
    });

    // Add initial marker if location exists
    if (selectedLocation) {
      updateMarker(selectedLocation.latitude, selectedLocation.longitude);
    }
  };

  const updateMarker = (lat: number, lng: number) => {
    if (markerRef.current) {
      markerRef.current.setMap(null);
    }

    markerRef.current = new window.google.maps.Marker({
      position: { lat, lng },
      map: mapInstanceRef.current,
      title: 'Selected Location',
      animation: window.google.maps.Animation.DROP
    });
  };

  const handleSearch = () => {
    if (!searchQuery.trim() || !window.google) {return;}

    setIsLoading(true);
    const geocoder = new window.google.maps.Geocoder();
    
    geocoder.geocode({ address: searchQuery }, (results: any[], status: string) => {
      setIsLoading(false);
      
      if (status === 'OK' && results[0]) {
        const result = results[0];
        const location = result.geometry.location;
        const lat = location.lat();
        const lng = location.lng();
        
        const locationData: LocationData = {
          address: result.formatted_address,
          latitude: lat,
          longitude: lng,
          city: result.address_components.find((comp: any) => 
            comp.types.includes('locality')
          )?.long_name,
          country: result.address_components.find((comp: any) => 
            comp.types.includes('country')
          )?.long_name
        };
        
        setSelectedLocation(locationData);
        
        // Center map on result
        mapInstanceRef.current.setCenter({ lat, lng });
        mapInstanceRef.current.setZoom(15);
        updateMarker(lat, lng);
      } else {
        alert('Location not found. Please try a different search term.');
      }
    });
  };

  const handleConfirm = () => {
    if (selectedLocation) {
      onSelect(selectedLocation);
      onClose();
    }
  };

  if (!isOpen) {return null;}

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-2 sm:p-4 overflow-hidden">
      <div className="bg-white rounded-lg w-full max-w-4xl max-h-[95vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-3 border-b border-gray-200 flex-shrink-0">
          <div className="flex items-center gap-2">
            <MapPin size={18} className="text-primary-600" />
            <h2 className="text-base font-semibold text-gray-900">Select Location</h2>
          </div>
          <button 
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X size={20} />
          </button>
        </div>

        {/* Search Bar */}
        <div className="p-3 border-b border-gray-200 flex-shrink-0">
          <div className="flex gap-2">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={16} />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                className="input-field pl-9 text-sm"
                placeholder="Search for a location..."
              />
            </div>
            <button
              onClick={handleSearch}
              disabled={isLoading || !searchQuery.trim()}
              className="btn-primary flex items-center gap-1 px-3 py-2 text-sm disabled:opacity-50"
            >
              <Search size={14} />
              {isLoading ? 'Searching...' : 'Search'}
            </button>
          </div>
        </div>

        {/* Map Area */}
        <div className="flex-1 p-3 overflow-hidden">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-3 h-full">
            {/* Google Map */}
            <div className="lg:col-span-2">
              <div 
                ref={mapRef}
                className="w-full h-full border border-gray-300 rounded-lg"
                style={{ 
                  minHeight: '250px',
                  maxHeight: '400px'
                }}
              >
                {!mapLoaded && (
                  <div className="w-full h-full bg-gray-100 flex items-center justify-center">
                    <div className="text-center text-gray-600">
                      <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600 mx-auto mb-2"></div>
                      <p className="text-xs">Loading map...</p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Location Details */}
            <div className="space-y-3">
              <div>
                <h3 className="text-xs font-medium text-gray-900 mb-2">Selected Location</h3>
                {selectedLocation ? (
                  <div className="bg-gray-50 p-2 rounded-lg">
                    <p className="text-xs font-medium text-gray-900 line-clamp-2">{selectedLocation.address}</p>
                    <p className="text-xs text-gray-600 mt-1">
                      {selectedLocation.latitude.toFixed(6)}, {selectedLocation.longitude.toFixed(6)}
                    </p>
                    {selectedLocation.city && (
                      <p className="text-xs text-gray-500 mt-1">
                        {selectedLocation.city}, {selectedLocation.country}
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="bg-gray-50 p-2 rounded-lg text-center">
                    <p className="text-xs text-gray-500">No location selected</p>
                    <p className="text-xs text-gray-400 mt-1">Click on the map or search</p>
                  </div>
                )}
              </div>

              {/* Instructions */}
              <div className="bg-blue-50 p-2 rounded-lg">
                <h4 className="text-xs font-medium text-blue-900 mb-1">How to use:</h4>
                <ul className="text-xs text-blue-800 space-y-0.5">
                  <li>• Search for a location</li>
                  <li>• Click on map to select</li>
                  <li>• Review details and confirm</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-2 p-3 border-t border-gray-200 flex-shrink-0">
          <button 
            onClick={onClose}
            className="btn-secondary px-4 py-2 text-sm"
          >
            Cancel
          </button>
          <button 
            onClick={handleConfirm}
            disabled={!selectedLocation}
            className="btn-primary flex items-center gap-2 px-4 py-2 text-sm disabled:opacity-50"
          >
            <Check size={14} />
            Confirm Selection
          </button>
        </div>
      </div>
    </div>
  );
}