import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
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
import Chip from '@mui/material/Chip';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Alert from '@mui/material/Alert';
import api from '../../api/client';

const STATUS_COLORS = {
  OPEN: 'error',
  IN_PROGRESS: 'warning',
  WAITING_ON_USER: 'secondary',
  RESOLVED: 'success',
  CLOSED: 'default',
};

const STATUS_LABELS = {
  OPEN: 'Open',
  IN_PROGRESS: 'In Progress',
  WAITING_ON_USER: 'Waiting on User',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
};

const PRIORITY_COLORS = {
  LOW: 'default',
  NORMAL: 'info',
  HIGH: 'warning',
  URGENT: 'error',
};

const ALL_STATUSES = ['OPEN', 'IN_PROGRESS', 'WAITING_ON_USER', 'RESOLVED', 'CLOSED'];
const ALL_PRIORITIES = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];

function AdminSupportPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const userIdFilter = searchParams.get('userId') || '';
  const [tickets, setTickets] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [editTicket, setEditTicket] = useState(null);
  const [editStatus, setEditStatus] = useState('');
  const [editPriority, setEditPriority] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  const fetchTickets = useCallback(() => {
    setLoading(true);
    setError('');
    const params = new URLSearchParams({ page });
    if (statusFilter) params.set('status', statusFilter);
    if (userIdFilter) params.set('userId', userIdFilter);
    api.get(`/api/admin/support/tickets?${params}`)
      .then((data) => {
        setTickets(data?.content || []);
        setTotal(data?.page?.totalElements || 0);
      })
      .catch((err) => setError(err.message || 'Failed to load support tickets'))
      .finally(() => setLoading(false));
  }, [page, statusFilter, userIdFilter]);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  function openEdit(ticket) {
    setEditTicket(ticket);
    setEditStatus(ticket.status);
    setEditPriority(ticket.priority);
    setSaveError('');
  }

  function closeEdit() {
    setEditTicket(null);
  }

  async function handleSave() {
    setSaving(true);
    setSaveError('');
    try {
      await api.patch(`/api/admin/support/tickets/${editTicket.id}`, {
        status: editStatus,
        priority: editPriority,
      });
      closeEdit();
      fetchTickets();
    } catch (err) {
      setSaveError(err.message || 'Failed to update ticket');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>Support Tickets</Typography>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Filter by Status</InputLabel>
          <Select
            value={statusFilter}
            label="Filter by Status"
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          >
            <MenuItem value="">All Statuses</MenuItem>
            {ALL_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>{STATUS_LABELS[s]}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Subject</TableCell>
              <TableCell>User</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Updated</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={7} align="center">Loading…</TableCell>
              </TableRow>
            )}
            {!loading && tickets.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center">No tickets found.</TableCell>
              </TableRow>
            )}
            {!loading && tickets.map((ticket) => (
              <TableRow key={ticket.id} hover>
                <TableCell sx={{ maxWidth: 280, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {ticket.subject}
                </TableCell>
                <TableCell>{ticket.userEmail}</TableCell>
                <TableCell>{ticket.category.replace('_', ' ')}</TableCell>
                <TableCell>
                  <Chip
                    label={STATUS_LABELS[ticket.status] || ticket.status}
                    color={STATUS_COLORS[ticket.status] || 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell>
                  <Chip
                    label={ticket.priority}
                    color={PRIORITY_COLORS[ticket.priority] || 'default'}
                    size="small"
                    variant="outlined"
                  />
                </TableCell>
                <TableCell>{new Date(ticket.updatedAt).toLocaleDateString()}</TableCell>
                <TableCell>
                  <Button size="small" onClick={() => navigate(`/support/tickets/${ticket.id}`)}>
                    View
                  </Button>
                  <Button size="small" onClick={() => openEdit(ticket)}>
                    Edit
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <TablePagination
        component="div"
        count={total}
        page={page}
        rowsPerPage={20}
        rowsPerPageOptions={[20]}
        onPageChange={(_, newPage) => setPage(newPage)}
      />

      {/* Edit dialog */}
      <Dialog open={!!editTicket} onClose={closeEdit} maxWidth="xs" fullWidth>
        <DialogTitle>Update Ticket</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          {editTicket && (
            <Typography variant="body2" color="text.secondary">{editTicket.subject}</Typography>
          )}
          <FormControl fullWidth size="small">
            <InputLabel>Status</InputLabel>
            <Select value={editStatus} label="Status" onChange={(e) => setEditStatus(e.target.value)}>
              {ALL_STATUSES.map((s) => (
                <MenuItem key={s} value={s}>{STATUS_LABELS[s]}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl fullWidth size="small">
            <InputLabel>Priority</InputLabel>
            <Select value={editPriority} label="Priority" onChange={(e) => setEditPriority(e.target.value)}>
              {ALL_PRIORITIES.map((p) => (
                <MenuItem key={p} value={p}>{p}</MenuItem>
              ))}
            </Select>
          </FormControl>
          {saveError && <Alert severity="error">{saveError}</Alert>}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeEdit} disabled={saving}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default AdminSupportPage;
