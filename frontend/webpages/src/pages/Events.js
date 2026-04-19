import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import '../styles/Events.css';

function Events() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [titleSearch, setTitleSearch] = useState('');
  const [venueSearch, setVenueSearch] = useState('');
  const [performerSearch, setPerformerSearch] = useState('');
  const [showPast, setShowPast] = useState(false);

  // Geolocation
  const [nearMe, setNearMe] = useState(false);
  const [userCoords, setUserCoords] = useState(null); // { lat, lon }
  const [geoError, setGeoError] = useState('');
  const [geoSupported] = useState(() => Boolean(navigator.geolocation));

  // City/zip fallback (shown when geolocation is denied)
  const [showCityFallback, setShowCityFallback] = useState(false);
  const [streetInput, setStreetInput] = useState('');
  const [cityInput, setCityInput] = useState('');
  const [stateInput, setStateInput] = useState('');
  const [zipInput, setZipInput] = useState('');
  const [countryInput, setCountryInput] = useState('');
  const [radiusKm, setRadiusKm] = useState(50);
  const [geocodingError, setGeocodingError] = useState('');
  const [geocoding, setGeocoding] = useState(false);

  // Pagination
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchEvents = useCallback(() => {
    setLoading(true);
    setError('');

    if (nearMe && userCoords) {
      const params = new URLSearchParams();
      params.set('lat', userCoords.lat);
      params.set('lon', userCoords.lon);
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
      return;
    }

    const params = new URLSearchParams();
    params.set('page', page);
    params.set('size', pageSize);
    params.set('upcoming', !showPast);
    if (titleSearch.trim()) params.set('title', titleSearch.trim());
    if (venueSearch.trim()) params.set('venueName', venueSearch.trim());
    if (performerSearch.trim()) params.set('performerName', performerSearch.trim());

    api.get(`/api/events?${params.toString()}`)
      .then((data) => {
        setEvents(data.content || []);
        setTotalPages(data.page?.totalPages || 0);
        setTotalElements(data.page?.totalElements || 0);
      })
      .catch((err) => {
        setError(err.message || 'Failed to load events');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [page, pageSize, titleSearch, venueSearch, performerSearch, showPast, nearMe, userCoords, radiusKm]);

  const handleNearMeToggle = () => {
    if (nearMe) {
      setNearMe(false);
      setGeoError('');
      setShowCityFallback(false);
      setStreetInput('');
      setCityInput('');
      setStateInput('');
      setZipInput('');
      setCountryInput('');
      setGeocodingError('');
      setPage(0);
      return;
    }
    if (!geoSupported) {
      setGeoError('Geolocation is not supported by your browser.');
      setShowCityFallback(true);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setUserCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude });
        setNearMe(true);
        setGeoError('');
        setShowCityFallback(false);
        setPage(0);
      },
      () => {
        setShowCityFallback(true);
        setGeoError('');
      }
    );
  };

  async function handleAddressSearch() {
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
      setUserCoords({ lat: parseFloat(results[0].lat), lon: parseFloat(results[0].lon) });
      setNearMe(true);
      setShowCityFallback(false);
      setPage(0);
    } catch {
      setGeocodingError('Unable to reach geocoding service. Try again.');
    } finally {
      setGeocoding(false);
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchEvents();
    }, 300); // debounce search inputs
    return () => clearTimeout(timer);
  }, [fetchEvents]);

  const handleSearchChange = (setter) => (e) => {
    setter(e.target.value);
    setPage(0); // reset to first page on filter change
  };

  const handleShowPastToggle = () => {
    setShowPast((prev) => !prev);
    setPage(0);
  };

  const handlePageSizeChange = (e) => {
    setPageSize(Number(e.target.value));
    setPage(0);
  };

  const formatDate = (isoString) => {
    const date = new Date(isoString);
    return date.toLocaleString(undefined, {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="events-page">
      <h2>Events</h2>

      <div className="events-filters">
        <input
          type="text"
          placeholder="Search by title..."
          value={titleSearch}
          onChange={handleSearchChange(setTitleSearch)}
          className="filter-input"
        />
        <input
          type="text"
          placeholder="Filter by venue..."
          value={venueSearch}
          onChange={handleSearchChange(setVenueSearch)}
          className="filter-input"
        />
        <input
          type="text"
          placeholder="Filter by performer..."
          value={performerSearch}
          onChange={handleSearchChange(setPerformerSearch)}
          className="filter-input"
        />
        <label className="filter-toggle">
          <input
            type="checkbox"
            checked={showPast}
            onChange={handleShowPastToggle}
          />
          Show past events
        </label>
        {geoSupported && (
          <button
            className={`filter-toggle-btn${nearMe ? ' filter-toggle-btn--active' : ''}`}
            onClick={handleNearMeToggle}
            type="button"
          >
            {nearMe ? 'Near Me (on)' : 'Near Me'}
          </button>
        )}
      </div>
      {geoError && <div className="error-message">{geoError}</div>}

      {showCityFallback && !nearMe && (
        <div className="city-fallback">
          <p className="city-fallback-title">Location access denied. Enter your address to find events nearby.</p>
          <div className="city-fallback-form">
            <div className="address-field address-field--full">
              <label>Street address <span className="address-optional">(optional)</span></label>
              <input
                type="text"
                placeholder="123 Main St"
                value={streetInput}
                onChange={(e) => setStreetInput(e.target.value)}
                className="filter-input"
                disabled={geocoding}
              />
            </div>
            <div className="address-row">
              <div className="address-field">
                <label>City</label>
                <input
                  type="text"
                  placeholder="Austin"
                  value={cityInput}
                  onChange={(e) => setCityInput(e.target.value)}
                  className="filter-input"
                  disabled={geocoding}
                />
              </div>
              <div className="address-field">
                <label>State / Province <span className="address-optional">(optional)</span></label>
                <input
                  type="text"
                  placeholder="TX"
                  value={stateInput}
                  onChange={(e) => setStateInput(e.target.value)}
                  className="filter-input"
                  disabled={geocoding}
                />
              </div>
            </div>
            <div className="address-row">
              <div className="address-field">
                <label>ZIP / Postal code</label>
                <input
                  type="text"
                  placeholder="78701"
                  value={zipInput}
                  onChange={(e) => setZipInput(e.target.value)}
                  className="filter-input"
                  disabled={geocoding}
                />
              </div>
              <div className="address-field">
                <label>Country <span className="address-optional">(optional)</span></label>
                <input
                  type="text"
                  placeholder="US"
                  value={countryInput}
                  onChange={(e) => setCountryInput(e.target.value)}
                  className="filter-input"
                  disabled={geocoding}
                />
              </div>
            </div>
            <div className="address-field">
              <label>Search radius</label>
              <select
                value={radiusKm}
                onChange={(e) => setRadiusKm(Number(e.target.value))}
                className="page-size-select address-radius-select"
                disabled={geocoding}
              >
                <option value={10}>10 km</option>
                <option value={25}>25 km</option>
                <option value={50}>50 km</option>
                <option value={100}>100 km</option>
                <option value={200}>200 km</option>
              </select>
            </div>
            {geocodingError && <div className="error-message">{geocodingError}</div>}
            <button
              type="button"
              onClick={handleAddressSearch}
              disabled={geocoding || (!cityInput.trim() && !zipInput.trim())}
              className="filter-toggle-btn address-search-btn"
            >
              {geocoding ? 'Searching...' : 'Search Nearby'}
            </button>
          </div>
        </div>
      )}

      {loading && (
        <div className="events-grid">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="event-card-skeleton">
              <div className="skeleton-line skeleton-title" />
              <div className="skeleton-line skeleton-meta" />
              <div className="skeleton-line skeleton-action" />
            </div>
          ))}
        </div>
      )}
      {error && <div className="error-message">{error}</div>}

      {!loading && !error && events.length === 0 && (
        <div className="events-empty">
          <p>No events match your search.</p>
        </div>
      )}

      {!loading && !error && events.length > 0 && (
        <>
          <div className="events-grid">
            {events.map((event) => (
              <Link key={event.id} to={`/events/${event.id}`} className="event-card">
                <h3>{event.title}</h3>
                <div className="event-card-meta">
                  <span>{event.venue?.name ?? ''}</span>
                  <span>{formatDate(event.startTime)}</span>
                  {nearMe && event.distanceKm != null && (
                    <span>{event.distanceKm.toFixed(1)} km away</span>
                  )}
                </div>
                {event.status === 'PUBLISHED' && (
                  <div className="event-card-status event-card-status--announced">Coming Soon</div>
                )}
                {event.status === 'ACTIVE' && (
                  <div className="event-card-status event-card-status--active">On Sale</div>
                )}
                <div className="event-card-action">View details &rarr;</div>
              </Link>
            ))}
          </div>

          <div className="events-pagination">
            <div className="pagination-info">
              Showing {page * pageSize + 1}–{Math.min((page + 1) * pageSize, totalElements)} of {totalElements} events
            </div>
            <div className="pagination-controls">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                Previous
              </button>
              <span className="pagination-page">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                Next
              </button>
              <select value={pageSize} onChange={handlePageSizeChange} className="page-size-select">
                <option value={10}>10 / page</option>
                <option value={20}>20 / page</option>
                <option value={50}>50 / page</option>
              </select>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default Events;
