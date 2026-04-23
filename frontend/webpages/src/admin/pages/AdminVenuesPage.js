import React, { useState, useEffect, useCallback } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
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
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ConfirmDialog from '../components/ConfirmDialog';
import api from '../../api/client';

function VenueFormDialog({ open, onClose, onSaved, venue }) {
  const isEdit = Boolean(venue);
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [country, setCountry] = useState('');
  const [capacity, setCapacity] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (venue) {
      setName(venue.name || '');
      setAddress(venue.address || '');
      setCity(venue.city || '');
      setCountry(venue.country || '');
      setCapacity(venue.capacity != null ? String(venue.capacity) : '');
      setLatitude(venue.latitude != null ? String(venue.latitude) : '');
      setLongitude(venue.longitude != null ? String(venue.longitude) : '');
    } else {
      setName('');
      setAddress('');
      setCity('');
      setCountry('');
      setCapacity('');
      setLatitude('');
      setLongitude('');
    }
    setError('');
  }, [venue, open]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Venue name is required.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const payload = {
        name: name.trim(),
        address: address.trim() || null,
        city: city.trim() || null,
        country: country.trim() || null,
        capacity: capacity !== '' ? parseInt(capacity, 10) : null,
        latitude: latitude !== '' ? parseFloat(latitude) : null,
        longitude: longitude !== '' ? parseFloat(longitude) : null,
      };
      if (isEdit) {
        await api.put(`/api/venues/${venue.id}`, payload);
      } else {
        await api.post('/api/venues', payload);
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to save venue.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>{isEdit ? 'Edit Venue' : 'Create Venue'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} required fullWidth />
            <TextField label="Address" value={address} onChange={(e) => setAddress(e.target.value)} fullWidth />
            <TextField label="City" value={city} onChange={(e) => setCity(e.target.value)} fullWidth />
            <TextField label="Country" value={country} onChange={(e) => setCountry(e.target.value)} fullWidth />
            <TextField
              label="Capacity"
              type="number"
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
              fullWidth
              inputProps={{ min: 1 }}
            />
            <TextField
              label="Latitude (optional)"
              type="number"
              value={latitude}
              onChange={(e) => setLatitude(e.target.value)}
              fullWidth
              inputProps={{ step: 'any', min: -90, max: 90 }}
              helperText="WGS84 decimal degrees, e.g. 40.750504"
            />
            <TextField
              label="Longitude (optional)"
              type="number"
              value={longitude}
              onChange={(e) => setLongitude(e.target.value)}
              fullWidth
              inputProps={{ step: 'any', min: -180, max: 180 }}
              helperText="WGS84 decimal degrees, e.g. -73.993439"
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={saving}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving}>
            {saving ? 'Saving...' : isEdit ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

function AdminVenuesPage() {
  const [venues, setVenues] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [formOpen, setFormOpen] = useState(false);
  const [editingVenue, setEditingVenue] = useState(null);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletingVenue, setDeletingVenue] = useState(null);

  const fetchVenues = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.get(`/api/venues?page=${page}&size=${rowsPerPage}`);
      setVenues(data.content || []);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message || 'Failed to load venues.');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage]);

  useEffect(() => {
    fetchVenues();
  }, [fetchVenues]);

  const handleCreate = () => {
    setEditingVenue(null);
    setFormOpen(true);
  };

  const handleEdit = (venue) => {
    setEditingVenue(venue);
    setFormOpen(true);
  };

  const handleDeleteClick = (venue) => {
    setDeletingVenue(venue);
    setDeleteOpen(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      await api.delete(`/api/venues/${deletingVenue.id}`);
      setDeleteOpen(false);
      setDeletingVenue(null);
      fetchVenues();
    } catch (err) {
      setError(err.message || 'Failed to delete venue.');
      setDeleteOpen(false);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Venues
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreate}>
          Create Venue
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>City</TableCell>
              <TableCell>Country</TableCell>
              <TableCell>Capacity</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center">Loading...</TableCell>
              </TableRow>
            ) : venues.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">No venues found.</TableCell>
              </TableRow>
            ) : (
              venues.map((venue) => (
                <TableRow key={venue.id} hover>
                  <TableCell>{venue.name}</TableCell>
                  <TableCell>{venue.city || '—'}</TableCell>
                  <TableCell>{venue.country || '—'}</TableCell>
                  <TableCell>{venue.capacity != null ? venue.capacity.toLocaleString() : '—'}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Edit">
                      <IconButton size="small" onClick={() => handleEdit(venue)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton size="small" color="error" onClick={() => handleDeleteClick(venue)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
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

      <VenueFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={fetchVenues}
        venue={editingVenue}
      />

      <ConfirmDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        title="Delete Venue"
        message={`Are you sure you want to delete "${deletingVenue?.name}"? This cannot be undone. Venues with associated events cannot be deleted.`}
      />
    </Box>
  );
}

export default AdminVenuesPage;
