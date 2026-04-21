import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TablePagination from '@mui/material/TablePagination';
import Paper from '@mui/material/Paper';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Alert from '@mui/material/Alert';
import InputAdornment from '@mui/material/InputAdornment';
import Chip from '@mui/material/Chip';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ChairIcon from '@mui/icons-material/Chair';
import SearchIcon from '@mui/icons-material/Search';
import EventFormDialog from '../components/EventFormDialog';
import ConfirmDialog from '../components/ConfirmDialog';
import api from '../../api/client';

const STATUS_COLORS = {
  DRAFT: 'default',
  PUBLISHED: 'info',
  ACTIVE: 'success',
  COMPLETED: 'secondary',
  CANCELLED: 'error',
  ARCHIVED: 'warning',
};

const STATUS_LABELS = {
  DRAFT: 'Draft',
  PUBLISHED: 'Announced',
  ACTIVE: 'On Sale',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  ARCHIVED: 'Archived',
};

const ALL_STATUSES = ['DRAFT', 'PUBLISHED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'ARCHIVED'];

function AdminEventsPage() {
  const navigate = useNavigate();
  const [events, setEvents] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [seedMessage, setSeedMessage] = useState('');
  const [seedError, setSeedError] = useState('');
  const [seeding, setSeeding] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletingEvent, setDeletingEvent] = useState(null);

  // Cancel dialog state
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancellingEvent, setCancellingEvent] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelError, setCancelError] = useState('');

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams({ page: page.toString(), size: rowsPerPage.toString(), upcoming: 'false' });
      if (search.trim()) params.set('title', search.trim());
      if (statusFilter) params.set('status', statusFilter);
      const data = await api.get(`/api/events?${params}`);
      setEvents(data.content || []);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message || 'Failed to load events.');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, search, statusFilter]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const handleCreate = () => {
    setEditingEvent(null);
    setFormOpen(true);
  };

  const handleSeedDatabase = async () => {
    setSeeding(true);
    setSeedMessage('');
    setSeedError('');
    try {
      const response = await api.post('/api/admin/seed-events', {});
      setSeedMessage(response?.message || 'Seeding started.');
      fetchEvents();
    } catch (err) {
      setSeedError(err.message || 'Failed to seed database.');
    } finally {
      setSeeding(false);
    }
  };

  const handleEdit = (event) => {
    setEditingEvent(event);
    setFormOpen(true);
  };

  const handleDeleteClick = (event) => {
    setDeletingEvent(event);
    setDeleteOpen(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      await api.delete(`/api/events/${deletingEvent.id}`);
      setDeleteOpen(false);
      setDeletingEvent(null);
      fetchEvents();
    } catch (err) {
      setError(err.message || 'Failed to delete event.');
      setDeleteOpen(false);
    }
  };

  const handleLifecycleAction = async (eventId, action) => {
    try {
      await api.post(`/api/events/${eventId}/${action}`);
      fetchEvents();
    } catch (err) {
      setError(err.message || `Failed to ${action} event.`);
    }
  };

  const handleCancelClick = (event) => {
    setCancellingEvent(event);
    setCancelReason('');
    setCancelError('');
    setCancelOpen(true);
  };

  const handleCancelConfirm = async () => {
    if (!cancelReason.trim()) {
      setCancelError('A cancellation reason is required.');
      return;
    }
    try {
      await api.post(`/api/events/${cancellingEvent.id}/cancel`, { reason: cancelReason.trim() });
      setCancelOpen(false);
      setCancellingEvent(null);
      fetchEvents();
    } catch (err) {
      setCancelError(err.message || 'Failed to cancel event.');
    }
  };

  const handleSearchChange = (e) => {
    setSearch(e.target.value);
    setPage(0);
  };

  const handleStatusFilterChange = (e) => {
    setStatusFilter(e.target.value);
    setPage(0);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString();
  };

  const isEditable = (event) => !['COMPLETED', 'CANCELLED', 'ARCHIVED'].includes(event.status);

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Events
        </Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            onClick={handleSeedDatabase}
            disabled={seeding}
          >
            {seeding ? 'Seeding...' : 'Seed Database'}
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreate}>
            Create Event
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {seedError && <Alert severity="error" sx={{ mb: 2 }}>{seedError}</Alert>}
      {seedMessage && <Alert severity="success" sx={{ mb: 2 }}>{seedMessage}</Alert>}

      <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center' }}>
        <TextField
          placeholder="Search by title..."
          value={search}
          onChange={handleSearchChange}
          size="small"
          sx={{ width: 280 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>Status</InputLabel>
          <Select value={statusFilter} onChange={handleStatusFilterChange} label="Status">
            <MenuItem value="">All</MenuItem>
            {ALL_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>{STATUS_LABELS[s]}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>Venue</TableCell>
              <TableCell>Start Time</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center">Loading...</TableCell>
              </TableRow>
            ) : events.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">No events found.</TableCell>
              </TableRow>
            ) : (
              events.map((event) => (
                <TableRow key={event.id} hover>
                  <TableCell>{event.title}</TableCell>
                  <TableCell>{event.venue?.name ?? ''}</TableCell>
                  <TableCell>{formatDate(event.startTime)}</TableCell>
                  <TableCell>
                    <Chip
                      label={STATUS_LABELS[event.status] ?? event.status}
                      color={STATUS_COLORS[event.status] ?? 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="right">
                    {/* Lifecycle action buttons */}
                    {event.status === 'DRAFT' && (
                      <Tooltip title="Publish">
                        <Button size="small" variant="outlined" sx={{ mr: 0.5 }}
                          onClick={() => handleLifecycleAction(event.id, 'publish')}>
                          Publish
                        </Button>
                      </Tooltip>
                    )}
                    {event.status === 'PUBLISHED' && (
                      <Tooltip title="Activate (go on sale)">
                        <Button size="small" variant="outlined" color="success" sx={{ mr: 0.5 }}
                          onClick={() => handleLifecycleAction(event.id, 'activate')}>
                          Activate
                        </Button>
                      </Tooltip>
                    )}
                    {event.status === 'ACTIVE' && (
                      <Tooltip title="Mark as Completed">
                        <Button size="small" variant="outlined" color="secondary" sx={{ mr: 0.5 }}
                          onClick={() => handleLifecycleAction(event.id, 'complete')}>
                          Complete
                        </Button>
                      </Tooltip>
                    )}
                    {['COMPLETED', 'CANCELLED'].includes(event.status) && (
                      <Tooltip title="Archive">
                        <Button size="small" variant="outlined" color="warning" sx={{ mr: 0.5 }}
                          onClick={() => handleLifecycleAction(event.id, 'archive')}>
                          Archive
                        </Button>
                      </Tooltip>
                    )}
                    {['DRAFT', 'PUBLISHED', 'ACTIVE'].includes(event.status) && (
                      <Tooltip title="Cancel Event">
                        <Button size="small" variant="outlined" color="error" sx={{ mr: 0.5 }}
                          onClick={() => handleCancelClick(event)}>
                          Cancel
                        </Button>
                      </Tooltip>
                    )}

                    {/* Standard CRUD actions */}
                    {isEditable(event) && (
                      <Tooltip title="Edit">
                        <IconButton size="small" onClick={() => handleEdit(event)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                    <Tooltip title="Manage Seats">
                      <IconButton size="small" onClick={() => navigate(`/admin/events/${event.id}/seats`)}>
                        <ChairIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    {isEditable(event) && (
                      <Tooltip title="Delete">
                        <IconButton size="small" color="error" onClick={() => handleDeleteClick(event)}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[5, 10, 25]}
        />
      </TableContainer>

      <EventFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={fetchEvents}
        event={editingEvent}
      />

      <ConfirmDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        title="Delete Event"
        message={`Are you sure you want to delete "${deletingEvent?.title}"? This action cannot be undone.`}
      />

      {/* Cancel with reason dialog */}
      <Dialog open={cancelOpen} onClose={() => setCancelOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Cancel Event</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Cancelling <strong>{cancellingEvent?.title}</strong> will release all active seat holds
            and cancel all valid tickets. This cannot be undone without re-activating the event.
          </DialogContentText>
          <TextField
            label="Cancellation Reason"
            value={cancelReason}
            onChange={(e) => { setCancelReason(e.target.value); setCancelError(''); }}
            fullWidth
            multiline
            rows={3}
            error={!!cancelError}
            helperText={cancelError}
            autoFocus
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCancelOpen(false)}>Back</Button>
          <Button onClick={handleCancelConfirm} color="error" variant="contained">
            Cancel Event
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default AdminEventsPage;
