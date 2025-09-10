'use client';

import { useState, useEffect, useRef } from 'react';
import { X, MapPin, Search, Check } from 'lucide-react';

interface MapPickerProps {
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

export default function MapPicker({ isOpen, onClose, onSelect, initialLocation }: MapPickerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedLocation, setSelectedLocation] = useState<LocationData | null>(initialLocation || null);
  const [mapCenter, setMapCenter] = useState({ lat: 39.9042, lng: 116.4074 }); // Default to Beijing
  const [isLoading, setIsLoading] = useState(false);
  const mapRef = useRef<HTMLDivElement>(null);

  // Initialize map when component opens
  useEffect(() => {
    if (isOpen && mapRef.current) {
      initializeMap();
    }
  }, [isOpen]);

  const initializeMap = () => {
    // For now, we'll create a simple interactive map using HTML5 geolocation
    // In a real implementation, you would integrate with Google Maps, OpenStreetMap, or similar
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setMapCenter({
            lat: position.coords.latitude,
            lng: position.coords.longitude
          });
        },
        (_error) => {
          // Geolocation error - keep default location
          // Keep default location
        }
      );
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) {return;}

    setIsLoading(true);
    try {
      // Simulate geocoding API call
      // In a real implementation, you would use Google Geocoding API or similar
      const mockLocation: LocationData = {
        address: searchQuery,
        latitude: mapCenter.lat + (Math.random() - 0.5) * 0.01,
        longitude: mapCenter.lng + (Math.random() - 0.5) * 0.01,
        city: 'Mock City',
        country: 'Mock Country'
      };
      
      setSelectedLocation(mockLocation);
      setMapCenter({ lat: mockLocation.latitude, lng: mockLocation.longitude });
    } catch (error) {
      console.error('Search error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleMapClick = (event: React.MouseEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    
    // Convert click coordinates to lat/lng (simplified)
    const lat = mapCenter.lat + (y - rect.height / 2) * 0.001;
    const lng = mapCenter.lng + (x - rect.width / 2) * 0.001;
    
    const location: LocationData = {
      address: `Selected Location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
      latitude: lat,
      longitude: lng,
      city: 'Selected City',
      country: 'Selected Country'
    };
    
    setSelectedLocation(location);
  };

  const handleConfirm = () => {
    if (selectedLocation) {
      onSelect(selectedLocation);
      onClose();
    }
  };

  if (!isOpen) {return null;}

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <div className="flex items-center gap-2">
            <MapPin size={20} className="text-primary-600" />
            <h2 className="text-lg font-semibold text-gray-900">Select Location</h2>
          </div>
          <button 
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X size={24} />
          </button>
        </div>

        {/* Search Bar */}
        <div className="p-4 border-b border-gray-200">
          <div className="flex gap-2">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={18} />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                className="input-field pl-10"
                placeholder="Search for a location..."
              />
            </div>
            <button
              onClick={handleSearch}
              disabled={isLoading || !searchQuery.trim()}
              className="btn-primary flex items-center gap-2 disabled:opacity-50"
            >
              <Search size={16} />
              {isLoading ? 'Searching...' : 'Search'}
            </button>
          </div>
        </div>

        {/* Map Area */}
        <div className="flex-1 p-4">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 h-96">
            {/* Map */}
            <div className="lg:col-span-2">
              <div 
                ref={mapRef}
                className="w-full h-full bg-gray-100 border border-gray-300 rounded-lg relative cursor-crosshair overflow-hidden"
                onClick={handleMapClick}
              >
                {/* Simple map representation */}
                <div className="absolute inset-0 bg-gradient-to-br from-blue-100 to-green-100 flex items-center justify-center">
                  <div className="text-center text-gray-600">
                    <MapPin size={48} className="mx-auto mb-2 text-primary-600" />
                    <p className="text-sm">Click on the map to select a location</p>
                    <p className="text-xs mt-1">Center: {mapCenter.lat.toFixed(4)}, {mapCenter.lng.toFixed(4)}</p>
                  </div>
                </div>
                
                {/* Selected location marker */}
                {selectedLocation && (
                  <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
                    <div className="w-4 h-4 bg-red-500 rounded-full border-2 border-white shadow-lg"></div>
                  </div>
                )}
              </div>
            </div>

            {/* Location Details */}
            <div className="space-y-4">
              <div>
                <h3 className="text-sm font-medium text-gray-900 mb-2">Selected Location</h3>
                {selectedLocation ? (
                  <div className="bg-gray-50 p-3 rounded-lg">
                    <p className="text-sm font-medium text-gray-900">{selectedLocation.address}</p>
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
                  <div className="bg-gray-50 p-3 rounded-lg text-center">
                    <p className="text-sm text-gray-500">No location selected</p>
                    <p className="text-xs text-gray-400 mt-1">Click on the map or search for a location</p>
                  </div>
                )}
              </div>

              {/* Instructions */}
              <div className="bg-blue-50 p-3 rounded-lg">
                <h4 className="text-sm font-medium text-blue-900 mb-2">How to use:</h4>
                <ul className="text-xs text-blue-800 space-y-1">
                  <li>• Search for a location using the search bar</li>
                  <li>• Click anywhere on the map to select a location</li>
                  <li>• Review the selected location details</li>
                  <li>• Click &quot;Confirm Selection&quot; to save</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 p-4 border-t border-gray-200">
          <button 
            onClick={onClose}
            className="btn-secondary"
          >
            Cancel
          </button>
          <button 
            onClick={handleConfirm}
            disabled={!selectedLocation}
            className="btn-primary flex items-center gap-2 disabled:opacity-50"
          >
            <Check size={16} />
            Confirm Selection
          </button>
        </div>
      </div>
    </div>
  );
}