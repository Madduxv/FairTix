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
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import ConfirmDialog from '../components/ConfirmDialog';
import api from '../../api/client';

function PerformerFormDialog({ open, onClose, onSaved, performer }) {
  const isEdit = Boolean(performer);
  const [name, setName] = useState('');
  const [genre, setGenre] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (performer) {
      setName(performer.name || '');
      setGenre(performer.genre || '');
    } else {
      setName('');
      setGenre('');
    }
    setError('');
  }, [performer, open]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Performer name is required.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const payload = {
        name: name.trim(),
        genre: genre.trim() || null,
      };
      if (isEdit) {
        await api.put(`/api/performers/${performer.id}`, payload);
      } else {
        await api.post('/api/performers', payload);
      }
      onSaved();
      onClose();
    } catch (err) {
      if (err.status === 409 || (err.message && err.message.includes('409'))) {
        setError('A performer with this name already exists.');
      } else {
        setError(err.message || 'Failed to save performer.');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>{isEdit ? 'Edit Performer' : 'New Performer'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              fullWidth
            />
            <TextField
              label="Genre (optional)"
              value={genre}
              onChange={(e) => setGenre(e.target.value)}
              fullWidth
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

function AdminPerformersPage() {
  const [performers, setPerformers] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [formOpen, setFormOpen] = useState(false);
  const [editingPerformer, setEditingPerformer] = useState(null);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletingPerformer, setDeletingPerformer] = useState(null);
  const [deleteError, setDeleteError] = useState('');

  const fetchPerformers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.get(`/api/performers?page=${page}&size=${rowsPerPage}`);
      setPerformers(data.content || []);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message || 'Failed to load performers.');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage]);

  useEffect(() => {
    fetchPerformers();
  }, [fetchPerformers]);

  const handleCreate = () => {
    setEditingPerformer(null);
    setFormOpen(true);
  };

  const handleEdit = (performer) => {
    setEditingPerformer(performer);
    setFormOpen(true);
  };

  const handleDeleteClick = (performer) => {
    setDeletingPerformer(performer);
    setDeleteError('');
    setDeleteOpen(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      await api.delete(`/api/performers/${deletingPerformer.id}`);
      setDeleteOpen(false);
      setDeletingPerformer(null);
      fetchPerformers();
    } catch (err) {
      setDeleteError(err.message || 'Failed to delete performer.');
      setDeleteOpen(false);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Performers
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreate}>
          New Performer
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {deleteError && <Alert severity="error" sx={{ mb: 2 }}>{deleteError}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Genre</TableCell>
              <TableCell>Created</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={4} align="center">Loading...</TableCell>
              </TableRow>
            ) : performers.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} align="center">No performers found.</TableCell>
              </TableRow>
            ) : (
              performers.map((performer) => (
                <TableRow key={performer.id} hover>
                  <TableCell>{performer.name}</TableCell>
                  <TableCell>{performer.genre || '—'}</TableCell>
                  <TableCell>
                    {performer.createdAt ? new Date(performer.createdAt).toLocaleDateString() : '—'}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Edit">
                      <IconButton size="small" onClick={() => handleEdit(performer)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton size="small" color="error" onClick={() => handleDeleteClick(performer)}>
                        <DeleteOutlineIcon fontSize="small" />
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

      <PerformerFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={fetchPerformers}
        performer={editingPerformer}
      />

      <ConfirmDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        title="Delete Performer"
        message={`Delete "${deletingPerformer?.name}"? This removes them from all events.`}
      />
    </Box>
  );
}

export default AdminPerformersPage;
