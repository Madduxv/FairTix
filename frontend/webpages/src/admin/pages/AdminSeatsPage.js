import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import Alert from '@mui/material/Alert';
import Breadcrumbs from '@mui/material/Breadcrumbs';
import Link from '@mui/material/Link';
import AddIcon from '@mui/icons-material/Add';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import SeatFormDialog from '../components/SeatFormDialog';
import api from '../../api/client';

const statusColors = {
  AVAILABLE: 'success',
  HELD: 'warning',
  BOOKED: 'error',
  SOLD: 'primary',
};

function AdminSeatsPage() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [formOpen, setFormOpen] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [eventData, seatsData] = await Promise.all([
        api.get(`/api/events/${eventId}`),
        api.get(`/api/events/${eventId}/seats`),
      ]);
      setEvent(eventData);
      setSeats(seatsData || []);
    } catch (err) {
      setError(err.message || 'Failed to load seat data.');
    } finally {
      setLoading(false);
    }
  }, [eventId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const statusSummary = seats.reduce((acc, seat) => {
    acc[seat.status] = (acc[seat.status] || 0) + 1;
    return acc;
  }, {});

  return (
    <Box>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link
          underline="hover"
          color="inherit"
          sx={{ cursor: 'pointer' }}
          onClick={() => navigate('/admin/events')}
        >
          Events
        </Link>
        <Typography color="text.primary">
          {event?.title || 'Loading...'}
        </Typography>
        <Typography color="text.primary">Seats</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Seats {event ? `- ${event.title}` : ''}
          </Typography>
          {!loading && (
            <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
              <Chip label={`${seats.length} total`} size="small" />
              {Object.entries(statusSummary).map(([status, count]) => (
                <Chip
                  key={status}
                  label={`${count} ${status.toLowerCase()}`}
                  size="small"
                  color={statusColors[status] || 'default'}
                />
              ))}
            </Box>
          )}
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/admin/events')}
          >
            Back
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setFormOpen(true)}>
            Add Seat
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Section</TableCell>
              <TableCell>Row</TableCell>
              <TableCell>Seat Number</TableCell>
              <TableCell>Price</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center">Loading...</TableCell>
              </TableRow>
            ) : seats.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">No seats added yet.</TableCell>
              </TableRow>
            ) : (
              seats.map((seat) => (
                <TableRow key={seat.id} hover>
                  <TableCell>{seat.section}</TableCell>
                  <TableCell>{seat.rowLabel}</TableCell>
                  <TableCell>{seat.seatNumber}</TableCell>
                  <TableCell>${seat.price?.toFixed(2) ?? '0.00'}</TableCell>
                  <TableCell>
                    <Chip
                      label={seat.status}
                      size="small"
                      color={statusColors[seat.status] || 'default'}
                    />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <SeatFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={fetchData}
        eventId={eventId}
      />
    </Box>
  );
}

export default AdminSeatsPage;
