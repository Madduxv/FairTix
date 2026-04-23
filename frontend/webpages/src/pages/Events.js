import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { useNearbyEvents } from '../hooks/useNearbyEvents';
import '../styles/Events.css';

function haversineKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function Events() {
  useEffect(() => { document.title = 'Browse Events | FairTix'; }, []);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [silentCoords, setSilentCoords] = useState(null);

  // Filters
  const [titleSearch, setTitleSearch] = useState('');
  const [venueSearch, setVenueSearch] = useState('');
  const [performerSearch, setPerformerSearch] = useState('');
  const [showPast, setShowPast] = useState(false);

  // Pagination
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const {
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
  } = useNearbyEvents({ autoRequest: false, autoFetch: false });

  const nearMe = coords !== null;

  useEffect(() => {
    if (!navigator.geolocation) return;
    navigator.permissions?.query({ name: 'geolocation' }).then((result) => {
      if (result.state === 'granted') {
        navigator.geolocation.getCurrentPosition(
          (pos) => setSilentCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
          () => {}
        );
      }
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (coords) setSilentCoords(coords);
  }, [coords]);

  const handleNearMeToggle = () => {
    if (nearMe) {
      clearCoords();
      setPage(0);
    } else {
      requestGeolocation();
    }
  };

  const fetchEvents = useCallback(() => {
    setLoading(true);
    setError('');

    if (coords) {
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
      .finally(() => setLoading(false));
  }, [page, pageSize, titleSearch, venueSearch, performerSearch, showPast, coords, radiusKm]);

  const handleSearchChange = (setter) => (e) => {
    setter(e.target.value);
    setPage(0);
  };

  const handleShowPastToggle = () => {
    setShowPast((prev) => !prev);
    setPage(0);
  };

  const handlePageSizeChange = (e) => {
    setPageSize(Number(e.target.value));
    setPage(0);
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchEvents();
    }, 300);
    return () => clearTimeout(timer);
  }, [fetchEvents]);

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
            disabled={geoLoading}
          >
            {geoLoading ? 'Detecting…' : nearMe ? 'Near Me (on)' : 'Near Me'}
          </button>
        )}
      </div>
      {geoError && <div className="error-message">{geoError}</div>}

      {showAddressFallback && (
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
                  {(() => {
                    if (nearMe && event.distanceKm != null) {
                      return <span>{event.distanceKm.toFixed(1)} km away</span>;
                    }
                    const uc = silentCoords || coords;
                    if (!nearMe && uc && event.venue?.latitude != null && event.venue?.longitude != null) {
                      const d = haversineKm(uc.lat, uc.lon, event.venue.latitude, event.venue.longitude);
                      return <span>{d.toFixed(1)} km away</span>;
                    }
                    return null;
                  })()}
                </div>
                {event.status === 'PUBLISHED' && (
                  <div className="event-card-status event-card-status--announced">Coming Soon</div>
                )}
                {event.status === 'ACTIVE' && (
                  <div className="event-card-status event-card-status--active">On Sale</div>
                )}
                {event.maxTicketsPerUser && (
                  <div className="event-card-status event-card-status--cap">
                    Limit: {event.maxTicketsPerUser} per person
                  </div>
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
