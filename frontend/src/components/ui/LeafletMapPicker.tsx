'use client';

import { useState, useEffect, useRef } from 'react';
import { X, MapPin, Search, Check, Navigation, MapPinIcon } from 'lucide-react';

interface LeafletMapPickerProps {
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
    L: any;
  }
}

export default function LeafletMapPicker({ isOpen, onClose, onSelect, initialLocation }: LeafletMapPickerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedLocation, setSelectedLocation] = useState<LocationData | null>(initialLocation || null);
  const [isLoading, setIsLoading] = useState(false);
  const [mapLoaded, setMapLoaded] = useState(false);
  const [userLocation, setUserLocation] = useState<LocationData | null>(null);
  const [isGettingLocation, setIsGettingLocation] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);
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

  // Load Leaflet CSS and JS
  useEffect(() => {
    if (isOpen) {
      if (!window.L) {
        // Load Leaflet CSS
        const cssLink = document.createElement('link');
        cssLink.rel = 'stylesheet';
        cssLink.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
        cssLink.integrity = 'sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=';
        cssLink.crossOrigin = '';
        document.head.appendChild(cssLink);

        // Load Leaflet JS
        const script = document.createElement('script');
        script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
        script.integrity = 'sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=';
        script.crossOrigin = '';
        script.onload = () => {
          setMapLoaded(true);
          // Small delay to ensure DOM is ready
          setTimeout(() => initializeMap(), 100);
        };
        document.head.appendChild(script);

        return () => {
          // Clean up script and CSS when component unmounts
          try {
            if (document.head.contains(cssLink)) {
              document.head.removeChild(cssLink);
            }
            if (document.head.contains(script)) {
              document.head.removeChild(script);
            }
          } catch (error) {
            console.warn('Error cleaning up Leaflet resources:', error);
          }
        };
      } else {
        // Leaflet is already loaded
        setMapLoaded(true);
        // Small delay to ensure DOM is ready
        setTimeout(() => initializeMap(), 100);
      }
    }
  }, [isOpen]);

  // Auto-detect user location when map opens (if permission granted)
  useEffect(() => {
    if (isOpen && mapLoaded && !userLocation && !locationError) {
      // Try to get location automatically (will show permission prompt)
      getUserLocation();
    }
  }, [isOpen, mapLoaded]);

  // Cleanup map instance when modal closes
  useEffect(() => {
    if (!isOpen && mapInstanceRef.current) {
      // Clean up map instance when modal closes
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
      markerRef.current = null;
    }
  }, [isOpen]);

  // Handle window resize to ensure map stays properly sized
  useEffect(() => {
    if (isOpen && mapInstanceRef.current) {
      const handleResize = () => {
        if (mapInstanceRef.current) {
          setTimeout(() => {
            mapInstanceRef.current.invalidateSize();
          }, 100);
        }
      };

      window.addEventListener('resize', handleResize);
      return () => window.removeEventListener('resize', handleResize);
    }
  }, [isOpen, mapInstanceRef.current]);

  const initializeMap = () => {
    if (!mapRef.current || !window.L) {return;}

    // Clean up existing map instance if it exists
    if (mapInstanceRef.current) {
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
    }

    // Clear any existing markers
    if (markerRef.current) {
      markerRef.current = null;
    }

    const defaultCenter = selectedLocation 
      ? [selectedLocation.latitude, selectedLocation.longitude]
      : userLocation
      ? [userLocation.latitude, userLocation.longitude]
      : [39.9042, 116.4074]; // Default to Beijing

    // Ensure the map container is properly sized
    const mapContainer = mapRef.current;
    mapContainer.style.width = '100%';
    mapContainer.style.height = '100%';
    mapContainer.style.minHeight = '250px';

    // Create new map instance with basic options
    mapInstanceRef.current = window.L.map(mapContainer).setView(defaultCenter, 10);

    // Add OpenStreetMap tiles
    window.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(mapInstanceRef.current);

    // Add click listener
    mapInstanceRef.current.on('click', (e: any) => {
      const lat = e.latlng.lat;
      const lng = e.latlng.lng;
      
      // Simple reverse geocoding using Nominatim (free)
      reverseGeocode(lat, lng);
    });

    // Force map to resize and invalidate size after a short delay
    setTimeout(() => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.invalidateSize();
        mapInstanceRef.current.setView(defaultCenter, 10);
      }
    }, 200);

    // Add initial marker if location exists
    if (selectedLocation) {
      updateMarker(selectedLocation.latitude, selectedLocation.longitude);
    }
  };

  const getUserLocation = () => {
    if (!navigator.geolocation) {
      setLocationError('Geolocation is not supported by this browser');
      return;
    }

    setIsGettingLocation(true);
    setLocationError(null);

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        
        try {
          // Get address for user's location
          const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`
          );
          const data = await response.json();
          
          const location: LocationData = {
            address: data.display_name || `Current Location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
            latitude: lat,
            longitude: lng,
            city: data.address?.city || data.address?.town || data.address?.village,
            country: data.address?.country
          };
          
          setUserLocation(location);
          
          // Center map on user location
          if (mapInstanceRef.current) {
            mapInstanceRef.current.setView([lat, lng], 15);
            updateMarker(lat, lng);
          }
          
          setIsGettingLocation(false);
        } catch (error) {
          console.error('Reverse geocoding failed:', error);
          // Still set location even if reverse geocoding fails
          const location: LocationData = {
            address: `Current Location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
            latitude: lat,
            longitude: lng
          };
          
          setUserLocation(location);
          
          if (mapInstanceRef.current) {
            mapInstanceRef.current.setView([lat, lng], 15);
            updateMarker(lat, lng);
          }
          
          setIsGettingLocation(false);
        }
      },
      (error) => {
        setIsGettingLocation(false);
        switch (error.code) {
          case error.PERMISSION_DENIED:
            setLocationError('Location access denied by user');
            break;
          case error.POSITION_UNAVAILABLE:
            setLocationError('Location information unavailable');
            break;
          case error.TIMEOUT:
            setLocationError('Location request timed out');
            break;
          default:
            setLocationError('An unknown error occurred');
            break;
        }
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000 // 5 minutes
      }
    );
  };

  const centerOnUserLocation = () => {
    if (userLocation && mapInstanceRef.current) {
      mapInstanceRef.current.setView([userLocation.latitude, userLocation.longitude], 15);
      updateMarker(userLocation.latitude, userLocation.longitude);
    } else {
      getUserLocation();
    }
  };

  const reverseGeocode = async (lat: number, lng: number) => {
    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`
      );
      const data = await response.json();
      
      if (data && data.display_name) {
        const location: LocationData = {
          address: data.display_name,
          latitude: lat,
          longitude: lng,
          city: data.address?.city || data.address?.town || data.address?.village,
          country: data.address?.country
        };
        
        setSelectedLocation(location);
        updateMarker(lat, lng);
      }
    } catch (error) {
      console.error('Reverse geocoding failed:', error);
      // Fallback to coordinates only
      const location: LocationData = {
        address: `Location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
        latitude: lat,
        longitude: lng
      };
      setSelectedLocation(location);
      updateMarker(lat, lng);
    }
  };

  const updateMarker = (lat: number, lng: number) => {
    if (markerRef.current) {
      mapInstanceRef.current.removeLayer(markerRef.current);
    }

    markerRef.current = window.L.marker([lat, lng])
      .addTo(mapInstanceRef.current)
      .bindPopup('Selected Location')
      .openPopup();
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) {return;}

    setIsLoading(true);
    try {
      // Use Nominatim for geocoding (free)
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchQuery)}&limit=1&addressdetails=1`
      );
      const data = await response.json();
      
      if (data && data.length > 0) {
        const result = data[0];
        const lat = parseFloat(result.lat);
        const lng = parseFloat(result.lon);
        
        const locationData: LocationData = {
          address: result.display_name,
          latitude: lat,
          longitude: lng,
          city: result.address?.city || result.address?.town || result.address?.village,
          country: result.address?.country
        };
        
        setSelectedLocation(locationData);
        
        // Center map on result
        mapInstanceRef.current.setView([lat, lng], 15);
        updateMarker(lat, lng);
      } else {
        alert('Location not found. Please try a different search term.');
      }
    } catch (error) {
      console.error('Search failed:', error);
      alert('Search failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
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
            <button
              onClick={centerOnUserLocation}
              disabled={isGettingLocation}
              className="btn-outline flex items-center gap-1 px-3 py-2 text-sm disabled:opacity-50"
              title="Use my current location"
            >
              <Navigation size={14} />
              {isGettingLocation ? 'Locating...' : 'My Location'}
            </button>
          </div>
          
          {/* Location Error Display */}
          {locationError && (
            <div className="mt-2 p-2 bg-red-50 border border-red-200 rounded text-xs text-red-800">
              <div className="flex items-center gap-2">
                <MapPinIcon size={12} />
                <span>{locationError}</span>
              </div>
            </div>
          )}
          
          {/* User Location Display */}
          {userLocation && (
            <div className="mt-2 p-2 bg-green-50 border border-green-200 rounded text-xs text-green-800">
              <div className="flex items-center gap-2">
                <Navigation size={12} />
                <span><strong>Your location:</strong> {userLocation.address}</span>
              </div>
            </div>
          )}
        </div>

        {/* Map Area */}
        <div className="flex-1 p-3 overflow-hidden">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-3 h-full">
            {/* Leaflet Map */}
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
                  <li>• <strong>My Location</strong> - Auto center on your position</li>
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