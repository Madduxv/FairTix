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
  const [showPast, setShowPast] = useState(false);

  // Pagination
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchEvents = useCallback(() => {
    setLoading(true);
    setError('');

    const params = new URLSearchParams();
    params.set('page', page);
    params.set('size', pageSize);
    params.set('upcoming', !showPast);
    if (titleSearch.trim()) params.set('title', titleSearch.trim());
    if (venueSearch.trim()) params.set('venueName', venueSearch.trim());

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
  }, [page, pageSize, titleSearch, venueSearch, showPast]);

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
    return date.toLocaleDateString(undefined, {
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
        <label className="filter-toggle">
          <input
            type="checkbox"
            checked={showPast}
            onChange={handleShowPastToggle}
          />
          Show past events
        </label>
      </div>

      {loading && <div className="loading">Loading events...</div>}
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
                  <span>{event.venue}</span>
                  <span>{formatDate(event.startTime)}</span>
                </div>
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
