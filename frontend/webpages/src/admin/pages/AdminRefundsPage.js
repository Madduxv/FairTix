import React, { useState, useEffect, useCallback } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import Button from '@mui/material/Button';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import Alert from '@mui/material/Alert';
import api from '../../api/client';

const STATUS_COLORS = {
  REQUESTED: 'warning',
  APPROVED: 'info',
  COMPLETED: 'success',
  REJECTED: 'error',
  CANCELLED: 'default',
};

function AdminRefundsPage() {
  const [refunds, setRefunds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [reviewDialog, setReviewDialog] = useState(null);
  const [adminNotes, setAdminNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const fetchRefunds = useCallback(() => {
    setLoading(true);
    setError('');
    const url = statusFilter
      ? `/api/admin/refunds?status=${statusFilter}&size=50`
      : '/api/admin/refunds?size=50';
    api.get(url)
      .then((data) => setRefunds(data?.content || []))
      .catch((err) => setError(err.message || 'Failed to load refunds'))
      .finally(() => setLoading(false));
  }, [statusFilter]);

  useEffect(() => { fetchRefunds(); }, [fetchRefunds]);

  const openReview = (refund) => {
    setReviewDialog(refund);
    setAdminNotes('');
    setSubmitError('');
  };

  const handleReview = async (approved) => {
    setSubmitting(true);
    setSubmitError('');
    try {
      await api.post(`/api/admin/refunds/${reviewDialog.id}/review`, {
        approved,
        adminNotes: adminNotes.trim() || null,
      });
      setReviewDialog(null);
      fetchRefunds();
    } catch (err) {
      setSubmitError(err.message || 'Failed to submit decision.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 3 }}>Refund Requests</Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 3, alignItems: 'center' }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Filter by Status</InputLabel>
          <Select
            value={statusFilter}
            label="Filter by Status"
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="REQUESTED">Pending Review</MenuItem>
            <MenuItem value="APPROVED">Approved</MenuItem>
            <MenuItem value="COMPLETED">Completed</MenuItem>
            <MenuItem value="REJECTED">Rejected</MenuItem>
            <MenuItem value="CANCELLED">Cancelled</MenuItem>
          </Select>
        </FormControl>
        <Button variant="outlined" size="small" onClick={fetchRefunds}>Refresh</Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Order ID</TableCell>
              <TableCell>User ID</TableCell>
              <TableCell>Amount</TableCell>
              <TableCell>Reason</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Submitted</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && (
              <TableRow><TableCell colSpan={7}>Loading...</TableCell></TableRow>
            )}
            {!loading && refunds.length === 0 && (
              <TableRow><TableCell colSpan={7}>No refund requests found.</TableCell></TableRow>
            )}
            {refunds.map((r) => (
              <TableRow key={r.id}>
                <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                  {r.orderId?.substring(0, 8)}…
                </TableCell>
                <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                  {r.userId?.substring(0, 8)}…
                </TableCell>
                <TableCell>${Number(r.amount).toFixed(2)}</TableCell>
                <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {r.reason}
                </TableCell>
                <TableCell>
                  <Chip
                    label={r.status}
                    color={STATUS_COLORS[r.status] || 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell>{new Date(r.createdAt).toLocaleDateString()}</TableCell>
                <TableCell>
                  {r.status === 'REQUESTED' && (
                    <Button size="small" variant="outlined" onClick={() => openReview(r)}>
                      Review
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={!!reviewDialog} onClose={() => setReviewDialog(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Review Refund Request</DialogTitle>
        <DialogContent>
          {reviewDialog && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
              <Box>
                <Typography variant="body2" color="text.secondary">Order ID</Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>{reviewDialog.orderId}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Amount</Typography>
                <Typography variant="body1">${Number(reviewDialog.amount).toFixed(2)}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Customer Reason</Typography>
                <Typography variant="body1">{reviewDialog.reason}</Typography>
              </Box>
              <TextField
                label="Admin Notes (optional)"
                multiline
                rows={3}
                value={adminNotes}
                onChange={(e) => setAdminNotes(e.target.value)}
                inputProps={{ maxLength: 1000 }}
                helperText="Notes will be included in the customer notification email."
              />
              {submitError && <Alert severity="error">{submitError}</Alert>}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReviewDialog(null)} disabled={submitting}>Cancel</Button>
          <Button
            color="error"
            onClick={() => handleReview(false)}
            disabled={submitting}
          >
            Reject
          </Button>
          <Button
            variant="contained"
            color="success"
            onClick={() => handleReview(true)}
            disabled={submitting}
          >
            Approve & Process
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default AdminRefundsPage;
