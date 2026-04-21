import { Link, useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import iconUrl from 'leaflet/dist/images/marker-icon.png';
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';
import { useNearbyEvents } from '../hooks/useNearbyEvents';
import '../styles/Events.css';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({ iconUrl, iconRetinaUrl, shadowUrl });

function formatDate(isoString) {
  const date = new Date(isoString);
  return date.toLocaleString(undefined, {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function EventsNearYou() {
  const navigate = useNavigate();
  const {
    coords,
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
    handleAddressSearch,
    events,
    loading,
    error,
    page, setPage,
    pageSize, setPageSize,
    totalPages,
    totalElements,
  } = useNearbyEvents({ autoRequest: true });

  const handlePageSizeChange = (e) => {
    setPageSize(Number(e.target.value));
    setPage(0);
  };

  const mappableEvents = events.filter(
    (e) => e.venue?.latitude != null && e.venue?.longitude != null
  );

  return (
    <div className="events-page">
      <h2>Events Near You</h2>

      {geoLoading && (
        <div className="events-empty">
          <p>Detecting your location&hellip;</p>
        </div>
      )}

      {geoError && <div className="error-message">{geoError}</div>}

      {showAddressFallback && (
        <div className="city-fallback">
          <p className="city-fallback-title">
            Location access denied. Enter your address to find events nearby.
          </p>
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
              className="address-search-btn"
            >
              {geocoding ? 'Searching...' : 'Search Nearby'}
            </button>
          </div>
        </div>
      )}

      {coords && !showAddressFallback && (
        <div className="events-filters">
          <label htmlFor="near-you-radius" style={{ fontSize: '0.9rem', color: '#555', whiteSpace: 'nowrap' }}>
            Radius:
          </label>
          <select
            id="near-you-radius"
            value={radiusKm}
            onChange={(e) => { setRadiusKm(Number(e.target.value)); setPage(0); }}
            className="page-size-select address-radius-select"
          >
            <option value={10}>10 km</option>
            <option value={25}>25 km</option>
            <option value={50}>50 km</option>
            <option value={100}>100 km</option>
            <option value={200}>200 km</option>
          </select>
          <button type="button" onClick={requestGeolocation} className="address-search-btn">
            Re-detect location
          </button>
        </div>
      )}

      {coords && !loading && mappableEvents.length > 0 && (
        <div className="nearby-map-container">
          <MapContainer
            center={[coords.lat, coords.lon]}
            zoom={10}
            className="nearby-map"
            scrollWheelZoom={false}
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            />
            {mappableEvents.map((event) => (
              <Marker
                key={event.id}
                position={[event.venue.latitude, event.venue.longitude]}
              >
                <Popup>
                  <div className="nearby-map-popup">
                    <strong>{event.title}</strong>
                    <span>{event.venue.name}</span>
                    <span>{formatDate(event.startTime)}</span>
                    {event.distanceKm != null && (
                      <span>{event.distanceKm.toFixed(1)} km away</span>
                    )}
                    <button
                      type="button"
                      className="nearby-map-popup-btn"
                      onClick={() => navigate(`/events/${event.id}`)}
                    >
                      View details
                    </button>
                  </div>
                </Popup>
              </Marker>
            ))}
          </MapContainer>
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

      {!loading && !error && coords && events.length === 0 && (
        <div className="events-empty">
          <p>No events found within {radiusKm} km of your location.</p>
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
                  {event.distanceKm != null && (
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
              <span className="pagination-page">Page {page + 1} of {totalPages}</span>
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

export default EventsNearYou;
