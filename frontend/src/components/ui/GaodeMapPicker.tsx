'use client';

import { useState, useEffect, useRef } from 'react';
import { X, MapPin, Search, Check, Navigation } from 'lucide-react';
import { useLanguage } from '@/contexts/LanguageContext';

interface GaodeMapPickerProps {
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
    AMap: any;
    initAMap: () => void;
  }
}

export default function GaodeMapPicker({ isOpen, onClose, onSelect, initialLocation }: GaodeMapPickerProps) {
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
  const { t: _t } = useLanguage();

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

  // Load Gaode Maps API
  useEffect(() => {
    if (isOpen) {
      if (!window.AMap) {
        const apiKey = process.env.NEXT_PUBLIC_GAODE_MAPS_API_KEY;
        console.log('Loading Gaode Maps with API key:', apiKey);
        
        const script = document.createElement('script');
        script.src = `https://webapi.amap.com/maps?v=2.0&key=${apiKey}&callback=initAMap`;
        script.async = true;
        script.defer = true;
        
        window.initAMap = () => {
          console.log('Gaode Maps API loaded successfully');
          console.log('AMap object:', window.AMap);
          setMapLoaded(true);
          // Small delay to ensure DOM is ready
          setTimeout(() => initializeMap(), 100);
        };
        
        document.head.appendChild(script);
        
        return () => {
          try {
            document.head.removeChild(script);
            delete (window as any).initAMap;
          } catch (error) {
            console.warn('Error cleaning up Gaode Maps resources:', error);
          }
        };
      } else {
        // Gaode Maps is already loaded
        setMapLoaded(true);
        // Small delay to ensure DOM is ready
        setTimeout(() => initializeMap(), 100);
      }
    }
  }, [isOpen]);

  // Auto-detect user location when map opens (if permission granted)
  useEffect(() => {
    if (isOpen && mapLoaded && !userLocation && !locationError) {
      // Small delay to ensure map is fully initialized
      setTimeout(() => {
        getUserLocation();
      }, 500);
    }
  }, [isOpen, mapLoaded]);

  // Cleanup map instance when modal closes
  useEffect(() => {
    if (!isOpen && mapInstanceRef.current) {
      console.log('Cleaning up map instance on modal close');
      try {
        mapInstanceRef.current.destroy();
      } catch (error) {
        console.warn('Error destroying map on close:', error);
      }
      mapInstanceRef.current = null;
      markerRef.current = null;
    }
  }, [isOpen]);

  // Reset state when modal closes
  useEffect(() => {
    if (!isOpen) {
      setMapLoaded(false);
      setUserLocation(null);
      setLocationError(null);
      setIsGettingLocation(false);
    }
  }, [isOpen]);

  // Handle window resize to ensure map stays properly sized
  useEffect(() => {
    if (isOpen && mapInstanceRef.current) {
      const handleResize = () => {
        if (mapInstanceRef.current) {
          setTimeout(() => {
            mapInstanceRef.current.getSize();
          }, 100);
        }
      };

      window.addEventListener('resize', handleResize);
      return () => window.removeEventListener('resize', handleResize);
    }
  }, [isOpen, mapInstanceRef.current]);

  const initializeMap = () => {
    if (!mapRef.current || !window.AMap) {
      console.log('Map initialization skipped:', { mapRef: !!mapRef.current, AMap: !!window.AMap });
      return;
    }

    console.log('Initializing Gaode map...');

    // Clean up existing map instance if it exists
    if (mapInstanceRef.current) {
      console.log('Destroying existing map instance');
      try {
        mapInstanceRef.current.destroy();
      } catch (error) {
        console.warn('Error destroying map:', error);
      }
      mapInstanceRef.current = null;
    }

    // Clear any existing markers
    if (markerRef.current) {
      markerRef.current = null;
    }

    const defaultCenter = selectedLocation 
      ? [selectedLocation.longitude, selectedLocation.latitude] // Gaode uses [lng, lat] format
      : userLocation
      ? [userLocation.longitude, userLocation.latitude]
      : [116.397428, 39.90923]; // Default to Beijing

    // Ensure the map container is properly sized
    const mapContainer = mapRef.current;
    mapContainer.style.width = '100%';
    mapContainer.style.height = '100%';
    mapContainer.style.minHeight = '250px';
    mapContainer.style.maxHeight = '400px';

    // Clear any existing content in the container
    mapContainer.innerHTML = '';

    try {
      // Create new map instance
      mapInstanceRef.current = new window.AMap.Map(mapContainer, {
        center: defaultCenter,
        zoom: 10,
        mapStyle: 'amap://styles/normal',
        features: ['bg', 'road', 'building', 'point'],
        viewMode: '3D',
        resizeEnable: true
      });

      console.log('Map instance created successfully');
    } catch (error) {
      console.error('Error creating map instance:', error);
      return;
    }

    // Add click listener
    mapInstanceRef.current.on('click', (event: any) => {
      const lng = event.lnglat.lng;
      const lat = event.lnglat.lat;
      
      console.log('Map clicked:', { lat, lng });
      
      // First set a temporary location with coordinates
      const tempLocation: LocationData = {
        address: `Getting address...`,
        latitude: lat,
        longitude: lng,
        city: 'Unknown',
        country: 'China'
      };
      setSelectedLocation(tempLocation);
      updateMarker(lat, lng);
      
      // Try reverse geocoding with Gaode API, fallback to OpenStreetMap if fails
      console.log('Starting reverse geocoding for:', { lat, lng });
      
      // First try Gaode Geocoder
      if (window.AMap && window.AMap.plugin) {
        window.AMap.plugin('AMap.Geocoder', () => {
          try {
            const geocoder = new window.AMap.Geocoder({
              city: '全国',
              radius: 1000,
              extensions: 'all'
            });
            
            geocoder.getAddress([lng, lat], (status: string, result: any) => {
              if (status === 'complete' && result && result.regeocode) {
                const addressComponents = result.regeocode.addressComponent;
                const formattedAddress = result.regeocode.formattedAddress;
                
                const location: LocationData = {
                  address: formattedAddress || `Location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
                  latitude: lat,
                  longitude: lng,
                  city: addressComponents.city || addressComponents.province || addressComponents.district || 'Unknown',
                  country: addressComponents.country || 'China'
                };
                
                console.log('Gaode geocoding successful:', location);
                setSelectedLocation(location);
                updateMarker(lat, lng);
              } else {
                console.log('Gaode geocoding failed, trying OpenStreetMap fallback');
                // Fallback to OpenStreetMap Nominatim
                fetchOpenStreetMapAddress(lat, lng);
              }
            });
          } catch (error) {
            console.log('Gaode geocoder error, trying OpenStreetMap fallback:', error);
            fetchOpenStreetMapAddress(lat, lng);
          }
        });
      } else {
        console.log('AMap not available, using OpenStreetMap fallback');
        fetchOpenStreetMapAddress(lat, lng);
      }
      
      // Fallback function using OpenStreetMap Nominatim
      function fetchOpenStreetMapAddress(lat: number, lng: number) {
        console.log('Using OpenStreetMap Nominatim for reverse geocoding');
        
        fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`)
          .then(response => response.json())
          .then(data => {
            if (data && data.display_name) {
              const location: LocationData = {
                address: data.display_name,
                latitude: lat,
                longitude: lng,
                city: data.address?.city || data.address?.town || data.address?.village || 'Unknown',
                country: data.address?.country || 'China'
              };
              
              console.log('OpenStreetMap geocoding successful:', location);
              setSelectedLocation(location);
              updateMarker(lat, lng);
            } else {
              console.log('OpenStreetMap geocoding failed, using coordinates');
              const location: LocationData = {
                address: `Location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
                latitude: lat,
                longitude: lng,
                city: 'Unknown',
                country: 'China'
              };
              setSelectedLocation(location);
              updateMarker(lat, lng);
            }
          })
          .catch(error => {
            console.error('OpenStreetMap geocoding error:', error);
            const location: LocationData = {
              address: `Location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
              latitude: lat,
              longitude: lng,
              city: 'Unknown',
              country: 'China'
            };
            setSelectedLocation(location);
            updateMarker(lat, lng);
          });
      }
    });

    // Force map to resize after a short delay
    setTimeout(() => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.getSize();
        mapInstanceRef.current.setCenter(defaultCenter);
        mapInstanceRef.current.setZoom(10);
      }
    }, 200);

    // Add initial marker if location exists
    if (selectedLocation) {
      updateMarker(selectedLocation.latitude, selectedLocation.longitude);
    }
  };

  const getUserLocation = () => {
    if (!navigator.geolocation) {
      setLocationError('浏览器不支持地理位置定位');
      return;
    }

    setIsGettingLocation(true);
    setLocationError(null);

    // Enhanced geolocation options for better accuracy
    const options = {
      enableHighAccuracy: true,
      timeout: 15000, // Increased timeout
      maximumAge: 60000 // 1 minute cache
    };

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        
        console.log('Got position:', { lat, lng, accuracy: position.coords.accuracy });
        
        try {
          // Try Gaode Geocoder first, fallback to OpenStreetMap
          if (window.AMap && window.AMap.plugin) {
            window.AMap.plugin('AMap.Geocoder', () => {
              const geocoder = new window.AMap.Geocoder({
                city: '全国',
                radius: 1000,
                extensions: 'all'
              });
              
              geocoder.getAddress([lng, lat], (status: string, result: any) => {
                if (status === 'complete' && result && result.regeocode) {
                  const addressComponents = result.regeocode.addressComponent;
                  const formattedAddress = result.regeocode.formattedAddress;
                  
                  const location: LocationData = {
                    address: formattedAddress || `Current location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
                    latitude: lat,
                    longitude: lng,
                    city: addressComponents.city || addressComponents.province || addressComponents.district || 'Unknown',
                    country: addressComponents.country || 'China'
                  };
                  
                  setUserLocation(location);
                  setIsGettingLocation(false);
                  
                  // Center map on user location
                  if (mapInstanceRef.current) {
                    mapInstanceRef.current.setCenter([lng, lat]);
                    mapInstanceRef.current.setZoom(15);
                    updateMarker(lat, lng);
                  }
                } else {
                  console.log('Gaode geocoding failed for user location, trying OpenStreetMap');
                  // Fallback to OpenStreetMap
                  fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`)
                    .then(response => response.json())
                    .then(data => {
                      setIsGettingLocation(false);
                      
                      if (data && data.display_name) {
                        const location: LocationData = {
                          address: data.display_name,
                          latitude: lat,
                          longitude: lng,
                          city: data.address?.city || data.address?.town || data.address?.village || '未知',
                          country: data.address?.country || '中国'
                        };
                        
                        setUserLocation(location);
                        
                        if (mapInstanceRef.current) {
                          mapInstanceRef.current.setCenter([lng, lat]);
                          mapInstanceRef.current.setZoom(15);
                          updateMarker(lat, lng);
                        }
                      } else {
                        const location: LocationData = {
                          address: `Current location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
                          latitude: lat,
                          longitude: lng,
                          city: 'Unknown',
                          country: 'China'
                        };
                        
                        setUserLocation(location);
                        
                        if (mapInstanceRef.current) {
                          mapInstanceRef.current.setCenter([lng, lat]);
                          mapInstanceRef.current.setZoom(15);
                          updateMarker(lat, lng);
                        }
                      }
                    })
                    .catch(error => {
                      console.error('OpenStreetMap geocoding error for user location:', error);
                      setIsGettingLocation(false);
                      
                      const location: LocationData = {
                        address: `Current location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
                        latitude: lat,
                        longitude: lng,
                        city: 'Unknown',
                        country: 'China'
                      };
                      
                      setUserLocation(location);
                      
                      if (mapInstanceRef.current) {
                        mapInstanceRef.current.setCenter([lng, lat]);
                        mapInstanceRef.current.setZoom(15);
                        updateMarker(lat, lng);
                      }
                    });
                }
              });
            });
          } else {
            console.log('AMap not available, using OpenStreetMap for user location');
            // Fallback to OpenStreetMap
            fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`)
              .then(response => response.json())
              .then(data => {
                setIsGettingLocation(false);
                
                if (data && data.display_name) {
                  const location: LocationData = {
                    address: data.display_name,
                    latitude: lat,
                    longitude: lng,
                    city: data.address?.city || data.address?.town || data.address?.village || '未知',
                    country: data.address?.country || '中国'
                  };
                  
                  setUserLocation(location);
                  
                  if (mapInstanceRef.current) {
                    mapInstanceRef.current.setCenter([lng, lat]);
                    mapInstanceRef.current.setZoom(15);
                    updateMarker(lat, lng);
                  }
                } else {
                  const location: LocationData = {
                    address: `Current location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
                    latitude: lat,
                    longitude: lng,
                    city: 'Unknown',
                    country: 'China'
                  };
                  
                  setUserLocation(location);
                  
                  if (mapInstanceRef.current) {
                    mapInstanceRef.current.setCenter([lng, lat]);
                    mapInstanceRef.current.setZoom(15);
                    updateMarker(lat, lng);
                  }
                }
              })
              .catch(error => {
                console.error('OpenStreetMap geocoding error for user location:', error);
                setIsGettingLocation(false);
                
                const location: LocationData = {
                  address: `Current location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
                  latitude: lat,
                  longitude: lng,
                  city: 'Unknown',
                  country: 'China'
                };
                
                setUserLocation(location);
                
                if (mapInstanceRef.current) {
                  mapInstanceRef.current.setCenter([lng, lat]);
                  mapInstanceRef.current.setZoom(15);
                  updateMarker(lat, lng);
                }
              });
          }
        } catch (error) {
          console.error('Location processing failed:', error);
          setIsGettingLocation(false);
          
          // Still set basic location
          const location: LocationData = {
            address: `Current location (${lat.toFixed(6)}, ${lng.toFixed(6)})`,
            latitude: lat,
            longitude: lng,
            city: 'Unknown',
            country: 'China'
          };
          
          setUserLocation(location);
          
          if (mapInstanceRef.current) {
            mapInstanceRef.current.setCenter([lng, lat]);
            mapInstanceRef.current.setZoom(15);
            updateMarker(lat, lng);
          }
        }
      },
      (error) => {
        setIsGettingLocation(false);
        console.error('Geolocation error:', error);
        
        switch (error.code) {
          case error.PERMISSION_DENIED:
            setLocationError('Location access denied by user. Please allow location access in browser settings.');
            break;
          case error.POSITION_UNAVAILABLE:
            setLocationError('Location information unavailable. Please check GPS or network connection.');
            break;
          case error.TIMEOUT:
            setLocationError('Location request timed out. Please try again.');
            break;
          default:
            setLocationError(`Error getting location: ${error.message}`);
            break;
        }
      },
      options
    );
  };

  const centerOnUserLocation = () => {
    if (userLocation && mapInstanceRef.current) {
      mapInstanceRef.current.setCenter([userLocation.longitude, userLocation.latitude]);
      mapInstanceRef.current.setZoom(15);
      updateMarker(userLocation.latitude, userLocation.longitude);
    } else {
      getUserLocation();
    }
  };

  const updateMarker = (lat: number, lng: number) => {
    if (markerRef.current) {
      markerRef.current.setMap(null);
    }

    markerRef.current = new window.AMap.Marker({
      position: [lng, lat],
      map: mapInstanceRef.current,
      title: '选中位置',
      icon: new window.AMap.Icon({
        size: new window.AMap.Size(25, 34),
        image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
        imageSize: new window.AMap.Size(25, 34)
      })
    });
  };

  const handleSearch = () => {
    if (!searchQuery.trim() || !window.AMap) {return;}

    setIsLoading(true);
    
    // Use Gaode Places API for search with plugin system
    if (window.AMap.plugin) {
      window.AMap.plugin('AMap.PlaceSearch', () => {
        const placeSearch = new window.AMap.PlaceSearch({
          city: '全国',
          pageSize: 1,
          pageIndex: 1
        });
        
        placeSearch.search(searchQuery, (status: string, result: any) => {
          setIsLoading(false);
          
          if (status === 'complete' && result.poiList && result.poiList.pois.length > 0) {
            const poi = result.poiList.pois[0];
            const location = poi.location;
            const lng = location.lng;
            const lat = location.lat;
            
            const locationData: LocationData = {
              address: poi.name + (poi.address ? ' - ' + poi.address : ''),
              latitude: lat,
              longitude: lng,
              city: poi.cityname,
              country: poi.adname || '中国'
            };
            
            setSelectedLocation(locationData);
            
            // Center map on result
            if (mapInstanceRef.current) {
              mapInstanceRef.current.setCenter([lng, lat]);
              mapInstanceRef.current.setZoom(15);
              updateMarker(lat, lng);
            }
          } else {
            alert('未找到该位置，请尝试其他搜索词。');
          }
        });
      });
    } else {
      setIsLoading(false);
      alert('搜索功能暂时不可用，请稍后重试。');
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
                <MapPin size={12} />
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
            {/* Gaode Map */}
            <div className="lg:col-span-2">
              <div 
                ref={mapRef}
                className="w-full h-full border border-gray-300 rounded-lg relative"
                style={{ 
                  minHeight: '250px',
                  maxHeight: '400px',
                  position: 'relative',
                  zIndex: 1
                }}
              >
                {!mapLoaded && (
                  <div className="w-full h-full bg-gray-100 flex items-center justify-center absolute inset-0">
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
                    <p className="text-xs text-gray-400 mt-1">Click on the map or search for a location</p>
                  </div>
                )}
              </div>

              {/* Instructions */}
              <div className="bg-blue-50 p-2 rounded-lg">
                <h4 className="text-xs font-medium text-blue-900 mb-1">How to use:</h4>
                <ul className="text-xs text-blue-800 space-y-0.5">
                  <li>• <strong>My Location</strong> - Automatically center on your current position</li>
                  <li>• Search for a location</li>
                  <li>• Click on map to select location</li>
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