import { useState, useEffect, useCallback } from 'react';
import api from '../api/client';

export function useNearbyEvents({ autoRequest = false, autoFetch = true } = {}) {
  const [coords, setCoords] = useState(null);
  const [geoSupported] = useState(() => Boolean(navigator.geolocation));
  const [geoLoading, setGeoLoading] = useState(false);
  const [geoError, setGeoError] = useState('');
  const [showAddressFallback, setShowAddressFallback] = useState(false);

  const [streetInput, setStreetInput] = useState('');
  const [cityInput, setCityInput] = useState('');
  const [stateInput, setStateInput] = useState('');
  const [zipInput, setZipInput] = useState('');
  const [countryInput, setCountryInput] = useState('');
  const [radiusKm, setRadiusKm] = useState(50);
  const [geocodingError, setGeocodingError] = useState('');
  const [geocoding, setGeocoding] = useState(false);

  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const requestGeolocation = useCallback(() => {
    if (!geoSupported) {
      setGeoError('Geolocation is not supported by your browser.');
      setShowAddressFallback(true);
      return;
    }
    setGeoLoading(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude });
        setGeoError('');
        setShowAddressFallback(false);
        setGeoLoading(false);
      },
      () => {
        setShowAddressFallback(true);
        setGeoError('');
        setGeoLoading(false);
      }
    );
  }, [geoSupported]);

  const handleAddressSearch = useCallback(async () => {
    if (!cityInput.trim() && !zipInput.trim()) {
      setGeocodingError('Enter at least a city or ZIP / postal code.');
      return;
    }
    setGeocoding(true);
    setGeocodingError('');
    try {
      const params = new URLSearchParams({ format: 'json', limit: '1' });
      if (streetInput.trim()) params.set('street', streetInput.trim());
      if (cityInput.trim()) params.set('city', cityInput.trim());
      if (stateInput.trim()) params.set('state', stateInput.trim());
      if (zipInput.trim()) params.set('postalcode', zipInput.trim());
      if (countryInput.trim()) params.set('country', countryInput.trim());
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?${params.toString()}`,
        { headers: { 'User-Agent': 'FairTix/1.0' } }
      );
      const results = await res.json();
      if (!results.length) {
        setGeocodingError('Address not found. Try removing some fields or check your spelling.');
        return;
      }
      setCoords({ lat: parseFloat(results[0].lat), lon: parseFloat(results[0].lon) });
      setShowAddressFallback(false);
      setPage(0);
    } catch {
      setGeocodingError('Unable to reach geocoding service. Try again.');
    } finally {
      setGeocoding(false);
    }
  }, [streetInput, cityInput, stateInput, zipInput, countryInput]);

  const clearCoords = useCallback(() => {
    setCoords(null);
    setShowAddressFallback(false);
    setGeoError('');
    setPage(0);
  }, []);

  useEffect(() => {
    if (!autoFetch || !coords) return;
    setLoading(true);
    setError('');
    const params = new URLSearchParams();
    params.set('lat', coords.lat);
    params.set('lon', coords.lon);
    params.set('radiusKm', radiusKm);
    params.set('page', page);
    params.set('size', pageSize);
    api.get(`/api/events/nearby?${params.toString()}`)
      .then((data) => {
        setEvents(data.content || []);
        setTotalPages(data.page?.totalPages || 0);
        setTotalElements(data.page?.totalElements || 0);
      })
      .catch((err) => {
        setError(err.message || 'Failed to load nearby events');
      })
      .finally(() => setLoading(false));
  }, [autoFetch, coords, radiusKm, page, pageSize]);

  useEffect(() => {
    if (autoRequest) requestGeolocation();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    coords,
    geoSupported,
    geoLoading,
    geoError,
    showAddressFallback,
    streetInput, setStreetInput,
    cityInput, setCityInput,
    stateInput, setStateInput,
    zipInput, setZipInput,
    countryInput, setCountryInput,
    radiusKm, setRadiusKm,
    geocodingError,
    geocoding,
    requestGeolocation,
    clearCoords,
    handleAddressSearch,
    events,
    loading,
    error,
    page, setPage,
    pageSize, setPageSize,
    totalPages,
    totalElements,
  };
}
